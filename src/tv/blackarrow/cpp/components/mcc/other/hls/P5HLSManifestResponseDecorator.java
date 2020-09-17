/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hls;

import java.text.DecimalFormat;

import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SegmentModifyType;
import tv.blackarrow.cpp.manifest.TagSequence;
import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * manifest response decorator for P5HLS
 * refactored from ManifestEventComponent  decorateP5HLSManifestResponse
 * 
 * @author pzhang
 *
 */
public class P5HLSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	/**
	 * P5 specific Response tags
	 * 
	 * @param signal
	 * @param cpo
	 * @param response
	 * @param requestMessage
	 * @param streamType
	 */
	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal,
				final ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception {
		response.setDataPassThrough(false);
		
		Short segmentType = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId();
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence();
		segModify.setFirstSegment(firstTag);
		
		if(SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId() == segmentType||
				SegmentType.PROGRAM_START.getSegmentTypeId() == segmentType){
			
			TagSequence spanTag = new TagSequence();
			TagSequence lastTag = new TagSequence();
			segModify.setSpanSegment(spanTag);
			segModify.setLastSegment(lastTag);
						
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(info.getLargestDuration()));
			
			double largestDurationInSeconds = info.getLargestDuration()/(double) 1000;
			DecimalFormat df = new DecimalFormat("#.000");
		    String durationInDecimals = df.format(largestDurationInSeconds);

			// Segment Modify for HLS
			String value = "SignalId=" + info.getSignalId()+",Duration=" + durationInDecimals;

			// First Segment
			TagType tag = new TagType();
			tag.setLocality("before");
			tag.setValue("#EXT-X-SIGNAL-START:"+ value);
			firstTag.getTag().add(tag);

			// Span Segment
			tag = new TagType();
			tag.setValue("#EXT-X-SIGNAL-SPAN:" + value+ ",TimeFromSignalStart=${timeFromSignal}");
			tag.setAdapt(true);
			spanTag.getTag().add(tag);

			// Last Segment
			tag = new TagType();
			tag.setValue("#EXT-X-SIGNAL-SPAN:" + value + ",TimeFromSignalStart=${timeFromSignal}");
			tag.setAdapt(true);
			lastTag.getTag().add(tag);
		}else{
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(0));
			
			// Segment Modify for HLS
			String value = "SignalId=" + response.getSignalPointID() +",Duration=" + 0;
			// Only First Segment
			TagType tag = new TagType();
			tag.setLocality("before");
			tag.setValue("#EXT-X-SIGNAL-END:"+ value);
			firstTag.getTag().add(tag);
		}
			
	}

}
