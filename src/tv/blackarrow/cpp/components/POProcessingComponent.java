package tv.blackarrow.cpp.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.exeptions.CppException;
import tv.blackarrow.cpp.exeptions.ResourceNotFoundException;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.POInfo;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.UUIDUtils;

/**
 * Component that processes and returns the placement opportunity given
 * acquisition point and utc signal time
 * 
 * @author mchakkarapani, hcao
 *
 */
public class POProcessingComponent extends EventProcessor {
    private static final Logger log = LogManager.getLogger(POProcessingComponent.class);
	private String acquisitionPointIdentity = null;
    
    public POProcessingComponent() {
    }
    
    public POProcessingComponent(final AcquisitionPoint acquisitionPoint, final long utcSignalTime) {
    	this.acquisitionPoint = acquisitionPoint;
    	this.utcSignalTime = utcSignalTime;
    	if(acquisitionPoint !=null ){
    		this.acquisitionPointIdentity = acquisitionPoint.getAcquisitionPointIdentity();
    	}
    }
     

    public ConfirmedPlacementOpportunity getPlacementOpportunity(PoisAuditLogVO poisAuditLogVO) throws CppException, ResourceNotFoundException {
        if (acquisitionPointIdentity == null || acquisitionPointIdentity.trim().equals("") || utcSignalTime == -1) {
            throw new ResourceNotFoundException(3, "Passed acquisition point identity or utc signal time is null");
    	}
    	if (acquisitionPoint == null) {
            throw new ResourceNotFoundException(2, "Invalid acquisition point identity");
    	}
        return getNextConfirmedPlacementOpportunity(poisAuditLogVO);
    }
    
    protected ConfirmedPlacementOpportunity getNextConfirmedPlacementOpportunity(PoisAuditLogVO poisAuditLogVO) throws CppException {
    	DataManager dataManager = DataManagerFactory.getInstance();
    	SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
    	List<String> breakIds = new ArrayList<String>();
        List<String> outSignalIds = new ArrayList<String>();
        ArrayList<BreakInfo> breakInfos = new ArrayList<BreakInfo>();
        HashMap<String, Boolean> isNPOMap = new HashMap<String, Boolean>();
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
    	
    	ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
    	if (cursor != null) {
            HashMap<String, String> nextKeyByZone = cursor.getNextPOKeyByZone();
            HashMap<String, Boolean> lastProcessedPoKeyByZone = cursor.getLastProcessedPOKeyByZone();
            if (lastProcessedPoKeyByZone == null) {
				lastProcessedPoKeyByZone = new HashMap<String, Boolean>();
			}
    		Set<String> zoneSet = nextKeyByZone.keySet();
    		if (zoneSet != null && !zoneSet.isEmpty()) {
    			for (String zoneName : zoneSet) {
                    getNextUnconfirmedBreakInfo(zoneName, nextKeyByZone, breakIds, outSignalIds, breakInfos, poKeyByZone,
                            lastProcessedPoKeyByZone, isNPOMap,poisAuditLogVO);
    			}
    		}
    		cpo.setPoKeyByZone(poKeyByZone);
            
            // make sure cpo always have utcSignalTime set for multi-cue check
            // if this field is not set from start, the first signal will be treated
            // as duplicate signal
            cursor.setLastConfirmedPOUTCTime(utcSignalTime); 
            
            log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                    + ") and returned PO Key by Zone: " + poKeyByZone.toString());
    		cursor.setNextPOKeyByZone(nextKeyByZone);
            log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                    + ") and returned NextPO Key By Zone : " + nextKeyByZone.toString());
        	cursor.setLastProcessedPOKeyByZone(lastProcessedPoKeyByZone);
            log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                    + ") and returned Last processed Key: " + lastProcessedPoKeyByZone.toString());
    	}
    	
    	cpo.setPoKeyByZone(poKeyByZone);
    	cpo.setAcquisitionPointIdentity(acquisitionPointIdentity);
    	cpo.setUtcSignalTime(utcSignalTime);
    	
        //Let's write if it is NPO to the audit log, by matching all possible zones
        boolean isNPO = true;
        if (cursor != null) {
            Set<String> zoneSet = cursor.getNextPOKeyByZone().keySet();
            for (String zoneName : zoneSet) {
                Boolean value = isNPOMap.get(zoneName);
                if (value == null || value.booleanValue() == false) {
                    isNPO = false;
                    break;
                }
            }
        }

        if (isNPO) {
            writePODetailstoAuditLog(acquisitionPointIdentity, null, CppConstants.SIGNAL_NPO_EVENT_TYPE, utcSignalTime,poisAuditLogVO);
        }else{
        	updateSignalProcessorCursor(cursor);
        }

        //Let's sort the Breaks, before setting it into the Confirmed PO
        Collections.sort(breakInfos);
        if (outSignalIds != null && !outSignalIds.isEmpty()) {
            cpo.setSignalId(outSignalIds.get(0));
            cpo.setBreakInfos(breakInfos);
            writeConfirmedPOToAuditLog(acquisitionPointIdentity, cpo, breakIds,poisAuditLogVO);

            //this means we are successful in processing the break
            log.debug("Input(Acquition Point=" + acquisitionPointIdentity + ", utcSignalTime=" + utcSignalTime
                    + ") and returned Output(" + "SignalId=" + cpo.getSignalId() + ",Durations="
                    + cpo.getPlacementsDurationsInMilliseconds() + ")");
            return cpo;
        } else if (breakIds != null && !breakIds.isEmpty()) {
    		Collections.sort(breakIds);
    		cpo.setSignalId(breakIds.get(0));
            cpo.setBreakInfos(breakInfos);
            writeConfirmedPOToAuditLog(acquisitionPointIdentity, cpo, breakIds,poisAuditLogVO);

    		//this means we are successful in processing the break
            log.debug("Input(Acquition Point=" + acquisitionPointIdentity + ", utcSignalTime=" + utcSignalTime
                    + ") and returned Output(" + "SignalId=" + cpo.getSignalId() + ",Durations="
                    + cpo.getPlacementsDurationsInMilliseconds() + ")");
    		return cpo;
    	} 

    	//log the empty signalId
        log.debug("Input(Acquition Point=" + acquisitionPointIdentity + ", utcSignalTime=" + utcSignalTime
                + ") and returned Output(" + "SignalId=" + cpo.getSignalId() + ",Durations="
                + cpo.getPlacementsDurationsInMilliseconds() + ")");
    	return null;
    
    }
    
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity() throws CppException {
        DataManager dataManager = DataManagerFactory.getInstance();
        ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedPlacementOpportunity(acquisitionPointIdentity, utcSignalTime);
        if (cpo == null) { // to support multi-cue
            SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
            if (cursor != null && cursor.getLastConfirmedPOUTCTime() > 0) {
                ConfirmedPlacementOpportunity lastCPO = dataManager.getConfirmedPlacementOpportunity(acquisitionPointIdentity,
                        cursor.getLastConfirmedPOUTCTime());
                if (lastCPO == null) {
					return null;
				}

                Set<Integer> durationSet = lastCPO.getPlacementsDurationsInMilliseconds();

                // find the longest duration
                Integer longestDuration = 0;
                for (Integer i : durationSet) {
                    if (i.compareTo(longestDuration) > 0) {
                        longestDuration = i;
                    }
                }
                long duration = longestDuration.longValue();
                long cpoSignalTime = lastCPO.getUtcSignalTime();
				if (cpoSignalTime <= utcSignalTime
						&& utcSignalTime <= (cpoSignalTime + duration)) {
					cpo = lastCPO;
				}
            }
        }
        return cpo;
    }


    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String signalId) throws CppException {
        if (signalId == null || signalId.trim().equals("")) {
            throw new CppException("Passed SignalId is invalid");
        }
        DataManager dataManager = DataManagerFactory.getInstance();
        ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedPlacementOpportunity(signalId);
        return cpo;
    }

    protected void writeConfirmedPOToAuditLog(String acquisitionPointIdentity, final ConfirmedPlacementOpportunity cpo,
            List<String> breakIds, PoisAuditLogVO poisAuditLogVO) {
    	StringBuilder message = new StringBuilder();
    	final String EMPTY_STRING = "";
    	for (String breakId : breakIds) {
            message.append(System.currentTimeMillis()).append(",").append(CppConstants.SIGNAL_INS_EVENT_TYPE);
            message.append(",").append(cpo.getUtcSignalTime());
            message.append(",").append(acquisitionPointIdentity).append(",").append(cpo.getSignalId()).append(",").append(breakId);            
			message.append(",").append(poisAuditLogVO != null ? poisAuditLogVO.getIpAddressOfClient() : EMPTY_STRING);
			message.append(",").append(poisAuditLogVO != null ? poisAuditLogVO.getAcquisitionSignalID() : EMPTY_STRING);
    		AuditLogger.auditLog(message.toString());
    		message = new StringBuilder();
    	}
    	
    }
    
    private void getNextUnconfirmedBreakInfo(String zoneName, final Map<String, String> nextKeyByZone, final List<String> breakIds, 
            final List<String> outSignalIds, final List<BreakInfo> breakInfos, final Map<String, String> poKeyByZone,
            final Map<String, Boolean> lastProcessedPoKeyByZone, final Map<String, Boolean> isNPOMap, PoisAuditLogVO poisAuditLogVO) throws CppException {
    	DataManager dataManager = DataManagerFactory.getInstance();
    	String poKey = nextKeyByZone.get(zoneName);
    	PlacementOpportunity po = dataManager.getPlacementOpportunity(poKey);
    	PlacementOpportunity prevPo = null;
    	
    	if (po == null) {
            isNPOMap.put(zoneName, true);
            String msg = "Unable to locate next placement opportunity with key \"" + poKey + "\" for acquisition point "
                    + acquisitionPointIdentity + " zone " + zoneName;
            log.error(msg);
            throw new CppException(msg);
    	}
    	
    	//Let's check to see whether time signal is before the current window start time
        if (utcSignalTime < po.getUtcWindowStartTime()) {
            isNPOMap.put(zoneName, true);
            return;
        }
		
    	//Let's check to see if last processed PO, is still the last PO, and skip the processing
		Boolean value = lastProcessedPoKeyByZone.get(zoneName);
        if (value == null) {
			lastProcessedPoKeyByZone.put(zoneName, new Boolean(false));
		} else if (value.booleanValue() == true) {
            if (po.getNextPOKey() == null) {
                isNPOMap.put(zoneName, true);
                return;
            } else {
    			prevPo = po;
    			po = dataManager.getPlacementOpportunity(po.getNextPOKey());
    			lastProcessedPoKeyByZone.put(zoneName, new Boolean(false));
    			
                if (this.utcSignalTime < po.getUtcWindowStartTime()) {
                    nextKeyByZone.put(zoneName, po.getPOKey());
                    isNPOMap.put(zoneName, true);
                    return;
                }
    		}
    	}
    	
        boolean includeInPoints = false; //CCMS default
        if (acquisitionPoint != null)
		 {
			includeInPoints = CppConstants.INTERFACE_COMCAST_CDVR.equals(acquisitionPoint.getBaIntefactTypeExternalRef()); //explicitly asked to generate in points
		}

        // start a loop and iterate till you find matching break or you reach the end of the list
    	while (po != null) {
            // if the next window has not started yet then there is no PO and we are done searching.
            // it is possible for this to happen now that we may skip POs that were confirmed for
            // a different signal time on another acquisition point.
            if (utcSignalTime < po.getUtcWindowStartTime()) {
                isNPOMap.put(zoneName, true);
                return;
            }
            boolean matched = false;

            // see if the PO's window covers this signal time first
            long utcWindowEndTime = po.getUtcWindowStartTime() + po.getWindowDurationMilliseconds();
            if ((po.getUtcWindowStartTime() <= this.utcSignalTime) && (utcSignalTime < utcWindowEndTime)) {

                // this PO is eligible but it is possible in a failure scenario that this PO
                // was confirmed for a different signal time already by another acquisition point.
                // make sure that this signal time is within an acceptable range of the previously
                // confirmed signal time. we can't do an exact match because transcoders on different
                // servers may not have perfectly synchronized clocks.
                if (po.getUtcSignalTime() == null) {
                    matched = true;
                } else {
                    long delta = Math.abs(utcSignalTime - po.getUtcSignalTime());
                    if (delta <= CppConfigurationBean.getInstance().getPoBreakReconfirmationWindowInMillis()) {
                        matched = true;
                        if (log.isDebugEnabled()) {
                            log.debug("PO \"" + po.getPOKey() + "\" was already confirmed with a signal time of "
                                    + po.getUtcSignalTime() + " but is close enough to this signal at " + utcSignalTime
                                    + " on acquisition point " + acquisitionPointIdentity
                                    + " to be considered the same signal.  Confirming this PO.");
                        }
                    } else {
                        log.warn("PO \"" + po.getPOKey() + "\" was already confirmed with a signal time of "
                                + po.getUtcSignalTime() + " and is not close enough to this signal at " + utcSignalTime
                                + " on acquisition point " + acquisitionPointIdentity
                                + " to be considered the same signal.  This PO will be skipped. "
                                + "This should only happen in the event of a system failure.");
                    }
                }
            }
            if (matched) {
                // Now that we are confirming this po, set it's signal time if necessary
                // so other acquisition points can be kept in synch if necessary.
                if (po.getUtcSignalTime() == null) {
                    setUtcSignalTime(po, utcSignalTime);
                }
    			poKeyByZone.put(zoneName, po.getPOKey());
    			if (po.getNextPOKey() == null) {
    				lastProcessedPoKeyByZone.put(zoneName, new Boolean(true));
    				nextKeyByZone.put(zoneName, po.getPOKey());
    			} else {
    				nextKeyByZone.put(zoneName, po.getNextPOKey());
    			}
                if (log.isDebugEnabled()) {
                    log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                            + ") and successfully found the matching breakId=" + po.getPOKey() + " for the ZoneName=" + zoneName);
                }

    			breakIds.add(po.getPOKey());
                breakInfos.add(new BreakInfo(
                        (includeInPoints && (po.getInSignalId() == null || po.getInSignalId().isEmpty())) ? UUIDUtils
                                .getBase64UrlEncodedUUID() : po.getInSignalId(), po.getPOKey(), po
                                .getPlacementsDurationMilliseconds(), po.getMetadata()));
                if (po.getOutSignalId() != null && !po.getOutSignalId().isEmpty()) {
					outSignalIds.add(po.getOutSignalId());
				}

                //Now let's check if variations list exists. If so, process them too.
                List<POInfo> variations = po.getVariations();
                if (variations != null) {
                    for (POInfo poInfo : variations) {
                        breakIds.add(poInfo.getPoKey());
                        breakInfos.add(new BreakInfo((includeInPoints && (poInfo.getInSignalId() == null || poInfo.getInSignalId()
                                .isEmpty())) ? UUIDUtils.getBase64UrlEncodedUUID() : poInfo.getInSignalId(), poInfo.getPoKey(),
                                poInfo.getDuration(), poInfo.getMetadata()));
                    }
                }
    			return;
    		} 
            //missed break means add zero duration
            //breakInfos.add(new BreakInfo(null, 0));
            log.warn("Unused PO - acquisition point : " + acquisitionPointIdentity + " zone name : " + zoneName + " startTime : "
                    + po.getUtcWindowStartTime() + " duration : " + po.getWindowDurationMilliseconds() + " key " + po.getPOKey());
            if (log.isDebugEnabled()) {
                log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                        + ") and Missed Cue for the breakId=" + po.getPOKey() + " for the ZoneName=" + zoneName);
            }
            writePODetailstoAuditLog(acquisitionPointIdentity, po.getPOKey(), CppConstants.SIGNAL_NOQ_EVENT_TYPE, utcWindowEndTime,poisAuditLogVO);
    		prevPo = po;
    		po = dataManager.getPlacementOpportunity(po.getNextPOKey());
    	}
    	
    	if (po == null) {
    		//durations.add(0);
    		poKeyByZone.put(zoneName, null);
            isNPOMap.put(zoneName, true);
            if (log.isDebugEnabled()) {
                log.debug("Input: (AQPT=" + acquisitionPointIdentity + " UTCSignal: " + this.utcSignalTime
                        + ") and no matching breaks found. Exhausted all breaks for the ZoneName=" + zoneName);
            }
    		nextKeyByZone.put(zoneName, prevPo.getPOKey());
    		lastProcessedPoKeyByZone.put(zoneName, new Boolean(true));
    	}
    }

    private void setUtcSignalTime(PlacementOpportunity po, long utcSignalTime) {
        DataManager dataManager = DataManagerFactory.getInstance();

        // going to do a compare-and-set so we need to handle retrying
        int retries = 0;
        while (true) {
            po.setUtcSignalTime(utcSignalTime);
            if (log.isDebugEnabled()) {
                log.debug("Updating signal time on PO \"" + po.getPOKey() + "\" to " + utcSignalTime);
            }
            if (dataManager.casPlacementOpportunity(po)) {
                break;
            }
            retries++;
            if (retries > 10) {
                throw new RuntimeException("Unable to update signal time on PO with key \"" + po.getPOKey()
                        + "\" after multiple retries");
            }
            if (log.isDebugEnabled()) {
                log.debug("Compare-and-set failed while updating signal time on PO \"" + po.getPOKey() + "\", retrying");
            }
            po = dataManager.getPlacementOpportunity(po.getPOKey());

            // the signal time may have been set by another signal processor on a different acquisition point.
            // if it is set now then we don't need to update it anymore, the first signal time is fine.
            if (po.getUtcSignalTime() != null) {
                return;
            }
        }
    }
}
