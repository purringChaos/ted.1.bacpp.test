/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationsSchedulerFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.NotificationQueueMessagesBuilder;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;
import tv.blackarrow.cpp.notifications.upstream.scheduler.ScheduleNotificationInfo;
import tv.blackarrow.cpp.utils.CppConstants.ESSNodeDeliveryType;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.EventAction;

/**
 *
 */
public class EventRuntimeNotificationsHandler {

	private static final Logger LOG = LogManager.getLogger(EventRuntimeNotificationsHandler.class);
	private static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();

	public static void cancelUpStreamQamSwitchBackJob(BlackoutEvent blackout, AcquisitionPoint aqpt) {
		if (blackout == null || aqpt == null) {
			return;
		}		
		String scheduledNotificationId = blackout.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + aqpt.getAcquisitionPointIdentity()
				+ NotificationServiceConfig.JOB_NAME_SEPARATOR + EventAction.COMPLETE.name();
		NotificationSchedulerService.getInstance().cancelScheduledNotificationByJobId(blackout.getSignalId(), scheduledNotificationId);
	}

	public static void rescheduleUpStreamQamSwitchBackJob(BlackoutEvent blackout, AcquisitionPoint aqpt) {
		cancelUpStreamQamSwitchBackJob(blackout, aqpt);//First cancel Qam Switchback
		long currentSystemTimeInMillis = System.currentTimeMillis();

		if (blackout == null || aqpt == null) {
			return;
		}
		//Prepare message for deleted Medias.
		ConfirmedPlacementOpportunity acqCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), blackout.getSignalId());
		if (BlackoutEvent.getActualBlackoutStopTime(acqCPO, blackout) < currentSystemTimeInMillis) {
			LOG.info(() -> "Blackout (" + blackout.getSignalId() + ") " + blackout.getEventId() + "is already expired so skipping any rescheduling for this Blackout.");
			return;
		}
		final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();
		NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance()
				.getNotificationMessageBuilder(aqpt.isIpAcquisitionPoint());
		notificationQueueMessagesBuilder.rebuildForLiveEventsUpstreamNotification(currentSystemTimeInMillis, blackout, aqpt, notificationMessagesByScheduleTime);

		List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
		if (!notificationMessagesByScheduleTime.isEmpty()) {
			notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
				notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
			});
			NotificationSchedulerService.getInstance().scheduleNotification(notificationList, blackout.getSignalId() + "(" + blackout.getEventId() + ")");
			Set<Integer> existingScheduleTime = DATA_MANAGER.getEventScheduledNotificationQueueTimes(blackout.getSignalId());
			existingScheduleTime.addAll(notificationMessagesByScheduleTime.keySet());
			DATA_MANAGER.saveEventScheduledNotificationQueueTimes(blackout.getSignalId(), existingScheduleTime);
		}
	}

	private static int getMaxProgramStartBufferOfAllAQ(final List<AcquisitionPoint> acqPointsForAGivenFeed) {
		return acqPointsForAGivenFeed.stream().mapToInt(AcquisitionPoint::getProgramStartBuffer).max().orElse(0);
	}
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//----------------------------------------ALL HOSTED INSTANT MESSAGE THAT ARE USED BY RUNTIME ARE DEFINED BELOW-------------------------------------------------------------
	public static void rescheduleHostedErrorCheckCompleteJob(BlackoutEvent blackout, AcquisitionPoint aqpt) {
		NotificationSchedulerService.getInstance().reScheduledHostedNotificationByJobId(blackout.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR
				+ aqpt.getAcquisitionPointIdentity() + NotificationServiceConfig.JOB_NAME_SEPARATOR + EventAction.COMPLETE.name());
		if (aqpt.isIpAcquisitionPoint()) {
			if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().equals(ESSNodeDeliveryType.IP)) {
				LOG.info(()->"This cluster is serving only " + CppUtil.getClusterType().name()
						+ " traffic. Therefore, no error_check messages for hosted needs to be scheduled at event level at this node.");
				return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
			}
			NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance().getNotificationMessageBuilder(true);
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap = new HashMap<>();
			Set<String> acquisitionPointIdsForThisBlackout = DATA_MANAGER.getFeedToAcquistionMap(blackout.getFeedExtRef());
			List<AcquisitionPoint> acquisitionPoints = DATA_MANAGER.getAcquisitionPoints(acquisitionPointIdsForThisBlackout);
			notificationQueueMessagesBuilder.buildForLiveEventHostedNotification(blackout, getMaxProgramStartBufferOfAllAQ(acquisitionPoints), hostedNotificationMap, aqpt);
			HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(false).scheduleHostedNotification(hostedNotificationMap);
		}
	}

	public static void notifyStatusToHostedImmediately(ConfirmedPlacementOpportunity acquisitionPointCPO, final BlackoutEvent blackoutEvent, String message) {
		Map<String, HostedAppEventStatusNotificationModel> hostedNotificationQueueMessages = new HashMap<>();
		ConfirmedPlacementOpportunity originalEventCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(acquisitionPointCPO.getAcquisitionPointIdentity(),
				blackoutEvent.getSignalId());
		EventAction action = EventAction.valueOf(message);
		
		HostedAppEventStatusI02NotifyModel statusUpdate = new HostedAppEventStatusI02NotifyModel(blackoutEvent.getFeedExtRef(), blackoutEvent.getEventTypeName(),
				blackoutEvent.getUtcStartTime(), blackoutEvent.getUtcStopTime(), blackoutEvent.getSignalId(), acquisitionPointCPO.getAcquisitionPointIdentity(),
				blackoutEvent.getEventId());
		LOG.info(() -> "Notifying status to hosted for " + blackoutEvent + " for Acquisition Point confirmation Object " + acquisitionPointCPO + ", message " + statusUpdate);
		LOG.debug(() -> "AqCPO again " + originalEventCPO);

		statusUpdate.setEventAction(action);
		statusUpdate.setEventType(EventType.I02);
		if (EventAction.CONFIRMED.equals(action)) {
			long actualBlackoutStartTime = BlackoutEvent.getActualBlackoutStartTime(originalEventCPO, blackoutEvent);
			statusUpdate.setActualUTCStartTime(actualBlackoutStartTime);
		}
		if (EventAction.COMPLETE.equals(action)) {
			long actualBlackoutStartTime = BlackoutEvent.getActualBlackoutStartTime(originalEventCPO, blackoutEvent);
			long actualBlackoutStopTime = BlackoutEvent.getActualBlackoutStopTime(originalEventCPO, blackoutEvent);
			statusUpdate.setActualUTCStartTime(actualBlackoutStartTime);
			statusUpdate.setActualUTCStopTime(actualBlackoutStopTime);
		}
		statusUpdate.setHostedNotificationScheduleTime(-1);

		String notificationKey = blackoutEvent.getSignalId() + NotificationServiceConfig.JOB_NAME_SEPARATOR + acquisitionPointCPO.getAcquisitionPointIdentity()
				+ NotificationServiceConfig.JOB_NAME_SEPARATOR + action;
		hostedNotificationQueueMessages.put(notificationKey, statusUpdate);
		LOG.debug(() -> "Sending the status update to hosted as " + notificationKey + " and message: " + hostedNotificationQueueMessages);

		HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(true).scheduleHostedNotification(hostedNotificationQueueMessages);
	}	

}
