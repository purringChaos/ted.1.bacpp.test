//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow, and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Author: $
// $Date: $
// $Revision: $
//

package tv.blackarrow.cpp.utils;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.signal.signaling.AvailDescriptorType;
import tv.blackarrow.cpp.signal.signaling.DTMFDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;

/**
 * an utility to handle SCTE 35 splice_info_section
 */
public class Scte35BinaryUtil {
	private static final Logger LOG = LogManager.getLogger(Scte35BinaryUtil.class);
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final boolean TRACE_ENABLED = LOG.isTraceEnabled(); 
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled(); 
	// private Scte35BinaryUtil() {}
	
	/**
	 * from a byte to binary bit string (8 bits)
	 * for example: 3 -- > "00000011"      15 --> "00001111"
	 * @param val
	 * @return
	 */
	public static String toBinary(final byte val) {
		final StringBuilder result = new StringBuilder();

		for (int i=0; i<8; i++) {
			result.append(val >> (8-(i+1)) & 0x0001);
		}
		return result.toString();
	}	

	/**
	 * for example: value 8, length = 6, then binary 1000 will append two 0s in the front --> 001000  
	 * @param inputvalue
	 * @param length
	 * @return
	 */
	public static String toBitString(final long inputvalue, final int length) {
		long value = inputvalue;
		final long orignalValue = inputvalue;
		
		final StringBuilder valueBf = new StringBuilder();
		for(;;) {
			if(orignalValue == 0 || orignalValue == 1) {
				valueBf.append(orignalValue);
				break;
			}
			final int mod = (int)(value % 2);
			if(value == 0) {
				break;
			} 
			if(valueBf.length() >= length) { break; }
			valueBf.append(mod);
			value >>= 1;
		}
		
		int len = valueBf.length(); 
		if( len < length) {
			for(int i = 0; i < length - len; i++) {
				valueBf.append("0");
			}
		} else if (len > length){
			LOG.warn(()->"The binary encoded value for " + inputvalue + " is " + inputvalue + ".  It's length is longer than expected " + length);
		}
		
		
		return valueBf.reverse().toString();
	}
	
	/**
	 * the input will be a BigInteger type, and goal is to convert it to a bit String 
	 * @param inputvalue BigInteger type
	 * @param length
	 * @return binary bit string
	 */
	public static String toBitString(final BigInteger inputvalue, final int length) {
		final StringBuilder valueBf = new StringBuilder();
		String val = inputvalue.toString(2);
		if(val.length() <= length) {
			for(int i = 0; i < length - val.length(); i++) {
				valueBf.append("0");
			}
		} else {
			LOG.warn(()->"The binary encoded value for " + inputvalue + " is " + inputvalue + ".  It's length is longer than expected " + length);
		}
		
		return valueBf.append(val).toString();
	}
	

	/**
		value =  Scte35BinaryUtil.getHexFromBitString("10101110");
		assertEquals("AE", value);
		value =  Scte35BinaryUtil.getHexFromBitString("111110101110");
		assertEquals("FAE", value);
	 * @param vals
	 * @return
	 */
	public static String getHexFromBitString(final String vals) {
		if(vals.toString().length() %4 != 0) {
			throw new RuntimeException(vals + " * Wrong format - length should be divided by 4 with mod value 0 - mode: " + vals.toString().length() %4);
		}
		
		final StringBuilder res = new StringBuilder();
		for(int i = 0; i < vals.length()/4; i++ ) {
			int value = Integer.parseInt(vals.substring(i * 4, i * 4 + 4), 2);
			switch(value) {
			case 10: res.append('A'); break;
			case 11: res.append('B'); break;
			case 12: res.append('C'); break;
			case 13: res.append('D'); break;
			case 14: res.append('E'); break;
			case 15: res.append('F'); break;
			default: res.append(value); break;
			}
		}
		
		return res.toString();
	}
	
	/**
	 * the input string should only include the character/number in 0123456789ABCEDFF
	 * TOTO pattern check
	 * @param input String
	 * @return
	 */
	public static byte[] getBytes(final String input) {
		byte[] bts = new byte[input.length()];
		
		for(int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if(ch == 'A') {
				bts[i] = 10; 
			}
			else if(ch == 'B') {
				bts[i] = 11; 
			}
			else if(ch == 'C') {
				bts[i] = 12; 
			}
			else if(ch == 'D') {
				bts[i] = 13; 
			}
			else if(ch == 'E') {
				bts[i] = 14; 
			}
			else if(ch == 'F') {
				bts[i] = 15; 
			}
			else {
				bts[i] = Byte.valueOf(String.valueOf(ch));
			}
		}
		
		return bts;
	}

	/**
	 * please refer to SCTE 35 (2007/2011) table 7.1 splice_info_section()
	 * 
	 * @param event
	 * @return
	 */
	public static String scte35SpliceInfoSection(final SCTE35PointDescriptorType scte35Pt, String pts, String pts_adjustment) {
		StringBuilder result = new StringBuilder();
		StringBuilder command = new StringBuilder();
		
		if(TRACE_ENABLED) { LOG.trace("**************pts value: " + pts); }
		if(TRACE_ENABLED) { LOG.trace("**************pts adjustment value: " + pts_adjustment); }
		if(pts_adjustment.length() != 33){
			LOG.error(()->"**************Invalid pts adjustment value: " + pts_adjustment+ ", the length should be 33.");
		}

		result.append("111111000011" + "000000000000000" + pts_adjustment + "11111111111111111111"); // content before command
		
		// detail command 
		if(scte35Pt != null && scte35Pt.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) {  // time signal
			//SegmentationDescriptorType sdType = scte35Pt.getSegmentationDescriptorInfo().get(0); 
			command.append(scte35TimeSignal(pts));  // time_signal()
		}
		else if(scte35Pt != null && scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {  // splice_insert
			SpliceInsertType insert = scte35Pt.getSpliceInsert();
			command.append(scte35SpliceInsert(insert, pts)); // splice_insert()  
		}
		if(command.length() % 8 != 0) {
			throw new RuntimeException("command wrong size (bits) : " + command.length());
		}

		result.append(toBitString(command.length() / 8, 12))  // command length
		.append(toBitString(scte35Pt.getSpliceCommandType(), 8)) // command type
		.append(command);  // command
		
		StringBuilder tmp = new StringBuilder();
		handleSpliceDescriptor(scte35Pt, tmp);
		
		if(tmp.length() % 8 > 0) {
			throw new RuntimeException("descriptor loop length is wrong with bits : " + tmp.length());
		}

		int descLoopLength = tmp.length() / 8;
		result.append(toBitString(descLoopLength, 16))  // descriptor_loop_length
		.append(tmp)  // splice descriptor	
		//.append("00000000")  // alignment_stuffing   nx8 bits
		;

		int sectionLength = (result.toString().length() - 12)/8 + 4; // plus 4 CRC32 bytes
		if(TRACE_ENABLED) { LOG.trace(()->"section length = " + sectionLength); }
		result.insert(12, Scte35BinaryUtil.toBitString(sectionLength, 12));

		String nstr = result.toString();
		byte[] bts = new byte[nstr.length() / 8];
		for(int i = 0; i < nstr.length() / 8; i++) {
			int value = Integer.parseInt(nstr.substring(i * 8, (i+1) * 8), 2);
			bts[i] = (byte)value;
		}
		
		result.append(Scte35BinaryUtil.toBitString(CRC32Mpeg2.getValue(bts), 32)); // append CRC32
		
		return result.toString();
	}

	private static void handleSpliceDescriptor(
			final SCTE35PointDescriptorType scte35Pt, StringBuilder tmp) {
		for(SegmentationDescriptorType sdType : scte35Pt.getSegmentationDescriptorInfo()) {  // add splice_descriptor
			tmp.append(scte35SpliceDescriptor(sdType));
		}

		for(AvailDescriptorType adType : scte35Pt.getAvailDescriptorInfo()) {  // add splice_descritor, in our case, it is size 0
			tmp.append(scte35SpliceDescriptor(adType));
		}
		
		for(DTMFDescriptorType ddType : scte35Pt.getDTMFDescriptorInfo()) {  // add splice_descrito,r in our case, it is size 0
			tmp.append(scte35SpliceDescriptor(ddType));
		}
		
		if(TRACE_ENABLED) {  LOG.trace(()->"splice descriptor: " + tmp.toString()); }
	}

	/**
	 * refer to table 7-5
	 * @param insert
	 * @return
	 */
	public static String scte35SpliceInsert(final SpliceInsertType insert, String pts) {
		final StringBuilder res = new StringBuilder();

		res//.append("00000001")
		.append(toBitString(insert.getSpliceEventId() == null ? 0 : insert.getSpliceEventId(), 32))  // event ID   - 32 bits
		.append((insert.isSpliceEventCancelIndicator() != null && insert.isSpliceEventCancelIndicator()) ? 1 : 0) // SpliceEventCancelIndicator 1 bit
		.append("1111111"); // reserved  7 bits
		
		if(insert.isSpliceEventCancelIndicator() == null || !insert.isSpliceEventCancelIndicator()) {
			res
			.append((insert.isOutOfNetworkIndicator() != null && insert.isOutOfNetworkIndicator()) ? 1 : 0) // out_of_network_flag
			.append("1"); // program splice flag 1   (shall we support Component Splice Mode?);
			
	       boolean hasDuration = false;
	       if(insert.getDuration() == null || insert.getDuration().getSign() == 0) {
		       res.append("0"); // splice duration flag
	       } else {
		       res.append("1"); // splice duration flag
		       hasDuration = true;
	       }
			
	        // splice immediate flag
	       if (insert.getOtherAttributes() != null && !insert.getOtherAttributes().isEmpty()) {
	    	   String value = null;
				if ((value = insert.getOtherAttributes().get(QName.valueOf("spliceImmediateFlag"))) != null && value.equals("1")) {
					res.append("1");
				} else res.append("0");
	       } else res.append("0");
	       res.append("1111") // reserved  4 bits
		      .append(scte35SpliceTime(pts));
	       
	       if(hasDuration) {
	    	   res.append(scte35BreakDuration(insert));
	       }
		
	       res.append(toBitString(insert.getUniqueProgramId() == null? 0 : insert.getUniqueProgramId(), 16));
	       res.append(toBitString(insert.getAvailNum() == null ? 0 : insert.getAvailNum(), 8));
	       res.append(toBitString(insert.getAvailsExpected() == null ? 0 : insert.getAvailsExpected(), 8));
		}
		
		if(TRACE_ENABLED) { LOG.trace(()->"splice_insert: " + res.toString()); }
		
		return res.toString();
	}
	
	
	/**
	 * check SCTE 35 table 8-2
	 * @param event
	 * @return
	 */
	public static String scte35SpliceDescriptor(final SegmentationDescriptorType sdType) {
		return scte35SegmentationDescriptor(sdType);
	}
	
	public static String scte35SpliceDescriptor(final AvailDescriptorType adType) {
		throw new RuntimeException("AvailDescriptorType not supported yet");
	}

	public static String scte35SpliceDescriptor(final DTMFDescriptorType ddType) {
		throw new RuntimeException(" DTMFDescriptorType not supported yet");
	}
	
	/**
	 * refer to SCTE 35 table 8-5
	 * @param sdType
	 * @return
	 */
	public static String scte35SegmentationDescriptor(final SegmentationDescriptorType sdType) {
		StringBuilder res = new StringBuilder();

		res.append("00000010");  // 0x02 segmentation descriptor
		
		// special treatment on segmentEventId and segmentTypeId
		// for some reason, those two attributes are not parsed directly under SegmentationDescriptorType
		// but rather under SegmentationDescriptorType::otherAttributes
		Map<QName, String> otherAttributes = sdType.getOtherAttributes();
		Long segmentEventId = sdType.getSegmentEventId();
		Short segmentTypeId = sdType.getSegmentTypeId();
		if (segmentEventId == null && otherAttributes != null) {
			String segEventId = otherAttributes.get(QName.valueOf("segmentEventID"));
			if (segEventId != null) segmentEventId = Long.parseLong(segEventId);
			if (segmentEventId == null) segmentEventId = 0l;
		}
		if (segmentTypeId == null && otherAttributes != null) {
			String segTypeId = otherAttributes.get(QName.valueOf("segmentTypeID"));
			if (segTypeId != null) segmentTypeId = Short.parseShort(segTypeId);
			if (segmentTypeId == null) segmentTypeId = 0;
		}
		
		//res.append(descriptor_len);  // will be insert 
		res.append("01000011010101010100010101001001") // identifier  0x43554549  (ASCII CUEI)
    	   .append(toBitString(segmentEventId, 32));  // segmentation event id
		if(sdType.isSegmentationEventCancelIndicator() == null || !sdType.isSegmentationEventCancelIndicator()) {
	       res.append("0") // segmentation event cancel event flag
	       .append("1111111") // reserved 7 bits
	       .append("1"); // program segmentation flag 1  (should we support Component? If yes, then set it 0)
	       boolean hasDuration = false;
		   String segDurationFlag = sdType.getOtherAttributes().get(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
		   if(segDurationFlag == null){
			  if(sdType.getDuration() == null || sdType.getDuration().getSign() < 0) {
			       res.append("0"); // segmentation duration flag
		       } else {
			       res.append("1"); // segmentation duration flag
			       hasDuration = true;
		       }
		   } else if("0".equals(segDurationFlag)){
			  res.append("0");	// segmentation duration flag
		   } else{
			  if(sdType.getDuration() == null || sdType.getDuration().getSign() < 0) {
			       res.append("0"); // segmentation duration flag
		       } else {
			       res.append("1"); // segmentation duration flag
			       hasDuration = true;
		       }
		   }
	       deliveryNotRestrictedFlagProcess(sdType, res);
	       
	       if(hasDuration) {
	    	  res.append(toBitString(sdType.getDuration().getTimeInMillis(new Date()) * 90, 40)); // segmentation duration should be in 90kHz for binary version
	       } 
	       res.append(toBitString(sdType.getUpidType(), 8)); // segmentation upid type

	       // upidLength and upid
	       int upidLength = 0;
	       byte[] upid = sdType.getUpid();
	       if (upid != null) {
	    	   upidLength = upid.length;
	       }
	       res.append(toBitString(upidLength, 8)); // segmentation upid length
	       if (upidLength > 0) {
	    	   for (int i=0; i<upidLength; i++) {
	    		   res.append(toBinary(upid[i]));
	    	   }
	       }
	       
	       res.append(toBitString(segmentTypeId, 8)) // segmentation type id
	       .append(toBitString(sdType.getSegmentNum(), 8)) // segment number 
	       .append(toBitString(sdType.getSegmentsExpected(), 8)) // segment expected 
	       ;
		} else {
			res.append("1") // segmentation event cancel event flag
		       .append("1111111"); // reserved 7 bits
		}
		
		int descLength = res.length()/8 - 1 ; // descriptor length (bytes)
		res.insert(8, toBitString(descLength, 8));
		
		return res.toString();
		
	}

	/**
	 * table 7-6
	 * @return
	 */
	public static String scte35TimeSignal(String pts) {
		return scte35SpliceTime(pts);
	}

	/**
	 * table 7-9
	 * @return
	 */
	public static String scte35SpliceTime(String pts) {
		StringBuilder res = new StringBuilder();
		
		boolean timeSpecifiedFlag = (pts != null  && pts.length() > 0) ? true: false;  
		
		if(timeSpecifiedFlag) {
			if(pts.length() != 33){
				pts = toBitString(Long.parseLong(pts), 33);
			}
			res.append("1"); // time_specified_flag  
			res.append("111111"); // reserved - 6 bits
			// append the pts time that may be adjusted by the offset already
			res.append(pts);
		} else {
			// fix on bug BAS-23287
			// in the case pts_time is not passed in or not available as in blackout out-band notification case
			// transcoder expects time_specified_flag to be set so that they can overwrite the pts_time accordingly
			// any value should work in this case, just to zero all the bits
			res.append("1"); // time_specified_flag  
			res.append("111111"); // reserved - 6 bits
			res.append(toBitString(0l, 33));
		}		
		
		return res.toString();
	}

	// table 7-10
	// TODO need fix it
	public static String scte35BreakDuration(final SpliceInsertType insert) {
		StringBuilder res = new StringBuilder();
		
		res.append("1") // auto return  
		.append("111111") // reserved - 6 bits
		.append(toBitString(insert.getDuration().getTimeInMillis(new Date()) * 90, 33)); // milliseconds to 90kHz conversion
		
		return res.toString();
	}
	
	public static void decodeScte35BinaryData(final String binaryData, final SCTE35PointDescriptorType scte35Pt, final StringBuilder pts, final StringBuilder pts_adjustment) {
		decodeScte35BinaryData(binaryData.getBytes(), scte35Pt, pts, pts_adjustment);
	}
	
	private static void deliveryNotRestrictedFlagProcess(
			final SegmentationDescriptorType sdType, StringBuilder res) {

		String notRestrictedFlag = sdType.getOtherAttributes().get(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG));
		if("0".equals(notRestrictedFlag)) {
			res.append("0"); // delivery_not_restricted_flag
			String webDeliveryAllowFlag = sdType.getOtherAttributes().get(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG));
			if("0".equals(webDeliveryAllowFlag)) {
				res.append("0"); // web_delivery_allow_flag
			} else {
				res.append("1"); // web_delivery_allow_flag
			}
			String noRegionalBlackoutFlag = sdType.getOtherAttributes().get(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG));
			if("0".equals(noRegionalBlackoutFlag)) {
				res.append("0"); // no_regional_blackout_flag
			} else {
				res.append("1"); // no_regional_blackout_flag
			}
			String archiveAllowedFlag = sdType.getOtherAttributes().get(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG));
			if("0".equals(archiveAllowedFlag)) {
				res.append("0"); // archive_allowed_flag
			} else {
				res.append("1"); // archive_allowed_flag
			}
			
			String deviceRestrictions = sdType.getOtherAttributes().get(new QName(CppConstants.DEVICE_RESTRICTIONS));
			if(deviceRestrictions!=null && deviceRestrictions.length()>0){
				res.append(toBitString(Integer.parseInt((deviceRestrictions)), 2));
			}else{
				res.append("11");
			}	// device restrictions
		} else {
			res.append("1")  // delivery_not_restricted_flag
			.append("11111"); // reserved 5 bits
		}
	}

	public static void decodeScte35BinaryData(final byte[] binaryData, final SCTE35PointDescriptorType scte35Pt, final StringBuilder pts, final StringBuilder pts_adjustment) {
		final int base = 2;
		
		StringBuilder vals = new StringBuilder();
		byte[] decoded = Base64.decodeBase64(binaryData);
		for(byte bt : decoded) {
			vals.append(Scte35BinaryUtil.toBinary(bt));
		}

		if(TRACE_ENABLED) {  LOG.trace(()->"decoded string: " + vals.toString()); }
		
		String str = vals.toString();
		
		// retrieve pts_adjustment
		String pts_adjust = str.substring(39, 39+33);
		pts_adjustment.append(pts_adjust);
		if(TRACE_ENABLED) {  LOG.trace(()->"pts_adjustment value: " + pts_adjust); }
		
		str = str.substring(92);  // remove not processed part

		// int commandLength = Integer.parseInt(str.substring(0, 12), base);  // in byte
		Integer.parseInt(str.substring(0, 12), base);  // in byte
		
		String value = str.substring(12, 20);
		int commandType = Integer.parseInt(value, base); 
		scte35Pt.setSpliceCommandType(commandType);
		
		//if(commandType != 6 && commandType != 5) { return; } 
		
		str = str.substring(20);
		if(commandType == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {//command type == 5
			SpliceInsertType spliceInsert = new SpliceInsertType();
			scte35Pt.setSpliceInsert(spliceInsert);
			
			long eventId = Long.parseLong(str.substring(0, 32), 2);  // splice event ID
			spliceInsert.setSpliceEventId(eventId);

			short eventCancelIndicator = Short.parseShort(str.substring(32, 33), 2);  // splice cancel indicator
			spliceInsert.setSpliceEventCancelIndicator(eventCancelIndicator == 1);
			str = str.substring(33 + 7);  // plus 7 reserved
			if(eventCancelIndicator == 0) {
				short outNetworkIndicator = Short.parseShort(str.substring(0, 1), 2);  // out_of_network_indicator
				short spliceFlag = Short.parseShort(str.substring(1, 2), 2);  // program_splice_flag
				short durationFlag = Short.parseShort(str.substring(2, 3), 2);  
				short immediateFlag = Short.parseShort(str.substring(3, 4), 2);  
				spliceInsert.setOutOfNetworkIndicator(outNetworkIndicator == 1);
				if (immediateFlag == 1) {
					Map<QName, String> attrMap = spliceInsert.getOtherAttributes();
					if (attrMap != null) {
						attrMap.put(new QName("spliceImmediateFlag"), "1");
					}
				}
				str = str.substring(8);
				if(spliceFlag == 1 && immediateFlag == 0) {
					str = parseSpliceTime(str, pts);
					if(TRACE_ENABLED) { LOG.trace(()->"pts_time: " + pts); }
				} else if(spliceFlag == 0) {  // TODO to be enhanced
					// component
				}
				if(durationFlag == 1) {
					long duration = Long.parseLong(str.substring(7, 40), 2);  // 90kHz
					try {
						spliceInsert.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration/90));	// 90kHz -> milliseconds when in clear text version
					} catch (DatatypeConfigurationException e) {
						LOG.error(()->e.getMessage() + "\nDuration value in 90kH: " + duration);
					}
					str = str.substring(40);
				}
				int upid = Integer.parseInt(str.substring(0, 16), 2);  // unique program id
				short availNum = Short.parseShort(str.substring(16, 24), 2);
				short availExpected = Short.parseShort(str.substring(24, 32), 2);
				spliceInsert.setUniqueProgramId(upid);
				spliceInsert.setAvailNum(availNum);
				spliceInsert.setAvailsExpected(availExpected);
				str = str.substring(32);
				//parsing splice descriptor Tag
				parseSpliceDescriptor(str,scte35Pt);
						}
		}else {  // command type == 6
			str = parseSpliceTime(str, pts);
			if(TRACE_ENABLED) { LOG.trace(()->"pts_time: " + pts); }
			//parsing splice descriptor Tag
			parseSpliceDescriptor(str,scte35Pt);
			}
	}

	private static String parseSegmentationStep2(String str,
			SegmentationDescriptorType segDesc) {
		short programSegFlag = Short.parseShort(str.substring(0, 1), 2);
		short segDurationFlag = Short.parseShort(str.substring(1, 2), 2);
		short deliveyNotRestrictedFlag = Short.parseShort(str.substring(2, 3), 2);
		segDesc.getOtherAttributes().put(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG), str.substring(2, 3));
		if(deliveyNotRestrictedFlag == 0) { // restricted
			segDesc.getOtherAttributes().put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), str.substring(1,2));// newly added
			segDesc.getOtherAttributes().put(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG), str.substring(3, 4));
			segDesc.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), str.substring(4, 5));
			segDesc.getOtherAttributes().put(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG), str.substring(5, 6));
			int deviceRestrictions = Integer.parseInt((str.substring(6,8)), 2);
			segDesc.getOtherAttributes().put(new QName(CppConstants.DEVICE_RESTRICTIONS),Integer.toString(deviceRestrictions) );
		}
		str = str.substring(2 + 6);  // 2 bits + 6 reserved Sect 8.3.3
		
		if(programSegFlag == 0) {  // TODO
			// process component
		}
		if(segDurationFlag == 1) {
			long duration = Long.parseLong(str.substring(0, 40), 2);  // 90kHz
//						System.out.println("duration; " + duration);
			try {
				segDesc.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration/90));	// 90kHz -> milliseconds when in clear text version
			} catch (DatatypeConfigurationException e) {
				LOG.error(()->e.getMessage() + "\nDuration value in 90kHz: " + duration);
			}
			str = str.substring(40);
		}
		
		short upidType = Short.parseShort(str.substring(0, 8), 2);
		short upidLength = Short.parseShort(str.substring(8, 16), 2);
		
		int index = 16 + upidLength * 8;
		String upidBitStr =  str.substring(16, index);
		// upid() = upidLength x 8
		short segmentTypeId = Short.parseShort(str.substring(index, index + 8), 2);
		short segmentNum = Short.parseShort(str.substring(index + 8, index + 16), 2);
		short segmentExpected = Short.parseShort(str.substring(index + 16, index + 24), 2);

		segDesc.setSegmentNum(segmentNum);
		segDesc.setSegmentsExpected(segmentExpected);
		segDesc.setSegmentTypeId(segmentTypeId);
		
		// upid as byte array
		byte [] upid = new byte[upidLength];
		for (int i=0; i<upidLength; i++) {
			upid[i] = (byte)Integer.parseInt(upidBitStr.substring(8*i, 8*(i+1)), 2);
		}
		
		segDesc.setUpid(upid);
		segDesc.setUpidType(upidType);
		return str.substring(40+upidBitStr.length());
	}
	
	private static void parseSpliceDescriptor(String str,SCTE35PointDescriptorType scte35Pt)
	{
		long segmentDescriptorLoopCount = Long.parseLong(str.substring(0, 16), 2);  // splice event ID
		long noOfBitsToRead = segmentDescriptorLoopCount * 8; 
		str = str.substring(16);  // after loop length
		if(noOfBitsToRead > str.length()){
			LOG.info("Invalid binary string received. No of bits specified for reading splice descriptors (" + noOfBitsToRead + 
					") is greater than number of bits remaining to read (" + str.length() + ").");
			return;
		}
		String beforeReading = str;
		String afterReading = str;
		// parsing different splice descriptor types tag
		while(beforeReading.length() - afterReading.length() < noOfBitsToRead){
			afterReading = parseSpliceDescriptorTypes(afterReading, scte35Pt);
			if(beforeReading.equals(afterReading) || StringUtils.isBlank(afterReading)){
				break;
			}
		}
		
	}
	/**
	 * Method to parse different SpliceDescriptor types
	 * @param str
	 * @param scte35Pt
	 * @return
	 */
	private static String parseSpliceDescriptorTypes(String str,SCTE35PointDescriptorType scte35Pt) {
		if(Long.parseLong(str.substring(0,8), 2) == SpliceDescriptorType.SEGMENTATION_DESCRIPTOR.getDescriptorType()){
			SegmentationDescriptorType segDesc = new SegmentationDescriptorType();
			scte35Pt.getSegmentationDescriptorInfo().add(segDesc);
			str = str.substring(8 + 8 + 32); // after splice_descriptor_tag + descriptor_length + identifier
			long SegEventId = Long.parseLong(str.substring(0, 32), 2);  // seg event ID
			short segEventCancelIndicator = Short.parseShort(str.substring(32, 33), 2);  // seg cancel indicator
			segDesc.setSegmentEventId(SegEventId);
			segDesc.setSegmentationEventCancelIndicator(segEventCancelIndicator == 1);

			str = str.substring(33 + 7);  // also include 7 bit reserved

			if(segEventCancelIndicator == 0) {
				str = parseSegmentationStep2(str, segDesc);
			}
		} else if(Long.parseLong(str.substring(0,8), 2) == SpliceDescriptorType.AVAIL_DESCRIPTOR.getDescriptorType()){
			str = str.substring(8+8+32+32);  //splice_descriptor_tag + descriptor_length + identifier+provider_avail_id
		} else if(Long.parseLong(str.substring(0,8), 2) == SpliceDescriptorType.DTMF_DESCRIPTOR.getDescriptorType()){
			str = str.substring(8+8+32+8);  //splice_descriptor_tag + descriptor_length + identifier+preroll
			long dtmfCount = Long.parseLong(str.substring(0, 3), 2);
			str = str.substring(3+5);  //dtmf_count+reserved
			int dtmfChar = (int)dtmfCount*8; //dtmfChar
			str = str.substring(dtmfChar);
		} else {
			Long descriptorType = Long.parseLong(str.substring(0,8), 2);
			Long descriptorLengthInBits = Long.parseLong(str.substring(8,16),2) * 8;
			LOG.info("Unknown splice descriptor type received (" + str.substring(0,8) + "): " + descriptorType + 
					". Skipping next " + descriptorLengthInBits + " bits from the remaining string of length: " + str.length() + ".");
			Long numberOfBitsActuallySkipping = (8 + 8 + descriptorLengthInBits) > str.length() ? str.length() : (8 + 8 + descriptorLengthInBits);
			str = str.substring(numberOfBitsActuallySkipping.intValue());
		}
		return str;
	}

	public static String parseSpliceTime(String str, StringBuilder pts) {
		String value;
		// splice_time
		// start splice_time()
		value = str.substring(0, 1);  // time_specified_flag
		if("1".equals(value)) {  // read another 6 + 33 = 39 bits
			pts.append(str.substring(7, 40));
			str = str.substring(40);
		} else { // read another 7 bits
			str = str.substring(8);
		}
		// end splice_time()
		return str;
	}

	public static String applySignalTimeOffset(String ptsBinaryString, long offset) {
		long pts = Long.parseLong(ptsBinaryString, 2);
		pts += offset*90;
		StringBuffer str = new StringBuffer();
		for(int i=0; i<33; i++) {
			str.append('0');
		}
		String value = Long.toBinaryString(pts);
		str.replace(33-value.length(), 33, value);
		return str.toString();
	}
	
	public static String encodeScte35DataToBinary(final SCTE35PointDescriptorType scte35Pt, String pts, String pts_adjustment) {
		String bitStr = Scte35BinaryUtil.scte35SpliceInfoSection(scte35Pt, pts, pts_adjustment);
		if(TRACE_ENABLED) { LOG.trace("final: " + bitStr); }
		
		if(bitStr.length() % 8 != 0) {
			throw new RuntimeException("wrong bit str length : " + bitStr);
		}
		
		byte[] bts = new byte[bitStr.length()/8];
		for(int i = 0; i < bitStr.length()/8 ; i++) {
			 bts[i] = (byte) Integer.parseInt(bitStr.substring(i * 8, (i+1) * 8), 2); //so mode 2
		}
		byte[] encoded = Base64.encodeBase64(bts);
		char[] encodeChar = new char[encoded.length]; 
		for(int i = 0; i < encoded.length; i++) {
			encodeChar[i] = (char)encoded[i];
		}
		
		return String.valueOf(encodeChar);
	}
	
	public static Map<String, String> adjustAQPointSignalOffsetInPTS(final HashMap<String, String> ptsTimes, String acquisitionSignalID, boolean isCPOAborted, long acquisitionPointSignalTimeOffset) {
		String ptsTimeInBinary = null;
		long ptsTimeinNintyKH = 0l;
		long ptsTimePlusOffsetinNintyKH = 0l;
		long ptsTimePlusOffsetInMillis = 0l;
		String ptsTimePlusOffsetInBinary = null;
		Map<String, String> ptsTimeSignalOffsetAdjustedMap = new HashMap<>();
		
		//unsupportedSCCRequestFlag determines that the incoming SCC request was never supported by our system, in such case no need to adjust time.
		if (ptsTimes != null) {
			ptsTimeInBinary = ptsTimes.get(acquisitionSignalID);		
			if (isCPOAborted) {//For Abort
				if ((ptsTimeInBinary != null) && !ptsTimeInBinary.isEmpty()) {
					ptsTimeinNintyKH = Long.parseLong(ptsTimeInBinary, 2);
				} else {
					ptsTimeInBinary = "0";
				}
				ptsTimePlusOffsetinNintyKH = ptsTimeinNintyKH + (acquisitionPointSignalTimeOffset * 90);
				ptsTimePlusOffsetInMillis = ptsTimePlusOffsetinNintyKH / 90;

				if (ptsTimePlusOffsetinNintyKH > 0) {
					ptsTimePlusOffsetInBinary = Scte35BinaryUtil.applySignalTimeOffset(ptsTimeInBinary, acquisitionPointSignalTimeOffset);
				} else {
					if (DEBUG_ENABLED) {
						LOG.debug("pts_time + offset value is a -ve value: " + ptsTimePlusOffsetinNintyKH + ". As pts_time can't be -ve so setting it to zero.");
					}
					ptsTimePlusOffsetInBinary = Scte35BinaryUtil.toBitString(0l, 33);
				}

			} else {//For Everything Else.
				try {
					if ((ptsTimeInBinary != null) && !ptsTimeInBinary.isEmpty()) {//old condition
						ptsTimePlusOffsetInBinary = Scte35BinaryUtil.applySignalTimeOffset(ptsTimeInBinary, acquisitionPointSignalTimeOffset);
					} else {
						ptsTimePlusOffsetInBinary = Scte35BinaryUtil.applySignalTimeOffset("0", acquisitionPointSignalTimeOffset);
					}
				} catch (Exception stringIndexOutOfBoundsException) {
					ptsTimePlusOffsetInBinary = Scte35BinaryUtil.toBitString(0l, 33);
				}
			}
		}
		ptsTimeSignalOffsetAdjustedMap.put(CppConstants.PTS_TIME_PLUS_OFFSET_IN_MILLIS, String.valueOf(ptsTimePlusOffsetInMillis));
		ptsTimeSignalOffsetAdjustedMap.put(CppConstants.PTS_TIME_PLUS_OFFSET_IN_BINARY, ptsTimePlusOffsetInBinary);
		return ptsTimeSignalOffsetAdjustedMap;
	}
	
	
	public static void adjustPtsTimeInStream(StreamTimesType stts, long signalTimeOffset) {
		if (stts != null) {
			List<StreamTimeType> sttList = stts.getStreamTime();
			for (StreamTimeType stt : sttList) {
				if (stt.getTimeType().equalsIgnoreCase("PTS")) {
					long time = Long.parseLong(stt.getTimeValue());
					time += (signalTimeOffset * 90);
					stt.setTimeValue(Long.toString(time));
				}
			}
		}
	}
	
	public static String stringToHex(String str) {
		byte[] buffer = str.getBytes();
		
		char[] chars = new char[2 * buffer.length];
		for (int i = 0; i < buffer.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buffer[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buffer[i] & 0x0F];
		}

		return new String(chars);
	}

	public static byte[] stringToHexBytes(String str) {
		byte[] buf = str.getBytes();
		
		char[] chars = new char[2 * buf.length];
		byte[] hex = new byte[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		for(int i = 0; i < hex.length; i++) {
			hex[i] = (byte)chars[i];
		}
		
		return hex;
	}

	public static String hexToString(String hexStr) {
		try {
			return new String(Hex.decodeHex(hexStr.toCharArray()));
		} catch (DecoderException de) {
			LOG.error(()->"Received Hexadecimal Sring (" + hexStr + ") is not valid.", de);
		}
		return "";
	}
	
	public static byte[] getBytefromBits(String bits) {
		bits = bits + '0';
		byte[] vals = new byte[bits.length()/8];
		for(int i = 0; i < bits.length() - 1;) {
			String str = bits.substring(i, i + 8);
			byte b = (byte)Integer.parseInt(str, 2);
			vals[i/8] = b;
			i += 8;
		}
		
		return vals;
	}

	public static void main(String[] args) {
		String base64Encoded = "/D//AH/////////wBQb//////wAwAi5DVUVJQUJDRH+/CR9TSUdOQUw6Z0ZrUlpab2JTbStOZDdmNWNjVzFsQT09NAAA//////////8=";
		final StringBuilder pts = new StringBuilder(); 
		final StringBuilder pts_adjustment = new StringBuilder(); 
		
		SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
		Scte35BinaryUtil.decodeScte35BinaryData(base64Encoded, scte35Pt, pts, pts_adjustment);
		try {
			System.out.println("SignalId: "+new String(scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid(), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		byte[] upidBinary = scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid();
		String upidHex = new HexBinaryAdapter().marshal(upidBinary);
		String signalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
		System.out.println(signalId);
		
		String bitStr = "1111110000110000010001110000000000000000000000000000000000000000000000001111111111111111111100000000000100000110111111100000000000110101000000100011001101000011010101010100010101001001000101001000100110110011011100110111111111111111000000000000000000001001010101000100000000001001000111110101001101001001010001110100111001000001010011000011101001001001011101110101010001110001010001100110101001000110011010100101001001000011010011110100110101101100011001110111100101001111001100000011100001010001011011000111100101010001001111010011110100110101000000000000000001101000100011010010100101111000";
		
		if(bitStr.length() % 8 != 0) {
			throw new RuntimeException("wrong bit str length : " + bitStr);
		}
		
		byte[] bts = new byte[bitStr.length()/8];
		for(int i = 0; i < bitStr.length()/8 ; i++) {
			 bts[i] = (byte) Integer.parseInt(bitStr.substring(i * 8, (i+1) * 8), 2); //so mode 2
		}
		byte[] encoded = Base64.encodeBase64(bts);
		char[] encodeChar = new char[encoded.length]; 
		for(int i = 0; i < encoded.length; i++) {
			encodeChar[i] = (char)encoded[i];
		}
		
		System.out.println("encoded " + String.valueOf(encodeChar));
		
		
	}
}
