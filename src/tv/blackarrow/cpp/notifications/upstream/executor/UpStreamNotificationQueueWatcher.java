/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.executor;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageFactory;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;

/**
 * @author Amit Kumar Sharma
 *
 */
public class UpStreamNotificationQueueWatcher implements Runnable {

	private static final Logger LOG = LogManager.getLogger(UpStreamNotificationQueueWatcher.class);

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		long currentlyProcessingQueueTimeInSeconds = Instant.now().getEpochSecond();
		while (true) {
			try {
				if (DataManagerFactory.getInstance().isServerInActiveDataCenter()) {
					LOG.trace("Thread id : "+ Thread.currentThread().getId()+ " Started Processing Notification Message Queue {}", currentlyProcessingQueueTimeInSeconds);
					String upstreamNotificationQueue = NotificationSchedulerService.getUpstreamNotificationQueueName(currentlyProcessingQueueTimeInSeconds);

					String queueElement = null;
					while ((queueElement = DATAMANAGER.popFromQueue(upstreamNotificationQueue)) != null) {
						long startTime = System.nanoTime();
						final Set<String> notificationMessageRefs = GSON.fromJson(queueElement, new TypeToken<HashSet<String>>() {
						}.getType());
						LOG.debug(() -> "Thread id : "+ Thread.currentThread().getId()+ " Started Processing Notification for following Ids: " + notificationMessageRefs);
						notificationMessageRefs.parallelStream().forEach(notificationMessageRef -> {
							LOG.info(() -> "Thread id : "+ Thread.currentThread().getId()+ " Started Processing Notification: \"" + notificationMessageRef + "\".");
							String notificationMessageJSON = DATAMANAGER.get(notificationMessageRef);
							if (notificationMessageJSON == null || notificationMessageJSON.trim().isEmpty()) {
								LOG.info(() -> "Thread id : "+ Thread.currentThread().getId()+ " No Scheduled Message found for the message reference: " + notificationMessageRef);
							} else {
								LOG.info(() -> "Thread id : " + Thread.currentThread().getId() + " Processing notificationMessageJSON : " + notificationMessageJSON);

								final NotificationMessage notificationMessage = GSON.fromJson(notificationMessageJSON, NotificationMessage.class);

								if (notificationMessage != null && notificationMessage.getScheduledNotificationId() != null) {
									String[] messageBreak = notificationMessage.getScheduledNotificationId().split("#@#");
									if (messageBreak.length >= 2 && messageBreak[2].equals("CONFIRMED")) {
										LOG.info(() -> "PROGRAM_START_JOB: \"" + notificationMessageRef + "\".");
									} else if (messageBreak.length >= 2 && messageBreak[2].equals("COMPLETE")) {
										LOG.info(() -> "PROGRAM_END_JOB: \"" + notificationMessageRef + "\".");
									} else {
										LOG.info(() -> "Thread id : " + Thread.currentThread().getId() + " getScheduledNotificationId() is not valid  ");
									}
								} else {
									LOG.error(() -> "Thread id : " + Thread.currentThread().getId() + " NotificationMessage details : " + notificationMessage.toString());
								}
								notificationMessage
										.setOnFlyCreatedScheduledUpStreamMessage(UpStreamNotificationMessageFactory.getUpstreamNoitificationMessage(notificationMessage));
								if (StringUtils.isBlank(notificationMessage.getOnFlyCreatedScheduledUpStreamMessage())) {
									LOG.error(() -> "Thread id : " + Thread.currentThread().getId() + " 'scheduledUpStreamMessage' can not be null or empty for SignalID:" + notificationMessage.getEventSignalId()
											+ " AcquistionPointID: " + notificationMessage.getStreamId() + ". Please verify.");
									return;
								}
								NotificationSenderFactory.getInstance().getSender(notificationMessage.getEventType(), notificationMessage.getUpStreamNotificationMessageType())
										.performSending(notificationMessage);

							}
							LOG.debug(() -> "Thread id : " + Thread.currentThread().getId() + " Finished processing for the following Message Ref: \"" + notificationMessageRef + "\".");
						});
						LOG.info(() -> "Thread id : " + Thread.currentThread().getId() + " It took " + Duration.ofNanos(System.nanoTime() - startTime) + " to process "+notificationMessageRefs.size()+" notifications.");
						LOG.debug(() -> "Thread id : " + Thread.currentThread().getId() + " It took " + Duration.ofNanos(System.nanoTime() - startTime) + " to process "+notificationMessageRefs+" notifications.");
					}
					LOG.trace("Thread id : " + Thread.currentThread().getId() + " Finished Processing Notification Message Queue {}", currentlyProcessingQueueTimeInSeconds);
					if (currentlyProcessingQueueTimeInSeconds < Instant.now().getEpochSecond()) {
						currentlyProcessingQueueTimeInSeconds++;
					} else {
						TimeUnit.MILLISECONDS.sleep(10);
					}
				} else {
					//Check every second
					TimeUnit.SECONDS.sleep(1);
				}
			} catch (Throwable anyExceptionOrError) {
				LOG.warn(() -> "Thread id : " + Thread.currentThread().getId() + " Some Unexpected Exception happened in the UpStreamNotificationQueueWatcher: ", anyExceptionOrError);
				//Just log it and continue.
			}
		}
	}

}
