package tv.blackarrow.cpp.components.mcc.scte224.hss;

import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SparseTrackType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public class MediaMCCCustomHSSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	public static String SPARSE_TRACK_NAME = "blackout";

	protected String getSparseTrackName() {
		return SPARSE_TRACK_NAME;
	}

	protected String getHssStreamTimeAbortActionValue() {
		return HSS_BLACKOUT_SIGNAL_ABORT_STREAM_TIME_VALUE;
	}

	@Override
	public void decorateManifestResponse(ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal, final MediaLedger cpo, ManifestInfo info,
			AcquisitionPoint apqt, Short segmentTypeId) throws Exception {
		response.setDataPassThrough(false);
		String adMarkerContent = buildResponseAcquiredSignalForHSS(response, signal, info.getSignalId(), cpo, info.getSchema(), apqt);
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName(getSparseTrackName());
		sparseTrack.setValue(adMarkerContent.getBytes());
		response.getSparseTrack().add(sparseTrack);
	}	

	protected String buildResponseAcquiredSignalForHSS(ManifestResponseType response, ManifestConfirmConditionEventType.AcquiredSignal signal, String signalId, MediaLedger confirmedPlacementOpportunity, Schema schema, AcquisitionPoint apqt) {
		AcquisitionPointInfoType acqSignal = new AcquisitionPointInfoType();
		acqSignal.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		acqSignal.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		setUtcPointInAcquiredSignal(response, acqSignal);
		acqSignal.setStreamTimes(signal.getStreamTimes());
		adjustSignalTimeOffset(acqSignal.getStreamTimes(), apqt);
		
		// Let's add SignalId to the stream Type
		StreamTimeType signalStreamTime = new StreamTimeType();
		signalStreamTime.setTimeType("SignalId");
		signalStreamTime.setTimeValue(signalId);
		acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);

		signalStreamTime = new StreamTimeType();
		signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
		signalStreamTime.setTimeValue(ALT_CONTENT);
		signal.getStreamTimes().getStreamTime().add(signalStreamTime);

		return objectToXML(acqSignal, schema);
	}

	protected String objectToXML(final AcquisitionPointInfoType acqSignal, Schema schema) {

		return schema.getResponseHandler().generateHSSSparseTrack(acqSignal);
	}
}
