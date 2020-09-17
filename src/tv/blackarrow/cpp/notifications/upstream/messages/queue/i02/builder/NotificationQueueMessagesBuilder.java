package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder;

import java.util.Map;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.utils.EventAction;

public interface NotificationQueueMessagesBuilder {
	public void buildForPendingEventsUpstreamNotification(final long currentSystemTime, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime);

	public void buildForLiveEventsUpstreamNotification(final long currentSystemTime, boolean immediateStart, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, EventAction update);

	public void buildForHostedNotifications(final AcquisitionPoint acquisitionPoint, final BlackoutEvent blackout, final int maxProgramStartBufferInMillis,
			final Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages, boolean immediateStart, int currentSystemTimeInSecs);

	public void buildForLiveEventHostedNotification(BlackoutEvent blackout, int maxProgramStartBufferOfAllAQ,
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap, AcquisitionPoint aqpt);

	public void rebuildForLiveEventsUpstreamNotification(long currentSystemTime, BlackoutEvent blackout, AcquisitionPoint acquisitionPoint,
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime);
}
