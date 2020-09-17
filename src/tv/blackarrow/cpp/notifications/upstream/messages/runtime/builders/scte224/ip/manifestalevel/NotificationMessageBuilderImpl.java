package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.i03.signaling.BinarySignalType;
import tv.blackarrow.cpp.i03.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.i03.signaling.EventScheduleType;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.SCTE224NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UPIDType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class NotificationMessageBuilderImpl extends SCTE224NotificationMessageBuilderImpl {
	private static final Logger log = LogManager.getLogger(NotificationMessageBuilderImpl.class);
	private static NotificationMessageBuilderImpl instance = new NotificationMessageBuilderImpl();

	public static final int MM_NOTIFICATION_SIGNAL_CONTENT_ID_INDEX = 0;
	private static final SCTE224DataManager SCTE224_DATA_MANAGER = DataManagerFactory.getSCTE224DataManager();

	public static NotificationMessageBuilderImpl getInstance() {
		return instance;
	}

	@Override
	protected String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		String message = null;
		SignalProcessingNotificationType spNotification = null;
		long eventStartTimeInMsWithDelta = notificationMessage.getEventSignalUTCStartTime();
		long eventStopTimeInMsWithDelta = notificationMessage.getEventSignalUTCStopTime();
		
		switch (notificationMessage.getEventAction()) {
		case CONFIRMED:
		case UPDATE:
			spNotification = getMMLevelStartOrUpdateEventIPNotification(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(), 
					notificationMessage.getAqContentFrequency(),notificationMessage.getTransactionSegmentType(), notificationMessage.getCurrentTime(), 
					eventStartTimeInMsWithDelta, eventStopTimeInMsWithDelta, notificationMessage.getContentDuration(), 
					notificationMessage.getAcquisitionSignalIds(), notificationMessage.getNoRegionalBlackout(), 
					notificationMessage.getDeviceRestrictions());
			message = Schema.i03.getResponseHandler().generateSCCResponse(spNotification);
			break;
		case STOP_NOW:
		case COMPLETE:
			spNotification = getMMLevelEndEventIPNotification(notificationMessage.getStreamId(), notificationMessage.getZoneIdentity(), notificationMessage.getEventSignalId(),
					notificationMessage.getCurrentTime(), eventStartTimeInMsWithDelta, eventStopTimeInMsWithDelta, notificationMessage.getAcquisitionSignalIds(),
					notificationMessage.getNoRegionalBlackout(), notificationMessage.getDeviceRestrictions());
			message = Schema.i03.getResponseHandler().generateSCCResponse(spNotification);
			break;
		default:
			break;
		}

		return message;
	}

	private static SignalProcessingNotificationType getMMLevelEndEventIPNotification(String acquisitionPointId, String zoneIdentity, String signalID, long currentSystemTime,
			long programStartUTCTimeWithAddedDelta, long programStopUTCTimeWithAddedDelta, final Map<SegmentType, String> acquisitionSignalIds, Boolean noRegionalBlackout, int deviceRestrictions) {

		try {
			final OutOfBandNotificationInfo outOfBandNotificationInfo = new OutOfBandNotificationInfo();

			// 0. Set the current system time.
			outOfBandNotificationInfo.setCurrentSystemTime(currentSystemTime);

			// 1. Transaction Type for Program End
			outOfBandNotificationInfo.setTransactionSegmentType(SegmentType.PROGRAM_END);

			// 3. Calculate the UTC time for the Content ID Signal.
			outOfBandNotificationInfo.setContentIdUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			// 4. Calculate the interval between content id requests.
			outOfBandNotificationInfo.setContentIdScheduleInterval(JavaxUtil.getDatatypeFactory().newDuration(0));

			// 5. Calculate the UTC time for the Content ID Event Schedule Start Time.
			outOfBandNotificationInfo.setContentIDScheduleStartUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			// 6. Calculate the UTC time for the Content ID Event Schedule Stop Time.
			outOfBandNotificationInfo.setContentIDScheduleStopUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			// 7. Calculate Duration to set in the segment information.
			outOfBandNotificationInfo.setContentDurationInMS(programStopUTCTimeWithAddedDelta - programStartUTCTimeWithAddedDelta);
			outOfBandNotificationInfo.setContentDuration(JavaxUtil.getDatatypeFactory().newDuration(outOfBandNotificationInfo.getContentDurationInMS()));

			// 8. Media Signal Id
			outOfBandNotificationInfo.setMediaSignalId(signalID);

			// 9. Set the AP ID
			outOfBandNotificationInfo.setAcquisitionPointId(acquisitionPointId);

			// 10. Set the Acquisition Signal Ids.
			outOfBandNotificationInfo.setAcquisitionSignalIds(acquisitionSignalIds);

			outOfBandNotificationInfo.setNoRegionalBlackout(noRegionalBlackout ? 1 : 0);
			outOfBandNotificationInfo.setDeviceRestrictions(deviceRestrictions);

			return buildEndEventNotification(acquisitionPointId, outOfBandNotificationInfo);
		} catch (Exception ex) {
			log.error("Error occured while prepairing the notification message: " + ex.getMessage());
		}
		return null;
	}

	/**
	 * @param acquisitionPointId
	 * @param outOfBandNotificationInfo
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	private static SignalProcessingNotificationType buildEndEventNotification(String acquisitionPointId, final OutOfBandNotificationInfo outOfBandNotificationInfo)
			throws DatatypeConfigurationException {

		final String acquisitionSignalIdForProgramEnd = StringUtils.isNotBlank(outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END))
				? outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END) : UUIDUtils.getBase64UrlEncodedUUID();

		final String acquisitionSignalIdForContentId = outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION);
		final String upidStr = ESAMHelper.generateUpidString(outOfBandNotificationInfo.getMediaSignalId());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

		// 0. Create Out-of-Band notification message
		final SignalProcessingNotificationType notification = I03_SIGNALING_OBJECT_FACTORY.createSignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(acquisitionPointId);

		// 1. Create program start component of the notification.
		final ResponseSignalType respSignalProgramEnd = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalProgramEnd.setBinaryData(I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType());
		respSignalProgramEnd.setSignalPointID(outOfBandNotificationInfo.getMediaSignalId());
		setProgramEndResponseSignal(respSignalProgramEnd, upid, acquisitionSignalIdForProgramEnd, outOfBandNotificationInfo);

		// Add this program start in the notification.
		notification.getResponseSignal().add(NOTIFICATION_PROGRAM_START_SIGNAL_INDEX, respSignalProgramEnd);

		// 2. Create Content Id component of the notification.
		createContentIndentficationRespSignal(upid, acquisitionSignalIdForContentId, outOfBandNotificationInfo, notification);

		//3. Generate Conditioning Info
		generateConditioningInfo(notification, respSignalProgramEnd.getAcquisitionSignalID(), outOfBandNotificationInfo.getContentDuration());
		return notification;
	}

	private static SignalProcessingNotificationType getMMLevelStartOrUpdateEventIPNotification(String acquisitionPointId, String signalID, int aqContentIDFrequency,
			List<SegmentType> trasactionSegmentTypeid, long currentSystemTime, long programStartUTCTimeWithAddedDelta, long programStopUTCTimeWithAddedDelta, long contentDurationInMillis,
			final Map<SegmentType, String> acquisitionSignalIds, Boolean noRegionalBlackout, int deviceRestrictions) {

		try {
			final OutOfBandNotificationInfo outOfBandNotificationInfo = new OutOfBandNotificationInfo();

			// 0. Set the current system time.
			outOfBandNotificationInfo.setCurrentSystemTime(currentSystemTime);

			// 1. Transaction Type for this notification.
			outOfBandNotificationInfo.setTransactionSegmentType(trasactionSegmentTypeid.get(0));

			// 2. Calculate the Start UTC time for the Program Start Signal.
			outOfBandNotificationInfo.setProgramStartUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStartUTCTimeWithAddedDelta));

			// 3. Calculate the UTC time for the Content ID Signal.
			outOfBandNotificationInfo.setContentIdUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStartUTCTimeWithAddedDelta + (aqContentIDFrequency * 1000)));

			// 4. Calculate the interval between content id requests.
			outOfBandNotificationInfo.setContentIdScheduleInterval(JavaxUtil.getDatatypeFactory().newDuration(aqContentIDFrequency * 1000));

			// 5. Calculate the UTC time for the Content ID Event Schedule Start Time.
			outOfBandNotificationInfo.setContentIDScheduleStartUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStartUTCTimeWithAddedDelta + (aqContentIDFrequency * 1000)));

			// 6. Calculate Duration to set in the segment information.
			if (contentDurationInMillis > 0l) {
				outOfBandNotificationInfo.setContentDurationInMS(contentDurationInMillis);
				outOfBandNotificationInfo.setContentDuration(JavaxUtil.getDatatypeFactory().newDuration(outOfBandNotificationInfo.getContentDurationInMS()));
			}

			// 7. Calculate the UTC time for the Content ID Event Schedule Stop Time.
			Long contentIDScheduleStopTimeUTCWithAddedDelta = null;
			if (outOfBandNotificationInfo.getContentDurationInMS() != null && outOfBandNotificationInfo.getContentDurationInMS() > -1) {
				contentIDScheduleStopTimeUTCWithAddedDelta = programStartUTCTimeWithAddedDelta + outOfBandNotificationInfo.getContentDurationInMS();
				outOfBandNotificationInfo.setContentIDScheduleStopUTCWithAddedDelta(SCCResponseUtil.generateUTC(contentIDScheduleStopTimeUTCWithAddedDelta));
			}

			// 8. Media Signal Id
			outOfBandNotificationInfo.setMediaSignalId(signalID);

			// 9. Set the AP ID
			outOfBandNotificationInfo.setAcquisitionPointId(acquisitionPointId);

			// 10. Set the Acquisition Signal Ids.
			outOfBandNotificationInfo.setAcquisitionSignalIds(acquisitionSignalIds);

			outOfBandNotificationInfo.setNoRegionalBlackout(noRegionalBlackout ? 1 : 0);
			outOfBandNotificationInfo.setDeviceRestrictions(deviceRestrictions);

			return buildStartOrUpdateEventNotification(acquisitionPointId, outOfBandNotificationInfo, trasactionSegmentTypeid);
		} catch (Exception ex) {
			log.error("Error occured while prepairing the notification message: " + ex.getMessage());
		}
		return null;
	}

	/**
	 * @param acquisitionPointId
	 * @param outOfBandNotificationInfo
	 * @param trasactionSegmentTypeid 
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	private static SignalProcessingNotificationType buildStartOrUpdateEventNotification(String acquisitionPointId, final OutOfBandNotificationInfo outOfBandNotificationInfo,
			List<SegmentType> trasactionSegmentTypeid) throws DatatypeConfigurationException {		
		final String acquisitionSignalIdForProgramStart = outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START);

		final String acquisitionSignalIdForContentId = outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION);

		// 0. Create Out-of-Band notification message
		final SignalProcessingNotificationType notification = I03_SIGNALING_OBJECT_FACTORY.createSignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(acquisitionPointId);

		for (SegmentType segmentType : trasactionSegmentTypeid) {

			switch (segmentType) {
			case PROGRAM_BLACKOUT_OVERRIDE:
				generateBlackoutOverrideResponse(outOfBandNotificationInfo, acquisitionSignalIdForContentId, notification);
				break;
			case PROGRAM_RUNOVER_UNPLANNED:
				generateRunoverUnplannedResponse(outOfBandNotificationInfo, acquisitionSignalIdForProgramStart, 
						acquisitionSignalIdForContentId, notification);
				break;
			case PROGRAM_OVERLAP_START:
			case PROGRAM_START:
				generateProgramStartResponse(outOfBandNotificationInfo, acquisitionSignalIdForProgramStart, 
						acquisitionSignalIdForContentId, notification);
				break;
			default:
				break;
			}

		}

		return notification;
	}

	private static void generateRunoverUnplannedResponse(OutOfBandNotificationInfo outOfBandNotificationInfo, String acquisitionSignalIdForProgramStart,
			String acquisitionSignalIdForContentId, SignalProcessingNotificationType notification) throws DatatypeConfigurationException {
		
		final String mediaSignalId = outOfBandNotificationInfo.getMediaSignalId();		
		MediaLedger mediaLedger = SCTE224_DATA_MANAGER.getAcquisitionPointMediaLedger(outOfBandNotificationInfo.getAcquisitionPointId(), 
					mediaSignalId);
		
		final String mediaSignalIdWithTerritoryUpdateCounter = mediaLedger != null ? mediaLedger.getSignalIdWithTerritoryCounter(mediaSignalId) : mediaSignalId;
		final String upidStrWithTerritoryUpdateCounter = ESAMHelper.generateUpidString(mediaSignalIdWithTerritoryUpdateCounter);
		final byte[] upidWithTerritoryUpdateCounter = new HexBinaryAdapter().unmarshal(upidStrWithTerritoryUpdateCounter);
		
		final ResponseSignalType respSignalProgramRunoverUnplanned = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalProgramRunoverUnplanned.setBinaryData(I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType());
		respSignalProgramRunoverUnplanned.setAcquisitionSignalID(UUIDUtils.getBase64UrlEncodedUUID());
		respSignalProgramRunoverUnplanned.setSignalPointID(mediaSignalIdWithTerritoryUpdateCounter);
		setBaseResponseSignal(respSignalProgramRunoverUnplanned, SegmentType.PROGRAM_RUNOVER_UNPLANNED, upidWithTerritoryUpdateCounter, 
				respSignalProgramRunoverUnplanned.getAcquisitionSignalID(),	outOfBandNotificationInfo);
		// Add this program start in the notification.
		notification.getResponseSignal().add(NOTIFICATION_PROGRAM_START_SIGNAL_INDEX, respSignalProgramRunoverUnplanned);
	}

	private static void generateProgramStartResponse(final OutOfBandNotificationInfo outOfBandNotificationInfo, final String acquisitionSignalIdForProgramStart,
			final String acquisitionSignalIdForContentId, final SignalProcessingNotificationType notification) throws DatatypeConfigurationException {
		
		final String upidStr = ESAMHelper.generateUpidString(outOfBandNotificationInfo.getMediaSignalId());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		
		// 1. Generate Program Start Override Notification Response Signal
		final ResponseSignalType respSignalProgramStart = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalProgramStart.setBinaryData(I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType());
		respSignalProgramStart.setSignalPointID(outOfBandNotificationInfo.getMediaSignalId());
		setBaseResponseSignal(respSignalProgramStart, SegmentType.PROGRAM_START, upid, respSignalProgramStart.getAcquisitionSignalID(), outOfBandNotificationInfo);
		// Add this response signal to the notification.
		notification.getResponseSignal().add(NOTIFICATION_PROGRAM_START_SIGNAL_INDEX, respSignalProgramStart);
		respSignalProgramStart.setAcquisitionSignalID(acquisitionSignalIdForProgramStart);

		// 2. Create Content Id component of the notification.		
		createContentIndentficationRespSignal(upid, acquisitionSignalIdForContentId, outOfBandNotificationInfo, notification);

		//3. Generate Conditioning Info
		Duration duration = JavaxUtil.getDatatypeFactory().newDuration(0l);
		generateConditioningInfo(notification, respSignalProgramStart.getAcquisitionSignalID(), duration);
	}

	private static void generateBlackoutOverrideResponse(final OutOfBandNotificationInfo outOfBandNotificationInfo, final String acquisitionSignalIdForContentId,
			final SignalProcessingNotificationType notification) throws DatatypeConfigurationException {
		final String mediaSignalId = outOfBandNotificationInfo.getMediaSignalId();
		final String upidStr = ESAMHelper.generateUpidString(mediaSignalId);
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		
		MediaLedger mediaLedger = SCTE224_DATA_MANAGER.getAcquisitionPointMediaLedger(outOfBandNotificationInfo.getAcquisitionPointId(), 
					mediaSignalId);
		
		final String mediaSignalIdWithTerritoryUpdateCounter = mediaLedger != null ? mediaLedger.getSignalIdWithTerritoryCounter(mediaSignalId) : mediaSignalId;
		final String upidStrWithTerritoryUpdateCounter = ESAMHelper.generateUpidString(mediaSignalIdWithTerritoryUpdateCounter);
		final byte[] upidWithTerritoryUpdateCounter = new HexBinaryAdapter().unmarshal(upidStrWithTerritoryUpdateCounter);
		
		// 1. Generate Blackout Override Notification Response Signal
		final ResponseSignalType respSignalBlackoutOverride = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalBlackoutOverride.setBinaryData(I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType());
		respSignalBlackoutOverride.setSignalPointID(mediaSignalIdWithTerritoryUpdateCounter);
		
		setBaseResponseSignal(respSignalBlackoutOverride, SegmentType.PROGRAM_BLACKOUT_OVERRIDE, upidWithTerritoryUpdateCounter, 
				respSignalBlackoutOverride.getAcquisitionSignalID(), outOfBandNotificationInfo);
		// Add this response signal to the notification.
		notification.getResponseSignal().add(NOTIFICATION_PROGRAM_START_SIGNAL_INDEX, respSignalBlackoutOverride);
		respSignalBlackoutOverride.setAcquisitionSignalID(UUIDUtils.getBase64UrlEncodedUUID());

		// 2. Create Content Id component of the notification.
		createContentIndentficationRespSignal(upid, acquisitionSignalIdForContentId, outOfBandNotificationInfo, notification);
	}

	private static void generateConditioningInfo(SignalProcessingNotificationType notificationResponse, String acquisitionSignalId, Duration duration) throws DatatypeConfigurationException {
		List<tv.blackarrow.cpp.i03.signaling.ConditioningInfoType> conditioningInfoList = notificationResponse.getConditioningInfo();
		ConditioningInfoType conditioningInfo = getConditioningInfo(acquisitionSignalId, duration);
		conditioningInfoList.add(conditioningInfo);
	}

	private static ConditioningInfoType getConditioningInfo(final String acquisitionSignalId, Duration duration) throws DatatypeConfigurationException {
		ConditioningInfoType conditioningInfo = new ConditioningInfoType();
		conditioningInfo.setAcquisitionSignalIDRef(acquisitionSignalId);
		if (duration != null) {
			conditioningInfo.setDuration(duration);
		}
		return conditioningInfo;
	}

	protected static void setBaseResponseSignal(final ResponseSignalType baseRespSignalType, SegmentType segmentTypeId, final byte[] upid, final String acquisitionSignalId,
			final OutOfBandNotificationInfo outOfBandNotificationInfo) throws DatatypeConfigurationException {

		SCTE35PointDescriptorType scte35Pnt = I03_SIGNALING_OBJECT_FACTORY.createSCTE35PointDescriptorType();
		SegmentationDescriptorType segment = I03_SIGNALING_OBJECT_FACTORY.createSegmentationDescriptorType();

		baseRespSignalType.setAcquisitionPointIdentity(outOfBandNotificationInfo.getAcquisitionPointId());
		baseRespSignalType.setAcquisitionSignalID(acquisitionSignalId);
		baseRespSignalType.setAction(ResponseSignalAction.CREATE.toString());

		// set UTC for program start ResponseSignal
		UTCPointDescriptorType programStartUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		programStartUTCPoint.setUtcPoint(outOfBandNotificationInfo.getProgramStartUTCWithAddedDelta());
		baseRespSignalType.setUTCPoint(programStartUTCPoint);
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(outOfBandNotificationInfo.getCurrentSystemTime() & 0x3fffffff);

		// duration of this content identification segmentation
		if (outOfBandNotificationInfo.getContentDuration() != null) {
			segment.getOtherAttributes().put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
			segment.setDuration(outOfBandNotificationInfo.getContentDuration());
		}

		segment.setSegmentTypeId(segmentTypeId.getSegmentTypeId());

		setBasicSegmentInfo(upid, UPIDType.CABLELAB_ADI.getUPIDTypeId(), segment);

		setSegmentDescriptorAttributesInResponseSignal(segment, outOfBandNotificationInfo);

		setSegmentDescriptorAsBinaryInResponseSignal("", Scte35BinaryUtil.toBitString(0, 33), baseRespSignalType, scte35Pnt);

	}

	protected static void setProgramEndResponseSignal(final ResponseSignalType programEndRespSignalType, final byte[] upid, final String acquisitionSignalId,
			final OutOfBandNotificationInfo outOfBandNotificationInfo) throws DatatypeConfigurationException {

		SCTE35PointDescriptorType scte35Pnt = I03_SIGNALING_OBJECT_FACTORY.createSCTE35PointDescriptorType();
		SegmentationDescriptorType segment = I03_SIGNALING_OBJECT_FACTORY.createSegmentationDescriptorType();

		programEndRespSignalType.setAcquisitionPointIdentity(outOfBandNotificationInfo.getAcquisitionPointId());
		programEndRespSignalType.setAcquisitionSignalID(acquisitionSignalId);
		programEndRespSignalType.setAction(ResponseSignalAction.CREATE.toString());

		// set UTC for program start ResponseSignal
		UTCPointDescriptorType programEndUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		programEndUTCPoint.setUtcPoint(outOfBandNotificationInfo.getContentIdUTCWithAddedDelta());
		programEndRespSignalType.setUTCPoint(programEndUTCPoint);
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(outOfBandNotificationInfo.getCurrentSystemTime() & 0x3fffffff);

		// duration of this content identification segmentation
		if (outOfBandNotificationInfo.getContentDuration() != null) {
			segment.getOtherAttributes().put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
			segment.setDuration(outOfBandNotificationInfo.getContentDuration());
		}

		segment.setSegmentTypeId(SegmentType.PROGRAM_END.getSegmentTypeId());

		setBasicSegmentInfo(upid, UPIDType.CABLELAB_ADI.getUPIDTypeId(), segment);

		setSegmentDescriptorAttributesInResponseSignal(segment, outOfBandNotificationInfo);

		setSegmentDescriptorAsBinaryInResponseSignal("", Scte35BinaryUtil.toBitString(0, 33), programEndRespSignalType, scte35Pnt);

	}

	protected static void createContentIndentficationRespSignal(final byte[] upid, final String acquisitionSignalId,
			final OutOfBandNotificationInfo outOfBandNotificationInfo, SignalProcessingNotificationType notification) throws DatatypeConfigurationException {
		if (!CppConfigurationBean.getInstance().isScte224CadentContentIDGenerationEnabled()) {
			return;
		}
		final ResponseSignalType respSignalContentIdentification = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalContentIdentification.setSignalPointID(outOfBandNotificationInfo.getMediaSignalId());
		// Add this content identification in the notification.
		notification.getResponseSignal().add(NOTIFICATION_CONTENT_ID_SIGNAL_INDEX, respSignalContentIdentification);

		respSignalContentIdentification.setAcquisitionPointIdentity(outOfBandNotificationInfo.getAcquisitionPointId());
		respSignalContentIdentification.setAcquisitionSignalID(acquisitionSignalId);
		respSignalContentIdentification.setAction(ResponseSignalAction.CREATE.toString());
		if (!SegmentType.PROGRAM_START.equals(outOfBandNotificationInfo.getTransactionSegmentType())) {
			respSignalContentIdentification.setAction(ResponseSignalAction.REPLACE.toString());
		}

		EventScheduleType eventSchedule = new EventScheduleType();

		UTCPointDescriptorType contentIdScheduleStartUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		contentIdScheduleStartUTCPoint.setUtcPoint(outOfBandNotificationInfo.getContentIDScheduleStartUTCWithAddedDelta());
		eventSchedule.setStartUTC(contentIdScheduleStartUTCPoint);

		eventSchedule.setInterval(outOfBandNotificationInfo.getContentIdScheduleInterval());

		if (!SegmentType.PROGRAM_START.equals(outOfBandNotificationInfo.getTransactionSegmentType()) && outOfBandNotificationInfo.getContentIDScheduleStopUTCWithAddedDelta() != null) {
			UTCPointDescriptorType contentIdScheduleStopUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
			contentIdScheduleStopUTCPoint.setUtcPoint(outOfBandNotificationInfo.getContentIDScheduleStopUTCWithAddedDelta());
			eventSchedule.setStopUTC(contentIdScheduleStopUTCPoint);
		}

		respSignalContentIdentification.setEventSchedule(eventSchedule);

		UTCPointDescriptorType contentIdUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		contentIdUTCPoint.setUtcPoint(outOfBandNotificationInfo.getContentIdUTCWithAddedDelta());
		respSignalContentIdentification.setUTCPoint(contentIdUTCPoint);

		SCTE35PointDescriptorType scte35Pnt = I03_SIGNALING_OBJECT_FACTORY.createSCTE35PointDescriptorType();
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
		SegmentationDescriptorType segment = I03_SIGNALING_OBJECT_FACTORY.createSegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(outOfBandNotificationInfo.getCurrentSystemTime() & 0x3fffffff);

		if (outOfBandNotificationInfo.getContentDuration() != null) {
			segment.getOtherAttributes().put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
			segment.setDuration(outOfBandNotificationInfo.getContentDuration());
		}

		setBasicSegmentInfoForContentIDSignal(upid, segment);
		setSegmentDescriptorAttributesInResponseSignal(segment, outOfBandNotificationInfo);

		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(getDeepCopy(scte35Pnt), "", Scte35BinaryUtil.toBitString(0, 33));
		BinarySignalType binarySignal = I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType("SCTE35");
		respSignalContentIdentification.setBinaryData(binarySignal);

	}

	private static void setBasicSegmentInfoForContentIDSignal(final byte[] upid, final SegmentationDescriptorType segment) {
		segment.setSegmentTypeId(SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId()); // content identification
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upid);
		segment.setSegmentNum((short) 0);
		segment.setSegmentsExpected((short) 0);
	}

}
