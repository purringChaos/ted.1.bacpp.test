package tv.blackarrow.cpp.notifications.upstream.executor.sender.scte224;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.model.scte224.SCTE224EventStatus;
import tv.blackarrow.cpp.notifications.hosted.enums.scte224.NotificationSignalTrigger;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.notifications.upstream.executor.NotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler.MediaRuntimeNotificationsHandler;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class SCTE224NotificationSenderImpl extends NotificationSenderImpl {
	private static final Logger LOG = LogManager.getLogger(SCTE224NotificationSenderImpl.class);
	private static final SCTE224DataManager SCTE224_DATA_MANAGER = DataManagerFactory.getSCTE224DataManager();

	@Override
	public void performSending(NotificationMessage notificationMessage) {
		MediaLedger acquisitionPointMediaLedger = SCTE224_DATA_MANAGER.getAcquisitionPointMediaLedger(notificationMessage.getStreamId(), notificationMessage.getEventSignalId());
		boolean isNotificationNeeded = isNotificationNeeded(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(), notificationMessage.getEventAction(),
				acquisitionPointMediaLedger);
		boolean isNotified = false;
		if (isNotificationNeeded) {
			if (acquisitionPointMediaLedger == null) {
				acquisitionPointMediaLedger = MediaLedger.build(notificationMessage.getStreamId(), notificationMessage.isExecutedAtEncoderLevel());
			}
			isNotified = notifyUpstreamSystem(notificationMessage);
			if (isNotified) {
				//log into Audit files.
				logNotificationEventIntoAuditFiles(notificationMessage);
				//Save Media Transaction in the persistence store.
				saveMediaTransactionIntoPersistenceStore(notificationMessage, acquisitionPointMediaLedger);
				//Save the notification for the SignalStateRequest
				DataManagerFactory.getInstance().putAPConfirmedSignal(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(),
						notificationMessage.getOnFlyCreatedScheduledUpStreamMessage());
			}
			if (!notificationMessage.isBlackoutOverride() && !notificationMessage.isProgramRunoverUnplanned()) {
				List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels = getHostedAppEventStatusNotifyModels(
						notificationMessage, acquisitionPointMediaLedger, isNotified);
				hostedAppEventStatusNotifyModels.stream().forEach(hem -> {
					MediaRuntimeNotificationsHandler.notify224EventStatusToHostedImmediately(hem);
				});
			}
		}
	}

	private boolean isNotificationNeeded(final String streamId, final String eventSignalId, final EventAction eventAction, final MediaLedger acquisitionPointMediaLedger) {
		boolean hasMediaEndNotificationSent = false;
		boolean hasMediaStartNotificationSent = false;
		if (acquisitionPointMediaLedger != null) {
			hasMediaEndNotificationSent = acquisitionPointMediaLedger.isMediaEndNotificationSent();
			hasMediaStartNotificationSent = acquisitionPointMediaLedger.isMediaStartNotificationSent();
		}
		/*
		This UPstream notification could only go in SCTE-224 flow, when UI/224 API update happens. There is no runtime signaling that could cause it
		If it is ever received then send it.(This fixes the issue that in OOH update notification was not sent)
		*/
		if((EventAction.COMPLETE.equals(eventAction) || EventAction.STOP_NOW.equals(eventAction) ) && acquisitionPointMediaLedger.isExecutedAtEncoderLevel()) {
			return true;
		}		
		
		if (hasMediaEndNotificationSent) {
			LOG.info("The event with signal id " + eventSignalId + " has already ended, so skipping the out of band notification this Stream: " + streamId);
			return Boolean.FALSE;
		} else if (EventAction.CONFIRMED.equals(eventAction) && hasMediaStartNotificationSent) {
			LOG.info("The event with signal id " + eventSignalId + " has already started, so skipping the notification to this Stream: " + streamId);
			return Boolean.FALSE;
		} else if ((EventAction.COMPLETE.equals(eventAction) || EventAction.STOP_NOW.equals(eventAction)) && !hasMediaStartNotificationSent) {
			LOG.info(() -> "The event with signal id " + eventSignalId + " has not started, so skipping sending an out of band program end notification to this Stream: "
					+ streamId);
			return Boolean.FALSE;
		} else if ((EventAction.COMPLETE.equals(eventAction) || EventAction.STOP_NOW.equals(eventAction)) && hasMediaEndNotificationSent) {
			LOG.info(() -> "The event with signal id " + eventSignalId + " has already ended, so skipping the out of band program end notification to this Stream: " + streamId);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	/**
	 * @param notificationMessage
	 */
	private void logNotificationEventIntoAuditFiles(final NotificationMessage notificationMessage) {
		//Log Into message Audit Log.
		final PoisAuditLogVO poisAuditLogVO = new PoisAuditLogVO();
		poisAuditLogVO.setIpAddressOfClient("NA");
		poisAuditLogVO.setAltEventId(notificationMessage.getEventId());
		if (notificationMessage.getAcquisitionSignalIds() != null && !notificationMessage.getAcquisitionSignalIds().isEmpty()) {
			poisAuditLogVO.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().values().iterator().next());
		}

		if (EventAction.CONFIRMED.equals(notificationMessage.getEventAction())) {
			if (notificationMessage.getAcquisitionSignalIds() != null) {
				poisAuditLogVO.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
			}

			//1.    Log into message Audit Log.
			AuditLogger.auditMessage(notificationMessage.getOnFlyCreatedScheduledUpStreamMessage(), poisAuditLogVO);

			//2(a). Log Program Start into transaction Audit Log.			
			AuditLogger.auditLogConfirmedBlackoutEvent(notificationMessage.getStreamId(), notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getEventSignalId(),
					poisAuditLogVO);

			//2(b). For Encoder Level Blackout, requirement is to log both program start and end transactions at the same time.
			if (notificationMessage.isExecutedAtEncoderLevel() && notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END) != null
					&& notificationMessage.getEventSignalUTCStopTime() > 0) {
				poisAuditLogVO.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END));
				AuditLogger.auditLogCompleteBlackoutEvent(notificationMessage.getStreamId(), notificationMessage.getEventSignalUTCStopTime(),
						notificationMessage.getEventSignalId(), poisAuditLogVO);
			}
		} else if (EventAction.COMPLETE.equals(notificationMessage.getEventAction()) || EventAction.STOP_NOW.equals(notificationMessage.getEventAction())) {
			//Log Into message Audit Log. For Encoder Level Blackouts Log the Program End acquisition signal id in the audit log.
			if (notificationMessage.isExecutedAtEncoderLevel() && notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END) != null) {
				poisAuditLogVO.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END));
			}
			//1.    Log into message Audit Log.		   
			AuditLogger.auditMessage(notificationMessage.getOnFlyCreatedScheduledUpStreamMessage(), poisAuditLogVO);
			//2. Log into transaction Audit Log.	
			AuditLogger.auditLogCompleteBlackoutEvent(notificationMessage.getStreamId(), notificationMessage.getEventSignalUTCStopTime(), notificationMessage.getEventSignalId(),
					poisAuditLogVO);
		} else {
			//Log Into message Audit Log.
			AuditLogger.auditMessage(notificationMessage.getOnFlyCreatedScheduledUpStreamMessage(), poisAuditLogVO);
			//2. Log into transaction Audit Log.	
			AuditLogger.auditLogConfirmedBlackoutEvent(notificationMessage.getStreamId(), notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getEventSignalId(),
					poisAuditLogVO);
		}
	}

	/**
	 * @param notificationMessage
	 * @param acquisitionPointMediaLedger
	 */
	private void saveMediaTransactionIntoPersistenceStore(final NotificationMessage notificationMessage, final MediaLedger acquisitionPointMediaLedger) {
		//Save the media transaction in persistence store.
		final List<MediaTransaction> mediaTransactions = getMediaTransactions(notificationMessage, acquisitionPointMediaLedger);
		if (mediaTransactions != null) {
			LOG.info(() -> "Saving event transaction in the persistence store after the notification.");
			/*	Media media = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(notificationMessage.getFeedExtRef(), notificationMessage.getEventSignalId());
			List<MediaPoint> mpList = media.getMediaPoints().stream().filter(mp -> mp.getSignalId().equals(notificationMessage.getMediaPointSignalId())).collect(Collectors.toList());*/

			for (MediaTransaction transaction : mediaTransactions) {
				////At this point send there is no start/end based on content ID on upstream. So we may pass null in mediapoint argument. Later we could think of mpList.iterator().next());
				//The second argument is useful only if inband contentID Start/End flow is needed. At this time no requirement for such upstream notifictaion.
				acquisitionPointMediaLedger.addMediaTransaction(transaction, null);
				if (SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId()) {
					acquisitionPointMediaLedger.setMediaStartNotificationSent(true);
				} else if (SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getSignalSegmentTypeId()) {
					acquisitionPointMediaLedger.setMediaEndNotificationSent(true);
				}

			}

			SCTE224_DATA_MANAGER.saveAcquisitionPointMediaLedger(acquisitionPointMediaLedger, notificationMessage.getStreamId(), notificationMessage.getEventSignalId());
			LOG.info(() -> "Saving event transaction in the persistence store after the notification completed.");
		}
	}

	
	private List<HostedAppEventStatusScte224NotifyModel> getHostedAppEventStatusNotifyModels(
			final NotificationMessage notificationMessage, final MediaLedger acquisitionPointMediaLedger,
			boolean isNotified) {
		List<HostedAppEventStatusScte224NotifyModel> hostedAppEvents = new ArrayList<>();
		HostedAppEventStatusScte224NotifyModel scheduledHostedMessage = getHostedAppEventStatusNotifyModel(
				notificationMessage, acquisitionPointMediaLedger, isNotified);
		hostedAppEvents.add(scheduledHostedMessage);

		// For encoder level blackouts we need to send signal for program end as well.
		if (notificationMessage.isExecutedAtEncoderLevel()
				&& notificationMessage.getProgramEndMediaPointSignalId() != null) {
			scheduledHostedMessage.setEventSignalId(notificationMessage.getProgramEndMediaPointSignalId());
			scheduledHostedMessage = getHostedAppEventStatusNotifyModel(notificationMessage,
					acquisitionPointMediaLedger, isNotified);
			scheduledHostedMessage.setSignalTime(scheduledHostedMessage.getSignalTime());
			scheduledHostedMessage.setMediaPointSignalId(notificationMessage.getProgramEndMediaPointSignalId());
			hostedAppEvents.add(scheduledHostedMessage);
		}
		return hostedAppEvents;
	}
	
	/**
	 * @param notificationMessage
	 * @param acquisitionPointMediaLedger
	 * @param isNotified
	 * @return
	 */
	private HostedAppEventStatusScte224NotifyModel getHostedAppEventStatusNotifyModel(final NotificationMessage notificationMessage, final MediaLedger acquisitionPointMediaLedger,
			boolean isNotified) {
		
		HostedAppEventStatusScte224NotifyModel scheduledHostedMessage = new HostedAppEventStatusScte224NotifyModel();
		if (!isNotified) {
			scheduledHostedMessage.setEventStatus(SCTE224EventStatus.ERROR);
		} else {
			//For encoder level blackouts we send the notification only at the time of confirmation action. Only other time when it will come here for
			// encoder level blackouts is 
			//1. If the Media Event's end time was updated in that case hosted needs a notification with "Confirmed" status.
			//2. If the Media was deleted(then we try to end immediately) in that case hosted needs a notification with "Completed" status.
			// Although in both the above case we send a notification for program end notification to the upstream.
			if (notificationMessage.getEventAction() != null) {
				switch (notificationMessage.getEventAction()) {
				case STOP_NOW:
					scheduledHostedMessage.setEventStatus(SCTE224EventStatus.STOP_NOW);
					break;
				default:
					scheduledHostedMessage.setEventStatus(SCTE224EventStatus.CONFIRMED);
					break;
				}
			}
			
			setSignalTrigger(notificationMessage, scheduledHostedMessage);		
		}

		scheduledHostedMessage.setStreamId(notificationMessage.getStreamId());
		scheduledHostedMessage.setEventSignalId(notificationMessage.getEventSignalId());
		scheduledHostedMessage.setEventType(notificationMessage.getEventType());
		scheduledHostedMessage.setMediaPointSignalId(notificationMessage.getMediaPointSignalId());
		scheduledHostedMessage.setSignalTime(notificationMessage.getSignalTime());

		return scheduledHostedMessage;
	}

	private void setSignalTrigger(final NotificationMessage notificationMessage, HostedAppEventStatusScte224NotifyModel scheduledHostedMessage) {
		EventAction eventAction = notificationMessage.getEventAction();
		if (EventAction.CONFIRMED.name().equals(eventAction.name()) || EventAction.COMPLETE.name().equals(eventAction.name())) {
			scheduledHostedMessage.setNotificationSignalTrigger(NotificationSignalTrigger.TIME);
			if (EventAction.COMPLETE.name().equals(eventAction.name()) && notificationMessage.getContentDuration() > 0) {
				scheduledHostedMessage.setNotificationSignalTrigger(NotificationSignalTrigger.DURATION);
			}
		} else if (EventAction.STOP_NOW.name().equals(eventAction.name())) {
			scheduledHostedMessage.setNotificationSignalTrigger(NotificationSignalTrigger.MANUAL);
		}
	}
	
	/**
	 * @param notificationMessage
	 * @param acquisitionPointMediaLedger 
	 */
	private List<MediaTransaction> getMediaTransactions(final NotificationMessage notificationMessage, MediaLedger acquisitionPointMediaLedger) {
		List<MediaTransaction> mediaTransactions = new ArrayList<>();
		if (EventAction.CONFIRMED.equals(notificationMessage.getEventAction())) {
			mediaTransactions.add(MediaTransaction.build(notificationMessage.getEventSignalUTCStartTime(), SegmentType.PROGRAM_START.getSegmentTypeId(),
					notificationMessage.getEventSignalUTCStopTime() > 0 ? notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime() : 0,
					ESAMHelper.generateUpidString(notificationMessage.getEventSignalId()), ESSRequestType.SCC));
			if (notificationMessage.getAcquisitionSignalIds() != null && notificationMessage.getAcquisitionSignalIds().containsKey(SegmentType.PROGRAM_END)) {
				mediaTransactions.add(MediaTransaction.build(notificationMessage.getEventSignalUTCStopTime(), SegmentType.PROGRAM_END.getSegmentTypeId(),
						notificationMessage.getEventSignalUTCStopTime() > 0 ? notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime()
								: 0,
						ESAMHelper.generateUpidString(notificationMessage.getEventSignalId()), ESSRequestType.SCC));
				//Add the corresponding acquisition signal id in the media ledger.
				acquisitionPointMediaLedger.addAcquisitionSignalId(SegmentType.PROGRAM_END,
						notificationMessage.getAcquisitionSignalIds() == null ? null : notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END));
			}
			//Add the corresponding acquisition signal id in the media ledger.
			if (notificationMessage.getAcquisitionSignalIds() != null) {
				if (notificationMessage.getAcquisitionSignalIds().containsKey(SegmentType.PROGRAM_START)) {
					acquisitionPointMediaLedger.addAcquisitionSignalId(SegmentType.PROGRAM_START, notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
				}
				if (notificationMessage.getAcquisitionSignalIds().containsKey(SegmentType.PROGRAM_END)) {
					acquisitionPointMediaLedger.addAcquisitionSignalId(SegmentType.PROGRAM_END, notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END));
				}
				if (notificationMessage.getAcquisitionSignalIds().containsKey(SegmentType.CONTENT_IDENTIFICATION)) {
					acquisitionPointMediaLedger.addAcquisitionSignalId(SegmentType.CONTENT_IDENTIFICATION,
							notificationMessage.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION));
				}
			}
		} else if (EventAction.COMPLETE.equals(notificationMessage.getEventAction()) || EventAction.STOP_NOW.equals(notificationMessage.getEventAction())) {
			mediaTransactions.add(MediaTransaction.build(notificationMessage.getEventSignalUTCStopTime(), SegmentType.PROGRAM_END.getSegmentTypeId(),
					notificationMessage.getEventSignalUTCStopTime() > 0 ? notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime() : 0,
					ESAMHelper.generateUpidString(notificationMessage.getEventSignalId()), ESSRequestType.SCC));
		} else if (EventAction.UPDATE.equals(notificationMessage.getEventAction())) {
			mediaTransactions.add(MediaTransaction.build(notificationMessage.getEventSignalUTCStopTime(), SegmentType.PROGRAM_RUNOVER_UNPLANNED.getSegmentTypeId(),
					notificationMessage.getEventSignalUTCStopTime() > 0 ? notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime() : 0,
					ESAMHelper.generateUpidString(notificationMessage.getEventSignalId()), ESSRequestType.SCC));
		}
		return mediaTransactions;
	}
	
}
