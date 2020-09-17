package tv.blackarrow.cpp.notifications.hosted.executor;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.HOSTED_NOTIFICATION_THREAD_POOL_SIZE;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.hosted.jobs.HostedNotificationSynchQueuesWatcherJob;

public class HostedNotificationExecutorsService {
	
	private static final Logger LOGGER = LogManager.getLogger(HostedNotificationExecutorsService.class);
	//private static final ExecutorService mainQueueThreadPool = Executors.newFixedThreadPool(HOSTED_NOTIFICATION_THREAD_POOL_SIZE);
	private static final ExecutorService timeBasedQueueThreadPool = Executors.newFixedThreadPool(HOSTED_NOTIFICATION_THREAD_POOL_SIZE);
	
	public static void initializeExecutors() {
		LOGGER.debug(()-> "Intializing executors for hosted notification." );
		/*HMQ Implementation Gone
		 * for(int numberOfWatcherThreads=0 ; numberOfWatcherThreads < HOSTED_NOTIFICATION_THREAD_POOL_SIZE; numberOfWatcherThreads++) {
			mainQueueThreadPool.execute(new HostedNotificationPosterJob());
		}*/
		
		for(int timeBasedThreadPoolIndex = 0; timeBasedThreadPoolIndex < HOSTED_NOTIFICATION_THREAD_POOL_SIZE; timeBasedThreadPoolIndex++) {
			timeBasedQueueThreadPool.execute(new HostedNotificationSynchQueuesWatcherJob());
		}
		
		LOGGER.debug(()-> "Intializing executors finished succesfully." );
	}

}
