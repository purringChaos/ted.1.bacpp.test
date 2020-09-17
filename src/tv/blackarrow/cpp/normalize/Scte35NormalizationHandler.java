package tv.blackarrow.cpp.normalize;

import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;

/**
 * Scte35NormalizationHandler defines the interface, and all normalization handler 
 * should implement normalizeSignal method
 * 
 * @author jwang
 *
 */
public interface Scte35NormalizationHandler {
	
	/**
	 * normalize incoming SCTE 35 binary data
	 * 
	 * @param event
	 * @return normalized SCTE 35 binary defined inside SignalProcessingNotification 
	 */
	SignalProcessingNotificationType normalizeSignal(final SignalProcessingEventType event);
}
