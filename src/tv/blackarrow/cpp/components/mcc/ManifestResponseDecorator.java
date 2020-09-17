/**
 * 
 */
package tv.blackarrow.cpp.components.mcc;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.scte224.MediaLedger;

/**
 * Manifest response decorator interface for different formats 
 * @author pzhang
 *
 */
public interface ManifestResponseDecorator {
	
	String HLS_SIGNAL_ABORT_TAG_VALUE = "#EXT-X-SIGNAL-ABORT";
	String HLS_SIGNAL_ABORT_FIRST_SEGMENT_TAG_LOCALITY = "before";
	Boolean HLS_SIGNAL_ABORT_FIRST_AND_SPAN_SEGMENT_ADAPT_VALUE = Boolean.TRUE;
	
	String HSS_SIGNAL_STREAM_TIME_ACTION_TYPE = "Action";
	String HSS_SIGNAL_ABORT_STREAM_TIME_VALUE = "DAI_ABORT";
	String HSS_BLACKOUT_SIGNAL_ABORT_STREAM_TIME_VALUE = "ALTCON_ABORT";
	
	/**
	 * decorate manifest response with response information. each response decorator may decorator with different elements for the response.
	 * some of the response include HLS, HSS responses, Blackout, and Placement Opportunities, 
	 * 
	 * @param signal
	 * @param response
	 * @param apqt 
	 */
	public void decorateManifestResponse(ManifestResponseType response, 
			final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final ConfirmedPlacementOpportunity cpo,
			ManifestInfo info, AcquisitionPoint apqt)
			throws Exception;
	
	public void decorateManifestResponse(ManifestResponseType response, 
			final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final MediaLedger cpo,
			ManifestInfo info, AcquisitionPoint apqt, Short segmentTypeId)
			throws Exception;

}
