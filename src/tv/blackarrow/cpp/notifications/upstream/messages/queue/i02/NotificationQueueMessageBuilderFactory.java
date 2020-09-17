package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.NotificationQueueMessagesBuilder;

public class NotificationQueueMessageBuilderFactory {

	private static NotificationQueueMessageBuilderFactory instance = new NotificationQueueMessageBuilderFactory();

	private NotificationQueueMessageBuilderFactory() {

	}

	public static NotificationQueueMessageBuilderFactory getInstance() {
		return instance;
	}

	public NotificationQueueMessagesBuilder getNotificationMessageBuilder(boolean ipNotificationMessageBuilder) {
		NotificationQueueMessagesBuilder scheduler = null;
		if (ipNotificationMessageBuilder) {
			//Only START Time Upstream Jobs needs to be scheduled
			scheduler = new tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.ip.NotificationQueueMessageBuilderImpl();
		} else {
			//Only START/END Time Upstream Jobs needs to be scheduled
			scheduler = new tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.qam.NotificationQueueMessageBuilderImpl();
		}
		return scheduler;
	}

}
