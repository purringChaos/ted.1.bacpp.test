/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.executor;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class ScheduledNotificationExecutor {

	private static final Logger LOG = LogManager.getLogger(ScheduledNotificationExecutor.class);
	public static final Executor EXECUTOR_FOR_NOTIFICATION_QUEUE_WATCHERS = Executors.newFixedThreadPool(UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE);
	
	public static void initializeExecutors() {
		if(NotificationServiceConfig.ENABLE_NOTIFICATION_SERVICE) {
			LOG.debug(()-> "Intializing executors." );
			for(int numberOfWatcherThreads=0 ; numberOfWatcherThreads < UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE; numberOfWatcherThreads++) {
				EXECUTOR_FOR_NOTIFICATION_QUEUE_WATCHERS.execute(new UpStreamNotificationQueueWatcher());
			}
			LOG.debug(()-> "Intializing executors finished succesfully." );
		}
	}
	
}
