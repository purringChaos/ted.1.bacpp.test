/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.dash.i03;

import javax.xml.datatype.DatatypeConfigurationException;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.mcctemplate.DASHConstants;
import tv.blackarrow.cpp.mcctemplate.DASHTemplateType;
import tv.blackarrow.cpp.mcctemplate.DASHTemplateVO;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateResponseComponentBuilder;
import tv.blackarrow.cpp.mcctemplate.macros.MCCTemplateMacroResolver;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.utils.JavaxUtil;

/**
 * @author 
 *
 */
public class MCCTemplateDASHI03ManifestResponseDecorator extends AbstractManifestResponseDecorator {

	@Override
	public void decorateManifestResponse(ManifestResponseType response, AcquiredSignal signal, ConfirmedPlacementOpportunity cpo, 
			ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception {

		Short segmentTypeID = getSegmentTypeId(signal);

		DASHTemplateVO dashTemplateVO = findDashTemplateVO(segmentTypeID, acquisitionPoint);
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			decorateProgramStartManifestResponse(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID, DASHTemplateType.NO_BLACKOUT_PROGRAM_START);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			decorateProgramStartManifestResponse(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID, DASHTemplateType.PROGRAM_RUNOVER_UNPLANNED);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			decorateProgramStartManifestResponse(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID, DASHTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			decorateProgramEndManifestResponse(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_EARLY_TERMINATION:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_PLANNED:
			decorateManifestResponseForProgram(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START:
		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_END:
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_END:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_START:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_END:
			decorateManifestResponseForPO(dashTemplateVO, signal, response, cpo, info, acquisitionPoint, segmentTypeID);
			break;
		default:
			throw new RuntimeException("unsupport segment type id " + segmentTypeID);
		}

	}

	private Short getSegmentTypeId(AcquiredSignal signal) {
		Short segmentTypeID = -1;
		if (Boolean.TRUE.equals(signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator())
				|| signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId() == null) {
			segmentTypeID = Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT;
		} else {
			segmentTypeID = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId();
		}
		return segmentTypeID;
	}

	/**
	 * add tags for signal Program Start Manifest response
	 * There are three segments
	 * @param dashTemplateVO 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param cpo 
	 * @param dashTemplate 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint, Short segmentTypeID, DASHTemplateType dashTemplate) throws Exception {
		if (cpo == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(dashTemplate);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, cpo, info, false, acquisitionPoint, segmentTypeID);
	}

	/**
	 * add tags for signal Program End Manifest response
	 * There are three segments
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param cpo 
	 * @throws Exception
	 */
	private void decorateProgramEndManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		if (cpo == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(DASHTemplateType.NO_BLACKOUT_PROGRAM_END);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, cpo, info, false, acquisitionPoint, segmentTypeID);
	}

	/**
	 * decorate the response for all other
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param cpo
	 * @param info
	 * @throws DatatypeConfigurationException 
	 */
	private void decorateManifestResponseForProgram(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		if (cpo != null) {
			decorateBasicManifestResponse(dashTemplateVO, signal, response, cpo, info, false, acquisitionPoint, segmentTypeID);
		}
	}

	/**
	 * decorate the response for all other
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param cpo
	 * @param info
	 * @throws DatatypeConfigurationException 
	 */
	private void decorateManifestResponseForPO(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		response.setDataPassThrough(false);
		if (cpo != null) {
			decorateBasicManifestResponse(dashTemplateVO, signal, response, cpo, info, true, acquisitionPoint, segmentTypeID);
		}
		//PO End for inpoint is only handled for DASH template(Special), for HLS and HSS it returns "No-Op" in notes.
		else {
			MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal, cpo, info, acquisitionPoint, segmentTypeID);
			processDashPattern(response, macroResolver, dashTemplateVO, signal);
		}
	}

	/**
	 * decorate the response for the signal of Content Identification
	 * The response will only include one Segment (SegmentModifiy)
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param cpo 
	 * @param isPlacementOpportunity 
	 * @throws Exception
	 */
	private void decorateBasicManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			ConfirmedPlacementOpportunity cpo, ManifestInfo info, boolean isPlacementOpportunity, AcquisitionPoint acquisitionPoint, Short segmentTypeID) throws Exception {
		MCCTemplateMacroResolver macroResolver = new MCCTemplateMacroResolver(response, signal, cpo, info, acquisitionPoint, segmentTypeID);
		dashTemplateVO = includeDurationIfCPOisAborted(response, cpo, dashTemplateVO, isPlacementOpportunity);
		processDashPattern(response, macroResolver, dashTemplateVO,signal);
	}

	private void processDashPattern(ManifestResponseType response, MCCTemplateMacroResolver macroResolver, DASHTemplateVO dashTemplateVO, AcquiredSignal signal) {
		if (dashTemplateVO != null) {
			//The TemplateResponse are currently supported in I03 schema, this whole class implementation has been pre-existed for I01. I have provided
			// a patch here to set the value needed by I03 in manifestResponse's other. It will be retrieved in IO3ResponseHandler class and will be set
			// in response accordingly.
			MCCTemplateResponseComponentBuilder.resolveMccDashTemplateResponse(dashTemplateVO, macroResolver);
			response.getOtherAttributes().put(DASHConstants.DASH_TEMPLATE_RESPONSE_TYPE, dashTemplateVO.getTemplateType());
			response.getOtherAttributes().put(DASHConstants.DASH_TEMPLATE_RESPONSE_VALUE, dashTemplateVO.getTemplateValue());
			response.getOtherAttributes().put(MCCTemplateConstants.MCC_TEMPLATE_SEGMENTATION_TYPE_ID, getSegmentTypeId(signal).toString());
		}
	}

	private DASHTemplateVO includeDurationIfCPOisAborted(ManifestResponseType response, ConfirmedPlacementOpportunity cpo, DASHTemplateVO dashTemplateVO,
			boolean isPlacementOpportunity) throws DatatypeConfigurationException {
		if (cpo != null && cpo.isAborted()) {
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
			dashTemplateVO = getDashTemplate(isPlacementOpportunity ? DASHTemplateType.PLACEMENT_OPPORTUNITY_END : DASHTemplateType.PROGRAM_END);
		}
		return dashTemplateVO;
	}

	/*
	 * The template chosen in this function is driven by underdog MPEG_DASH work.
	 */
	private DASHTemplateVO findDashTemplateVO(int segmentTypeID, AcquisitionPoint acquisitionPoint) {
		DASHTemplateVO dashTemplateVO = null;
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_START);
			break;
		
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_END);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			dashTemplateVO = getDashTemplate(DASHTemplateType.CONTENT_IDENTIFICATION);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_EARLY_TERMINATION:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_PLANNED:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_EXT);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_RUNOVER_UNPLANNED);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START:
			if(acquisitionPoint.isUseInbandOpportunitySignal()) {
				dashTemplateVO = getDashTemplate(DASHTemplateType.SCHEDULELESS_INBAND_PLACEMENT_OPPORTUNITY_START);
			}else {
				dashTemplateVO = getDashTemplate(DASHTemplateType.PLACEMENT_OPPORTUNITY_START);
			}
			break;
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_START:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_START:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PLACEMENT_OPPORTUNITY_START);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_END:
		case Scte35Contants.SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_END:
		case Scte35Contants.SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_END:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PLACEMENT_OPPORTUNITY_END);
			break;

		default:
			break;
		}
		return dashTemplateVO;
	}

	private DASHTemplateVO getDashTemplate(DASHTemplateType dashType) {
		DASHTemplateVO dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(dashType);
		if (dashTemplateVO == null) {
			dashTemplateVO = getDashTemplate(DASHTemplateType.DEFAULT);
		}
		return dashTemplateVO;
	}

}
