package tv.blackarrow.cpp.handler;

import java.util.List;
import java.util.Map;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;

public interface ResponseHandler{
	
	String generateSCCResponse(Object signalProcessingNotificationType);
	
	String generateSCCResponse(SignalProcessingNotificationType notification, Map<String, I03ResponseModelDelta> alternates);
	 
	String generateSCCResponse(tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType notification, Map<String, I03ResponseModelDelta> alternates);
	
	String generateMCCResponse(ManifestConfirmConditionNotificationType notification);
	
	String generateHSSSparseTrack(final AcquisitionPointInfoType acqSignal);
}
