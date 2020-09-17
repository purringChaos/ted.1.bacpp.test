package tv.blackarrow.cpp.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.SignalProcessorCursor;

public abstract class EventProcessor {
    private static final Logger LOGGER = LogManager.getLogger(EventProcessor.class);
    
    protected AcquisitionPoint acquisitionPoint;
    protected long utcSignalTime = -1;

    protected void writePODetailstoAuditLog(String acquisitionPointId, String breakId, String eventType, long utcEventTime, PoisAuditLogVO poisAuditLogVO) {
        StringBuilder message = new StringBuilder();
        final String EMPTY_STRING = "";
        message.append(System.currentTimeMillis()).append(",").append(eventType);
        message.append(",").append(utcEventTime);        
		message.append(",").append(acquisitionPointId).append(",").append(breakId == null ? EMPTY_STRING : breakId);
		message.append(",").append(poisAuditLogVO != null? poisAuditLogVO.getIpAddressOfClient() : EMPTY_STRING);
		message.append(",").append(poisAuditLogVO != null? poisAuditLogVO.getAcquisitionSignalID() : EMPTY_STRING);
        AuditLogger.auditLog(message.toString());
    }
    
    public void updateSignalProcessorCursor(SignalProcessorCursor cursor) {
        DataManager dataManager = DataManagerFactory.getInstance();

        // save references to these in case we need to retry the update
        long lastConfirmedPOUTCTime = cursor.getLastConfirmedPOUTCTime();
        long lastConfirmedBlackoutUTCTime = cursor.getLastConfirmedBlackoutUTCTime();
        String lastConfirmedBlackoutSignalId = cursor.getLastConfirmedBlackoutSignalId();
        HashMap<String, String> originalNextPoKeyByZone = cursor.getNextPOKeyByZone();
        HashMap<String, Boolean> originalLastProcessedPoKeyByZone = cursor.getLastProcessedPOKeyByZone();

        // going to do a compare-and-set so we need to handle retrying
        int retries = 0;
        while (true) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updating signal processor cursor for acquisition point \"" + cursor.getAcquisitionPointIdentity());
            }
            if (dataManager.casSignalProcessorCursor(cursor)) {
                break;
            }
            retries++;
            if (retries > 10) {
                throw new RuntimeException("Unable to update signal processor cursor for acquisition point \""
                        + cursor.getAcquisitionPointIdentity() + " after multiple retries");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Compare-and-set failed while updating signal processor cursor for acquisition point \""
                        + cursor.getAcquisitionPointIdentity() + ", retrying");
            }
            cursor = dataManager.getSignalProcessorCursor(acquisitionPoint.getAcquisitionPointIdentity());
            cursor.setLastConfirmedPOUTCTime(lastConfirmedPOUTCTime);
            cursor.setLastConfirmedBlackoutUTCTime(lastConfirmedBlackoutUTCTime);
            cursor.setLastConfirmedBlackoutSignalId(lastConfirmedBlackoutSignalId);
            HashMap<String, String> nextPoKeyByZone = cursor.getNextPOKeyByZone();
            HashMap<String, Boolean> lastProcessedPoKeyByZone = cursor.getLastProcessedPOKeyByZone();
            if (lastProcessedPoKeyByZone == null) {
                lastProcessedPoKeyByZone = new HashMap<String, Boolean>();
                cursor.setLastProcessedPOKeyByZone(lastProcessedPoKeyByZone);
            }

            // we don't want to overwrite any new zone entries that the loader might 
            // have added.  however, the signal processor always wins when changing the
            // value of a PO pointer so always overwrite with these new values.
            for (Map.Entry<String, String> entry : originalNextPoKeyByZone.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                nextPoKeyByZone.put(key, value);
                lastProcessedPoKeyByZone.put(key, originalLastProcessedPoKeyByZone.get(key));
            }
        }
    }

}
