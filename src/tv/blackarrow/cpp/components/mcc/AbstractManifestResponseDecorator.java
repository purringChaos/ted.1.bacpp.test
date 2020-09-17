/**
 * 
 */
package tv.blackarrow.cpp.components.mcc;

import java.util.List;

import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

/**
 * Manifest response decorator interface for different formats 
 * @author pzhang
 *
 */
public abstract class AbstractManifestResponseDecorator implements ManifestResponseDecorator {

	protected static final String ALT_CONTENT = "ALT_CONTENT";
	
	@Override
	public void decorateManifestResponse(ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal, final MediaLedger cpo, ManifestInfo info,
			AcquisitionPoint apqt, Short segmentTypeId) throws Exception {

	}
	@Override
	public void decorateManifestResponse(ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo, ManifestInfo info,
			AcquisitionPoint apqt) throws Exception {

	}
	
	protected void setUtcPointInAcquiredSignal(ManifestResponseType response, AcquisitionPointInfoType acqSignal) {
		UTCPointDescriptorType utcPointDescriptorType = getUTCPointDescriptorType(
				response.getOtherAttributes().get(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME));
		if (utcPointDescriptorType != null) {
			acqSignal.setUTCPoint(utcPointDescriptorType);
		}
	}
	
	protected UTCPointDescriptorType getUTCPointDescriptorType(String utcTime) {
		UTCPointDescriptorType utc = null;
		if (utcTime != null) {
			utc = new UTCPointDescriptorType();
			utc.setUtcPoint(SCCResponseUtil.generateUTC(Long.valueOf(utcTime)));
		}
		return utc;
	}
	
	protected void adjustSignalTimeOffset(StreamTimesType streamTimesType, AcquisitionPoint aqpt) {
		long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset()
				: ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);
		if (streamTimesType != null) {
			Scte35BinaryUtil.adjustPtsTimeInStream(streamTimesType, signalTimeOffset);
		}
	}
}
