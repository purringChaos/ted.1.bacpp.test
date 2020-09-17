package tv.blackarrow.cpp.notifications.upstream.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.AcquisitionPointNotificationService;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

public abstract class NotificationSenderImpl implements NotificationSender {
	private static final Logger LOG = LogManager.getLogger(NotificationSenderImpl.class);

	protected final boolean notifyUpstreamSystem(final NotificationMessage notificationMessage) {
		boolean isNotified = false;
		if (notificationMessage == null) {
			return isNotified;
		}

		try {
			isNotified = AcquisitionPointNotificationService.notify(notificationMessage);
		} catch (Throwable e) {
			LOG.error(()->"Failed to notify the end point : " + notificationMessage.getStreamURL() + ", due to the following unexpected error: \n", e);
		}
		return isNotified;
	}

}
