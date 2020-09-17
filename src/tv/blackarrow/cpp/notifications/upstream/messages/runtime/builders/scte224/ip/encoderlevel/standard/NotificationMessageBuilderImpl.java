package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.encoderlevel.standard;

import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.i03.signaling.AlternateContentType;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.SCTE224NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel.OutOfBandNotificationInfo;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UPIDType;

public class NotificationMessageBuilderImpl extends SCTE224NotificationMessageBuilderImpl {
	private static final Logger log = LogManager.getLogger(NotificationMessageBuilderImpl.class);
	private static NotificationMessageBuilderImpl instance = new NotificationMessageBuilderImpl();
	private static final ObjectFactory I03_SIGNALING_OBJECT_FACTORY = new ObjectFactory();

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
		//Only Stop now case is valid for encoder level inband which a disney case for delete in-flight events 
		case STOP_NOW:
			spNotification = getMMLevelEndEventIPNotification(notificationMessage.getStreamId(), notificationMessage.getEventSignalId(), notificationMessage.getCurrentTime(),
					eventStartTimeInMsWithDelta, eventStopTimeInMsWithDelta, notificationMessage.getAcquisitionSignalIds(), notificationMessage.getNoRegionalBlackout(),
					notificationMessage.getDeviceRestrictions(), notificationMessage.getZoneIdentity());
			message = Schema.i03.getResponseHandler().generateSCCResponse(spNotification);
			break;
		default:
			break;

		}
		return message;
	}

	private static SignalProcessingNotificationType getMMLevelEndEventIPNotification(String acquisitionPointId, String signalID, long currentSystemTime,
			long programStartUTCTimeWithAddedDelta, long programStopUTCTimeWithAddedDelta, final Map<SegmentType, String> acquisitionSignalIds, Boolean noRegionalBlackout,
			int deviceRestrictions, final String zoneIdentity) {

		try {
			final OutOfBandNotificationInfo outOfBandNotificationInfo = new OutOfBandNotificationInfo();

			outOfBandNotificationInfo.setCurrentSystemTime(currentSystemTime);

			outOfBandNotificationInfo.setTransactionSegmentType(SegmentType.PROGRAM_END);

			outOfBandNotificationInfo.setContentIdUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			outOfBandNotificationInfo.setContentIdScheduleInterval(JavaxUtil.getDatatypeFactory().newDuration(0));

			outOfBandNotificationInfo.setContentIDScheduleStartUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			outOfBandNotificationInfo.setContentIDScheduleStopUTCWithAddedDelta(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));

			outOfBandNotificationInfo.setContentDurationInMS(programStopUTCTimeWithAddedDelta - programStartUTCTimeWithAddedDelta);
			outOfBandNotificationInfo.setContentDuration(JavaxUtil.getDatatypeFactory().newDuration(outOfBandNotificationInfo.getContentDurationInMS()));

			outOfBandNotificationInfo.setMediaSignalId(signalID);

			outOfBandNotificationInfo.setAcquisitionPointId(acquisitionPointId);

			outOfBandNotificationInfo.setAcquisitionSignalIds(acquisitionSignalIds);

			outOfBandNotificationInfo.setNoRegionalBlackout(noRegionalBlackout? 1 :0);
			outOfBandNotificationInfo.setDeviceRestrictions(deviceRestrictions);

			return buildEndEventNotification(acquisitionPointId, outOfBandNotificationInfo, zoneIdentity);

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
	private static SignalProcessingNotificationType buildEndEventNotification(final String acquisitionPointId, final OutOfBandNotificationInfo outOfBandNotificationInfo, final String zoneIdentity)
			throws DatatypeConfigurationException {

		final String acquisitionSignalIdForProgramEnd = outOfBandNotificationInfo.getAcquisitionSignalIds().get(SegmentType.PROGRAM_END);
		final String upidStr = ESAMHelper.generateUpidString(outOfBandNotificationInfo.getMediaSignalId());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

		final SignalProcessingNotificationType notification = I03_SIGNALING_OBJECT_FACTORY.createSignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(acquisitionPointId);

		final ResponseSignalType programEndRespSignalType = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		programEndRespSignalType.setBinaryData(I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType());

		SCTE35PointDescriptorType scte35Pnt = I03_SIGNALING_OBJECT_FACTORY.createSCTE35PointDescriptorType();
		SegmentationDescriptorType segment = I03_SIGNALING_OBJECT_FACTORY.createSegmentationDescriptorType();

		programEndRespSignalType.setAcquisitionPointIdentity(outOfBandNotificationInfo.getAcquisitionPointId());
		programEndRespSignalType.setAcquisitionSignalID(acquisitionSignalIdForProgramEnd);
		programEndRespSignalType.setAction(ResponseSignalAction.REPLACE.toString());

		programEndRespSignalType.setSignalPointID(outOfBandNotificationInfo.getMediaSignalId());
		
		//Add Altsource to signal 
		AlternateContentType alternateContenttypeEnd = I03_SIGNALING_OBJECT_FACTORY.createAlternateContentType();
		alternateContenttypeEnd.setAltContentIdentity(StringUtils.EMPTY);
		alternateContenttypeEnd.setZoneIdentity(zoneIdentity);
		programEndRespSignalType.getAlternateContent().add(alternateContenttypeEnd);


		// set UTC for program end ResponseSignal
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

		// Add this program start in the notification.
		notification.getResponseSignal().add(NOTIFICATION_PROGRAM_START_SIGNAL_INDEX, programEndRespSignalType);
		return notification;
	}
}
