/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hss;

import java.net.URLEncoder;
import java.util.List;

import tv.blackarrow.cpp.components.Scte35Contants;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.manifest.SparseTrackType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signal.signaling.ExtType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signaling.ext.AP;
import tv.blackarrow.cpp.signaling.ext.Duration;
import tv.blackarrow.cpp.signaling.ext.Feed;
import tv.blackarrow.cpp.signaling.ext.Network;
import tv.blackarrow.cpp.signaling.ext.UTC;
import tv.blackarrow.cpp.utils.JavaxUtil;

/**
 * @author pzhang
 *
 */
public class HSSManifestResponseDecorator extends HSSManifestResponseDecoratorBase {

	@Override
	public void decorateManifestResponse(ManifestResponseType response, final ManifestConfirmConditionEventType.AcquiredSignal signal,
			final ConfirmedPlacementOpportunity cpo, ManifestInfo info, AcquisitionPoint acquisitionPoint) throws Exception  {
		AcquisitionPointInfoType acqSignal = new AcquisitionPointInfoType();
		acqSignal.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		acqSignal.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		acqSignal.setUTCPoint(signal.getUTCPoint());
		acqSignal.setStreamTimes(signal.getStreamTimes());

		// Let's add SignalId to the stream Type
		StreamTimeType signalStreamTime = new StreamTimeType();
		signalStreamTime.setTimeType("SignalId");
		signalStreamTime.setTimeValue(info.getSignalId());
		acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
		
		
		Duration durationEle = new Duration();
		durationEle.setValue(URLEncoder.encode(response.getDuration().toString(), "UTF-8"));
		
		BlackoutEvent event = DataManagerFactory.getInstance().getSingleBlackoutEvent(cpo.getSignalId());
		boolean isBlackout = event !=null?true:false;
		
		//If this PO was aborted then add a stream time with timeType = Action; timeValue = "DAI_ABORT"
		if(cpo!=null && cpo.isAborted()) {
			signalStreamTime = new StreamTimeType();
			signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
			if(isBlackout){
				signalStreamTime.setTimeValue(HSS_SIGNAL_ABORT_STREAM_TIME_VALUE);
			}else{
				signalStreamTime.setTimeValue(HSS_BLACKOUT_SIGNAL_ABORT_STREAM_TIME_VALUE);
			}
			
			acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(cpo.getRemainingDuration()));
		}else{
			
			if(isBlackout){
				signalStreamTime = new StreamTimeType();
				signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
				signalStreamTime.setTimeValue(event.getEventTypeName());
				acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
			}
			
			try{
				// content id
				if(signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId() == Scte35Contants.SEGMENTATION_TYPE_CONTENT_IDENT){
					long duration = BlackoutEvent.getActualBlackoutStopTime(cpo, event) - signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime().getTime();
					if(duration < 0) {
						duration = 0;
					}
					durationEle.setValue(URLEncoder.encode(JavaxUtil.getDatatypeFactory().newDuration(duration).toString(), "UTF-8"));
					response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
				}
			}catch(Exception e){}
		}

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

}
