/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.i02;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationsSchedulerFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.builder.NotificationQueueMessagesBuilder;
import tv.blackarrow.cpp.notifications.upstream.scheduler.CancelScheduledNotificationByEventSignalIdInfo;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;
import tv.blackarrow.cpp.notifications.upstream.scheduler.ScheduleNotificationInfo;
import tv.blackarrow.cpp.utils.CppConstants.ESSNodeDeliveryType;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.EventAction;

/**
 *
 */
public class EventCRUDNotificationsHandler {

	private static final Logger LOG = LogManager.getLogger(EventCRUDNotificationsHandler.class);
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	private static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();

	public static void handleNotification(final List<AcquisitionPoint> acqPoints, final EventAction eventAction, final List<BlackoutEvent> passedBlackouts, boolean sendImmediately, EventCRUDNotificationHandlerInvokerType invoker) {
		final long currentSystemTime = System.currentTimeMillis();
		final int currentSystemTimeInSecs = (int) TimeUnit.MILLISECONDS.toSeconds(currentSystemTime);
		if (passedBlackouts == null || passedBlackouts.isEmpty() || acqPoints == null || acqPoints.isEmpty()) {
			return;
		}
		switch (eventAction) {
		case START_NOW://For Future
		case CREATE://For Future
			if (DEBUG_ENABLED)
				LOG.debug(()->"1.Received following new blackout in the realtime CREATE request:\n " + passedBlackouts.toString());
			handleNewBlackoutAdditionNotifications(acqPoints, passedBlackouts, currentSystemTime, currentSystemTimeInSecs, sendImmediately);//immediate Start is False

			break;
		case UPDATE://This EVENT_ACTION IS USED during RealtimeUpdate/As well rule loading
			//May come for Inflight/Future
			if (DEBUG_ENABLED)
				LOG.debug(()->"1. Received following existing blackout in the realtime UPDATE request:\n " + passedBlackouts.toString());
			handleBlackoutUpdateNotifications(acqPoints, passedBlackouts, currentSystemTime, currentSystemTimeInSecs, sendImmediately, invoker);

			break;
		case DELETE://comes for future event
			if (DEBUG_ENABLED)
				LOG.debug(()->"1.Received following existing blackout in the realtime DELETE request:\n " + passedBlackouts.toString());
			handleFutureBlackoutDeletionNotifications(acqPoints, passedBlackouts, currentSystemTime, currentSystemTimeInSecs);

			break;
		case STOP_NOW://Comes for Inflight Event
			if (DEBUG_ENABLED)
				LOG.debug(()->"1.Received following existing blackout in the realtime STOP_NOW request:\n " + passedBlackouts.toString());
			handleInflightBlackoutDeletionNotifications(acqPoints, passedBlackouts, currentSystemTime, currentSystemTimeInSecs);

			break;
		default:
			break;
		}

	}

	/**
	 * Schedules the Out of Band Notifications for the given Blackout.
	 * Currently there are two kind of notifications that are supported on Blackout
	 * 1. OOB Notification
	 * 2. Immediate Start/End notification sent on out of band.
	 * 
	 * Both the notifications have different message structures and different scheduling requirements.
	 * 
	 * @param acqPointsForAGivenFeed Acquisition Points List that are associated with the Feed that is associated to all the Medias in the given Blackout List.
	 * @param blackoutsForAGivenFeed Medias belonging to a single feed that needs scheduling.
	 * @param currentSystemTime current system time up to millis.
	 * @param currentSystemTimeInSecs current system time up to last second.
	 * @param eventAction the action that has taken place with the event.
	 * 
	 * @throws DatatypeConfigurationException
	 */
	private static void handleNewBlackoutAdditionNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<BlackoutEvent> blackoutsForAGivenFeed,
			final long currentSystemTime, final int currentSystemTimeInSecs, boolean immediateStart) {
		if (blackoutsForAGivenFeed == null || blackoutsForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for newly added Medias.
		blackoutsForAGivenFeed.forEach(blackout -> {
			if (blackout == null) {
				return;
			}
			final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();
			acqPointsForAGivenFeed.forEach(acquisitionPoint -> {
				//Doing it here, because in future we may want to support both IP/QAM for blackout. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Blackout(New)--> " + blackout.getEventId() + "(" + blackout.getSignalId() + "):" + " This cluster is serving only "
								+ CppUtil.getClusterType().name() + " traffic. Therefore, not scheduling any notifications for acquisitionPoint = "
								+ acquisitionPoint.getAcquisitionPointIdentity() + " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
		
				boolean shouldSkipScheduling = checkForOpenEndedFeed(immediateStart, acquisitionPoint);
				if (shouldSkipScheduling) {
					return;
				}
				NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance()
						.getNotificationMessageBuilder(acquisitionPoint.isIpAcquisitionPoint());
				notificationQueueMessagesBuilder.buildForPendingEventsUpstreamNotification(currentSystemTimeInSecs, immediateStart, blackout, acquisitionPoint,
						notificationMessagesByScheduleTime);
			});

			//Create Event Level ERROR_CHECK and COMPLETE hosted notification Job (If Feed has IP AQs)
			scheduleHostedNotificationIfIPAcqExists(acqPointsForAGivenFeed, blackout, immediateStart, currentSystemTimeInSecs);

			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if (!notificationMessagesByScheduleTime.isEmpty()) {
				notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, blackout.getSignalId() + "(" + blackout.getEventId() + ")");
				DATA_MANAGER.saveEventScheduledNotificationQueueTimes(blackout.getSignalId(), notificationMessagesByScheduleTime.keySet());
			}
		});
	}

	private static boolean checkForOpenEndedFeed(boolean immediateStart, AcquisitionPoint acquisitionPoint) {
		boolean shouldSkipScheduling = false;
		//Open Ended feed no job needs to be scheduled(However, specific STARTNOW could be scheduled as conditioned below).
		if (acquisitionPoint.isFeedAllowsOpenEndedBlackouts()) {
			if (acquisitionPoint.isQAMAcquisitionPoint()) {////6.5.6 PRODISSUE-1120 Patch,Please understand this p4 checkin should not be merged to 7.2 branch.
				// On Open Ended Feed, for QAMInband acquisition Points do not send any start/Switch Notification. 
				shouldSkipScheduling = true;
			} else {
				if (!immediateStart) {//If not immediate start then just return.
					shouldSkipScheduling = true;
				} else if (immediateStart && ESAMHelper.isOpenEndedBlackoutOverlapping(DATA_MANAGER, acquisitionPoint.getAcquisitionPointIdentity())) {
					shouldSkipScheduling = true;
				}
				//Only immediateStart with NOOverlapp will proceed further to notifictation queue
			}
		}
		return shouldSkipScheduling;
	}

	private static void handleBlackoutUpdateNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<BlackoutEvent> blackoutsUpdatedForAGivenFeed,
			final long currentSystemTime, final int currentSystemTimeInSecs, boolean immediateSend, EventCRUDNotificationHandlerInvokerType invoker) {

		if (blackoutsUpdatedForAGivenFeed == null || blackoutsUpdatedForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for Updated Medias.
		blackoutsUpdatedForAGivenFeed.forEach(blackout -> {
			if (blackout == null) {
				return;
			}
			if (currentSystemTime > blackout.getUtcStopTime()) {
				LOG.info(() -> "Blackout " + blackout.getEventId() + "(" + blackout.getSignalId()
						+ ") is already expired, so skipping the notifications scheduling for this Blackout.");
				return;
			}
			//1. First cancel all the future pending notifications on this Blackout.
			final Set<Integer> scheduledTimes = DATA_MANAGER.getEventScheduledNotificationQueueTimes(blackout.getSignalId());
			if (!scheduledTimes.isEmpty()) {
				final Set<Integer> scheduledTimesRemoved = new HashSet<>();
				final List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos = new ArrayList<>();
				scheduledTimes.forEach(scheduledTime -> {
					if (currentSystemTimeInSecs < scheduledTime) {
						cancelScheduledNotificationInfos.add(new CancelScheduledNotificationByEventSignalIdInfo(scheduledTime, blackout.getSignalId()));
						scheduledTimesRemoved.add(scheduledTime);
					}
				});
				NotificationSchedulerService.getInstance().cancelScheduledNotificationBySignalId(cancelScheduledNotificationInfos);
				scheduledTimes.removeAll(scheduledTimesRemoved);
			}

			//2. Now prepare new pending notifications for this Blackout
			final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();
			acqPointsForAGivenFeed.forEach(acquisitionPoint -> {
				
				//On Open Ended Feed there is Nothing that we need to unschedule, because only conditionally start_now job was possible and that is anyways
				//gone.
				boolean shouldSkipScheduling = checkForOpenEndedFeed(immediateSend, acquisitionPoint);
				if (shouldSkipScheduling) {
					return;
				}
				//Doing it here, because in future we may want to support both IP/QAM for blackout. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Blackout(Update)--> " + blackout.getSignalId() + "(" + blackout.getEventId() + "):" + " This cluster is serving only "
								+ CppUtil.getClusterType().name() + " traffic. Therefore, not scheduling any notifications for acquisitionPoint = "
								+ acquisitionPoint.getAcquisitionPointIdentity() + " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
				if (acquisitionPoint.isFeedAllowsOpenEndedBlackouts()
						&& ((acquisitionPoint.isIpAcquisitionPoint() && !immediateSend) || acquisitionPoint.isQAMAcquisitionPoint())) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Scheduling is not applicable for openEnded blackouts if it is not the start now case. ");
					}
					return;
				}
				NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance()
						.getNotificationMessageBuilder(acquisitionPoint.isIpAcquisitionPoint());
				
				//Option1: Updated event is In-Flight 
				if (isInflightEvent(acquisitionPoint, blackout, currentSystemTime)) {
					//Update on Inflight Event is never immediate
					switch (invoker) {
					case COMPILED_RULE_LOADING:
						//In Compilation flow, we do not need to send any notification to IP, it's just QAM swithcback rescheduling
						if (acquisitionPoint.isQAMAcquisitionPoint()) {
							notificationQueueMessagesBuilder.buildForLiveEventsUpstreamNotification(currentSystemTimeInSecs, false, blackout, acquisitionPoint,
									notificationMessagesByScheduleTime, EventAction.UPDATE);
						}
						break;
					case REAL_TIME_MESSAGE:
						//In-flight event will be reschedule for QAM switchback/and Notify to IP about time change immediately
						//For IP It's immediate
						//For QAM it's just rescheduling of switch back, no immediate notification
						notificationQueueMessagesBuilder.buildForLiveEventsUpstreamNotification(currentSystemTimeInSecs, false, blackout, acquisitionPoint,
								notificationMessagesByScheduleTime, EventAction.UPDATE);
						break;
					default:
						break;
					}

				} //Option2: Updated event is In Future  
				else {
					//In future(We are not trying to see window of 30 sec/notification buffer/ for simplicity we are scheduling all future. Generally, START_NOW/STOP_NOW was never meant to be handled via compilation. And it will continue to be same.
					if (blackout.getUtcStartTime() > currentSystemTime || immediateSend) {//Some Start Now That is coming from Realtime update may have UTC time<currentTime, so added additional or condition
						notificationQueueMessagesBuilder.buildForPendingEventsUpstreamNotification(currentSystemTimeInSecs, immediateSend, blackout, acquisitionPoint,
								notificationMessagesByScheduleTime);
					}
				}
			});

			//Create Event Level ERROR_CHECK and COMPLETE hosted notification Job (If Feed has IP AQs)
			scheduleHostedNotificationIfIPAcqExists(acqPointsForAGivenFeed, blackout, false, currentSystemTimeInSecs);

			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if (!notificationMessagesByScheduleTime.isEmpty()) {
				notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, blackout.getSignalId() + "(" + blackout.getEventId() + ")");
				DATA_MANAGER.saveEventScheduledNotificationQueueTimes(blackout.getSignalId(), notificationMessagesByScheduleTime.keySet());
			}

		});
	}

	private static void handleFutureBlackoutDeletionNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<BlackoutEvent> blackoutsDeletedForAGivenFeed,
			final long currentSystemTime, final int currentSystemTimeInSecs) {
		if (blackoutsDeletedForAGivenFeed == null || blackoutsDeletedForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for deleted Medias.
		blackoutsDeletedForAGivenFeed.forEach(blackout -> {
			//1. First cancel all the pending notifications on this Blackout.
			final Set<Integer> scheduledTimes = DATA_MANAGER.getEventScheduledNotificationQueueTimes(blackout.getSignalId());
			if (!scheduledTimes.isEmpty()) {
				final Set<Integer> scheduledTimesRemoved = new HashSet<>();
				final List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos = new ArrayList<>();
				scheduledTimes.forEach(scheduledTime -> {
					cancelScheduledNotificationInfos.add(new CancelScheduledNotificationByEventSignalIdInfo(scheduledTime, blackout.getSignalId()));
					scheduledTimesRemoved.add(scheduledTime);
				});
				NotificationSchedulerService.getInstance().cancelScheduledNotificationBySignalId(cancelScheduledNotificationInfos);
				scheduledTimes.removeAll(scheduledTimesRemoved);
			}

			//Remove Event Level ERROR_CHECK and COMPLETE hosted notification Job
			List<AcquisitionPoint> containsIPAcqPoints = acqPointsForAGivenFeed.stream().filter(acquisitionPoint -> acquisitionPoint.isIpAcquisitionPoint())
					.collect(Collectors.toList());
			if (containsIPAcqPoints != null && !containsIPAcqPoints.isEmpty()) {
				HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(false).cancelScheduledHostedNotification(Arrays.asList(blackout.getSignalId()));
			}
			DATA_MANAGER.deleteEventScheduledNotificationQueueTimes(blackout.getSignalId());
		});
	}

	private static void handleInflightBlackoutDeletionNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<BlackoutEvent> blackoutsDeletedForAGivenFeed,
			final long currentSystemTime, final int currentSystemTimeInSecs) {
		if (blackoutsDeletedForAGivenFeed == null || blackoutsDeletedForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for deleted Medias.
		blackoutsDeletedForAGivenFeed.forEach(blackout -> {

			//1. First cancel all the pending notifications on this Blackout.
			final Set<Integer> scheduledTimes = DATA_MANAGER.getEventScheduledNotificationQueueTimes(blackout.getSignalId());
			if (!scheduledTimes.isEmpty()) {
				final Set<Integer> scheduledTimesRemoved = new HashSet<>();
				final List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos = new ArrayList<>();
				scheduledTimes.forEach(scheduledTime -> {
					if (currentSystemTimeInSecs < scheduledTime) {
						cancelScheduledNotificationInfos.add(new CancelScheduledNotificationByEventSignalIdInfo(scheduledTime, blackout.getSignalId()));
						scheduledTimesRemoved.add(scheduledTime);
					}
				});
				NotificationSchedulerService.getInstance().cancelScheduledNotificationBySignalId(cancelScheduledNotificationInfos);
				scheduledTimes.removeAll(scheduledTimesRemoved);
			}

			//2. Now prepare new notifications only for the in progress Blackout.				
			Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();

			acqPointsForAGivenFeed.forEach(acquisitionPoint -> {
				//Doing it here, because in future we may want to support both IP/QAM for blackout. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info("Blackout(Delete)--> " + blackout.getSignalId() + "(" + blackout.getEventId() + "):" + " This cluster is serving only "
								+ CppUtil.getClusterType().name() + " traffic. Therefore, not scheduling any notifications for acquisitionPoint = "
								+ acquisitionPoint.getAcquisitionPointIdentity() + " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
				if (acquisitionPoint.isFeedAllowsOpenEndedBlackouts()) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Scheduling is not applicable for openEnded blackouts.");
					}
					return;
				}
				
				//Future Delete will already be handled by above removeAll, below code will take care of rescheduling the switchback/Ip Notification
				NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance()
						.getNotificationMessageBuilder(acquisitionPoint.isIpAcquisitionPoint());
				notificationQueueMessagesBuilder.buildForLiveEventsUpstreamNotification(currentSystemTimeInSecs, true, blackout, acquisitionPoint,
						notificationMessagesByScheduleTime, EventAction.STOP_NOW);

			});

			//Create Event Level ERROR_CHECK and COMPLETE hosted notification Job (If Feed has IP AQs)
			scheduleHostedNotificationIfIPAcqExists(acqPointsForAGivenFeed, blackout, false, currentSystemTimeInSecs);

			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if (!notificationMessagesByScheduleTime.isEmpty()) {
				notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				scheduledTimes.addAll(notificationMessagesByScheduleTime.keySet());
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, blackout.getSignalId() + "(" + blackout.getEventId() + ")");
			}
			DATA_MANAGER.saveEventScheduledNotificationQueueTimes(blackout.getSignalId(), scheduledTimes);
		});
	}

	private static int getMaxProgramStartBufferOfAllAQ(final List<AcquisitionPoint> acqPointsForAGivenFeed) {
		return acqPointsForAGivenFeed.stream().mapToInt(AcquisitionPoint::getProgramStartBuffer).max().orElse(0);
	}

	private static void scheduleHostedNotificationIfIPAcqExists(final List<AcquisitionPoint> acqPointsForAGivenFeed, BlackoutEvent blackout, boolean immediateStart, int currentSystemTimeInSecs) {
		if(acqPointsForAGivenFeed==null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		boolean feedHasIPAcquisitionPoint = acqPointsForAGivenFeed.stream().anyMatch(AcquisitionPoint::isIpAcquisitionPoint);
		boolean doesFeedSupportsOpenEndedEvents = acqPointsForAGivenFeed.get(0).isFeedAllowsOpenEndedBlackouts();
		if (feedHasIPAcquisitionPoint && !doesFeedSupportsOpenEndedEvents) {
			if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().equals(ESSNodeDeliveryType.IP)) {
				LOG.info("This cluster is serving only " + CppUtil.getClusterType().name()
						+ " traffic. Therefore, no error_check messages for hosted needs to be scheduled at event level at this node.");

				return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
			}
			NotificationQueueMessagesBuilder notificationQueueMessagesBuilder = NotificationQueueMessageBuilderFactory.getInstance()
					.getNotificationMessageBuilder(feedHasIPAcquisitionPoint);
			Map<String, HostedAppEventStatusNotificationModel> hostedNotificationMap = new HashMap<>();
			notificationQueueMessagesBuilder.buildForHostedNotifications(acqPointsForAGivenFeed.get(0), blackout, getMaxProgramStartBufferOfAllAQ(acqPointsForAGivenFeed), hostedNotificationMap, immediateStart,
					currentSystemTimeInSecs);
			HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(false).scheduleHostedNotification(hostedNotificationMap);
		}
	}

	private static boolean isInflightEvent(AcquisitionPoint acquisitionPoint, BlackoutEvent blackout, long currentSystemTime) {
		boolean isInFlight = false;

		ConfirmedPlacementOpportunity commonConfirmedBlackout = DATA_MANAGER.getConfirmedBlackoutCommonAcrossAFeedAPs(blackout.getSignalId());
		if (commonConfirmedBlackout != null) {
			long blackoutActualEndTime = commonConfirmedBlackout.isAborted() ? commonConfirmedBlackout.getAbortTime()
					: (commonConfirmedBlackout.hasProgramEndReceived() ? commonConfirmedBlackout.getActualUtcStopTime() : blackout.getUtcStopTime());
			isInFlight = blackoutActualEndTime > currentSystemTime;
		}
		return isInFlight;
	}	

}
