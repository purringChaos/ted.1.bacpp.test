package tv.blackarrow.cpp.components.scc.scte224.response.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.ResponseSignalAction;

public class DeleteResponseProcessor implements IBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(DeleteResponseProcessor.class);
	protected MediaLedger matchedMediaLedger;

	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notification, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels,
			ResponseSignalType baseResponseSignal, Date CurrentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {
		//Delete All Unknown Signal to which we do not handle.
		if (notification.getResponseSignal() != null && !notification.getResponseSignal().isEmpty()) {
			notification.getResponseSignal().get(0).setAction(ResponseSignalAction.DELETE.toString());
		}
	}

	@Override
	public void matchAcquiredSignal(MuleEventContext context, AcquiredSignal signal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date CurrentSystemTimeWithAddedDelta) {
		// NOT SUPPORTED

	}

	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short segmentTypeId) {
		// NOT SUPPORTED
		return null;
	}

	@Override
	public void saveConfirmedSignalIDInContextForSSR(String acquisitionSignalId, String acquisitionPointId, MuleEventContext context) {
		// NOT SUPPORTED

	}

	@Override
	public Long getDurationFromLedger(MediaLedger mediaLedger) {
		// NOT SUPPORTED
		return null;
	}


	@Override
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, MediaLedger mediaLedger) {
		// NOT SUPPORTED
		return null;
	}


	@Override
	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration, MediaLedger mediaLedger, AcquisitionPoint aqpt) {
		// TODO Auto-generated method stub
		return null;
	}

}
