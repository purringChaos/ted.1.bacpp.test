package tv.blackarrow.cpp.notifications.hosted.scheduler;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;

public class ScheduledHostedNotificationSchedulerService implements HostedNotificationScheduler{

	private static final Logger LOGGER = LogManager.getLogger(ScheduledHostedNotificationSchedulerService.class);
	private static HostedNotificationScheduler HOSTED_NOTIFICATION_SCHEDULER = new ScheduledHostedNotificationSchedulerService();


	public static HostedNotificationScheduler getInstance() {
		return HOSTED_NOTIFICATION_SCHEDULER;
	}

	@Override
	public void scheduleHostedNotification(Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap) {
		LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES: ADD hosted notification in Queue");
		//This map holds Map of Schedule Messages for schedule time: 
		final Map<Long, Map<String, HostedAppEventStatusNotificationModel>> hostedEventNotificationsByScheduleTime = new HashMap<>();
		//This Map holds the schedule time against the signal Id: used for cancel notification in next schedule.  
		final Map<String, Set<Long>> hostedEventNotificationTimesBySignalId = new HashMap<>();
		long currentTimeInSeconds = Instant.now().getEpochSecond();
		//Prepare a MAP with the schedule time, so that notification can be set at that time. 
		hostedNotificationMap.entrySet().forEach(entry -> {
			HostedAppEventStatusNotificationModel modal = entry.getValue();
			if(currentTimeInSeconds < modal.getHostedNotificationScheduleTime()) {
				hostedEventNotificationsByScheduleTime.computeIfAbsent(modal.getHostedNotificationScheduleTime(), key -> new HashMap<String, HostedAppEventStatusNotificationModel>()).put(entry.getKey(), entry.getValue());
				String signalId = StringUtils.EMPTY;
				if(modal instanceof HostedAppEventStatusI02NotifyModel) {
					HostedAppEventStatusI02NotifyModel tempEventNotificationModal = (HostedAppEventStatusI02NotifyModel)modal;
					signalId = tempEventNotificationModal.getSignalId();
				}else if(modal instanceof HostedAppEventStatusScte224NotifyModel) {
					HostedAppEventStatusScte224NotifyModel tempEventNotificationModal = (HostedAppEventStatusScte224NotifyModel)modal;
					signalId = tempEventNotificationModal.getEventSignalId();
				}
				final String finalSignalIdForLogging = signalId;
				LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES: Signal Id Recieved as : "+finalSignalIdForLogging);
				hostedEventNotificationTimesBySignalId.computeIfAbsent(signalId, key -> new HashSet<Long>()).add(modal.getHostedNotificationScheduleTime());
			}
		});
		
		//Cancel the previous schedule notification against the signal id:
		if(!hostedEventNotificationTimesBySignalId.isEmpty()) {
			List<String> hostedNotificationKeys = new ArrayList<>(hostedEventNotificationTimesBySignalId.keySet());
			LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES: Following keys found to cancel the schedule: "+hostedNotificationKeys);
			cancelScheduledHostedNotification(hostedNotificationKeys);
		}
		
		//prepare the current schedule notification against the signal id: 		
		hostedEventNotificationTimesBySignalId.entrySet().forEach(entryKey -> {
			String signalId = entryKey.getKey();
			Set<Long> scheduleTimes = entryKey.getValue();
			LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES: For Signal Id: "+signalId+" Schedule times are: "+scheduleTimes);
			saveHostedNotificationTimesForSignalId(signalId, scheduleTimes);			
		});
		
		//Hosted Notification schedule: 
		hostedEventNotificationsByScheduleTime.entrySet().forEach(entry -> {
			final long hostedNotificationScheduleTime = entry.getKey();
			final String timeBasedHostedNotificationQueueName = HostedNotificationScheduler.getHostedNotificationTimeBasedQueueName(hostedNotificationScheduleTime);
			
			Map<String, HostedAppEventStatusNotificationModel> hostedMessageByKeyMap = entry.getValue();
			Iterable<List<String>> hostedAppEventsBatches = Iterables.partition(hostedMessageByKeyMap.keySet(), QUEUE_PARTITION_BATCH);
			LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES : Message is going to Add in Queue: "+timeBasedHostedNotificationQueueName);
			
			hostedAppEventsBatches.forEach(hostedAppEventsBatch -> {
				DATAMANAGER.appendToQueue(timeBasedHostedNotificationQueueName, GSON.toJson(hostedAppEventsBatch, new TypeToken<HashSet<String>>(){}.getType()),
						(int)(hostedNotificationScheduleTime + EXPIRATION_TIME_DELTA));
				LOGGER.info(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES :Added to " + timeBasedHostedNotificationQueueName + " Queue-->(" + new Date(hostedNotificationScheduleTime * 1000)
						+")--> element " + hostedAppEventsBatch);
			});
			
			hostedMessageByKeyMap.entrySet().forEach(entryKey -> {
				HostedAppEventStatusNotificationModel eventStatusModal = entryKey.getValue();
				
				
				if(eventStatusModal instanceof HostedAppEventStatusI02NotifyModel) {
					DATAMANAGER.set(entryKey.getKey(), 
							GSON.toJson(eventStatusModal, HostedAppEventStatusI02NotifyModel.class),
							(int)(hostedNotificationScheduleTime + EXPIRATION_TIME_DELTA));
				}else if(eventStatusModal instanceof HostedAppEventStatusScte224NotifyModel) {
					DATAMANAGER.set(entryKey.getKey(), 
							GSON.toJson(eventStatusModal, HostedAppEventStatusScte224NotifyModel.class),
							(int)(hostedNotificationScheduleTime + EXPIRATION_TIME_DELTA));
				}
			});
			LOGGER.debug(() -> "HOSTED_NOTIFICATION_TIMED_QUEUES : The Queue preview: "+timeBasedHostedNotificationQueueName);				
		});			
	}
	
	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.notifications.hosted.HostedNotificationScheduler#cancelScheduleHostedNotification(java.util.List)
	 */
	@Override
	public void cancelScheduledHostedNotification(List<String> hostedNotificationSignalIds) {
		final long currentTimeInSeconds = Instant.now().getEpochSecond();
		hostedNotificationSignalIds.forEach(signalIdInput -> {
			Set<Long> scheduleTimesToUnscheduleSet = this.getHostedNotificationTimeFromSignalIdQueue(signalIdInput);
			if(Objects.nonNull(scheduleTimesToUnscheduleSet.isEmpty()) && !scheduleTimesToUnscheduleSet.isEmpty()) {
				scheduleTimesToUnscheduleSet.parallelStream().forEach(scheduleTimesToUnschedule -> {
					if(currentTimeInSeconds > scheduleTimesToUnschedule) {
						LOGGER.debug(() -> "HOSTED_NOTIFICATION: CANCEL_SCHEDULE: current time "+currentTimeInSeconds+" is greater than schedule time, so skipping cancel schedule process for this schedule time: "+scheduleTimesToUnschedule);
					}else {
						final String hostedNotificationTimeBasedQueueName = HostedNotificationScheduler.getHostedNotificationTimeBasedQueueName(scheduleTimesToUnschedule);
						LOGGER.debug(() -> "HOSTED_NOTIFICATION: CANCEL_SCHEDULE: Queue "+ hostedNotificationTimeBasedQueueName + " is found to cancel the schedule for Signal Id: "+signalIdInput);
						DATAMANAGER.unscheduleNotification(hostedNotificationTimeBasedQueueName, signalIdInput);
						LOGGER.debug(() -> "HOSTED_NOTIFICATION: CANCEL_SCHEDULE: "+ hostedNotificationTimeBasedQueueName + " cancelled for Signal Id: "+signalIdInput);
					}
				});
			}
		});		
	}


}
