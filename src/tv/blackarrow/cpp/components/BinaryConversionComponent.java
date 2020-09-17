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
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ObjectFactory;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public class BinaryConversionComponent implements Callable {
	
	private static final Logger LOGGER = LogManager.getLogger(BinaryConversionComponent.class);

	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		// we need to come up with the notification response for all decisions

		final String message =  context.getMessageAsString();
		String response = "";
		try {
			int index = message.indexOf("SignalProcessingEvent");
			boolean isRequest = index >= 0;
			index = message.indexOf("SignalProcessingNotification");
			boolean isResponse = index >= 0;
			final StringReader reader = new StringReader(message);
			final JAXBContext jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
			final Unmarshaller unmarshaller	= jaxbCxt.createUnmarshaller();
			if (isRequest) {
				JAXBElement<SignalProcessingEventType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingEventType.class);			
				SignalProcessingEventType event = jxbElement.getValue();
				
				LOGGER.debug(()->"How many AcquiredSignals " + event.getAcquiredSignal().size());
				
				for(SignalProcessingEventType.AcquiredSignal signal : event.getAcquiredSignal()) {
					if (signal.getBinaryData() != null) {
						// binary request source, will convert to clear text format
						final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
						final byte[] encoded = Base64.encodeBase64(signal.getBinaryData().getValue());
						final StringBuilder pts = new StringBuilder(); 
						final StringBuilder pts_adjustment = new StringBuilder(); 
						Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustment);
						signal.setSCTE35PointDescriptor(scte35Pt);
						signal.setBinaryData(null);
					}
					else if (signal.getSCTE35PointDescriptor() != null){
						// source is in clear text format, will convert to binary format
						String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(signal.getSCTE35PointDescriptor(), "", Scte35BinaryUtil.toBitString(0, 33));
						BinarySignalType bst = new BinarySignalType();
						bst.setValue(Base64.decodeBase64(encodedStr.getBytes())); 
						signal.setBinaryData(bst);
						bst.setSignalType("SCTE35");
						signal.setSCTE35PointDescriptor(null);
					}
				}
				response = objectToXML(event);
			}
			else if (isResponse) {
				JAXBElement<SignalProcessingNotificationType> jxbElement =  unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingNotificationType.class);
				SignalProcessingNotificationType event = jxbElement.getValue();
				
				LOGGER.debug(()->"How many AcquiredSignals " + event.getResponseSignal().size());
				
				for(ResponseSignalType signal : event.getResponseSignal()) {
					if (signal.getBinaryData() != null) {
						// binary response source, will convert to clear text format
						final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
						final byte[] encoded = Base64.encodeBase64(signal.getBinaryData().getValue());
						final StringBuilder pts = new StringBuilder(); 
						final StringBuilder pts_adjustment = new StringBuilder(); 
						Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustment);
						signal.setSCTE35PointDescriptor(scte35Pt);
						signal.setBinaryData(null);
					}
					else if (signal.getSCTE35PointDescriptor() != null){
						// source is in clear text format, will convert to binary format
						String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(signal.getSCTE35PointDescriptor(), "", "");
						BinarySignalType bst = new BinarySignalType();
						bst.setValue(Base64.decodeBase64(encodedStr.getBytes())); 
						bst.setSignalType("SCTE35");
						signal.setBinaryData(bst);
						signal.setSCTE35PointDescriptor(null);
					}
				}
				response = objectToXML(event);
			}
			else {
				LOGGER.debug(()->"Decode binary message");
				LOGGER.debug(()->"Original binary string in base64 encoding:\n" + message);
				StringBuilder sb = new StringBuilder();
				decodeScte35BinaryData(message.getBytes(),sb);
				response = "Original Binary Data:\n\n" + message + sb.toString();
			}
			
		} catch(Exception ex) {
			LOGGER.error(()->ex.getMessage(), ex);
			response = message;
		}
		 
		return response;
	}
	
	public static void decodeScte35BinaryData(final byte[] binaryData,StringBuilder sb) {
		final int base = 2;
		
		StringBuilder vals = new StringBuilder();
		byte[] decoded = Base64.decodeBase64(binaryData);
		for(byte bt : decoded) {
			vals.append(Scte35BinaryUtil.toBinary(bt));
		}

		LOGGER.debug(()->"\nDecoded binary string:\n" + vals.toString());
		
		sb.append("\n\nDecoded binary string:\n\n" + vals.toString());

		sb.append("\n\nFirst not processed 92 bits: " + vals.toString().substring(0,92));
		
		sb.append("\n\nPTS Adjustment: " + vals.toString().substring(39, 39+33));
		
		String str = vals.toString().substring(92);  // remove not processed part

		// int commandLength = Integer.parseInt(str.substring(0, 12), base);  // in byte
		
		sb.append("\nsplice_command_length (12bits): " + str.substring(0, 12) + " == " + Integer.parseInt(str.substring(0, 12), base));
		
		String value = str.substring(12, 20);
		int commandType = Integer.parseInt(value, base); 
		sb.append("\nsplice_command_type (8bits): " + value + " == " + commandType);
		
		if(commandType != 6 && commandType != 5) { throw new RuntimeException("not supported"); } 
		
		str = str.substring(20);
		if(commandType == 5) {
			//SpliceInsertType spliceInsert = new SpliceInsertType();
			//scte35Pt.setSpliceInsert(spliceInsert);
			
			long eventId = Long.parseLong(str.substring(0, 32), 2);  // splice event ID
			sb.append("\nsplice_event_id (32bits): " + str.substring(0, 32) + " == " + eventId);
			
			short eventCancelIndicator = Short.parseShort(str.substring(32, 33), 2);  // splice cancel indicator
			sb.append("\nsplice_event_cancel_indicator (1bit): " + str.substring(32, 33) + " == " + eventCancelIndicator);

			sb.append("\nreserved (7bits): " + str.substring(33, 40));
			str = str.substring(33 + 7);  // plus 7 reserved
			if(eventCancelIndicator == 0) {
				short outNetworkIndicator = Short.parseShort(str.substring(0, 1), 2);  // out_of_network_indicator
				sb.append("\nOut_of_network_indicator (1bit): " + str.substring(0, 1) + " == " + outNetworkIndicator);
				short spliceFlag = Short.parseShort(str.substring(1, 2), 2);  // program_splice_flag
				sb.append("\nprogram_splice_flag (1bit): " + str.substring(1, 2) + " == " + spliceFlag);
				short durationFlag = Short.parseShort(str.substring(2, 3), 2);  
				sb.append("\nduration_flag (1bit): " + str.substring(2, 3) + " == " + durationFlag);
				short immediateFlag = Short.parseShort(str.substring(3, 4), 2);  
				sb.append("\nsplice_immediate_flag (1bit): " + str.substring(3, 4) + " == " + immediateFlag);
				sb.append("\nreserved (4bits): " + str.substring(4, 8));
		
				//spliceInsert.setOutOfNetworkIndicator(outNetworkIndicator == 1);
				
				str = str.substring(8);
				if(spliceFlag == 1 && immediateFlag == 0) {
					str = parseSpliceTime(str,sb);
					//LOG.debug("pts_time: " + pts);
				} else if(spliceFlag == 0) {  // TODO to be enhanced
					// component
				}
				if(durationFlag == 1) {
					long duration = Long.parseLong(str.substring(7, 40), 2);  // milliseconds
					sb.append("\nSpliceInsert Duration (33bit): " + str.substring(7, 40) + " == " + duration + " (in 90kHz) == " + (duration/90) + "(in milliseconds)");
					//try {
						//spliceInsert.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
					//} catch (DatatypeConfigurationException e) {
						//LOG.error(e.getMessage() + "\nDuration value: " + duration);
					//}
					str = str.substring(40);
				}
				int upid = Integer.parseInt(str.substring(0, 16), 2);  // unique program id
				sb.append("\nunique_program_id (16bit): " + str.substring(0, 16) + " == " + upid);
				short availNum = Short.parseShort(str.substring(16, 24), 2);
				sb.append("\navail_num (8bit): " + str.substring(16, 24) + " == " + availNum);
				short availExpected = Short.parseShort(str.substring(24, 32), 2);
				sb.append("\navails_expected (8bit): " + str.substring(24, 32) + " == " + availExpected);
				//spliceInsert.setUniqueProgramId(upid);
				//spliceInsert.setAvailNum(availNum);
				//spliceInsert.setAvailsExpected(availExpected);
				
				str = str.substring(32);
			}
		}else {  // command type == 6
			str = parseSpliceTime(str,sb);
			//LOG.debug("pts_time: " + pts);
			//				int loopLength = Integer.parseInt(str.substring(0, 16), 2);
			//System.out.println("loop length: " + str.substring(0, 16));
			//SegmentationDescriptorType segDesc = new SegmentationDescriptorType();
			//scte35Pt.getSegmentationDescriptorInfo().add(segDesc);

			
			sb.append("\nDescriptor_loop_length (16bits): " + str.substring(0, 16));
			str = str.substring(16);  // after loop length
			sb.append("\nsplice_descriptor_tag (8bits): " + str.substring(0, 8));
			sb.append("\ndescriptor_length (8bits): " + str.substring(8, 16));
			sb.append("\nIdentifier (32bits): " + str.substring(16, 48));
			str = str.substring(8 + 8 + 32); // after splice_descriptor_tag + descriptor_length + identifier
			long SegEventId = Long.parseLong(str.substring(0, 32), 2);  // seg event ID
			sb.append("\nsegmentation_event_id (32bit): " + str.substring(0, 32) + " == " + SegEventId);
			short segEventCancelIndicator = Short.parseShort(str.substring(32, 33), 2);  // seg cancel indicator
			sb.append("\nsegmentation_event_cancel_indicator (1bit): " + str.substring(32, 33) + " == " + segEventCancelIndicator);
			//segDesc.setSegmentEventId(SegEventId);
			//segDesc.setSegmentationEventCancelIndicator(segEventCancelIndicator == 1);

			sb.append("\nReserved (7bits): " + str.substring(33, 40));
			str = str.substring(33 + 7);  // also include 7 bit reserved

			if(segEventCancelIndicator == 0) {
				parseSegmentationStep2(str,sb);
			}  
		}
	}

	private static void parseSegmentationStep2(String str,StringBuilder sb) {
		short programSegFlag = Short.parseShort(str.substring(0, 1), 2);
		sb.append("\nprogram_segmentation_flag (1bit): " + str.substring(0, 1) + " == " + programSegFlag);
		short segDurationFlag = Short.parseShort(str.substring(1, 2), 2);
		sb.append("\nsegmentation_duration_flag (1bit): " + str.substring(1, 2) + " == " + segDurationFlag);

		sb.append("\ndelivery_not_restricted_flag (1bit): " + str.substring(2, 3));
		sb.append("\nweb_delivery_allowed_flag (1bit): " + str.substring(3, 4));
		sb.append("\nno_regional_blackout_flag (1bit): " + str.substring(4, 5));
		sb.append("\narchive_allowed_flag (1bit): " + str.substring(5, 6));
		sb.append("\ndevice_restrictions (2bits): " + str.substring(6, 8));
		str = str.substring(2 + 6);  // 2 bits + 6 reserved Sect 8.3.3
		
		if(programSegFlag == 0) {  // TODO
			// process component
		}
		if(segDurationFlag == 1) {
			long duration = Long.parseLong(str.substring(0, 40), 2);  // 90 kHz ticks to milliseconds
			sb.append("\nSegmentation Duration (40bit): " + str.substring(0, 40) + " == " + duration + " (in 90kHz) == " + (duration/90) + "(in milliseconds)");
//						System.out.println("duration; " + duration);
			//try {
				//segDesc.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			//} catch (DatatypeConfigurationException e) {
				//LOG.error(e.getMessage() + "\nDuration value: " + duration);
			//}
			str = str.substring(40);
		}
		
		short upidType = Short.parseShort(str.substring(0, 8), 2);
		sb.append("\nsegmentation_upid_type (8bit): " + str.substring(0, 8) + " == " + upidType);
		short upidLength = Short.parseShort(str.substring(8, 16), 2);
		sb.append("\nsegmentation_upid_length (8bit): " + str.substring(8, 16) + " == " + upidLength);
		
		int index = 16 + upidLength * 8;
		String upidBitStr =  str.substring(16, index);
		// upid() = upidLength x 8
		short segmentTypeId = Short.parseShort(str.substring(index, index + 8), 2);
		sb.append("\nsegmentation_type_id (8bit): " + str.substring(index, index + 8) + " == " + segmentTypeId);
		short segmentNum = Short.parseShort(str.substring(index + 8, index + 16), 2);
		sb.append("\nsegment_num (8bit): " + str.substring(index + 8, index + 16) + " == " + segmentNum);
		short segmentExpected = Short.parseShort(str.substring(index + 16, index + 24), 2);
		sb.append("\nsegments_expected (8bit): " + str.substring(index + 16, index + 24) + " == " + segmentExpected);

		//segDesc.setSegmentNum(segmentNum);
		//segDesc.setSegmentsExpected(segmentExpected);
		//segDesc.setSegmentTypeId(segmentTypeId);
		
		// upid as byte array
		byte [] upid = new byte[upidLength];
		for (int i=0; i<upidLength; i++) {
			upid[i] = (byte)Integer.parseInt(upidBitStr.substring(8*i, 8*(i+1)), 2);
		}
		
		sb.append("\nsegmentation_upid (" + upidLength + "*8bits): " + upidBitStr + " == " + new String(upid));
		//segDesc.setUpid(upid);
		//segDesc.setUpidType(upidType);
	}

	public static String parseSpliceTime(String str,StringBuilder sb) {
		String value;
		// splice_time
		// start splice_time()
		value = str.substring(0, 1);  // time_specified_flag
		sb.append("\ntime_specified_flag (1bit): " + str.substring(0, 1) + " == " + value);
		if("1".equals(value)) {  // read another 6 + 33 = 39 bits
			String ptsBitString = str.substring(7, 40);
			long ptsTime = Long.parseLong(ptsBitString, 2);
			sb.append("\nreserved (6bits): " + str.substring(1, 7));
			sb.append("\npts_time bit string (33bits): " + ptsBitString + " == " + ptsTime + " (in 90kHz) == " + (ptsTime/90)  + " (in milliseconds)");
			str = str.substring(40);
		} else { // read another 7 bits
			sb.append("\nreserved (6bits): " + str.substring(1, 7));
			str = str.substring(8);
		}
		// end splice_time()
		return str;
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
	
	private String objectToXML(final SignalProcessingNotificationType signalEvent) {
		JAXBContext jaxbCxt;
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createSignalProcessingNotification(signalEvent);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		
		return writer.toString();
	}
	
	public static void main(String [] args) {
		String ptsTime = "111111111111111111111111111111001";
		
		long ptstime = 0;
		try {
			ptstime = Long.parseLong(ptsTime, 2);
		}
		catch (NumberFormatException e) {
			ptstime = 0;
		}
		ptstime += 15;
		
		String pts = Scte35BinaryUtil.toBitString(ptstime, 33);
		
		// this is to test the overflow case
		// in this particular case, the result will be 8 with the bit string as the following:
		// 000000000000000000000000000001000
		System.out.println("PTS_TIME: " + pts);
	}
}
