package tv.blackarrow.cpp.components;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.SegmentType;

public abstract class BlackoutDecisionBase {

	private static final Logger LOGGER = LogManager.getLogger(BlackoutDecisionBase.class);
	protected static final String MULE_CONTEXT_PROPERTY_HAS_EVENT_END_TIME_CHANGED = "hasBlackoutEndTimeChanged";
	protected static final String MULE_CONTEXT_PROPERTY_IS_END_NOW = "isEndNow";
	protected static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();


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
		if(blackoutEvent == null && aqpt.isQAMAcquisitionPoint() && SegmentType.isProgramStartSignal(segmentTypeId)){
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
		if(acquisitionPointCPO == null && blackoutEvent != null && aqpt.isQAMAcquisitionPoint() && SegmentType.isProgramStartSignal(segmentTypeId)){
			return dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());
		}
		return acquisitionPointCPO;
	}

	protected void setResponseSignalAction (final ResponseSignalType responseSignalType, final AcquisitionPoint aqpt){
		responseSignalType.setAction(aqpt == null || aqpt.isSccDeleteEmptyBreak() ?
				CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
	}

	protected boolean isQamApQualify4Blackout(AcquisitionPoint aqpt,
			ConfirmedPlacementOpportunity cpo, BlackoutEvent event,
			Map<String, I03ResponseModelDelta> alternateContents,
			HashMap<String, Map<QName, String>> attributes,
			ResponseSignalType responseSignalType, Short segmentTypeId) {
		// check if acquisition point is QAM and involved in this blackout.
		if (aqpt.isQAMAcquisitionPoint()) {
			I03ResponseModelDelta i03ResponseModelDelta = null;
			try {
				i03ResponseModelDelta = BlackoutQAMHandler.retrieveZoneInfo(event, cpo, attributes.get(responseSignalType.getAcquisitionSignalID()));
				if (i03ResponseModelDelta != null && !i03ResponseModelDelta.getAlternateContentIDList().isEmpty()) {
					if (!SegmentType.isProgramEndSignal(segmentTypeId)) {
						alternateContents.put(cpo.getSignalId(), i03ResponseModelDelta);
					}
				} else {
					LOGGER.info("No restrications found for blackout " + event.getEventId() +"("+event.getSignalId()+")"+ " on QAM AP " + aqpt.getAcquisitionPointIdentity());
					Map<QName, String> attri = attributes.get(responseSignalType.getAcquisitionSignalID());
					String zoneIdentity = "";
					if (attri != null && !attri.isEmpty()) {
						zoneIdentity = attri.get(new QName(CppConstants.SERVINCE_ZONE_IDENTITY_ATTRIBUTE));
					}
					LOGGER.warn("@zoneIdentity " + zoneIdentity + " is not recognized.  Cannot determine whether alternate content should be used.");
				}
			} catch (Exception e) {
				LOGGER.error("Error while retrieve Zone info ", e);
			}

			// return no-op/delete if no alternate content available, and no
			// nothing for this blackout.
			if (i03ResponseModelDelta == null || i03ResponseModelDelta.getAlternateContentIDList().isEmpty()) {
				return false;
			}
		}

		return true;
	}
	
	protected void saveActualStartTimeInAqCPO(ConfirmedPlacementOpportunity acquisitionPointConfirmedBlackout, final BlackoutEvent blackoutEvent, long actualUtcStartTime, AcquisitionPoint aqpt) {
		if (acquisitionPointConfirmedBlackout != null) {
			acquisitionPointConfirmedBlackout.setActualUtcStartTime(actualUtcStartTime);
			if(aqpt!=null){
				acquisitionPointConfirmedBlackout.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
			}
			DATA_MANAGER.putConfirmedBlackout(acquisitionPointConfirmedBlackout);
		}
	}
	
	public static ConfirmedPlacementOpportunity saveActualUtcEndInAqCPOp(AcquisitionPoint aqpt, DataManager dataManager, final BlackoutEvent blackoutEvent, String message, 
			long actualUtcStopTime) {
		ConfirmedPlacementOpportunity confirmedBlackoutCPO = null;
		if(aqpt!=null){
			confirmedBlackoutCPO = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), blackoutEvent.getSignalId());
		} else {
			confirmedBlackoutCPO = dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());
		}

		if (confirmedBlackoutCPO != null) {
			confirmedBlackoutCPO.setActualUtcStopTime(actualUtcStopTime);
			if(aqpt!=null){
				confirmedBlackoutCPO.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
			}
			dataManager.putConfirmedBlackout(confirmedBlackoutCPO);		
		}
		return confirmedBlackoutCPO;
	}


}
