package tv.blackarrow.cpp.log;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import tv.blackarrow.cpp.log.model.ALEssAuditLogVO;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.utils.CppConstants;

/**
 * This utility class is used by the Linear POIS runtime to log the POIS audit
 * logs and audit SCC and MCC messages
 *
 * @author mchakkarapani
 */
public class AuditLogger {

    private static final String EMPTY_STRING = "";
    private static final String MISSING_VALUE = "_";

	protected final static Logger LOGGER = LogManager.getLogger(AuditLogger.class);

	// The names of the log4j loggers, should be defined in log4j2.xml
    // file.
    public final static String AUDIT_LOGGER = "AuditLog";
    public final static String MESSAGE_LOGGER = "MessageLog";
    public final static String PRETRIGGER_LOGGER = "PretriggerLog";
    public final static String NORMALIZE_LOGGER = "NormalizeLog";
    public final static String AUDIT_VERSION = "1";

    public final static String VERSION_PREFIX = "v";

    private final static Logger auditLog = LogManager.getLogger(AUDIT_LOGGER);
    private final static Logger messageLog = LogManager.getLogger(MESSAGE_LOGGER);
    private final static Logger pretriggerLog = LogManager.getLogger(PRETRIGGER_LOGGER);
    private final static Logger normalizeLog = LogManager.getLogger(NORMALIZE_LOGGER);

    private final static String LOG_DELIMITER = ":";
    private static final String AUDIT_MESSAGE_DELIMITER = "+++++";

    /**
     * This method logs the audit log of all POIS placement decisions.
     *
     * @param message
     */
    public static void auditLog(String message) {
        if (auditLog != null) {
            auditLog.info(()->VERSION_PREFIX + AUDIT_VERSION + LOG_DELIMITER + message);
        } else {
            LOGGER.warn(()->"No Audit Logger found, logger=>" + AUDIT_LOGGER);
        }
    }

    /** audit log blackout event decisions
     *
     * @param acquisitionPointId acquisition point id
     * @param utcEventTime signal utc time
     * @param eventType event type BCE for confirmed blackout even, or BNE for no blackout event
     * @param signalId signal id being confirmed.
     * @param poisAuditLogVO
     */
    static private  void auditLogBlackoutEvent(String acquisitionPointId, long utcEventTime, String eventType, String signalId, PoisAuditLogVO poisAuditLogVO) {
        StringBuilder message = new StringBuilder();
        message.append(System.currentTimeMillis()).append(",").append(eventType);
        message.append(",").append(utcEventTime);
        message.append(",").append(acquisitionPointId).append(",").append(signalId == null ? EMPTY_STRING : signalId);
		message.append(",").append(poisAuditLogVO != null ? poisAuditLogVO.getIpAddressOfClient() : EMPTY_STRING);
		message.append(",").append(poisAuditLogVO != null ? poisAuditLogVO.getAcquisitionSignalID() : EMPTY_STRING);
		message.append(",").append(poisAuditLogVO != null && poisAuditLogVO.getAltEventId() != null ? poisAuditLogVO.getAltEventId() : EMPTY_STRING);
        AuditLogger.auditLog(message.toString());
    }


    /** audit log confirmed blackout event
     *
     * @param acquisitionPointId acquisition point id
     * @param utcEventTime signal utc time
     * @param signalId signal id being confirmed.
     * @param poisAuditLogVO
     */
	static public void auditLogConfirmedBlackoutEvent(String acquisitionPointId, long utcEventTime, String signalId, PoisAuditLogVO poisAuditLogVO) {
		auditLogBlackoutEvent(acquisitionPointId, utcEventTime, CppConstants.BLACKOUT_CONFIRMED_EVENT_TYPE, signalId, poisAuditLogVO);
	}
	
    /** audit log COMPLETE blackout event
    *
    * @param acquisitionPointId acquisition point id
    * @param utcEventTime signal utc time
    * @param signalId signal id being confirmed.
    * @param poisAuditLogVO
    */
	static public void auditLogCompleteBlackoutEvent(String acquisitionPointId, long utcEventTime, String signalId, PoisAuditLogVO poisAuditLogVO) {
		auditLogBlackoutEvent(acquisitionPointId, utcEventTime, CppConstants.BLACKOUT_COMPLETE_EVENT_TYPE, signalId, poisAuditLogVO);
	}

    /** audit log no blackout event
     *
     * @param acquisitionPointId acquisition point id
     * @param utcEventTime signal utc time
     * @param poisAuditLogVO
     * @param signalId signal id being confirmed.
     */
	static public void auditLogNoBlackoutEvent(String acquisitionPointId, long utcEventTime, PoisAuditLogVO poisAuditLogVO) {
		auditLogBlackoutEvent(acquisitionPointId, utcEventTime, CppConstants.BLACKOUT_NO_EVENT_TYPE, null, poisAuditLogVO);
	}

    /**
     * This method logs the message request and response log of SCC and MCC.
     *
     * @param trackingId
     */
    public static void auditMessage(String message, PoisAuditLogVO poisAuditLogVO) {
        if (messageLog != null) {
        	StringBuilder finalMessage = new StringBuilder(String.valueOf(System.currentTimeMillis()))
    		.append(AUDIT_MESSAGE_DELIMITER).append(poisAuditLogVO != null ? poisAuditLogVO.getIpAddressOfClient() : MISSING_VALUE)
    		.append(AUDIT_MESSAGE_DELIMITER).append(poisAuditLogVO != null ? poisAuditLogVO.getAcquisitionSignalID() : MISSING_VALUE)
    		.append(AUDIT_MESSAGE_DELIMITER).append(message.replace('\n', ' ').replace('\r', ' '));
        	 messageLog.info(()->finalMessage.toString());
        } else {
            LOGGER.warn(()->"No Audit Message Logger found, logger=>" + MESSAGE_LOGGER);
        }
    }

    /**
     * This method logs the message request and response log of SCC and MCC.
     *
     * @param trackingId
     */
    public static void auditMessage(String message, PoisAuditLogVO poisAuditLogVO, String eventId) {
        if (messageLog != null) {
        	StringBuilder finalMessage = new StringBuilder(String.valueOf(System.currentTimeMillis()))
    		.append(AUDIT_MESSAGE_DELIMITER).append(poisAuditLogVO != null ? poisAuditLogVO.getIpAddressOfClient() : MISSING_VALUE)
    		.append(AUDIT_MESSAGE_DELIMITER).append(poisAuditLogVO != null ? poisAuditLogVO.getAcquisitionSignalID() : MISSING_VALUE)
    		.append(AUDIT_MESSAGE_DELIMITER).append(eventId != null ? eventId : MISSING_VALUE)
    		.append(AUDIT_MESSAGE_DELIMITER).append(message.replace('\n', ' ').replace('\r', ' '));
        	 messageLog.info(finalMessage.toString());
        } else {
            LOGGER.warn(()->"No Audit Message Logger found, logger=>" + MESSAGE_LOGGER);
        }
    }

    public static void rollover() {
    		Map<String, Appender> auditAppenderMap = ((org.apache.logging.log4j.core.Logger) auditLog).getAppenders();
    	for(Map.Entry<String, Appender> entry : auditAppenderMap.entrySet()) {
			if(entry.getValue()!=null && entry.getValue() instanceof RollingRandomAccessFileAppender) {
				((RollingRandomAccessFileAppender)entry.getValue()).getManager().rollover();
			}
		}

    	Map<String, Appender> messageAppenderMap = ((org.apache.logging.log4j.core.Logger) messageLog).getAppenders();
    	for(Map.Entry<String, Appender> entry : messageAppenderMap.entrySet()) {
			if(entry.getValue()!=null && entry.getValue() instanceof RollingRandomAccessFileAppender) {
				((RollingRandomAccessFileAppender)entry.getValue()).getManager().rollover();

    		}
    	}
    	
    	Map<String, Appender> pretriggerAppenderMap = ((org.apache.logging.log4j.core.Logger) pretriggerLog).getAppenders();
        for(Map.Entry<String, Appender> entry : pretriggerAppenderMap.entrySet()) {
            if(entry.getValue()!=null && entry.getValue() instanceof RollingRandomAccessFileAppender) {
                ((RollingRandomAccessFileAppender)entry.getValue()).getManager().rollover();

            }
        }
        
        Map<String, Appender> normalizeAppenderMap = ((org.apache.logging.log4j.core.Logger) normalizeLog).getAppenders();
        for(Map.Entry<String, Appender> entry : normalizeAppenderMap.entrySet()) {
            if(entry.getValue()!=null && entry.getValue() instanceof RollingRandomAccessFileAppender) {
                ((RollingRandomAccessFileAppender)entry.getValue()).getManager().rollover();

            }
        }
    }
    
    
    /**
     * This method logs the Pretrigger ESAM message sent to DCM.
     *
     * @param trackingId
     */
    public static void auditPretriggerMessage(String message,ALEssAuditLogVO alEssAuditLogVO,String type) {
      if (pretriggerLog != null) {
        StringBuilder finalMessage = new StringBuilder(String.valueOf(System.currentTimeMillis()))
            .append(AUDIT_MESSAGE_DELIMITER).append(type)
            .append(AUDIT_MESSAGE_DELIMITER).append(alEssAuditLogVO.getUniqueId())
            .append(AUDIT_MESSAGE_DELIMITER).append(message.replace('\n', ' ').replace('\r', ' '));
        pretriggerLog.info(()->finalMessage.toString());
    } else {
        LOGGER.warn(()->"No Audit Message Logger found, logger=>" + PRETRIGGER_LOGGER);
    }
   }
    
    /**
     * This method logs the Pretrigger ESAM message sent to DCM.
     *
     * @param trackingId
     */
    public static void auditNormalizeMessage(String message,ALEssAuditLogVO alEssAuditLogVO,String type) {
      if (normalizeLog != null) {
        StringBuilder finalMessage = new StringBuilder(String.valueOf(System.currentTimeMillis()))
        .append(AUDIT_MESSAGE_DELIMITER).append(type)
        .append(AUDIT_MESSAGE_DELIMITER).append(alEssAuditLogVO.getIpAddressOfClient())
        .append(AUDIT_MESSAGE_DELIMITER).append(alEssAuditLogVO.getUniqueId())
        .append(AUDIT_MESSAGE_DELIMITER).append(message.replace('\n', ' ').replace('\r', ' '));
        normalizeLog.info(()->finalMessage.toString());
    } else {
        LOGGER.warn(()->"No Audit Message Logger found, logger=>" + NORMALIZE_LOGGER);
    }
    }

}
