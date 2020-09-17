/**
 *
 */
package tv.blackarrow.cpp.utils;

import static tv.blackarrow.cpp.utils.SpliceCommandType.SPLICE_INSERT;
import static tv.blackarrow.cpp.utils.SpliceCommandType.TIME_SIGNAL;
import static tv.blackarrow.cpp.utils.SpliceCommandType.valueOf;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.safehaus.uuid.UUID;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;

/**
 * @author amit
 *
 */
public abstract class SCCResponseUtil {

	private static final Logger LOGGER = LogManager.getLogger(SCCResponseUtil.class);

	private static Map<String, TimeZone> timeZoneMap;

	static{
		timeZoneMap = new ConcurrentHashMap<String, TimeZone>();
		timeZoneMap.put("GMT", TimeZone.getTimeZone("GMT"));
	}

	private SCCResponseUtil() {}

	/**
	 * Retrieves the segment/splice event id from the response signal.
	 *
	 * @param responseSignalType
	 * @return
	 */
	public static Long getIdForCountDownQueueCheck(final ResponseSignalType responseSignalType){
		if(isBinary(responseSignalType)){
			return getIdForCountDownQueueCheck(responseSignalType.getBinaryData());
		} else {
			return getIdForCountDownQueueCheck(responseSignalType.getSCTE35PointDescriptor());
		}
	}

	/**
	 * Retrieves the segment/splice event id from the binary data.
	 *
	 * @param binaryData
	 * @return
	 */
	public static Long getIdForCountDownQueueCheck(final BinarySignalType binaryData) {
		SCTE35PointDescriptorType scte35PointDescriptorType = getSCTE35PointDescriptor(binaryData.getValue());
		return getIdForCountDownQueueCheck(scte35PointDescriptorType);
	}

	/**
	 * Retrieves the segment/splice event id from the descriptor.
	 *
	 * @param scte35PointDescriptorType
	 * @return
	 */
	public static Long getIdForCountDownQueueCheck(final SCTE35PointDescriptorType scte35PointDescriptorType) {
		SpliceCommandType spliceCommandType = valueOf(scte35PointDescriptorType.getSpliceCommandType());
		if(spliceCommandType==null){
			return null;
		}
		switch(spliceCommandType){
			case SPLICE_INSERT:
				Long spliceEventId = null;
				List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35PointDescriptorType.getSegmentationDescriptorInfo();
				if (!segmentationDescriptorInfo.isEmpty() && segmentationDescriptorInfo.get(0) != null) {
					spliceEventId = segmentationDescriptorInfo.get(0).getSegmentEventId();
				} else {
					SpliceInsertType spliceInsert = scte35PointDescriptorType.getSpliceInsert();
					if (spliceInsert != null) {
						spliceEventId = spliceInsert.getSpliceEventId();
					}
				}
				LOGGER.debug("Splice Event Id is: " + spliceEventId);
				return spliceEventId;
			case TIME_SIGNAL:
				Long segmentEventId = null;
				List<SegmentationDescriptorType> segmentationDescriptorInfos = scte35PointDescriptorType.getSegmentationDescriptorInfo();
				if(!segmentationDescriptorInfos.isEmpty() && segmentationDescriptorInfos.get(0) != null){
					segmentEventId = segmentationDescriptorInfos.get(0).getSegmentEventId();
				}
				LOGGER.debug("Segment Event Id is: " + segmentEventId);
				return segmentEventId;
			default:
				throw new IllegalArgumentException("Splice Command type \"" + spliceCommandType + "\" is not supported.");
		}
	}

	/**
	 * Identifies weather a request is binary or not.
	 *
	 * @param responseSignalType
	 * @return
	 */
	public static boolean isBinary(final ResponseSignalType responseSignalType) {
		return responseSignalType.getBinaryData() != null &&
				responseSignalType.getBinaryData().getValue() != null &&
				responseSignalType.getBinaryData().getValue().length >0;
	}

	/**
	 * Decodes the binary request into object form to retrieve the segment/splice event id.
	 *
	 * @param binaryData
	 * @return
	 */
	private static SCTE35PointDescriptorType getSCTE35PointDescriptor(final byte[] binaryData) {
		final SCTE35PointDescriptorType scte35PointDescriptorType = new SCTE35PointDescriptorType();
		final byte[] encoded = Base64.encodeBase64(binaryData);
		Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35PointDescriptorType, new StringBuilder(), new StringBuilder());
		return scte35PointDescriptorType;
	}


	public static SegmentationDescriptorType generateSegment(final byte[] upid, final Long duration, short segmentTypeId, boolean segEventCancelIndicator,
			Long segmentEventId, short segmentNum, short segmentExpected, short upidType) {
		final SegmentationDescriptorType segment = new SegmentationDescriptorType();
		setSegmentationFieldValues(upid, duration, segmentTypeId, segEventCancelIndicator, segmentEventId, segmentNum, segmentExpected, upidType, segment);
		return segment;
	}

	public static void setSegmentationFieldValues(final byte[] upid, final Long duration, short segmentTypeId, boolean segEventCancelIndicator, Long segmentEventId,
			short segmentNum, short segmentExpected, short upidType, final SegmentationDescriptorType segment) {
		if (duration != null) {
			try {
				segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			} catch (DatatypeConfigurationException e) {
				LOGGER.error(e.getMessage() + "duration " + duration, e);
			}
		}

		segment.setSegmentationEventCancelIndicator(segEventCancelIndicator);
		segment.setSegmentEventId(segmentEventId);

		if(!segEventCancelIndicator){
			segment.setSegmentNum(segmentNum);
			segment.setSegmentsExpected(segmentExpected);
			segment.setSegmentTypeId(segmentTypeId);
			segment.setUpid(upid);
			segment.setUpidType(upidType);
		}
	}

	public static UTCPointDescriptorType generateUTCPoint(long utcTime){
		try{
			UTCPointDescriptorType utcPoint = new UTCPointDescriptorType();
			utcPoint.setUtcPoint(generateUTC(utcTime));
			return utcPoint;
		}catch(Exception e){
			LOGGER.error("Unexpected error while create UTC Point "+ utcTime, e);
		}
		return null;
	}
	public static tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType generateI03UTCPoint(long utcTime){
		try{
			tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType utcPoint = new tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType();
			utcPoint.setUtcPoint(generateUTC(utcTime));
			return utcPoint;
		}catch(Exception e){
			LOGGER.error("Unexpected error while create UTC Point "+ utcTime, e);
		}
		return null;
	}

	public static UTCPointDescriptorType adjustUTCPoint(XMLGregorianCalendar originalCalendar, long adjustTime){
		try{
			XMLGregorianCalendar cal = (XMLGregorianCalendar) originalCalendar.clone();
			cal.add(JavaxUtil.getDatatypeFactory().newDuration(adjustTime));
			UTCPointDescriptorType utcPoint = new UTCPointDescriptorType();
		    utcPoint .setUtcPoint(cal);
			return utcPoint;
		}catch(Exception e){
			LOGGER.error("Unexpected error while adjust UTC Point with "+ adjustTime, e);
		}
		return null;
	}


	public static XMLGregorianCalendar generateUTC(long utcTime){
		try{
			GregorianCalendar gcalendar = new GregorianCalendar();
			gcalendar.setTimeZone(getTimeZone("GMT"));
			gcalendar.setTimeInMillis(utcTime);
			XMLGregorianCalendar cal = JavaxUtil.getDatatypeFactory().newXMLGregorianCalendar(gcalendar);
			return cal;
		}catch(Exception e){
			LOGGER.error("Unexpected error while create UTC time "+ utcTime, e);
		}
		return null;
	}


	public static byte[] generateBinaryData(final byte[] upid,
														Long duration,
														SCTE35PointDescriptorType scte35Pt,
														String ptsTime,
														String pts_adjustment,
														boolean segEventCancelIndicator,
														AcquisitionPoint acquisitionPoint) {
		try {
			// set UPID based on the Signal ID
			/*
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuffer pts = new StringBuffer();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts);
			*/
			if(scte35Pt != null && valueOf(scte35Pt.getSpliceCommandType()) == SPLICE_INSERT) {
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				Long spliceEventId = getIdForCountDownQueueCheck(scte35Pt);
				scte35Pt.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
				scte35Pt.setSpliceInsert(null);

				// add one segmentation descriptor
				final SegmentationDescriptorType segment = generateSegment(upid, duration, AcquisitionPoint.getDefaultSegmentTypeForAdStart(acquisitionPoint).getSegmentTypeId(),
						segEventCancelIndicator, spliceEventId, (short)0, (short)0, (short)9);
				scte35Pt.getSegmentationDescriptorInfo().add(segment);
			} else if(scte35Pt != null && valueOf(scte35Pt.getSpliceCommandType()) == TIME_SIGNAL) {
				if(!scte35Pt.getSegmentationDescriptorInfo().isEmpty() && !segEventCancelIndicator) {
					scte35Pt.getSegmentationDescriptorInfo().get(0).setUpid(upid);
					scte35Pt.getSegmentationDescriptorInfo().get(0).setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
				}
			}

			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, ptsTime, pts_adjustment);
			return Base64.decodeBase64(encodedStr.getBytes());
		} catch(Exception ex) {
			LOGGER.error("binary data process issue", ex);
		}
		return null;
	}

	public static StatusCodeType generateErrorStatusCode(String errorMessage){
		StatusCodeType statusCode = new StatusCodeType();
		statusCode.setClassCode("1");
		statusCode.setDetailCode("1");
		statusCode.getNote().add(errorMessage);
		return statusCode;
	}

	public static TimeZone getTimeZone(String timeZoneId){
		TimeZone t = timeZoneMap.get(timeZoneId);
		if(t != null){
			return t;
		}

		t = TimeZone.getTimeZone(timeZoneId);
		if(t != null){
			timeZoneMap.put(timeZoneId, t);
		}
		return t;
	}


	public static ResponseSignalType generateResponseSignal(String signalId, String acquistionPointId, String action,
			UTCPointDescriptorType utc, String ptsTimeInBinary, String pts_adjustment, long duration, SegmentType segmentType,
			Long segmentEventId, boolean isBinary ){
		ResponseSignalType signal = new ResponseSignalType();
		signal.setAcquisitionPointIdentity(acquistionPointId);
		signal.setAcquisitionSignalID(signalId);
		signal.setAction(action);
		signal.setSignalPointID(signalId);
		signal.setUTCPoint(utc);

		generatedAndAddSCTE35PointDescriptor(signal, signalId, ptsTimeInBinary, pts_adjustment, duration, segmentType, segmentEventId, isBinary);
		return signal;
	}

	public static void generatedAndAddSCTE35PointDescriptor(ResponseSignalType signal, String signalId, String ptsTimeInBinary, String pts_adjustment, long duration, SegmentType segmentType, Long segmentEventId, boolean isBinary){
		if(signal == null){return;}
		// generate scte35PointDescriptor for this in point
		byte[] upidInPoint = (ESAMHelper.UPID_PREFIX + signalId).getBytes();
		SCTE35PointDescriptorType scte35Point = new SCTE35PointDescriptorType();
		scte35Point.setSpliceCommandType(6);
		SegmentationDescriptorType inPointSegment = SCCResponseUtil.generateSegment(upidInPoint, duration, segmentType.getSegmentTypeId(), false,
				segmentEventId, (short)0, (short)0, (short)9);
		scte35Point.getSegmentationDescriptorInfo().add(inPointSegment);

		if (isBinary) {
			// encode scte35Point to binary
			// first calculate pts_time
			long ptstime = 0;
			try {
				ptstime = Long.parseLong(ptsTimeInBinary, 2);
			}
			catch (NumberFormatException e) {
				ptstime = 0;
			}
			ptstime += 90 * duration;	// duration already in millisecond

			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Point, Scte35BinaryUtil.toBitString(ptstime, 33), pts_adjustment);
			BinarySignalType bst = new BinarySignalType();
			bst.setValue(Base64.decodeBase64(encodedStr.getBytes()));
			bst.setSignalType("SCTE35");
			signal.setBinaryData(bst);
		}
		else {
			signal.setSCTE35PointDescriptor(scte35Point);
		}
	}

	/**
	 *
	 * Retrieves the segment type id from the descriptor.
	 * @param scte35PointDescriptorType
	 * @return
	 */
	public static Short getSegmentationTypeId(final SCTE35PointDescriptorType scte35PointDescriptorType) {
		Short segmentTypeId = null;
		List<SegmentationDescriptorType> segmentationDescriptorInfos = scte35PointDescriptorType.getSegmentationDescriptorInfo();
		if(!segmentationDescriptorInfos.isEmpty() && segmentationDescriptorInfos.get(0) != null){
			segmentTypeId = segmentationDescriptorInfos.get(0).getSegmentTypeId();
		}
		LOGGER.debug("Segment Type Id is: " + segmentTypeId);
		return segmentTypeId;
	}

	/**
	 * Retrieves the SegmentationDescriptor from scte35PointDescriptorType to save/persist in couchbase.
	 * Save the upid from request if present otherwise save the generated upid
	 * @param scte35PointDescriptorType
	 * @param upid
	 * @return
	 */
	public static SegmentationDescriptor getSegmentationDescriptor(final SCTE35PointDescriptorType scte35PointDescriptorType, byte[] upid) {
		return getSegmentationDescriptor(scte35PointDescriptorType,upid, null);
	}

	public static SegmentationDescriptor getSegmentationDescriptor(final SCTE35PointDescriptorType scte35PointDescriptorType, byte[] upid, SegmentType segmentTypedDecsriptor) {
		
		SegmentationDescriptor segmentationDescriptor = new SegmentationDescriptor();
		
		boolean doNotPopulateUpid = false;
		if (scte35PointDescriptorType != null && scte35PointDescriptorType.getSegmentationDescriptorInfo() != null && !scte35PointDescriptorType.getSegmentationDescriptorInfo().isEmpty()) {
			SegmentationDescriptorType segmentationDescriptorTypeContentIdentification = null;
			SegmentationDescriptorType segmentationDescriptorType = null;
			
			
			if(segmentTypedDecsriptor == null){
				segmentationDescriptorType = scte35PointDescriptorType.getSegmentationDescriptorInfo().get(0);
			}else{
				//find the one that is passed in argument
				for(SegmentationDescriptorType descriptorType: scte35PointDescriptorType.getSegmentationDescriptorInfo()){
					if (descriptorType.getSegmentTypeId() != null) {
						if (segmentTypedDecsriptor.getSegmentTypeId() == descriptorType.getSegmentTypeId()) {
							segmentationDescriptorTypeContentIdentification = descriptorType;
						} else if (SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId() == descriptorType.getSegmentTypeId()) {
							segmentationDescriptorType = descriptorType;
						}
					}
				}
			}
			//Means we didn't get any content descriptor for use in band PO.
			if(segmentationDescriptorTypeContentIdentification == null){
				//it means no content descriptor is present in the request. So do not populate the UPID in the couchbase.
				doNotPopulateUpid = true;				
			}
			if (segmentationDescriptorType!=null && segmentationDescriptorType.getSegmentTypeId() != null) {
				segmentationDescriptor.setSegmentationTypeId(segmentationDescriptorType.getSegmentTypeId());
			}
			if(!doNotPopulateUpid){
				if(segmentTypedDecsriptor == null){
					//Normal flow as we don't have descriptor.
					String upidAsString = new HexBinaryAdapter().marshal(segmentationDescriptorType.getUpid());
					segmentationDescriptor.setSegmentationUpid(upidAsString);
				}else{
					//Viacom flow as we received request to populate CONTENT segment type .
					byte[] upidBinary = segmentationDescriptorTypeContentIdentification.getUpid();
					String upidAsString = new HexBinaryAdapter().marshal(upidBinary);
					if(upidBinary!=null && upidBinary.length > 0) {
						String viacomSignal = new UUID(Arrays.copyOfRange(upidBinary, 5, 21)).toString();
						if (LOGGER.isDebugEnabled()) {
							byte[] format_identifier = Arrays.copyOfRange(upidBinary, 0, 4);//4 byte
							String str_format_identifier = new String(format_identifier);
							byte[] viacom_upid_version = Arrays.copyOfRange(upidBinary, 4, 5);// 1 byte
							byte[] viacom_program_id = Arrays.copyOfRange(upidBinary, 5, 21);// 16 byte
							String str_viacom_program_id = new UUID(viacom_program_id).toString();
							byte[] viacom_in_ad_break = Arrays.copyOfRange(upidBinary, 21, 22);// 1byte
							byte[] viacom_network_id = Arrays.copyOfRange(upidBinary, 22, 26);// 4byte
							String str_viacom_network_id = new HexBinaryAdapter().marshal(viacom_network_id);;
							LOGGER.debug(
										"Viacom fields populated \nformat_identifier: " + str_format_identifier + 
										"\nviacom_upid_version: " + Byte.toString(viacom_upid_version[0]) + 
										"\nviacom_program_id: "	+ str_viacom_program_id + 
										"\nviacom_in_ad_break: " + Byte.toString(viacom_in_ad_break[0]) + 
										"\nviacom_network_id: " + str_viacom_network_id+ 
										"\nUPID : " + upidAsString);
						}
						segmentationDescriptor.setProgramId(viacomSignal);
					}
					segmentationDescriptor.setSegmentationUpid(upidAsString);
					segmentationDescriptor.setSegmentationTypeId(segmentationDescriptorTypeContentIdentification.getSegmentTypeId());
				}
			}else{
				segmentationDescriptor.setSegmentationUpid(StringUtils.EMPTY);
			}
			if (segmentationDescriptorType != null) {
				segmentationDescriptor.setUpidType(segmentationDescriptorType.getUpidType());
				segmentationDescriptor.setNoRegionalBlackoutFlag(segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG)));

				segmentationDescriptor
						.setBreakDuration(segmentationDescriptorType.getDuration() != null ? segmentationDescriptorType.getDuration().getTimeInMillis(Calendar.getInstance()) : 0);
				segmentationDescriptor.setBreakNumber(segmentationDescriptorType.getSegmentNum());
				segmentationDescriptor.setTotalBreak(segmentationDescriptorType.getSegmentsExpected());
			}
		} else {
			if(segmentTypedDecsriptor == null)
				segmentationDescriptor.setSegmentationTypeId(SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId());
			else segmentationDescriptor.setSegmentationTypeId(segmentTypedDecsriptor.getSegmentTypeId());
			String upidAsString = new HexBinaryAdapter().marshal(upid);
			segmentationDescriptor.setSegmentationUpid(upidAsString);
			segmentationDescriptor.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		}
		return segmentationDescriptor;
	}

}
