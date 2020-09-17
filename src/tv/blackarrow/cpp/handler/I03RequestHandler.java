package tv.blackarrow.cpp.handler;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.ds.common.util.XmlUtils;

public class I03RequestHandler implements RequestHandler {
	
	private static Logger LOGGER = LogManager.getLogger(I03RequestHandler.class);
	public static final String LINEAR_POIS_SCC_PACKAGE_NAME = "tv.blackarrow.cpp.i03.signaling";
	public static final String LINEAR_POIS_MCC_PACKAGE_NAME = "tv.blackarrow.cpp.i03.manifest";
	public static JAXBContext linearPOISSccJAXBContext = null;
	public static JAXBContext linearPOISMccJAXBContext = null;
	static{
		try {
			linearPOISSccJAXBContext = JAXBContext.newInstance(LINEAR_POIS_SCC_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOGGER.error(()->"Exception occured in getting JAXB context for context path: "	+ LINEAR_POIS_SCC_PACKAGE_NAME, e);
		}
		try {
			linearPOISMccJAXBContext = JAXBContext.newInstance(LINEAR_POIS_MCC_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOGGER.error(()->"Exception occured in getting JAXB context for context path: "	+ LINEAR_POIS_MCC_PACKAGE_NAME, e);
		}
	}

	@Override
	public SignalProcessingEventType parseSCCRequest(String requestXml) throws Exception {
		if(!requestXml.contains(CppConstants.IO3_NAMESPACE_SIGNALING)){
			throw new RuntimeException("ESAM I03 error. Please ensure messages sent to this endpoint comply with ESAM I03 schema.");
		}
		
		final StringReader reader = new StringReader(requestXml);
		final Unmarshaller unmarshaller	= linearPOISSccJAXBContext.createUnmarshaller();
		JAXBElement<tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType.class);			
		SignalProcessingEventType t =convertToI01EventType(jxbElement.getValue());
		if(t== null || t.getAcquiredSignal() == null || t.getAcquiredSignal().get(0) == null || 
				(t.getAcquiredSignal().get(0).getBinaryData() == null && t.getAcquiredSignal().get(0).getSCTE35PointDescriptor() == null)){
			throw new RuntimeException("ESAM I03 error. Please ensure messages sent to this endpoint comply with ESAM I03 schema.");
		}
		
		return t;
	}
	
	private SignalProcessingEventType convertToI01EventType(tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType evenType){
		SignalProcessingEventType type = new SignalProcessingEventType();
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Decrypted Binary :\n" + XmlUtils.getJAXBString(tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType.class,
						new tv.blackarrow.cpp.i03.signaling.ObjectFactory().createSignalProcessingEvent(evenType)));
			}
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		for(tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType.AcquiredSignal signal:evenType.getAcquiredSignal()){
			type.getAcquiredSignal().add(convertToI01Signal(signal));
		}
		type.getOtherAttributes().putAll(evenType.getOtherAttributes());
		return type;
	}

	private AcquiredSignal convertToI01Signal(tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType.AcquiredSignal signal) {
		if(signal == null){return null;}
		AcquiredSignal sig = new AcquiredSignal();
		sig.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		sig.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		sig.setAcquisitionTime(signal.getAcquisitionTime());
		sig.setBinaryData(convertToI01BinaryData(signal.getBinaryData()));
		sig.setSCTE35PointDescriptor(convertToI01SCTE35PointDescriptor(signal.getSCTE35PointDescriptor()));
		sig.setUTCPoint(convertToI01UTCPoint(signal.getUTCPoint()));
		sig.setStreamTimes(convertToI01StreamTimes(signal.getStreamTimes()));
		sig.getOtherAttributes().putAll(signal.getOtherAttributes());
		return sig;
	}

	private StreamTimesType convertToI01StreamTimes(tv.blackarrow.cpp.i03.signaling.StreamTimesType streamTimes) {
		if(streamTimes == null){return null;}
		StreamTimesType t = new StreamTimesType();
		for(tv.blackarrow.cpp.i03.signaling.StreamTimeType s: streamTimes.getStreamTime()){
			t.getStreamTime().add(convertToI01StreamTime(s));
		}
		t.getOtherAttributes().putAll(streamTimes.getOtherAttributes());
		return t;
	}

	private StreamTimeType convertToI01StreamTime(tv.blackarrow.cpp.i03.signaling.StreamTimeType s) {
		if(s == null ){return null;}
		StreamTimeType t = new StreamTimeType();
		t.setTimeType(s.getTimeType());
		t.setTimeValue(s.getTimeValue());
		t.getOtherAttributes().putAll(s.getOtherAttributes());
		return t;
	}

	private UTCPointDescriptorType convertToI01UTCPoint(tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType utcPoint) {
		if(utcPoint == null){throw new RuntimeException("Unable to get UTC time at request, maybe a wrong format request received.");}
		UTCPointDescriptorType t = new UTCPointDescriptorType();
		t.setUtcPoint(utcPoint.getUtcPoint());
		t.getOtherAttributes().putAll(utcPoint.getOtherAttributes());
		return t;
	}

	private SCTE35PointDescriptorType convertToI01SCTE35PointDescriptor(tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		SCTE35PointDescriptorType des = new SCTE35PointDescriptorType();
		des.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		des.setSpliceInsert(convertToI01SpliceInsert(scte35PointDescriptor.getSpliceInsert()));
		
		for(tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType type:scte35PointDescriptor.getSegmentationDescriptorInfo()){
			des.getSegmentationDescriptorInfo().add(convertToI01SegmentationDescriptorType(type));
		}
		return des;
	}

	private SegmentationDescriptorType convertToI01SegmentationDescriptorType(tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType type) {
		if(type == null){return null;}
		SegmentationDescriptorType t = new SegmentationDescriptorType();
		t.setDuration(type.getDuration());
		t.setSegmentationEventCancelIndicator(type.isSegmentationEventCancelIndicator());
		t.setSegmentEventId(type.getSegmentEventId());
		t.setSegmentNum(type.getSegmentNum());
		t.setSegmentsExpected(type.getSegmentsExpected());
		t.setSegmentTypeId(type.getSegmentTypeId());
		t.setUpid(type.getUpid());
		t.setUpidType(type.getUpidType());
		t.getOtherAttributes().putAll(type.getOtherAttributes());
		return t;
	}

	private SpliceInsertType convertToI01SpliceInsert(tv.blackarrow.cpp.i03.signaling.SpliceInsertType spliceInsert) {
		if(spliceInsert == null){return null;}
		SpliceInsertType t = new SpliceInsertType();
		t.setAvailNum(spliceInsert.getAvailNum());
		t.setAvailsExpected(spliceInsert.getAvailsExpected());
		t.setDuration(spliceInsert.getDuration());
		t.setOutOfNetworkIndicator(spliceInsert.isOutOfNetworkIndicator());
		t.setSpliceEventCancelIndicator(spliceInsert.isSpliceEventCancelIndicator());
		t.setSpliceEventId(spliceInsert.getSpliceEventId());
		t.setUniqueProgramId(spliceInsert.getUniqueProgramId());
		t.getOtherAttributes().putAll(spliceInsert.getOtherAttributes());
		return t;
	}

	private BinarySignalType convertToI01BinaryData(tv.blackarrow.cpp.i03.signaling.BinarySignalType binaryData) {
		if(binaryData == null){return null;}
		BinarySignalType binary = new BinarySignalType();
		binary.setSignalType(binaryData.getSignalType());
		binary.setValue(binaryData.getValue());
		return binary;
	}

	@Override
	public ManifestConfirmConditionEventType parseMCCRequest(String requestXml) throws Exception {
		if(!requestXml.contains(CppConstants.IO3_NAMESPACE_SIGNALING)){
			throw new RuntimeException("ESAM I03 error. Please ensure messages sent to this endpoint comply with ESAM I03 schema.");
		}
		final StringReader reader = new StringReader(requestXml);
		final Unmarshaller unmarshaller	= linearPOISMccJAXBContext.createUnmarshaller();
		JAXBElement<tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionEventType.class);			
		return convertToI01Menifest(jxbElement.getValue());
	}

	private ManifestConfirmConditionEventType convertToI01Menifest(tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionEventType manifest) {
		if(manifest == null){return null;}
		ManifestConfirmConditionEventType t = new ManifestConfirmConditionEventType();
		for(tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionEventType.AcquiredSignal s: manifest.getAcquiredSignal()){
			t.getAcquiredSignal().add(convertToI01ManifestSignal(s));
		}
		t.getOtherAttributes().putAll(manifest.getOtherAttributes());
		return t;
	}

	private tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal convertToI01ManifestSignal(
			tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionEventType.AcquiredSignal signal) {
		if(signal == null){return null;}
		tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal s = new tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal();
		s.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		s.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		s.setAcquisitionTime(signal.getAcquisitionTime());
		s.setBinaryData(convertToI01ManifestBinaryData(signal.getBinaryData()));
		s.setSCTE35PointDescriptor(convertToI01SCTE35PointDescriptor(signal.getSCTE35PointDescriptor()));
		s.setSignalPointID(signal.getSignalPointID());
		s.setStreamTimes(convertToI01StreamTimes(signal.getStreamTimes()));
		s.setUTCPoint(convertToI01UTCPoint(signal.getUTCPoint()));
		s.getOtherAttributes().putAll(signal.getOtherAttributes());
		return s;
	}

	private UTCPointDescriptorType convertToI01UTCPoint(tv.blackarrow.cpp.i03.manifest.UTCPointDescriptorType utcPoint) {
		if(utcPoint == null){return null;}
		UTCPointDescriptorType t = new UTCPointDescriptorType();
		t.setUtcPoint(utcPoint.getUtcPoint());
		t.getOtherAttributes().putAll(utcPoint.getOtherAttributes());
		return t;
	}

	private StreamTimesType convertToI01StreamTimes(tv.blackarrow.cpp.i03.manifest.StreamTimesType streamTimes) {
		if(streamTimes == null){return null;}
		StreamTimesType s = new StreamTimesType();
		s.getOtherAttributes().putAll(streamTimes.getOtherAttributes());
		for( tv.blackarrow.cpp.i03.manifest.StreamTimeType st: streamTimes.getStreamTime()){
			s.getStreamTime().add(convertToI01StreamTime(st));
		}
		return s;
	}

	private StreamTimeType convertToI01StreamTime(tv.blackarrow.cpp.i03.manifest.StreamTimeType st) {
		if(st == null){return null;}
		StreamTimeType t = new StreamTimeType();
		t.setTimeType(st.getTimeType());
		t.setTimeValue(st.getTimeValue());
		t.getOtherAttributes().putAll(st.getOtherAttributes());
		return t;
	}

	private SCTE35PointDescriptorType convertToI01SCTE35PointDescriptor(tv.blackarrow.cpp.i03.manifest.SCTE35PointDescriptorType scte35) {
		if(scte35 == null){return null;}
		SCTE35PointDescriptorType t = new SCTE35PointDescriptorType();
		t.setSpliceCommandType(scte35.getSpliceCommandType());
		t.setSpliceInsert(converToI01SpliceInsert(scte35.getSpliceInsert()));
		
		for(tv.blackarrow.cpp.i03.manifest.SegmentationDescriptorType s: scte35.getSegmentationDescriptorInfo()){
			t.getSegmentationDescriptorInfo().add(convertToI01SegmentationDescriptorType(s));
		}
		return t;
	}

	private SegmentationDescriptorType convertToI01SegmentationDescriptorType(tv.blackarrow.cpp.i03.manifest.SegmentationDescriptorType s) {
		if(s == null){return null;}
		SegmentationDescriptorType t = new SegmentationDescriptorType();
		t.setDuration(s.getDuration());
		t.setSegmentationEventCancelIndicator(s.isSegmentationEventCancelIndicator());
		t.setSegmentEventId(s.getSegmentEventId());
		t.setSegmentNum(s.getSegmentNum());
		t.setSegmentsExpected(s.getSegmentsExpected());
		t.setSegmentTypeId(s.getSegmentTypeId());
		t.setUpid(s.getUpid());
		t.setUpidType(s.getUpidType());
		t.getOtherAttributes().putAll(s.getOtherAttributes());
		return t;
	}

	private SpliceInsertType converToI01SpliceInsert(tv.blackarrow.cpp.i03.manifest.SpliceInsertType spliceInsert) {
		if(spliceInsert == null){return null;}
		SpliceInsertType t = new SpliceInsertType();
		t.setAvailNum(spliceInsert.getAvailNum());
		t.setAvailsExpected(spliceInsert.getAvailsExpected());
		t.setDuration(spliceInsert.getDuration());
		t.setOutOfNetworkIndicator(spliceInsert.isOutOfNetworkIndicator());
		t.setSpliceEventCancelIndicator(spliceInsert.isSpliceEventCancelIndicator());
		t.setSpliceEventId(spliceInsert.getSpliceEventId());
		t.setUniqueProgramId(spliceInsert.getUniqueProgramId());
		t.getOtherAttributes().putAll(spliceInsert.getOtherAttributes());
		return t;
	}

	private BinarySignalType convertToI01ManifestBinaryData(
			tv.blackarrow.cpp.i03.manifest.BinarySignalType binary) {
		if(binary == null){return null;}
		BinarySignalType t = new BinarySignalType();
		t.setSignalType(binary.getSignalType());
		t.setValue(binary.getValue());
		return t;
	}

}
