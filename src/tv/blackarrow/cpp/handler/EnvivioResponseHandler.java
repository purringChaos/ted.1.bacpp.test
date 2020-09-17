package tv.blackarrow.cpp.handler;

import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.i03.manifest.envivio.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.i03.manifest.envivio.ManifestResponseType;
import tv.blackarrow.cpp.i03.manifest.envivio.ObjectFactory;
import tv.blackarrow.cpp.i03.manifest.envivio.SegmentModifyType;
import tv.blackarrow.cpp.i03.manifest.envivio.SegmentReplaceType;
import tv.blackarrow.cpp.i03.manifest.envivio.SegmentType;
import tv.blackarrow.cpp.i03.manifest.envivio.SparseTrackType;
import tv.blackarrow.cpp.i03.manifest.envivio.StatusCodeType;
import tv.blackarrow.cpp.i03.manifest.envivio.TagSequence;
import tv.blackarrow.cpp.i03.manifest.envivio.TagType;
import tv.blackarrow.cpp.i03.manifest.envivio.TemplateResponseType;
import tv.blackarrow.cpp.i03.manifest.envivio.UTCPointDescriptorType;
import tv.blackarrow.cpp.mcctemplate.DASHConstants;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.SCCResponseUtil;

public class EnvivioResponseHandler extends I03ResponseHandler {
	
	private static Logger LOGGER = LogManager.getLogger(EnvivioResponseHandler.class);
	
	public static final String LINEAR_POIS_MCC_ENVIVIO_PACKAGE_NAME = "tv.blackarrow.cpp.i03.manifest.envivio";
	public static JAXBContext linearPOISMccEnvivioJAXBContext = null;
	static{
		try {
			linearPOISMccEnvivioJAXBContext = JAXBContext.newInstance(LINEAR_POIS_MCC_ENVIVIO_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOGGER.error(()->"Exception occured in getting JAXB context for context path: "	+ LINEAR_POIS_MCC_ENVIVIO_PACKAGE_NAME, e);
		}
	}
	
	
	@Override
	public String generateMCCResponse(tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType notification) {
		JAXBElement<ManifestConfirmConditionNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			Marshaller marshaller	= linearPOISMccEnvivioJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createManifestConfirmConditionNotification(convertToI03MenifestNotificationType(notification));
			marshaller.marshal(jxbElement, writer);					
		} catch (Exception e) {
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


	private StatusCodeType convertToI03ManifestStatusCode(tv.blackarrow.cpp.signal.signaling.StatusCodeType statusCode) {
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

	private ManifestResponseType convertToI03ManifestResponse(tv.blackarrow.cpp.manifest.ManifestResponseType manifestResponse) {
		if(manifestResponse == null){return null;}
		ObjectFactory factory= new ObjectFactory();
		ManifestResponseType t = new ManifestResponseType();
		t.setAcquisitionPointIdentity(manifestResponse.getAcquisitionPointIdentity());
		t.setAcquisitionSignalID(manifestResponse.getAcquisitionSignalID());
		t.setDataPassThrough(manifestResponse.isDataPassThrough());
		t.setDuration(manifestResponse.getDuration());
		t.setSegmentModify(convertToI03SegmentModify(manifestResponse.getSegmentModify()));
		t.setSegmentReplace(convertToI03SegmentReplace(manifestResponse.getSegmentReplace()));
		t.setSignalPointID(manifestResponse.getSignalPointID());
		
		
		Short segmentTypeId =  -1;
		//MPEG_DASH==>START==Removing Dash Template Response value from Other attribute and setting them in Envivio Response.
		if (manifestResponse.getOtherAttributes() != null) {
			String templateType = manifestResponse.getOtherAttributes().get(DASHConstants.DASH_TEMPLATE_RESPONSE_TYPE);
			String templateValue = manifestResponse.getOtherAttributes().get(DASHConstants.DASH_TEMPLATE_RESPONSE_VALUE);
			String valueOfSegmentTypeId = manifestResponse.getOtherAttributes().get(MCCTemplateConstants.MCC_TEMPLATE_SEGMENTATION_TYPE_ID);
			segmentTypeId = (valueOfSegmentTypeId==null||valueOfSegmentTypeId.trim().length()==0)? segmentTypeId : Short.valueOf(valueOfSegmentTypeId);
			
			if (templateType != null) {
				TemplateResponseType templateResponseTypeObject = factory.createTemplateResponseType();
				templateResponseTypeObject.setTemplateType(templateType);
				if (templateValue != null) {
					//No need to base64Encode here, since TemplateResponseType-->value in xsd schema is base64Binary, thus JAXB will automatically perform base64Encoding
					templateResponseTypeObject.setValue(templateValue.getBytes());
				}
				t.setTemplateResponse(templateResponseTypeObject);
			}
			//Removing the value that was set explicitly for custom retrieving here.
			manifestResponse.getOtherAttributes().remove(DASHConstants.DASH_TEMPLATE_RESPONSE_TYPE);
			manifestResponse.getOtherAttributes().remove(DASHConstants.DASH_TEMPLATE_RESPONSE_VALUE);
			manifestResponse.getOtherAttributes().remove(MCCTemplateConstants.MCC_TEMPLATE_SEGMENTATION_TYPE_ID);
			addUTCSignalPointAndActionOnlyforBO(t, segmentTypeId,
					manifestResponse.getOtherAttributes().get(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME));
			manifestResponse.getOtherAttributes().remove(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME);
		}		
		
		//MPEG_DASH End

		for(tv.blackarrow.cpp.manifest.SparseTrackType s: manifestResponse.getSparseTrack()){
			t.getSparseTrack().add(convertToI03SparseTrack(s));
		}
		t.getOtherAttributes().putAll(manifestResponse.getOtherAttributes());
		return t;
	}

	private void addUTCSignalPointAndActionOnlyforBO(ManifestResponseType t,Short segmentTypeId, String utcTime) {		
		
		if (tv.blackarrow.cpp.utils.SegmentType.isValidBlackoutSignal(segmentTypeId) || segmentTypeId==-1) {
			t.setAction("replace");
			if (utcTime != null) {
				UTCPointDescriptorType utc = new UTCPointDescriptorType();
				utc.setUtcPoint(SCCResponseUtil.generateUTC(Long.valueOf(utcTime)));
				t.setUTCPoint(utc);
			}
			
		}
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

	private SparseTrackType convertToI03SparseTrack(tv.blackarrow.cpp.manifest.SparseTrackType s) {
		if(s == null){return null;}
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName(s.getTrackName());
		sparseTrack.setValue(s.getValue());
		return sparseTrack;
	}

}
