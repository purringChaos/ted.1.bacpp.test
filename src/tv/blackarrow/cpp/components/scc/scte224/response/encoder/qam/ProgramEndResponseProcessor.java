package tv.blackarrow.cpp.components.scc.scte224.response.encoder.qam;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;

public class ProgramEndResponseProcessor extends tv.blackarrow.cpp.components.scc.scte224.response.common.ProgramEndResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramEndResponseProcessor.class);
	private static final String EMPTY_STRING = "";

	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels,
			ResponseSignalType baseResponseSignal, Date CurrentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(() -> "Program End Response--->");
		}
		final String acquisitionSignalId = acquiredSignal.getAcquisitionSignalID();
		Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel = null;
		if (matchedMediaObject == null) {
			decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", No Media was matched hence deleting this signal. ");
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> "Matched media = " + matchedMediaObject.getSignalid()
						+ ". Generating response with action replace/replace for Start/ContentID respectively, with Conditioning info section.");
			}
			String pts_adjustment = ptsAdjustments == null || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33) : ptsAdjustments.get(acquisitionSignalId);
			//use the response signal coming as base
			SegmentationDescriptorType baseSegment = findDescriptorFromRequest(SegmentType.PROGRAM_END, baseResponseSignal);

			//Step1: Program End Section
			generateProgramStartSection(ptsTimes, pts_adjustment, aqpt, ResponseSignalAction.REPLACE.toString(), matchedMediaLedger, SegmentType.PROGRAM_END,
					baseResponseSignal, CurrentSystemTimeWithAddedDelta, baseSegment);

			//Step2: Conditioning Info
			//generateConditioningInfo(notificationResponse, acquisitionSignalId, aqpt, matchedMediaLedger);
			matchedMediaLedger.setMediaEndNotificationSent(true);	
			DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(matchedMediaLedger, aqpt.getAcquisitionPointIdentity(), matchedMediaObject.getSignalid());

			//Step3: Populate alt content Identity in Program Start Section

			Map<QName, String> attri = acquiredSignal.getOtherAttributes();
			String zoneIdentity = attri != null && !attri.isEmpty() ? attri.get(new QName(CppConstants.SERVINCE_ZONE_IDENTITY_ATTRIBUTE)) : "";
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = addAlternateContentUrlForGivenZone(
					notificationResponse.getResponseSignal().get(0).getSignalPointID(), EMPTY_STRING, zoneIdentity);
			AltContentIdentityResponseModelDeltaInContext.putAll(AltContentIdentityResponseModelDelta);
			ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);

			PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notificationResponse, matchedMediaLedger.getSignalId());
			AuditLogger.auditLogConfirmedBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), matchedMediaLedger.getSignalId(), poisAuditLogVO);

			hostedAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(matchedMediaLedger, EventAction.COMPLETE, matchedMediaObject.getSignalid(),
					matchedMediaPointObject.getSignalId(), aqpt);
		}
		if (hostedAppEventStatusNotifyModel != null) {
			hostedAppEventStatusNotifyModels.add(hostedAppEventStatusNotifyModel);
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
		//return hostedAppEventStatusNotifyModel;
	}

	@Override
	public Long getDurationFromLedger(MediaLedger mediaLedger) {
		//In Case Of QAM Program End, Duration will be sent as 0
		return 0l;
	}

}
