/**
 * 
 */
package tv.blackarrow.cpp.components.mcc;

import tv.blackarrow.cpp.components.mcc.other.OtherMCCReponseDecoratorFactory;
import tv.blackarrow.cpp.components.mcc.scte224.SCTE224MCCReponseDecoratorFactory;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.utils.CppConstants;

/**
 * @author pzhang
 *
 */
public class ManifestResponseDecoratorFactory {

	static public ManifestResponseDecorator getManifestResponseDector(ManifestInfo info, Schema schema, AcquiredSignal signal, String streamType, String hlsInterfaceType,
			String hssInterfaceType, String signalId, MCCTypes mccType, Short segmentTypeId, AcquisitionPoint  acquisitionPoint) {
		ManifestResponseDecorator decorator = null;
		switch (mccType) {
		case SCTE224:
			if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HLS)) {
				decorator = SCTE224MCCReponseDecoratorFactory.getHLSManifestResponseDector(signal, hlsInterfaceType, signalId, segmentTypeId);
			} else if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HSS)) {// HSS
				decorator = SCTE224MCCReponseDecoratorFactory.getHSSManifestResponseDector(signal, hssInterfaceType, signalId, segmentTypeId);
			} else if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH)) {// DASH
				decorator = SCTE224MCCReponseDecoratorFactory.getDashManifestResponseDector(schema, info, segmentTypeId);
			}
			break;

		default:
			if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HLS)) {
				decorator = OtherMCCReponseDecoratorFactory.getHLSManifestResponseDector(signal, hlsInterfaceType, signalId, acquisitionPoint);
			} else if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HSS)) {// HSS
				decorator = OtherMCCReponseDecoratorFactory.getHSSManifestResponseDector(signal, hssInterfaceType, signalId, acquisitionPoint);
			} else if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH)) {// DASH
				decorator = OtherMCCReponseDecoratorFactory.getDashManifestResponseDector(schema, info, acquisitionPoint);
			}
			break;
		}

		if (decorator != null) {
			return decorator;
		}
		throw new RuntimeException("unsupported");
	}

}
