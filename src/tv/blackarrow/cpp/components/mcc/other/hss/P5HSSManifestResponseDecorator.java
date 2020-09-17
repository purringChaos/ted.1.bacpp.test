/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hss;

import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SparseTrackType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;

/**
 * @author pzhang
 *
 */
public class P5HSSManifestResponseDecorator extends HSSManifestResponseDecoratorBase {

	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final ConfirmedPlacementOpportunity cpo,ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception  {
		response.setDataPassThrough(false);
		String adMarkerContent = buildResponseAcquiredSignalForHSS(signal, info.getSignalId(), info.getSchema());
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName("admarker");
		sparseTrack.setValue(adMarkerContent.getBytes());
		response.getSparseTrack().add(sparseTrack);
	}

}
