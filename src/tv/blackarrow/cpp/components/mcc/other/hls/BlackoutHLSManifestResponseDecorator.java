/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hls;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import tv.blackarrow.cpp.components.BlackoutResponseDurationHandler;
import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SegmentModifyType;
import tv.blackarrow.cpp.manifest.TagSequence;
import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.JavaxUtil;

/**
 * @author pzhang
 *
 */
public class BlackoutHLSManifestResponseDecorator extends AbstractManifestResponseDecorator{

	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint) throws Exception {
		
		int segmentTypeID = -1;
		if(Boolean.TRUE.equals(signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator())
				|| signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId() == null) {
			segmentTypeID = Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT;
		} else {
			segmentTypeID = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId();
		}
		
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_PLANNED:
			decorateProgramStartManifestResponse(signal, response, cpo, info, acquisitionPoint);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			decorateContentIdentityManifestResponse(signal, response, cpo, info, acquisitionPoint);
			break;
		default:
				throw new RuntimeException("unsupport segment type id " + segmentTypeID);
		}
	}

	/**
	 * add tags for signal Program Start Manifest response
	 * There are three segments
	 * @param signal
	 * @param response
	 * @param info 
	 * @param cpo 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, 
			final ConfirmedPlacementOpportunity cpo, final ManifestInfo info, final AcquisitionPoint acquisitionPoint) throws Exception {
		
		String signalId = response.getSignalPointID();
		String spliceType = findSpliceType(signalId);
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence(); 
		TagSequence spanTag = new TagSequence(); 
		TagSequence lastTag = new TagSequence(); 
		segModify.setFirstSegment(firstTag);
		segModify.setSpanSegment(spanTag);
		segModify.setLastSegment(lastTag);
		
		String signalIdHex = signalId;
		
		// Segment Modify for HLS
		Date utcDate = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String utc = sdf.format(utcDate);
		
		String apid = signal.getAcquisitionPointIdentity();
		String feed = info.getFeed();
		String network = info.getNetwork();

		StringBuilder extraInfo = new StringBuilder();
		extraInfo.append("SignalId=").append(signalIdHex).append(",SpliceType=").append(spliceType).append(",feed=")
		.append(URLEncoder.encode(feed,"UTF-8")).append(",network=").append(URLEncoder.encode(network,"UTF-8"))
		.append(",ap=").append(URLEncoder.encode(apid,"UTF-8")).append(",utc=")
		.append(utc).append(",timeFromSignal=${timeFromSignal},streamID=${streamID}");
		
		// First Segment
		TagType tag = new TagType();
		tag.setAdapt(true);
		tag.setValue("#EXT-X-SPLICE-EXIT:" + extraInfo.toString());
		firstTag.getTag().add(tag);
		tag = new TagType();
		tag.setValue("#EXT-X-DISCONTINUITY");
		firstTag.getTag().add(tag);
		
		// Span Segment
		tag = new TagType();
		tag.setValue("#EXT-X-SPLICE-SPAN:" + extraInfo.toString());
		tag.setAdapt(true);
		spanTag.getTag().add(tag);
		
		// Last Segment
		tag = new TagType();
		tag.setAdapt(true);
		tag.setValue("#EXT-X-SPLICE-RETURN:" + extraInfo.toString());
		tag.setLocality("after");
		lastTag.getTag().add(tag);

		tag = new TagType();
		tag.setValue("#EXT-X-DISCONTINUITY");
		tag.setLocality("after");
		lastTag.getTag().add(tag);
	}

	private String findSpliceType(String signalId) {
		BlackoutEvent event = DataManagerFactory.getInstance().getSingleBlackoutEvent(signalId);
		if(event==null){
		    return "ALT_CONTENT";
		}
		return event.getEventTypeName();
	}
	

	/**
	 * decorate the response for the signal of Content Identification
	 * The response will only include one Segment (SegmentModifiy)
	 * @param signal
	 * @param response
	 * @param info 
	 * @param cpo 
	 * @throws Exception
	 */
	private void decorateContentIdentityManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, 
			final ConfirmedPlacementOpportunity cpo, final ManifestInfo info, final AcquisitionPoint acquisitionPoint) throws Exception {
		
		String signalId = response.getSignalPointID();
		String spliceType = findSpliceType(signalId);
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		TagSequence firstTag = new TagSequence(); 
		segModify.setFirstSegment(firstTag);
		String signalIdHex = signalId;
		
		// Segment Modify for HLS
		
		Date utcDate = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String utc = sdf.format(utcDate);
		
		String apid = signal.getAcquisitionPointIdentity();
		String feed = info.getFeed();
		String network = info.getNetwork();
		
		StringBuilder extraInfo = new StringBuilder();
		extraInfo.append("SignalId=").append(signalIdHex).append(",SpliceType=").append(spliceType).append(",feed=")
		.append(URLEncoder.encode(feed,"UTF-8")).append(",network=").append(URLEncoder.encode(network,"UTF-8"))
		.append(",ap=").append(URLEncoder.encode(apid,"UTF-8")).append(",utc=")
		.append(utc).append(",timeFromSignal=${timeFromSignal},streamID=${streamID}");
		
		// First Segment
		TagType tag = new TagType();
		tag.setAdapt(true);
		tag.setValue("#EXT-X-SPLICE-SPAN:" + extraInfo.toString());
		firstTag.getTag().add(tag);
		
		SCTE35PointDescriptorType scte35PointDescriptor = signal.getSCTE35PointDescriptor();
		List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35PointDescriptor.getSegmentationDescriptorInfo();
		SegmentationDescriptorType segmentationDescriptorType = null;
		if (segmentationDescriptorInfo != null) {
			segmentationDescriptorType = segmentationDescriptorInfo.get(0);
		}
		final boolean canSetDurationInMCCResponse = BlackoutResponseDurationHandler.canSetDurationInMccResponse(cpo, acquisitionPoint, segmentationDescriptorType);
		
		//Override duration to Blackout stop time - requested utc time.
		if(cpo != null) {
			BlackoutEvent event = DataManagerFactory.getInstance().getSingleBlackoutEvent(signalId);
			if(event!=null) {
				long duration = BlackoutEvent.getActualBlackoutStopTime(cpo, event) - utcDate.getTime();
				if(duration < 0) {
					duration = 0;
				}
				response.setDuration(canSetDurationInMCCResponse ? JavaxUtil.getDatatypeFactory().newDuration(duration) : null);
			}
		}
	}	
}
