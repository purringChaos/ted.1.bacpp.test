/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hls;

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
import tv.blackarrow.cpp.mcctemplate.macros.MCCTemplateMacroResolver;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author shwetanks
 *
 */
public class MCCTemplateHLSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo, final ManifestInfo info, 
			final AcquisitionPoint acquisitionPoint) throws Exception {
		
		Short segmentTypeID = -1;
		if(Boolean.TRUE.equals(signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator())
				|| signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId() == null) {
			segmentTypeID = Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT;
		} else {
			segmentTypeID = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId();
		}
		
		// For blackout even for template use envivio schema for mcc response.
		if (Schema.i03 == info.getSchema() && SegmentType.isValidBlackoutSignal(segmentTypeID)) {
			info.setSchema(Schema.Envivio);
		}
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			decorateProgramStartManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID, HLSTemplateType.NO_BLACKOUT_PROGRAM_START);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			decorateProgramStartManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID, HLSTemplateType.PROGRAM_RUNOVER_UNPLANNED);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			decorateProgramStartManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID, HLSTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			decorateProgramEndManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID);
			break;	
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			decorateContentIdentityManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START:
		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_END:
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_END:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_START:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_END:
			decoratePlacementOppourtunityManifestResponse(signal, response, cpo, info, acquisitionPoint, segmentTypeID);
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
	 * @param hlsTemplate 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final ConfirmedPlacementOpportunity cpo, 
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, Short segmentTypeID, HLSTemplateType hlsTemplate) throws Exception {
		
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify); 
		if(cpo == null){
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal, cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(hlsTemplate);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if(confirmationSegments != null && confirmationSegments.getFirstSegment() != null){
					Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
					TagSequence firstTag = new TagSequence(); 
					segModify.setFirstSegment(firstTag);
					List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
					if(firstTags != null){
						firstTag.getTag().addAll(firstTags);
					}
				}
		}else{
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PROGRAM_START);
			HLSTemplateSegmentsVO templateSegments = null;
			if (cpo.isAborted()) {
				response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
				templateSegments = mccTemplateVO.getAbortSegments();
			}else{
				templateSegments = mccTemplateVO.getConfirmationSegments();				
			}
			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if(firstTags != null){
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if(spanTags != null){
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if(lastTags != null){
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
	 * @param cpo 
	 * @throws Exception
	 */
	private void decorateProgramEndManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, final ConfirmedPlacementOpportunity cpo, 
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		
		final SegmentModifyType segModify = new SegmentModifyType(); 
		response.setSegmentModify(segModify); 
		if (cpo == null) {
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.NO_BLACKOUT_PROGRAM_END);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if (confirmationSegments != null&& confirmationSegments.getLastSegment() != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(lastTags);
				}
			}
		}else{
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PROGRAM_END);
			HLSTemplateSegmentsVO templateSegments = null;
			
			if (cpo.isAborted()) {
				response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
				templateSegments = mccTemplateVO.getAbortSegments();
			}else{
				templateSegments = mccTemplateVO.getConfirmationSegments();				
			}
			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if(firstTags != null){
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if(spanTags != null){
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if(lastTags != null){
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}	
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
	private void decorateContentIdentityManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal,final ManifestResponseType response, final ConfirmedPlacementOpportunity cpo, 
			final ManifestInfo info, final AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		
		final SegmentModifyType segModify = new SegmentModifyType(); 
		response.setSegmentModify(segModify); 
		//TOD need to verify this condition for ContentIdentity 
		if (cpo == null) {
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.CONTENT_IDENTIFICATION);
			HLSTemplateSegmentsVO confirmationSegments = mccTemplateVO.getConfirmationSegments();
			if (confirmationSegments != null&& confirmationSegments.getLastSegment() != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(confirmationSegments, macroResolver);
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if (lastTags != null) {
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(lastTags);
				}
			}
		}else{
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.CONTENT_IDENTIFICATION);
			HLSTemplateSegmentsVO templateSegments = null;
			if (cpo.isAborted()) {
				response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
				templateSegments = mccTemplateVO.getAbortSegments();
			}else{
				templateSegments = mccTemplateVO.getConfirmationSegments();				
			}
			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);
				// First Segment
				
				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if(firstTags != null){
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if(spanTags != null){
					TagSequence spanTag = new TagSequence();
					segModify.setSpanSegment(spanTag);
					spanTag.getTag().addAll(spanTags);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if(lastTags != null){
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}
	}
	/**
	 * decorate the response for placement Oppourtunity
	 * @param signal
	 * @param response
	 * @param cpo
	 * @param info
	 * @param isPOStart 
	 * @throws DatatypeConfigurationException 
	 */
	private void decoratePlacementOppourtunityManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ManifestResponseType response, 
			final ConfirmedPlacementOpportunity cpo,ManifestInfo info, final AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws DatatypeConfigurationException {
		
		response.setDataPassThrough(false);	
		final SegmentModifyType segModify = new SegmentModifyType();
		response.setSegmentModify(segModify);

		if (cpo != null) {
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal,cpo, info, acquisitionPoint, segmentTypeID);
			HLSTemplateType template = null;
			if(Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START ==  segmentTypeID && acquisitionPoint.isUseInbandOpportunitySignal()) {
				template = HLSTemplateType.SCHEDULELESS_INBAND_PLACEMENT_OPPORTUNITY;
			}else {
				template = HLSTemplateType.PLACEMENT_OPPORTUNITY;
			}
			HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(template);
			HLSTemplateSegmentsVO templateSegments = null;
			
			if (cpo.isAborted()) {
				response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
				templateSegments = mccTemplateVO.getAbortSegments();
			} else {
				templateSegments = mccTemplateVO.getConfirmationSegments();
			}
			
			if (templateSegments != null) {
				Map<String, List<TagType>> segments = MCCTemplateResponseComponentBuilder.resolveMccHLSTemplateSegment(templateSegments, macroResolver);

				List<TagType> firstTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT);
				if(firstTags != null){
					TagSequence firstTag = new TagSequence();
					segModify.setFirstSegment(firstTag);
					firstTag.getTag().addAll(firstTags);
				}
				// Span Segment
				List<TagType> spanTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT);
				if(spanTags != null){
					TagSequence spanTag = new TagSequence();
					spanTag.getTag().addAll(spanTags);
					segModify.setSpanSegment(spanTag);
				}
				// Last Segment
				List<TagType> lastTags = segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT);
				if(lastTags != null){
					TagSequence lastTag = new TagSequence();
					segModify.setLastSegment(lastTag);
					lastTag.getTag().addAll(segments.get(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT));
				}
			}
		}
	}

}
