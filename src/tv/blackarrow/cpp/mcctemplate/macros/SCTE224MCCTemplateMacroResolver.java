/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate.macros;

import java.text.DecimalFormat;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.Duration;

import org.apache.commons.lang.StringUtils;

import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author shwetanks
 *
 */
public class SCTE224MCCTemplateMacroResolver extends BaseMCCTemplateMacroResolver {

	private static final String EMPTY_STRING = "";
	private MediaLedger mediaLedger;

	public SCTE224MCCTemplateMacroResolver(final ManifestResponseType response, final AcquiredSignal signal, final ManifestInfo info, final AcquisitionPoint acquisitionPoint,
			MediaLedger mediaLedger, SegmentType requestSegmentTypeId) {
		super(response, signal, info, acquisitionPoint);
		this.mediaLedger = mediaLedger;
		buildReplacements();
	}

	@Override
	protected Object deriveSegmentationUpid(final ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		/*
		 * Send the original UPID for all signal that were recieved at SCC phase.
		 */
		macroValue = mediaLedger.getMediaTransactions().get(0).getOriginalUpid();
		
		//Fill the same that is coming in request
		if (StringUtils.isBlank((String) macroValue)) {
			byte[] upidBinary = segmentationDescriptorType.getUpid();
			macroValue = new HexBinaryAdapter().marshal(upidBinary);
		}
		
		return macroValue;
	}
	 
	@Override
	protected Object getSignalId(ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		return mediaLedger.getSignalIdWithTerritoryCounter(mediaLedger.getSignalId());
	}

	/*
	 * Project Khaleesi /  PKH-1161
	 * SCTE-224, Template and Custom both should not try to set 0 if not present
	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.mcctemplate.macros.BaseMCCTemplateMacroResolver#calculateDurationInSeconds(tv.blackarrow.cpp.components.mcc.ManifestInfo, javax.xml.datatype.Duration)
	 */
	@Override
	protected Object calculateDurationInSeconds(ManifestInfo info, Duration duration) {
		if (duration == null) {
			return EMPTY_STRING;
		}
		Object macroValue = (convertDurationInMilliSeconds(duration)) / 1000;
		DecimalFormat df = new DecimalFormat(DURATION_FORMAT);
		macroValue = df.format(macroValue);

		return macroValue;
	}

}
