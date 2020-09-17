package tv.blackarrow.cpp.components.scc.scte224.response.common;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
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

public abstract class ProgramStartResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramStartResponseProcessor.class);

	/*
	 * Take it from Apply
	 */
	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short segmentTypeId) {
		
		Long duration = null;
		if(!matchedMediaPoint.getApply().isEmpty()) {
			duration = matchedMediaPoint.getApply().iterator().next().getDurationInMillis();
		}
		return duration;
	}


	@Override
	public void saveConfirmedSignalIDInContextForSSR(String acquisitionSignalId, String acquisitionPointId, MuleEventContext context) {
		if (matchedMediaObject != null) {
			LOGGER.debug(() -> acquisitionSignalId + "=>" + acquisitionPointId + ", " + matchedMediaObject.getSignalid() + " will be saved for SSR Request.");
			context.getMessage().setProperty(ContextConstants.CONFIRMED_EVENT_SIGNAL_ID, matchedMediaObject.getSignalid(), PropertyScope.OUTBOUND);
		}
	}

	@Override
	public Long getDurationFromLedger(MediaLedger mediaLedger) {
		Long timeInMillis = null;
		if (mediaLedger != null && mediaLedger.getMediaTransactions() != null && !mediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			if (programStartTransaction != null && programStartTransaction.getTotalDurationInMS() != null && programStartTransaction.getTotalDurationInMS() > 0) {
				timeInMillis = programStartTransaction.getTotalDurationInMS();
			}

		}
		return timeInMillis;
	}

	@Override
	//TO DO Avnee
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, MediaLedger mediaLedger) {
		Long timeInMillis = null;
		if (mediaLedger != null && mediaLedger.getMediaTransactions() != null && !mediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			if (programStartTransaction != null) {
				timeInMillis = programStartTransaction.getSignalTimeInMS();
				timeInMillis = timeInMillis + (aqpt.getContentIDFrequency() * 1000); // need add interval
			}
		}
		return timeInMillis;
	}
	
	@Override
	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration, MediaLedger mediaLedger, AcquisitionPoint aqpt) {
		long originalRequestSignalTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		originalRequestSignalTime = originalRequestSignalTime + duration;
		return originalRequestSignalTime;
	}
	
	protected void logConfirmEventInAuditLog(SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt, MuleEventContext context, Date requestTime) {
		//Log in Audit Log as we are confirming this.
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notificationResponse, matchedMediaLedger.getSignalId());
		AuditLogger.auditLogConfirmedBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), matchedMediaLedger.getSignalId(), poisAuditLogVO);
	}

}
