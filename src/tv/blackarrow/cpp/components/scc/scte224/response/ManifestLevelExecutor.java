package tv.blackarrow.cpp.components.scc.scte224.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.scc.scte224.response.common.IBaseResponseProcessor;
import tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.CombinedSignalResponseProcessor;
import tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ContentIDResponseProcessor;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SegmentType;

public class ManifestLevelExecutor extends SCCBaseExecutor {
	private static final Logger LOGGER = LogManager.getLogger(CombinedSignalResponseProcessor.class);

	@Override
	public IBaseResponseProcessor getResponseProcessor(SegmentType segmentTypeId, AcquisitionPoint aqpt, AcquiredSignal acquiredSignal) {
		IBaseResponseProcessor response = null;
		int numberOfSegmentationDescriptor = acquiredSignal.getSCTE35PointDescriptor() != null && acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null
				? acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().size()
				: 0;

		if (CppConstants.CADENT_OOH_ZONE.equalsIgnoreCase(aqpt.getZoneIdentity())) {
			if (numberOfSegmentationDescriptor == 1) {
				switch (segmentTypeId) {
				case PROGRAM_START:
				case PROGRAM_OVERLAP_START:
					response = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramStartResponseProcessor();
					break;

				case PROGRAM_END:
				case PROGRAM_EARLY_TERMINATION:
					response = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramEndResponseProcessor();
					break;

				case CONTENT_IDENTIFICATION:
					response = new ContentIDResponseProcessor();
					break;
				default:
					break;
				}
			} else if (numberOfSegmentationDescriptor > 1) {
				response = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.CombinedSignalResponseProcessor();
			}
		}

		if (response == null) {//Delete this unrecognized Signal
			LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", Unsupported SegmentType(" + segmentTypeId
					+ "), Deleting this signal. ");
			response = new tv.blackarrow.cpp.components.scc.scte224.response.common.DeleteResponseProcessor();
		}
		return response;
	}

}
