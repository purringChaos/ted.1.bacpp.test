package tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class ProgramStartResponseProcessor extends tv.blackarrow.cpp.components.scc.scte224.response.common.ProgramStartResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramStartResponseProcessor.class);

	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels,
			ResponseSignalType baseResponseSignal, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {

		HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel = null;
		Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		if (matchedMediaObject == null) {
			decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", No Media was matched hence deleting this signal. ");
		} else {

			String pts_adjustment = ptsAdjustments == null || ptsAdjustments.isEmpty() || StringUtils.isBlank(ptsAdjustments.get(baseResponseSignal.getAcquisitionSignalID()))
					? defautlPtsAdjustment : ptsAdjustments.get(baseResponseSignal.getAcquisitionSignalID());

			SegmentationDescriptorType baseSegment = findDescriptorFromRequest(SegmentType.PROGRAM_START, baseResponseSignal);
			generateProgramStartSection(ptsTimes, pts_adjustment, aqpt, ResponseSignalAction.REPLACE.toString(), matchedMediaLedger,
					SegmentType.PROGRAM_START,
					baseResponseSignal, currentSystemTimeWithAddedDelta, baseSegment);

			pts_adjustment = defautlPtsAdjustment;//Content Id was inserted by us, so it will always be default adjustment
			appendContentIdSection(ptsTimes, pts_adjustment, aqpt, notificationResponse, ResponseSignalAction.CREATE, matchedMediaLedger,
					SegmentType.CONTENT_IDENTIFICATION, baseResponseSignal, baseSegment);
			generateConditioningInfo(notificationResponse, baseResponseSignal.getAcquisitionSignalID(), aqpt, SegmentType.PROGRAM_START, matchedMediaLedger);
			
			matchedMediaLedger.setMediaStartNotificationSent(true);	
			DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(matchedMediaLedger, aqpt.getAcquisitionPointIdentity(), matchedMediaObject.getSignalid());

			hostedAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(matchedMediaLedger, EventAction.CONFIRMED, matchedMediaObject.getSignalid(),
					matchedMediaPointObject.getSignalId(), aqpt);
			
			//Log in Audit Log(not the message log, that goes from final layer)
			logConfirmEventInAuditLog(notificationResponse, aqpt, context, requestTime);
			
			LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity()
					+ ", Populated PROGRAM_START(Replace) and CONTENT_IDENTIFICATION(Created) Response Signal. ");
		}
		if (hostedAppEventStatusNotifyModel != null) {
			hostedAppEventStatusNotifyModels.add(hostedAppEventStatusNotifyModel);
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
		//return hostedAppEventStatusNotifyModel;
	}
}
