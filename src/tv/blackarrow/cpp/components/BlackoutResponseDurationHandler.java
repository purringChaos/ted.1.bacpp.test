/**
 * 
 */
package tv.blackarrow.cpp.components;

import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author Amit Kumar Sharma
 *
 * This class contains the business logic to calculate and set the duration value in the response signals.
 */
public abstract class BlackoutResponseDurationHandler {

	/**
	 * Calculates the duration to be set into the given segmentation descriptor and sets it only if the request was not for an open ended blackout. Or if it was for an open ended blackout then set the value only if 
	 * this is a Program End request.
	 * @param attributes 
	 * 
	 * @param segmentDescriptor
	 * @param cpo the confirmed placement opportunity.
	 * @param blackoutEvent the event for which this request came.
	 * @param aqpt Acquisition Point that sent this request.
	 * @param requestedSegmentTypeId specifies the request type.
	 * @param isQAMBlackoutSwitchBackRequest 
	 * @param contentDuration 
	 * @param isEndNow 
	 * 
	 * @return the calculated duration regardless of whether we set it or not.  
	 * @throws DatatypeConfigurationException
	 */
	public static long setContentIdentificationSegmentDuration(Map<QName, String> attributes, final SegmentationDescriptorType segmentDescriptor, final ConfirmedPlacementOpportunity cpo, 
			final BlackoutEvent blackoutEvent, final AcquisitionPoint aqpt, Short requestedSegmentTypeId, boolean isQAMBlackoutSwitchBackRequest, long contentDuration, boolean isEndNow) throws DatatypeConfigurationException{
        //The stop time and end time always has offset. Calculate content Id value will be recieved here and is valid for IP(Inband/OOb) both. QAM flow doesn't comes here.
		setDuration(attributes, segmentDescriptor, contentDuration, aqpt, requestedSegmentTypeId, isQAMBlackoutSwitchBackRequest, isEndNow);
		
		return contentDuration;
	}
	
	/**
	 * Sets the given duration into the given segmentation descriptor only if the request was not for an open ended blackout. 
	 * Or if it was for an open ended blackout then set the value only if this is a Program End request.
	 * Or if it is a QAM Switch back request set the duration to zero regardless of whether it is open ended or not. 
	 * @param attributes 
	 * 
	 * @param segment response segment segmentation descriptor to set the value in.
	 * @param duration duration to set in the segmentation descriptor.
	 * @param aqpt Acquisition Point that sent this request.
	 * @param requestedSegmentTypeId specifies the request type.
	 * @param isQAMBlackoutSwitchBackRequest whether this call is a QAM switch back request.
	 * @param isOpenEndedEndNow 
	 * 
	 * @throws DatatypeConfigurationException
	 */
	public static void setDuration(Map<QName, String> attributes, final SegmentationDescriptorType segment, final long duration, final AcquisitionPoint aqpt, final Short requestedSegmentTypeId
			, final boolean isQAMBlackoutSwitchBackRequest, boolean isOpenEndedEndNow) throws DatatypeConfigurationException{
		// For open ended Blackouts don't set the duration unless it is a Program End request.
		boolean setDuration = !aqpt.isFeedAllowsOpenEndedBlackouts() || isQAMBlackoutSwitchBackRequest || 
				(aqpt.isFeedAllowsOpenEndedBlackouts() && (SegmentType.isProgramEndSignal(requestedSegmentTypeId) || isOpenEndedEndNow));
		if(setDuration && attributes != null){
			attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
		}
		segment.setDuration(setDuration ? JavaxUtil.getDatatypeFactory().newDuration(isQAMBlackoutSwitchBackRequest ? 0 : duration) : null);
	}
	
	public static ConditioningInfoType getConditioningInfo(final String acquisitionSignalId, final ConfirmedPlacementOpportunity cpo, final long contentDuration, 
			final AcquisitionPoint aqpt, final boolean isQAMBlackoutSwitchBackRequest, final Short requestedSegmentTypeId, boolean isEndNow) throws DatatypeConfigurationException{
		//For QAM Switch back or aborted blackouts the duration is set as 0 in the conditioning info regardless of whether the BO is open ended or not.
		final boolean setZeroDuration = (cpo!=null && cpo.isAborted()) || isQAMBlackoutSwitchBackRequest;
		ConditioningInfoType conditioningInfo = new ConditioningInfoType();
		conditioningInfo.setAcquisitionSignalIDRef(acquisitionSignalId);
		setDuration(conditioningInfo, aqpt, setZeroDuration, contentDuration, requestedSegmentTypeId, isEndNow);
		return conditioningInfo;
	}
	
	public static EventScheduleType getEventSchedule(final long startOrCurrentTimeWithOffset, long stopTimeWithOffset, long frequency, final AcquisitionPoint aqpt, 
			final Short requestedSegmentTypeId, final boolean isAborted) throws DatatypeConfigurationException{
		
		//fix the issue that endTime is less than start time, when getting the program_end immediately after program start.
		if(stopTimeWithOffset <= startOrCurrentTimeWithOffset){
			stopTimeWithOffset=startOrCurrentTimeWithOffset;
			frequency = 0;
		}
		
		EventScheduleType eventSchedule = new EventScheduleType();
		eventSchedule.setStartUTC(SCCResponseUtil.generateUTCPoint(startOrCurrentTimeWithOffset));
		eventSchedule.setInterval(JavaxUtil.getDatatypeFactory().newDuration(frequency));
		// For open ended Blackouts don't set the and time and duration.
		eventSchedule.setStopUTC(aqpt.isFeedAllowsOpenEndedBlackouts() && !(SegmentType.isProgramEndSignal(requestedSegmentTypeId) || isAborted) ? null : 
			SCCResponseUtil.generateUTCPoint(stopTimeWithOffset));
		return eventSchedule;
	}
	
	
	public static long getStopTimeOfEvent(final long startTime, long endTime) throws DatatypeConfigurationException {
		//fix the issue that endTime is less than start time, when getting the program_end immediately after program start.
		//Same like above EventScheduleType getEventSchedule(final long startTime, long endTime, long frequency, final AcquisitionPoint aqpt, final Short requestedSegmentTypeId, final boolean isAborted)
		if (endTime <= startTime) {
			endTime = startTime;
		}
		return endTime;
	}
	
	public static void setEventSchedule(final EventScheduleType eventSchedule, final UTCPointDescriptorType startTime, final UTCPointDescriptorType endTime, 
			final Duration duration, final AcquisitionPoint aqpt, final Short requestedSegmentTypeId, final boolean isAborted) throws DatatypeConfigurationException{
		eventSchedule.setStartUTC(startTime);
		eventSchedule.setInterval(duration);
		// For open ended Blackouts don't set the end time.
		eventSchedule.setStopUTC(aqpt.isFeedAllowsOpenEndedBlackouts() && !SegmentType.isProgramEndSignal(requestedSegmentTypeId) && !isAborted ? null : endTime);
	}
	
	private static void setDuration(ConditioningInfoType conditioningInfo, AcquisitionPoint aqpt, boolean setZeroDuration, long contentDuration, final Short requestedSegmentTypeId, boolean isEndNow)
			throws DatatypeConfigurationException{
		if(contentDuration <0) {
			contentDuration=0;
		}
		Duration duration = setZeroDuration ?
							//For QAM Switch back or aborted blackouts the duration is set as 0 in the conditioning info regardless of whether the BO is open ended or not.
							JavaxUtil.getDatatypeFactory().newDuration(0) :
							// For open ended Blackouts don't set the duration.
							(aqpt.isFeedAllowsOpenEndedBlackouts()  && !(SegmentType.isProgramEndSignal(requestedSegmentTypeId) || isEndNow)? null : 
								JavaxUtil.getDatatypeFactory().newDuration(contentDuration));
		conditioningInfo.setDuration(duration);
	}

	public static void setMCCResponseDuration(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo,
			final ManifestResponseType response, ManifestInfo manifestDTO, final AcquisitionPoint acquisitionPoint)throws DatatypeConfigurationException {
		SCTE35PointDescriptorType scte35PointDescriptor = signal.getSCTE35PointDescriptor();
		List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35PointDescriptor.getSegmentationDescriptorInfo();
		SegmentationDescriptorType segmentationDescriptorType = null;
		if (segmentationDescriptorInfo != null) {
			segmentationDescriptorType = segmentationDescriptorInfo.get(0);
		}
		
		boolean setResponseDuration = canSetDurationInMccResponse(cpo, acquisitionPoint,	segmentationDescriptorType);
		
		if (cpo != null) {
			long largestDuration = cpo.getLongestDuration();
			long newDuration  = cpo.isAborted() ? cpo.getRemainingDuration() : largestDuration;
			if(newDuration<0){
				newDuration = 0;
			}
			// Let's set the duration
			response.setDuration(setResponseDuration ? JavaxUtil.getDatatypeFactory().newDuration(newDuration) : null);
			manifestDTO.setLargestDuration((int) largestDuration);
		} else {//setting duration if present in request.
			if (segmentationDescriptorType != null && segmentationDescriptorType.getDuration() != null) {
				response.setDuration(setResponseDuration ? segmentationDescriptorType.getDuration() : null);
			}else{
				response.setDuration(setResponseDuration ? JavaxUtil.getDatatypeFactory().newDuration(0) : null);
			}
		}
	}

	/**
 	 * Set the duration only when
	 * 1. For signals coming to any acquisition point that does not support open ended blackouts.
	 * 2. For all non blackout requests.
	 * 3. If the Blackout was aborted.
	 * 4. SCC Program End has been received already, in that case the actual UTC stop time in CPO would be present.
	 * 5. MCC request has the Segmentation type as Program End. 
	 * 
	 * @param cpo
	 * @param acquisitionPoint
	 * @param segmentationDescriptorType
	 * @return
	 */
	public static boolean canSetDurationInMccResponse(final ConfirmedPlacementOpportunity cpo, final AcquisitionPoint acquisitionPoint, final SegmentationDescriptorType segmentationDescriptorType) {
		boolean setResponseDuration = !acquisitionPoint.isFeedAllowsOpenEndedBlackouts() || 
				!(segmentationDescriptorType != null && SegmentType.isValidBlackoutSignal(segmentationDescriptorType.getSegmentTypeId())) ||
				(acquisitionPoint.isFeedAllowsOpenEndedBlackouts() &&
						(
						 (segmentationDescriptorType != null && SegmentType.isProgramEndSignal(segmentationDescriptorType.getSegmentTypeId())) ||
						 (cpo != null && (cpo.isAborted() || cpo.hasProgramEndReceived() && (cpo.isProgramEnded() || cpo.isConsiderActualUtcStopTimeAsProgramEnd())))
						)
				);
		return setResponseDuration;
	}
}
