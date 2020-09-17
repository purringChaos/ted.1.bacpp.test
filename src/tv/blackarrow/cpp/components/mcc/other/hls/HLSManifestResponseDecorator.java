/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hls;

import java.net.URLEncoder;
import java.util.Set;

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

/**
 * @author pzhang
 *
 */
public class HLSManifestResponseDecorator extends AbstractManifestResponseDecorator {
	
	/**
	 *  HLS manifest response
	 * 
	 * @param signal
	 * @param response
	 */
	@Override
	public void decorateManifestResponse(final ManifestResponseType response, 
			final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final ConfirmedPlacementOpportunity cpo,
			ManifestInfo info, AcquisitionPoint acquisitionPoint)
			throws Exception {
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence();
		TagSequence spanTag = new TagSequence();
		TagSequence lastTag = new TagSequence();
		segModify.setFirstSegment(firstTag);
		segModify.setSpanSegment(spanTag);
		segModify.setLastSegment(lastTag);
		
		if(cpo!=null && cpo.isAborted()) {
			long poStartUTCPoint = cpo.getUtcSignalTime() + cpo.getSignalTimeOffset();
			long largestPODuration = info.getLargestDuration();
			// long abortTime = cpo.getAbortTime();
			// As per the discussions with PD. The time received in the MCC request should always be considered as Abort Time.
			long abortTime = cpo.getAbortTime();
			if(signal.getUTCPoint() != null ) {
				abortTime = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime().getTime();
			}
			long duration = (poStartUTCPoint + largestPODuration) - abortTime;
			if(duration<0) {
				duration = 0;
			}
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		}

		String value = "SignalId="
				+ info.getSignalId()
				+ ",Feed="
				+ URLEncoder.encode(info.getFeed(), "UTF-8")
				+ ",Network="
				+ URLEncoder.encode(info.getNetwork(), "UTF-8")
				+ ",AP="
				+ URLEncoder.encode(info.getApid(), "UTF-8")
				+ ",utc="
				+ info.getUtc()
				+ ",timeFromSignal=${timeFromSignal},streamID=${streamID},Duration="
				+ URLEncoder.encode(response.getDuration().toString(), "UTF-8");

		// First Segment
		TagType tag = new TagType();
		tag.setAdapt(true);
		tag.setValue("#EXT-X-SPLICE-EXIT:" + value);
		firstTag.getTag().add(tag);
		
		//If the signal is aborted add this additional tag.
		if(cpo != null && cpo.isAborted()) {
			tag = new TagType();
			tag.setLocality(HLS_SIGNAL_ABORT_FIRST_SEGMENT_TAG_LOCALITY);
			tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE);
			firstTag.getTag().add(tag);
		}
		
		tag = new TagType();
		tag.setValue("#EXT-X-DISCONTINUITY");
		firstTag.getTag().add(tag);

		// Span Segment
		tag = new TagType();
		tag.setValue("#EXT-X-SPLICE-SPAN:" + value);
		tag.setAdapt(true);
		spanTag.getTag().add(tag);

		if (cpo != null) {
			
			//If the signal is aborted add this additional tag.
			if(cpo.isAborted()) {
				tag = new TagType();
				tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE);
				tag.setAdapt(HLS_SIGNAL_ABORT_FIRST_AND_SPAN_SEGMENT_ADAPT_VALUE);
				spanTag.getTag().add(tag);
			}
			
			Set<Integer> placementsDurationsInMilliseconds = cpo.getPlacementsDurationsInMilliseconds();
			if (placementsDurationsInMilliseconds != null) {
				placementsDurationsInMilliseconds.remove(new Integer(0));
				if (placementsDurationsInMilliseconds.size() > 1) {
					// we have multiple duration across all ad zones for
					// this particular POIS decision
					// this is the special case we need to append
					// #EXT-X-DISCONTINUITY tag with locality of "after"
					tag = new TagType();
					tag.setValue("#EXT-X-DISCONTINUITY");
					tag.setLocality("after");
					spanTag.getTag().add(tag);
				}
			}
		}

		// Last Segment
        // adding a span before the last segment as well based on lab packager results
        tag = new TagType();
        tag.setValue("#EXT-X-SPLICE-SPAN:" + value);
        tag.setAdapt(true);
        lastTag.getTag().add(tag);
        
        tag = new TagType();
		tag.setAdapt(true);
		tag.setValue("#EXT-X-SPLICE-RETURN:" + value);
		tag.setLocality("after");
		lastTag.getTag().add(tag);
		
		//If the signal is aborted add this additional tag.
		if(cpo != null && cpo.isAborted()) {
			tag = new TagType();
			tag.setValue(HLS_SIGNAL_ABORT_TAG_VALUE);
			tag.setAdapt(HLS_SIGNAL_ABORT_FIRST_AND_SPAN_SEGMENT_ADAPT_VALUE);
			lastTag.getTag().add(tag);
		}

		tag = new TagType();
		tag.setValue("#EXT-X-DISCONTINUITY");
		tag.setLocality("after");
		lastTag.getTag().add(tag);
	}

}
