package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders;

import java.io.StringReader;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.I03RequestHandler;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.SpliceInsertType;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public abstract class BaseNotificationMessageBuilderImpl implements NotificationMessageBuilder {

	public static final Logger LOG = LogManager.getLogger(BaseNotificationMessageBuilderImpl.class);
	protected static final tv.blackarrow.cpp.i03.signaling.ObjectFactory I03_SIGNALING_OBJECT_FACTORY = new tv.blackarrow.cpp.i03.signaling.ObjectFactory();
	protected static final tv.blackarrow.cpp.signal.signaling.ObjectFactory I01_SIGNALING_CHILD_OBJECT_FACTORY = new tv.blackarrow.cpp.signal.signaling.ObjectFactory();

	protected static final int NOTIFICATION_PROGRAM_START_SIGNAL_INDEX = 0;
	protected static final int NOTIFICATION_CONTENT_ID_SIGNAL_INDEX = 1;

	public static Map<SegmentType, String> getAcquisitionSingalIds() {
		Map<SegmentType, String> acquisitionSignalIds = new HashMap<>();
		String acquisitionStartSignalId = UUIDUtils.getBase64UrlEncodedUUID();
		acquisitionSignalIds.put(SegmentType.PROGRAM_START, acquisitionStartSignalId);
		acquisitionSignalIds.put(SegmentType.CONTENT_IDENTIFICATION, acquisitionStartSignalId);
		return acquisitionSignalIds;
	}

	public static SignalProcessingNotificationType parseSignalProcessingNotification(String spnXML) {
		try (final StringReader reader = new StringReader(spnXML);) {
			final JAXBContext jaxbCxt = I03RequestHandler.linearPOISSccJAXBContext;
			final Unmarshaller unmarshaller = jaxbCxt.createUnmarshaller();
			JAXBElement<SignalProcessingNotificationType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingNotificationType.class);
			return jxbElement.getValue();
		} catch (Exception ex) {
			throw new RuntimeException("Couldn't unmarshall SPN XML into SPN Object.", ex);
		}
	}

	protected XMLGregorianCalendar getXMLGregorianCalenderTime(long utctimewithOffset) throws DatatypeConfigurationException {
		DatatypeFactory df = DatatypeFactory.newInstance();
		GregorianCalendar tempGregorianCalendar = new GregorianCalendar();

		tempGregorianCalendar.setTimeInMillis(utctimewithOffset);
		return df.newXMLGregorianCalendar(tempGregorianCalendar);
	}	

	public static tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType getDeepCopy(SCTE35PointDescriptorType i03Scte35PointDescriptor) {
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

	private static tv.blackarrow.cpp.signal.signaling.SpliceInsertType getDeepCopy(SpliceInsertType i03SpliceInsert) {
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

	private static tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType getDeepCopy(SegmentationDescriptorType seg) {
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
