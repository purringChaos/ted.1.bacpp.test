package tv.blackarrow.cpp.handler;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signaling.ObjectFactory;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;


public class I01ResponseHandler implements ResponseHandler {
	
	private static Logger LOGGER = LogManager.getLogger(I01ResponseHandler.class);

	@Override
	public String generateSCCResponse(SignalProcessingNotificationType notification, Map<String, I03ResponseModelDelta> alternates) {
		JAXBContext jaxbCxt;
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createSignalProcessingNotification(notification);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		
		return writer.toString();
	}

	@Override
	public String generateMCCResponse(ManifestConfirmConditionNotificationType response) {
		JAXBContext jaxbCxt;
		JAXBElement<ManifestConfirmConditionNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			jaxbCxt = JAXBUtil.getLinearPOISMccJAXBContext();
			Marshaller marshaller = jaxbCxt.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
			tv.blackarrow.cpp.manifest.ObjectFactory factory = new tv.blackarrow.cpp.manifest.ObjectFactory();
			jxbElement = factory.createManifestConfirmConditionNotification(response);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		return writer.toString();
	}
	
	
	@Override
	public String generateHSSSparseTrack(final AcquisitionPointInfoType acqSignal) {
		JAXBContext jaxbCxt;
		JAXBElement<AcquisitionPointInfoType> jxbElement = null;
		StringWriter writer = new StringWriter();

		try {
			jaxbCxt = JAXBUtil.getLinearPOISMccJAXBContext();
			Marshaller marshaller = jaxbCxt.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
			tv.blackarrow.cpp.signal.signaling.ObjectFactory factory = new tv.blackarrow.cpp.signal.signaling.ObjectFactory();
			jxbElement = factory.createAcquiredSignal(acqSignal);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		return writer.toString();
	}

	@Override
	public String generateSCCResponse(Object signalProcessingNotificationType) {
		return generateSCCResponse((SignalProcessingNotificationType)signalProcessingNotificationType, null); 
	}

	@Override
	public String generateSCCResponse(tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType notification,
			Map<String, I03ResponseModelDelta> alternates) {
		// TODO Auto-generated method stub
		return null;
	}

}
