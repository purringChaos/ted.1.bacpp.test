/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.scte224.hls;

import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SegmentModifyType;
import tv.blackarrow.cpp.manifest.TagSequence;
import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateSegmentsVO;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateType;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateVO;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateResponseComponentBuilder;
import tv.blackarrow.cpp.mcctemplate.macros.SCTE224MCCTemplateMacroResolver;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author shwetanks
 *
 */
public class MediaMCCTemplateHLSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final AcquiredSignal signal, final MediaLedger mediaLedger, final ManifestInfo info,
			final AcquisitionPoint acquisitionPoint, Short segmentTypeIDValue) throws Exception {

		// For Media even for template use envivio schema for mcc response.
		if (Schema.i03 == info.getSchema() && SegmentType.isValidBlackoutSignal(segmentTypeIDValue)) {
			info.setSchema(Schema.Envivio);
		}
		SegmentType segmentTypeId= SegmentType.valueOf(segmentTypeIDValue);
		switch (segmentTypeIDValue) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			decorateProgramStartManifestResponse(signal, response, mediaLedger, info, acquisitionPoint, segmentTypeId, HLSTemplateType.PROGRAM_START);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			decorateProgramEndManifestResponse(signal, response, mediaLedger, info, acquisitionPoint,segmentTypeId);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			decorateContentIdentityManifestResponse(signal, response, mediaLedger, info, acquisitionPoint, segmentTypeId);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			decorateProgramStartManifestResponse(signal, response, mediaLedger, info, acquisitionPoint, segmentTypeId, HLSTemplateType.PROGRAM_RUNOVER_UNPLANNED);
			break;
			
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			decorateProgramStartManifestResponse(signal, response, mediaLedger, info, acquisitionPoint, segmentTypeId, HLSTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
			break;
			
		default:
			throw new RuntimeException("unsupport segment type id " + segmentTypeIDValue);
		}
	}

	/**
	 * add tags for signal Program Start Manifest response
	 * There are three segments
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param segmentType 
	 * @param hlsTemplateType 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final MediaLedger mediaLedger,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, SegmentType segmentType, HLSTemplateType hlsTemplateType) throws Exception {

		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		if (mediaLedger == null) {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger, segmentType);
			HLSTemplateType hlsTemplate = hlsTemplateType.equals(HLSTemplateType.PROGRAM_START)? HLSTemplateType.NO_BLACKOUT_PROGRAM_START : hlsTemplateType;
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(hlsTemplate);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if (confirmationSegments != null && confirmationSegments.getFirstSegment() != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
				TagSequence firstTag = new TagSequence();
				segModify.setFirstSegment(firstTag);
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if (firstTags != null) {
					firstTag.getTag().addAll(firstTags);
				}
			}
		} else {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger, segmentType);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(hlsTemplateType);
			HLSTemplateSegmentsVO templateSegments = mccTemplateVO.getConfirmationSegments();/*populateTemplateSegmentsAndDuration(response, mediaLedger, mccTemplateVO);*/

			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if (firstTags != null) {
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if (spanTags != null) {
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}
	}
	
	/**
	 * add tags for signal Program End Manifest response
	 * There are three segments
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param segmentTypeId 
	 * @throws Exception
	 */
	private void decorateProgramEndManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final MediaLedger mediaLedger,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, SegmentType segmentTypeId) throws Exception {

		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		if (mediaLedger == null) {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger, segmentTypeId);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.NO_BLACKOUT_PROGRAM_END);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if (confirmationSegments != null && confirmationSegments.getLastSegment() != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(lastTags);
				}
			}
		} else {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger,segmentTypeId);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PROGRAM_END);
			HLSTemplateSegmentsVO templateSegments = mccTemplateVO.getConfirmationSegments();/*populateTemplateSegmentsAndDuration(response, mediaLedger, mccTemplateVO);*/

			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if (firstTags != null) {
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if (spanTags != null) {
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}
	}

	/*private HLSTemplateSegmentsVO populateTemplateSegmentsAndDuration(final ManifestResponseType response, final MediaLedger mediaLedger, HLSTemplateVO mccTemplateVO)
			throws DatatypeConfigurationException {
		HLSTemplateSegmentsVO templateSegments = mccTemplateVO.getConfirmationSegments();
		Long duration = null;
		if (mediaLedger.isMediaStarted() && !mediaLedger.isMediaEnded()) {
			MediaTransaction startMediaTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			duration = startMediaTransaction.getTotalDurationInMS();
		} else if (mediaLedger.isMediaEnded()) {
			MediaTransaction endMediaTransaction = mediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
			duration = endMediaTransaction.getTotalDurationInMS();
		}

		if (duration != null) {
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		}
		return templateSegments;
	}
*/
	private HLSTemplateSegmentsVO populateTemplateSegments(final ManifestResponseType response, final MediaLedger mediaLedger, HLSTemplateVO mccTemplateVO)
			throws DatatypeConfigurationException {
		HLSTemplateSegmentsVO templateSegments = mccTemplateVO.getConfirmationSegments();		
		return templateSegments;
	}

	/**
	 * decorate the response for the signal of Content Identification
	 * The response will only include one Segment (SegmentModifiy)
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param segmentTypeId 
	 * @throws Exception
	 */
	private void decorateContentIdentityManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final MediaLedger mediaLedger,
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, SegmentType segmentTypeId) throws Exception {

		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);
		//TOD need to verify this condition for ContentIdentity 
		if (mediaLedger == null) {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger,segmentTypeId);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.CONTENT_IDENTIFICATION);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if (confirmationSegments != null && confirmationSegments.getLastSegment() != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(lastTags);
				}
			}
		} else {
			SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger, segmentTypeId);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.CONTENT_IDENTIFICATION);
			HLSTemplateSegmentsVO templateSegments = populateTemplateSegments(response, mediaLedger, mccTemplateVO);
			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment

				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if (firstTags != null) {
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if (spanTags != null) {
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}
	}

}
