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
import tv.blackarrow.cpp.utils.JavaxUtil;

/**
 * @author pzhang
 *
 */
public class TWCHSSManifestResponseDecorator extends HSSManifestResponseDecoratorBase {
	
	public static String SPARSE_TRACK_NAME = "admarker";
	
	@Override
	public void decorateManifestResponse(final ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception  {
		response.setDataPassThrough(false);
		Integer largestDuration = info.getLargestDuration();
		if(cpo!=null && cpo.isAborted()) {
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
			largestDuration = (int)cpo.getRemainingDuration();
		}
		String adMarkerContent = buildResponseAcquiredSignalForHSS(signal, info.getSignalId(), largestDuration, cpo, info.getSchema());
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName(getSparseTrackName());
		sparseTrack.setValue(adMarkerContent.getBytes());
		response.getSparseTrack().add(sparseTrack);
	}
	
	protected String getSparseTrackName(){
		return SPARSE_TRACK_NAME;
	}
	
}
