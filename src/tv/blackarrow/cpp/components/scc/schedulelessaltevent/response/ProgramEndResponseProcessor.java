package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventTransaction;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class ProgramEndResponseProcessor extends SCCBaseResponseProcessor {
	private static final String EMPTY_STRING = "";
	private static final Logger LOGGER = LogManager.getLogger(ProgramEndResponseProcessor.class);

	@Override
	public void processSignal(MuleEventContext context, AcquiredSignal signal, AcquisitionPoint aqpt, SegmentationDescriptorType descriptorInfo,
			SchedulelessAltEventLedger ledger, Date requestTime) {
		if (ledger != null) {
			ledger.setAltEventEnded(true);
			Long duration = requestTime.getTime() - ledger.getProgramStartSignalTransaction(ESSRequestType.SCC).getSignalTimeInMS();
			saveLedgerInCouchbase(aqpt, descriptorInfo, ledger, duration, requestTime);
		}
	}

	@Override
	public void generateResponse(SignalProcessingNotificationType notificationResponse, ResponseSignalType firstResponseSection, AcquisitionPoint aqpt,
			SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			SchedulelessAltEventLedger ledger, Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap, Date requestTime) throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(()->"Program End Response--->");
		}

		if (ledger != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(()->"Ending Cadent Signal Id: " + ledger.getCadentSignalId()
						+ ", Generating response with action replace/replace for Start/ContentID respectively, with Conditioning info section.");
			}

			generateProgramStartSection(requestSegmentTypeIdEnum, acquisitionSignalId, ptsTimes, ptsAdjustments, aqpt, firstResponseSection,
					ResponseSignalAction.REPLACE.toString(), ledger, SegmentType.PROGRAM_START);
			generateContentIdSection(requestSegmentTypeIdEnum, acquisitionSignalId, ptsTimes, ptsAdjustments, aqpt, notificationResponse, firstResponseSection,
					ResponseSignalAction.REPLACE, ledger, SegmentType.CONTENT_IDENTIFICATION);
			generateConditioningInfo(notificationResponse, acquisitionSignalId, aqpt, requestSegmentTypeIdEnum, ledger);

			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = addAlternateContentUrls(firstResponseSection.getSignalPointID(), EMPTY_STRING);

			responseToI03ResponseModelDeltaMap.putAll(AltContentIdentityResponseModelDelta);

			ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
		}

	}

	@Override
	public Long getDurationFromLedger(SchedulelessAltEventLedger schedulelessAltEventLedger) {
		Long timeInMillis = null;
		if (schedulelessAltEventLedger != null && schedulelessAltEventLedger.getSignalTransactions() != null && !schedulelessAltEventLedger.getSignalTransactions().isEmpty()) {
			SchedulelessAltEventTransaction programEndTransaction = schedulelessAltEventLedger.getProgramEndSignalTransaction(ESSRequestType.SCC);
			if (programEndTransaction != null) {
				timeInMillis = programEndTransaction.getTotalDurationInMS();
			}
		}
		return timeInMillis;
	}

	/*here Start time for event schedule could be current time.
	 * (non-Javadoc)
	 * @see tv.blackarrow.cpp.components.scc.schedulelessaltevent.response.IBaseResponseProcessor#getStartTimeFromLedgerForEventSchedule(tv.blackarrow.cpp.model.AcquisitionPoint, tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger)
	 */
	@Override
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, SchedulelessAltEventLedger schedulelessAltEventLedger) {
		Long timeInMillis = null;
		if (schedulelessAltEventLedger != null && schedulelessAltEventLedger.getSignalTransactions() != null && !schedulelessAltEventLedger.getSignalTransactions().isEmpty()) {
			SchedulelessAltEventTransaction programEndTransaction = schedulelessAltEventLedger.getProgramEndSignalTransaction(ESSRequestType.SCC);
			if (programEndTransaction != null) {
				long currentTimeInMillis = System.currentTimeMillis();
				timeInMillis = programEndTransaction.getSignalTimeInMS();
				if (currentTimeInMillis < timeInMillis) {
					timeInMillis = currentTimeInMillis;
				} else {
					timeInMillis = applyAQSignalOffsetInEventStartTime(aqpt, timeInMillis);
				}
			}
		}
		return timeInMillis;
	}

}
