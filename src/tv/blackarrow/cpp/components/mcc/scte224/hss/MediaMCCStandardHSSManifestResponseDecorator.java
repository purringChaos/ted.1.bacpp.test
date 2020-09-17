/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.scte224.hss;

import java.net.URLEncoder;
import java.util.List;

import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SparseTrackType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signal.signaling.ExtType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.ext.AP;
import tv.blackarrow.cpp.signaling.ext.Duration;
import tv.blackarrow.cpp.signaling.ext.Feed;
import tv.blackarrow.cpp.signaling.ext.Network;
import tv.blackarrow.cpp.signaling.ext.UTC;

/**
 * @author pzhang
 *
 */
public class MediaMCCStandardHSSManifestResponseDecorator extends AbstractManifestResponseDecorator {

	@Override
	public void decorateManifestResponse(ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal, final MediaLedger cpo, ManifestInfo info,
			AcquisitionPoint acquisitionPoint, Short segmentTypeId) throws Exception {
		AcquisitionPointInfoType acqSignal = new AcquisitionPointInfoType();
		acqSignal.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		acqSignal.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		setUtcPointInAcquiredSignal(response, acqSignal);
		acqSignal.setStreamTimes(signal.getStreamTimes());
		adjustSignalTimeOffset(acqSignal.getStreamTimes(), acquisitionPoint);
		
		// Let's add SignalId to the stream Type
		StreamTimeType signalStreamTime = new StreamTimeType();
		signalStreamTime.setTimeType("SignalId");
		signalStreamTime.setTimeValue(info.getSignalId());
		acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);

		Duration durationEle = new Duration();
		durationEle.setValue(URLEncoder.encode(response.getDuration().toString(), "UTF-8"));

		if (cpo.isMediaStarted()) {
			signalStreamTime = new StreamTimeType();
			signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
			signalStreamTime.setTimeValue(ALT_CONTENT);
			acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
		}

//		populateDuration(response, cpo);

		// Ext Type
		// Let's add Ext Elements
		ExtType extType = new ExtType();
		List<Object> elements = extType.getAny();
		Feed feedEle = new Feed();
		feedEle.setValue(URLEncoder.encode(info.getFeed(), "UTF-8"));
		Network networkEle = new Network();
		networkEle.setValue(URLEncoder.encode(info.getNetwork(), "UTF-8"));
		AP apEle = new AP();
		apEle.setValue(URLEncoder.encode(info.getApid(), "UTF-8"));
		UTC utcEle = new UTC();
		utcEle.setValue(info.getUtc());

		elements.add(feedEle);
		elements.add(networkEle);
		elements.add(apEle);
		elements.add(utcEle);
		elements.add(durationEle);
		acqSignal.setExt(extType);

		String adMarkerContent = objectToXML(acqSignal, info.getSchema());
		SparseTrackType sparseTrack = new SparseTrackType();
		sparseTrack.setTrackName("admarker");
		sparseTrack.setValue(adMarkerContent.getBytes());
		response.getSparseTrack().add(sparseTrack);
	}

	protected String objectToXML(final AcquisitionPointInfoType acqSignal, Schema schema) {

		return schema.getResponseHandler().generateHSSSparseTrack(acqSignal);
	}

}
