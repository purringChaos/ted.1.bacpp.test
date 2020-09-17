package tv.blackarrow.cpp.components;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signaling.ObjectFactory;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public class BinaryGenerationComponent implements Callable {
	
	private static final Logger LOGGER = LogManager.getLogger(BinaryGenerationComponent.class);

	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		// we need to come up with the notification response for all decisions

		final String message =  context.getMessageAsString();
		String response = "";
		try {
			int index = message.indexOf("SignalProcessingEvent");
			boolean isRequest = index >= 0;
			index = message.indexOf("SignalProcessingNotification");
			final StringReader reader = new StringReader(message);
			final JAXBContext jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
			final Unmarshaller unmarshaller	= jaxbCxt.createUnmarshaller();
			if (isRequest) {
				JAXBElement<SignalProcessingEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingEventType.class);			
				SignalProcessingEventType event = jxbElement.getValue();
				
				LOGGER.debug(()->"How many AcquiredSignals " + event.getAcquiredSignal().size());
				
				for(SignalProcessingEventType.AcquiredSignal signal : event.getAcquiredSignal()) {
					if (signal.getBinaryData() != null) {
						return "Invalid Input: Binary data is unexpected in the input request. Please submit with SCTE35PointDescriptor Element in the request.";
					}
					else if (signal.getSCTE35PointDescriptor() != null){
						// source is in clear text format, will convert to binary format
						String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(signal.getSCTE35PointDescriptor(), getPTSTime(signal.getStreamTimes()), getPTSAdjustment(signal.getStreamTimes()));
						BinarySignalType bst = new BinarySignalType();
						bst.setValue(Base64.decodeBase64(encodedStr.getBytes())); 
						signal.setBinaryData(bst);
						bst.setSignalType("SCTE35");
						signal.setSCTE35PointDescriptor(null);
					}
				}
				response = objectToXML(event);
			} else {
				return "Invalid Input: Splice insert/Time Signal in xml format is expected in the request. Please submit with SCTE35PointDescriptor Element in the request.";
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			LOGGER.error(()->ex.getMessage(), ex);
			response = message;
		}
		 
		return response;
	}
	
	private String objectToXML(final SignalProcessingEventType signalEvent) {
		JAXBContext jaxbCxt;
		JAXBElement<SignalProcessingEventType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createSignalProcessingEvent(signalEvent);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		
		return writer.toString();
	}
	
	private String getPTSTime(StreamTimesType streamType) {
		if (streamType != null && streamType.getStreamTime() != null && !streamType.getStreamTime().isEmpty()) {
			for (StreamTimeType type : streamType.getStreamTime()) {
				if (type.getTimeType().equalsIgnoreCase("pts")) return Long.toBinaryString(Long.parseLong(type.getTimeValue()));
			}
		}
		return "";
	}
	
	private String getPTSAdjustment(StreamTimesType streamType) {
		if (streamType != null && streamType.getStreamTime() != null && !streamType.getStreamTime().isEmpty()) {
			for (StreamTimeType type : streamType.getStreamTime()) {
				if (type.getTimeType().equalsIgnoreCase("pts_adjustment")) 
				return Scte35BinaryUtil.toBitString(Long.parseLong(type.getTimeValue()), 33);
			}
		}
		return "";
	}
}
