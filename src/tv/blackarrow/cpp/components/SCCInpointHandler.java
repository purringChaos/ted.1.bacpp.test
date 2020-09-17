package tv.blackarrow.cpp.components;


import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SCCResponseUtil;


public class SCCInpointHandler {
	
	private static Logger LOGGER = LogManager.getLogger(SCCInpointHandler.class);

	public static void processInPointResponse(String inPointSignalId,
			SignalProcessingNotificationType notification,
			ResponseSignalType responseSignalType,
			String ptsTimePlusOffsetInBinary,
			String pts_adjustment,
			List<ResponseSignalType> additionalResponseSignalTypes, AcquisitionPoint aqpt) {
		
		if(inPointSignalId == null){
			responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			return;
		}
		
		String orignalSignal = DataManagerFactory.getInstance().getInPointsSignal(inPointSignalId);
		
		if(orignalSignal == null){
			responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			return;
		}
		
		ConfirmedPlacementOpportunity  cpo = DataManagerFactory.getInstance().getConfirmedPlacementOpportunity(orignalSignal);
		if(cpo == null || cpo.isAborted()){
			if(LOGGER.isInfoEnabled()){
				LOGGER.debug(()->"Unable to find origianl CPO with signal id "+ orignalSignal + " for in-point signal "+ inPointSignalId);
			}
			responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			return;
		}
		
		// add signal to delete original confirm signal
		ResponseSignalType deleteOrignialSignal = SCCResponseUtil.generateResponseSignal(cpo.getSignalId(), responseSignalType.getAcquisitionPointIdentity(), 
				CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE, responseSignalType.getUTCPoint(), ptsTimePlusOffsetInBinary, pts_adjustment, cpo.getLongestDuration(), 
				AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt), SCCResponseUtil.getIdForCountDownQueueCheck(responseSignalType), 
											SCCResponseUtil.isBinary(responseSignalType));
		notification.getResponseSignal().add(deleteOrignialSignal);
		
		SCCResponseUtil.generatedAndAddSCTE35PointDescriptor(responseSignalType, inPointSignalId, ptsTimePlusOffsetInBinary,pts_adjustment, 0, 
				AcquisitionPoint.getDefaultSegmentTypeForAdEnd(aqpt), SCCResponseUtil.getIdForCountDownQueueCheck(responseSignalType), SCCResponseUtil.isBinary(responseSignalType));
		
	}
}
