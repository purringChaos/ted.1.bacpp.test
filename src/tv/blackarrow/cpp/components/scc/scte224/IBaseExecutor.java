package tv.blackarrow.cpp.components.scc.scte224;

import tv.blackarrow.cpp.components.scc.scte224.response.common.IBaseResponseProcessor;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.SegmentType;

public interface IBaseExecutor {
	public IBaseResponseProcessor getResponseProcessor(SegmentType segmentTypeId, AcquisitionPoint aqpt, AcquiredSignal acquiredSignal);

}
