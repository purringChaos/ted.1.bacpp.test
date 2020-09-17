package tv.blackarrow.cpp.notifications.hosted.scheduler;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;

public interface HostedNotificationScheduler {
	public static String HOSTED_MESSAGE_QUEUE = "HMQ";
	public static String HOSTED_MESSAGE_TIMEBASED_QUEUE = "HJNQ_";
	public static String HOSTED_MESSAGE_SCHEDULE_TIME_BY_SIGNAL_ID_KEY = "HJNQT:";
	
	public static int EXPIRATION_TIME_DELTA = 10 * 60;
	public static int QUEUE_PARTITION_BATCH = 5;

	/*
	 * This method scheduled the hosted notifications
	 * 1. It schedule hosted notification in HMQ instantly. 
	 * 2. It schedule hosted notification in HJNQ_EPOCTIME_IN_SECONDS[[BATCH_OF_FIVE], [BATCH_OF_FIVE], ....]
	 * 3. Before scheduling it check if there is already a schedule present in TIME_BASED_QUEUE HJNQ_, then 
	 * it cancel the existing schedule and reschedule the notification.  
	 * 
	 */
	public void scheduleHostedNotification(Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap);
	/*
	 * 1. Cancel the message from whichever time based Queue it exists.
	 */
	public void cancelScheduledHostedNotification(List<String> hostedNotificationKeys);
	
	/**
	 * Time based queue name
	 * @param scheduleTimeInSeconds
	 * @return
	 */
	public static String getHostedNotificationTimeBasedQueueName(long scheduleTimeInSeconds) {
		return HOSTED_MESSAGE_TIMEBASED_QUEUE+scheduleTimeInSeconds;
	}
	
	/**
	 * @param signalId
	 * @return
	 */
	public static String getScheduleTimeBySignalIdMessage(String signalId) {
		return HOSTED_MESSAGE_SCHEDULE_TIME_BY_SIGNAL_ID_KEY+signalId;
	}
	
	public default Set<Long> getHostedNotificationTimeFromSignalIdQueue(String signalId){
		String hostedNotificationScheduleTimes = DATAMANAGER.get(HostedNotificationScheduler.getScheduleTimeBySignalIdMessage(signalId));
		if(!StringUtils.isEmpty(hostedNotificationScheduleTimes)) {
			return GSON.fromJson(hostedNotificationScheduleTimes, new TypeToken<HashSet<Long>>() {}.getType());
		}
		return new HashSet<>();
	}
	
	public default void saveHostedNotificationTimesForSignalId(final String signalId, final Set<Long> scheduledTimes) {
		DATAMANAGER.set(HostedNotificationScheduler.getScheduleTimeBySignalIdMessage(signalId), GSON.toJson(scheduledTimes, new TypeToken<HashSet<Long>>() {
		}.getType()));
	}

}
