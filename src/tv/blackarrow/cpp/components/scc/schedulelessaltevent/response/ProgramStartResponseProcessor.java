package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventTransaction;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class ProgramStartResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramStartResponseProcessor.class);

	@Override
	public void processSignal(MuleEventContext context, AcquiredSignal signal, AcquisitionPoint aqpt, SegmentationDescriptorType descriptorInfo, 
			SchedulelessAltEventLedger ledger, Date requestTime) {
		ledger.setAltEventStarted(true);
		Long duration = null;//Should be null as Program Start doesn't define duration, rather it will be calculated at program end
		saveLedgerInCouchbase(aqpt, descriptorInfo, ledger, duration, requestTime);
	}

	@Override
	public void generateResponse(SignalProcessingNotificationType notificationResponse, ResponseSignalType programStartResponse16Section, AcquisitionPoint aqpt,
			SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			SchedulelessAltEventLedger ledger, Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap, Date requestTime)
			throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(()->"Program Start Response--->");
		}

		if (ledger != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(()->"Generating response with action replace/create for Start/ContentID respectively, with Conditioning info section.");
			}

			generateProgramStartSection(requestSegmentTypeIdEnum, acquisitionSignalId, ptsTimes, ptsAdjustments, aqpt, programStartResponse16Section,
					ResponseSignalAction.REPLACE.toString(), ledger, SegmentType.PROGRAM_START);
			generateContentIdSection(requestSegmentTypeIdEnum, acquisitionSignalId, ptsTimes, ptsAdjustments, aqpt, notificationResponse, programStartResponse16Section,
					ResponseSignalAction.CREATE, ledger, SegmentType.CONTENT_IDENTIFICATION);
			generateConditioningInfo(notificationResponse, acquisitionSignalId, aqpt, requestSegmentTypeIdEnum, ledger);

			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = addAlternateContentUrls(programStartResponse16Section.getSignalPointID(),
					aqpt.getSchedulelessAlternateContentLocation());
			responseToI03ResponseModelDeltaMap.putAll(AltContentIdentityResponseModelDelta);
			ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);

			PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notificationResponse, ledger.getCadentSignalId());

			AuditLogger.auditLogConfirmedBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), ledger.getCadentSignalId(), poisAuditLogVO);
		}

	}

	/*Here the start time should be same as Start time in Program Start section
	 * (non-Javadoc)
	 * @see tv.blackarrow.cpp.components.scc.schedulelessaltevent.response.IBaseResponseProcessor#getStartTimeFromLedgerForEventSchedule(tv.blackarrow.cpp.model.AcquisitionPoint, tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger)
	 */
	@Override
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, SchedulelessAltEventLedger schedulelessAltEventLedger) {
		Long timeInMillis = null;
		if ((schedulelessAltEventLedger != null) && (schedulelessAltEventLedger.getSignalTransactions() != null) && !schedulelessAltEventLedger.getSignalTransactions().isEmpty()) {
			SchedulelessAltEventTransaction programStartTransaction = schedulelessAltEventLedger.getProgramStartSignalTransaction(ESSRequestType.SCC);
			if (programStartTransaction != null) {
				timeInMillis = programStartTransaction.getSignalTimeInMS();
				timeInMillis = applyAQSignalOffsetInEventStartTime(aqpt, timeInMillis);
				timeInMillis = timeInMillis + (aqpt.getContentIDFrequency() * 1000); // need add interval
			}
		}
		return timeInMillis;
	}

}
