package tv.blackarrow.cpp.loader.bo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.components.filter.RestrictionFilter;
import tv.blackarrow.cpp.components.util.ESSClusterLockUtil;
import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.setting.FeedConfigBean;
import tv.blackarrow.cpp.utils.EventAction;
/**
 * Read file every hour from the inprocess directory
 * 
 * @author pzhang
 * 
 */
public class LinearBlackoutDataFileLoader {
    private static final Logger LOGGER = LogManager.getLogger(LinearBlackoutDataFileLoader.class);
    private static final String BLACKOUT_FEED_DATA_FILE_SUFFIX = "_blackout.json";
	private static final String BLACKOUT_FEED_DATA_FILE_DATEFORMAT = "yyyyMMddHHmmss";
	private static final int MAX_RETRY = 10;
	
    public void load() {
        File dataDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir());
        if (!dataDir.exists()) {
            LOGGER.fatal(()->"inprocess data directory does not exist: " + dataDir.getAbsolutePath());
            return;
        }
        LOGGER.info("I02 Blackout data files loading starts here.");

        // just keep trying to load until everything is loaded successfully.
        // if one of the nodes in the cluster has failed, it may take some time 
        // for the situation to be corrected. we need the load process to run 
        // once the system is working again though so keep trying.
        FeedConfigBean.load();
        Map<String, Map<String, String>> feedConfigMap = FeedConfigBean.getInstance().getFeedConfigMap();
        Set<String> availableFeeds = feedConfigMap.keySet();
        Map<String, Integer> retryFeeds = new HashMap<>();
		Set<String> loadedFeeds = new HashSet<>();
        while (true) {
        	LOGGER.info("Total Feeds available in the system and are to be tried for loading blackouts: " + availableFeeds);
            LOGGER.info("Loaded Feeds: " + loadedFeeds);
            LOGGER.info("Feeds to be retried for reloading: " + retryFeeds.keySet());

            for (String feedId : availableFeeds) {
            	if(!FeedConfigBean.getInstance().isI02Feed(feedId)) {
            		LOGGER.info("Skipping Feed \"" + feedId + "\" as this feed is not an I02 feed.");
	        		 continue;
	        	}
                try {
                	LOGGER.info("loading blackout data for feed \"" + feedId + "\".");
                	
                    String[] datafiles = getDataFilesToLoad(feedId);
                    if(loadedFeeds.contains(feedId)){
                    	LOGGER.info("Feed \"" + feedId + "\" has already been loadded, so skipping it.");
                		continue;
                	}
                    if (datafiles == null || datafiles.length == 0) {
                    	LOGGER.info("No data files present for Feed \"" + feedId + "\" so marking it as done and moving on.");
                    	loadedFeeds.add(feedId);
                    	retryFeeds.remove(feedId);//Since it has already been added in done list, remove it from retryFeed. This caused PRODISSUE-890
                    	continue;
                    }
                    
            		/** 
            		 * 1. Take required locks.  
            		 */
            		StringDocument lockedDocument = null;
            		try {
                        // other nodes in the cluster may be loading at the same time so use a lock to make sure only one node works with each feed at one time. Also            			
            			// Synchronize the actions between the loader and real-time updates to POIS across cluster.
            			LOGGER.info("Taking cluster-wide lock for feed \"" + feedId + "\", so that not other process can update/load/modify the data for this feed's blackouts.");
                		lockedDocument = ESSClusterLockUtil.acquireClusterWideLockWithFeedExternalRef(feedId);
            			
            	   	    // Load the data file.
            			LOGGER.info("Loading I02 blackout data for feed \"" + feedId + "\" started.");
            	   	    loadDataFile(feedId, datafiles);
                        loadedFeeds.add(feedId);
                        retryFeeds.remove(feedId);
                        LOGGER.info("Loading I02 blackout data for feed \"" + feedId + "\" completed.");
                    } finally {
                    	ESSClusterLockUtil.releaseClusterWideLock(lockedDocument);
                    		LOGGER.info("Released cluster-wide lock for feed \"" + feedId + "\".");
                    }
                } catch (Exception e) {
                    // if the data store is in a failure mode, we may get an exception but we
                    // don't want to give up on the load so just skip this one and we'll retry it
                    LOGGER.warn("Unexpected exception trying to load data for feed " + feedId + ", will retry", e);
                    int retryCount = retryFeeds.get(feedId)==null?0:retryFeeds.get(feedId);
                    loadedFeeds.remove(feedId);
                	retryFeeds.put(feedId, ++retryCount);
                }
            }
            
            if (!retryFeeds.isEmpty()) {
            	boolean shouldBreak = true;
            	for(String feed: retryFeeds.keySet()){
            		if(retryFeeds.get(feed) < MAX_RETRY){
            			shouldBreak = false;
            			break;
            		}
            	}
            	
            	if(shouldBreak){
            		LOGGER.warn("Enough retries(10) were made for loading blackouts data for all the feeds available but following feeds were still not loaded at the end of loading process <feed_id, number_of_retries>: " + retryFeeds);
            		break;
            	}
                // wait for some time to allow other nodes to finish and keep this process
                // from swamping the server in case of a failure.  
                LOGGER.info("Following feeds that should have been sucessfully loaded did not get loaded, waiting 30 seconds before retrying.");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                }
            } else {
                break;
            }
        }
        LOGGER.info("Completed loading I02 blackout data files.");
    }

    /**
     * fetch an array of data files in the order from oldest to the latest for the feed in the feed data directory
     * @param feedName
     * @return
     */
    private String[] getDataFilesToLoad(String feedName) {
        File feedDir = new File(LoaderUtil.getFeedDataDirectory(feedName));

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
            	return name.endsWith(BLACKOUT_FEED_DATA_FILE_SUFFIX); 
            }
        };

        String[] datafiles = feedDir.list(filter);
        if (datafiles != null && datafiles.length > 0) {        
        	Arrays.sort(datafiles);
        }
        return datafiles;
    }
    

    private long getCompileTime(String datafile) throws ParseException {

    	int index = datafile.indexOf("_");
    	String datestr = datafile.substring(0, index);
    	SimpleDateFormat format = new SimpleDateFormat(BLACKOUT_FEED_DATA_FILE_DATEFORMAT);
    	TimeZone tzone = TimeZone.getTimeZone("UTC");
    	format.setTimeZone(tzone);
    	Date date = format.parse(datestr);
		return date.getTime();
    	
    	
    }
    /**
     * load the blackout data from the data files which is sorted from the oldest to the latest 
     * only the latest data file is loaded. upon the completion of the data files, the data files are removed
     * from the feed data directory 
     * @param feedName
     * @param datafiles
     * @param locked if locked, the data will be save/update to couchbase.
     * @param feedSkipReschedule the set to mark if reschedule should be skip for feeds
     */
    private void loadDataFile(String feedName, String[] datafiles) {
    	if (datafiles == null || datafiles.length == 0){
    		return;
    	}

    	final String datafile = datafiles[datafiles.length - 1];
    	final String feedDir = LoaderUtil.getFeedDataDirectory(feedName);
        for (int i = 0; i < datafiles.length - 1; i++) {
        	final String filename = datafiles[i];
        	// remove all data files after loading the data 
        	LoaderUtil.deleteFile(feedDir, filename);
        }

    	final DataManager dataManager = DataManagerFactory.getInstance();
    	final long lastBlackoutRulesCompilationTime = dataManager.getBlackoutEventCompilationTime(feedName);
    	
    	FileInputStream instream = null;
    	
    	try {
	    	LOGGER.info("Last I02 blackout loading for feed \"" + feedName + "\" happened for the rules compiled at " + lastBlackoutRulesCompilationTime + " UTC time.");
    		long currentBlackoutRulesCompilationTime = getCompileTime(datafile);
	    	LOGGER.info("Now loading I02 blackout data for feed \"" + feedName + "\", that was compiled at " + lastBlackoutRulesCompilationTime + " UTC time.");
    		if (currentBlackoutRulesCompilationTime <= lastBlackoutRulesCompilationTime) {
    			LOGGER.info("Blackout feed data file already loaded for feed \"" + feedName +"\".");
    		} else {
    			//Fetch currently present blackout events from Couchbase.
    			List<BlackoutEvent> eventsPresentInDatastoreBeforeLoading = dataManager.getAllBlackoutEventsOnFeed(feedName);
    			LOGGER.info("Events present in datastore for feed \"" + feedName + "\": " + eventsPresentInDatastoreBeforeLoading);

    			Set<String> eventsDeletedSinceLastRulesLoading = dataManager.getEventsDeletedSinceLastRulesLoading(lastBlackoutRulesCompilationTime, feedName);
    			LOGGER.info("Events deleted since last rules loading that came via near realtime channel for feed \"" + feedName + "\": " + eventsDeletedSinceLastRulesLoading);
    			//Load the blackout events received in the compiled rule file for this feed.
    			Gson gson = new Gson();
    			File blackaoutRulesFile = new File(feedDir + File.separator + datafile);
				instream = new FileInputStream(blackaoutRulesFile);
				String blackoutJSONFromTheBORuleFile = IOUtils.toString(instream);
				List<BlackoutEvent> eventsReceivedInTheNewBORuleFile = gson.fromJson(blackoutJSONFromTheBORuleFile, new TypeToken<ArrayList<BlackoutEvent>>(){}.getType());
				RestrictionFilter.cleanupRestrictions(eventsReceivedInTheNewBORuleFile);
				LOGGER.info("Events received in the new rules file for feed \"" + feedName + "\": " + eventsReceivedInTheNewBORuleFile);

				LOGGER.info("Starting the merge of events from Datastore and events from rules file for feed \"" + feedName + "\".");

				Map<String,BlackoutEvent> signalIdVsBlackoutEvents = new HashMap<String, BlackoutEvent>();
				if(eventsPresentInDatastoreBeforeLoading!=null && !eventsPresentInDatastoreBeforeLoading.isEmpty()) {
					for (BlackoutEvent blackoutEvent : eventsPresentInDatastoreBeforeLoading) {
						signalIdVsBlackoutEvents.put(blackoutEvent.getSignalId(),blackoutEvent);
					}
				}
				List<BlackoutEvent> filteredOutThrowAwayEvents = new ArrayList<>();
				//Data store event List: Cleanup 1, Remove all the Deleted events from the list of events in the data store.

				//Remove if somehow any deleted events have reached to this blackout events list maintained at the feed level.
				Map<String, List<BlackoutEvent>> eventsByEventIdFromDatastore = new HashMap<>();
				if(eventsPresentInDatastoreBeforeLoading!=null) {
					Iterator<BlackoutEvent> eventIterator = eventsPresentInDatastoreBeforeLoading.listIterator();
					while(eventIterator.hasNext()) {
						BlackoutEvent blackoutEvent = eventIterator.next();
						if(EventAction.DELETE.equals(blackoutEvent.getEventAction()) ||
								(eventsDeletedSinceLastRulesLoading != null && eventsDeletedSinceLastRulesLoading.contains(blackoutEvent.getSignalId()))) {
							LOGGER.info("Removing the event " + blackoutEvent.getEventId() + "(" + blackoutEvent.getSignalId() +
									") from the list maintained at feed level for the feed \"" + feedName + "\" as this was in DELETED state.");
							eventIterator.remove();
							filteredOutThrowAwayEvents.add(blackoutEvent);
						} else {
							eventsByEventIdFromDatastore.computeIfAbsent(blackoutEvent.getEventId(), k -> new ArrayList<BlackoutEvent>()).add(blackoutEvent);
						}
					}
				}
				
				//Data store event List: Cleanup 2, Remove all the events having the same event id and overlapping time periods, keep only the one with latest update time.

				//if there are more than one events with the same event id, see that if they are overlapping with each other.
				//if they are, then just keep the one with the latest last update date.
				Map<String, BlackoutEvent> eventsBySignalIdFromDatastore = new HashMap<>();
				if(!eventsByEventIdFromDatastore.isEmpty()) {
					for(Map.Entry<String, List<BlackoutEvent>> entry : eventsByEventIdFromDatastore.entrySet()) {
						List<BlackoutEvent> eventsWithSameEventId = entry.getValue();
						if(!eventsWithSameEventId.isEmpty() && eventsWithSameEventId.size() > 1) {
							LOGGER.info("More than one blackout event found on feed \""+ feedName +"\" that has the same event id. Going to compare there execution time and " +
									"keep only the one having the latest last update date if there life span overlaps with each other.");
							Collections.sort(eventsWithSameEventId, Comparator.comparing((BlackoutEvent event) -> event.getUtcStartTime()));
							Iterator<BlackoutEvent> eventsFromDataStoreIterator = eventsWithSameEventId.iterator();
							BlackoutEvent currentEventFromDataStore = eventsFromDataStoreIterator.next();
							BlackoutEvent previousEventFromDataStore = currentEventFromDataStore;
							while(eventsFromDataStoreIterator.hasNext()) {
								currentEventFromDataStore = eventsFromDataStoreIterator.next();
								if(previousEventFromDataStore.getUtcStartTime() <= currentEventFromDataStore.getUtcStartTime() && currentEventFromDataStore.getUtcStartTime() <= previousEventFromDataStore.getUtcStopTime() ||
										previousEventFromDataStore.getUtcStartTime() <= currentEventFromDataStore.getUtcStopTime() && currentEventFromDataStore.getUtcStopTime() <= previousEventFromDataStore.getUtcStopTime()) {
									if(previousEventFromDataStore.getLastUpdateTime() >= currentEventFromDataStore.getLastUpdateTime()) {
										LOGGER.info("Removing the event " + currentEventFromDataStore.getEventId() + "(" + currentEventFromDataStore.getSignalId() +
												") from the list maintained at feed level for the feed \"" + feedName + "\" as this was in DELETED state.");
										filteredOutThrowAwayEvents.add(currentEventFromDataStore);
									} else {
										LOGGER.info("Removing the event " + previousEventFromDataStore.getEventId() + "(" + previousEventFromDataStore.getSignalId() +
												") from the list maintained at feed level for the feed \"" + feedName + "\" as this was in DELETED state.");
										previousEventFromDataStore = currentEventFromDataStore;
										filteredOutThrowAwayEvents.add(previousEventFromDataStore);
									}
								} else {
									eventsBySignalIdFromDatastore.put(previousEventFromDataStore.getSignalId(), previousEventFromDataStore);
									previousEventFromDataStore = currentEventFromDataStore;
								}
							}
							eventsBySignalIdFromDatastore.put(previousEventFromDataStore.getSignalId(), previousEventFromDataStore);
						} else {
							BlackoutEvent currentEventFromDataStore = eventsWithSameEventId.get(0);
							eventsBySignalIdFromDatastore.put(currentEventFromDataStore.getSignalId(), currentEventFromDataStore);
						}
					}
				}
				
				//Data store event List: Cleanup 3, Remove all the events having the last update time before the new compilation time. As if they are still there they would be in rules file as well.
				if(eventsBySignalIdFromDatastore !=null && !eventsBySignalIdFromDatastore.isEmpty()){
					Iterator<Map.Entry<String, BlackoutEvent>> iteratorForEventsFromDatastore = eventsBySignalIdFromDatastore.entrySet().iterator();
					while(iteratorForEventsFromDatastore.hasNext()) {
						Map.Entry<String, BlackoutEvent> eventFromDataStore = iteratorForEventsFromDatastore.next();
						if(eventFromDataStore != null && eventFromDataStore.getValue().getLastUpdateTime() < currentBlackoutRulesCompilationTime) {
							LOGGER.info("Event " + eventFromDataStore.getValue().getEventId() + "(" + eventFromDataStore.getValue().getSignalId() +") present in the data store for feed \"" + feedName + "\", "
					        			+ "has a last update time that is prior to rules compilation time so removing this event from the list. later on it would be added to the list if present in the rules file.");
							iteratorForEventsFromDatastore.remove();
							filteredOutThrowAwayEvents.add(eventFromDataStore.getValue());
						}
					}
				}
				
				
				//Rules event List: Cleanup 1, Prepare a map of event signal id by event for the events that came from the rule file.
				Map<String, BlackoutEvent> eventsBySignalIdFromRulesFile = new HashMap<>();
				if(eventsReceivedInTheNewBORuleFile!=null) {
					Iterator<BlackoutEvent> eventIterator = eventsReceivedInTheNewBORuleFile.listIterator();
					while(eventIterator.hasNext()) {
						BlackoutEvent blackoutEvent = eventIterator.next();
						if(EventAction.DELETE.equals(blackoutEvent.getEventAction()) ||
								(eventsDeletedSinceLastRulesLoading != null && eventsDeletedSinceLastRulesLoading.contains(blackoutEvent.getSignalId()))) {
							LOGGER.info("Removing the event " + blackoutEvent.getEventId() + "(" + blackoutEvent.getSignalId() +
									") from the list maintained at feed level for the feed \"" + feedName + "\" as this was in DELETED state.");
							eventIterator.remove();
							filteredOutThrowAwayEvents.add(blackoutEvent);
						} else {
							eventsBySignalIdFromRulesFile.put(blackoutEvent.getSignalId(), blackoutEvent);
						}
					}
				}

				//Rules event List: Cleanup 2, Remove all the events that are older than the events that are present in the data store.
				if(eventsBySignalIdFromRulesFile !=null && !eventsBySignalIdFromRulesFile.isEmpty()){
					Iterator<Map.Entry<String, BlackoutEvent>> iteratorForEventsFromRulesFile = eventsBySignalIdFromRulesFile.entrySet().iterator();
					while(iteratorForEventsFromRulesFile.hasNext()) {
						Map.Entry<String, BlackoutEvent> eventFromRuleFile = iteratorForEventsFromRulesFile.next();
						BlackoutEvent eventFromDataStore = eventsBySignalIdFromDatastore.get(eventFromRuleFile.getKey());
						if(eventFromDataStore != null && eventFromDataStore.getLastUpdateTime() > eventFromRuleFile.getValue().getLastUpdateTime()) {
							LOGGER.info("Event " + eventFromDataStore.getEventId() + "(" + eventFromDataStore.getSignalId() +") present in rule file for feed \"" + feedName + "\", "
					        			+ "seems to have already been updated by the realtime update flow, so skipping the processing for this event.");
							iteratorForEventsFromRulesFile.remove();
						}
					}
				}

				LOGGER.info("Final list of events remaining after cleanup that was fetched from datastore for feed \"" + feedName + "\": " + eventsBySignalIdFromDatastore.keySet().toString());
				LOGGER.info("Final list of events remaining after cleanup that was fetched from rulesfile for feed \"" + feedName + "\": " + eventsBySignalIdFromRulesFile.keySet().toString());

				//Prepare the final list to schedule the events.
				List<BlackoutEvent> finalListOfEvents = new ArrayList<>();
				finalListOfEvents.addAll(eventsBySignalIdFromDatastore.values());
				finalListOfEvents.addAll(eventsBySignalIdFromRulesFile.values());

				Calendar eventExpirationTime = Calendar.getInstance();
				int expirationSeconds = 24*60*60;// Taking By Default to 1 Day
				eventExpirationTime.add(Calendar.SECOND, -expirationSeconds);

				LOGGER.info("Now the application will be cleaning all the events that have been expired for more than 24 hours in the past for the feed \"" + feedName +"\".");
				Iterator<BlackoutEvent> iterator = finalListOfEvents.iterator();
				while(iterator.hasNext()) {
					BlackoutEvent blackoutEvent = iterator.next();
					ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedPlacementOpportunity(blackoutEvent.getSignalId());
					if(BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent) < eventExpirationTime.getTimeInMillis()) {
						LOGGER.info("Event " + blackoutEvent.getEventId() + "(" + blackoutEvent.getSignalId() + ") End time is more than 24 Hours in past, so ignoring this Blackout event on feed: \"" + feedName +"\"");
						iterator.remove();
						filteredOutThrowAwayEvents.add(blackoutEvent);
					}
				}

				List<String> validEventSignalIds = finalListOfEvents.stream().map(validEvent -> validEvent.getSignalId()).collect(Collectors.toList());
				filteredOutThrowAwayEvents = filteredOutThrowAwayEvents.stream().filter(throwAway -> !validEventSignalIds.contains(throwAway.getSignalId()))
						.collect(Collectors.toList());

				LOGGER.info("Final cleanedup events list on feed \"" + feedName + "\" is: " + filteredOutThrowAwayEvents.toString());
				LOGGER.info("Final valid events list on feed \"" + feedName + "\" is: " + finalListOfEvents.toString());

				Set<String> acquistionPointSet = dataManager.getFeedToAcquistionMap(feedName);				
				if(acquistionPointSet == null || acquistionPointSet.isEmpty()){
					LOGGER.info("Skipping the scheduling for the following feed as there are no acquisition points available for this feed: " + feedName );
				} else {
					List<AcquisitionPoint> acquistionPointList = dataManager.getAcquisitionPoints(acquistionPointSet);
					if (!filteredOutThrowAwayEvents.isEmpty()) {						
						//TODO UNSchedule notifications for this signal IDs
						SchedulingClient.unScheduleAllNotificationsForRulesLoading(feedName,acquistionPointList, filteredOutThrowAwayEvents);
					}
					//Reschedule all notifications for these BOs across the cluster.
					if (!finalListOfEvents.isEmpty()) {
						dataManager.putBlackoutEvents(feedName, gson.toJson(finalListOfEvents, new TypeToken<ArrayList<BlackoutEvent>>() {
						}.getType()), currentBlackoutRulesCompilationTime);
						for (BlackoutEvent blackoutEvent : finalListOfEvents) {
							// update/save individual events in Couchbase.
							dataManager.putBlackoutEvent(blackoutEvent);
						}
						//Reschedule OOB notifications for this BO if required.
						SchedulingClient.scheduleAllNotificationsForRulesLoading(feedName,acquistionPointList, finalListOfEvents);
					} else {
						LOGGER.info(() -> "No Blackouts present for the feed: " + feedName);
					}
				}
    		}
    		LoaderUtil.deleteFile(feedDir, datafile); 

    	} catch (Exception ex) {    	
    		LOGGER.error(()->"invalid blackout feed data file format: " + datafile, ex);
    		LoaderUtil.moveFile(LoaderUtil.getFeedDataDirectory(feedName) + File.separator + datafile, LoaderUtil.getErrorDirectory(), datafile);
    	} finally{
    	    if(instream != null){
    	        try {
                    instream.close();
                } catch (Exception e) {}
    	    }
    	}
    	
    }

}
