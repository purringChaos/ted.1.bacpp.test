package tv.blackarrow.cpp.components.scc.scte224.response.encoder.qam;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.ApplyorRemove;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
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

public class ProgramStartResponseProcessor extends tv.blackarrow.cpp.components.scc.scte224.response.common.ProgramStartResponseProcessor {

	private static final Logger LOGGER = LogManager.getLogger(ProgramStartResponseProcessor.class);
	private String alternateSrcURLIfZoneMatched;

	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels,
			ResponseSignalType baseResponseSignal, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {

		final String acquisitionSignalId = acquiredSignal.getAcquisitionSignalID();
		Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		HostedAppEventStatusScte224NotifyModel hostedAppEventStatusNotifyModel = null;
		if (matchedMediaObject == null || matchedMediaObject==null) {
			decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", No Media was matched hence deleting this signal. ");
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(()->"Matched media = " + matchedMediaObject.getSignalid()
						+ ". Generating response with action replace/create for Start/ContentID respectively, with Conditioning info section.");
			}

			//Step0: Diagnose the impact of ServizeZone.
			Map<QName, String> attri = acquiredSignal.getOtherAttributes();
			String zoneIdentity = attri != null && !attri.isEmpty() ? attri.get(new QName(CppConstants.SERVINCE_ZONE_IDENTITY_ATTRIBUTE)) : "";

			ServiceZoneResponseImpact serviceZoneImpact = identifyServiceZoneImpact(zoneIdentity, matchedMediaPointObject);
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = null;
			switch (serviceZoneImpact) {
			case MATCHED_ZONE:
				addProgramStartSection(notificationResponse, aqpt, acquisitionSignalId, ptsTimes, ptsAdjustments, currentSystemTimeWithAddedDelta);
				//Option1: Send matched Zones
				AltContentIdentityResponseModelDelta = addAlternateContentUrlForGivenZone(notificationResponse.getResponseSignal().get(0).getSignalPointID(), alternateSrcURLIfZoneMatched,
						zoneIdentity);
				AltContentIdentityResponseModelDeltaInContext.putAll(AltContentIdentityResponseModelDelta);
				ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
				matchedMediaLedger.setMediaStartNotificationSent(true);	
				DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(matchedMediaLedger, aqpt.getAcquisitionPointIdentity(), matchedMediaObject.getSignalid());
				//Audit Log
				auditLog(notificationResponse, aqpt, context, requestTime, matchedMediaLedger);
				//Hosted Notification
				hostedAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(matchedMediaLedger, EventAction.CONFIRMED, matchedMediaObject.getSignalid(), matchedMediaPointObject.getSignalId(), aqpt);
				break;
			case NO_ZONE_RECIEVED:
				addProgramStartSection(notificationResponse, aqpt, acquisitionSignalId, ptsTimes, ptsAdjustments, currentSystemTimeWithAddedDelta);
				//Option2: Send All Zones
				AltContentIdentityResponseModelDelta = addAlternateContentUrlForAllZones(notificationResponse.getResponseSignal().get(0).getSignalPointID(), matchedMediaPointObject.getApply());
				AltContentIdentityResponseModelDeltaInContext.putAll(AltContentIdentityResponseModelDelta);
				ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
				matchedMediaLedger.setMediaStartNotificationSent(true);	
				DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(matchedMediaLedger, aqpt.getAcquisitionPointIdentity(), matchedMediaObject.getSignalid());
				//Audit Log
				auditLog(notificationResponse, aqpt, context, requestTime, matchedMediaLedger);
				//Hosted Notification
				hostedAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(matchedMediaLedger, EventAction.CONFIRMED, matchedMediaObject.getSignalid(), matchedMediaPointObject.getSignalId(), aqpt);
				break;

			default:
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
				AuditLogHelper.populateAuditLogVO(context, notificationResponse, "");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No Media was matched.Deleting this signal.");
				}
				break;

			}
		}
		if(hostedAppEventStatusNotifyModel!=null) {
			hostedAppEventStatusNotifyModels.add(hostedAppEventStatusNotifyModel);
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
		//return hostedAppEventStatusNotifyModel;
	}

	private void auditLog(SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt, MuleEventContext context, Date requestTime, MediaLedger mediaLedger) {
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notificationResponse, mediaLedger.getSignalId());
		AuditLogger.auditLogConfirmedBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), mediaLedger.getSignalId(), poisAuditLogVO);
	}

	private void addProgramStartSection(SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt,
			String acquisitionSignalId, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {		
		//Step1: Program Start Section
		List<ResponseSignalType> responseSignalTypes = notificationResponse.getResponseSignal();
		ResponseSignalType baseResponseSignal = responseSignalTypes.get(0);//use the response signal coming as base
		String pts_adjustment = ptsAdjustments == null || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33) : ptsAdjustments.get(acquisitionSignalId);
		SegmentationDescriptorType baseSegment = findDescriptorFromRequest(SegmentType.PROGRAM_START, baseResponseSignal);

		generateProgramStartSection(ptsTimes, pts_adjustment, aqpt, ResponseSignalAction.REPLACE.toString(),
				matchedMediaLedger, SegmentType.PROGRAM_START, baseResponseSignal, currentSystemTimeWithAddedDelta, baseSegment);
		//Step2: Conditioning Info
		//generateConditioningInfo(notificationResponse, acquisitionSignalId, aqpt, matchedMediaLedger);
	}

	private ServiceZoneResponseImpact identifyServiceZoneImpact(String zoneIdentity, MediaPoint matchedMediaPointObject) {

		if (StringUtils.isBlank(zoneIdentity)) {
			return ServiceZoneResponseImpact.NO_ZONE_RECIEVED;
		} else {
			alternateSrcURLIfZoneMatched = findAlternateURLForAskedZone(matchedMediaPointObject, zoneIdentity);
			if (StringUtils.isNotBlank(alternateSrcURLIfZoneMatched)) {
				return ServiceZoneResponseImpact.MATCHED_ZONE;
			} else {
				return ServiceZoneResponseImpact.UNMATCHED_ZONE;
			}
		}
	}

	private String findAlternateURLForAskedZone(MediaPoint matchedMediaPointObject, String zoneIdentity) {
		if (matchedMediaPointObject != null && matchedMediaPointObject.getApply() != null) {
			for (ApplyorRemove apply : matchedMediaPointObject.getApply()) {
				if (apply.getServiceZones() != null) {
					Predicate<String> p1 = str -> zoneIdentity.equalsIgnoreCase(str);
					boolean containsZone = apply.getServiceZones().stream().anyMatch(p1);
					if (containsZone) {
						return apply.getAltSourceValue();
					}
				}
			}
		}
		return null;
	}

}
