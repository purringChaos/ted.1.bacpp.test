package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.qam;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.BaseQueueNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.EventAction;

public class NotificationQueueMessageBuilderImpl extends BaseQueueNotificationMessageBuilderImpl {
	private static final Logger LOG = LogManager.getLogger(NotificationQueueMessageBuilderImpl.class);

	@Override
	public void buildForPendingEventsUpstreamNotification(final long currentSystemTimeInSecs, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime) {
		long startNotificationTimeInSecs = getStartNotificationTimeOfBlackoutWithBuffer(blackout, acquisitionPoint, currentSystemTimeInSecs,
				immediateStart);
		if (startNotificationTimeInSecs < currentSystemTimeInSecs) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s start time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}
		//1. If the Program End is already in past then no need to schedule anything because this event is already in error state.
		long stopNotificationTimeInSecs = getStopNotificationTimeOfBlackoutWithBuffer(blackout, acquisitionPoint, currentSystemTimeInSecs, false);
		if (stopNotificationTimeInSecs < currentSystemTimeInSecs) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s stop time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}

		if (immediateStart) {
			//0. Prepare the notification that goes at the program start time to all QAM AQ Inband/OOB.
			addStartNotificationMessage(immediateStart, blackout, notificationMessagesByScheduleTime, acquisitionPoint, startNotificationTimeInSecs);
		} else {//set Start Notification only on OOB
			if (acquisitionPoint.isOutBand()) {
				//0. Prepare the notification that goes at the program start time.
				addStartNotificationMessage(immediateStart, blackout, notificationMessagesByScheduleTime, acquisitionPoint, startNotificationTimeInSecs);
				LOG.info(() -> "Scheduled a new Program Start Out of Band Notification job for AP(" + acquisitionPoint.getAcquisitionPointIdentity() + ") Blackout "+ blackout.getEventId() + "(" + blackout.getSignalId() + ")" + " PROGRAM_START: expected time is " + startNotificationTimeInSecs );
			}
		}

		//1. Prepare the notification that goes at the program end time to all QAM AQ Inband/OOB.
		addEndNotificationMessage(false, blackout, notificationMessagesByScheduleTime, acquisitionPoint, stopNotificationTimeInSecs, EventAction.COMPLETE, null);
		LOG.info(() -> "Scheduled a new Switchback job for AP(" + acquisitionPoint.getAcquisitionPointIdentity() + ") Blackout "+ blackout.getEventId() + "(" + blackout.getSignalId() + ")" + " PROGRAM_END: expected time is " + stopNotificationTimeInSecs );
	}

	@Override
	public void buildForLiveEventsUpstreamNotification(long currentSystemTime, boolean immediate, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, EventAction action) {
		long stopNotificationTimeInSecs = getStopNotificationTimeOfBlackoutWithBuffer(blackout, acquisitionPoint, currentSystemTime, immediate);
		if (stopNotificationTimeInSecs < currentSystemTime) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s stop time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}
		ConfirmedPlacementOpportunity aqCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(acquisitionPoint.getAcquisitionPointIdentity(), blackout.getSignalId());
		addEndNotificationMessage(immediate, blackout, notificationMessagesByScheduleTime, acquisitionPoint, stopNotificationTimeInSecs, action, aqCPO);
		LOG.info(() -> "Scheduled a new Switchback job for AP(" + acquisitionPoint.getAcquisitionPointIdentity() + ") Blackout "+ blackout.getEventId() + "(" + blackout.getSignalId() + ")" + " PROGRAM_END: expected time is " + stopNotificationTimeInSecs );
	}
	
	@Override
	public void rebuildForLiveEventsUpstreamNotification(long currentSystemTimeInMillis, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime) {
		ConfirmedPlacementOpportunity acqCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(acquisitionPoint.getAcquisitionPointIdentity(), blackout.getSignalId());
		long actualBlackoutStopTime = BlackoutEvent.getActualBlackoutStopTime(acqCPO, blackout);
		if (actualBlackoutStopTime < TimeUnit.MILLISECONDS.toSeconds(currentSystemTimeInMillis)) {
			LOG.info(() -> "Blackout (" + blackout.getSignalId() + ") " + blackout.getEventId() + "is already expired so skipping any rescheduling for this Blackout.");
			return;
		}
		long stopNotificationTimeInSecs = TimeUnit.MILLISECONDS.toSeconds(actualBlackoutStopTime - (acquisitionPoint.getFeedSCCNotificationBuffer() * 1000));
		if (stopNotificationTimeInSecs < TimeUnit.MILLISECONDS.toSeconds(currentSystemTimeInMillis)) {
			LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId() + ")'s stop time is already expired, "
					+ "so skipping the notifications scheduling for this Blackout.");
			return;
		}
		addEndNotificationMessage(false, blackout, notificationMessagesByScheduleTime, acquisitionPoint, stopNotificationTimeInSecs, EventAction.COMPLETE, acqCPO);
		LOG.info(() -> "Rescheduled a new Switchback job for AP(" + acquisitionPoint.getAcquisitionPointIdentity() + ") Blackout "+ blackout.getEventId() +"(" + blackout.getSignalId() + ")" +  " PROGRAM_END: expected time is " + stopNotificationTimeInSecs );
		
		if(notificationMessagesByScheduleTime!=null && !notificationMessagesByScheduleTime.isEmpty()) {
			notificationMessagesByScheduleTime.values().stream().forEach(map -> {
				if (map != null && !map.isEmpty()) {
					map.values().stream().forEach(notificationMessage -> {
						notificationMessage.setEventSignalUTCStopTime(actualBlackoutStopTime);
						notificationMessage.setContentDuration(notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime());//Since only for this AQ blackout is stopping at this time. Thus changing content duration for it.
					});
				}
			});
		}		
	}
	
	protected void addEndNotificationMessage(boolean immediateStop, BlackoutEvent blackout, final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime,
			AcquisitionPoint acquisitionPoint, long stopNotificationTimeInSecs, EventAction action, ConfirmedPlacementOpportunity aqCPO) {
		final NotificationMessage notificationAtEventEnd = defaultEndOrUpdateEventNotificationMessage(action, acquisitionPoint, blackout, immediateStop, stopNotificationTimeInSecs, aqCPO);
		addMessageInNotificationByScheduledTimeMap(notificationMessagesByScheduleTime, notificationAtEventEnd);
	}
}
