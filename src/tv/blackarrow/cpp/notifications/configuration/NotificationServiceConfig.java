/**
 * 
 */
package tv.blackarrow.cpp.notifications.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModelDeserializer;

/**
 * @author Amit Kumar Sharma
 *
 */
public class NotificationServiceConfig {
	
	private static final Logger LOGGER = LogManager.getLogger(NotificationServiceConfig.class);
	
	//Default Values.
	private static final int DEFAULT_NOTIFICATION_BUFFER_IN_SECONDS = 5;
	private static final int DEFAULT_BATCH_SIZE_FOR_NOTIFICATION_MESSGAES = 5;
	private static final int DEFAULT_UPSTREAM_NOTIFICATION_QUEUE_WATCHER_THREADS = 5;
	private static final int DEFAULT_HOSTED_NOTIFICATION_THREADS = 5;
	private static final int DEFAULT_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS = 5;
	private static final String DEFAULT_NOTIFICATION_SERVICE_ENABLED = "true";
	
	//Configured Values via System properties
	private static final String CONFIGURED_NOTIFICATION_SERVICE_ENABLED = System.getProperty("enable.notification.service", 
			String.valueOf(DEFAULT_NOTIFICATION_SERVICE_ENABLED));
	private static final String CONFIGURED_NOTIFICATION_MESSAGE_BATCH_SIZE = System.getProperty("upstream.notification.batch.size.perthread", 
			String.valueOf(DEFAULT_BATCH_SIZE_FOR_NOTIFICATION_MESSGAES));
	private static final String CONFIGURED_UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE = System.getProperty("upstream.notification.queue.watcher.threads", 
			String.valueOf(DEFAULT_UPSTREAM_NOTIFICATION_QUEUE_WATCHER_THREADS));
	private static final String CONFIGURED_HOSTED_APP_NOTIFIER_THREAD_POOL_SIZE = System.getProperty("hosted.notification.threads", 
			String.valueOf(DEFAULT_HOSTED_NOTIFICATION_THREADS));
	
	private static final String CONFIGURED_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS = System.getProperty("hosted.notification.timebased.synch.threads", 
			String.valueOf(DEFAULT_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS));
	
	public static final Boolean ENABLE_NOTIFICATION_SERVICE;
	
	public static final int ESS_PROCESSING_TIME_IN_SECS = CppConfigurationBean.getInstance().getLinearPoisProcessingTimeInSeconds() > 0
					? CppConfigurationBean.getInstance().getLinearPoisProcessingTimeInSeconds()
					: DEFAULT_NOTIFICATION_BUFFER_IN_SECONDS;
	public static final int UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE;
	public static final int UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE;
	public static final int HOSTED_NOTIFICATION_THREAD_POOL_SIZE;
	public static final int HOSTED_NOTIFICATION_THREAD_POOL_SYNCH_SIZE;
	
	public static final String JOB_NAME_SEPARATOR = "#@#";
	
	public static final String IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER = JOB_NAME_SEPARATOR + "I";

	public static final int DEFAULT_NUMBER_OF_RETRIES = 3;
	public static final long DEFAULT_RETRY_INTERVAL_IN_MILLIS = 300;
	
	//Set the valid values.
	static {
		
		int notificationMessageBatchSize = DEFAULT_BATCH_SIZE_FOR_NOTIFICATION_MESSGAES;
		try {
			notificationMessageBatchSize = Integer.parseInt(CONFIGURED_NOTIFICATION_MESSAGE_BATCH_SIZE);
		} catch(NumberFormatException numberFormatException) {
			LOGGER.debug(()-> numberFormatException.getMessage() + " for upstream.notification.batch.size.perthread," +
					"Using Default value: " + DEFAULT_BATCH_SIZE_FOR_NOTIFICATION_MESSGAES);
		}
		UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE = notificationMessageBatchSize;
		
		int upstreamNotificationQueueWatcherThreadPoolSize = DEFAULT_UPSTREAM_NOTIFICATION_QUEUE_WATCHER_THREADS;
		try {
			upstreamNotificationQueueWatcherThreadPoolSize = Integer.parseInt(CONFIGURED_UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE);
		} catch(NumberFormatException numberFormatException) {
			LOGGER.debug(()-> numberFormatException.getMessage() + " for upstream.notification.queue.watcher.threads," +
					"Using Default value: " + DEFAULT_UPSTREAM_NOTIFICATION_QUEUE_WATCHER_THREADS);
		}
		UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE = upstreamNotificationQueueWatcherThreadPoolSize;
		
		int hostedNotificationThreadPoolSize = DEFAULT_HOSTED_NOTIFICATION_THREADS;
		try {
			hostedNotificationThreadPoolSize = Integer.parseInt(CONFIGURED_HOSTED_APP_NOTIFIER_THREAD_POOL_SIZE);
		} catch(NumberFormatException numberFormatException) {
			LOGGER.debug(()-> numberFormatException.getMessage() + " for upstream.notification.queue.watcher.threads," +
					"Using Default value: " + DEFAULT_HOSTED_NOTIFICATION_THREADS);
		}
		HOSTED_NOTIFICATION_THREAD_POOL_SIZE = hostedNotificationThreadPoolSize;
		
		boolean enableNotificationService = Boolean.valueOf(DEFAULT_NOTIFICATION_SERVICE_ENABLED);
		try {
			enableNotificationService = Boolean.valueOf(CONFIGURED_NOTIFICATION_SERVICE_ENABLED);
		} catch(NumberFormatException numberFormatException) {
			LOGGER.debug(()-> numberFormatException.getMessage() + " for enable.notification.service," +
					"Using Default value: " + DEFAULT_NOTIFICATION_SERVICE_ENABLED);
		}
		ENABLE_NOTIFICATION_SERVICE = enableNotificationService;
		LOGGER.debug(()-> {
			if(!ENABLE_NOTIFICATION_SERVICE) {
				return new StringBuilder()
						.append("Following values are used by the notification service on this node: \n")
						.append("\t enable.notification.service: " + ENABLE_NOTIFICATION_SERVICE +"\n");
			} else {
				return new StringBuilder()
						.append("Following values are used by the notification service on this node: \n")
						.append("\t enable.notification.service: " + ENABLE_NOTIFICATION_SERVICE +"\n")
						.append("\t upstream.notify.ahead.time.inseconds: " + ESS_PROCESSING_TIME_IN_SECS +"\n")
						.append("\t upstream.notification.batch.size.perthread: " + UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE +"\n")
						.append("\t upstream.notification.queue.watcher.threads: " + UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE +"\n")
						.append("\t hosted.notification.threads: " + HOSTED_NOTIFICATION_THREAD_POOL_SIZE +"\n");
			}
		});
		int timeBasedNotificationThreads = DEFAULT_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS;
		try {
			timeBasedNotificationThreads = Integer.parseInt(CONFIGURED_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS);
		} catch(NumberFormatException numberFormatException) {
			LOGGER.debug(()-> numberFormatException.getMessage() + " for hosted.notification.timebased.synch.threads," +
					"Using Default value: " + DEFAULT_TIMEBASED_HOSTED_NOTIFICATION_SYNCH_THREADS);
		}
		HOSTED_NOTIFICATION_THREAD_POOL_SYNCH_SIZE = timeBasedNotificationThreads;
		LOGGER.info(() -> "UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE=" + UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE + ", UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE="
				+ UPSTREAM_QUEUE_WATCHER_THREAD_POOL_SIZE + ", HOSTED_NOTIFICATION_THREAD_POOL_SIZE=" + HOSTED_NOTIFICATION_THREAD_POOL_SIZE + ", ENABLE_NOTIFICATION_SERVICE="
				+ ENABLE_NOTIFICATION_SERVICE + ", HOSTED_NOTIFICATION_THREAD_POOL_SYNCH_SIZE=" + HOSTED_NOTIFICATION_THREAD_POOL_SYNCH_SIZE
				+ " will be used for notification service.");
	}
	
	//Queues used by the notification service.
	public static final String UPSTREAM_NOTIFICATION_QUEUE_PREFIX = "USNNQ_";//Upstream SPNs Notification New Queue. (Not using v1 as this terminology is not globally defined for linear)
	
	//JSON DE/Serialization Provider
	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(HostedAppEventStatusNotificationModel.class, 
			new HostedAppEventStatusNotificationModelDeserializer()).create();
	
	//Couchbase Operations Service Provider
	public static final DataManager DATAMANAGER = DataManagerFactory.getInstance();
	
}
