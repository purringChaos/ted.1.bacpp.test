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

public class ProgramEndResponseProcessor extends tv.blackarrow.cpp.components.scc.scte224.response.common.ProgramEndResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ProgramEndResponseProcessor.class);

	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels,
			ResponseSignalType baseResponseSignal, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {

		Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel = null;
		if (matchedMediaObject == null) {
			decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", No Media was matched hence deleting this signal. ");
		} else {			

			String pts_adjustment = ptsAdjustments == null || ptsAdjustments.isEmpty() || StringUtils.isBlank(ptsAdjustments.get(baseResponseSignal.getAcquisitionSignalID()))
					? defautlPtsAdjustment : ptsAdjustments.get(baseResponseSignal.getAcquisitionSignalID());

			SegmentationDescriptorType baseSegment = findDescriptorFromRequest(SegmentType.PROGRAM_END, baseResponseSignal);
			//use the response signal coming as base
			//CS2 - 387: changes made here to remove therequestTime and updated with CurrentSystemTime With Delta By Avnee
			generateProgramEndSection(ptsTimes, pts_adjustment, aqpt, notificationResponse, ResponseSignalAction.REPLACE.toString(), matchedMediaLedger, SegmentType.PROGRAM_END,
					baseResponseSignal, currentSystemTimeWithAddedDelta, baseSegment);

			pts_adjustment = defautlPtsAdjustment;//Content Id was inserted by us, so it will always be default adjustment
			appendContentIdSection(ptsTimes, pts_adjustment, aqpt, notificationResponse, ResponseSignalAction.REPLACE, matchedMediaLedger,
					SegmentType.CONTENT_IDENTIFICATION, baseResponseSignal, baseSegment);
			generateConditioningInfo(notificationResponse, baseResponseSignal.getAcquisitionSignalID(), aqpt, SegmentType.PROGRAM_END, matchedMediaLedger);
			matchedMediaLedger.setMediaEndNotificationSent(true);	
			DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(matchedMediaLedger, aqpt.getAcquisitionPointIdentity(), matchedMediaObject.getSignalid());

			//Log in Audit Log(not the message log, that goes from final layer)
			logCompleteEventInAuditLog(notificationResponse, aqpt, context, requestTime);
			hostedAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(matchedMediaLedger, EventAction.COMPLETE, matchedMediaObject.getSignalid(),
					matchedMediaPointObject.getSignalId(), aqpt);
			
			
			LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity()
					+ ", Populated PROGRAM_END(Replace) and CONTENT_IDENTIFICATION(Replace) Response Signal. ");
		}
		if (hostedAppEventStatusNotifyModel != null) {
			hostedAppEventStatusNotifyModels.add(hostedAppEventStatusNotifyModel);
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
	}

}
