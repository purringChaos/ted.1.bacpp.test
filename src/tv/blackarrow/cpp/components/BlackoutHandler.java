package tv.blackarrow.cpp.components;

import java.util.HashMap;
import java.util.List;

import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;

public class BlackoutHandler {

	
	// create and add addition response signals for combined segmentations.
	// Currently we only support PROGRAM_END and PROGRAM_START signals as combined signal, and all others will be ignored.
	// order of PROGRAM_END and PROGRAM_START doesn't matter in the request, we need to re-order them before processing. 
	public static void processAndOrderResponseSignals(MuleEventContext context, final SignalProcessingNotificationType notification, 
												List<SegmentationDescriptorType> segmentationDescriptorInfo,
												final SignalProcessingEventType.AcquiredSignal signal) {
		// if not a combined segmentation, do nothing.
		if(segmentationDescriptorInfo.size() !=2){
			return;
		}
		
		SegmentationDescriptorType seg1 = null;
		SegmentationDescriptorType seg2 = null;
		
		if(SegmentType.valueOf(segmentationDescriptorInfo.get(0).getSegmentTypeId()) == SegmentType.PROGRAM_START 
				&& SegmentType.valueOf(segmentationDescriptorInfo.get(1).getSegmentTypeId()) == SegmentType.PROGRAM_END){
			seg1 = segmentationDescriptorInfo.get(1);
			seg2 = segmentationDescriptorInfo.get(0);
			
		}else if(SegmentType.valueOf(segmentationDescriptorInfo.get(0).getSegmentTypeId()) == SegmentType.PROGRAM_END 
				&& SegmentType.valueOf(segmentationDescriptorInfo.get(1).getSegmentTypeId()) == SegmentType.PROGRAM_START){
			seg1 = segmentationDescriptorInfo.get(0);
			seg2 = segmentationDescriptorInfo.get(1);
		}else{
			// not PROGRAM_END and PROGRAM_START combine signal, do nothing.
			return;
		}
		
		context.getMessage().setOutboundProperty(CppConstants.IS_COMBINED_SIGNAL, Boolean.TRUE);
		
		// response signal created at SCCRequestComponent
		ResponseSignalType type1 = notification.getResponseSignal().get(0);
		type1.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().clear();
		type1.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(seg1);
		
		ResponseSignalType type2 = CppUtil.addNewResponseSignal(notification, signal);
		if(type2.getSCTE35PointDescriptor() == null){
			type2.setSCTE35PointDescriptor(CppUtil.getCopy(type1.getSCTE35PointDescriptor()));
		}
		type2.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().clear();
		type2.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(seg2);
	}


	public static void handleContentIdSignal(AcquisitionPoint aqpt, SCTE35PointDescriptorType scte35Pt,	ResponseSignalType responseSignalType,final HashMap<String, ConfirmedPlacementOpportunity> decisions) {
		String signalId = responseSignalType.getSignalPointID();
		if(signalId == null) {
			byte[] upid = scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid();
			if(upid!= null){
				signalId = new String(upid);
				if(signalId.startsWith(ESAMHelper.UPID_PREFIX)){
					signalId = signalId.substring(signalId.indexOf(ESAMHelper.UPID_PREFIX) + ESAMHelper.UPID_PREFIX.length() );
				}
			}
		}
		
		
		if(signalId == null){
			if(aqpt.getInBandContentIdConfiguredValue() == SignalHandlingConfiguration.PRESERVE){
				responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
			}else{
				responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
			}
			return;
		}
		DataManager dataManager = DataManagerFactory.getInstance();
		ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), signalId);

		// signal that from third party.
		if(cpo == null){
			if(aqpt.getInBandContentIdConfiguredValue() == SignalHandlingConfiguration.PRESERVE){
				responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
			}else{
				responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
			}
			return;
		}
		// Put the CPO with the new SignalId
		SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
		if(!CppConfigurationBean.getInstance().isSendTerritoryUpdateConfirmation() && cpo.getTerritoryUpdateSignalId() != null 
				&& !cpo.getTerritoryUpdateSignalId().equals(cpo.getSignalId()) && 
						!cursor.getLastConfirmedBlackoutSignalId().equals(cpo.getTerritoryUpdateSignalId())){
			dataManager.putConfirmedBlackout(cpo,cpo.getTerritoryUpdateSignalId());
			responseSignalType.setAction(ResponseSignalAction.REPLACE.toString());
			responseSignalType.setSignalPointID(cpo.getTerritoryUpdateSignalId());
			decisions.put(cpo.getTerritoryUpdateSignalId(), cpo);
			if(cursor!=null){
				cursor.setLastConfirmedBlackoutSignalId(cpo.getTerritoryUpdateSignalId());
				new BlackoutSearcher().updateSignalProcessorCursor(cursor);
			}
			return;
		}
		// blackout that already ended.
		if(cpo.isAborted() || cpo.isProgramEnded()){
			responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
			return;
		}
		
		//Preserve the content id signal that generated by POIS if it is still effective
		responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
	}
	

}
