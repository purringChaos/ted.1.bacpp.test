/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationsSchedulerFactory;

/**
 *
 */
public class MediaRuntimeNotificationsHandler {

	private static final Logger LOG = LogManager.getLogger(MediaRuntimeNotificationsHandler.class);

	public static void notify224EventStatusToHostedImmediately(HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel) {
		Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages = new HashMap<>();

		hostedAppEventStatusNotifyModel.setHostedNotificationScheduleTime(-1);
		hostedNotificationQueueMessages.put(hostedAppEventStatusNotifyModel.getEventSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR
				+ hostedAppEventStatusNotifyModel.getStreamId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + hostedAppEventStatusNotifyModel.getEventStatus().name(),
				hostedAppEventStatusNotifyModel);
		HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(true).scheduleHostedNotification(hostedNotificationQueueMessages);
	}
	
	public static void notify224MediaPointStatusToHostedImmediately(HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel) {
		Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages = new HashMap<>();

		hostedAppEventStatusNotifyModel.setHostedNotificationScheduleTime(-1);
		hostedNotificationQueueMessages.put(hostedAppEventStatusNotifyModel.getMediaPointSignalId()+ NotificationServiceConfig.JOB_NAME_SEPARATOR
				+ hostedAppEventStatusNotifyModel.getStreamId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + hostedAppEventStatusNotifyModel.getEventStatus().name(),
				hostedAppEventStatusNotifyModel);
		HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(true).scheduleHostedNotification(hostedNotificationQueueMessages);
	}

}
