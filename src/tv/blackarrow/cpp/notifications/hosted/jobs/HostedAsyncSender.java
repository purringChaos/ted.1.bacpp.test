/**
 * 
 */
package tv.blackarrow.cpp.notifications.hosted.jobs;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DEFAULT_NUMBER_OF_RETRIES;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DEFAULT_RETRY_INTERVAL_IN_MILLIS;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.HOSTED_NOTIFICATION_THREAD_POOL_SIZE;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.HttpMessagePoster;

/*
 * This class should be accessed only via HostedNotificationPosterJob, This class is not visible to other package
 */
class HostedAsyncSender implements Callable<Boolean> {

	private static final Logger LOGGER = LogManager.getLogger(HostedAsyncSender.class);
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(HOSTED_NOTIFICATION_THREAD_POOL_SIZE);
	private static final DataManager DATAMANAGER = DataManagerFactory.getInstance();
	private static final String ERROR_CHECK_CONFIRMED = "#@#ERROR_CHECK#@#CONFIRMED";
	private static final String ERROR_CHECK_COMPLETE = "#@#ERROR_CHECK#@#COMPLETE";

	private String messageToSend = null;
	private int numberOfRetries;
	private long retryIntervalInMillis;
	private EventType eventType;

	private HostedAsyncSender(final String messageToSend, final EventType eventType, final int numberOfRetries, final long retryIntervalInMillis) {
		super();
		this.messageToSend = messageToSend;
		this.eventType = eventType;
		this.numberOfRetries = numberOfRetries;
		this.retryIntervalInMillis = retryIntervalInMillis;
	}

	@Override
	public Boolean call() throws Exception {
		String hostedAppEndPoint = CppConfigurationBean.getInstance().getHostedBlackoutEventStatusUrl();
		if (hostedAppEndPoint == null || hostedAppEndPoint.trim().isEmpty()) {
			LOGGER.error("Following property is not properly configured in cpp_bean.xml : hostedAppEventUpdateURL");
			throw new RuntimeException("Following property is not properly configured in cpp_bean.xml : hostedAppEventUpdateURL");
		}
		if (hostedAppEndPoint.endsWith("/")) {
			hostedAppEndPoint += (eventType == null ? EventType.I02.name() : eventType.name());
		} else {
			hostedAppEndPoint += ("/" + (eventType == null ? EventType.I02.name() : eventType.name()));
		}

		if (!HttpMessagePoster.postMessage(hostedAppEndPoint, numberOfRetries, retryIntervalInMillis, messageToSend)) {
			//We have already logged the message above
			//LOGGER.error("Failed to synchronize hosted app data with runtime.");
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	private static Future<Boolean> notify(final String messageToSend, final EventType eventType, final String errorMessageToSend, final String actionToSend, final int numberOfRetries,
			final long retryIntervalInMillis) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Sending following message to the hosted APP: " + messageToSend);
		}
		return threadPool.submit(new HostedAsyncSender(messageToSend, eventType, numberOfRetries, retryIntervalInMillis));
	}

	public static void notifyToHosted(final String LOG_IDENTIFIER, Set<String> hostedNotificationMessageKeys) {
		LOGGER.debug(() -> LOG_IDENTIFIER + " Keys from Queue: " + hostedNotificationMessageKeys);
		hostedNotificationMessageKeys.forEach(inputKeyFromHJNQ -> {
			String inputKey = inputKeyFromHJNQ;
			LOGGER.debug(() -> LOG_IDENTIFIER + " Input element: " + inputKeyFromHJNQ);

			if (inputKey.endsWith(NotificationServiceConfig.IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER)) {
				inputKey = inputKey.substring(0, inputKey.lastIndexOf(NotificationServiceConfig.IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER));//Remove that Immediate Flag
			}

			String hostedNotificationInfo = DATAMANAGER.get(inputKey);
			if (hostedNotificationInfo == null || hostedNotificationInfo.trim().isEmpty()) {
				LOGGER.debug(() -> LOG_IDENTIFIER + " No hosted notification object found.");
			} else {
				LOGGER.debug(() -> LOG_IDENTIFIER + " Hosted notification object found : " + hostedNotificationInfo);
				HostedAppEventStatusNotificationModel hostedAppEventModalBase = GSON.fromJson(hostedNotificationInfo, HostedAppEventStatusNotificationModel.class);
				String messageToSend = StringUtils.EMPTY;
				LOGGER.debug(() -> LOG_IDENTIFIER + " Deserialized Hosted notification object : " + hostedAppEventModalBase.toString());
				hostedAppEventModalBase.setHostedNotificationTime(System.currentTimeMillis());
				LOGGER.debug(() -> LOG_IDENTIFIER + " sending hosted notification message for Event type: " + hostedAppEventModalBase.getEventType());
				if(EventType.I02.equals(hostedAppEventModalBase.getEventType())) {
					HostedAppEventStatusI02NotifyModel hostedAppEventModal = (HostedAppEventStatusI02NotifyModel)hostedAppEventModalBase;
					if(inputKey.contains(ERROR_CHECK_CONFIRMED)) {
						String blackoutSignalId = StringUtils.substringBefore(inputKey, ERROR_CHECK_CONFIRMED);
						ConfirmedPlacementOpportunity cpo = DATAMANAGER.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutSignalId);
						if(cpo == null) {
							hostedAppEventModal.setEventAction(EventAction.ERROR);
						} else {
							long actualUTCStartTime = HostedAppEventStatusNotificationModel.getActualBlackoutStartTime(cpo);
							if (actualUTCStartTime==0) {
								BlackoutEvent blackoutEventBySignalId = DATAMANAGER.getSingleBlackoutEvent(blackoutSignalId);
								actualUTCStartTime = blackoutEventBySignalId.getUtcStartTime();
							}
							hostedAppEventModal.setActualUTCStartTime(actualUTCStartTime);
						}
					} else if(inputKey.contains(ERROR_CHECK_COMPLETE)) {
						//ERROR_CHECK_COMPLETE JOB (GETS SCHEDULE ONLY WHEN IP AQs and The CLUSTER is either IP or MIXED delivery type), We are trying to populate ActualSTART/END from common places.
						String blackoutSignalId = StringUtils.substringBefore(inputKey, ERROR_CHECK_COMPLETE);
						ConfirmedPlacementOpportunity cpo = DATAMANAGER.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutSignalId);
						if(cpo == null) {
							hostedAppEventModal.setEventAction(EventAction.ERROR);
				} else {
							long actualUTCStopTime = HostedAppEventStatusNotificationModel.getActualBlackoutStopTime(cpo);
							if (actualUTCStopTime==0) {
								BlackoutEvent blackoutEventBySignalId = DATAMANAGER.getSingleBlackoutEvent(blackoutSignalId);
								actualUTCStopTime = blackoutEventBySignalId.getUtcStopTime();
							}
							hostedAppEventModal.setActualUTCStopTime(actualUTCStopTime);
						}
					}
					messageToSend = GSON.toJson(hostedAppEventModal, HostedAppEventStatusI02NotifyModel.class);
					HostedAsyncSender.notify(messageToSend, (hostedAppEventModal.getEventType().equals(EventType.SCTE224) ? EventType.SCTE224 : EventType.I02), null,
							hostedAppEventModal.getEventAction().name(), DEFAULT_NUMBER_OF_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);
				} else {
					messageToSend = GSON.toJson(hostedAppEventModalBase, HostedAppEventStatusScte224NotifyModel.class);
					HostedAsyncSender.notify(messageToSend, (hostedAppEventModalBase.getEventType().equals(EventType.SCTE224) ? EventType.SCTE224 : EventType.I02), null,
							((HostedAppEventStatusScte224NotifyModel)hostedAppEventModalBase).getEventStatus().name(), DEFAULT_NUMBER_OF_RETRIES, DEFAULT_RETRY_INTERVAL_IN_MILLIS);
				}
				
				

				LOGGER.debug(() -> LOG_IDENTIFIER + " Hosted Notification sent to hosted app for :" + inputKeyFromHJNQ);
			}
		});
	}

}
