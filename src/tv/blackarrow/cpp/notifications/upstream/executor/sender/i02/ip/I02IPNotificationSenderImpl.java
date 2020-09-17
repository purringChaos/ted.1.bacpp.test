package tv.blackarrow.cpp.notifications.upstream.executor.sender.i02.ip;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationsSchedulerFactory;
import tv.blackarrow.cpp.notifications.upstream.executor.sender.i02.I02NotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.utils.EventAction;

public class I02IPNotificationSenderImpl extends I02NotificationSenderImpl {
	private static final Logger LOG = LogManager.getLogger(I02IPNotificationSenderImpl.class);
	protected static final String SEPARATOR = "#@#";

	@Override
	public void performSending(NotificationMessage notificationMessage) {
		super.performSending(notificationMessage);
		//Send Status Update to Hosted In case of IP, But do not need to notify Hosted again when we receive any UPDATE from them(on Live Event)
		if (!EventAction.UPDATE.equals(notificationMessage.getEventAction())) {
			LOG.info(() -> notificationMessage.getEventSignalId() + ": Notifying hosted for " + notificationMessage.getEventAction());
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap = new HashMap<>();

			HostedAppEventStatusI02NotifyModel hostedStatusMessage = new HostedAppEventStatusI02NotifyModel(notificationMessage.getFeedExtRef(),
					notificationMessage.getEventType().name(), notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getEventSignalUTCStopTime(),
					notificationMessage.getEventSignalId(), notificationMessage.getStreamId(), notificationMessage.getEventId());

			hostedStatusMessage.setEventAction(isNotified ? EventAction.CONFIRMED : EventAction.ERROR);
			hostedStatusMessage.setActualUTCStartTime(isNotified ? notificationMessage.getEventSignalUTCStartTime() : 0l);
			hostedStatusMessage.setEventType(EventType.I02);
			hostedStatusMessage.setHostedNotificationScheduleTime(-1);

			hostedNotificationMap.put(notificationMessage.getEventSignalId() + SEPARATOR + notificationMessage.getStreamId() + SEPARATOR + EventAction.CONFIRMED,
					hostedStatusMessage);
			HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(true).scheduleHostedNotification(hostedNotificationMap);
		}
	}

}
