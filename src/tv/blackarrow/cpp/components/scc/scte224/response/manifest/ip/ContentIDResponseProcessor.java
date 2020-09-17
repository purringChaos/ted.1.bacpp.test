package tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.scc.scte224.response.common.SCCBaseResponseProcessor;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.CompactMediaInfo;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;

/*
 * REDO this class, based on new Requirement
 */
public class ContentIDResponseProcessor extends SCCBaseResponseProcessor {
	private static final Logger LOGGER = LogManager.getLogger(ContentIDResponseProcessor.class);
	private boolean cadentContentID;
	private boolean confirmedNewEvent;

	@Override
	public void matchAcquiredSignal(MuleEventContext context, AcquiredSignal signal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date currentSystemTimeWithAddedDelta) {
		String mediaSignalId = null;
		byte[] upid = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getUpid();
		if (upid != null) {
			mediaSignalId = new String(upid);
		}

		// Option1: Cadent's Content ID
		if (StringUtils.isNotBlank(mediaSignalId) && mediaSignalId.startsWith(ESAMHelper.UPID_PREFIX)) {
			cadentContentID = true;
			mediaSignalId = mediaSignalId.substring(mediaSignalId.indexOf(ESAMHelper.UPID_PREFIX) + ESAMHelper.UPID_PREFIX.length());
			matchedMediaLedger = DataManagerFactory.getSCTE224DataManager().getAcquisitionPointMediaLedger(acquisitionPointIdentity, mediaSignalId);
			matchedMediaObject = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(medias.getFeedId(), mediaSignalId);
		} else {
			// Option2: Provider's Content ID
			boolean isBasicValidationPassed = validateBasicFields(context, signal, medias, acquisitionPointIdentity);
			if (!isBasicValidationPassed) {
				return;
			}
			Date requestTime = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
			List<SegmentationDescriptorType> segmentationDescriptorTypes = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo();
			//Only one segmentation Descriptor
			if (segmentationDescriptorTypes != null && segmentationDescriptorTypes.size() == 1) {
				SegmentationDescriptorType singleDescriptorInfo = segmentationDescriptorTypes.get(0);
				LOGGER.debug(() -> signal.getAcquisitionSignalID() + "=>" + acquisitionPointIdentity + ", matching SegmentationDescriptor -> " + singleDescriptorInfo.getSegmentTypeId());
				matchBySegmenationDescritorType(signal.getAcquisitionSignalID(), medias, acquisitionPointIdentity, requestTime, singleDescriptorInfo, currentSystemTimeWithAddedDelta);
			}
		}
	}
	@Override
	protected void matchBySegmenationDescritorType(String acquisitionSignalIDOfAcquiredSignal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date requestTime,
			SegmentationDescriptorType segmentationDescriptorType, Date currentSystemTimeWithAddedDelta) {
		Short segmentTypeId = segmentationDescriptorType.getSegmentTypeId();
		long currentTimeMillis = System.currentTimeMillis();
		if ((SegmentType.isAdStartSignal(segmentTypeId) || SegmentType.isProgramOverlapStartSignal(segmentTypeId) || SegmentType.isProgramStartSignal(segmentTypeId))
				&& (requestTime.getTime() + 30000) < currentTimeMillis) {
			return;
		}
		if (medias.getBasicMediaInfo() != null || !medias.getBasicMediaInfo().isEmpty()) {
			Boolean checkIfStartMediaPoint = false;
			for (CompactMediaInfo mediaItr : medias.getBasicMediaInfo().values()) {
				// Step1:Find matching Media
				Media matchedMedia = tryMatchingMedia(mediaItr, medias.getFeedId(), requestTime);

				// This function is useful if we need to skip In-band matching for any media.
				shouldSkipMediaEvaluation = shouldSkipMediaInbandEvaluation(acquisitionSignalIDOfAcquiredSignal, acquisitionPointIdentity, matchedMedia);

				if (matchedMedia == null || shouldSkipMediaEvaluation || !mediaItr.getMatchSignalExists() || matchedMedia.isInflightMediaDeleted()) {// If MatchSignal(i.e. Asserts Doesn't exist do not consider this for
																											// Inband flow
					continue;
				}

				// Step2:Find matching MediaPoint
				MediaPoint matchedMediaPoint = findMatchingMediaPoint(matchedMedia, requestTime, segmentationDescriptorType, acquisitionPointIdentity);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.info(() -> "Matched media's signalId is = " + (matchedMedia != null ? matchedMedia.getSignalid() : null));
					LOGGER.info(() -> "Matched media point signalId is = " + (matchedMediaPoint != null ? matchedMediaPoint.getSignalId() : null));
				}
				
				if(matchedMediaPoint != null && matchedMediaPoint.getOrder() == 1) {
					matchedMediaPointObject = matchedMediaPoint;
					matchedMediaObject = matchedMedia;
					checkIfStartMediaPoint = true;
					continue;
				}
				if (matchedMediaPoint != null && !isMediaEnded(acquisitionPointIdentity, matchedMedia)) {
					// Step3:Assign the matched object
					checkIfStartMediaPoint = false;
					matchedMediaPointObject = matchedMediaPoint;
					matchedMediaObject = matchedMedia;
					// Populate The in band-Transaction In MediaLedger
					getMediaLedgerObjectWithTransactionPopulated(acquisitionSignalIDOfAcquiredSignal, acquisitionPointIdentity, segmentationDescriptorType,
							segmentTypeId, currentSystemTimeWithAddedDelta);
					break;
				}
				
			}
			if (checkIfStartMediaPoint) {
				getMediaLedgerObjectWithTransactionPopulated(acquisitionSignalIDOfAcquiredSignal, acquisitionPointIdentity, segmentationDescriptorType,
						segmentTypeId, currentSystemTimeWithAddedDelta);
			}
		}
		if (matchedMediaObject == null) {
			LOGGER.debug(() -> acquisitionSignalIDOfAcquiredSignal + "=>" + acquisitionPointIdentity + ", Could not find match for SegmentationDescriptor -> "
					+ segmentationDescriptorType.getSegmentTypeId());
		}
	}
	@Override
	public void generateResponse(AcquiredSignal acquiredSignal, SignalProcessingNotificationType notificationResponse, AcquisitionPoint aqpt, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			Map<String, I03ResponseModelDelta> i03ResponseModelDeltaInContext, List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModelList, ResponseSignalType baseResponseSignal, Date currentSystemTimeWithAddedDelta) throws DatatypeConfigurationException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(() -> "Content ID Signal Response--->");
		}

		if (cadentContentID) {
			// 2. Found this content id in our system, that mean it's Cadent Content ID. Then we send NOOP if media is running and DELETE after it's done
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + "," + "Matched media = " + matchedMediaObject.getSignalid()
						+ ". Generating response 'noop' if Media has not ended else 'delete' if media has ended.");
			}
			// Option1: If Media is running it will be NOOP
			if (matchedMediaLedger == null || (matchedMediaLedger.isMediaStarted() && !matchedMediaLedger.isMediaEnded())) {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.NOOP);
			} // Option2: If Media is Ended we do DELETE(as we do not recognize it anymore)
			else if (matchedMediaLedger.isMediaEnded()) {
				decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
			}
		} else {
			// It's Provider's content ID
			// Option1: Did not find this content id in our system or MediaPoint is already
			// confirmed then apply Acquisition Point Settings
			if (matchedMediaObject == null || isMediaPointAlreadyConfirmed(aqpt.getAcquisitionPointIdentity())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", Programmer's Content ID Signal: "
							+ (matchedMediaObject == null ? "No Media was matched" : "MediaPoint is already confirmed for this segmentTypeID.") + " Sending response based on Acquisition Point InBand content Id setting as "
							+ (aqpt.getInBandContentIdConfiguredValue() == SignalHandlingConfiguration.PRESERVE ? ResponseSignalAction.NOOP : ResponseSignalAction.DELETE));
				}
				if (aqpt.getInBandContentIdConfiguredValue() == SignalHandlingConfiguration.PRESERVE) {
					decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.NOOP);
				} else {
					decorateNoOPorDeleteResponse(notificationResponse, ResponseSignalAction.DELETE);
				}
			} else if (matchedMediaObject != null && matchedMediaPointObject != null) {
				// Option2: Content ID started/Ended the Program
				if (matchedMediaLedger != null) {
					SegmentType resultedResponseSegmentID = matchedMediaLedger.getResultedReponseSignalForContentID(matchedMediaPointObject, SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId());

					SCCBaseResponseProcessor processor = null;
					if (resultedResponseSegmentID != null) {
						switch (resultedResponseSegmentID) {

						case PROGRAM_END:

							processor = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramEndResponseProcessor();
							processor.setMatchedMediaObject(matchedMediaObject);
							processor.setMatchedMediaPointObject(matchedMediaPointObject);
							processor.setMatchedMediaLedger(matchedMediaLedger);
							processor.generateResponse(acquiredSignal, notificationResponse, aqpt, ptsTimes, ptsAdjustments, context, i03ResponseModelDeltaInContext, hostedAppEventStatusNotifyModelList, baseResponseSignal, currentSystemTimeWithAddedDelta);
							LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", Generated Response Signals SegmentationDescriptor -> " + SegmentType.PROGRAM_START);

							break;
						case PROGRAM_START:
							ResponseSignalType anotherBaseResponseSignal = baseResponseSignal;

							processor = new tv.blackarrow.cpp.components.scc.scte224.response.manifest.ip.ProgramStartResponseProcessor();
							processor.setMatchedMediaObject(matchedMediaObject);
							processor.setMatchedMediaPointObject(matchedMediaPointObject);
							processor.setMatchedMediaLedger(matchedMediaLedger);
							processor.generateResponse(acquiredSignal, notificationResponse, aqpt, ptsTimes, ptsAdjustments, context, i03ResponseModelDeltaInContext, hostedAppEventStatusNotifyModelList, anotherBaseResponseSignal, currentSystemTimeWithAddedDelta);
							confirmedNewEvent = true;

							LOGGER.debug(() -> acquiredSignal.getAcquisitionSignalID() + "=>" + aqpt.getAcquisitionPointIdentity() + ", Generated Response Signals SegmentationDescriptor -> " + SegmentType.PROGRAM_END);
							break;
						default:
							break;

						}
					}
				}

			}
		}
		ESAMObjectCreationHelper.setResponseStatusCode(notificationResponse, context);
	}

	@Override
	public void saveConfirmedSignalIDInContextForSSR(String acquisitionSignalId, String acquisitionPointId, MuleEventContext context) {
		if (confirmedNewEvent && matchedMediaObject != null && matchedMediaPointObject != null) {
			LOGGER.debug(() -> acquisitionSignalId + "=>" + acquisitionPointId + ", " + matchedMediaObject.getSignalid() + " will be saved for SSR Request.");
			context.getMessage().setProperty(ContextConstants.CONFIRMED_EVENT_SIGNAL_ID, matchedMediaObject.getSignalid(), PropertyScope.OUTBOUND);
		}
	}

	/** 
	 * 
	 * @param acquisitionPointIdentity
	 * @param matchedMediaObject
	 * @return
	 */
	private boolean isMediaPointAlreadyConfirmed(String acquisitionPointIdentity) {
		boolean isMediaPointAlreadyConfirmed = false;

		if (matchedMediaLedger != null) {
			SegmentType resultedResponseSegmentID = matchedMediaLedger.getResultedReponseSignalForContentID(matchedMediaPointObject, SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId());

			if (resultedResponseSegmentID != null && matchedMediaLedger.getMediaTransactions() != null) {
				switch (resultedResponseSegmentID) {

				case PROGRAM_END:
					isMediaPointAlreadyConfirmed = matchedMediaLedger.isMediaEnded() || matchedMediaLedger.getMediaTransactions().stream()
							.filter(transaction -> ((SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getSignalSegmentTypeId() || SegmentType.PROGRAM_EARLY_TERMINATION.getSegmentTypeId() == transaction.getSignalSegmentTypeId()
									|| (transaction.getResultedSignalSegmentTypeId() != null
											&& (SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId() || SegmentType.PROGRAM_EARLY_TERMINATION.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId())))
									&& ESSRequestType.SCC.equals(transaction.getEssRequestType())))
							.collect(Collectors.toList()).size() > 1;
					if (isMediaPointAlreadyConfirmed) {
						LOGGER.debug("Media signal id: " + matchedMediaObject.getMediaSignalid() + " is already confirmed for end media point so, no need to confirm it again by programmer's content id ");
					}

					break;
				case PROGRAM_START:
					isMediaPointAlreadyConfirmed = matchedMediaLedger.isMediaStarted() || matchedMediaLedger.getMediaTransactions().stream()
							.filter(transaction -> ((SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId() || SegmentType.PROGRAM_OVERLAP_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId()
									|| (transaction.getResultedSignalSegmentTypeId() != null
											&& (SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId() || SegmentType.PROGRAM_OVERLAP_START.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId())))
									&& ESSRequestType.SCC.equals(transaction.getEssRequestType())))
							.collect(Collectors.toList()).size() > 1;
					if (isMediaPointAlreadyConfirmed) {
						LOGGER.debug(() -> "Media signal id: " + matchedMediaObject.getMediaSignalid() + " is already confirmed for start media point so, no need to confirm it again by programmer's content id ");

					}

					break;
				default:
					break;
				}
			}

		}
		return isMediaPointAlreadyConfirmed;
	}
	
	private boolean isMediaEnded(String acquisitionPointIdentity, Media matchedMedia) {
		matchedMediaLedger = DataManagerFactory.getSCTE224DataManager().getAcquisitionPointMediaLedger(acquisitionPointIdentity, matchedMedia.getSignalid());
		return matchedMediaLedger != null && matchedMediaLedger.isMediaEndNotificationSent();
	}

	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short resultedSegmentType) {
		Long timeInMillis = null;
		//For contentId we send duration if it resulted in start/end signal 
		SegmentType segmentType = SegmentType.valueOf(resultedSegmentType);
		switch (segmentType) {
		case PROGRAM_EARLY_TERMINATION:
		case PROGRAM_END:
			timeInMillis = 0l;
			if (matchedMediaLedger != null && matchedMediaLedger.getMediaTransactions() != null && !matchedMediaLedger.getMediaTransactions().isEmpty()) {
				MediaTransaction programStartOrOverlapTransaction = matchedMediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
				if (programStartOrOverlapTransaction != null) {
					timeInMillis = signalTime - programStartOrOverlapTransaction.getSignalTimeInMS();
				}
			}
			break;
		case PROGRAM_OVERLAP_START:
		case PROGRAM_START:
			if (!matchedMediaPoint.getApply().isEmpty()) {
				timeInMillis = matchedMediaPoint.getApply().iterator().next().getDurationInMillis();
			}
			break;
		default:
			break;
		}
		return timeInMillis;
	}

	@Override
	protected MediaLedger getMediaLedgerObjectWithTransactionPopulated(String acquisitionSignalIDOfAcquiredSignal, String acquisitionPointIdentity,
			SegmentationDescriptorType segmentationDescriptorType, Short segmentTypeId, Date CurrentSystemTimeWithAddedDelta) {
		//Step1: Adding UPID in segmentationDescriptorType(TODO more refractor-delaying here)
		final String upidHex = ESAMHelper.generateUpidString(matchedMediaObject.getSignalid());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidHex);
		segmentationDescriptorType.setUpid(upid);

		//Step2: Add the transaction in MediaLedger
		getOrCreateMediaLedger(acquisitionPointIdentity);
		//Add the original acquisition signal id in the Media Ledger.
		matchedMediaLedger.setSignalId(matchedMediaObject.getSignalid());

		//start--Populate that content ID resulted in Satrt/Stop and depending populated resultedSignalSegmentID, its not needed to populate For Any Other Signal at this moment.
		SegmentType resultedSegmentType = matchedMediaLedger.getResultedReponseSignalForContentID(matchedMediaPointObject, segmentTypeId);
		matchedMediaLedger.addAcquisitionSignalId(resultedSegmentType, acquisitionSignalIDOfAcquiredSignal);//In that case this acquisition signal id is either attached to 16/17. If content id was used to start/add. 

		Long durationInMillis = getDurationForPersistingInMediaLedger(matchedMediaPointObject, matchedMediaObject, matchedMediaLedger, acquisitionPointIdentity,
				CurrentSystemTimeWithAddedDelta.getTime(), resultedSegmentType.getSegmentTypeId());

		//CS2-387 Adding CurrentSystemTimeWithAddedDelta as a signal time since this is the time when transcoder will act upon and will go in Response signal in UTC Points
		matchedMediaLedger.addMediaTransaction(MediaTransaction.build(CurrentSystemTimeWithAddedDelta.getTime(), segmentTypeId, durationInMillis, upidHex, ContextConstants.ESSRequestType.SCC),
				resultedSegmentType);

		LOGGER.debug(() -> acquisitionSignalIDOfAcquiredSignal + "=>" + acquisitionPointIdentity + ", match found for segmentationDescriptor(segmentTypeId=" + segmentTypeId + "). SignalId="
				+ matchedMediaObject.getSignalid() + ", matchedMediaPoint=" + matchedMediaPointObject.getSignalId() + ". Stored Transaction in Ledger.");
		return matchedMediaLedger;
	}

}
