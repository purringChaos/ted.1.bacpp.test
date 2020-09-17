/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hls;

import java.text.DecimalFormat;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SegmentModifyType;
import tv.blackarrow.cpp.manifest.TagSequence;
import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SpliceType;

/**
 * @author pzhang
 *
 */
public class TWCHLSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	protected static final String EMPTY_STRING = "";
	
	protected static final String DURATION_FORMAT = "#0.000";
	
	private int segmentTypeID; 
	
	public TWCHLSManifestResponseDecorator() {
		
	}
	/**
	 * TWC specific Response tags
	 * 
	 * @param signal
	 * @param cpo
	 * @param response
	 * @param requestMessage
	 * @param streamType
	 */
	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo,
			 ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception {
		response.setDataPassThrough(false);
		if(cpo.isAborted()) {
			decorateForAbort(response, signal, cpo, info);
		} else {
			decorateForConfirmation(response, signal, cpo, info, acquisitionPoint);
		}
	}

	private void decorateForAbort(ManifestResponseType response, AcquiredSignal signal, ConfirmedPlacementOpportunity cpo, ManifestInfo info) throws Exception {
		if(cpo!=null && cpo.isAborted()) {
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
		}
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence();
		TagSequence spanTag = new TagSequence();
		TagSequence lastTag = new TagSequence();
		segModify.setFirstSegment(firstTag);
		segModify.setSpanSegment(spanTag);
		segModify.setLastSegment(lastTag);
		
		// First Segment
		TagType tag = new TagType();
		tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE + ",SpliceType=" + SpliceType.LIVE_DAI.name());
		tag.setLocality("before");
		firstTag.getTag().add(tag);

		// Span Segment
		tag = new TagType();
		tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE + ",SpliceType=" + SpliceType.LIVE_DAI.name());
		spanTag.getTag().add(tag);

        // Last Segment
		tag = new TagType();
		tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE + ",SpliceType=" + SpliceType.LIVE_DAI.name());
		tag.setLocality("after");
		lastTag.getTag().add(tag);
	}

	private void decorateForConfirmation(ManifestResponseType response,	AcquiredSignal signal, ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception {
		
		
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence();
		TagSequence spanTag = new TagSequence();
		TagSequence lastTag = new TagSequence();
		segModify.setFirstSegment(firstTag);
		segModify.setSpanSegment(spanTag);
		segModify.setLastSegment(lastTag);
		
		DecimalFormat df = new DecimalFormat(DURATION_FORMAT);
	    String directives;
		String signalId = response.getSignalPointID();
		
		boolean useInbandOpportunitySignal = acquisitionPoint.isUseInbandOpportunitySignal();
		if(useInbandOpportunitySignal && segmentTypeID == Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START) {
			directives = getInBandOpportunityDirectives(signalId, info, df);
		}else {
			long largestDuration = info.getLargestDuration();
			double largestDurationInSeconds = largestDuration/(double) 1000;
			String durationInDecimals = df.format(largestDurationInSeconds);
			directives = durationInDecimals;
		}
		
		// First Segment
		TagType tag = new TagType();
		tag.setLocality("before");
		tag.setValue("#EXT-X-SIGNAL-EXIT:" + directives);
		firstTag.getTag().add(tag);
		
		// Span Segment
		tag = new TagType();
		tag.setValue("#EXT-X-SIGNAL-SPAN:${secondsFromSignal}/"+ directives);
		tag.setAdapt(true);
		spanTag.getTag().add(tag);
		
        // Last Segment
        // adding a span before the last segment as well based on lab packager results
        tag = new TagType();
        tag.setValue("#EXT-X-SIGNAL-SPAN:${secondsFromSignal}/" + directives);
        tag.setAdapt(true);
        lastTag.getTag().add(tag);
        
		tag = new TagType();
		tag.setValue("#EXT-X-SIGNAL-RETURN:" + directives);
		tag.setLocality("after");
		lastTag.getTag().add(tag);
	}

	private String getInBandOpportunityDirectives(String signalId, ManifestInfo info, DecimalFormat df) {
		DataManager dataManager = DataManagerFactory.getInstance();
		String inBandOpportunitySignalDirectives = "";
		String programId = EMPTY_STRING;
		String breakNum = EMPTY_STRING;
		String network = EMPTY_STRING;
		String duration = EMPTY_STRING;
		SegmentationDescriptor segmentationDescriptor = dataManager.getSegmentationDescriptor(signalId);
		if(segmentationDescriptor != null) {
			programId = segmentationDescriptor.getProgramId() == null ? EMPTY_STRING: segmentationDescriptor.getProgramId();
			breakNum = segmentationDescriptor.getBreakNumber() == null ? EMPTY_STRING : segmentationDescriptor.getBreakNumber().toString();
			if(segmentationDescriptor.getBreakDuration() != null) {
				double segmentDuration = segmentationDescriptor.getBreakDuration() / (double) 1000;
				duration = df.format(segmentDuration);
			}
		}
		if(info != null) {
			network =  info.getNetwork() == null ? EMPTY_STRING :info.getNetwork();
		}
		StringBuilder directive = new StringBuilder();
		directive.append(duration).append(",PROGRAM=").append(programId).append(",BREAK=")
				.append(breakNum).append(",NETWORK=").append(network);
		inBandOpportunitySignalDirectives = directive.toString();
		return inBandOpportunitySignalDirectives;
	}
	/**
	 * @return the segmentTypeID
	 */
	public int getSegmentTypeID() {
		return segmentTypeID;
	}
	/**
	 * @param segmentTypeID the segmentTypeID to set
	 */
	public void setSegmentTypeID(int segmentTypeID) {
		this.segmentTypeID = segmentTypeID;
	}
}
