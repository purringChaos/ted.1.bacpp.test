package tv.blackarrow.cpp.components.mcc.other;

import java.util.List;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.dash.i03.MCCTemplateDASHI03ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.BlackoutHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.HLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.MCCTemplateHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.P5HLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.TWCBlackoutHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hls.TWCHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hss.HSSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hss.P5HSSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hss.TWCBlackoutHSSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.other.hss.TWCHSSManifestResponseDecorator;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateType;
import tv.blackarrow.cpp.mcctemplate.HLSTemplateVO;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateCompiledConfiguration;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.CppConstants;

public class OtherMCCReponseDecoratorFactory {

	static public ManifestResponseDecorator getHLSManifestResponseDector(AcquiredSignal signal, String hlsInterfaceType, String signalId, AcquisitionPoint  acquisitionPoint) {

		int segmentTypeID = getSegmentTypeId(signal);

		if (segmentTypeID == -1) {
			/**
			 * Implies segmentTypeID was not present in the request. So try to identify based on the signal id whether this is a blackout or PO request.
			 */
			if (signalId != null && !signalId.isEmpty()) {
				boolean isBlackout = DataManagerFactory.getInstance().getSingleBlackoutEvent(signalId) != null;
				if (isBlackout) {
					segmentTypeID = Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT;//Consider it a blackout event.
				}
			}
		}

		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			// blackout signals 
			if (CppConstants.INTERFACE_CUSTOM.equals(hlsInterfaceType) || CppConstants.INTERFACE_TWC_LINEAR.equals(hlsInterfaceType)) {
				return new TWCBlackoutHLSManifestResponseDecorator();
			} else if (CppConstants.INTERFACE_TEMPLATE.equals(hlsInterfaceType)) {
				//need to check template is there otherwise cpp bean
				//if(MCCTemplateCompiledConfiguration.g)
				HLSTemplateVO mccTemplateVO = null;
				if (Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT == segmentTypeID) {
					mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.CONTENT_IDENTIFICATION);
				} else {
					mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PROGRAM_START);
				}
				if (mccTemplateVO != null) {
					return new MCCTemplateHLSManifestResponseDecorator();
				} else {
					if (CppConstants.INTERFACE_CUSTOM.equals(CppConfigurationBean.getInstance().getDefaultHLSInterfaceType())
							|| CppConstants.INTERFACE_TWC_LINEAR.equals(CppConfigurationBean.getInstance().getDefaultHLSInterfaceType())) {
						return new TWCBlackoutHLSManifestResponseDecorator();
					} else {
						return new BlackoutHLSManifestResponseDecorator();
					}
				}
			} else {
				return new BlackoutHLSManifestResponseDecorator();
			}
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			if (CppConstants.INTERFACE_TEMPLATE.equals(hlsInterfaceType)) {
				HLSTemplateVO mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PROGRAM_END);
				if (mccTemplateVO != null) {
					return new MCCTemplateHLSManifestResponseDecorator();
				}
			}
		}

		// other than blackout signals 
		if (CppConstants.INTERFACE_COMCAST_P5.equals(hlsInterfaceType)) {
			return new P5HLSManifestResponseDecorator();
		}

		if (CppConstants.INTERFACE_CUSTOM.equals(hlsInterfaceType) || CppConstants.INTERFACE_TWC_LINEAR.equals(hlsInterfaceType)) {
			TWCHLSManifestResponseDecorator responseDecorator = new TWCHLSManifestResponseDecorator();
			responseDecorator.setSegmentTypeID(segmentTypeID);
			return responseDecorator;
		}

		// Handling of Template Response for PO
		if (CppConstants.INTERFACE_TEMPLATE.equals(hlsInterfaceType)) {
			HLSTemplateVO mccTemplateVO = null; 
			if(acquisitionPoint.isUseInbandOpportunitySignal() && segmentTypeID == Scte35Contants.SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START) {
				mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.SCHEDULELESS_INBAND_PLACEMENT_OPPORTUNITY);
			}else {
				mccTemplateVO = MCCTemplateCompiledConfiguration.getInstance().getHLSTemplates().get(HLSTemplateType.PLACEMENT_OPPORTUNITY);
			}
			
			if (mccTemplateVO != null) {
				return new MCCTemplateHLSManifestResponseDecorator();
			} else {
				if (CppConstants.INTERFACE_COMCAST_P5.equals(CppConfigurationBean.getInstance().getDefaultHLSInterfaceType())) {
					return new P5HLSManifestResponseDecorator();
				}
				if (CppConstants.INTERFACE_CUSTOM.equals(CppConfigurationBean.getInstance().getDefaultHLSInterfaceType())
						|| CppConstants.INTERFACE_TWC_LINEAR.equals(CppConfigurationBean.getInstance().getDefaultHLSInterfaceType())) {
					return new TWCHLSManifestResponseDecorator();
				} else {
					return new HLSManifestResponseDecorator();
				}
			}
		}
		return new HLSManifestResponseDecorator();
	}

	static public ManifestResponseDecorator getDashManifestResponseDector(Schema schema, ManifestInfo info, AcquisitionPoint  acquisitionPoint) {
		switch (schema) {
		case i01:
			//Not supported
			return null;
		case i03:
			info.setSchema(Schema.Envivio);
			return new MCCTemplateDASHI03ManifestResponseDecorator();
		default:
			break;
		}
		return null;
	}

	static public ManifestResponseDecorator getHSSManifestResponseDector(AcquiredSignal signal, String hssInterfaceType, String signalId, AcquisitionPoint  acquisitionPoint) {
		int segmentTypeID = getSegmentTypeId(signal);

		if (segmentTypeID == -1) {
			/**
			 * Implies segmentTypeID was not present in the request. So try to identify based on the signal id whether this is a blackout or PO request.
			 */
			if (signalId != null && !signalId.isEmpty()) {
				boolean isBlackout = DataManagerFactory.getInstance().getSingleBlackoutEvent(signalId) != null;
				if (isBlackout) {
					segmentTypeID = Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT;//Consider it a blackout event.
				}
			}
		}

		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			if (CppConstants.INTERFACE_CUSTOM.equals(hssInterfaceType) || CppConstants.INTERFACE_TWC_LINEAR.equals(hssInterfaceType)) {
				return new TWCBlackoutHSSManifestResponseDecorator();
			}
			return new HSSManifestResponseDecorator();
		}

		if (CppConstants.INTERFACE_COMCAST_P5.equals(hssInterfaceType)) {
			return new P5HSSManifestResponseDecorator();
		}

		if (CppConstants.INTERFACE_CUSTOM.equals(hssInterfaceType) || CppConstants.INTERFACE_TWC_LINEAR.equals(hssInterfaceType)) {
			return new TWCHSSManifestResponseDecorator();
		}

		return new HSSManifestResponseDecorator();
	}

	static public int getSegmentTypeId(AcquiredSignal signal) {
		int segmentTypeID = -1;

		if (signal.getSCTE35PointDescriptor() != null) {
			List<SegmentationDescriptorType> segmentInfo = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo();
			if (segmentInfo != null && segmentInfo.size() > 0 && segmentInfo.get(0).getSegmentTypeId() != null) {
				segmentTypeID = segmentInfo.get(0).getSegmentTypeId();
			}
		}
		return segmentTypeID;
	}

}
