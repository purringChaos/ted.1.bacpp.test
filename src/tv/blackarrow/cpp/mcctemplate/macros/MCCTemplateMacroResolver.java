/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate.macros;

import java.text.DecimalFormat;
import java.util.List;

import javax.xml.datatype.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.BlackoutResponseDurationHandler;
import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;;

/**
 * @author shwetanks
 *
 */
public class MCCTemplateMacroResolver extends BaseMCCTemplateMacroResolver {

	private final static Logger LOGGER = LogManager.getLogger(MCCTemplateMacroResolver.class);
	
	protected static final String INBAND_DURATION_FORMAT = "#0.000";
	private short segmentTypeID = 0;
	
	public MCCTemplateMacroResolver(final ManifestResponseType response, final AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo, final ManifestInfo info,
			final AcquisitionPoint acquisitionPoint, short segmentTypeID) {
		super(response, signal, info, acquisitionPoint);
		boolean canSetDuration = BlackoutResponseDurationHandler.canSetDurationInMccResponse(cpo, acquisitionPoint, segmentationDescriptorType);
		this.segmentTypeID = segmentTypeID;
		buildReplacements(canSetDuration);
	}

	@Override
	protected Object calculateDurationInSeconds(ManifestInfo info, Duration duration) {
		if(isInBandOpportunitySignalWithPOStartSegment()) {
			Object macroValue = EMPTY_STRING;
			if (info != null && info.getSignalId() != null) {
				SegmentationDescriptor descriptor = getSegmentationDescriptor(info.getSignalId());
				if (descriptor != null  && descriptor.getBreakDuration() != null) {
					double breakDurationInSeconds = descriptor.getBreakDuration() / (double) 1000;
					DecimalFormat df = new DecimalFormat(INBAND_DURATION_FORMAT);
					macroValue = df.format(breakDurationInSeconds);
				} 
			}
			return macroValue;
		}else {
			return super.calculateDurationInSeconds(info, duration);
		}
		
	}
	@Override
	protected Object getSegmentationNum(final ManifestInfo info) {
		if(isInBandOpportunitySignalWithPOStartSegment()) {
			Object macroValue = EMPTY_STRING;
			if (info != null && info.getSignalId() != null) {
				SegmentationDescriptor descriptor = getSegmentationDescriptor(info.getSignalId());
				if (descriptor != null  && descriptor.getBreakNumber() != null) {
					macroValue = descriptor.getBreakNumber();
				} 
			}
			return macroValue;
		}else {
			return super.getSegmentationNum(info);
		}
	}
	
	
	protected Object deriveSegmentationUpidProgramId(final ManifestInfo info, Object macroValue, SegmentationDescriptorType segmentationDescriptorType,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		if(isInBandOpportunitySignalWithPOStartSegment()) {
			SegmentationDescriptor descriptor = null;
			if (info != null && info.getSignalId() != null) {
				descriptor = getSegmentationDescriptor(info.getSignalId());
			}
			if (descriptor != null && descriptor.getProgramId() != null) {
				macroValue = descriptor.getProgramId();
			}
			return macroValue;
		}else {
			return super.deriveSegmentationUpidProgramId(info, macroValue, segmentationDescriptorType, segmentationDescriptorInfo);
		}
	}
		
	@Override
	protected Object getSegmentationCount(final ManifestInfo info) {
		if(isInBandOpportunitySignalWithPOStartSegment()) {
			Object macroValue = EMPTY_STRING;
			if (info != null && info.getSignalId() != null) {
				SegmentationDescriptor descriptor = getSegmentationDescriptor(info.getSignalId());
				if (descriptor != null  && descriptor.getTotalBreak() != null) {
					macroValue = descriptor.getTotalBreak();
				} 
			}
			return macroValue;
		}else {
			return super.getSegmentationCount(info);
		}
	}
	
	private boolean isInBandOpportunitySignalWithPOStartSegment() {
		return acquisitionPoint.isUseInbandOpportunitySignal() && segmentTypeID == Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START;
	}
}
