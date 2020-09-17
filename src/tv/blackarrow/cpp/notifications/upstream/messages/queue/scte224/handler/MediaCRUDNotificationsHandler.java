/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.model.scte224.SCTE224EventStatus;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.builder.MediaNotificationMessageBuilder;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;
import tv.blackarrow.cpp.notifications.upstream.scheduler.CancelScheduledNotificationByEventSignalIdInfo;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;
import tv.blackarrow.cpp.notifications.upstream.scheduler.ScheduleNotificationInfo;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppConstants.ESSNodeDeliveryType;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.StreamExecutionType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class MediaCRUDNotificationsHandler {
	
	private static final Logger LOG = LogManager.getLogger(MediaCRUDNotificationsHandler.class);
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	private static final DataManager GENERAL_DATA_MANAGER = DataManagerFactory.getInstance();
	private static final SCTE224DataManager SCTE224_DATA_MANAGER = DataManagerFactory.getSCTE224DataManager();

	
	
	public static void notify(final List<AcquisitionPoint> acqPoints, final EventAction actionForUpdateCase,  final List<Media> mediasToBeNewlyAdded,final List<Media> mediasToBeUpdated,
			List<Media> mediasToBeDeleted) {
		try {
			final long currentSystemTime = System.currentTimeMillis();
			final int currentSystemTimeInSecs = (int) TimeUnit.MILLISECONDS.toSeconds(currentSystemTime);
			if (DEBUG_ENABLED) {
				LOG.debug("0.Received following new medias in the realtime update request at currentTime = " + currentSystemTime + "ms " + " or " + currentSystemTimeInSecs
						+ "seconds");
			}
			if (mediasToBeNewlyAdded != null && !mediasToBeNewlyAdded.isEmpty()) {
				if (DEBUG_ENABLED) {
					LOG.debug("4.Received following new medias in the realtime update request:\n " + mediasToBeNewlyAdded.toString());
				}
				MediaCRUDNotificationsHandler.handleNewMediaAdditionNotifications(acqPoints, mediasToBeNewlyAdded, currentSystemTime, currentSystemTimeInSecs);
			}
			if (mediasToBeUpdated != null && !mediasToBeUpdated.isEmpty()) {
				if (DEBUG_ENABLED) {
					LOG.debug("4. Received following existing medias in the realtime update request:\n " + mediasToBeUpdated.toString());
				}
				MediaCRUDNotificationsHandler.handleMediaUpdateNotifications(acqPoints, mediasToBeUpdated, currentSystemTime, currentSystemTimeInSecs, actionForUpdateCase);
			}

			if (mediasToBeDeleted != null && !mediasToBeDeleted.isEmpty()) {
				if (DEBUG_ENABLED) {
					LOG.debug("4.Received following new medias in the realtime update request:\n " + mediasToBeNewlyAdded.toString());
				}
				MediaCRUDNotificationsHandler.handleMediaDeletionNotifications(acqPoints, mediasToBeDeleted, currentSystemTime, currentSystemTimeInSecs);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
	}
	
	/**
	 * Schedules the Out of Band Notifications for the given Media.
	 * Currently there are two kind of notifications that are supported by Medias.
	 * 1. Manifest Manipulator Level
	 * 2. encoder Level.
	 * 
	 * Both the notifications have different message structures and different scheduling requirements.
	 * 
	 * @param acqPointsForAGivenFeed Acquisition Points List that are associated with the Feed that is associated to all the Medias in the given Media List.
	 * @param mediasForAGivenFeed Medias belonging to a single feed that needs scheduling.
	 * @param mediaPointRestrictionsByMediaSignalId Zones if there are any, that are associated to one or Medias in the given Media List.
	 * @param currentSystemTime current system time up to millis.
	 * @param currentSystemTimeInSecs current system time up to last second.
	 * @param SCTE224EventAction the action that has taken place with the event.
	 * 
	 * @throws DatatypeConfigurationException
	 */
	private static void handleNewMediaAdditionNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<Media> mediasForAGivenFeed,
			final long currentSystemTime, final int currentSystemTimeInSecs) {
		if (mediasForAGivenFeed == null || mediasForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		// Prepare message for newly added Medias.
		mediasForAGivenFeed.forEach(media -> {
			if (media == null || media.getMediaPoints() == null) {
				return;
			}
			if (currentSystemTime > media.getExpiresTimeInMS()) {
				LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ") is already expired, so skipping the notifications scheduling for this Media.");
				return;
			}
			List<MediaPoint> mediaPoints = media.getMediaPoints();
			MediaPoint startMediaPoint = null;
			MediaPoint endMediaPoint = null;
			for (MediaPoint mediaPoint : mediaPoints) {
				List<Short> segmentationTypeIds = mediaPoint.getMatchSignal().getSegmentationTypeIds();
				for (Short segmentationTypeId : segmentationTypeIds) {
					if (startMediaPoint == null) {
						if (SegmentType.isProgramOverlapStartSignal(segmentationTypeId) || SegmentType.isProgramStartSignal(segmentationTypeId)) {
							startMediaPoint = mediaPoint;
						}
					}
					if (endMediaPoint == null) {
						if (SegmentType.isProgramEndSignal(segmentationTypeId) || SegmentType.isProgramEarlyTerminationSignal(segmentationTypeId)) {
							endMediaPoint = mediaPoint;
						}
					}
					if (startMediaPoint != null && endMediaPoint != null) {
						break;
					}
				}
			}
			final MediaPoint programStartMediaPoint = startMediaPoint;
			final MediaPoint programEndMediaPoint = endMediaPoint;
			if(checkMediaPointValidations(media, programStartMediaPoint, programEndMediaPoint)) {
				return;
			}

			final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();
			
			acqPointsForAGivenFeed.forEach(acquisitionPoint -> {
				// Doing it here, because in future we may want to support both IP/QAM for media. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Media(New)--> " + media.getSignalid() + "(" + media.getMediaId() + "):" + " This cluster is serving only " + CppUtil.getClusterType().name()
								+ " traffic. Therefore, not scheduling any notifications for acquisitionPoint = " + acquisitionPoint.getAcquisitionPointIdentity()
								+ " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;// Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
				List<String> mediaZones = media.getMediaPoints().get(0).getZones();
				StreamExecutionType executionType = acquisitionPoint.getExecutionType();
				switch (executionType) {
				case ENCODER_LEVEL:

					// CASE 1 : Encoder Level XMP OOB
					if (mediaZones != null && !mediaZones.isEmpty()) {
						if (StringUtils.isBlank(acquisitionPoint.getZoneIdentity())) {
							LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification scheduling as this Media "
									+ media.getMediaId() + "(" + media.getSignalid() + ") is supposed to be executed at encoder level and this AP has no zones configured.");
						} else if (StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity()) && isMediaZonesContainsAqZone(mediaZones, acquisitionPoint.getZoneIdentity())) {
							// Acquisition Zone is not Empty and zone present in media matches to it then prepare the encoder level blackout notification.
							final String altSourceValue = media.getMediaPoints().get(0).getAltSourceValue();
							// 0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
							if (MediaNotificationMessageBuilder.getThresholdTimeOfMediaPoint(programStartMediaPoint) < currentSystemTime) {
								LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ")'s start time is already expired, "
										+ "so skipping the notifications scheduling for this Media.");
								return;
							}

							// 1. Prepare the notification that goes at the program start time.
							final NotificationMessage notificationAtEventStart = MediaNotificationMessageBuilder.getEncoderLevelStartEventIPNotificationMessage(acquisitionPoint,
									currentSystemTime, media, programStartMediaPoint, programEndMediaPoint, altSourceValue);
							if (notificationAtEventStart != null) {
								notificationMessagesByScheduleTime
										.computeIfAbsent(notificationAtEventStart.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
										.put(notificationAtEventStart.getScheduledNotificationId(), notificationAtEventStart);
							}
						} else {
							LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification as this Media "
									+ media.getMediaId() + "(" + media.getSignalid() + ") is supposed to be executed at encoder level and this AP has a zone configured"
									+ ", that is not matching to the zone present in the Media.");
						}
					} else {
						// CASE 2 : Encoder Level Disney, we do not schedule upstream notification for these Acquisition point. They can only be executed via Inband
						LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "(" + media.getSignalid()
								+ "): Media doesn't utilize cadent:Zone feature. "
								+ " Currently, this media can only be executed via Inband signals(in Stream) on this Acquisition Point(Encoder Level). "
								+ (programStartMediaPoint.getMatchTimeInMS() != null && programStartMediaPoint.getMatchTimeInMS().longValue() > 0l
										? "It's recommended to not send matchtime on such media."
										: "")
								+ " So skipping the upstream notifications scheduling for this Media...");

					}

					break;
				case MANIFEST_LEVEL:

					// CASE 3: Manifest level
					// a. Acquisition Zone is Empty and zones in media are not present then prepare the manifest manipulator level blackout notification.
					// b. Acquisition Zone is OOH and zones in media are not present then prepare the manifest manipulator level blackout notification.

					if (programStartMediaPoint.getZones() == null || programStartMediaPoint.getZones().isEmpty()
							&& (StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity())
									&& acquisitionPoint.getZoneIdentity().equals(CppConstants.CADENT_OOH_ZONE))) {
						// 0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
						Long matchTime = programStartMediaPoint.getMatchTimeInMS();
						if(matchTime == null || matchTime <= 0) {
								LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "(" + media.getSignalid()
										+ ")'s is invalid case as for manifest level blackout. Since, match time is not present. "
										+ "so skipping the notifications scheduling for this Media.");
								return;
						} else if (MediaNotificationMessageBuilder.getThresholdTimeOfMediaPoint(programStartMediaPoint) < currentSystemTime) {
							LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ")'s start time is already expired, "
									+ "so skipping the notifications scheduling for this Media.");
							return;
						}

						// 1. Prepare the notification that goes at the program start time.
						final NotificationMessage notificationAtEventStart = MediaNotificationMessageBuilder.getMMLevelStartEventIPNotificationMessage(acquisitionPoint,
								currentSystemTime, media, programStartMediaPoint, programEndMediaPoint);
						if (notificationAtEventStart != null) {
							notificationMessagesByScheduleTime
									.computeIfAbsent(notificationAtEventStart.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
									.put(notificationAtEventStart.getScheduledNotificationId(), notificationAtEventStart);
						}

						// 2. Prepare the notification that goes at the program end time.
						final NotificationMessage notificationAtEventEnd = MediaNotificationMessageBuilder.getMMLevelEndEventIPNotificationMessage(acquisitionPoint,
								currentSystemTime, currentSystemTimeInSecs, null, media, programStartMediaPoint, programEndMediaPoint,
								notificationAtEventStart.getAcquisitionSignalIds(), false, EventAction.COMPLETE);
						if (notificationAtEventEnd != null) {
							notificationMessagesByScheduleTime
									.computeIfAbsent(notificationAtEventEnd.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
									.put(notificationAtEventEnd.getScheduledNotificationId(), notificationAtEventEnd);
						}
					} else {
						LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "(" + media.getSignalid()
								+ "): Only Acquistion points with Zone Identity 'OOH' are currently supported for Manifest Level Execution. The AQ contains unsupported Zone Identity as '"
								+ acquisitionPoint.getZoneIdentity() + "'. " + "So skipping the upstream notifications scheduling for this Media...");
					}

					break;
				default:
					break;

				}
			});

			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if (!notificationMessagesByScheduleTime.isEmpty()) {
				notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, media.getSignalid() + "(" + media.getMediaId() + ")");
				GENERAL_DATA_MANAGER.saveEventScheduledNotificationQueueTimes(media.getSignalid(), notificationMessagesByScheduleTime.keySet());
			}

		});
	}

	private static Boolean checkMediaPointValidations(Media media, final MediaPoint programStartMediaPoint, final MediaPoint programEndMediaPoint) {
		if (programStartMediaPoint == null) {
			LOG.info(() -> "No Program Start Media Point found on this Media, so skipping notification scheduling for this Media: " + media.getMediaId() + "("
					+ media.getSignalid() + ")");
			return true;
		}
		if (programEndMediaPoint == null) {
			LOG.info(() -> "No Program End Media Point found on this Media, so skipping notification scheduling for this Media: " + media.getMediaId() + "("
					+ media.getSignalid() + ")");
			return true;
		}
		if (programStartMediaPoint.getMatchTimeInMS() == null) {
			LOG.info("No match time present on MediaPoint" + programStartMediaPoint.getSignalId() + ", so skipping notification scheduling for this Media: "
					+ media.getMediaId() + "(" + media.getSignalid() + ")");
			return true;
		}
		return false;
	}
	
	/**
	 * Schedules the Out of Band Notifications for the given Medias if they are runtime live to end them.
	 * Currently there are two kind of notifications that are supported by Medias.
	 * 1. Manifest Manipulator Level
	 * 2. encoder Level.
	 * 
	 * Both the notifications have different message structures and different scheduling requirements.
	 * 
	 * @param acqPointsForAGivenFeed Acquisition Points List that are associated with the Feed that is associated to all the Medias in the given Media List.
	 * @param mediasDeletedForAGivenFeed Medias belonging to a single feed that are deleted.
	 * @param currentSystemTime current system time up to millis.
	 * @param currentSystemTimeInSecs current system time up to last second.
	 * 
	 * @throws DatatypeConfigurationException
	 */
	private static void handleMediaDeletionNotifications(
			final List<AcquisitionPoint> acqPointsForAGivenFeed, 
			final List<Media> mediasDeletedForAGivenFeed,
			final long currentSystemTime,
			final int currentSystemTimeInSecs){
		if(mediasDeletedForAGivenFeed == null || mediasDeletedForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for deleted Medias.
		mediasDeletedForAGivenFeed.forEach( media -> {
			if(media==null || media.getMediaPoints() == null) {
				return;
			}
			if(media.getExpiresTimeInMS() < currentSystemTime) {
				LOG.info(()-> "Media (" + media.getSignalid() + ") " + media.getMediaId() + "is already expired so skipping any rescheduling for this Media.");
				return;
			}
			//1. First cancel all the pending notifications on this Media.
			final Set<Integer> scheduledTimes = GENERAL_DATA_MANAGER.getEventScheduledNotificationQueueTimes(media.getSignalid());
			if(!scheduledTimes.isEmpty()) {
				final Set<Integer> scheduledTimesRemoved = new HashSet<>();
				final List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos = new ArrayList<>();
				scheduledTimes.forEach(scheduledTime -> {
					if(currentSystemTimeInSecs < scheduledTime) {
						cancelScheduledNotificationInfos.add(new CancelScheduledNotificationByEventSignalIdInfo(scheduledTime, media.getSignalid()));
						scheduledTimesRemoved.add(scheduledTime);
					}
				});
				NotificationSchedulerService.getInstance().cancelScheduledNotificationBySignalId(cancelScheduledNotificationInfos);
				scheduledTimes.removeAll(scheduledTimesRemoved);
			}
			
			//2. Now prepare new notifications only for the in progress Media.				
			Map<Integer, Map<String, NotificationMessage>> notificationsByTime = new HashMap<>();

			acqPointsForAGivenFeed.forEach( acquisitionPoint -> {
				//Doing it here, because in future we may want to support both IP/QAM for media. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info("Media(Delete)--> " + media.getSignalid() + "(" + media.getMediaId() + "):" + " This cluster is serving only " + CppUtil.getClusterType().name()
								+ " traffic. Therefore, not scheduling any notifications for acquisitionPoint = " + acquisitionPoint.getAcquisitionPointIdentity()
								+ " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
				MediaLedger mediaLedger = SCTE224_DATA_MANAGER.getAcquisitionPointMediaLedger(acquisitionPoint.getAcquisitionPointIdentity(), media.getSignalid());
				String acqPointZone = acquisitionPoint.getZoneIdentity();
				//If Media with no Zones
				List<String> mediaZones = media.getMediaPoints().get(0).getZones();
				if(mediaLedger!= null && mediaLedger.isMediaEnded()) {
					LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ") is already ended. So kipping any re/un/scheduling.");
					return;
				} else if(StringUtils.isBlank(acquisitionPoint.getZoneIdentity()) && (mediaZones != null && !mediaZones.isEmpty())) { 
					LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification scheduling as this Media " + 
							media.getMediaId() + "(" + media.getSignalid() + ") is supposed to be executed at encoder level and this AP has no zones configured.");
					return;
				} else if(StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity()) && (mediaZones == null || mediaZones.isEmpty())) {
					LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification scheduling as this Media " + 
							media.getMediaId() + "(" + media.getSignalid() + ") is not supposed to be executed at encoder level and this AP has zones configured.");
					return;
				} else if(StringUtils.EMPTY.equals(acqPointZone) && (mediaZones == null || mediaZones.isEmpty())) {
					//Acquisition Zone is Empty or zones in media are not present then prepare the manifest manipulator level blackout notification. 		
					if(mediaLedger != null) {
						if(mediaLedger.isMediaEnded()) {
							LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity() + 
									". So skipping Media Notification scheduling for this Media.");
							return;
						} else if(mediaLedger.isMediaStarted()) {
							//Prepare the old message:
							MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);						 
							NotificationMessage message = MediaNotificationMessageBuilder.getMMLevelEndEventIPNotificationMessage(acquisitionPoint, 
									currentSystemTime, currentSystemTimeInSecs, programStartTransaction, media, null, null, mediaLedger.getAcquisitionSignalIds(), 
									true, EventAction.STOP_NOW);
							if(message != null) {
								notificationsByTime.computeIfAbsent(message.getNotificationScheduledTime(), 
										key -> new HashMap<String, NotificationMessage>()).put(message.getScheduledNotificationId(), message);
							}
						}
					}						
				} else {
					//Acquisition Zone is not Empty and zone present in media matches to it then prepare the encoder level blackout notification.
					if(isMediaZonesContainsAqZone(mediaZones, acqPointZone)){
						if(mediaLedger != null) {
							if(mediaLedger.isMediaEnded()) {
								LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity() + 
										". So skipping Media Notification scheduling for this Media.");
								return;	
							} else if(mediaLedger.isMediaStarted()) {
								//prepare the delete message:									
								MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
								NotificationMessage message = MediaNotificationMessageBuilder.getEncoderLevelEndEventIPNotificationMessage(acquisitionPoint, 
										currentSystemTime, currentSystemTimeInSecs, media, null, null, programStartTransaction, 
										mediaLedger.getAcquisitionSignalIds(), true, EventAction.STOP_NOW);
								if(message != null) {
									notificationsByTime.computeIfAbsent(message.getNotificationScheduledTime(), 
											key -> new HashMap<String, NotificationMessage>()).put(message.getScheduledNotificationId(), message);										
								}
							}
						}
					}
				}
			});
			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if(!notificationsByTime.isEmpty()) {
				notificationsByTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				scheduledTimes.addAll(notificationsByTime.keySet());
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, media.getSignalid() + "(" + media.getMediaId() + ")");
			}
			GENERAL_DATA_MANAGER.saveEventScheduledNotificationQueueTimes(media.getSignalid(), scheduledTimes);
		});
	}

	private static boolean isMediaZonesContainsAqZone(List<String> mediaZones, String acqPointZone) {
		return mediaZones.stream().anyMatch(z -> z.equalsIgnoreCase(acqPointZone));
	}
	
	private static void handleMediaUpdateNotifications(final List<AcquisitionPoint> acqPointsForAGivenFeed, final List<Media> mediasForAGivenFeed, 
			final long currentSystemTime, final int currentSystemTimeInSecs,
			final EventAction SCTE224EventAction) {

		if(mediasForAGivenFeed == null || mediasForAGivenFeed.isEmpty() || acqPointsForAGivenFeed == null || acqPointsForAGivenFeed.isEmpty()) {
			return;
		}
		//Prepare message for Updated Medias.
		mediasForAGivenFeed.forEach( media -> {
			if(media==null || media.getMediaPoints() == null) {
				return;
			}
			if (currentSystemTime > media.getExpiresTimeInMS()) {
				LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ") is already expired, so skipping the notifications scheduling for this Media.");
				return;
			}
			//1. First cancel all the future pending notifications on this Media.
			final Set<Integer> scheduledTimes = GENERAL_DATA_MANAGER.getEventScheduledNotificationQueueTimes(media.getSignalid());
			if(!scheduledTimes.isEmpty()) {
				final Set<Integer> scheduledTimesRemoved = new HashSet<>();
				final List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos = new ArrayList<>();
				scheduledTimes.forEach(scheduledTime -> {
					if(currentSystemTimeInSecs < scheduledTime) {
						cancelScheduledNotificationInfos.add(new CancelScheduledNotificationByEventSignalIdInfo(scheduledTime, media.getSignalid()));
						scheduledTimesRemoved.add(scheduledTime);
					}
				});
				NotificationSchedulerService.getInstance().cancelScheduledNotificationBySignalId(cancelScheduledNotificationInfos);
				scheduledTimes.removeAll(scheduledTimesRemoved);
			}
			
			//2. Now prepare new pending notifications for this Media.				
			List<MediaPoint> mediaPoints = media.getMediaPoints();
			MediaPoint startMediaPoint = null;
			MediaPoint endMediaPoint = null;
			for (MediaPoint mediaPoint : mediaPoints) {
				List<Short> segmentationTypeIds = mediaPoint.getMatchSignal().getSegmentationTypeIds();
				for (Short segmentationTypeId : segmentationTypeIds) {
					if (startMediaPoint == null) {
						if (SegmentType.isProgramOverlapStartSignal(segmentationTypeId) || SegmentType.isProgramStartSignal(segmentationTypeId)) {
							startMediaPoint = mediaPoint;
						}
					}
					if (endMediaPoint == null) {
						if (SegmentType.isProgramEndSignal(segmentationTypeId) || SegmentType.isProgramEarlyTerminationSignal(segmentationTypeId)) {
							endMediaPoint = mediaPoint;
						}
					}
					if (startMediaPoint != null && endMediaPoint != null) {
						break;
					}
				}
			}
			final MediaPoint programStartMediaPoint = startMediaPoint;
			final MediaPoint programEndMediaPoint = endMediaPoint;
			if(checkMediaPointValidations(media, programStartMediaPoint, programEndMediaPoint)) {
				return;
			}

			final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime = new HashMap<>();
			acqPointsForAGivenFeed.forEach(acquisitionPoint -> {
				//Doing it here, because in future we may want to support both IP/QAM for media. Then adding in this place will take care of skipping for relevant jobs.
				if (!ESSNodeDeliveryType.ALL.equals(CppUtil.getClusterType()) && !CppUtil.getClusterType().name().equalsIgnoreCase(acquisitionPoint.getDeliveryType())) {
					if (LOG.isInfoEnabled()) {
						LOG.info(() -> "Media(Update)--> " + media.getSignalid() + "(" + media.getMediaId() + "):" + " This cluster is serving only "
								+ CppUtil.getClusterType().name() + " traffic. Therefore, not scheduling any notifications for acquisitionPoint = "
								+ acquisitionPoint.getAcquisitionPointIdentity() + " configured for " + acquisitionPoint.getDeliveryType() + " traffic.");
					}
					return;//Skip this iteration(if delivery type doesn't match with acquisition points delivery type)
				}
				MediaLedger mediaLedger = SCTE224_DATA_MANAGER.getAcquisitionPointMediaLedger(acquisitionPoint.getAcquisitionPointIdentity(), media.getSignalid());
				//If Media with no Zones
				List<String> mediaZones = media.getMediaPoints().get(0).getZones();
				StreamExecutionType executionType = acquisitionPoint.getExecutionType();
				// Check for encoder level blackout
				LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " (Execution Type= " + executionType + ") and (AQ Zone="
						+ acquisitionPoint.getZoneIdentity() + ") evaluation going on for "
						+ media.getMediaId() + "(" + media.getSignalid() + ").");
				switch (executionType) {
				case ENCODER_LEVEL:

					// CASE 1 : Encoder Level XMP OOB
					if (mediaZones != null && !mediaZones.isEmpty()) {
						if (StringUtils.isBlank(acquisitionPoint.getZoneIdentity())) {
							LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification scheduling as this Media "
									+ media.getMediaId() + "(" + media.getSignalid() + ") is supposed to be executed at encoder level and this AP has no zones configured.");
						} else if (StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity()) && isMediaZonesContainsAqZone(mediaZones, acquisitionPoint.getZoneIdentity())) {
							if (mediaLedger != null) {
								if (mediaLedger.isMediaEnded()) {
									LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
											+ ". So skipping Media Notification scheduling for this Media.");
									return;
								} else if (mediaLedger.isMediaStarted()) {
									LOG.info(() -> "Media : " + media.getSignalid() + " has already started on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
											+ " (Execution Type= " + executionType + ") and (AQ Zone=" + acquisitionPoint.getZoneIdentity() + "). The current System Time in Seconds are "+ currentSystemTimeInSecs);
									//prepare the update message:									
									MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
									NotificationMessage message = MediaNotificationMessageBuilder.getEncoderLevelEndEventIPNotificationMessage(acquisitionPoint, currentSystemTime,
											currentSystemTimeInSecs, media, null, null, programStartTransaction, mediaLedger.getAcquisitionSignalIds(), true, SCTE224EventAction);
									if (message != null) {
										LOG.info(() -> "Media : " + media.getSignalid() + " Scheduling immediate message on Acq Point: "
												+ acquisitionPoint.getAcquisitionPointIdentity() + " at USNNQ:" + message.getNotificationScheduledTime() + " time: " + message);
										notificationMessagesByScheduleTime
												.computeIfAbsent(message.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
												.put(message.getScheduledNotificationId(), message);
									}
								}
							} else {
								//0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
								Long matchTime = programStartMediaPoint.getMatchTimeInMS();
								if (matchTime == null || matchTime <= 0) {
									LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "("
											+ media.getSignalid()
											+ "): This Media contains matching cadent:zone as configured on Encoder Level AQ point(zoneIdentity=" + acquisitionPoint
													.getZoneIdentity()
											+ "), but doesn't have match time." + " So skipping the notifications scheduling for this Media.");
									return;
								} else if (MediaNotificationMessageBuilder.getThresholdTimeOfMediaPoint(programStartMediaPoint) < currentSystemTime) {
									LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ")'s start time is already expired, "
											+ "so skipping the notifications scheduling for this Media.");
									return;
								}

								//Acquisition Zone is not Empty and zone present in media matches to it then prepare the encoder level blackout notification.
								final String altSourceValue = media.getMediaPoints().get(0).getAltSourceValue();

								//1. Prepare the notification that goes at the program start time.
								final NotificationMessage notificationAtEventStart = MediaNotificationMessageBuilder.getEncoderLevelStartEventIPNotificationMessage(
										acquisitionPoint, currentSystemTime, media, programStartMediaPoint, programEndMediaPoint, altSourceValue);
								if (notificationAtEventStart != null) {
									notificationMessagesByScheduleTime
											.computeIfAbsent(notificationAtEventStart.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
											.put(notificationAtEventStart.getScheduledNotificationId(), notificationAtEventStart);
								}
							}
						} else {
							LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " is not considered for notification as this Media "
									+ media.getMediaId() + "(" + media.getSignalid() + ") is supposed to be executed at encoder level and this AP has a zone Identity("
									+ acquisitionPoint.getZoneIdentity() + ") configured"
									+ ", that is not matching to the zone present in the Media.");
						}
					} else {
						//CASE 2 : Encoder Level Disney, we schedule upstream notifications for stop_now cases
						if ((programStartMediaPoint.getMatchTimeInMS() <= 0l || programStartMediaPoint.getMatchTimeInMS() == null)
								&& EventAction.STOP_NOW.equals(SCTE224EventAction)) {
							if (mediaLedger != null) {
								if (mediaLedger.isMediaEndNotificationSent()) {
									LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
											+ ". So skipping Media Stop Now Notification scheduling for this Media.");
									return;
								} else if (mediaLedger.isMediaStartNotificationSent()) {
									LOG.info(() -> "Media : " + media.getSignalid() + " has already started on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
											+ " (Execution Type= " + executionType + ") and (AQ Zone=" + acquisitionPoint.getZoneIdentity()
											+ "). The current System Time in Seconds are " + currentSystemTimeInSecs);
									//prepare the stop now message							
									scheduleStopNowUpstreamNotificationMessage(currentSystemTime, currentSystemTimeInSecs, SCTE224EventAction, media,
											notificationMessagesByScheduleTime, acquisitionPoint, mediaLedger);
								}
							} else {
								//We only notify host system for this case 
								sendStopNowStatusToHostSystem(currentSystemTime, media, programStartMediaPoint, programEndMediaPoint, acquisitionPoint);

							}
						} else {

							//  We do not schedule upstream notification for these Acquisition point. They can only be executed via Inband
							LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "(" + media.getSignalid()
									+ "): Media doesn't utilize cadent:Zone feature. "
									+ " Currently, this media can only be executed via Inband signals(in Stream) on this Acquisition Point(Encoder Level). "
									+ (programStartMediaPoint.getMatchTimeInMS() != null && programStartMediaPoint.getMatchTimeInMS().longValue() > 0l
											? "It's recommended to not send matchtime on such media."
											: "")
									+ " So skipping the upstream notifications scheduling for this Media...");
						}
					}

					break;
				case MANIFEST_LEVEL:

					// CASE 3: Manifest level
					// a. Acquisition Zone is Empty and zones in media are not present then prepare the manifest manipulator level blackout notification.
					// b. Acquisition Zone is OOH and zones in media are not present then prepare the manifest manipulator level blackout notification.
					
					//Scheduling blackout override and program runover notifications
					if ((StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity()) && acquisitionPoint.getZoneIdentity().equals(CppConstants.CADENT_OOH_ZONE))) {
						if(mediaLedger != null && (media.iSBlackoutOverride() || media.isProgramRunover()) && !mediaLedger.isMediaEnded()) {
							if(media.iSBlackoutOverride()) {
								mediaLedger.incrementTerritoryUpdateCounter();
								SCTE224_DATA_MANAGER.saveAcquisitionPointMediaLedger(mediaLedger, acquisitionPoint.getAcquisitionPointIdentity(), media.getSignalid());
							}
							// 1. Prepare the combined notification that goes at the program start time.for Blackout override and program runover
							final NotificationMessage notificationForBlackoutOverrideAndProgramRunover = MediaNotificationMessageBuilder.getBlackoutOverrideAndProgramRunoverNotificationMessages(acquisitionPoint,
											currentSystemTime, currentSystemTimeInSecs, media, programStartMediaPoint, programEndMediaPoint, mediaLedger);
							if (notificationForBlackoutOverrideAndProgramRunover != null) {
								notificationMessagesByScheduleTime
										.computeIfAbsent(notificationForBlackoutOverrideAndProgramRunover.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
										.put(notificationForBlackoutOverrideAndProgramRunover.getScheduledNotificationId(), notificationForBlackoutOverrideAndProgramRunover);
							}
						}
						if (mediaLedger != null) {
							LOG.debug(() -> "Program Start Media Point has matchTime ("+programStartMediaPoint.getMatchTimeInMS()+")" + (mediaLedger.isMediaStarted() && programStartMediaPoint != null && programStartMediaPoint.getMatchTimeInMS() > 0));
							LOG.debug(() -> "Program End Media Point has matchTime ("+programEndMediaPoint.getMatchTimeInMS()+")" + (mediaLedger.isMediaStarted() && programEndMediaPoint != null && programEndMediaPoint.getMatchTimeInMS() > 0));
							if (mediaLedger.isMediaEnded()) {
								LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity() + ". So skipping Media Notification scheduling for this Media.");
								return;
							} else if (mediaLedger.isMediaStarted() && programEndMediaPoint != null && programEndMediaPoint.getMatchTimeInMS() > 0) {
								//1. Prepare the notification that goes at the program end time.
								MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
								boolean isImmediateEndByDelete = EventAction.STOP_NOW.equals(SCTE224EventAction);
								NotificationMessage message = MediaNotificationMessageBuilder.getMMLevelEndEventIPNotificationMessage(acquisitionPoint, currentSystemTime,
										currentSystemTimeInSecs, programStartTransaction, media, null, null, mediaLedger.getAcquisitionSignalIds(), isImmediateEndByDelete, SCTE224EventAction);
								if (message != null) {
									notificationMessagesByScheduleTime.computeIfAbsent(message.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>()).put(message.getScheduledNotificationId(), message);
								}
							}
							else {
								//For inband case we only send notification when event action is stop now 
								if (EventAction.STOP_NOW.equals(SCTE224EventAction) && (programStartMediaPoint.getMatchTimeInMS() <= 0l || programStartMediaPoint.getMatchTimeInMS() == null)) {
										if (mediaLedger.isMediaEndNotificationSent()) {
											LOG.info(() -> "Media : " + media.getSignalid() + " has already ended on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
													+ ". So skipping Media Stop Now Notification scheduling for this Media.");
											return;
										} else if (mediaLedger.isMediaStartNotificationSent()) {
											LOG.info(() -> "Media : " + media.getSignalid() + " has already started on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
													+ " (Execution Type= " + executionType + ") and (AQ Zone=" + acquisitionPoint.getZoneIdentity()
													+ "). The current System Time in Seconds are " + currentSystemTimeInSecs);
											//prepare the stop now message							
											MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
											NotificationMessage message = MediaNotificationMessageBuilder.getMMLevelEndEventIPNotificationMessage(acquisitionPoint, currentSystemTime,
													currentSystemTimeInSecs, programStartTransaction, media, null, null, mediaLedger.getAcquisitionSignalIds(), true, SCTE224EventAction);
											
											if (message != null) {
												LOG.info(() -> "Media : " + media.getSignalid() + " Scheduling immediate message on Acq Point: "
														+ acquisitionPoint.getAcquisitionPointIdentity() + " at USNNQ:" + message.getNotificationScheduledTime() + " time: " + message);
												notificationMessagesByScheduleTime
														.computeIfAbsent(message.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
														.put(message.getScheduledNotificationId(), message);
											}
										}
									
								} 
							}
						} else {
							//THis is the case when event is in effective state and it's status is not confirmed yet so we notify to host system only.
							if (EventAction.STOP_NOW.equals(SCTE224EventAction)) {
								sendStopNowStatusToHostSystem(currentSystemTime, media, programStartMediaPoint, programEndMediaPoint, acquisitionPoint);
							}
							//0. If the Program Start is already in past then no need to schedule anything because this event is already in error state.
							if (MediaNotificationMessageBuilder.getThresholdTimeOfMediaPoint(programStartMediaPoint) < currentSystemTime) {
								LOG.info(() -> "Media " + media.getMediaId() + "(" + media.getSignalid() + ")'s start time is already expired, "
										+ "so skipping the notifications scheduling for this Media.");
								return;
							}
							else {
								NotificationMessage notificationAtEventStart = null;
								if (programStartMediaPoint != null && programStartMediaPoint.getMatchTimeInMS() > 0) {
								//1. Prepare the notification that goes at the program start time.
								notificationAtEventStart = MediaNotificationMessageBuilder.getMMLevelStartEventIPNotificationMessage(acquisitionPoint, currentSystemTime, media,
										programStartMediaPoint, programEndMediaPoint);
								if (notificationAtEventStart != null) {
									notificationMessagesByScheduleTime.computeIfAbsent(notificationAtEventStart.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
											.put(notificationAtEventStart.getScheduledNotificationId(), notificationAtEventStart);
								}
							}

							if (notificationAtEventStart != null && programEndMediaPoint != null && programEndMediaPoint.getMatchTimeInMS() > 0) {
								//2. Prepare the notification that goes at the program end time.
								final NotificationMessage notificationAtEventEnd = MediaNotificationMessageBuilder.getMMLevelEndEventIPNotificationMessage(acquisitionPoint,
										currentSystemTime, currentSystemTimeInSecs, null, media, programStartMediaPoint, programEndMediaPoint,
										notificationAtEventStart.getAcquisitionSignalIds(), false, SCTE224EventAction);
								if (notificationAtEventEnd != null) {
									notificationMessagesByScheduleTime
											.computeIfAbsent(notificationAtEventEnd.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
											.put(notificationAtEventEnd.getScheduledNotificationId(), notificationAtEventEnd);
								}
							}
						  }
						}

					}
					else {
				
						LOG.info(() -> "Acquisition Point " + acquisitionPoint.getAcquisitionPointIdentity() + " for Media " + media.getMediaId() + "(" + media.getSignalid()
								+ "): Only Acquistion points with Zone Identity 'OOH' are currently supported for Manifest Level Execution. The AQ contains unsupported Zone Identity as '"
								+ acquisitionPoint.getZoneIdentity() + "'. " + "So skipping the upstream notifications scheduling for this Media...");
					
				}

					break;
				default:
					break;
				}

			});

			List<ScheduleNotificationInfo> notificationList = new ArrayList<>();
			if(!notificationMessagesByScheduleTime.isEmpty()) {
				notificationMessagesByScheduleTime.entrySet().forEach(entry -> {
					notificationList.add(new ScheduleNotificationInfo(entry.getKey(), entry.getValue()));
				});
				NotificationSchedulerService.getInstance().scheduleNotification(notificationList, media.getSignalid() + "(" + media.getMediaId() + ")");
				GENERAL_DATA_MANAGER.saveEventScheduledNotificationQueueTimes(media.getSignalid(), notificationMessagesByScheduleTime.keySet());
			}
		
		});
	}

	/**
	 * @param currentSystemTime
	 * @param currentSystemTimeInSecs
	 * @param SCTE224EventAction
	 * @param media
	 * @param notificationMessagesByScheduleTime
	 * @param acquisitionPoint
	 * @param mediaLedger
	 */
	private static void scheduleStopNowUpstreamNotificationMessage(final long currentSystemTime, final int currentSystemTimeInSecs, final EventAction SCTE224EventAction,
			Media media, final Map<Integer, Map<String, NotificationMessage>> notificationMessagesByScheduleTime, AcquisitionPoint acquisitionPoint, MediaLedger mediaLedger) {
		MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
		NotificationMessage message = MediaNotificationMessageBuilder.getEncoderLevelDisneyEndEventIPNotificationMessage(acquisitionPoint,
				currentSystemTime, currentSystemTimeInSecs, media, null, null, programStartTransaction, mediaLedger.getAcquisitionSignalIds(), true,
				SCTE224EventAction);
		if (message != null) {
			LOG.info(() -> "Media : " + media.getSignalid() + " Scheduling immediate message on Acq Point: "
					+ acquisitionPoint.getAcquisitionPointIdentity() + " at USNNQ:" + message.getNotificationScheduledTime() + " time: " + message);
			notificationMessagesByScheduleTime
					.computeIfAbsent(message.getNotificationScheduledTime(), key -> new HashMap<String, NotificationMessage>())
					.put(message.getScheduledNotificationId(), message);
		}
	}

	/**
	 * @param currentSystemTime
	 * @param media
	 * @param programStartMediaPoint
	 * @param programEndMediaPoint
	 * @param acquisitionPoint
	 */
	private static void sendStopNowStatusToHostSystem(final long currentSystemTime, Media media, final MediaPoint programStartMediaPoint, final MediaPoint programEndMediaPoint,
			AcquisitionPoint acquisitionPoint) {
		boolean isStartMediaPointNotExpired = false;
		if (isEligibleToHostNotification(programStartMediaPoint, currentSystemTime, acquisitionPoint)) {
			isStartMediaPointNotExpired = true;
			//schedule host notification only for start media point 
			MediaRuntimeNotificationsHandler.notify224MediaPointStatusToHostedImmediately(
					getHostedAppEventStatusNotifyModel(media.getMediaSignalid(), programStartMediaPoint.getSignalId(), acquisitionPoint));

		}
		if(isStartMediaPointNotExpired || isEligibleToHostNotification(programEndMediaPoint, currentSystemTime, acquisitionPoint) ) {
			//schedule host notification only for END media point 
			MediaRuntimeNotificationsHandler.notify224MediaPointStatusToHostedImmediately(
					getHostedAppEventStatusNotifyModel(media.getMediaSignalid(), programEndMediaPoint.getSignalId(), acquisitionPoint));
		}
	}

	private static boolean isEligibleToHostNotification(MediaPoint mediaPoint, long currentSystemTime, final AcquisitionPoint acquisitionPoint) {
		long signalTolerance = mediaPoint.getMatchSignal() != null ? mediaPoint.getMatchSignal().getSignalToleranceDurationInMS() : 0l;
		//TODO Need a peer review
		if (mediaPoint.getEffectiveTimeInMS() - signalTolerance - acquisitionPoint.getFeedSCCNotificationBuffer() < currentSystemTime
				&& mediaPoint.getExpiresTimeInMS() - signalTolerance - acquisitionPoint.getFeedSCCNotificationBuffer() > currentSystemTime) {
			return true;
		} else {
			LOG.info(() -> "Media point: " + mediaPoint.getSignalId() + " has already expired or near to expire on Acq Point: " + acquisitionPoint.getAcquisitionPointIdentity()
					+ ". So skipping scheduling for this Media to Hosted system. ");

		}
		return false;
	}

	/**
	 * Returns the HostedAppEventStatusNotifyModel that would be used for notifying the hosted app.
	 * 
	 * @param eventSignalId the event's signal id.
	 * @param acquisitionPoint the acquisition point on which this event signal has been received.
	 * @return
	 */
	private static HostedAppEventStatusScte224NotifyModel getHostedAppEventStatusNotifyModel(final String eventSignalId, final String mediaPointSignalId,
			final AcquisitionPoint acquisitionPoint) {
		HostedAppEventStatusScte224NotifyModel scheduledHostedMessage = new HostedAppEventStatusScte224NotifyModel();
		scheduledHostedMessage.setEventStatus(SCTE224EventStatus.STOP_NOW);
		scheduledHostedMessage.setStreamId(acquisitionPoint.getAcquisitionPointIdentity());
		scheduledHostedMessage.setEventSignalId(eventSignalId);
		scheduledHostedMessage.setMediaPointSignalId(mediaPointSignalId);
		scheduledHostedMessage.setEventType(EventType.SCTE224);		
		return scheduledHostedMessage;
	}
}
