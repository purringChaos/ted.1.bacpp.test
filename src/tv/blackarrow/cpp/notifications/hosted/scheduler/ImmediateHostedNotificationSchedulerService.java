/**
 * 
 */
package tv.blackarrow.cpp.notifications.hosted.scheduler;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;

/**
 * @author asharma
 *
 */
public class ImmediateHostedNotificationSchedulerService implements HostedNotificationScheduler {

	private static final Logger LOGGER = LogManager.getLogger(ImmediateHostedNotificationSchedulerService.class);
	private static HostedNotificationScheduler HOSTED_NOTIFICATION_SCHEDULER = new ImmediateHostedNotificationSchedulerService();

	public static HostedNotificationScheduler getInstance() {
		return HOSTED_NOTIFICATION_SCHEDULER;
	}

	@Override
	public void cancelScheduledHostedNotification(List<String> hostedNotificationKeys) {
		// Does nothing immediate hosted notifications can't be stopped or unscheduled.

	}

	/*@Override
	 * 
	 * Original HMQ based implementation******
	public void scheduleHostedNotification(Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap) {
		LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: ADD hosted notification in Queue");
		Iterable<List<String>> hostedAppEventsBatches = Iterables.partition(hostedNotificationMap.keySet(), QUEUE_PARTITION_BATCH);
		LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message is going to Add in Queue: " + HOSTED_MESSAGE_QUEUE);
	
		hostedNotificationMap.entrySet().forEach(entryKey -> {
			HostedAppEventStatusNotificationModel eventStatusModal = entryKey.getValue();
			if (eventStatusModal instanceof HostedAppEventStatusI02NotifyModel) {
				DATAMANAGER.set(entryKey.getKey(), GSON.toJson(eventStatusModal, HostedAppEventStatusI02NotifyModel.class));
				LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message Added for " + eventStatusModal.getEventType() + " in Queue : " + HOSTED_MESSAGE_QUEUE);
			} else if (eventStatusModal instanceof HostedAppEventStatusScte224NotifyModel) {
				DATAMANAGER.set(entryKey.getKey(), GSON.toJson(eventStatusModal, HostedAppEventStatusScte224NotifyModel.class));
				LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message Added for " + eventStatusModal.getEventType() + " in Queue : " + HOSTED_MESSAGE_QUEUE);
			}
		});
		hostedAppEventsBatches.forEach(queueElement -> {
			long threadId = Thread.currentThread().getId();
			DATAMANAGER.appendToQueue(HOSTED_MESSAGE_QUEUE, GSON.toJson(queueElement, new TypeToken<HashSet<String>>() {
			}.getType()));
			LOGGER.debug(() -> "Thread: " + threadId + "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Queue Element passed to " + HOSTED_MESSAGE_QUEUE + " are " + queueElement);
		});
	}*/

	@Override
	/*
	 * Adding to Time queue with 5 seconds in future.
	 * (non-Javadoc)
	 * @see tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationScheduler#scheduleHostedNotification(java.util.Map)
	 */
	public void scheduleHostedNotification(Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap) {

		long currentTime = Instant.now().getEpochSecond();
		LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: ADD hosted notification in Queue" + hostedNotificationMap.keySet());		

		hostedNotificationMap.entrySet().forEach(entryKey -> {
			HostedAppEventStatusNotificationModel eventStatusModal = entryKey.getValue();
			if (eventStatusModal instanceof HostedAppEventStatusI02NotifyModel) {
				DATAMANAGER.set(entryKey.getKey(), GSON.toJson(eventStatusModal, HostedAppEventStatusI02NotifyModel.class), (int) (currentTime + EXPIRATION_TIME_DELTA));
				LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message Data Added for " + eventStatusModal.getEventType());
			} else if (eventStatusModal instanceof HostedAppEventStatusScte224NotifyModel) {
				DATAMANAGER.set(entryKey.getKey(), GSON.toJson(eventStatusModal, HostedAppEventStatusScte224NotifyModel.class), (int) (currentTime + EXPIRATION_TIME_DELTA));
				LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message Added for " + eventStatusModal.getEventType());
			}
		});

		long futureTimeInSeconds = Instant.now().getEpochSecond() + CppConfigurationBean.getInstance().getLinearPoisProcessingTimeInSeconds();
		final String timeBasedHostedNotificationQueueName = HostedNotificationScheduler.getHostedNotificationTimeBasedQueueName(futureTimeInSeconds);
		
		String sb = Arrays.toString(hostedNotificationMap.keySet().toArray());
		LOGGER.debug(() -> "HOSTED_NOTIFICATION: IMMEDIATE_SCHEDULE: Message Data Added for " + sb + " in Queue : " + timeBasedHostedNotificationQueueName);

		
		Iterable<List<String>> hostedAppEventsBatches = Iterables.partition(hostedNotificationMap.keySet(), QUEUE_PARTITION_BATCH);
		LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES : Message is going to Scheduled in Queue: " + timeBasedHostedNotificationQueueName);
		hostedAppEventsBatches.forEach(hostedAppEventsBatchToProcess -> {

			//Modified to contain I=Immediate Flag at the end, so DataManagerCouchbaseImpl:unscheduleNotification skip these keys. Since these are meant to be in realtime and already happened, we do not want any activity to touch it. 
			ListIterator<String> iter = hostedAppEventsBatchToProcess.listIterator();
			List<String> hostedAppEventsImmediateBatch = new ArrayList<>();
			while (iter.hasNext()) {
				String element = iter.next();
				hostedAppEventsImmediateBatch.add((new StringBuilder(element).append(NotificationServiceConfig.IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER).toString()));
			}

			DATAMANAGER.appendToQueue(timeBasedHostedNotificationQueueName, GSON.toJson(hostedAppEventsImmediateBatch, new TypeToken<HashSet<String>>() {
			}.getType()), (int) (futureTimeInSeconds + EXPIRATION_TIME_DELTA));
			LOGGER.info(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES :Scheduled to " + timeBasedHostedNotificationQueueName + " Queue-->(" + new Date(futureTimeInSeconds * 1000) + ")--> element "
					+ hostedAppEventsBatchToProcess);
		});
	}

}
