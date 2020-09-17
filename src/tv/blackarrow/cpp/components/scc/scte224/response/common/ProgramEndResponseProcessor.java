package tv.blackarrow.cpp.components.scc.scte224.response.common;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;

public abstract class ProgramEndResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramEndResponseProcessor.class);

	/*
	 * Take it from EndTime-StartTime
	 * 
	 */
	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short segmentTypeId) {
		Long timeInMillis = 0l;
		
		if (matchedMediaLedger != null && matchedMediaLedger.getMediaTransactions() != null && !matchedMediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programStartOrOverlapTransaction = matchedMediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			if (programStartOrOverlapTransaction != null) {
				timeInMillis = signalTime - programStartOrOverlapTransaction.getSignalTimeInMS();
			}
		}
		return timeInMillis;
	}

	@Override
	public Long getDurationFromLedger(MediaLedger mediaLedger) {
		Long timeInMillis = null;
		if (mediaLedger != null && mediaLedger.getMediaTransactions() != null && !mediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programEndTransaction = mediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
			if (programEndTransaction != null) {
				timeInMillis = programEndTransaction.getTotalDurationInMS();
			}
		}
		return timeInMillis;
	}

	@Override
	//TO DO Avnee
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, MediaLedger mediaLedger) {
		Long timeInMillis = null;
		if (mediaLedger != null && mediaLedger.getMediaTransactions() != null && !mediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programEndTransaction = mediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
			if (programEndTransaction != null) {
				timeInMillis = programEndTransaction.getSignalTimeInMS();
			}
		}
		return timeInMillis;
	}
	
	@Override
	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration, MediaLedger mediaLedger, AcquisitionPoint aqpt) {
		//Same as Start Time Above
		return getStartTimeFromLedgerForEventSchedule(aqpt, mediaLedger);
	}

	protected void logCompleteEventInAuditLog(SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt, MuleEventContext context, Date requestTime) {
		//Log in Audit Log as we are confirming this.
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notificationResponse, matchedMediaLedger.getSignalId());
		AuditLogger.auditLogCompleteBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), matchedMediaLedger.getSignalId(), poisAuditLogVO);
	}
}
