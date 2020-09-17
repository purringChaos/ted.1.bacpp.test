package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

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
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventTransaction;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;

public class ContentIDResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ContentIDResponseProcessor.class);
	protected SchedulelessAltEventTransaction matchedScheduleslessAltEventLedger;
	public boolean cadentSignalMatched;

	@Override
	public void processSignal(MuleEventContext context, AcquiredSignal signal, AcquisitionPoint aqpt, SegmentationDescriptorType descriptorInfo, 
			SchedulelessAltEventLedger ledger, Date requestTime) {
		String signalIdRecievedInRequest = null;
		byte[] upid = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getUpid();
		if (upid != null) {
			signalIdRecievedInRequest = new String(upid);
			if (signalIdRecievedInRequest.startsWith(ESAMHelper.UPID_PREFIX)) {
				signalIdRecievedInRequest = signalIdRecievedInRequest.substring(signalIdRecievedInRequest.indexOf(ESAMHelper.UPID_PREFIX) + ESAMHelper.UPID_PREFIX.length());
			}
		}
		if (ledger != null) {
			if (ledger.getCadentSignalId().equals(signalIdRecievedInRequest)) {
				cadentSignalMatched = true;
			}
		}
		//Step3:Not saving this transaction to save some space on couchbase, We could chose to save later for logging purpose if needed
		if (cadentSignalMatched) {
			/*Long duration = 0l;
			saveLedgerInCouchbase(signal, aqpt, descriptorInfo, ledger, duration, requestTime);*/
		}
	}

	@Override
	public void generateResponse(SignalProcessingNotificationType notificationResponse, ResponseSignalType responseSignal, AcquisitionPoint aqpt,
			SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			SchedulelessAltEventLedger ledger, Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, Date requestTime) throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(()->"Content ID Signal Response--->");
		}
		//1. Did not find this content id in our system then apply Acquisition Point Settings
		if (ledger == null || !cadentSignalMatched) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(()->"Scheduleless Alt Event is running.Sending response based on Acquisition Point InBand content Id setting.");
			}
			if (aqpt.getInBandContentIdConfiguredValue() == SignalHandlingConfiguration.PRESERVE) {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.NOOP);
			} else {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			}
		} else if (ledger != null && cadentSignalMatched)//2. Found this content id in our system then NOOP if schedule alt event running else delete if it has ended
		{
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(()->"Scheduleless Alt Event  = " + ledger.getCadentSignalId()
						+ ". Generating response 'noop' if Scheduleless Alt Event has not ended else 'delete' if Scheduleless Alt Event has ended.");
			}
			if (ledger.isAltEventStarted() && !ledger.isAltEventEnded()) {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.NOOP);
			} else if (ledger.isAltEventEnded()) {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			}
		} else {
			decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.NOOP);
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);

	}

	@Override
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, SchedulelessAltEventLedger schedulelessAltEventLedger) {
		// TODO Auto-generated method stub
		return null;
	}

}
