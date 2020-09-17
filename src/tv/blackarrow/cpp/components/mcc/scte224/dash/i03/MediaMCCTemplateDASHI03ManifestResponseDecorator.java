/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.scte224.dash.i03;

import javax.xml.datatype.DatatypeConfigurationException;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.scte224.SCTE224ManifestResponseDecorator;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.mcctemplate.DASHConstants;
import tv.blackarrow.cpp.mcctemplate.DASHTemplateType;
import tv.blackarrow.cpp.mcctemplate.DASHTemplateVO;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateResponseComponentBuilder;
import tv.blackarrow.cpp.mcctemplate.macros.SCTE224MCCTemplateMacroResolver;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author yrajoria
 *
 */
public class MediaMCCTemplateDASHI03ManifestResponseDecorator extends AbstractManifestResponseDecorator {

	@Override
	public void decorateManifestResponse(ManifestResponseType response, AcquiredSignal signal, MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint,
			Short segmentTypeID) throws Exception {

		SegmentType segmentType = SegmentType.valueOf(segmentTypeID);

		DASHTemplateVO dashTemplateVO = findDashTemplateVO(segmentTypeID);
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			decorateProgramStartManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, acquisitionPoint, segmentType);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			decorateProgramEndManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, acquisitionPoint, segmentType);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:

			decorateManifestResponseForProgram(dashTemplateVO, signal, response, mediaLedger, info, acquisitionPoint, segmentType);
			break;
			
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			decorateProgramRunoverUnplannedManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, acquisitionPoint, segmentType);
			break;
			
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			decorateBlackoutOverrideResponse(dashTemplateVO, signal, response, mediaLedger, info, acquisitionPoint, segmentType);
			break;
			
		default:
			throw new RuntimeException("unsupport segment type id " + segmentTypeID);
		}

	}

	/**
	 * add tags for signal Program Start Manifest response
	 * There are three segments
	 * @param dashTemplateVO 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param segmentType 
	 * @throws Exception
	 */
	private void decorateProgramStartManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {
		if (mediaLedger == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(DASHTemplateType.NO_BLACKOUT_PROGRAM_START);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, false, acquisitionPoint, segmentType);
	}
	
	private void decorateProgramRunoverUnplannedManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {
		if (mediaLedger == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(DASHTemplateType.PROGRAM_RUNOVER_UNPLANNED);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, false, acquisitionPoint, segmentType);
	}
	private void decorateBlackoutOverrideResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {
		if (mediaLedger == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(DASHTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, false, acquisitionPoint, segmentType);
	}
	/**
	 * add tags for signal Program End Manifest response
	 * There are three segments
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param segmentType 
	 * @throws Exception
	 */
	private void decorateProgramEndManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {
		if (mediaLedger == null) {
			dashTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getDashTemplateTypeVsTemplate().get(DASHTemplateType.NO_BLACKOUT_PROGRAM_END);
		}
		decorateBasicManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, false, acquisitionPoint, segmentType);
	}

	/**
	 * decorate the response for all other
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param mediaLedger
	 * @param info
	 * @param segmentType 
	 * @throws DatatypeConfigurationException 
	 */
	private void decorateManifestResponseForProgram(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, AcquisitionPoint acquisitionPoint, SegmentType segmentType) throws Exception {
		if (mediaLedger != null) {
			decorateBasicManifestResponse(dashTemplateVO, signal, response, mediaLedger, info, false, acquisitionPoint, segmentType);
		}
	}

	/**
	 * decorate the response for the signal of Content Identification
	 * The response will only include one Segment (SegmentModifiy)
	 * @param dashTemplateVO2 
	 * @param signal
	 * @param response
	 * @param info 
	 * @param mediaLedger 
	 * @param isPlacementOpportunity 
	 * @throws Exception
	 */
	private void decorateBasicManifestResponse(DASHTemplateVO dashTemplateVO, ManifestConfirmConditionEventType.AcquiredSignal signal, ManifestResponseType response,
			MediaLedger mediaLedger, ManifestInfo info, boolean isPlacementOpportunity, AcquisitionPoint acquisitionPoint, SegmentType segmentTypeId) throws Exception {
		SCTE224MCCTemplateMacroResolver macroResolver = new SCTE224MCCTemplateMacroResolver(response, signal, info, acquisitionPoint, mediaLedger, segmentTypeId);
		//dashTemplateVO = includeDurationIfCPOisAborted(response, mediaLedger, dashTemplateVO, isPlacementOpportunity);
		processDashPattern(response, macroResolver, dashTemplateVO, signal, segmentTypeId);
	}

	private void processDashPattern(ManifestResponseType response, SCTE224MCCTemplateMacroResolver macroResolver, DASHTemplateVO dashTemplateVO, AcquiredSignal signal,
			SegmentType segmentTypeId) {
		if (dashTemplateVO != null) {
			//The TemplateResponse are currently supported in I03 schema, this whole class implementation has been pre-existed for I01. I have provided
			// a patch here to set the value needed by I03 in manifestResponse's other. It will be retrieved in IO3ResponseHandler class and will be set
			// in response accordingly.
			MCCTemplateResponseComponentBuilder.resolveMccDashTemplateResponse(dashTemplateVO, macroResolver);
			response.getOtherAttributes().put(DASHConstants.DASH_TEMPLATE_RESPONSE_TYPE, dashTemplateVO.getTemplateType());
			response.getOtherAttributes().put(DASHConstants.DASH_TEMPLATE_RESPONSE_VALUE, dashTemplateVO.getTemplateValue());
			response.getOtherAttributes().put(MCCTemplateConstants.MCC_TEMPLATE_SEGMENTATION_TYPE_ID, Short.toString(segmentTypeId.getSegmentTypeId()));
		}
	}

	/*
	 * The template chosen in this function is driven by underdog MPEG_DASH work.
	 */
	private DASHTemplateVO findDashTemplateVO(int segmentTypeID) {
		DASHTemplateVO dashTemplateVO = null;
		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_START);
			break;
			
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_RUNOVER_UNPLANNED);
			break;
			
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_BLACKOUT_OVERRIDE);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			dashTemplateVO = getDashTemplate(DASHTemplateType.PROGRAM_END);
			break;

		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
			dashTemplateVO = getDashTemplate(DASHTemplateType.CONTENT_IDENTIFICATION);
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
