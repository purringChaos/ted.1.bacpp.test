/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.scte224.hls;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.scte224.SCTE224ManifestResponseDecorator;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.mcctemplate.DASHTemplateType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SegmentModifyType;
import tv.blackarrow.cpp.manifest.TagSequence;
import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceType;

/**
 * @author pzhang
 *
 */
public class MediaMCCCustomHLSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	private static Logger logger = LogManager.getLogger(MediaMCCCustomHLSManifestResponseDecorator.class);

	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final AcquiredSignal signal, final MediaLedger cpo, final ManifestInfo info,
			final AcquisitionPoint acquisitionPoint, final Short segmentTypeID) throws Exception {

		// use envivio schema for mcc response.
		if (Schema.i03 == info.getSchema()) {
			info.setSchema(Schema.Envivio);
		}
		SegmentType segmentType = SegmentType.valueOf(segmentTypeID);
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			decorateProgramStartManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentType);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			decorateContentIdentityManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentType);
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
	 * @param segmentType 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final MediaLedger cpo,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {

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
		extraInfo.append("SignalId=").append(signalIdHex).append(",SpliceType=").append(spliceType).append(",feed=").append(URLEncoder.encode(feed, "UTF-8")).append(",network=")
				.append(URLEncoder.encode(network, "UTF-8")).append(",ap=").append(URLEncoder.encode(apid, "UTF-8")).append(",utc=").append(utc).append(",streamID=${streamID}");

		// First Segment
		TagType tag = new TagType();

		tag.setValue("#EXT-X-SIGNAL-EXIT:" + extraInfo.toString());
		tag.setAdapt(true);

		tag.setLocality("before");
		firstTag.getTag().add(tag);

		// Span Segment
		tag = new TagType();

		tag.setValue("#EXT-X-SIGNAL-SPAN:${secondsFromSignal}," + extraInfo.toString());

		tag.setAdapt(true);

		spanTag.getTag().add(tag);

		// Last Segment
		TagType lasttagType = new TagType();

		lastTag.getTag().add(tag);
		lasttagType.setValue("#EXT-X-SIGNAL-RETURN:" + extraInfo.toString());
		lasttagType.setAdapt(true);

		lasttagType.setLocality("after");
		lastTag.getTag().add(lasttagType);

	}

	private String findSpliceType(String signalId) {
		BlackoutEvent event = DataManagerFactory.getInstance().getSingleBlackoutEvent(signalId);
		if (event == null) {
			return SpliceType.ALT_CONTENT.name();
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
	 * @param segmentType 
	 * @throws Exception
	 */
	private void decorateContentIdentityManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final MediaLedger cpo,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {

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
		extraInfo.append("SignalId=").append(signalIdHex).append(",SpliceType=").append(spliceType).append(",feed=").append(URLEncoder.encode(feed, "UTF-8")).append(",network=")
				.append(URLEncoder.encode(network, "UTF-8")).append(",ap=").append(URLEncoder.encode(apid, "UTF-8")).append(",utc=").append(utc).append(",streamID=${streamID}");

		// First Segment
		TagType tag = new TagType();

		tag.setAdapt(true);
		tag.setValue("#EXT-X-SIGNAL-EXIT:" + extraInfo.toString());

		tag.setLocality("before");
		firstTag.getTag().add(tag);

		tag = new TagType();

		tag.setAdapt(true);

		tag.setValue("#EXT-X-SIGNAL-SPAN:${secondsFromSignal}," + extraInfo.toString());

		TagSequence spanTag = new TagSequence();
		spanTag.getTag().add(tag);
		segModify.setSpanSegment(spanTag);

		TagSequence lastTag = new TagSequence();
		TagType lasttagType = new TagType();

		lastTag.getTag().add(tag);
		lasttagType.setValue("#EXT-X-SIGNAL-RETURN:" + extraInfo.toString());
		lasttagType.setAdapt(true);

		lasttagType.setLocality("after");
		lastTag.getTag().add(lasttagType);
		segModify.setLastSegment(lastTag);

	}
	
}
