package tv.blackarrow.cpp.utils;

import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;

/**
 * The utility inherits the existing Scte35BinaryUtil, and is used to handle 
 * Sky/VM specific binary data decoding
 * 
 * @author jwang
 *
 */
public class SkyScte35BinaryUtil extends Scte35BinaryUtil {
	private static final Logger LOG = LogManager.getLogger(SkyScte35BinaryUtil.class);
	private static final boolean DEBUG = LOG.isDebugEnabled(); 
	
	/**
	 * 
	 * decode SCTE 35 binary data, and find out pts_adjustment, pts and provider_avail_id if available
	 * 
	 * @param binaryData
	 * @param scte35Pt
	 * @param pts
	 * @param ptsAdjustment
	 * @param providerAvailId
	 */
	public static void decodeScte35BinaryData(final byte[] binaryData, final SCTE35PointDescriptorType scte35Pt, 
			final StringBuilder pts, final StringBuilder ptsAdjustment, final StringBuilder providerAvailId) {
		final int base = 2;
		
		StringBuilder vals = new StringBuilder();
		byte[] decoded = Base64.decodeBase64(binaryData);
		for(byte bt : decoded) {
			vals.append(Scte35BinaryUtil.toBinary(bt));
		}

		if(DEBUG) {  LOG.debug(()->"decoded string: " + vals.toString()); }
		String str = vals.toString();
		
		// retrieve pts_adjustment
		String ptsAdjust = str.substring(39, 39+33);
		ptsAdjustment.append(ptsAdjust);
		if(DEBUG) {  LOG.debug(()->"pts_adjustment value: " + ptsAdjust); }
		
		str = str.substring(92);  // remove not processed part

		Integer.parseInt(str.substring(0, 12), base);  // in byte
		
		String value = str.substring(12, 20);
		int commandType = Integer.parseInt(value, base); 
		scte35Pt.setSpliceCommandType(commandType);
		
		str = str.substring(20);
		if(commandType == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {//command type == 5
			SpliceInsertType spliceInsert = new SpliceInsertType();
			scte35Pt.setSpliceInsert(spliceInsert);
			
			long eventId = Long.parseLong(str.substring(0, 32), 2);  // splice event ID
			spliceInsert.setSpliceEventId(eventId);

			final short eventCancelIndicator = Short.parseShort(str.substring(32, 33), 2);  // splice cancel indicator
			spliceInsert.setSpliceEventCancelIndicator(eventCancelIndicator == 1);
			str = str.substring(33 + 7);  // plus 7 reserved
			if(eventCancelIndicator == 0) {
				final short outNetworkIndicator = Short.parseShort(str.substring(0, 1), 2);  // out_of_network_indicator
				final short spliceFlag = Short.parseShort(str.substring(1, 2), 2);  // program_splice_flag
				final short durationFlag = Short.parseShort(str.substring(2, 3), 2);  
				final short immediateFlag = Short.parseShort(str.substring(3, 4), 2);  
				spliceInsert.setOutOfNetworkIndicator(outNetworkIndicator == 1);
				
				// if splice_insert and splice into network, do not normalize
				if(outNetworkIndicator == 0) {return;}
				
				if (immediateFlag == 1) {
					Map<QName, String> attrMap = spliceInsert.getOtherAttributes();
					if (attrMap != null) {
						attrMap.put(new QName("spliceImmediateFlag"), "1");
					}
				}
				str = str.substring(8);
				if(spliceFlag == 1 && immediateFlag == 0) {
					str = parseSpliceTime(str, pts);
					if(DEBUG) { LOG.debug(()->"pts_time: " + pts); }
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
				final int upid = Integer.parseInt(str.substring(0, 16), 2);  // unique program id
				final short availNum = Short.parseShort(str.substring(16, 24), 2);
				final short availExpected = Short.parseShort(str.substring(24, 32), 2);
				spliceInsert.setUniqueProgramId(upid);
				spliceInsert.setAvailNum(availNum);
				spliceInsert.setAvailsExpected(availExpected);
				str = str.substring(32);
				//parsing splice descriptor Tag
				searchAvailDescriptor(str, scte35Pt, providerAvailId);
			}
		} else {  // command type == 6
			ptsAdjustment.setLength(0); pts.setLength(0);
			decodeScte35BinaryData(binaryData, scte35Pt, pts, ptsAdjustment);
			long providerVailId = scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentEventId();
			providerAvailId.append(Scte35BinaryUtil.toBitString(providerVailId, 32)); 
		}
	}	
	
	
	protected static void searchAvailDescriptor(String str, final SCTE35PointDescriptorType scte35Pt, final StringBuilder providerAvailId)	{
		long segmentDescriptorLoopCount = Long.parseLong(str.substring(0, 16), 2);  // descriptor_loop_length
		
		// https://jira.crossmw.com/browse/PRODISSUE-1530
		if(SpliceCommandType.SPLICE_INSERT.getCommandtype() == scte35Pt.getSpliceCommandType() && segmentDescriptorLoopCount == 0) {
			long providerVailId = scte35Pt.getSpliceInsert().getSpliceEventId(); // use splice event ID instead
			providerAvailId.append(Scte35BinaryUtil.toBitString(providerVailId, 32)); 
		} else {
			long noOfBitsToRead = segmentDescriptorLoopCount * 8; 
			str = str.substring(16);  // after loop length
			if(noOfBitsToRead > str.length()){
				LOG.info("Invalid binary string received. No of bits specified for reading splice descriptors (" + noOfBitsToRead + 
						") is greater than number of bits remaining to read (" + str.length() + ").");
				return;
			}

			// looking for avail descriptor
			if(Long.parseLong(str.substring(0,8), 2) == SpliceDescriptorType.AVAIL_DESCRIPTOR.getDescriptorType()) {
				providerAvailId.append(str.substring(48, 80));
			} else {
				LOG.info(()->"did not find avail_descriptor");
			}
		}
	}
	
	/**
	 * generate a combination key name
	 * @param breakId
	 * @param acquisitionPointId
	 * @return
	 */
	public static String generateBreakUUIDMapping(final String breakId, final String acquisitionPointId) {
		return breakId + "-" + acquisitionPointId;	
	}
	
}
