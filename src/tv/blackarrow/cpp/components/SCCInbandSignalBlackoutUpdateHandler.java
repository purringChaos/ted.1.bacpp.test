package tv.blackarrow.cpp.components;

import static tv.blackarrow.cpp.utils.ResponseSignalAction.REPLACE;

import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UPIDType;

/**
 * @author PrabhatSingh
 */
public abstract class SCCInbandSignalBlackoutUpdateHandler {
	
	public static ResponseSignalType generateContentIdentificationForBlackoutEndTimeUpdate(AcquisitionPoint aqpt, final ConfirmedPlacementOpportunity cpo, final byte[] upid, final Long segmentEventId, 
			final boolean binary,final String ptsTimeInBinary, final String pts_adjustment, ArrayList<Long> contentDurationList, BlackoutEvent blackoutEvent, Map<QName, String> attributes, 
			final Short requestedSegmentTypeId, final String acquisitionSignalId, long blackoutStartTimeWithOffset, long blackoutStopTimeWithOffset, boolean isEndNow) throws DatatypeConfigurationException{
		
		long contentDuration = blackoutStopTimeWithOffset-blackoutStartTimeWithOffset;
		ResponseSignalType respSignalContentIdentification = new ResponseSignalType();
		long currentTimeOrActualStopTime = cpo!= null && cpo.isProgramEnded() ? cpo.getActualUtcStopTime() : System.currentTimeMillis();
		
		EventScheduleType eventSchedule = BlackoutResponseDurationHandler.getEventSchedule(currentTimeOrActualStopTime, blackoutStopTimeWithOffset, 
				aqpt.getContentIDFrequency() * 1000, aqpt, requestedSegmentTypeId, cpo.isAborted() || isEndNow);
		respSignalContentIdentification.setEventSchedule(eventSchedule);
		
		respSignalContentIdentification.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalContentIdentification.setAcquisitionSignalID(acquisitionSignalId);
		respSignalContentIdentification.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upid)));
		
		respSignalContentIdentification.setUTCPoint(eventSchedule.getStartUTC());
		respSignalContentIdentification.setAction(REPLACE.toString());
		
		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		// respSignalContentIdentification.setSCTE35PointDescriptor(scte35Pnt);
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(segmentEventId); //System.currentTimeMillis() & 0x3fffffff);  // event id from alter event
		//Set the duration for content identification.
		contentDurationList.add(BlackoutResponseDurationHandler.setContentIdentificationSegmentDuration(attributes, segment, cpo, blackoutEvent, aqpt, requestedSegmentTypeId, false, contentDuration, isEndNow));
		
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upid);
		segment.setSegmentTypeId(SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId());  // content identification
		segment.setSegmentNum((short)0);
		segment.setSegmentsExpected((short)0);
		if(attributes!=null){
			segment.getOtherAttributes().putAll(attributes);
		}
		if(attributes.get(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG))!=null){
			segment.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), "0");
		}
		
		if (binary) {
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pnt, ptsTimeInBinary, pts_adjustment);
			BinarySignalType binarySignal = new BinarySignalType();
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
			binarySignal.setSignalType("SCTE35");
			respSignalContentIdentification.setBinaryData(binarySignal);
		} else {
			respSignalContentIdentification.setSCTE35PointDescriptor(scte35Pnt);
		}

		return respSignalContentIdentification;
	}
}
