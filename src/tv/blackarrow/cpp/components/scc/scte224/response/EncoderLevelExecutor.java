package tv.blackarrow.cpp.components.scc.scte224.response;

import tv.blackarrow.cpp.components.scc.scte224.response.common.IBaseResponseProcessor;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.SegmentType;

public class EncoderLevelExecutor extends SCCBaseExecutor {

	@Override
	public IBaseResponseProcessor getResponseProcessor(SegmentType segmentTypeId, AcquisitionPoint aqpt, AcquiredSignal acquiredSignal) {
		IBaseResponseProcessor response = null;
		/*	int numberOfSegmentationDescriptor = acquiredSignal.getSCTE35PointDescriptor() != null && acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null
					? acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().size()
					: 0;*/
		//Disney Encoder Level Are Strictly Inband-combined Signal only. Disney is never supposed to send single signal on encoder level.
		//Only Combined Signal Was Implemented/Tested for this phase.
		//if (numberOfSegmentationDescriptor > 1) {
		//Dated:Phase2 Implemetation:
		//Not changing this. Don't why, but in Disney Phase1 Encoder level(individual/combined both) were handled from CombinedSignalResponseProcessor. May be no one looked for different response
		//Yaminee: looks to me customer never wanted/tested individual ones.
			response = new tv.blackarrow.cpp.components.scc.scte224.response.encoder.ip.CombinedSignalResponseProcessor();
		/*	} else {
		
				//XMP Encoder Levels Are Strictly OOB + Any other than above goes to
				response = new tv.blackarrow.cpp.components.scc.scte224.response.common.DeleteResponseProcessor();
			}*/
		return response;
	}

}
