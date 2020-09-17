package tv.blackarrow.cpp.handler;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JAXBUtil;

public class I01RequestHandler implements RequestHandler {

	@Override
	public SignalProcessingEventType parseSCCRequest(String requestXml) throws Exception{
		
		if(requestXml.contains(CppConstants.IO3_NAMESPACE_SIGNALING)){
			throw new RuntimeException("ESAM I01 error. Please ensure messages sent to this endpoint comply with ESAM I01 schema.");
		}
		
		final StringReader reader = new StringReader(requestXml);
		final JAXBContext jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
		final Unmarshaller unmarshaller	= jaxbCxt.createUnmarshaller();
		JAXBElement<SignalProcessingEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingEventType.class);	
		SignalProcessingEventType t = jxbElement.getValue();
		if(t== null || t.getAcquiredSignal() == null || t.getAcquiredSignal().get(0) == null || 
				(t.getAcquiredSignal().get(0).getBinaryData() == null && t.getAcquiredSignal().get(0).getSCTE35PointDescriptor() == null)){
			throw new RuntimeException("ESAM I01 error. Please ensure messages sent to this endpoint comply with ESAM I01 schema.");
		}
		return jxbElement.getValue();
	}
	
	
	@Override
	public ManifestConfirmConditionEventType parseMCCRequest(String requestXml)	throws Exception {
		if(requestXml.contains(CppConstants.IO3_NAMESPACE_SIGNALING)){
			throw new RuntimeException("ESAM I01 error. Please ensure messages sent to this endpoint comply with ESAM I01 schema.");
		}
		final StringReader reader = new StringReader(requestXml);
		final JAXBContext jaxbCxt = JAXBUtil.getLinearPOISMccJAXBContext();
		Unmarshaller unmarshaller = jaxbCxt.createUnmarshaller();
		JAXBElement<ManifestConfirmConditionEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), ManifestConfirmConditionEventType.class);
		return jxbElement.getValue();
	}

}
