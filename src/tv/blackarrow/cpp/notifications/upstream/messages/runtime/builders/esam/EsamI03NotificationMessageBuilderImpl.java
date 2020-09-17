package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.I03RequestHandler;
import tv.blackarrow.cpp.i03.signaling.BinarySignalType;
import tv.blackarrow.cpp.i03.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.i03.signaling.EventScheduleType;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.SpliceInsertType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UPIDType;

/**
 * @author nyadav
 *
 */
/**
 * @author nyadav
 *
 */
public class EsamI03NotificationMessageBuilderImpl extends BaseEsamNotificationMessageBuilder implements EsamNotificationMessageBuilder {

	private static final Logger LOG = LogManager.getLogger(EsamI03NotificationMessageBuilderImpl.class);
	private static final ObjectFactory FACTORY = new ObjectFactory();
	private static tv.blackarrow.cpp.signal.signaling.ObjectFactory I01_SIGNALING_CHILD_OBJECT_FACTORY = new tv.blackarrow.cpp.signal.signaling.ObjectFactory();

	/**
	 * Set the program start signal @{@link ResponseSignalType} to {@link SignalProcessingNotificationType}
	 * 
	 * @param notificationMessage
	 * @param signalProcessingNotificationType
	 * @param blackoutEventStartTimeForProgramStartWithOffset
	 * @param eventAction
	 * @param segmentTypeId
	 * @param contentDuretion
	 * @throws Exception
	 */
	public void setProgramStartResponseSignal(final NotificationMessage notificationMessage, final SignalProcessingNotificationType signalProcessingNotificationType,
			final XMLGregorianCalendar blackoutEventStartTimeForProgramStartWithOffset, final String eventAction, SegmentType segmentTypeId, long contentDuretion)
			throws Exception {

		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		ResponseSignalType respSignalType = new ResponseSignalType();
		SegmentationDescriptorType segment = new SegmentationDescriptorType();

		signalProcessingNotificationType.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		signalProcessingNotificationType.getResponseSignal().add(respSignalType);

		final String upidStr = ESAMHelper.generateUpidString(notificationMessage.getEventSignalId());
		final byte[] upId = new HexBinaryAdapter().unmarshal(upidStr);

		respSignalType.setBinaryData(new BinarySignalType());
		respSignalType.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		respSignalType.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
		respSignalType.setAction(eventAction);
		respSignalType.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upId)));
		respSignalType.setUTCPoint(SCCResponseUtil.generateI03UTCPoint(blackoutEventStartTimeForProgramStartWithOffset.toGregorianCalendar().getTimeInMillis()));

		segment.setSegmentEventId(getSegmentEventId(notificationMessage));
		segment.setSegmentTypeId(segmentTypeId.getSegmentTypeId());
		segment.setDuration(getDuration(contentDuretion));

		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		scte35Pnt.setSpliceCommandType(6);

		setSegmentDescriptorAttributesInResponseSignal(segment);
		setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upId, segment);
		setSegmentDescriptorAsBinaryOrXMLInResponseSignal(getPTSTime(), Scte35BinaryUtil.toBitString(0, 33), respSignalType, scte35Pnt);

	}

	public void setContentIndentficationRespSignal(final NotificationMessage notificationMessage, final SignalProcessingNotificationType signalProcessingNotificationType,
			final XMLGregorianCalendar blackoutEventStartTimeForContentIdentification, final XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset, final String eventAction,
			final SegmentType segmentTypeId, long contentDuretion) throws DatatypeConfigurationException {

		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		ResponseSignalType respSignalContentIdentification = new ResponseSignalType();
		SegmentationDescriptorType segment = new SegmentationDescriptorType();

		signalProcessingNotificationType.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		signalProcessingNotificationType.getResponseSignal().add(respSignalContentIdentification);

		final String upidStr = ESAMHelper.generateUpidString(notificationMessage.getEventSignalId());
		final byte[] upId = new HexBinaryAdapter().unmarshal(upidStr);

		blackoutEventStartTimeForContentIdentification.add(DatatypeFactory.newInstance().newDuration(notificationMessage.getAqContentFrequency() * 1000));
		EventScheduleType eventSchedule = getEventSchedule(blackoutEventStartTimeForContentIdentification.toGregorianCalendar().getTimeInMillis(),
				blackoutEventStopTimeAsXMLGCWithOffset.toGregorianCalendar().getTimeInMillis(), notificationMessage.getAqContentFrequency() * 1000);

		respSignalContentIdentification.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		respSignalContentIdentification.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION));
		respSignalContentIdentification.setAction(eventAction);
		respSignalContentIdentification.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upId)));

		respSignalContentIdentification.setEventSchedule(eventSchedule);
		respSignalContentIdentification.setUTCPoint(eventSchedule.getStartUTC());
		respSignalContentIdentification.setUTCPoint(SCCResponseUtil.generateI03UTCPoint(blackoutEventStartTimeForContentIdentification.toGregorianCalendar().getTimeInMillis()));

		scte35Pnt.setSpliceCommandType(6);
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);

		segment.setSegmentEventId(getSegmentEventId(notificationMessage));
		segment.setDuration(getDuration(contentDuretion));
		segment.setSegmentTypeId(segmentTypeId.getSegmentTypeId());

		setBasicSegmentInfoForContentIDSignal(upId, segment);
		setSegmentDescriptorAttributesInResponseSignal(segment);
		setSegmentDescriptorAsBinaryOrXMLInResponseSignal(getPTSTime(), Scte35BinaryUtil.toBitString(0, 33), respSignalContentIdentification, scte35Pnt);

	}

	/**
	 * Set the conditioning info to {@link SignalProcessingNotificationType}
	 * 
	 * @param notificationMessage
	 * @param processingNotificationType
	 * @param contentDuretion
	 * @throws DatatypeConfigurationException
	 */
	public void setConditioningInfo(final NotificationMessage notificationMessage, final SignalProcessingNotificationType processingNotificationType, long contentDuretion)
			throws DatatypeConfigurationException {
		List<ConditioningInfoType> conditioningInfoList = processingNotificationType.getConditioningInfo();
		ConditioningInfoType conditioningInfo = new ConditioningInfoType();
		conditioningInfo.setAcquisitionSignalIDRef(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
		conditioningInfo.setDuration(getDuration(contentDuretion));
		conditioningInfoList.add(conditioningInfo);
	}

	/**
	 * Set a runoverUnplanned signal {@link ResponseSignalType }to {@link SignalProcessingNotificationType}
	 * 
	 * @param notificationMessage
	 * @param signalProcessingNotificationType
	 * @param blackoutEventStartTimeWithOffset
	 * @param blackoutEventStopTimeAsXMLGCWithOffset
	 * @param notificationScheduledTime
	 * @param action
	 * @param programRunoverUnplanned
	 * @throws DatatypeConfigurationException
	 */
	public void setProgramRunoverUnplannedRespSignal(final NotificationMessage notificationMessage, final SignalProcessingNotificationType signalProcessingNotificationType,
			final XMLGregorianCalendar blackoutEventStartTimeWithOffset, final XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset,
			final XMLGregorianCalendar notificationScheduledTime, final String action, final SegmentType programRunoverUnplanned) throws DatatypeConfigurationException {

		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		ResponseSignalType respSignalType = new ResponseSignalType();
		SegmentationDescriptorType segment = new SegmentationDescriptorType();

		signalProcessingNotificationType.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		signalProcessingNotificationType.getResponseSignal().add(respSignalType);

		final String upidStr = ESAMHelper.generateUpidString(notificationMessage.getEventSignalId());
		final byte[] upId = new HexBinaryAdapter().unmarshal(upidStr);

		segment.setSegmentEventId(getSegmentEventId(notificationMessage));
		segment.setSegmentTypeId(programRunoverUnplanned.getSegmentTypeId());
		// duration of this content identification segmentation (Event Stop Time - Current System Time)
		segment.setDuration(getDuration(
				blackoutEventStopTimeAsXMLGCWithOffset.toGregorianCalendar().getTimeInMillis() <= blackoutEventStartTimeWithOffset.toGregorianCalendar().getTimeInMillis()
						? blackoutEventStartTimeWithOffset.toGregorianCalendar().getTimeInMillis()
						: blackoutEventStopTimeAsXMLGCWithOffset.toGregorianCalendar().getTimeInMillis() - notificationMessage.getNotificationScheduledTime()));//getNotificationScheduledTime is currentTime for us

		respSignalType.setBinaryData(new BinarySignalType());
		respSignalType.setAcquisitionPointIdentity(notificationMessage.getStreamId());
		respSignalType.setAcquisitionSignalID(notificationMessage.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
		respSignalType.setAction(action);
		respSignalType.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upId)));
		respSignalType.setUTCPoint(SCCResponseUtil.generateI03UTCPoint(notificationScheduledTime.toGregorianCalendar().getTimeInMillis()));//getNotificationScheduledTime is currentTime for us

		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		scte35Pnt.setSpliceCommandType(6);

		setSegmentDescriptorAttributesInResponseSignal(segment);
		setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upId, segment);
		setSegmentDescriptorAsBinaryOrXMLInResponseSignal(getPTSTime(), Scte35BinaryUtil.toBitString(0, 33), respSignalType, scte35Pnt);
	}

	/**
	 * 
	 * @param upId
	 * @param segment
	 */
	private static void setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(byte[] upId, final SegmentationDescriptorType segment) {
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upId);
		segment.setSegmentNum((short) 1);
		segment.setSegmentsExpected((short) 1);
	}

	/**
	 * 
	 * @param upid
	 * @param segment
	 */
	private static void setBasicSegmentInfoForContentIDSignal(final byte[] upid, final SegmentationDescriptorType segment) {
		segment.setSegmentTypeId(SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId());
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upid);
		segment.setSegmentNum((short) 0);
		segment.setSegmentsExpected((short) 0);
	}

	/**
	 * 
	 * @param segment
	 */
	private static void setSegmentDescriptorAttributesInResponseSignal(final SegmentationDescriptorType segment) {
		segment.getOtherAttributes().put(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG), "0");
		segment.getOtherAttributes().put(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG), "1");
		segment.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), "0");
		segment.getOtherAttributes().put(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG), "1");
		segment.getOtherAttributes().put(new QName(CppConstants.DEVICE_RESTRICTIONS), "0");
	}

	/**
	 * 
	 * @param startOrCurrentTimeWithOffset
	 * @param stopTimeWithOffset
	 * @param frequency
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	private static EventScheduleType getEventSchedule(final long startOrCurrentTimeWithOffset, long stopTimeWithOffset, long frequency) throws DatatypeConfigurationException {
		// fix the issue that endTime is less than start time, when getting the
		// program_end immediately after program start.
		if (stopTimeWithOffset <= startOrCurrentTimeWithOffset) {
			stopTimeWithOffset = startOrCurrentTimeWithOffset;
			frequency = 0;
		}
		EventScheduleType eventSchedule = new EventScheduleType();
		eventSchedule.setStartUTC(SCCResponseUtil.generateI03UTCPoint(startOrCurrentTimeWithOffset));
		eventSchedule.setInterval(JavaxUtil.getDatatypeFactory().newDuration(frequency));
		eventSchedule.setStopUTC((SCCResponseUtil.generateI03UTCPoint(stopTimeWithOffset)));
		return eventSchedule;
	}

	/**
	 * 
	 * @param ptsTimeInBinary
	 * @param pts_adjustment
	 * @param respSignalType
	 * @param scte35Pnt
	 */
	private static void setSegmentDescriptorAsBinaryOrXMLInResponseSignal(final String ptsTimeInBinary, final String pts_adjustment, ResponseSignalType respSignalType,
			SCTE35PointDescriptorType scte35Pnt) {
		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(getDeepCopy(scte35Pnt), ptsTimeInBinary, pts_adjustment);
		BinarySignalType binarySignal = new BinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType("SCTE35");
		respSignalType.setBinaryData(binarySignal);
		respSignalType.setSCTE35PointDescriptor(null);

	}

	/**
	 * 
	 * @param notification
	 * @return
	 * @throws Exception
	 */
	public String marshalMessage(final SignalProcessingNotificationType notification) throws Exception {
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		try {

			Marshaller marshaller = I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jxbElement = FACTORY.createSignalProcessingNotification(notification);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOG.error(()->e.getMessage());
		}
		return writer.toString();
	}

	/**
	 * 
	 * @param i03Scte35PointDescriptor
	 * @return
	 */
	private static tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType getDeepCopy(final SCTE35PointDescriptorType i03Scte35PointDescriptor) {
		if (i03Scte35PointDescriptor == null) {
			return null;
		}
		tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType i01Scte35PointDescriptorType = I01_SIGNALING_CHILD_OBJECT_FACTORY.createSCTE35PointDescriptorType();
		i01Scte35PointDescriptorType.setSpliceCommandType(i03Scte35PointDescriptor.getSpliceCommandType());
		for (SegmentationDescriptorType i03SegmentDescriptor : i03Scte35PointDescriptor.getSegmentationDescriptorInfo()) {
			i01Scte35PointDescriptorType.getSegmentationDescriptorInfo().add(getDeepCopy(i03SegmentDescriptor));
		}
		i01Scte35PointDescriptorType.setSpliceInsert(getDeepCopy(i03Scte35PointDescriptor.getSpliceInsert()));
		i01Scte35PointDescriptorType.getOtherAttributes().putAll(i03Scte35PointDescriptor.getOtherAttributes());
		return i01Scte35PointDescriptorType;
	}

	/**
	 * 
	 * @param i03SpliceInsert
	 * @return
	 */
	private static tv.blackarrow.cpp.signal.signaling.SpliceInsertType getDeepCopy(final SpliceInsertType i03SpliceInsert) {
		if (i03SpliceInsert == null) {
			return null;
		}
		tv.blackarrow.cpp.signal.signaling.SpliceInsertType i01SpliceInsertType = I01_SIGNALING_CHILD_OBJECT_FACTORY.createSpliceInsertType();
		i01SpliceInsertType.setAvailNum(i03SpliceInsert.getAvailNum());
		i01SpliceInsertType.setAvailsExpected(i03SpliceInsert.getAvailsExpected());
		i01SpliceInsertType.setDuration(i03SpliceInsert.getDuration());
		i01SpliceInsertType.setOutOfNetworkIndicator(i03SpliceInsert.isOutOfNetworkIndicator());
		i01SpliceInsertType.setSpliceEventCancelIndicator(i03SpliceInsert.isSpliceEventCancelIndicator());
		i01SpliceInsertType.setSpliceEventId(i03SpliceInsert.getSpliceEventId());
		i01SpliceInsertType.setUniqueProgramId(i03SpliceInsert.getUniqueProgramId());
		i01SpliceInsertType.getOtherAttributes().putAll(i03SpliceInsert.getOtherAttributes());
		return i01SpliceInsertType;
	}

	/**
	 * 
	 * @param seg
	 * @return
	 */
	private static tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType getDeepCopy(final SegmentationDescriptorType seg) {
		if (seg == null) {
			return null;
		}
		tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType segmentationDescriptorType = I01_SIGNALING_CHILD_OBJECT_FACTORY.createSegmentationDescriptorType();
		segmentationDescriptorType.setDuration(seg.getDuration());
		segmentationDescriptorType.setSegmentationEventCancelIndicator(seg.isSegmentationEventCancelIndicator());
		segmentationDescriptorType.setSegmentEventId(seg.getSegmentEventId());
		segmentationDescriptorType.setSegmentNum(seg.getSegmentNum());
		segmentationDescriptorType.setSegmentsExpected(seg.getSegmentsExpected());
		segmentationDescriptorType.setSegmentTypeId(seg.getSegmentTypeId());
		segmentationDescriptorType.setUpid(seg.getUpid());
		segmentationDescriptorType.setUpidType(seg.getUpidType());
		segmentationDescriptorType.getOtherAttributes().putAll(seg.getOtherAttributes());
		return segmentationDescriptorType;
	}
}