package tv.blackarrow.cpp.handler;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.i03.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.i03.manifest.ManifestResponseType;
import tv.blackarrow.cpp.i03.manifest.SegmentModifyType;
import tv.blackarrow.cpp.i03.manifest.SegmentReplaceType;
import tv.blackarrow.cpp.i03.manifest.SegmentType;
import tv.blackarrow.cpp.i03.manifest.SparseTrackType;
import tv.blackarrow.cpp.i03.manifest.TagSequence;
import tv.blackarrow.cpp.i03.manifest.TagType;
import tv.blackarrow.cpp.i03.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.i03.signaling.AlternateContentType;
import tv.blackarrow.cpp.i03.signaling.BinarySignalType;
import tv.blackarrow.cpp.i03.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.i03.signaling.EventScheduleType;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.SpliceInsertType;
import tv.blackarrow.cpp.i03.signaling.StatusCodeType;
import tv.blackarrow.cpp.i03.signaling.StreamTimeType;
import tv.blackarrow.cpp.i03.signaling.StreamTimesType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.SCCResponseUtil;

public class I03ResponseHandler implements ResponseHandler{
	private static final ObjectFactory factory = new ObjectFactory();
	private Logger LOGGER = LogManager.getLogger(I03ResponseHandler.class);

	@Override
	public String generateSCCResponse(tv.blackarrow.cpp.signaling.SignalProcessingNotificationType notification, Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta) {
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			Marshaller marshaller	= I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jxbElement = factory.createSignalProcessingNotification(convertToI03NotificationType(notification, AltContentIdentityResponseModelDelta));
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		return writer.toString();
	}

	private SignalProcessingNotificationType convertToI03NotificationType(tv.blackarrow.cpp.signaling.SignalProcessingNotificationType notification,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta) {
		SignalProcessingNotificationType nt = new SignalProcessingNotificationType();
		nt.setAcquisitionPointIdentity(notification.getAcquisitionPointIdentity());
		for(tv.blackarrow.cpp.signaling.ResponseSignalType signal: notification.getResponseSignal()){
			nt.getResponseSignal().add(convertToI03ResponseSignal(signal, AltContentIdentityResponseModelDelta));
		}

		for(tv.blackarrow.cpp.signaling.ConditioningInfoType info: notification.getConditioningInfo()){
			nt.getConditioningInfo().add(convertToI03ConditioningType(info));
		}
		nt.setStatusCode(convertToI03StatusCode(notification.getStatusCode()));
		nt.getOtherAttributes().putAll(notification.getOtherAttributes());
		return nt;
	}

	private ConditioningInfoType convertToI03ConditioningType(tv.blackarrow.cpp.signaling.ConditioningInfoType info) {
		if(info == null){return null;}
		ConditioningInfoType t = new ConditioningInfoType();
		t.setAcquisitionSignalIDRef(info.getAcquisitionSignalIDRef());
		t.setDuration(info.getDuration());
		t.setStartOffset(info.getStartOffset());
		return t;
	}

	private ResponseSignalType convertToI03ResponseSignal(tv.blackarrow.cpp.signaling.ResponseSignalType signal,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta) {
		if(signal == null){return null;}
		ResponseSignalType t = new ResponseSignalType();
		t.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		t.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		t.setAcquisitionTime(signal.getAcquisitionTime());
		t.setAction(signal.getAction());
		t.setBinaryData(convertToI03BinaryData(signal.getBinaryData()));
		t.setEventSchedule(convertToI03EventSchdule(signal.getEventSchedule()));
		t.setSCTE35PointDescriptor(convertToI03SCTE35PointDescriptor(signal.getSCTE35PointDescriptor()));
		t.setSignalPointID(signal.getSignalPointID());
		t.setStreamTimes(convertToI03StreamTimes(signal.getStreamTimes()));
		t.setUTCPoint(convertToI03UTCPoint(signal.getUTCPoint()));
		t.getOtherAttributes().putAll(signal.getOtherAttributes());

		if(AltContentIdentityResponseModelDelta!=null && !AltContentIdentityResponseModelDelta.isEmpty()
				&& !signal.getAction().equals(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP)
				&& !signal.getAction().equals(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE)){
			I03ResponseModelDelta delta = AltContentIdentityResponseModelDelta.get(t.getSignalPointID());
			if (delta != null && !delta.getAlternateContentIDList().isEmpty()) {
				for (AlternateContentTypeModel c : delta.getAlternateContentIDList()) {
					t.getAlternateContent().add(convertAlternateContent(c));
				}
			}

		}
		//DIRTY PATCH can't help, refractoring is out of scope.
		if (AltContentIdentityResponseModelDelta != null && !AltContentIdentityResponseModelDelta.isEmpty() && AltContentIdentityResponseModelDelta.get(ContextConstants.NONE_SIGNAL_POINT_ID) != null) {
			I03ResponseModelDelta delta = AltContentIdentityResponseModelDelta.get(ContextConstants.NONE_SIGNAL_POINT_ID);
			if (delta != null && !delta.getAlternateContentIDList().isEmpty()) {
				for (AlternateContentTypeModel c : delta.getAlternateContentIDList()) {
					t.getAlternateContent().add(convertAlternateContent(c));
				}
			}
		}

		return t;
	}

	private UTCPointDescriptorType convertToI03UTCPoint(tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType utcPoint) {
		if(utcPoint == null){return null;}
		UTCPointDescriptorType t = new UTCPointDescriptorType();
		t.setUtcPoint(utcPoint.getUtcPoint());
		t.getOtherAttributes().putAll(utcPoint.getOtherAttributes());
		return t;
	}

	private StreamTimesType convertToI03StreamTimes(tv.blackarrow.cpp.signal.signaling.StreamTimesType streamTimes) {
		if(streamTimes == null){return null;}
		StreamTimesType t = new StreamTimesType();
		for(tv.blackarrow.cpp.signal.signaling.StreamTimeType time: streamTimes.getStreamTime()){
			t.getStreamTime().add(convertToI03StreamType(time));
		}
		t.getOtherAttributes().putAll(streamTimes.getOtherAttributes());
		return t;
	}

	private StreamTimeType convertToI03StreamType(tv.blackarrow.cpp.signal.signaling.StreamTimeType time) {
		if(time == null){return null;}
		StreamTimeType t = new StreamTimeType();
		t.setTimeType(time.getTimeType());
		t.setTimeValue(time.getTimeValue());
		t.getOtherAttributes().putAll(time.getOtherAttributes());
		return t;
	}

	private SCTE35PointDescriptorType convertToI03SCTE35PointDescriptor(tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		SCTE35PointDescriptorType t = new SCTE35PointDescriptorType();
		t.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		for(tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType seg: scte35PointDescriptor.getSegmentationDescriptorInfo()){
			t.getSegmentationDescriptorInfo().add(convertToI03SegmentationDescriptor(seg));
		}
		t.setSpliceInsert(convertToI03SpliceInsert(scte35PointDescriptor.getSpliceInsert()));
		t.getOtherAttributes().putAll(scte35PointDescriptor.getOtherAttributes());
		return t;
	}


	private SpliceInsertType convertToI03SpliceInsert(tv.blackarrow.cpp.signal.signaling.SpliceInsertType spliceInsert) {
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

	private SegmentationDescriptorType convertToI03SegmentationDescriptor(tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType seg) {
		if(seg == null){return null;}
		SegmentationDescriptorType s= new SegmentationDescriptorType();
		s.setDuration(seg.getDuration());
		s.setSegmentationEventCancelIndicator(seg.isSegmentationEventCancelIndicator());
		s.setSegmentEventId(seg.getSegmentEventId());
		s.setSegmentNum(seg.getSegmentNum());
		s.setSegmentsExpected(seg.getSegmentsExpected());
		s.setSegmentTypeId(seg.getSegmentTypeId());
		s.setUpid(seg.getUpid());
		s.setUpidType(seg.getUpidType());
		s.getOtherAttributes().putAll(seg.getOtherAttributes());
		return s;
	}

	private EventScheduleType convertToI03EventSchdule(tv.blackarrow.cpp.signaling.EventScheduleType eventSchedule) {
		if(eventSchedule == null){return null;}
		EventScheduleType  e = new EventScheduleType();
		e.setInterval(eventSchedule.getInterval());
		e.setStartOffset(eventSchedule.getStartOffset());
		e.setStartUTC(convertToI03UTCPoint(eventSchedule.getStartUTC()));
		e.setStopOffset(eventSchedule.getStopOffset());
		e.setStopUTC(convertToI03UTCPoint(eventSchedule.getStopUTC()));
		e.getOtherAttributes().putAll(eventSchedule.getOtherAttributes());
		return e;
	}

	private BinarySignalType convertToI03BinaryData(tv.blackarrow.cpp.signal.signaling.BinarySignalType binaryData) {
		if(binaryData == null){return null;}
		BinarySignalType b = new BinarySignalType();
		b.setSignalType(binaryData.getSignalType());
		b.setValue(binaryData.getValue());
		return b;
	}

	private StatusCodeType convertToI03StatusCode(tv.blackarrow.cpp.signal.signaling.StatusCodeType statusCode) {
		if(statusCode == null){return null;}
		StatusCodeType s = new StatusCodeType();
		try{
			s.setClassCode(BigInteger.valueOf(Long.parseLong(statusCode.getClassCode())));
		}catch (Exception e){}
		try{
			s.setDetailCode(BigInteger.valueOf(Long.parseLong(statusCode.getDetailCode())));
		}catch (Exception e){}

		s.getNote().addAll(statusCode.getNote());
		return s;
	}

	@Override
	public String generateMCCResponse(tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType notification) {
		JAXBElement<ManifestConfirmConditionNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			Marshaller marshaller	= I03RequestHandler.linearPOISMccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			tv.blackarrow.cpp.i03.manifest.ObjectFactory factory = new tv.blackarrow.cpp.i03.manifest.ObjectFactory();
			jxbElement = factory.createManifestConfirmConditionNotification(convertToI03MenifestNotificationType(notification));
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		return writer.toString();
	}


	
	private ManifestConfirmConditionNotificationType convertToI03MenifestNotificationType(tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType notification) {
		if(notification == null){return null;}
		ManifestConfirmConditionNotificationType t = new ManifestConfirmConditionNotificationType();
		t.setManifestResponse(convertToI03ManifestResponse(notification.getManifestResponse()));
		t.setStatusCode(convertToI03ManifestStatusCode(notification.getStatusCode()));
		t.getOtherAttributes().putAll(notification.getOtherAttributes());
		return t;
	}


	private tv.blackarrow.cpp.i03.manifest.StatusCodeType convertToI03ManifestStatusCode(tv.blackarrow.cpp.signal.signaling.StatusCodeType statusCode) {
		if(statusCode == null){return null;}
		tv.blackarrow.cpp.i03.manifest.StatusCodeType s = new tv.blackarrow.cpp.i03.manifest.StatusCodeType();
		try{
			s.setClassCode(BigInteger.valueOf(Long.parseLong(statusCode.getClassCode())));
		}catch (Exception e){}
		try{
			s.setDetailCode(BigInteger.valueOf(Long.parseLong(statusCode.getDetailCode())));
		}catch (Exception e){}

		s.getNote().addAll(statusCode.getNote());
		return s;
	}

	private ManifestResponseType convertToI03ManifestResponse(tv.blackarrow.cpp.manifest.ManifestResponseType manifestResponse) {
		if(manifestResponse == null){return null;}
		ManifestResponseType t = new ManifestResponseType();
		t.setAcquisitionPointIdentity(manifestResponse.getAcquisitionPointIdentity());
		t.setAcquisitionSignalID(manifestResponse.getAcquisitionSignalID());
		t.setDataPassThrough(manifestResponse.isDataPassThrough());
		t.setDuration(manifestResponse.getDuration());
		t.setSegmentModify(convertToI03SegmentModify(manifestResponse.getSegmentModify()));
		t.setSegmentReplace(convertToI03SegmentReplace(manifestResponse.getSegmentReplace()));
		t.setSignalPointID(manifestResponse.getSignalPointID());
		manifestResponse.getOtherAttributes().remove(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME);
		t.getOtherAttributes().putAll(manifestResponse.getOtherAttributes());

		for(tv.blackarrow.cpp.manifest.SparseTrackType s: manifestResponse.getSparseTrack()){
			t.getSparseTrack().add(convertToI03SparseTrack(s));
		}
		return t;
	}
	
	private SegmentReplaceType convertToI03SegmentReplace(tv.blackarrow.cpp.manifest.SegmentReplaceType segmentReplace) {
		if(segmentReplace == null){return null;}
		SegmentReplaceType t = new SegmentReplaceType();
		for(tv.blackarrow.cpp.manifest.SegmentType s: segmentReplace.getSegment()){
			t.getSegment().add(convertToI03Segment(s));
		}
		return t;
	}

	private SegmentType convertToI03Segment(tv.blackarrow.cpp.manifest.SegmentType s) {
		if(s == null){return null;}
		SegmentType t = new SegmentType();
		t.setDuration(s.getDuration());
		t.setExtinf(s.getExtinf());
		t.setUri(s.getUri());
		return t;
	}

	private SegmentModifyType convertToI03SegmentModify(tv.blackarrow.cpp.manifest.SegmentModifyType segmentModify) {
		if(segmentModify == null){return null;}
		SegmentModifyType t = new SegmentModifyType();
		t.setFirstSegment(convertToI03Tag(segmentModify.getFirstSegment()));
		t.setSpanSegment(convertToI03Tag(segmentModify.getSpanSegment()));
		t.setLastSegment(convertToI03Tag(segmentModify.getLastSegment()));
		return t;
	}

	private TagSequence convertToI03Tag(tv.blackarrow.cpp.manifest.TagSequence seg) {
		if(seg == null){return null;}
		TagSequence t = new TagSequence();
		for(tv.blackarrow.cpp.manifest.TagType tg: seg.getTag()){
			t.getTag().add(convertToI03TagType(tg));
		}
		return t;
	}

	private TagType convertToI03TagType(tv.blackarrow.cpp.manifest.TagType tg) {
		if(tg == null){return null;}
		TagType t = new TagType();
		t.setValue(tg.getValue());
		t.setAdapt(tg.isAdapt());
		t.setLocality(tg.getLocality());
		return t;
	}


	private AlternateContentType convertAlternateContent(AlternateContentTypeModel c) {
		if (c == null)
			return null;
		AlternateContentType t = new AlternateContentType();
		if (c.getAltContentIdentity() != null) {
			t.setAltContentIdentity(c.getAltContentIdentity());
		}
		if (c.getZoneIdentity() != null) {
			t.setZoneIdentity(c.getZoneIdentity());
		}
		return t;
	}

	private SparseTrackType convertToI03SparseTrack(tv.blackarrow.cpp.manifest.SparseTrackType s) {
		if(s == null){return null;}
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName(s.getTrackName());
		sparseTrack.setValue(s.getValue());
		return sparseTrack;
	}

	@Override
	public String generateHSSSparseTrack(final tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType acqSignal) {
		JAXBElement<AcquisitionPointInfoType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			Marshaller marshaller= I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jxbElement = factory.createAcquiredSignal(convertToI03AcquisitionPointInfoType(acqSignal));
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(e.getMessage());
		}
		return writer.toString();
	}

	private AcquisitionPointInfoType convertToI03AcquisitionPointInfoType(tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType signal) {
		AcquisitionPointInfoType a = new AcquisitionPointInfoType();
		a.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		a.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		a.setUTCPoint(convertToI03UTCPoint(signal.getUTCPoint()));
		a.setStreamTimes(convertToI03StreamTimes(signal.getStreamTimes()));
		a.getOtherAttributes().putAll(signal.getOtherAttributes());
		return a;
	}

	@Override
	public String generateSCCResponse(Object signalProcessingNotificationType) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller marshaller	= I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement<SignalProcessingNotificationType> jxbElement = factory.createSignalProcessingNotification(
					(SignalProcessingNotificationType)signalProcessingNotificationType);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(e.getMessage());
		}
		return writer.toString();
	}

	@Override
	public String generateSCCResponse(SignalProcessingNotificationType notification,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta) {
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			Marshaller marshaller	= I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			jxbElement = factory.createSignalProcessingNotification(notification);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(e.getMessage());
		}
		return writer.toString();
	}

}

