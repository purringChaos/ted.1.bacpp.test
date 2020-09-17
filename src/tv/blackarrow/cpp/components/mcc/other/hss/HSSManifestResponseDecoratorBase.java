/**
 * 
 */
package tv.blackarrow.cpp.components.mcc.other.hss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.mcc.AbstractManifestResponseDecorator;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.AcquisitionPointInfoType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;

/**
 * @author pzhang
 *
 */
public abstract class HSSManifestResponseDecoratorBase extends AbstractManifestResponseDecorator {
	private static final Logger log = LogManager.getLogger(HSSManifestResponseDecoratorBase.class);

	protected String buildResponseAcquiredSignalForHSS(ManifestConfirmConditionEventType.AcquiredSignal signal, String signalId, 
			Integer duration, ConfirmedPlacementOpportunity confirmedPlacementOpportunity, Schema schema) {
		AcquisitionPointInfoType acqSignal = new AcquisitionPointInfoType();
		acqSignal.setAcquisitionPointIdentity(signal
				.getAcquisitionPointIdentity());
		acqSignal.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		acqSignal.setUTCPoint(signal.getUTCPoint());
		acqSignal.setStreamTimes(signal.getStreamTimes());

		// Let's add SignalId to the stream Type
		StreamTimeType signalStreamTime = new StreamTimeType();
		signalStreamTime.setTimeType("SignalId");
		signalStreamTime.setTimeValue(signalId);
		acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
		
		// Let's add Duration to the stream Type if not null
		if (duration != null) {
			signalStreamTime = new StreamTimeType();
			signalStreamTime.setTimeType("Duration");
			signalStreamTime.setTimeValue(duration.toString());
			acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
		}
		
		//If this PO was aborted then add a stream time with timeType = "Action"; timeValue = "DAI_ABORT"
		if(confirmedPlacementOpportunity !=null && confirmedPlacementOpportunity.isAborted()) {
			signalStreamTime = new StreamTimeType();
			signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
			signalStreamTime.setTimeValue(getHssStreamTimeAbortActionValue());
			acqSignal.getStreamTimes().getStreamTime().add(signalStreamTime);
		}
		
		BlackoutEvent event = DataManagerFactory.getInstance().getSingleBlackoutEvent(confirmedPlacementOpportunity.getSignalId());
		if(event != null){
			signalStreamTime = new StreamTimeType();
			signalStreamTime.setTimeType(HSS_SIGNAL_STREAM_TIME_ACTION_TYPE);
			signalStreamTime.setTimeValue(event.getEventTypeName());
			signal.getStreamTimes().getStreamTime().add(signalStreamTime);
		}
		
		return objectToXML(acqSignal, schema);
	}

	protected String buildResponseAcquiredSignalForHSS(ManifestConfirmConditionEventType.AcquiredSignal signal,String signalId, Schema schema) {
		return buildResponseAcquiredSignalForHSS(signal, signalId, null, null, schema);
	}
		
	protected String objectToXML(final AcquisitionPointInfoType acqSignal, Schema schema) {
//		JAXBContext jaxbCxt;
//		JAXBElement<AcquisitionPointInfoType> jxbElement = null;
//		StringWriter writer = new StringWriter();
//
//		try {
//			jaxbCxt = JAXBUtil.getLinearPOISMccJAXBContext();
//			Marshaller marshaller = jaxbCxt.createMarshaller();
//			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
//			tv.blackarrow.cpp.signal.signaling.ObjectFactory factory = new tv.blackarrow.cpp.signal.signaling.ObjectFactory();
//			jxbElement = factory.createAcquiredSignal(acqSignal);
//			marshaller.marshal(jxbElement, writer);
//		} catch (JAXBException e) {
//			log.error(e.getMessage());
//		}
//		return writer.toString();
		return schema.getResponseHandler().generateHSSSparseTrack(acqSignal);
	}
	
	protected String getHssStreamTimeAbortActionValue(){
		return HSS_SIGNAL_ABORT_STREAM_TIME_VALUE;
	}


}
