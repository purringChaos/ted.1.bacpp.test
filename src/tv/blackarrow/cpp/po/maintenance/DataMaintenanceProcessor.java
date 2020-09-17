package tv.blackarrow.cpp.po.maintenance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.utils.CppConstants;

public class DataMaintenanceProcessor {

    private static final Logger LOGGER = LogManager.getLogger(DataMaintenanceProcessor.class);
    private static final String AP_LOCK_PREFIX = "DATA_MAINTENANCE_AP_LOCK_";

    public static void maintainData() {
        // this process gets run frequently. if one of the nodes in the cluster fails 
        // while performing the work, we can just wait until the next run to do the 
        // maintenance work. that means we don't need to retry until we succeed here.
        // we can just try to do it once and move on.
        Map<String, Map<String, String>> acquisitionPointMap = AcquisitionConfigBean.getInstance().getAcquisitionPointMap();
        DataManager dataManager = DataManagerFactory.getInstance();
        for (String acquisitionPointIdentity : acquisitionPointMap.keySet()) {
            try {
                // other nodes in the cluster may be running at the same time so
                // use a lock to make sure only one node works with each ap at one time.
                // note: it's still possible that more than one node may run the 
                // maintenance process for an acquisition point. only the first node
                // to do the maintenance will really do any work though. any other node
                // would only examine the first po in each list and be done.  it would 
                // just add more complexity to put in a mechanism to ensure that only 
                // one node does this work and even with a large amount of data it would 
                // likely save less than 1 second of processing time.
                String lockName = AP_LOCK_PREFIX + acquisitionPointIdentity;
                boolean locked = false;
                try {
                    if (dataManager.lock(lockName, 30)) {
                        locked = true;
                        maintainAcquisitionPointData(acquisitionPointIdentity);
                    } else {
                        LOGGER.debug(()->"Another node appears to be maintaining data for acquisition point " + acquisitionPointIdentity
                                + ", skipping");
                    }
                } finally {
                    if (locked) {
                        dataManager.unlock(lockName);
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.info(()->"Unexpected exception trying to maintain data for acquisition point " + acquisitionPointIdentity, e);
            }
        }
    }

    private static void maintainAcquisitionPointData(String acquisitionPointIdentity) {
		//log.debug("Beginning data maintenance for acquisition point " + acquisitionPointIdentity);
        DataManager dataManager = DataManagerFactory.getInstance();
        SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
        if (cursor == null) {
            // it's possible that the loader just hasn't run yet so skip for now
            return;
        }

        // we're only going to move the signal cursor for expired entries here
        HashMap<String, String> nextPoKeyByZone = cursor.getNextPOKeyByZone();
        HashMap<String, Boolean> lastProcessedPoKeyByZone = cursor.getLastProcessedPOKeyByZone();
        if (lastProcessedPoKeyByZone == null) {
            lastProcessedPoKeyByZone = new HashMap<String, Boolean>();
            cursor.setLastProcessedPOKeyByZone(lastProcessedPoKeyByZone);
        }

        // do the maintenance for each zone po list
        Set<String> zoneSet = nextPoKeyByZone.keySet();
        if (zoneSet != null && !zoneSet.isEmpty()) {
            for (String zoneName : zoneSet) {
                maintainZonePoList(acquisitionPointIdentity, zoneName, nextPoKeyByZone, lastProcessedPoKeyByZone);
            }
        }

        // if another process modifies the cursor while we were running then it either
        // maintained the cursor for us or we'll do it on the next run.  either way
        // let's just move on and not bother retrying immediately.
        if (!dataManager.casSignalProcessorCursor(cursor)) {
            LOGGER.debug(()->"The signal processor cursor for acquisition point " + acquisitionPointIdentity
                    + " was modified by another process during maintenance, skipping");
        }

		//log.debug("Done with data maintenance for acquisition point " + acquisitionPointIdentity);
    }

    private static void maintainZonePoList(String acquisitionPointIdentity, String zoneName,
            final Map<String, String> nextPoKeyByZone, final Map<String, Boolean> lastProcessedPoKeyByZone) {
        DataManager dataManager = DataManagerFactory.getInstance();
        String poKey = nextPoKeyByZone.get(zoneName);

        // we can't expire entries immediately when their window ends because the 
        // clocks may not be synchronized perfectly between all equipment.
        // use an event time that is far enough in the past that we know
        // we won't get a signal for that time still.
        long utcEventTime = System.currentTimeMillis() - CppConfigurationBean.getInstance().getPoBreakReconfirmationWindowInMillis();

        while (poKey != null) {
            PlacementOpportunity po = dataManager.getPlacementOpportunity(poKey);

            // this should never happen and it is really bad if it does
            if (po == null) {
                LOGGER.error("Unable to locate next placement opportunity with key \"" + poKey + "\" for acquisition point "
                        + acquisitionPointIdentity + " zone " + zoneName + " reseting cursor to prevent repeated failures.");

                // rather than let the system remain in this unrecoverable state, we will
                // clear this item from the signal cursor.  the next time the loader runs,
                // it will re-populate this with the next loaded PO. this will result in 
                // missed POs but at least processing will resume eventually.                
                nextPoKeyByZone.put(zoneName, null);
                lastProcessedPoKeyByZone.put(zoneName, Boolean.FALSE);
                break;
            }

            // if the event time is before the window end time then that po is 
            // still active and we are done
            long utcWindowEndTime = po.getUtcWindowStartTime() + po.getWindowDurationMilliseconds();
            if (utcEventTime < utcWindowEndTime) {
                break;
            }

            // only maintain the po if it has not already been processed
            Boolean processed = lastProcessedPoKeyByZone.get(zoneName);
            if ((processed == null) || (!processed)) {
                LOGGER.warn("Unused PO - acquisition point : " + acquisitionPointIdentity + " zone name : " + zoneName
                        + " startTime : " + po.getUtcWindowStartTime() + " duration : " + po.getWindowDurationMilliseconds()
                        + " key " + po.getPOKey());
				writeMaintainenceDetailToAuditLog(acquisitionPointIdentity, po.getPOKey(), CppConstants.SIGNAL_NOQ_EVENT_TYPE, utcWindowEndTime);
            }

            // move to the next po
            poKey = po.getNextPOKey();
            if (poKey != null) {
                nextPoKeyByZone.put(zoneName, poKey);
                lastProcessedPoKeyByZone.put(zoneName, Boolean.FALSE);
            } else {
                // update the last po to make sure it does not expire.  this should not
                // happen unless there is no schedule data for a long period of time.
                // note: we didn't change anything on the po here and we don't care if
                // another process updated the po while we were running and this cas fails.
                dataManager.casPlacementOpportunity(po);
                lastProcessedPoKeyByZone.put(zoneName, Boolean.TRUE);
            }
        }
    }

    private static void writeMaintainenceDetailToAuditLog(String acquisitionPointId, String breakId, String eventType, long utcEventTime) {
        StringBuilder message = new StringBuilder();
        message.append(System.currentTimeMillis()).append(",").append(eventType);
        message.append(",").append(utcEventTime);
        message.append(",").append(acquisitionPointId).append(",").append(breakId == null ? "" : breakId);
        AuditLogger.auditLog(message.toString());
    }

}
