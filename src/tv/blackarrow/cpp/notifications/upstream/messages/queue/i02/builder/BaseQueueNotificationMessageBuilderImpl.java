package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.JOB_NAME_SEPARATOR;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;
import tv.blackarrow.cpp.utils.EventAction;

public abstract class BaseQueueNotificationMessageBuilderImpl implements NotificationQueueMessagesBuilder {
	protected static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();

	protected void addStartNotificationMessage(boolean immediateStart, BlackoutEvent blackout,
			final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, AcquisitionPoint acquisitionPoint, long startNotificationTimeInSecs) {
		final NotificationMessage notificationAtEventStart = getStartEventNotificationMessage(acquisitionPoint, blackout, immediateStart, startNotificationTimeInSecs);
		addMessageInNotificationByScheduledTimeMap(notificationMessagesByScheduleTime, notificationAtEventStart);
	}	
	
	protected void addMessageInNotificationByScheduledTimeMap(final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime,
			final NotificationMessage notificationMessage) {
		if (notificationMessage != null) {
			notificationMessagesByScheduleTime.computeIfAbsent(notificationMessage.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
					.put(notificationMessage.getScheduledNotificationId(), notificationMessage);
		}
	}
	
	protected long getStartNotificationTimeOfBlackoutWithBuffer(BlackoutEvent event, AcquisitionPoint acqPoint, long currentSystemTimeInSecs, boolean isImmediate) {
		return isImmediate ? notifyTimeInImmediateCaseWithAddedBuffer(acqPoint, currentSystemTimeInSecs)
				: TimeUnit.MILLISECONDS.toSeconds(event.getUtcStartTime() - (acqPoint.getFeedSCCNotificationBuffer() * 1000));
	}

	protected long getStopNotificationTimeOfBlackoutWithBuffer(BlackoutEvent event, AcquisitionPoint acqPoint, long currentSystemTimeInSecs, boolean isImmediate) {
		return isImmediate ? notifyTimeInImmediateCaseWithAddedBuffer(acqPoint, currentSystemTimeInSecs)
				: TimeUnit.MILLISECONDS.toSeconds(event.getUtcStopTime() - (acqPoint.getFeedSCCNotificationBuffer() * 1000)
						-  CppConfigurationBean.getInstance().getEsamProgramEndNotifyAheadDeltaInMillis());
	}


	@Override
	public void buildForHostedNotifications(final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout, final int maxProgramStartBufferInMillis,
			final Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages, boolean immediateStart, int currentSystemTimeInSecs) {
	}

	private NotificationMessage getStartEventNotificationMessage(final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout, boolean immediateStart,
			final long startNotificationTimeInSecs) {

		//In case of ImmediateStart, we will notify the AQ in next 5 seconds, that it has to start the program in next 5 seconds.(So you will see, all immediate operations will result in starting/ending events within 10 seconds, 
		//as this is the time needed by our system to process it)
		final long programStartUTCTime = immediateStart ? immediateEventStartOrEndValueInBlackout(acquisitionPoint, startNotificationTimeInSecs) : blackout.getUtcStartTime();
		final long programStopUTCTime = blackout.getUtcStopTime();

		NotificationMessage notificationMessage = getDefaultNotificationMessage(EventAction.CONFIRMED, acquisitionPoint, blackout);
		//1. Add EventStart/EventStop UTC Offset
		notificationMessage.setNotificationScheduledTime((int) startNotificationTimeInSecs);
		notificationMessage.setEventSignalUTCStartTime(programStartUTCTime);
		notificationMessage.setEventSignalUTCStopTime(programStopUTCTime);
		notificationMessage.setContentDuration(programStopUTCTime-programStartUTCTime);
		return notificationMessage;
	}
	

	protected NotificationMessage defaultEndOrUpdateEventNotificationMessage(EventAction action, final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout, final boolean isImmediateStop,
			final long endNotificationTimeInSecs,  ConfirmedPlacementOpportunity aqCPO) {
		
		final long programStartUTCTime = endNotificationTimeInSecs*1000 ;
		final long programStopUTCTime = isImmediateStop ? immediateEventStartOrEndValueInBlackout(acquisitionPoint, endNotificationTimeInSecs) : blackout.getUtcStopTime();

		NotificationMessage notificationMessage = getDefaultNotificationMessage(action, acquisitionPoint, blackout);
		//1. Add EventStart/EventStop UTC Offset
		notificationMessage.setNotificationScheduledTime((int) endNotificationTimeInSecs);
		notificationMessage.setEventSignalUTCStartTime(programStartUTCTime);
		notificationMessage.setEventSignalUTCStopTime(programStopUTCTime);
		notificationMessage.setContentDuration(programStopUTCTime-programStartUTCTime);
		return notificationMessage;
	}
	

	private static String getScheduleNotificationId(final BlackoutEvent blackout, final AcquisitionPoint acqPoint, final EventAction eventAction) {
		return blackout.getSignalId() + JOB_NAME_SEPARATOR + acqPoint.getAcquisitionPointIdentity() + JOB_NAME_SEPARATOR
				+ (EventAction.STOP_NOW.equals(eventAction) ? EventAction.COMPLETE.name() : eventAction.name());
	}

	private static NotificationMessage getDefaultNotificationMessage(final EventAction eventAction, final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout) {

		NotificationMessage notificationMessage = new NotificationMessage();
		notificationMessage.setFeedExtRef(acquisitionPoint.getFeedExternalRef());
		notificationMessage.setEventAction(eventAction);
		notificationMessage.setEventId(blackout.getEventId());
		notificationMessage.setEventSignalId(blackout.getSignalId());

		notificationMessage.setEventType(EventType.I02);
		notificationMessage.setScheduledNotificationId(getScheduleNotificationId(blackout, acquisitionPoint, eventAction));
		notificationMessage.setStreamId(acquisitionPoint.getAcquisitionPointIdentity());
		notificationMessage.setStreamSignalTimeOffset(acquisitionPoint.getSignalTimeOffset());
		notificationMessage.setStreamURL(acquisitionPoint.getTranscoderEndpoint());
		notificationMessage.setUpStreamNotificationMessageType(UpStreamNotificationMessageType.getNotificationMessageType(acquisitionPoint));
		notificationMessage.setSchema(Schema.getSchema(acquisitionPoint.getEsamVersion()));
		notificationMessage.setAqContentFrequency(acquisitionPoint.getContentIDFrequency());
		notificationMessage.setZoneIdentity(acquisitionPoint.getZoneIdentity());

		return notificationMessage;

	}

	@Override
	public void buildForPendingEventsUpstreamNotification(long currentSystemTime, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void buildForLiveEventsUpstreamNotification(long currentSystemTime, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, EventAction update) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rebuildForLiveEventsUpstreamNotification(long currentSystemTime, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime) {
		// TODO Auto-generated method stub
		
	} 

	@Override
	public void buildForLiveEventHostedNotification(BlackoutEvent blackout, int maxProgramStartBufferOfAllAQ,
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap, AcquisitionPoint aqpt) {
		// TODO Auto-generated method stub
		
	}

	
	protected long immediateEventStartOrEndValueInBlackout(final AcquisitionPoint acqPoint, final long timeWhenNotificationWillBeSendInSecs) {
		return (timeWhenNotificationWillBeSendInSecs + acqPoint.getFeedSCCNotificationBuffer()) * 1000;
	}
	
	protected long notifyTimeInImmediateCaseWithAddedBuffer(final AcquisitionPoint acqPoint,long currentSystemTimeInSecs) {
		return currentSystemTimeInSecs + NotificationServiceConfig.ESS_PROCESSING_TIME_IN_SECS;
	}

}
