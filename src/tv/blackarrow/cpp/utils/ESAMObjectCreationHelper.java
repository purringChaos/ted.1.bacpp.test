//
// Copyright (c) 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.utils;

import static tv.blackarrow.cpp.utils.ResponseSignalAction.REPLACE;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.components.BlackoutResponseDurationHandler;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;

public final class ESAMObjectCreationHelper {

	public static final String UPID_PREFIX = "SIGNAL:";
	private ESAMObjectCreationHelper() {}
	private static final Logger LOG = LogManager.getLogger(ESAMObjectCreationHelper.class);

	
	
	/**
	 * If the segment type ID is Content Identification (segment type ID is 1),  
	 * the response signal will include StartUTC and StopUTC
	 * Please refer to ESAM Spec
	 * @param upid
	 * @param startTimeWithOffset
	 * @param stopTimeWithOffset
	 * @param acquisitionSignalId
	 * @param contentIdentificationSignalAction 
	 * @param ptsTimeInBinary
	 * @param requestedSegmentTypeId 
	 * @param isAborted 
	 * @param isEndNow 
	 * @param acquisitionPointId
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	public static ResponseSignalType createContentIndentficationRespSignal(AcquisitionPoint aqpt, final byte[] upid, XMLGregorianCalendar startTimeWithOffset, 
			XMLGregorianCalendar stopTimeWithOffset, final String acquisitionSignalId, final boolean binary, final Long segmentEventId, final ResponseSignalAction contentIdentificationSignalAction, 
			final String ptsTimeInBinary, final String pts_adjustment, final boolean isInbandBlackoutAbortRequest, final long contentDuration, 
			final Map<QName, String> attributes, final Short requestedSegmentTypeId, final boolean isAborted, boolean isEndNow) throws DatatypeConfigurationException {
		SCTE35PointDescriptorType scte35Pnt;
		ResponseSignalType respSignalContentIdentification = new ResponseSignalType();
		respSignalContentIdentification.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalContentIdentification.setAcquisitionSignalID(acquisitionSignalId);
		respSignalContentIdentification.setAction(contentIdentificationSignalAction == null ? null : contentIdentificationSignalAction.toString());
		respSignalContentIdentification.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upid)));

		startTimeWithOffset.add( DatatypeFactory.newInstance().newDuration(aqpt.getContentIDFrequency() * 1000));  // need add interval
		
		EventScheduleType eventSchedule = BlackoutResponseDurationHandler.getEventSchedule(startTimeWithOffset.toGregorianCalendar().getTimeInMillis(), 
				stopTimeWithOffset.toGregorianCalendar().getTimeInMillis(), aqpt.getContentIDFrequency() * 1000, aqpt, requestedSegmentTypeId, isAborted || isEndNow);
		
		respSignalContentIdentification.setEventSchedule(eventSchedule);
		respSignalContentIdentification.setUTCPoint(eventSchedule.getStartUTC());  // need set UTCpoint
		
		scte35Pnt = new SCTE35PointDescriptorType();
		// respSignalContentIdentification.setSCTE35PointDescriptor(scte35Pnt);
		scte35Pnt.setSpliceCommandType(6);
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(segmentEventId); //System.currentTimeMillis() & 0x3fffffff);  // event id from alter event
		
		if(isInbandBlackoutAbortRequest) {
			segment.setSegmentationEventCancelIndicator(Boolean.TRUE);
		} else {
			// duration of this content identification segmentation
			BlackoutResponseDurationHandler.setDuration(attributes, segment, contentDuration, aqpt, requestedSegmentTypeId, false, isEndNow);
			setBasicSegmentInfoForContentIDSignal(upid, segment);
			setSegmentDescriptorAttributesInResponseSignal(attributes, segment);
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
	
	/**
	 * Utilize this function to generate response object, offset calculations must be performed before passing in this method
	 * program start will not include StartUTC and StopUTC
	 * Please refer to ESAM Spec 
	 * @param startTimeWithOffset
	 * @param stopTimeWithOffset 
	 * @param acquisitionSignalId
	 * @param ptsTimeInBinary
	 * @param requestedSegmentTypeId 
	 * @param qamBlackoutSwitchBackRequest 
	 * @param isEndNow 
	 * @param acquisitionPointId
	 * @param upid 
	 * @return ResponseSignalType
	 * @throws DatatypeConfigurationException 
	 */
	public static void setProgramStartResponseSignal(final ResponseSignalType respSignalType, final AcquisitionPoint aqpt, byte[] upId, XMLGregorianCalendar startTimeWithOffset, XMLGregorianCalendar stopTimeWithOffset, 
			String acquisitionSignalId, final ResponseSignalAction action, final Long segmentEventId, final String ptsTimeInBinary, final String pts_adjustment, final boolean isInbandBlackoutAbortRequest, 
			long contentDuration, Short segmentTypeId, Map<QName, String> attributes, final boolean qamBlackoutSwitchBackRequest, final Short requestedSegmentTypeId, boolean isOpenEndedEndNow) throws DatatypeConfigurationException {
		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		respSignalType.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalType.setAcquisitionSignalID(acquisitionSignalId);
		respSignalType.setAction(action == null ? null : action.toString());
		respSignalType.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upId)));
		scte35Pnt.setSpliceCommandType(6);		
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(segmentEventId);
		
		boolean binary = respSignalType.getBinaryData() != null;
		
		if(isInbandBlackoutAbortRequest) {
			segment.setSegmentationEventCancelIndicator(Boolean.TRUE);
		} else {
			// duration of this content identification segmentation
			BlackoutResponseDurationHandler.setDuration(attributes, segment, contentDuration, aqpt, requestedSegmentTypeId, qamBlackoutSwitchBackRequest, isOpenEndedEndNow);
			segment.setSegmentTypeId(segmentTypeId==null?SegmentType.PROGRAM_START.getSegmentTypeId():segmentTypeId);  // default to program_start
			setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upId, segment);
			setSegmentDescriptorAttributesInResponseSignal(attributes, segment);
		}

		setSegmentDescriptorAsBinaryOrXMLInResponseSignal(ptsTimeInBinary, pts_adjustment, respSignalType, scte35Pnt, binary);
		
		// set UTC for program start ResponseSignal
		respSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(startTimeWithOffset.toGregorianCalendar().getTimeInMillis()));
	}
	
	

	/**
	 * for blackout case, POIS needs send notification (SignalProcessingNotificationType) 
	 * to the transcoders. The method is to generate/populate SignalProcessingNotificationType object
	 * @param signalId
	 * @param blackoutEventStartTimeWithOffset
	 * @param blackoutEventStopTimeWithOffset
	 * @param acquisitionSignalId
	 * @param programStartSignalAction 
	 * @param contentIdentificationSignalAction 
	 * @param boEventUIorESNIAction 
	 * @param isSwitchBackJob 
	 * @param acquisitionPointId
	 * 
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	
	public static SignalProcessingNotificationType constructBlackoutNotificationObject(final AcquisitionPoint aqpt, long currentSystemTime, final ConfirmedPlacementOpportunity cpo, 
			final String signalId, final long blackoutEventStartTimeWithOffset, final long blackoutEventStopTimeWithOffset, String acquisitionSignalId, Long segmentEventId, 
			final ResponseSignalAction programStartSignalAction, final ResponseSignalAction contentIdentificationSignalAction, Short segmentTypeId, 
			BOEventUIorESNIAction boEventUIorESNIAction, boolean isSwitchBackJob) throws DatatypeConfigurationException {
		
		final boolean isBinary = true;
		
	    if(aqpt == null){
	    	LOG.warn("Acquisition Point is null while prepare the out-of-band notification.");
	        return null;
	    }
	    
	    if(segmentTypeId==null){
	    	segmentTypeId = SegmentType.PROGRAM_START.getSegmentTypeId();
	    }
		
        // create Out-of-Band notification message
		SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		
		DatatypeFactory df = DatatypeFactory.newInstance();
    	GregorianCalendar tempGregorianCalendar = new GregorianCalendar();
        
    	//Create blackout event start time element.
    	tempGregorianCalendar.setTimeInMillis(blackoutEventStartTimeWithOffset);
        XMLGregorianCalendar blackoutEventStartTimeForProgramStartWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);
        XMLGregorianCalendar blackoutEventStartTimeForContentIdentification = df.newXMLGregorianCalendar(tempGregorianCalendar);
        
        //Create blackout event end time element.
        tempGregorianCalendar.setTimeInMillis(blackoutEventStopTimeWithOffset);
        XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);

		// Create program start component of the notification.
        ResponseSignalType respSignalProgramStart = new ResponseSignalType();
        respSignalProgramStart.setBinaryData(new BinarySignalType());
		final String upidStr = ESAMHelper.generateUpidString(signalId);
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		//TODO:Amit, Following line was introduced to test the patch and should be cleaned up in the later releases.
		final String pts_time = Boolean.valueOf(System.getProperty("cadent.test.pts", Boolean.FALSE.toString())) ? "000010100110011001111111010100101" : "";
		if(Boolean.valueOf(System.getProperty("cadent.test.pts", Boolean.FALSE.toString()))) {
			LOG.error(()->"\"cadent.test.pts\" system property should not be set in a production environment, this should only be set for testing in Dev and QA environments. "
					+ "Actual out of band notification should send empty string \"\" i.e. 0 in the pts_time.");
		}
		setProgramStartResponseSignal(respSignalProgramStart, aqpt, upid, blackoutEventStartTimeForProgramStartWithOffset, blackoutEventStopTimeAsXMLGCWithOffset, acquisitionSignalId, 
				programStartSignalAction, segmentEventId, pts_time, Scte35BinaryUtil.toBitString(0, 33), false, blackoutEventStopTimeWithOffset - blackoutEventStartTimeWithOffset, segmentTypeId, 
				null, isSwitchBackJob, null, false);
		
		//Add this program start in the notification.
		notification.getResponseSignal().add(respSignalProgramStart);

		//Content id is not for QAM.
		if(aqpt.isIpAcquisitionPoint()){
			//Comment out to remove start signal
			notification.getResponseSignal().clear();
			// create content identification component of the notification.
			ResponseSignalType respSignalContentIdentification = createContentIndentficationRespSignal(aqpt, upid, blackoutEventStartTimeForContentIdentification,
					blackoutEventStopTimeAsXMLGCWithOffset, acquisitionSignalId, isBinary, segmentEventId, contentIdentificationSignalAction, "", Scte35BinaryUtil.toBitString(0, 33), 
					false, blackoutEventStopTimeWithOffset - blackoutEventStartTimeWithOffset, null, null, (cpo != null && cpo.isAborted()), false);

			//Add this content identification in the notification.
			notification.getResponseSignal().add(respSignalContentIdentification);
			
			//PU-110 All the IP Acquisition points should be notified that duration was changed from UI. Runover signal should be added only for Standard Feed. (Not on Open Ended Feed)
			if (!aqpt.isFeedAllowsOpenEndedBlackouts() && boEventUIorESNIAction != null && boEventUIorESNIAction.compareTo(BOEventUIorESNIAction.UPDATE) == 0) {
				ResponseSignalType respSignalRunoverUnplanned = createProgramRunoverUnplannedRespSignal(aqpt, upid, blackoutEventStartTimeForProgramStartWithOffset,
						blackoutEventStopTimeAsXMLGCWithOffset, acquisitionSignalId, segmentEventId, "", Scte35BinaryUtil.toBitString(0, 33), null, currentSystemTime, true);
				//Add this content identification in the notification.
				notification.getResponseSignal().add(respSignalRunoverUnplanned);
			}
		}
		else {
		// list of ConditioningInfo of this notification
		List<ConditioningInfoType> conditioningInfoList = notification.getConditioningInfo();
		
		//Out-of-band, the cpo is not confirmed yet.
		ConditioningInfoType conditioningInfo = BlackoutResponseDurationHandler.getConditioningInfo(acquisitionSignalId, cpo, blackoutEventStopTimeWithOffset-blackoutEventStartTimeWithOffset, 
				aqpt, isSwitchBackJob, null, false);
		
		conditioningInfoList.add(conditioningInfo);
		}
		return notification;
	}
	
	public static SignalProcessingNotificationType createTerritoryUpdateTransCoderNotification(long currentSystemTime,BlackoutEvent blackoutEvent,AcquisitionPoint acquisitionPoint,ConfirmedPlacementOpportunity cpo,DataManager dataManager) throws DatatypeConfigurationException{
		
		long segmentEventId = currentSystemTime & 0x3fffffff;
		try {
			segmentEventId = Long.valueOf(blackoutEvent.getEventId());
		} catch(Exception ex) {
			LOG.debug(()->"Event id \"" + blackoutEvent.getEventId() + "\" is not a number and can not be parsed as long value "+" : Using the system generated segment event id.");
		}
		
			SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
			notification.setAcquisitionPointIdentity(acquisitionPoint.getAcquisitionPointIdentity());
			

			DatatypeFactory df = DatatypeFactory.newInstance();
	    	GregorianCalendar tempGregorianCalendar = new GregorianCalendar();

	    	long blackoutEventStartTimeWithOffset = cpo.getActualUtcStartTime()+acquisitionPoint.getSignalTimeOffset();
	    	long blackoutEventStopTimeWithOffset  = BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent)+ acquisitionPoint.getSignalTimeOffset();
	        
	    	//Create blackout event start time element.
	    	tempGregorianCalendar.setTimeInMillis(blackoutEventStartTimeWithOffset);
	        XMLGregorianCalendar startUTCTimeWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);
	        
	        //Create blackout event end time element.
	        tempGregorianCalendar.setTimeInMillis(blackoutEventStopTimeWithOffset);
	        XMLGregorianCalendar stopUTCTimeWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);
	        
	        //create the Acquisition Signal Id
	        String acquisitionSignalId = UUIDUtils.getBase64UrlEncodedUUID();
	        // Create program start component of the notification.
	        ResponseSignalType respSignalProgramStart = new ResponseSignalType();
	        respSignalProgramStart.setBinaryData(new BinarySignalType());
			String upidStr = ESAMHelper.generateUpidString(blackoutEvent.getSignalId());
			byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
			ESAMObjectCreationHelper.setProgramStartResponseSignal(respSignalProgramStart, acquisitionPoint, upid, startUTCTimeWithOffset, stopUTCTimeWithOffset, acquisitionSignalId, 
					REPLACE, segmentEventId, "", Scte35BinaryUtil.toBitString(0, 33), false, blackoutEventStopTimeWithOffset - blackoutEventStartTimeWithOffset, null, 
					null, false, null, false);
	        
			notification.getResponseSignal().add(respSignalProgramStart);
			SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPoint.getAcquisitionPointIdentity());
	        upidStr = ESAMHelper.generateUpidString(cursor.getLastConfirmedBlackoutSignalId());
			upid = new HexBinaryAdapter().unmarshal(upidStr);

			
			ResponseSignalType respSignalContentIdentification = ESAMObjectCreationHelper.createContentIndentficationRespSignal( acquisitionPoint, upid, startUTCTimeWithOffset, 
					stopUTCTimeWithOffset, acquisitionSignalId, true, 
					segmentEventId, REPLACE, "", Scte35BinaryUtil.toBitString(0, 33), false, blackoutEventStopTimeWithOffset-blackoutEventStartTimeWithOffset, 
					null, null, (cpo != null && cpo.isAborted()), false);
			
			//Add this content identification in the notification.
			notification.getResponseSignal().add(respSignalContentIdentification);
			return notification;
			
	}

	private static ResponseSignalType createProgramRunoverUnplannedRespSignal(final AcquisitionPoint aqpt, byte[] upId, XMLGregorianCalendar startTime,
			XMLGregorianCalendar stopTime, String acquisitionSignalId, final Long segmentEventId, final String ptsTimeInBinary, final String pts_adjustment,
			Map<QName, String> attributes, long currentSystemTime, boolean binary) throws DatatypeConfigurationException {
		ResponseSignalType respSignalType = new ResponseSignalType();
		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		scte35Pnt.setSpliceCommandType(6);
		respSignalType.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalType.setAcquisitionSignalID(acquisitionSignalId);
		respSignalType.setSignalPointID(ESAMHelper.getSignalIdFromUPIDHexString(new HexBinaryAdapter().marshal(upId)));

		respSignalType.setAction("create");
		segment.setSegmentEventId(segmentEventId);
		segment.setSegmentTypeId(SegmentType.PROGRAM_RUNOVER_UNPLANNED.getSegmentTypeId());
		// duration of this content identification segmentation (Event Stop Time - Current System Time) 
		long eventStopTime = BlackoutResponseDurationHandler.getStopTimeOfEvent(startTime.toGregorianCalendar().getTimeInMillis(),
				stopTime.toGregorianCalendar().getTimeInMillis());
		segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(eventStopTime - currentSystemTime));
		if (binary) {
			respSignalType.setBinaryData(new BinarySignalType());
		}
		setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upId, segment);
		setSegmentDescriptorAttributesInResponseSignal(attributes, segment);
		setSegmentDescriptorAsBinaryOrXMLInResponseSignal(ptsTimeInBinary, pts_adjustment, respSignalType, scte35Pnt, binary);

		// set UTC for program start ResponseSignal		
		respSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(currentSystemTime));
		return respSignalType;
	}

	public static void setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(byte[] upId, SegmentationDescriptorType segment) {
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upId);
		segment.setSegmentNum((short) 1);
		segment.setSegmentsExpected((short) 1);
	}

	public static void setBasicSegmentInfoForContentIDSignal(final byte[] upid, SegmentationDescriptorType segment) {
		segment.setSegmentTypeId(SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId());  // content identification
		segment.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segment.setUpid(upid);			
		segment.setSegmentNum((short)0);
		segment.setSegmentsExpected((short)0);
	}
	
public static void setSegmentDescriptorAsBinaryOrXMLInResponseSignal(final String ptsTimeInBinary, final String pts_adjustment, ResponseSignalType respSignalType,
			SCTE35PointDescriptorType scte35Pnt, boolean binary) {
		if (binary) {
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pnt, ptsTimeInBinary, pts_adjustment);
			BinarySignalType binarySignal = new BinarySignalType();
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
			binarySignal.setSignalType("SCTE35");
			respSignalType.setBinaryData(binarySignal);
			respSignalType.setSCTE35PointDescriptor(null);
		} else {
			respSignalType.setSCTE35PointDescriptor(scte35Pnt);
		}
	}

	public static void setSegmentDescriptorAttributesInResponseSignal(Map<QName, String> attributes, SegmentationDescriptorType segment) {
		if (attributes == null) {
			segment.getOtherAttributes().put(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG), "0");
			segment.getOtherAttributes().put(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG), "1");
			segment.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), "0");
			segment.getOtherAttributes().put(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG), "1");
			segment.getOtherAttributes().put(new QName(CppConstants.DEVICE_RESTRICTIONS), "0");
		} else {
			segment.getOtherAttributes().putAll(attributes);
			if (attributes.get(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG)) != null) {
				segment.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), "0");
			}
		}
	}
	
	public static void setResponseStatusCode(SignalProcessingNotificationType notificationResponse, MuleEventContext context) {

		if (notificationResponse != null && context != null) {
			// Error handling.
			String error = (String) context.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
			StatusCodeType statusCode = new StatusCodeType();
			if (error != null) {
				statusCode = SCCResponseUtil.generateErrorStatusCode(error);
			} else {
				statusCode.setClassCode(ContextConstants.SUCCESS_STATUS_CODE);
			}
			notificationResponse.setStatusCode(statusCode);
		}
	}

}
