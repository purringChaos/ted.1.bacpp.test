package tv.blackarrow.cpp.handler;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;

public interface RequestHandler {
	
	SignalProcessingEventType parseSCCRequest(String requestXml) throws Exception;
	
	ManifestConfirmConditionEventType parseMCCRequest(String requestXml) throws Exception;
}
