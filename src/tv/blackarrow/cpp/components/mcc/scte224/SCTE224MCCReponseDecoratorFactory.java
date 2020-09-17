package tv.blackarrow.cpp.components.mcc.scte224;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.dash.i03.MediaMCCTemplateDASHI03ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.hls.MediaMCCCustomHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.hls.MediaMCCStandardHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.hls.MediaMCCTemplateHLSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.hss.MediaMCCCustomHSSManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.scte224.hss.MediaMCCStandardHSSManifestResponseDecorator;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.CppConstants;

public class SCTE224MCCReponseDecoratorFactory {

	static public ManifestResponseDecorator getHLSManifestResponseDector(AcquiredSignal signal, String hlsInterfaceType, String signalId, Short segmentTypeID) {

		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_END:
			if (CppConstants.INTERFACE_TEMPLATE.equals(hlsInterfaceType)) {
				return new MediaMCCTemplateHLSManifestResponseDecorator();
			}else if (CppConstants.INTERFACE_CUSTOM.equals(hlsInterfaceType)) {
				return new MediaMCCCustomHLSManifestResponseDecorator();
			}else if (CppConstants.INTERFACE_STANDARD.equals(hlsInterfaceType)) {
				return new MediaMCCStandardHLSManifestResponseDecorator();
			}
			break;
		default:
			break;
		}
		return null;
	}

	static public ManifestResponseDecorator getHSSManifestResponseDector(AcquiredSignal signal, String hssInterfaceType, String signalId, Short segmentTypeID) {

		switch (segmentTypeID) {
		case Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_START:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED:
		case Scte35Contants.SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE:
			if (CppConstants.INTERFACE_CUSTOM.equals(hssInterfaceType)) {
				return new MediaMCCCustomHSSManifestResponseDecorator();
			}
			break;
		default:
			break;
		}

		return new MediaMCCStandardHSSManifestResponseDecorator();
	}

	public static ManifestResponseDecorator getDashManifestResponseDector(Schema schema, ManifestInfo info, Short segmentTypeId) {
		switch (schema) {
		case i01:
			//Not supported
			return null;
		case i03:
			info.setSchema(Schema.Envivio);
			return new MediaMCCTemplateDASHI03ManifestResponseDecorator();
		default:
			break;
		}
		return null;
	}
}
