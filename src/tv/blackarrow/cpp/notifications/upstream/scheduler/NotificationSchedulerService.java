/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.scheduler;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.UPSTREAM_NOTIFICATION_QUEUE_PREFIX;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

/**
 * @author Amit Kumar Sharma
 *
 */
public class NotificationSchedulerService implements NotificationScheduler {
	private static final Logger LOG = LogManager.getLogger(NotificationSchedulerService.class);
	private static final NotificationScheduler NOTIFICATION_SCHEDULER = new NotificationSchedulerService();
	public static final int EXPIRATION_TIME_DELTA = 5 * 60;//5 minutes
	
	private NotificationSchedulerService() {
		super();
	}
	
	public static NotificationScheduler getInstance() {
		return NOTIFICATION_SCHEDULER;
	}

	@Override
	public void cancelScheduledNotificationBySignalId(
			List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos) {
		if(cancelScheduledNotificationInfos!=null) {
			cancelScheduledNotificationInfos.parallelStream().forEach(this::cancelScheduledNotificationBySignalId);
		}
	}
	
	@Override
	public void cancelScheduledNotificationBySignalId(CancelScheduledNotificationByEventSignalIdInfo cancelScheduledNotificationInfo) {
		if(cancelScheduledNotificationInfo!=null) {
			//Just remove the references from the queue. No need to remove the individual message Objects they will either get overwritten or will expire at the 
			//right time.
			final String upstreamNotificationQueueName = getUpstreamNotificationQueueName(cancelScheduledNotificationInfo.getNotificationTimeInSeconds());
			DATAMANAGER.unscheduleNotification(upstreamNotificationQueueName, cancelScheduledNotificationInfo.getEventSignalId());
			if(LOG.isDebugEnabled()) {
				LOG.debug("UPStreamNotificationQueueName: "+ upstreamNotificationQueueName + " cancelled");
			}
		}
	}

	@Override
	public void scheduleNotification(List<ScheduleNotificationInfo> scheduleInfos, String eventIdForLogging) {
		if (scheduleInfos != null) {
			scheduleInfos.parallelStream().forEach(scheduleInfo -> {
				scheduleNotification(scheduleInfo, eventIdForLogging);
			});
		}
	}

	@Override
	public void scheduleNotification(ScheduleNotificationInfo scheduleInfo, String eventIdForLogging) {
		if(scheduleInfo == null) {
			return;
		}
		String logFormat = "Event id: " + eventIdForLogging + "), ";
		String someDebugLog = "EsamProgramEndNotifyAheadDeltaInMillis = " + CppConfigurationBean.getInstance().getEsamProgramEndNotifyAheadDeltaInMillis()
				+ " essProcessingTimeInSeconds = " + CppConfigurationBean.getInstance().getLinearPoisProcessingTimeInSeconds();
		int scheduleTimeForQueue = scheduleInfo.getNotificationTimeInSeconds();
		if(scheduleTimeForQueue <= TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())) {
			LOG.error(
					() -> "1. " + logFormat + "The scheduling time(" + scheduleInfo + ") is too close to current time and hence this scheduling job can not be accepted."
							+ " Here, application Level Settings are " + someDebugLog);
			return;
		}
		final String upstreamNotificationQueueName = getUpstreamNotificationQueueName(scheduleTimeForQueue);
		LOG.debug(() -> "1. " + logFormat + "Scheudling Notifications in the queue : " + upstreamNotificationQueueName);
		Iterable<List<String>> notificationBatches = Iterables.partition(scheduleInfo.getNotificationMessages().keySet(), 
				UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE);

		LOG.debug(() -> "2. " + logFormat + "Notification Messages: " + scheduleInfo.getNotificationMessages());
		scheduleInfo.getNotificationMessages()
					.entrySet()
					.parallelStream()
					.forEach(entry -> DATAMANAGER.set(entry.getKey(), 
							GSON.toJson(entry.getValue(), NotificationMessage.class),
							scheduleTimeForQueue + EXPIRATION_TIME_DELTA));
		
		LOG.debug(() -> "3. " + logFormat + "Notification Batches: " + notificationBatches);
		notificationBatches.forEach(queueElement ->	DATAMANAGER.appendToQueue(
														upstreamNotificationQueueName, 
														GSON.toJson(queueElement,new TypeToken<HashSet<String>>(){}.getType()), 
														scheduleTimeForQueue + EXPIRATION_TIME_DELTA
													)
									);

		LOG.info(() -> "4. " + logFormat + ": Finished Adding in Queue : " + upstreamNotificationQueueName + "--> (" + new Date((long)scheduleTimeForQueue * 1000) +") -->"+ scheduleInfo.printable());
	}
	
	public static String getUpstreamNotificationQueueName(long notificationTimeInSeconds) {
		return UPSTREAM_NOTIFICATION_QUEUE_PREFIX + notificationTimeInSeconds;
	}

	@Override
	public void cancelScheduledNotificationByJobId(String blackoutSignalId, String scheduledNotificationId) {
		final Set<Integer> scheduledTimes = DATAMANAGER.getEventScheduledNotificationQueueTimes(blackoutSignalId);
		if (!scheduledTimes.isEmpty()) {
			scheduledTimes.forEach(scheduledTime -> {
				final String upstreamNotificationQueueName = getUpstreamNotificationQueueName(scheduledTime);
				DATAMANAGER.unscheduleNotificationByNotificationId(upstreamNotificationQueueName, scheduledNotificationId);
				if (LOG.isDebugEnabled()) {
					LOG.debug("UPStreamNotificationQueueName: " + upstreamNotificationQueueName + " cancelled");
				}
			});
		}
	}


	@Override
	public void reScheduledHostedNotificationByJobId(String string) {
		// TODO Auto-generated method stub
		
	}

}
