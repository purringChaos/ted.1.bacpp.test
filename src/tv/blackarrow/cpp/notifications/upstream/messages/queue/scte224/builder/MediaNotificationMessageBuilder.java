package tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.builder;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.ESS_PROCESSING_TIME_IN_SECS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;


public class MediaNotificationMessageBuilder {

	private static final Logger LOGGER = LogManager.getLogger(MediaNotificationMessageBuilder.class);
	private static final ObjectFactory I03_SIGNALING_OBJECT_FACTORY = new ObjectFactory();

	public static final int MM_NOTIFICATION_FIRST_SIGNAL_PROGRAM_START_INDEX = 0;
	public static final int MM_NOTIFICATION_SECOND_SIGNAL_CONTENT_ID_INDEX = 1;
	public static final int MM_NOTIFICATION_SIGNAL_CONTENT_ID_INDEX = 0;
	public static final int ENCODER_NOTIFICATION_PROGRAM_START_SECOND_SIGNAL_INDEX = 1;
	public static final int ENCODER_NOTIFICATION_PROGRAM_END_FIRST_SIGNAL_INDEX = 0;
	
	public static final long CANCEL_UPSTREAM_NOTIFY_AHEAD_TIME_BUFFER = 5;
	public static final long CANCEL_SCHEDULE_AHEAD_TIME_IN_SECS = 1;
	public static final long SCHEDULE_LAG_BEGIND_TIME_IN_SECS = 5;

	private static final String SEPARATOR = "#@#";
	
	/*****************************************************************************
	 * Manifest manipulator Level Notification Message builder methods starts here.*
	 *****************************************************************************/
	
	public static NotificationMessage getMMLevelStartEventIPNotificationMessage(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final Media media, MediaPoint programStartMediaPoint, MediaPoint programEndMediaPoint) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}

		long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		final long contentDurationInMillis = programStartMediaPoint.getApply() != null && !programStartMediaPoint.getApply().isEmpty()
				&& programStartMediaPoint.getApply().get(0).getDurationInMillis() != null ? (programStartMediaPoint.getApply().get(0).getDurationInMillis()) : -1;

		Map<SegmentType, String> acquisitionSignalIds = new HashMap<>();
		String acquisitionStartSignalId = UUIDUtils.getBase64UrlEncodedUUID();
		String acquisitionStopSignalId = UUIDUtils.getBase64UrlEncodedUUID();
		acquisitionSignalIds.put(SegmentType.PROGRAM_START, acquisitionStartSignalId);
		acquisitionSignalIds.put(SegmentType.CONTENT_IDENTIFICATION, acquisitionStopSignalId);

		NotificationMessage notificationMessageForEventStart = getScheduledEventIPNotificationMessage(EventAction.CONFIRMED, null, acquisitionPoint, media);
		if (notificationMessageForEventStart != null) {
			//  Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForEventStart, programStartMediaPoint);
			//1. Set the event start time.
			notificationMessageForEventStart.setEventSignalUTCStartTime(programStartUTCTime);

			//2. Set the time at which this notification needs to be sent.
			//It's a regular start. Use the time from first signal which is always a Program Start Signal.
			int scheduledTime = getFutureStartNotificationTimeInSecs((int) (TimeUnit.MILLISECONDS.toSeconds(programStartUTCTime)), acquisitionPoint);
			notificationMessageForEventStart.setNotificationScheduledTime(scheduledTime);

			//3. Set the acquisitionSignalIds
			notificationMessageForEventStart.setAcquisitionSignalIds(acquisitionSignalIds);

			notificationMessageForEventStart.setMediaPointSignalId(programStartMediaPoint.getSignalId());
			notificationMessageForEventStart.setSignalTime(programStartUTCTime);
			notificationMessageForEventStart.setEventSignalUTCStartTime(programStartUTCTime);
			notificationMessageForEventStart.setCurrentTime(currentSystemTime);
			notificationMessageForEventStart.setContentDuration(contentDurationInMillis);
			notificationMessageForEventStart.setTransactionSegmentType(Arrays.asList(SegmentType.PROGRAM_START));
			
			// 4. Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForEventStart, programStartMediaPoint);
		}
		return notificationMessageForEventStart;

	}
	

	public static NotificationMessage getMMLevelEndEventIPNotificationMessage(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final int currentSystemTimeInSecs, final MediaTransaction programStartTransaction, final Media media, MediaPoint programStartMediaPoint, 
			MediaPoint programEndMediaPoint, final Map<SegmentType, String> acquisitionSignalIds, final boolean isImmediateEndByDelete,
			final EventAction eventAction) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}

		long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		long programStopUTCTime = getThresholdTimeOfMediaPoint(programEndMediaPoint);
		int scheduleNotificationTimeInSecs = getFutureEndNotificationTimeInSecs((int) TimeUnit.MILLISECONDS.toSeconds(programStopUTCTime), acquisitionPoint);

		NotificationMessage notificationMessageForCompleteEvent = isValidForNotification(programEndMediaPoint, acquisitionPoint)
				? getScheduledEventIPNotificationMessage(eventAction, null, acquisitionPoint, media) : null;
		if (notificationMessageForCompleteEvent != null) {
			//  Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForCompleteEvent, programStartMediaPoint);
			//1. Set the event start time.
			if (programStartTransaction != null) {
				programStartUTCTime = programStartTransaction.getSignalTimeInMS();
			}

			//2. Set the time at which this notification needs to be sent.
			//It's a regular future complete notification. Use the time from second signal which is always a Content Id Signal.
			if (isImmediateEndByDelete) {
				scheduleNotificationTimeInSecs = getImmediateNotificationTimeInSecs(currentSystemTimeInSecs);
				programStopUTCTime = getImmediateUTCTimeInSignalInMillis(scheduleNotificationTimeInSecs, acquisitionPoint);
			}
		
			notificationMessageForCompleteEvent.setNotificationScheduledTime(scheduleNotificationTimeInSecs);
			//3. Set the acquisitionSignalIds
			notificationMessageForCompleteEvent.setAcquisitionSignalIds(acquisitionSignalIds);
			
			notificationMessageForCompleteEvent.setSignalTime(programStopUTCTime);
			notificationMessageForCompleteEvent.setEventSignalUTCStartTime(programStartUTCTime);
			notificationMessageForCompleteEvent.setEventSignalUTCStopTime(programStopUTCTime);
			notificationMessageForCompleteEvent.setCurrentTime(currentSystemTime);

			notificationMessageForCompleteEvent.setMediaPointSignalId(programEndMediaPoint.getSignalId());
			
			notificationMessageForCompleteEvent.setContentDuration(programStopUTCTime - programStartUTCTime);
			
			// 4. Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForCompleteEvent, programEndMediaPoint);

		}
		return notificationMessageForCompleteEvent;

	}
	
	
	
	/*****************************************************************************
	 * Manifest manipulator Level Notification Message builder methods ends here.*
	 *****************************************************************************/
	
	
	/*****************************************************************************
	 * Blackout Override And Program Runover Notification Message builder methods starts here.
	 * @param mediaLedger.g *
	 *****************************************************************************/
	
	public static NotificationMessage getBlackoutOverrideAndProgramRunoverNotificationMessages(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final int currentSystemTimeInSecs, final Media media, MediaPoint programStartMediaPoint, MediaPoint programEndMediaPoint, MediaLedger mediaLedger) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}
		
		if (media.isProgramRunover() && !isValidForNotification(programEndMediaPoint, acquisitionPoint)) {
			return null;
		}

		long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		final long contentDurationInMillis = programStartMediaPoint.getApply() != null && !programStartMediaPoint.getApply().isEmpty()
				&& programStartMediaPoint.getApply().get(0).getDurationInMillis() != null ? (programStartMediaPoint.getApply().get(0).getDurationInMillis()) : -1;

		Map<SegmentType, String> acquisitionSignalIds = new HashMap<>();
		String acquisitionStartSignalId = UUIDUtils.getBase64UrlEncodedUUID();
		acquisitionSignalIds.put(SegmentType.PROGRAM_START, acquisitionStartSignalId);
		acquisitionSignalIds.put(SegmentType.CONTENT_IDENTIFICATION, mediaLedger.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION));

		NotificationMessage notificationMessageForBlackoutOverride = getScheduledEventIPNotificationMessage(EventAction.UPDATE, null, acquisitionPoint, media);
		if (notificationMessageForBlackoutOverride != null) {
			//  Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForBlackoutOverride, programStartMediaPoint);
			//1. Set the time at which this notification needs to be sent.
			//It's a regular start. Use the time from first signal which is always a Program Start Signal.
			//This is always immediate
			int scheduleNotificationTimeInSecs = getImmediateNotificationTimeInSecs(currentSystemTimeInSecs);
			long signTimeInMillis = getImmediateUTCTimeInSignalInMillis(scheduleNotificationTimeInSecs, acquisitionPoint);
			notificationMessageForBlackoutOverride.setNotificationScheduledTime(scheduleNotificationTimeInSecs);
			
			//2. Set the event start time.
			notificationMessageForBlackoutOverride.setEventSignalUTCStartTime(signTimeInMillis);

			//3. Set the acquisitionSignalIds
			notificationMessageForBlackoutOverride.setAcquisitionSignalIds(acquisitionSignalIds);
			
			notificationMessageForBlackoutOverride.setMediaPointSignalId(programStartMediaPoint.getSignalId());
			notificationMessageForBlackoutOverride.setSignalTime(programStartUTCTime);
			notificationMessageForBlackoutOverride.setCurrentTime(currentSystemTime);
			notificationMessageForBlackoutOverride.setContentDuration(contentDurationInMillis);
			notificationMessageForBlackoutOverride.setTerritoryUpdateCounter(mediaLedger.getTerritoryUpdateCounter());
			
			List<SegmentType> segmentTypeIds = new ArrayList<SegmentType>();
			if(media.iSBlackoutOverride()) {
				notificationMessageForBlackoutOverride.setEventSignalId(media.getSignalid());
				segmentTypeIds.add(SegmentType.PROGRAM_BLACKOUT_OVERRIDE);
				notificationMessageForBlackoutOverride.setBlackoutOverride(true);
			}
			if(media.isProgramRunover()) {
				notificationMessageForBlackoutOverride.setEventSignalId(media.getSignalid());
				segmentTypeIds.add(SegmentType.PROGRAM_RUNOVER_UNPLANNED);
				notificationMessageForBlackoutOverride.setProgramRunoverUnplanned(true);
			}
			notificationMessageForBlackoutOverride.setTransactionSegmentType(segmentTypeIds);
		}
		return notificationMessageForBlackoutOverride;

	}
	

	/*****************************************************************************
	 * Blackout Override and Program RunoverNotification Message builder methods ends here.*
	 *****************************************************************************/
	

	
/*****************************************************************************************************************************************************************************
 * Common Methods starts here.																																				 *
 *****************************************************************************************************************************************************************************/
	/*
	 * No Scheduling Start/End Added Here. Only Default unchangeable things added.
	 */
	private static NotificationMessage getScheduledEventIPNotificationMessage(final EventAction programRunoverPlanned, 
			final SignalProcessingNotificationType spNotification,
			final AcquisitionPoint acquisitionPoint, 
			final Media media) {

		NotificationMessage notificationMessage = new NotificationMessage();
		notificationMessage.setFeedExtRef(acquisitionPoint.getFeedExternalRef());
		notificationMessage.setEventAction(programRunoverPlanned);
		notificationMessage.setEventId(media.getMediaId());
		notificationMessage.setEventSignalId(media.getSignalid());
		notificationMessage.setEventType(EventType.SCTE224);
		notificationMessage.setScheduledNotificationId(getScheduleNotificationId(media, acquisitionPoint, programRunoverPlanned));
		notificationMessage.setStreamId(acquisitionPoint.getAcquisitionPointIdentity());
		notificationMessage.setStreamSignalTimeOffset(acquisitionPoint.getSignalTimeOffset());
		notificationMessage.setStreamURL(acquisitionPoint.getTranscoderEndpoint());
		notificationMessage.setUpStreamNotificationMessageType(UpStreamNotificationMessageType.getNotificationMessageType(acquisitionPoint));
		notificationMessage.setSchema(Schema.getSchema(acquisitionPoint.getEsamVersion()));
		notificationMessage.setAqContentFrequency(acquisitionPoint.getContentIDFrequency());
		notificationMessage.setZoneIdentity(acquisitionPoint.getZoneIdentity());

		return notificationMessage;

	}
	
	public static MediaPoint getStartMediaPoint(final Media media) {		
		MediaPoint startMediaPoint = null;		
		for(MediaPoint mediaPoint: media.getMediaPoints()) {
			List<Short> segmentationTypeIds = mediaPoint.getMatchSignal().getSegmentationTypeIds();
			for(Short segmentationTypeId : segmentationTypeIds) {
				if(startMediaPoint == null) {
					if(SegmentType.isProgramOverlapStartSignal(segmentationTypeId) || SegmentType.isProgramStartSignal(segmentationTypeId)) {
						startMediaPoint = mediaPoint;
					}
				}
				if(startMediaPoint != null) {
					break;
				}
			}
		}
		return startMediaPoint;
	}
	
	public static MediaPoint getEndMediaPoint(final Media media) {
		MediaPoint endMediaPoint = null;
		for(MediaPoint mediaPoint: media.getMediaPoints()) {
			List<Short> segmentationTypeIds = mediaPoint.getMatchSignal().getSegmentationTypeIds();
			for(Short segmentationTypeId : segmentationTypeIds) {
				if(endMediaPoint == null) {
					if(SegmentType.isProgramEndSignal(segmentationTypeId) || SegmentType.isProgramEarlyTerminationSignal(segmentationTypeId)) {
						endMediaPoint = mediaPoint;
					}
				}
				if(endMediaPoint != null) {
					break;
				}
			}
		}
		return endMediaPoint;
	}
	
	
	public static String getScheduleNotificationId(final Media media, final AcquisitionPoint acqPoint, final EventAction eventAction) {
		return media.getSignalid() + SEPARATOR + acqPoint.getAcquisitionPointIdentity() + SEPARATOR + 
				(EventAction.STOP_NOW.equals(eventAction) ? EventAction.COMPLETE.name() : eventAction.name());
	}

	
	public static long getThresholdTimeOfMediaPoint(MediaPoint mediaPoint) {
		long notificationTime =0;
		if(mediaPoint.getMatchTimeInMS()!=null && mediaPoint.getMatchTimeInMS() > 0) {
			notificationTime = mediaPoint.getMatchTimeInMS();
		}else {
			notificationTime = mediaPoint.getExpiresTimeInMS();
		}
		return notificationTime + getSignalTolerance(mediaPoint);
	}

	/*
	 * Program End has not arrived.
	 */
	private static boolean isValidForNotification(MediaPoint programEndMediaPoint, AcquisitionPoint acquisitionPoint) {
		long applicableNotificationTime = programEndMediaPoint.getExpiresTimeInMS();

		if(programEndMediaPoint.getMatchTimeInMS()!=null && programEndMediaPoint.getMatchTimeInMS() > 0) {
			applicableNotificationTime = programEndMediaPoint.getMatchTimeInMS() + getSignalTolerance(programEndMediaPoint);
		}
		applicableNotificationTime -= acquisitionPoint.getFeedSCCNotificationBuffer();

		long currentTimeMillis = System.currentTimeMillis();
		boolean isValidForNotification = currentTimeMillis < applicableNotificationTime;
		if (!isValidForNotification) {
			LOGGER.info("Skipping scheduling notification of this media point because scheduled notification time is in past.");
		}
		return isValidForNotification;
	}
	
	public static long getSignalTolerance(final MediaPoint mediaPoint) {
		final long matchOffset = mediaPoint.getMatchSignal() == null ? 0 : (
				mediaPoint.getMatchSignal().getSignalToleranceDurationInMS() == null ? 0 :mediaPoint.getMatchSignal().getSignalToleranceDurationInMS());
		return matchOffset;
	}

	/*
	 * For Program Start Scheduling we schedule FeedSCCNotificationBuffer time ahead
	 */
	public static int getFutureStartNotificationTimeInSecs(int futureTimeInSeconds, AcquisitionPoint acquisitionPoint) {
		return futureTimeInSeconds - acquisitionPoint.getFeedSCCNotificationBuffer();
	}

	/*
	 * For Program Start Scheduling we schedule FeedSCCNotificationBuffer +  getEsamProgramEndNotifyAheadDeltaInMillis(for back to back prod fix) time ahead
	 */
	public static int getFutureEndNotificationTimeInSecs(int futureTimeInSeconds, AcquisitionPoint acquisitionPoint) {
		return (int) (futureTimeInSeconds - acquisitionPoint.getFeedSCCNotificationBuffer()
				- TimeUnit.MILLISECONDS.toSeconds(CppConfigurationBean.getInstance().getEsamProgramEndNotifyAheadDeltaInMillis()));//All in seconds
	}

	/*
	 * Immediate notifications gets schedule ESS_PROCESSING_TIME_IN_SECS in couchbase Queue.
	 */
	public static int getImmediateNotificationTimeInSecs(int currentTimeInSecs) {
		return currentTimeInSecs + ESS_PROCESSING_TIME_IN_SECS;
	}

	/*
	 * Immediate notifications's UTC Signal Time pretends it was sent well in advance to Encoders, i.e. adding FeedSCCNotificationBuffer
	 */
	public static long getImmediateUTCTimeInSignalInMillis(int notificationTimeInSeconds, AcquisitionPoint acquisitionPoint) {
		return TimeUnit.SECONDS.toMillis((notificationTimeInSeconds + acquisitionPoint.getFeedSCCNotificationBuffer()));
	}
	
/*****************************************************************************************************************************************************************************
 * Common Methods ends here.																																				 *
 *****************************************************************************************************************************************************************************/
	
	/*****************************************************************************
	 * Encoder Level Notification Message builder methods starts here.           *
	 *****************************************************************************/
	
	public static NotificationMessage getEncoderLevelStartEventIPNotificationMessage(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final Media media, MediaPoint programStartMediaPoint, MediaPoint programEndMediaPoint, final String altSourceValue) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}
		final long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		final long programStopUTCTime = getThresholdTimeOfMediaPoint(programEndMediaPoint);
		String acquisitionStartSignalId = UUIDUtils.getBase64UrlEncodedUUID();
		String acquisitionStopSignalId = UUIDUtils.getBase64UrlEncodedUUID();

		NotificationMessage notificationMessageForEventStart = getScheduledEventIPNotificationMessage(EventAction.CONFIRMED, null, acquisitionPoint, media);
		notificationMessageForEventStart.setExecutedAtEncoderLevel(true);
		notificationMessageForEventStart.setMediaPointSignalId(programStartMediaPoint.getSignalId());
		notificationMessageForEventStart.setProgramEndMediaPointSignalId(
				programEndMediaPoint != null ? programEndMediaPoint.getSignalId() : null);
		notificationMessageForEventStart.setSignalTime(programStartUTCTime);
		
		//2. Set the time at which this notification needs to be sent.
		//It's a regular start. Use the time from first signal which is always a Program Start Signal.
		int scheduledTime = getFutureStartNotificationTimeInSecs((int) (TimeUnit.MILLISECONDS.toSeconds(programStartUTCTime)), acquisitionPoint);

		notificationMessageForEventStart.setNotificationScheduledTime(scheduledTime);

		//3. Set the acquisitionSignalIds
		Map<SegmentType, String> acquisitionSignalIds = new HashMap<>();
		acquisitionSignalIds.put(SegmentType.PROGRAM_START, acquisitionStartSignalId);
		acquisitionSignalIds.put(SegmentType.PROGRAM_END, acquisitionStopSignalId);
		notificationMessageForEventStart.setAcquisitionSignalIds(acquisitionSignalIds);

		notificationMessageForEventStart.setEventSignalUTCStartTime(programStartUTCTime);
		notificationMessageForEventStart.setEventSignalUTCStopTime(programStopUTCTime);
		notificationMessageForEventStart.setEventAltSourceValue(altSourceValue);

		// 4. Set No Regional blackout and device restrictions
		setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForEventStart, programStartMediaPoint);
		
		return notificationMessageForEventStart;

	}	
	
	public static NotificationMessage getEncoderLevelEndEventIPNotificationMessage(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final int currentSystemTimeInSecs, final Media media, MediaPoint programStartMediaPoint, MediaPoint programEndMediaPoint, 
			final MediaTransaction programStartTransaction, final Map<SegmentType, String> acquisitionSignalIds, boolean isImmediateEndByDelete, 
			final EventAction eventAction) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}

		long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		long programEndUTCTime = getThresholdTimeOfMediaPoint(programEndMediaPoint);
		long scheduleNotificationTimeInSecs = getFutureEndNotificationTimeInSecs((int) (TimeUnit.MILLISECONDS.toSeconds(programEndUTCTime)), acquisitionPoint);
		if (isImmediateEndByDelete) {
			scheduleNotificationTimeInSecs = getImmediateNotificationTimeInSecs(currentSystemTimeInSecs);
			programEndUTCTime = getImmediateUTCTimeInSignalInMillis((int) scheduleNotificationTimeInSecs, acquisitionPoint);
		}

		NotificationMessage notificationMessageForCompleteEvent = getScheduledEventIPNotificationMessage(eventAction, null, acquisitionPoint, media);
		if (notificationMessageForCompleteEvent != null && isValidForNotification(programEndMediaPoint, acquisitionPoint)) {
			notificationMessageForCompleteEvent.setExecutedAtEncoderLevel(true);

			//If this is an update then use the original Program Start Time.
			if (programStartTransaction != null) {
				programStartUTCTime = programStartTransaction.getSignalTimeInMS();
			}
			notificationMessageForCompleteEvent.setEventSignalUTCStartTime(programStartUTCTime);
			notificationMessageForCompleteEvent.setEventSignalUTCStopTime(programEndUTCTime);
			notificationMessageForCompleteEvent.setMediaPointSignalId(programEndMediaPoint.getSignalId());
			notificationMessageForCompleteEvent.setSignalTime(programEndUTCTime);
			
			//2. Set the time at which this notification needs to be sent. In case of encoder level it is supposed to go almost immediately.

			notificationMessageForCompleteEvent.setNotificationScheduledTime((int) scheduleNotificationTimeInSecs);

			//3. Set the acquisitionSignalIds
			notificationMessageForCompleteEvent.setAcquisitionSignalIds(acquisitionSignalIds);

			notificationMessageForCompleteEvent.setZoneIdentity(acquisitionPoint.getZoneIdentity());
			
			// 4. Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForCompleteEvent, programStartMediaPoint);
		}
		return notificationMessageForCompleteEvent;

	}	
	public static NotificationMessage getEncoderLevelDisneyEndEventIPNotificationMessage(final AcquisitionPoint acquisitionPoint, final long currentSystemTime, 
			final int currentSystemTimeInSecs, final Media media, MediaPoint programStartMediaPoint, MediaPoint programEndMediaPoint, 
			final MediaTransaction programStartTransaction, final Map<SegmentType, String> acquisitionSignalIds, boolean isImmediateEndByDelete, 
			final EventAction eventAction) {
		if (programStartMediaPoint == null) {
			programStartMediaPoint = getStartMediaPoint(media);
		}
		if (programEndMediaPoint == null) {
			programEndMediaPoint = getEndMediaPoint(media);
		}

		long programStartUTCTime = getThresholdTimeOfMediaPoint(programStartMediaPoint);
		long programEndUTCTime = getThresholdTimeOfMediaPoint(programEndMediaPoint);
		long scheduleNotificationTimeInSecs = getFutureEndNotificationTimeInSecs((int) (TimeUnit.MILLISECONDS.toSeconds(programEndUTCTime)), acquisitionPoint);
		if (isImmediateEndByDelete) {
			scheduleNotificationTimeInSecs = getImmediateNotificationTimeInSecs(currentSystemTimeInSecs);
			programEndUTCTime = getImmediateUTCTimeInSignalInMillis((int) scheduleNotificationTimeInSecs, acquisitionPoint);
		}

		NotificationMessage notificationMessageForCompleteEvent = getScheduledEventIPNotificationMessage(eventAction, null, acquisitionPoint, media);
		if (notificationMessageForCompleteEvent != null && isValidForNotification(programEndMediaPoint, acquisitionPoint)) {
			notificationMessageForCompleteEvent.setExecutedAtEncoderLevel(true);
			
			notificationMessageForCompleteEvent.setUpStreamNotificationMessageType(UpStreamNotificationMessageType.SCTE224_IP_ENCODER_LEVEL_INBAND);


			//If this is an update then use the original Program Start Time.
			if (programStartTransaction != null) {
				programStartUTCTime = programStartTransaction.getSignalTimeInMS();
			}
			notificationMessageForCompleteEvent.setEventSignalUTCStartTime(programStartUTCTime);
			notificationMessageForCompleteEvent.setEventSignalUTCStopTime(programEndUTCTime);
			notificationMessageForCompleteEvent.setMediaPointSignalId(programEndMediaPoint.getSignalId());
			notificationMessageForCompleteEvent.setSignalTime(programEndUTCTime);
			
			//2. Set the time at which this notification needs to be sent. In case of encoder level it is supposed to go almost immediately.

			notificationMessageForCompleteEvent.setNotificationScheduledTime((int) scheduleNotificationTimeInSecs);

			//3. Set the acquisitionSignalIds
			notificationMessageForCompleteEvent.setAcquisitionSignalIds(acquisitionSignalIds);

			notificationMessageForCompleteEvent.setZoneIdentity(acquisitionPoint.getZoneIdentity());
			
			// 4. Set No Regional blackout and device restrictions
			setNoRegionalBlackoutAndDeviceRestrictions(notificationMessageForCompleteEvent, programStartMediaPoint);
		}
		return notificationMessageForCompleteEvent;

	}	
	private static void setNoRegionalBlackoutAndDeviceRestrictions(NotificationMessage notificationMessage, MediaPoint mediaPoint) {
		// No regional blackout and device restrictions
		notificationMessage.setDeviceRestrictions(mediaPoint.getDeviceRestrictions());
		notificationMessage.setNoRegionalBlackout(mediaPoint.getNoRegionalBlackoutFlag());
	}
	
}
