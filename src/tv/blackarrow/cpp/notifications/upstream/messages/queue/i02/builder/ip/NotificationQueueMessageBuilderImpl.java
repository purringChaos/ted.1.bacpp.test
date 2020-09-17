package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.ip;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.BaseQueueNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.EventAction;

public class NotificationQueueMessageBuilderImpl extends BaseQueueNotificationMessageBuilderImpl {
	private static final Logger LOG = LogManager.getLogger(NotificationQueueMessageBuilderImpl.class);

	@Override
	public void buildForPendingEventsUpstreamNotification(final long currentSystemTimeInSecs, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime) {

		if (!immediateStart && acquisitionPoint.isInBand()) {
			return;
		}
		//0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
		long startNotificationTimeInSecs = getStartNotificationTimeOfBlackoutWithBuffer(blackout, acquisitionPoint, currentSystemTimeInSecs, immediateStart);
		if (startNotificationTimeInSecs < currentSystemTimeInSecs) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s start time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}
		//0. Prepare the notification that goes at the program start time.
		addStartNotificationMessage(immediateStart, blackout, notificationMessagesByScheduleTime, acquisitionPoint, startNotificationTimeInSecs);

	}

	@Override
	public void buildForLiveEventsUpstreamNotification(long currentSystemTime, boolean immediateSend, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, EventAction action) {
		//We will send immediate Update Notification
		//0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
		long updateNotificationTimeImmediately = getStopNotificationTimeOfBlackoutWithBuffer(blackout, acquisitionPoint, currentSystemTime, true);
		if (updateNotificationTimeImmediately < currentSystemTime) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s start time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}
		//1. Prepare the notification that goes at the program end time to all IP AQs Inband/OOB.
		ConfirmedPlacementOpportunity aqCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(acquisitionPoint.getAcquisitionPointIdentity(), blackout.getSignalId());
		if(aqCPO==null) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s has never started on acquisition point, hence "
					+ "skipping the upsate notifications sending for this Blackout.");
			return;
		}
		
		//Sending immediateSend to False in below function, because we do not want to change the actual End time
		addUpdateNotificationMessage(immediateSend, blackout, notificationMessagesByScheduleTime, acquisitionPoint, updateNotificationTimeImmediately, action, aqCPO);
	}
	
	@Override
	public void buildForHostedNotifications(final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout, final int maxProgramStartBufferInMillis, 
			final Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages, boolean immediateStart, int currentSystemTimeInSecs) {
		long immediateNotificationTime = notifyTimeInImmediateCaseWithAddedBuffer(acquisitionPoint, currentSystemTimeInSecs);//When the notifications were scheduled to upstream
		long scheduledTimeForStartErrorCheck = immediateStart
				? TimeUnit.MILLISECONDS.toSeconds(immediateEventStartOrEndValueInBlackout(acquisitionPoint, immediateNotificationTime) + maxProgramStartBufferInMillis)
				: TimeUnit.MILLISECONDS.toSeconds(blackout.getUtcStartTime() + maxProgramStartBufferInMillis);
		long scheduledTimeForCompleteErrorCheck = TimeUnit.MILLISECONDS.toSeconds(blackout.getUtcStopTime() + maxProgramStartBufferInMillis);

		HostedAppEventStatusI02NotifyModel startErrorCheckModel = new HostedAppEventStatusI02NotifyModel(blackout.getFeedExtRef(), blackout.getEventTypeName(),
				blackout.getUtcStartTime(), blackout.getUtcStopTime(), blackout.getSignalId(), null, blackout.getEventId());

		startErrorCheckModel.setEventAction(EventAction.CONFIRMED);
		startErrorCheckModel.setEventType(EventType.I02);
		startErrorCheckModel.setHostedNotificationScheduleTime(scheduledTimeForStartErrorCheck);

		HostedAppEventStatusI02NotifyModel completeErrorCheckModel = new HostedAppEventStatusI02NotifyModel(blackout.getFeedExtRef(), blackout.getEventTypeName(),
				blackout.getUtcStartTime(), blackout.getUtcStopTime(), blackout.getSignalId(), null, blackout.getEventId());

		completeErrorCheckModel.setEventAction(EventAction.COMPLETE);
		completeErrorCheckModel.setEventType(EventType.I02);
		completeErrorCheckModel.setHostedNotificationScheduleTime(scheduledTimeForCompleteErrorCheck);

		hostedNotificationQueueMessages.put(
				blackout.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + "ERROR_CHECK" + NotificationServiceConfig.JOB_NAME_SEPARATOR + EventAction.CONFIRMED,
				startErrorCheckModel);
		hostedNotificationQueueMessages.put(
				blackout.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + "ERROR_CHECK" + NotificationServiceConfig.JOB_NAME_SEPARATOR + EventAction.COMPLETE,
				completeErrorCheckModel);
	}
	
	@Override
	public void buildForLiveEventHostedNotification(BlackoutEvent blackout, int maxProgramStartBufferOfAllAQ,
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages, AcquisitionPoint aqpt) {
		ConfirmedPlacementOpportunity originalEventCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), blackout.getSignalId());
		long actualBlackoutStopTime = BlackoutEvent.getActualBlackoutStopTime(originalEventCPO, blackout);
		long scheduledTimeForCompleteErrorCheck = TimeUnit.MILLISECONDS.toSeconds(actualBlackoutStopTime + maxProgramStartBufferOfAllAQ);
		HostedAppEventStatusI02NotifyModel completeErrorCheckModel = new HostedAppEventStatusI02NotifyModel(blackout.getFeedExtRef(), blackout.getEventTypeName(),
				blackout.getUtcStartTime(), actualBlackoutStopTime, blackout.getSignalId(), null, blackout.getEventId());

		completeErrorCheckModel.setEventAction(EventAction.CONFIRMED);
		completeErrorCheckModel.setEventType(EventType.I02);
		completeErrorCheckModel.setHostedNotificationScheduleTime(scheduledTimeForCompleteErrorCheck);

		hostedNotificationQueueMessages.put(
				blackout.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + "ERROR_CHECK" + NotificationServiceConfig.JOB_NAME_SEPARATOR + EventAction.COMPLETE,
				completeErrorCheckModel);
	}
	
	protected void addUpdateNotificationMessage(boolean immediateStop, BlackoutEvent blackout, final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime,
			AcquisitionPoint acquisitionPoint, long stopNotificationTimeInSecs, EventAction action, ConfirmedPlacementOpportunity aqCPO) {
		final NotificationMessage notificationAtEventUpdate = defaultEndOrUpdateEventNotificationMessage(action, acquisitionPoint, blackout, immediateStop, stopNotificationTimeInSecs, aqCPO);
		addMessageInNotificationByScheduledTimeMap(notificationMessagesByScheduleTime, notificationAtEventUpdate);
	}
}
