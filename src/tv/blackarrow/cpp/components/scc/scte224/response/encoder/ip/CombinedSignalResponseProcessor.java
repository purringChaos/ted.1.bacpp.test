package tv.blackarrow.cpp.components.scc.scte224.response.encoder.ip;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.scc.scte224.response.common.SCCBaseResponseProcessor;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
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
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class CombinedSignalResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(CombinedSignalResponseProcessor.class);
	protected Map<Short, MediaPoint> segmentTypeToMatchedMediaPointMap = new HashMap<>();
	protected Map<Short, Media> segmentTypeToMatchedMediaMap = new HashMap<>();
	protected Map<Short, MediaLedger> segmentTypeToMatchedMediaLedgerMap = new HashMap<>();

	@Override
	public void matchAcquiredSignal(MuleEventContext context, AcquiredSignal signal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date currentSystemTimeWithAddedDelta) {
		boolean isBasicValidationPassed = validateBasicFields(context, signal, medias, acquisitionPointIdentity);
		if (!isBasicValidationPassed) {
			return;
		}

		Date requestTime = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		
		/**
		 * Sorting so that the program end segmentation descriptors gets processed first.
		 */
		Collections.sort(signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo(), 
				Comparator.comparing(SegmentationDescriptorType::getSegmentTypeId).reversed());
		List<SegmentationDescriptorType> segmentationDescriptorTypes = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo();
		
		boolean isAnyProgramEndConfirmed = false;
		for (SegmentationDescriptorType segmentationDescriptorType : segmentationDescriptorTypes) {
			String acquisitionSignalID = signal.getAcquisitionSignalID();
			if(isAnyProgramEndConfirmed) {
				acquisitionSignalID = UUIDUtils.getBase64UrlEncodedUUID();
			}
			matchBySegmenationDescritorType(acquisitionSignalID, medias, acquisitionPointIdentity, requestTime, 
					segmentationDescriptorType, currentSystemTimeWithAddedDelta);			
			if (matchedMediaObject != null && matchedMediaPointObject != null) {
				if(SegmentType.isProgramEndSignal(segmentationDescriptorType.getSegmentTypeId()) || 
						SegmentType.isProgramEarlyTerminationSignal(segmentationDescriptorType.getSegmentTypeId())) {
					isAnyProgramEndConfirmed = true;
				}
				segmentTypeToMatchedMediaPointMap.put(segmentationDescriptorType.getSegmentTypeId(), matchedMediaPointObject);
				segmentTypeToMatchedMediaMap.put(segmentationDescriptorType.getSegmentTypeId(), matchedMediaObject);
				segmentTypeToMatchedMediaLedgerMap.put(segmentationDescriptorType.getSegmentTypeId(), matchedMediaLedger);

				matchedMediaPointObject = null;//Resetting for next loop
				matchedMediaObject = null;//Resetting for next loop
				matchedMediaLedger = null;//Resetting for next loop
			}
		}
	}
	
	
	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notification, AcquisitionPoint aqpt,
			HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModel,
			ResponseSignalType baseResponseSignal,Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(() -> "Program Start Response--->");
		}
		Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		MediaPoint programEndMediaPoint = segmentTypeToMatchedMediaPointMap.get(SegmentType.PROGRAM_END.getSegmentTypeId());
		MediaPoint programStartMediaPoint = segmentTypeToMatchedMediaPointMap.get(SegmentType.PROGRAM_START.getSegmentTypeId());
		Media programEndMedia = segmentTypeToMatchedMediaMap.get(SegmentType.PROGRAM_END.getSegmentTypeId());
		Media programStartMedia = segmentTypeToMatchedMediaMap.get(SegmentType.PROGRAM_START.getSegmentTypeId());
		MediaLedger programEndMediaLedger = segmentTypeToMatchedMediaLedgerMap.get(SegmentType.PROGRAM_END.getSegmentTypeId());
		MediaLedger programStartMediaLedger = segmentTypeToMatchedMediaLedgerMap.get(SegmentType.PROGRAM_START.getSegmentTypeId());
		

		if (segmentTypeToMatchedMediaPointMap == null || segmentTypeToMatchedMediaPointMap.size() == 0 || segmentTypeToMatchedMediaMap == null
				|| segmentTypeToMatchedMediaMap.size() == 0 || (programStartMediaPoint == null && programEndMediaPoint == null)) {
			decorateNoOPorDeleteResponse(notification, ResponseSignalAction.DELETE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> "No Media was matched.Sending DELETE reponse");
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> "Generating response with action replace/create for Start/ContentID respectively, with Conditioning info section.");
			}

			List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
			String altContentIndentity = "";
			if ((programStartMediaPoint != null && programEndMediaPoint == null) || (programStartMediaPoint != null && programEndMediaPoint != null)) {
				altContentIndentity = programStartMediaPoint.getAltSourceValue();
			}

			// add alt content
			addAltContent(notification, aqpt, AltContentIdentityResponseModelDeltaInContext, altContentIndentity, responseSignalTypes, ptsTimes, ptsAdjustments,
					currentSystemTimeWithAddedDelta, acquiredSignal.getAcquisitionSignalID());

			if (programEndMedia != null && programEndMediaPoint!=null) {
				buildHostedAppEventStatusAndLogForAuditing(notification, context, requestTime, aqpt, programEndMedia, programEndMediaPoint, programEndMediaLedger, EventAction.COMPLETE,
						hostedAppEventStatusNotifyModel);
				programEndMediaLedger.setMediaEndNotificationSent(true);	
				DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(programEndMediaLedger, aqpt.getAcquisitionPointIdentity(), programEndMedia.getSignalid());
			}

			if (programStartMedia != null && programStartMediaPoint!=null) {
				buildHostedAppEventStatusAndLogForAuditing(notification, context, requestTime, aqpt, programStartMedia, programStartMediaPoint, programStartMediaLedger, EventAction.CONFIRMED,
						hostedAppEventStatusNotifyModel);
				programStartMediaLedger.setMediaStartNotificationSent(true);	
				DataManagerFactory.getSCTE224DataManager().saveAcquisitionPointMediaLedger(programStartMediaLedger, aqpt.getAcquisitionPointIdentity(), programStartMedia.getSignalid());
			}
		}

		//If Neither Program Started/Nor Ended then Delete this signal.
		if (segmentTypeToMatchedMediaPointMap == null || segmentTypeToMatchedMediaPointMap.size() == 0) {
			baseResponseSignal.setAction(ResponseSignalAction.DELETE.toString());
			LOGGER.debug(
					() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", Deleting this signal as it neither started nor ended any event");
		}

		ESAMObjectCreationHelper.setResponseStatusCode(notification, context);
	}

	@Override
	public void saveConfirmedSignalIDInContextForSSR(String acquisitionSignalId, String acquisitionPointId, MuleEventContext context) {
		//Our need to identify did we ever confirmed any media?
		//In combined Signal Case this proessor may find two matched media, Select the Program Start one that will be saved as AQ level saveResponeMessageAtAQLevel.		
		if (segmentTypeToMatchedMediaMap != null) {
			Media startMediaObject = segmentTypeToMatchedMediaMap.get(SegmentType.PROGRAM_START.getSegmentTypeId());
			if (startMediaObject != null) {
				LOGGER.debug(() -> acquisitionSignalId + "=>" + acquisitionPointId + ", " + startMediaObject.getSignalid() + " will be saved for SSR Request.");
				context.getMessage().setProperty(ContextConstants.CONFIRMED_EVENT_SIGNAL_ID, startMediaObject.getSignalid(), PropertyScope.OUTBOUND);
			}

		}
	}

	private void addAltContent(SignalProcessingNotificationType notification, AcquisitionPoint aqpt, Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMapInContext,
			String altContentIndentity, List<ResponseSignalType> responseSignalTypes, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments,
			Date currentSystemTimeWithAddedDelta, String acquisitionSignalId) {
		Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap = new HashMap<String, I03ResponseModelDelta>();
		I03ResponseModelDelta list = new I03ResponseModelDelta();
		AlternateContentTypeModel delta = new AlternateContentTypeModel();
		delta.setAltContentIdentity(altContentIndentity);
		delta.setZoneIdentity(aqpt.getZoneIdentity());
		list.getAlternateContentIDList().add(delta);
		responseToI03ResponseModelDeltaMap.put(notification.getResponseSignal().get(0).getSignalPointID(), list);
		responseToI03ResponseModelDeltaMapInContext.putAll(responseToI03ResponseModelDeltaMap);
		final String pts_adjustment = ptsAdjustments == null || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33) : ptsAdjustments.get(acquisitionSignalId);
		responseSignalTypes.stream().forEach(rs -> {
			//changes as part of CS2-387 : updating event time to the time current System Time with added delta
			XMLGregorianCalendar startUtcTimeWithAddedDelta = SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta.getTime()).getUtcPoint();
			// set UTC for program start ResponseSignal
			rs.setUTCPoint(SCCResponseUtil.generateUTCPoint(startUtcTimeWithAddedDelta.toGregorianCalendar().getTimeInMillis()));
			rs.setAction(ResponseSignalAction.REPLACE.toString());
			setBinaryOrXMLSegmentDescriptorInResponseSignal(ptsTimes, pts_adjustment, rs, rs.getSCTE35PointDescriptor(), true, aqpt);
		});
	}

	private void buildHostedAppEventStatusAndLogForAuditing(SignalProcessingNotificationType notification, MuleEventContext context, Date requestTime, AcquisitionPoint aqpt,
			Media media, MediaPoint mediaPoint, MediaLedger mediaLedger, EventAction eventAction, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModel) {
		
		HostedAppEventStatusScte224NotifyModel hAppEventStatusNotifyModel = getHostedAppEventStatusNotifyModel(mediaLedger, eventAction, media.getSignalid(),
				mediaPoint.getSignalId(), aqpt);
		hostedAppEventStatusNotifyModel.add(hAppEventStatusNotifyModel);
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notification, mediaLedger.getSignalId());
		if (EventAction.COMPLETE == eventAction) {
			AuditLogger.auditLogCompleteBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), mediaLedger.getSignalId(), poisAuditLogVO);
		} else if (EventAction.CONFIRMED == eventAction) {
			AuditLogger.auditLogConfirmedBlackoutEvent(aqpt.getAcquisitionPointIdentity(), requestTime.getTime(), mediaLedger.getSignalId(), poisAuditLogVO);
		}
	}

	@Override
	protected boolean shouldSkipMediaInbandEvaluation(String acquisitionSignalIDOfAcquiredSignal, String acquisitionPointIdentity, Media matchedMedia) {
		//For Encoder Level Feed, if the passed Media seems have been ingested with Cadent:OOH(XMP Encoder), Then it will not be available for Inband Execution evaluation
		if (matchedMedia == null) {
			return false;
		}
		MediaPoint startMediaPoint = matchedMedia.getMediaPoints().get(0);
		shouldSkipMediaEvaluation = startMediaPoint.getZones().stream()
			.anyMatch(z -> z.equalsIgnoreCase(CppConstants.CADENT_OOH_ZONE));
		if (shouldSkipMediaEvaluation) {
			LOGGER.info(() -> acquisitionSignalIDOfAcquiredSignal + "=>" + acquisitionPointIdentity
					+ ", This media is use Cadent:Zone restriction, it's only for OOB execution. Currently, it is created by Cadent I02->SCTE224 Adapter. Hence, Inband will not supported on this.");
		}
		return shouldSkipMediaEvaluation;
	}

}
