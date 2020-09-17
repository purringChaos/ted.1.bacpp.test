package tv.blackarrow.cpp.notifications.hosted.jobs;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationScheduler;

/**
 * This class is used to push the data from time based queues to main queues.   
 * @author cpandey
 *
 */
public class HostedNotificationSynchQueuesWatcherJob implements Runnable{
	private static final Logger LOGGER = LogManager.getLogger(HostedNotificationSynchQueuesWatcherJob.class);

	//THIS JOB WILL WAKE UP EVERY SECONDS.
	//THIS JOB WILL PICK UP THE DATA HJNQ QUEUE FOR THAT SECONDS. 
	//IF THIS DATA IS PRESENT, THEN POP IT AND PLACE IT IN HMQ. 

	@Override
	public void run() {
		long currentTimeInSeconds = Instant.now().getEpochSecond();
		final Thread CURRENT_THREAD = Thread.currentThread();
		final String LOG_IDENTIFIER_BASIC = "Thread: " + CURRENT_THREAD.getName() + "_" + CURRENT_THREAD.getId() + ": HOSTED_NOTIFICATION_TIMED_QUEUES: ";
		LOGGER.debug(()-> LOG_IDENTIFIER_BASIC + "hosted Notification job : ");
		while(true) {
			try {
				if (DataManagerFactory.getInstance().isServerInActiveDataCenter()) {
					String TIME_BASED_QUEUE_NAME = HostedNotificationScheduler.getHostedNotificationTimeBasedQueueName(currentTimeInSeconds);
					//LOGGER.debug(()-> LOG_IDENTIFIER + "Processing Queue : " + TIME_BASED_QUEUE_NAME);
					String queueElement = null;
					while((queueElement= DATAMANAGER.popFromQueue(TIME_BASED_QUEUE_NAME))!=null) {
						long startTime = System.nanoTime();
						String queueElementForLog = queueElement;
						final String LOG_IDENTIFIER = LOG_IDENTIFIER_BASIC +"(" + TIME_BASED_QUEUE_NAME +") "; 
						LOGGER.debug(() -> LOG_IDENTIFIER + "Queue Element recieved :"+ queueElementForLog);
						final Set<String> hostedNotificationErrorOrCompleteMessageSet = GSON.fromJson(queueElement, new TypeToken<HashSet<String>>(){}.getType());
						LOGGER.info(()-> LOG_IDENTIFIER + "Started Processing HostedNotification: " + hostedNotificationErrorOrCompleteMessageSet);

						HostedAsyncSender.notifyToHosted(LOG_IDENTIFIER, hostedNotificationErrorOrCompleteMessageSet);

						LOGGER.debug(()-> LOG_IDENTIFIER + "Finished appending to main queue: " + hostedNotificationErrorOrCompleteMessageSet);
						LOGGER.debug(()-> LOG_IDENTIFIER + "It took " + Duration.ofNanos(System.nanoTime() - startTime) + " to finish the appending.");
					}
					
					LOGGER.trace(LOG_IDENTIFIER_BASIC + "Finished Processing Notification Message Queue {}" , currentTimeInSeconds);

					if(currentTimeInSeconds < Instant.now().getEpochSecond()) {
						currentTimeInSeconds++;
					} else {
						TimeUnit.MILLISECONDS.sleep(10);
					}
				}else {
					LOGGER.debug("Sleeping for 1 second because server is not in active data center ");
					TimeUnit.SECONDS.sleep(1);
				}

			} catch(Throwable e) {
				LOGGER.warn(LOG_IDENTIFIER_BASIC + "Some Unexpected Exception happened in the UpStreamNotificationQueueWatcher: ", e);
				//Just log it and continue.
			}
		}

	}
}
