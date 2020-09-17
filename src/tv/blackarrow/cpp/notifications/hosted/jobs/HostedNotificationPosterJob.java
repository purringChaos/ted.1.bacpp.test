package tv.blackarrow.cpp.notifications.hosted.jobs;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationScheduler;

public class HostedNotificationPosterJob implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(HostedNotificationPosterJob.class);
	public static String SEPARATOR = "#@#";

	public static final DataManager DATAMANAGER = DataManagerFactory.getInstance();

	@Override
	public void run() {
		final String HOSTED_NOTIFICATION_QUEUE = HostedNotificationScheduler.HOSTED_MESSAGE_QUEUE;
		final Thread CURRENT_THREAD = Thread.currentThread();
		final String LOG_IDENTIFIER = "Thread: " + CURRENT_THREAD.getName() + "_" + CURRENT_THREAD.getId() + ": HOSTED_NOTIFICATION_POSTER: ";

		try {
			LOGGER.debug(() -> LOG_IDENTIFIER + " Mesage look up from QUEUE started: " + HOSTED_NOTIFICATION_QUEUE);
			while (true) {
				if (DataManagerFactory.getInstance().isServerInActiveDataCenter()) {
					String queueElement = null;

					while ((queueElement = DATAMANAGER.popFromQueue(HOSTED_NOTIFICATION_QUEUE)) != null) {
						String queueElementForLogs = queueElement;

						Set<String> hostedNotificationMessageKeys = GSON.fromJson(queueElement, new TypeToken<HashSet<String>>() {
						}.getType());
						LOGGER.debug(() -> LOG_IDENTIFIER + ":" + HOSTED_NOTIFICATION_QUEUE + " Element found and popped from Queue: " + HOSTED_NOTIFICATION_QUEUE + ": queueElement: "
								+ queueElementForLogs + " and Keys from Queue: " + hostedNotificationMessageKeys);
						HostedAsyncSender.notifyToHosted(LOG_IDENTIFIER, hostedNotificationMessageKeys);

					}
				} else {
					LOGGER.debug(() -> LOG_IDENTIFIER + " No Element found in : " + HOSTED_NOTIFICATION_QUEUE + ", going to sleep for a second.");
					TimeUnit.SECONDS.sleep(1);
				}
			}
		} catch (Throwable e) {
			LOGGER.error(() -> LOG_IDENTIFIER + " Error occured while sending hosted notification: " + e.getMessage(), e);
		}
	}

}
