package tv.blackarrow.cpp.notifications.upstream.executor.sender.i02;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.notifications.upstream.executor.NotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.utils.UUIDUtils;

public abstract class I02NotificationSenderImpl extends NotificationSenderImpl {
	private static final Logger LOG = LogManager.getLogger(I02NotificationSenderImpl.class);
	protected static final String SEPARATOR = "#@#";
	protected boolean isNotified=false;

	@Override
	public void performSending(NotificationMessage notificationMessage) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		String hostname = System.getenv("hostname");
		try {
			vo.setIpAddressOfClient(StringUtils.isNotBlank(hostname) ? hostname : InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			LOG.info(()->"Hostname will not be logged in POISAudit File");
		}

		if(notificationMessage.getAcquisitionSignalIds() != null && notificationMessage.getAcquisitionSignalIds().values() != null
				&& !notificationMessage.getAcquisitionSignalIds().values().isEmpty()) {
			vo.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().values().iterator().next());
		}

		AuditLogger.auditMessage(notificationMessage.getOnFlyCreatedScheduledUpStreamMessage(), vo, notificationMessage.getEventId());

		isNotified = notifyUpstreamSystem(notificationMessage);
		//If we have confirmed then create AQCPO
		if (isNotified && notificationMessage.getEventAction()!=null) {
			ConfirmedPlacementOpportunity acquisitionPointCPO = null;
			switch (notificationMessage.getEventAction()) {
			case CONFIRMED://Create First Time aqCPO
				DATAMANAGER.putAPConfirmedSignal(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(),
						notificationMessage.getOnFlyCreatedScheduledUpStreamMessage());
				createConfirmedBlackout(notificationMessage);
				break;
			case STOP_NOW://Update Abort Information in aqCPO
				acquisitionPointCPO = DATAMANAGER.getConfirmedBlackoutForGivenAP(notificationMessage.getStreamId(), notificationMessage.getEventSignalId());
				acquisitionPointCPO.setAbortTime(notificationMessage.getEventSignalUTCStopTime());
				acquisitionPointCPO.setAbortedViaESNIOrUI(true);
				DATAMANAGER.putConfirmedBlackout(acquisitionPointCPO);
				break;
			case UPDATE://Update the new duration in aqCPO
				acquisitionPointCPO = DATAMANAGER.getConfirmedBlackoutForGivenAP(notificationMessage.getStreamId(), notificationMessage.getEventSignalId());
				if (notificationMessage != null && acquisitionPointCPO != null && acquisitionPointCPO.getBreakInfos() != null && !acquisitionPointCPO.getBreakInfos().isEmpty()) {
					acquisitionPointCPO.getBreakInfos().get(0).setDuration((int) notificationMessage.getContentDuration());
					DATAMANAGER.putConfirmedBlackout(acquisitionPointCPO);
					DATAMANAGER.putAPConfirmedSignal(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(),
							notificationMessage.getOnFlyCreatedScheduledUpStreamMessage());
				} else {
					LOG.info(() -> " The Break info is not present in aqcpo in CBAP for " + " AQ " + notificationMessage.getStreamId() + " and signal id "
							+ notificationMessage.getEventSignalId());
				}
				break;
			default:
				break;
			}
		}

	}

	private ConfirmedPlacementOpportunity createConfirmedBlackout(NotificationMessage notificationMessage) {

		final String signalId = notificationMessage.getEventSignalId();

		// Only create a new CPO and update the signal cursor if this is the first time when this event is being confirmed otherwise
		// only update the duration in the CPO. 
		ConfirmedPlacementOpportunity acquisitionPointConfirmedBlackout = DATAMANAGER.getConfirmedBlackoutForGivenAP(notificationMessage.getStreamId(), signalId);
		if (acquisitionPointConfirmedBlackout == null) {
			acquisitionPointConfirmedBlackout = new ConfirmedPlacementOpportunity();
			acquisitionPointConfirmedBlackout.setAcquisitionPointIdentity(notificationMessage.getStreamId());
			long signalTimeInMillis = TimeUnit.SECONDS.toMillis(notificationMessage.getNotificationScheduledTime());
			acquisitionPointConfirmedBlackout.setUtcSignalTime(signalTimeInMillis);
			acquisitionPointConfirmedBlackout.setSignalId(signalId);
			ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
			BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) (notificationMessage.getContentDuration()));
			breaks.add(breakInfo);
			//Current Branch Pycelle.1===>
			//Please note ActualSTARTTIME and ACTUAL ENDTIME Are saved without signal offset in AQ CPO(verified in ned.1, You may check)		
			acquisitionPointConfirmedBlackout.setActualUtcStartTime(notificationMessage.getEventSignalUTCStartTime());
			acquisitionPointConfirmedBlackout.setBreakInfos(breaks);
			DATAMANAGER.putConfirmedBlackout(acquisitionPointConfirmedBlackout);
		}

		//Update the signal process cursor with the last confirmed blackout time.
		SignalProcessorCursor cursor = DATAMANAGER.getSignalProcessorCursor(notificationMessage.getStreamId());
		if (cursor != null) {
			// going to do a compare-and-set so we need to handle retrying
			int retries = 0;
			while (retries++ < 10) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Updating signal processor cursor for acquisition point \"" + cursor.getAcquisitionPointIdentity());
				}
				cursor.setLastConfirmedBlackoutUTCTime(acquisitionPointConfirmedBlackout.getUtcSignalTime());
				cursor.setLastConfirmedBlackoutSignalId(acquisitionPointConfirmedBlackout.getSignalId());
				if (DATAMANAGER.casSignalProcessorCursor(cursor)) {
					break;
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("Compare-and-set failed while updating signal processor cursor for acquisition point \"" + cursor.getAcquisitionPointIdentity() + ", retrying");
				}
				cursor = DATAMANAGER.getSignalProcessorCursor(notificationMessage.getStreamId());
			}
			if (retries == 10) {
				throw new RuntimeException("Unable to update signal processor cursor for acquisition point \"" + cursor.getAcquisitionPointIdentity() + " after multiple retries");
			}
		}
		return acquisitionPointConfirmedBlackout;
	}

}
