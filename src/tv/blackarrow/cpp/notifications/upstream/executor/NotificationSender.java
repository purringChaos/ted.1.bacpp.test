package tv.blackarrow.cpp.notifications.upstream.executor;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

public interface NotificationSender {

	public void performSending(final NotificationMessage notificationMessage);
}
