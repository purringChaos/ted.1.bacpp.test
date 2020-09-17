package tv.blackarrow.cpp.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.SegmentType;

public abstract class SCTE224EventDecisionBase {

	private static final Logger LOGGER = LogManager.getLogger(SCTE224EventDecisionBase.class);
	protected static final String MULE_CONTEXT_PROPERTY_HAS_EVENT_END_TIME_CHANGED = "hasBlackoutEndTimeChanged";
	protected static final String MULE_CONTEXT_PROPERTY_IS_END_NOW = "isEndNow";


	/**
	 * Blackout searcher does not return the QAM blackout when it receives a Program Start request, if the blackout was started as a START_NOW case.
	 * Because in that case the time that will come in the program start will always be greater than the blackout's scheduled time. So fetch that blackout based on the
	 * signal id present in the Program Start UPID. This case happens only for the non open ended blackout feeds.
	 *
	 * @param dataManager
	 * @param aqpt
	 * @param segmentTypeId
	 * @param responseSignalType
	 * @param scte35Pt
	 * @param blackoutEvent
	 * @return
	 */
	protected BlackoutEvent getBlackoutEventForQAMStartNowProgramStart(DataManager dataManager, AcquisitionPoint aqpt, Short segmentTypeId, ResponseSignalType responseSignalType,
			SCTE35PointDescriptorType scte35Pt, BlackoutEvent blackoutEvent) {
		if((blackoutEvent == null) && aqpt.isQAMAcquisitionPoint() && SegmentType.isProgramStartSignal(segmentTypeId)){
			String signalId = responseSignalType.getSignalPointID();
			if(signalId == null) {
				byte[] upid = scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid();
				if(upid!= null){
					signalId = new String(upid);
					if(signalId.startsWith(ESAMHelper.UPID_PREFIX)){
						signalId = signalId.substring(signalId.indexOf(ESAMHelper.UPID_PREFIX) + ESAMHelper.UPID_PREFIX.length() );
					} else {
						signalId = null;//not our signal id.
					}
				}
			}
			if(signalId != null){
				blackoutEvent = dataManager.getSingleBlackoutEvent(signalId);
			}
		}
		return blackoutEvent;
	}

	protected ConfirmedPlacementOpportunity getCPOForQAMStartNowProgramStart(DataManager dataManager, AcquisitionPoint aqpt, Short segmentTypeId,
			BlackoutEvent blackoutEvent, ConfirmedPlacementOpportunity acquisitionPointCPO){
		if((acquisitionPointCPO == null) && (blackoutEvent != null) && aqpt.isQAMAcquisitionPoint() && SegmentType.isProgramStartSignal(segmentTypeId)){
			return dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());
		}
		return acquisitionPointCPO;
	}

	protected void setResponseSignalAction (final ResponseSignalType responseSignalType, final AcquisitionPoint aqpt){
		responseSignalType.setAction(((aqpt == null) || aqpt.isSccDeleteEmptyBreak()) ?
				CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
	}

}
