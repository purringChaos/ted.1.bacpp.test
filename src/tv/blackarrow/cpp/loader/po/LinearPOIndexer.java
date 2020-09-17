package tv.blackarrow.cpp.loader.po;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.RuntimeEnvironmentState;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.setting.FeedConfigBean;
import tv.blackarrow.cpp.setting.SettingUtils;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;

public class LinearPOIndexer {

    private static final Logger LOGGER = LogManager.getLogger(LinearPOIndexer.class);
    private static Map<String, List<String>> feedAcquisitionPointListMap = new ConcurrentHashMap<String, List<String>>();

    public void indexOneFeed(String feedID, List<ZonePO> poList, String pathName) {
        DataManager dataManager = DataManagerFactory.getInstance();

        LOGGER.debug(()->"Linear Indexer processing feed: " + feedID + ", and considering " + poList.size() + " pos from file \""
                + pathName + "\"");
        String dataFileName = new File(pathName).getName();
        LoaderCursor loaderCursor = dataManager.getLoaderCursor(feedID);
        if (loaderCursor == null) {
            loaderCursor = new LoaderCursor();
            loaderCursor.setFeedExternalRef(feedID);
            HashMap<String, String> lastPOKeyByZone = new HashMap<String, String>();
            loaderCursor.setLastPOKeyByZone(lastPOKeyByZone);
        } else {
            if (dataFileName.compareTo(loaderCursor.getLastLoadedDataFile()) <= 0) {
                LOGGER.debug(()->"Data file \"" + pathName + "\" has already been loaded for feed: " + feedID + " skipping");
                return;
            }
        }
        
        if (poList == null || poList.size() == 0) {
            LOGGER.debug(()->"No new Placement Opportunity data for feed " + feedID + " in file \"" + pathName + "\", skipping indexing.");

            // even though there was no data we still want to update the last loaded
            // data file name so that we don't keep trying to load that file
            loaderCursor.setLastLoadedDataFile(dataFileName);
            dataManager.putLoaderCursor(loaderCursor);
            return;
        }
        Map<String, String> lastPOKeyByZone = loaderCursor.getLastPOKeyByZone();
        Map<String, String> firstPOKeyByZone = new HashMap<String, String>();

        List<String> acqIDList = getAcquisitionPointListByFeed(feedID);
        List<SignalProcessorCursor> sigProcessorCursorList = new ArrayList<SignalProcessorCursor>();
        for (String acqID : acqIDList) {
            SignalProcessorCursor sigCursor = dataManager.getSignalProcessorCursor(acqID);
            if (sigCursor == null) {
                sigCursor = new SignalProcessorCursor();
                sigCursor.setAcquisitionPointIdentity(acqID);
                HashMap<String, String> nextPOKeyByZone = new HashMap<String, String>();
                sigCursor.setNextPOKeyByZone(nextPOKeyByZone);
            }
            sigProcessorCursorList.add(sigCursor);
        }

        // first just make sure all of the POs are saved in the data store. note that
        // the po lists are already linked together in the parser so we don't need to 
        // link each of them here, we only need to link the whole list into the existing
        // list at the end.  that saves a lot of updates on the data store and avoids
        // some concurrency issues.
        for (ZonePO zonePO : poList) {
            PlacementOpportunity po = zonePO.getPlacementOpportunity();
            if (dataManager.getPlacementOpportunity(po.getPOKey()) == null) {
                LOGGER.debug(()->"Adding PO \"" + po.getPOKey() + "\" to data store");
                dataManager.putPlacementOpportunity(po);
            }
            if (firstPOKeyByZone.get(zonePO.getZoneExtRef()) == null) {
                firstPOKeyByZone.put(zonePO.getZoneExtRef(), po.getPOKey());
            }
        }

        // now link the new PO lists onto the end of the existing PO lists.  because
        // these POs could be actively in use by the signal processor, we need to do
        // this using compare-and-set to make sure we don't overwrite any changes.
        for (Map.Entry<String, String> entry : firstPOKeyByZone.entrySet()) {
            String zone = entry.getKey();
            String firstPOKey = entry.getValue();
            String lastPOKey = lastPOKeyByZone.get(zone);
            if (lastPOKey != null) {
                // going to do a compare-and-set so we need to handle retrying
                int retries = 0;
                while (true) {
                    PlacementOpportunity prevPO = dataManager.getPlacementOpportunity(lastPOKey);
                    // if the PO is gone for some reason (multiple node failures without re-balance in between)
                    // then we will not be able to link these POs together
                    if (prevPO == null) {
                        LOGGER.error(()->"Placement opportunity with key \"" + lastPOKey + "\" was not found for zone \"" + zone
                                + "\". Unable to link PO lists together, skipping link step");
                        break;
                    }
                    prevPO.setNextPOKey(firstPOKey);
                    LOGGER.debug(()->"Linking PO \"" + prevPO.getPOKey() + "\" to PO \"" + firstPOKey + "\"");
                    if (dataManager.casPlacementOpportunity(prevPO)) {
                        break;
                    }
                    retries++;
                    if (retries > 10) {
                        throw new RuntimeException("Unable to add new POs to end of PO list with key \"" + lastPOKey
                                + "\" after multiple retries");
                    }
                    LOGGER.debug(()->"Compare-and-set failed while linking PO \"" + prevPO.getPOKey() + "\" to PO \"" + firstPOKey
                            + "\", retrying");
                }
            }
        }

        // now we can advance the loader cursor pointers to the end of each list
        for (ZonePO zonePO : poList) {
            lastPOKeyByZone.put(zonePO.getZoneExtRef(), zonePO.getBreakID());

            // it's possible that this load has some new zones in it so we need to
            // be sure that those get added to the signal processor cursor.
            for (SignalProcessorCursor cursor : sigProcessorCursorList) {
                String nextPOKey = cursor.getNextPOKeyByZone().get(zonePO.getZoneExtRef());
                if (nextPOKey == null) {
                    cursor.getNextPOKeyByZone().put(zonePO.getZoneExtRef(), zonePO.getBreakID());
                }
            }
        }
        loaderCursor.setLastLoadedDataFile(dataFileName);

        // note that in this update we are saving the fact that we have already loaded
        // this data file and the new PO keys for the end of the linked lists. this ensures
        // that this process is fault tolerant.
        dataManager.putLoaderCursor(loaderCursor);

        // note: the merge operation guarantees that we won't overwrite any other
        // processes changes so we don't have to deal with that here.
        for (SignalProcessorCursor cursor : sigProcessorCursorList) {
            dataManager.mergeSignalProcessorCursor(cursor);
        }
        LOGGER.debug(()->"LinearIndexer indexed " + poList.size() + " POs for feed " + feedID + " from file \"" + pathName + "\".");
    }

    private List<String> getAcquisitionPointListByFeed(String feedID) {

        List<String> acquisitionPointList = feedAcquisitionPointListMap.get(feedID);
        if (acquisitionPointList == null) {
            Set<Entry<String, String>> entrySet = AcquisitionConfigBean.getInstance().getAcquisitionFeedMap().entrySet();
            acquisitionPointList = new ArrayList<String>();
            for (Entry<String, String> entry : entrySet) {
                if (entry.getValue().equals(feedID)) {
                    acquisitionPointList.add(entry.getKey());
                }
            }
            feedAcquisitionPointListMap.put(feedID, acquisitionPointList);
        }
        return acquisitionPointList;
    }

    public synchronized void refreshAcquisitionPoint(AcquisitionConfigBean previousLoadedAPConfigSpringBean) {
    	Set<String> previouslyLoadedAPSet = null;
    	if (previousLoadedAPConfigSpringBean != null && previousLoadedAPConfigSpringBean.getAcquisitionPointMap() != null) {
			previouslyLoadedAPSet = new HashSet<String>(previousLoadedAPConfigSpringBean.getAcquisitionPointMap().keySet());
		} else {
			previouslyLoadedAPSet = new HashSet<String>();
		}
    	
        LOGGER.debug(()->"Loading acquisition point data from mapping file");
        final DataManager dataManager = DataManagerFactory.getInstance();
        final AcquisitionConfigBean latestLoadedAPConfigSpringBean = AcquisitionConfigBean.getInstance();
		final Map<String, String> latestLoadedAPFeedIdMap = latestLoadedAPConfigSpringBean.getAcquisitionFeedMap();
		final Map<String, Boolean> latestLoadedAPsDeleteEmptyBreaValueMap = latestLoadedAPConfigSpringBean.getAcquisitionDeleteEmptyBreakMap();
		final Map<String, Boolean> latestLoadedAPInterfaceTypeMap = latestLoadedAPConfigSpringBean.getAcquisitionInterfaceTypeMap();
		final Map<String, String> latestLoadedAPHLSInterfaceTypeMap = latestLoadedAPConfigSpringBean.getAcquisitionHLSInterfaceTypeMap();
		final Map<String, String> latestLoadedAPHSSInterfaceTypeMap = latestLoadedAPConfigSpringBean.getAcquisitionHSSInterfaceTypeMap();
		final Map<String, String> latestLoadedAPSchedulesInterfaceTypeMap = latestLoadedAPConfigSpringBean
                .getAcquisitionSchedulesInterfaceTypeMap();
		final Map<String, Long> latestLoadedAPSignalTimeOffsetMap = latestLoadedAPConfigSpringBean.getAcquisitionSignalTimeOffsetMap();
		final Map<String, Long> latestLoadedAPlastUpdateTimeMap = latestLoadedAPConfigSpringBean.getAcquisitionLastUpdatedTimeMap();

        final Iterator<String> latestLoadedAPFeedIdMapKeySetIterator = latestLoadedAPFeedIdMap.keySet().iterator();
        final FeedConfigBean latestLoadedFeedConfigBean = FeedConfigBean.getInstance();
        while (latestLoadedAPFeedIdMapKeySetIterator.hasNext()) {
            String acqID = latestLoadedAPFeedIdMapKeySetIterator.next();
            previouslyLoadedAPSet.remove(acqID);		// remove 
            AcquisitionPoint dataSourcePersistedAP = dataManager.getAcquisitionPoint(acqID);
            if (dataSourcePersistedAP == null) {
                dataSourcePersistedAP = new AcquisitionPoint();
                dataSourcePersistedAP.setAcquisitionPointIdentity(acqID);
            }
            String feedID = latestLoadedAPFeedIdMap.get(acqID);
            Boolean sccDeleteEmptyBreak = latestLoadedAPsDeleteEmptyBreaValueMap.get(acqID);
            Boolean interfaceType = latestLoadedAPInterfaceTypeMap.get(acqID);
            String hlsInterfaceType = latestLoadedAPHLSInterfaceTypeMap.get(acqID);
            String hssInterfaceType = latestLoadedAPHSSInterfaceTypeMap.get(acqID);
            String schedulesInterfaceType = latestLoadedAPSchedulesInterfaceTypeMap.get(acqID);
            long signalTimeOffset = latestLoadedAPSignalTimeOffsetMap.get(acqID);
            long lastUpdatedTime =  latestLoadedAPlastUpdateTimeMap.get(acqID);
            
            Map<String, String> latestLoadedAPConfigFieldsValuesMap = latestLoadedAPConfigSpringBean.getAcquisitionPointConfig(acqID);

            dataSourcePersistedAP.setFeedExternalRef(feedID);
            dataSourcePersistedAP.setNetworkExternalRef(getNetworkExtRefByFeed(feedID));
            dataSourcePersistedAP.setProviderExternalRef(getProviderExtRefByFeed(feedID));
            dataSourcePersistedAP.setSignalAbortEnabled(getSignalAbortEnabledByFeed(feedID));
            dataSourcePersistedAP.setSccDeleteEmptyBreak(sccDeleteEmptyBreak);
            dataSourcePersistedAP.setBaIntefactTypeExternalRef(interfaceType ? "includeInPoint" : "linear");
            dataSourcePersistedAP.setBaHlsInterfaceTypeExternalRef(hlsInterfaceType);
            dataSourcePersistedAP.setBaHssInterfaceTypeExternalRef(hssInterfaceType);
            dataSourcePersistedAP.setBaSchedulesInterfaceTypeExternalRef(schedulesInterfaceType);
            dataSourcePersistedAP.setSignalTimeOffset(signalTimeOffset);
			dataSourcePersistedAP.setFeedAllowsOpenEndedBlackouts(latestLoadedFeedConfigBean.isOpenEndedBlackoutAllowed(feedID));
            dataSourcePersistedAP.setFeedHasAlternateContentEnabled(latestLoadedFeedConfigBean.isAltContentEnabled(feedID));
            dataSourcePersistedAP.setFeedsAlternateContentVersion(latestLoadedFeedConfigBean.getAltContentVersion(feedID));
            dataSourcePersistedAP.setSchedulelessAlternateContentLocation(latestLoadedFeedConfigBean.getSchedulelessAltContentLocation(feedID));
            dataSourcePersistedAP.setUseInbandOpportunitySignal(latestLoadedFeedConfigBean.getUseInBandPlacementOpportunitySignals(feedID));
            
            dataSourcePersistedAP.setFeedTriggerEventsByEventID(latestLoadedFeedConfigBean.isTriggerEventsByEventID(feedID));
            dataSourcePersistedAP.setSpliceInsertConfiguredValue(SignalHandlingConfiguration.getConfigurationValue(
            		latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMap(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.SPLICE_INSERT)));
            dataSourcePersistedAP.setProviderAdsConfiguredValue(SignalHandlingConfiguration.getConfigurationValue(
            		latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMap(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.PROVIDER_ADS)));
            dataSourcePersistedAP.setDistributorAdsConfiguredValue(SignalHandlingConfiguration.getConfigurationValue(
            		latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMap(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.DISTIRIBUTOR_ADS)));
            dataSourcePersistedAP.setPoSignalsConfiguredValue(SignalHandlingConfiguration.getConfigurationValue(
            		latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMap(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.PO_SIGNALS)));
            dataSourcePersistedAP.setInBandContentIdConfiguredValue(SignalHandlingConfiguration.getConfigurationValue(
            		latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMap(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.INBAND_CONTENT_ID)));
            dataSourcePersistedAP.setAdSlateLocation(latestLoadedFeedConfigBean.getAdSlateLocation(feedID));
            if(latestLoadedFeedConfigBean.isAltContentEnabled(feedID)) {
				dataSourcePersistedAP.setFeedSCCNotificationBuffer(
						latestLoadedFeedConfigBean.getSCCNotificationBuffer(feedID) != null ? Integer.parseInt(latestLoadedFeedConfigBean.getSCCNotificationBuffer(feedID)) : 0);
            }
            
            String zoneIdentity = latestLoadedAPConfigSpringBean.getValueFromAcquistionPointMapIfPresent(dataSourcePersistedAP.getAcquisitionPointIdentity(), SettingUtils.ZONE_IDENTITY);
            if(zoneIdentity != null) {
            	dataSourcePersistedAP.setZoneIdentity(zoneIdentity);
            }
            
            try {
            	// in band and out of band specific parameters
            	if(latestLoadedFeedConfigBean.isAltContentEnabled(feedID)) {
	            	String blackoutConfirmationType = latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.BLACKOUT_CONFIRMATION_TYPE);
	            	dataSourcePersistedAP.setBlackoutConfirmationType(blackoutConfirmationType);
	            	if(CppConstants.IN_BAND.equalsIgnoreCase(blackoutConfirmationType) || CppConstants.ENABLED.equalsIgnoreCase(blackoutConfirmationType) || CppConstants.DISABLED.equalsIgnoreCase(blackoutConfirmationType)) {
	            		String v = latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.PROGRAM_START_BUFFER);
	            		if(v!=null){
	            			dataSourcePersistedAP.setProgramStartBuffer(Integer.parseInt(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.PROGRAM_START_BUFFER)));
	            		}
	            	}
	            	dataSourcePersistedAP.setTranscoderEndpoint(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.TRANSCODER_ENDPOINT));
	            	
	            	String v = latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.CONTENT_ID_FREQUENCY);
	            	if(v != null){
	            		dataSourcePersistedAP.setContentIDFrequency(Integer.parseInt(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.CONTENT_ID_FREQUENCY)));
	            	}
	            	dataSourcePersistedAP.setDeliveryType(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.DELIVERY_TYPE));

					v = latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.EXECUTION_TYPE);
					if (v != null) {
						dataSourcePersistedAP.setExecutionType(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.EXECUTION_TYPE));
					}
            	}
            	dataSourcePersistedAP.setEsamVersion(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.ESAM_VERSION));
            	
            	//Update the last updated time for the AP.
            	dataSourcePersistedAP.setLastUpdatedTime(lastUpdatedTime);
                // SCC Notification Buffer
				String v = latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.STREAM_SCC_NOTIFICATION_BUFFER);
				if (v != null) {
					//TODO: osho.3 onwards below is not needed. This is only used for backward compatibitlity to ned.1. After all customer's linear moves to 9.2.2 branch remove this code.
					dataSourcePersistedAP.setSCCNotificationBuffer(Integer.valueOf(latestLoadedAPConfigFieldsValuesMap.get(SettingUtils.STREAM_SCC_NOTIFICATION_BUFFER)));
                }
            } catch(Exception ex) { // catch any exception and log it
            	LOGGER.error(()->ex.getMessage(), ex);
            }
            dataManager.putAcquisitionPoint(dataSourcePersistedAP);
        }
        feedAcquisitionPointListMap.clear();
        
        //  remove the acquisition point not being used anymore.
        Iterator<String> itr = previouslyLoadedAPSet.iterator();
        while (itr.hasNext()) {
        	dataManager.deleteAcquisitionPoint(itr.next());
        }
        
        // also keep a record of each feed -- acquisition points mapping
        persistFeedToAcquisitionPointMap();
        
        RuntimeEnvironmentState evState = new RuntimeEnvironmentState();
        evState.setAcquisitionPointIdentities(new HashSet<String>(latestLoadedAPFeedIdMap.keySet()));
        dataManager.putRuntimeEnvironmentState(evState);
    }

    public String getProviderExtRefByFeed(String feedID) {
        Map<String, String> feedProviderMap = FeedConfigBean.getInstance().getFeedProviderMap();
        return feedProviderMap.get(feedID);
    }

    public String getNetworkExtRefByFeed(String feedID) {
        Map<String, String> feedNetworkMap = FeedConfigBean.getInstance().getFeedNetworkMap();
        return feedNetworkMap.get(feedID);
    }
    
    public boolean getSignalAbortEnabledByFeed(String feedID) {
        Map<String, Boolean> feedNetworkMap = FeedConfigBean.getInstance().getFeedSignalAbortEnabledMap();
        Boolean enabled = feedNetworkMap.get(feedID);
        if(enabled == null) {
			return false;
		}
        return enabled;
    }
    
    /**
     * it will save feed to acquisition point mapping records in the DB for future usage 
     */
    public void persistFeedToAcquisitionPointMap() {
    	Map<String, Set<String>> feedMap = AcquisitionConfigBean.getInstance().getFeedToAcquisitionPointMap();
    	Set<String> keySet = feedMap.keySet();
		Iterator<String> it = keySet.iterator();
		while(it.hasNext()) {
			String key = it.next(); // feed
			DataManagerFactory.getInstance().putFeedToAcquistionMap(key, feedMap.get(key));
		}
    }
}
