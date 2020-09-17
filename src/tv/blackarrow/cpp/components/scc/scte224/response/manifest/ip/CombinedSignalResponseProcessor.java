package tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.scc.scte224.response.common.SCCBaseResponseProcessor;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
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
			if (SegmentType.isContentIdentificationSignal(segmentationDescriptorType.getSegmentTypeId())) {
				continue;
			}
			String acquisitionSignalID = signal.getAcquisitionSignalID();
			if(isAnyProgramEndConfirmed) {
				acquisitionSignalID = UUIDUtils.getBase64UrlEncodedUUID();
			}
			matchBySegmenationDescritorType(acquisitionSignalID, medias, acquisitionPointIdentity, requestTime, segmentationDescriptorType, currentSystemTimeWithAddedDelta);
			//Base class objects may have been set, clean them and send in this map in this class.
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
			Map<String, I03ResponseModelDelta> i03ResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModelList,
			ResponseSignalType baseResponseSignal, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {
		final String acquisitionSignalId = acquiredSignal.getAcquisitionSignalID();
		SCTE35PointDescriptorType descriptorType = acquiredSignal.getSCTE35PointDescriptor();
		SCCBaseResponseProcessor processor = null;
		boolean baseResponseSignalUsed = false;
		List<SegmentationDescriptorType> segmentationDescriptorInfoList = descriptorType.getSegmentationDescriptorInfo();
		segmentationDescriptorInfoList.sort(Comparator.comparing(SegmentationDescriptorType::getSegmentTypeId).reversed());//Send 17, 16 response order 
		if (descriptorType != null && segmentationDescriptorInfoList != null) {
			for (SegmentationDescriptorType descriptorInfo : segmentationDescriptorInfoList) {
				SegmentType segmentType = SegmentType.valueOf(descriptorInfo.getSegmentTypeId());
				Media mediaMatchBySegmentType = segmentTypeToMatchedMediaMap.get(segmentType.getSegmentTypeId());
				MediaPoint mediaPointMatchBySegmentType = segmentTypeToMatchedMediaPointMap.get(segmentType.getSegmentTypeId());
				MediaLedger mediaLedger = segmentTypeToMatchedMediaLedgerMap.get(segmentType.getSegmentTypeId());

				switch (segmentType) {
				case CONTENT_IDENTIFICATION:
					break;
				case PROGRAM_EARLY_TERMINATION:
				case PROGRAM_END:
					if (mediaMatchBySegmentType != null) {
						processor = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramEndResponseProcessor();
						processor.setMatchedMediaObject(mediaMatchBySegmentType);
						processor.setMatchedMediaPointObject(mediaPointMatchBySegmentType);
						processor.setMatchedMediaLedger(mediaLedger);
						processor.generateResponse(acquiredSignal, notification, aqpt, ptsTimes, ptsAdjustments, context,
								i03ResponseModelDeltaInContext, hostedAppEventStatusNotifyModelList, baseResponseSignal, currentSystemTimeWithAddedDelta);
						baseResponseSignalUsed = true;
						LOGGER.debug(
								() -> acquisitionSignalId + "=>" + aqpt.getAcquisitionPointIdentity() + ", Generated Response Signals SegmentationDescriptor -> " + segmentType);
					}

					break;
				case PROGRAM_OVERLAP_START:
				case PROGRAM_START:
					ResponseSignalType anotherBaseResponseSignal = baseResponseSignal;
					if (mediaMatchBySegmentType != null) {
						if (baseResponseSignalUsed) {//So Add Another Base Signal
							anotherBaseResponseSignal = CppUtil.addNewResponseSignal(notification, acquiredSignal);
							anotherBaseResponseSignal.setAcquisitionSignalID(mediaLedger.getAcquisitionSignalIds().get(SegmentType.PROGRAM_START));
							notification.getConditioningInfo().clear();//if Program End has been served by this signal, then send only the conditioning info of start signal. Appended by this start response. Hence, clearing this map to remove anything added before.
						}
						processor = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramStartResponseProcessor();
						processor.setMatchedMediaObject(mediaMatchBySegmentType);
						processor.setMatchedMediaPointObject(mediaPointMatchBySegmentType);
						processor.setMatchedMediaLedger(mediaLedger);
						processor.generateResponse(acquiredSignal, notification, aqpt, ptsTimes, ptsAdjustments, context,
								i03ResponseModelDeltaInContext, hostedAppEventStatusNotifyModelList, anotherBaseResponseSignal, currentSystemTimeWithAddedDelta);
						if (baseResponseSignalUsed) {
							//This mean Program End has been served so program Start's first signal should be Create this time.
							anotherBaseResponseSignal.setAction(ResponseSignalAction.CREATE.toString());
							LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity()
									+ ", Converting Program Start Response Signal to Create, if program was ended and started both. ");
						}
						baseResponseSignalUsed = true;
						LOGGER.debug(
								() -> acquisitionSignalId + "=>" + aqpt.getAcquisitionPointIdentity() + ", Generated Response Signals SegmentationDescriptor -> " + segmentType);
					}
					break;
				default:
					break;
				}
			}
		}
		//If Neither Program Started/Nor Ended then Delete this signal.
		if (segmentTypeToMatchedMediaMap == null || segmentTypeToMatchedMediaMap.size() == 0) {
			baseResponseSignal.setAction(ResponseSignalAction.DELETE.toString());
			LOGGER.debug(() -> acquisitionSignalId + "=>" + aqpt.getAcquisitionPointIdentity() + ", Deleting this signal as it neither started nor ended any event");
		}

		ESAMObjectCreationHelper.setResponseStatusCode(notification, context);

		//here we got all 4 signals in single notification
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

	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short segmentTypeId) {
		Long duration = null;
		SegmentType segmentType = SegmentType.valueOf(segmentTypeId);
		if (segmentType != null) {
			switch (segmentType) {
			case PROGRAM_EARLY_TERMINATION:
			case PROGRAM_END:
				duration = 0l;
				
				if (matchedMediaLedger != null && matchedMediaLedger.getMediaTransactions() != null && !matchedMediaLedger.getMediaTransactions().isEmpty()) {
					MediaTransaction programStartOrOverlapTransaction = matchedMediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
					if (programStartOrOverlapTransaction != null) {
						duration = signalTime - programStartOrOverlapTransaction.getSignalTimeInMS();
					}
				}

				break;
			case PROGRAM_OVERLAP_START:
			case PROGRAM_START:
				if (!matchedMediaPoint.getApply().isEmpty()) {
					duration = matchedMediaPoint.getApply().iterator().next().getDurationInMillis();
				}
				break;
			default:
				break;
			}
		}
		return duration;
	}

}
