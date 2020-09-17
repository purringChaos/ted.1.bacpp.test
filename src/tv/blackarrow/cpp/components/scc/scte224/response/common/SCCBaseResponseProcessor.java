package tv.blackarrow.cpp.components.scc.scte224.response.common;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.ApplyorRemove;
import tv.blackarrow.cpp.model.scte224.CompactMediaInfo;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.MatchSignal;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.model.scte224.SCTE224EventStatus;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UUIDUtils;
// This class is the base class for single signal
public abstract class SCCBaseResponseProcessor implements IBaseResponseProcessor {
	private static final String EMPTY_STRING = "";

	private static Logger LOGGER = LogManager.getLogger(SCCBaseResponseProcessor.class);

	protected Media matchedMediaObject;
	protected MediaPoint matchedMediaPointObject;
	protected MediaLedger matchedMediaLedger;
	protected boolean shouldSkipMediaEvaluation = false;
	protected String defautlPtsAdjustment = Scte35BinaryUtil.toBitString(0l, 33);

	/*
	 * (non-Javadoc)
	 * This is the base implementation for single Descriptor Info to be matched and only supports basis functions.
	 * Override this function to provide specific implementation.
	 */
	@Override
	public void matchAcquiredSignal(MuleEventContext context, AcquiredSignal signal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date currentSystemTimeWithAddedDelta) {
		boolean isBasicValidationPassed = validateBasicFields(context, signal, medias, acquisitionPointIdentity);
		if (!isBasicValidationPassed) {
			return;
		}
		
		Date requestTime = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		List<SegmentationDescriptorType> segmentationDescriptorTypes = signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo();
		//Only one segmentation Descriptor
		if (segmentationDescriptorTypes != null && segmentationDescriptorTypes.size() == 1) {
			SegmentationDescriptorType singleDescriptorInfo = segmentationDescriptorTypes.get(0);
			LOGGER.debug(
					() -> signal.getAcquisitionSignalID() + "=>" + acquisitionPointIdentity + ", matching SegmentationDescriptor -> " + singleDescriptorInfo.getSegmentTypeId());
			matchBySegmenationDescritorType(signal.getAcquisitionSignalID(), medias, acquisitionPointIdentity, requestTime, singleDescriptorInfo, currentSystemTimeWithAddedDelta);
		}
	}

	protected boolean validateBasicFields(MuleEventContext context, AcquiredSignal signal, FeedMediaCompactedDetail medias, String acquisitionPointIdentity) {
		boolean isBasicValidationPassed = true;
		if (signal.getUTCPoint() == null || signal.getUTCPoint().getUtcPoint() == null || signal.getUTCPoint().getUtcPoint().toGregorianCalendar() == null) {
			LOGGER.error(() -> "Passed in UTCPoint: " + signal.getUTCPoint() + " is invalid");
			context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
			isBasicValidationPassed = false;
		}

		if (medias == null || medias.getBasicMediaInfo().isEmpty() || signal == null || StringUtils.isBlank(acquisitionPointIdentity)) {
			isBasicValidationPassed = false;
		}
		return isBasicValidationPassed;
	}

	protected void matchBySegmenationDescritorType(String acquisitionSignalID, FeedMediaCompactedDetail medias, String acquisitionPointIdentity, Date requestTime,
			SegmentationDescriptorType segmentationDescriptorType, Date currentSystemTimeWithAddedDelta) {
		Short segmentTypeId = segmentationDescriptorType.getSegmentTypeId();
		long currentTimeMillis = System.currentTimeMillis();
		if ((SegmentType.isAdStartSignal(segmentTypeId) || SegmentType.isProgramOverlapStartSignal(segmentTypeId) || SegmentType.isProgramStartSignal(segmentTypeId))
				&& (requestTime.getTime() + 30000) < currentTimeMillis) {
			return;
		}
		if (medias.getBasicMediaInfo() != null || !medias.getBasicMediaInfo().isEmpty()) {
			for (CompactMediaInfo mediaItr : medias.getBasicMediaInfo().values()) {
				// Step1:Find matching Media
				Media matchedMedia = tryMatchingMedia(mediaItr, medias.getFeedId(), requestTime);

				// This function is useful if we need to skip In-band matching for any media.
				shouldSkipMediaEvaluation = shouldSkipMediaInbandEvaluation(acquisitionSignalID, acquisitionPointIdentity, matchedMedia);

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

				if (matchedMediaPoint != null) {
					// Step3:Assign the matched object
					matchedMediaPointObject = matchedMediaPoint;
					matchedMediaObject = matchedMedia;
					// Populate The in band-Transaction In MediaLedger
					getMediaLedgerObjectWithTransactionPopulated(acquisitionSignalID, acquisitionPointIdentity, segmentationDescriptorType,
							segmentTypeId, currentSystemTimeWithAddedDelta);
					break;
				}
			}
		}
		if (matchedMediaObject == null) {
			LOGGER.debug(() -> acquisitionSignalID + "=>" + acquisitionPointIdentity + ", Could not find match for SegmentationDescriptor -> "
					+ segmentationDescriptorType.getSegmentTypeId());
		}
	}

	/*
	 * Mediapoint got confirmed now save this at AQ level as record
	 */
	protected MediaLedger getMediaLedgerObjectWithTransactionPopulated(String acquisitionSignalID, String acquisitionPointIdentity,
			SegmentationDescriptorType segmentationDescriptorType, Short segmentTypeId, Date CurrentSystemTimeWithAddedDelta) {
		//Step1: Adding UPID in segmentationDescriptorType(TODO more refractor-delaying here)
		final String upidHex = ESAMHelper.generateUpidString(matchedMediaObject.getSignalid());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidHex);
		segmentationDescriptorType.setUpid(upid);

		//Step2: Add the transaction in MediaLedger
		getOrCreateMediaLedger(acquisitionPointIdentity);
		Long durationInMillis = getDurationForPersistingInMediaLedger(matchedMediaPointObject, matchedMediaObject, matchedMediaLedger, acquisitionPointIdentity,
				CurrentSystemTimeWithAddedDelta.getTime(), segmentationDescriptorType.getSegmentTypeId());
		//Add the original acquisition signal id in the Media Ledger.
		matchedMediaLedger.setSignalId(matchedMediaObject.getSignalid());
		// updating the APML model's audienceIncrement by 1 on blackout override being true
		matchedMediaLedger.addAcquisitionSignalId(SegmentType.valueOf(segmentTypeId), acquisitionSignalID);
		//CS2-387 Adding CurrentSystemTimeWithAddedDelta as a signal time since this is the time when transcoder will act upon and will go in Response signal in UTC Points
		matchedMediaLedger.addMediaTransaction(MediaTransaction.build(CurrentSystemTimeWithAddedDelta.getTime(), segmentTypeId, durationInMillis, upidHex, ContextConstants.ESSRequestType.SCC), null);

		LOGGER.debug(() -> acquisitionSignalID + "=>" + acquisitionPointIdentity + ", match found for segmentationDescriptor(segmentTypeId=" + segmentTypeId + "). SignalId=" + matchedMediaObject.getSignalid() + ", matchedMediaPoint="
				+ matchedMediaPointObject.getSignalId() + ". Stored Transaction in Ledger.");
		return matchedMediaLedger;
	}

	protected boolean shouldSkipMediaInbandEvaluation(String acquisitionSignalIDOfAcquiredSignal, String acquisitionPointIdentity, Media matchedMedia) {
		return false;
	}

	/**
	 * Returns the HostedAppEventStatusNotifyModel that would be used for notifying the hosted app.
	 * 
	 * @param acquisitionPointMediaLedger the Media Ledger containing all the transactions that has happened on this AP for this Media.
	 * @param eventAction what is this request for, it could be a Confirmation Event or a Completion Event.
	 * @param eventSignalId the event's signal id.
	 * @param acquisitionPoint the acquisition point on which this event signal has been received.
	 * @return
	 */
	protected HostedAppEventStatusScte224NotifyModel getHostedAppEventStatusNotifyModel(final MediaLedger acquisitionPointMediaLedger, final EventAction eventAction,
			final String eventSignalId, final String mediaPointSignalId, final AcquisitionPoint acquisitionPoint) {
		HostedAppEventStatusScte224NotifyModel scheduledHostedMessage = new HostedAppEventStatusScte224NotifyModel();
		scheduledHostedMessage.setEventStatus(SCTE224EventStatus.CONFIRMED);
		scheduledHostedMessage.setStreamId(acquisitionPointMediaLedger.getAcquisitionPointIdentity());
		scheduledHostedMessage.setEventSignalId(eventSignalId);
		scheduledHostedMessage.setMediaPointSignalId(mediaPointSignalId);
		scheduledHostedMessage.setEventType(EventType.SCTE224);
		MediaTransaction programStartTransaction = acquisitionPointMediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
		Long programStartTime = null;
		Long programEndTime = null;
		if (programStartTransaction != null) {
			//CS2-387: taking the signal time for the HOsted update
			programStartTime = programStartTransaction.getSignalTimeInMS();
		}
		MediaTransaction programEndTransaction = acquisitionPointMediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
		if (programEndTransaction != null) {
			programEndTime = programEndTransaction.getSignalTimeInMS() ;
		}
		scheduledHostedMessage.setSignalTime(EventAction.COMPLETE == eventAction ? programEndTime : programStartTime);
		return scheduledHostedMessage;
	}

	/*
	 * processSignal--->Step2
	 * Apply incoming signal on media points and find the matching media point
	 */
	protected MediaPoint findMatchingMediaPoint(Media matchedMedia, Date recievedTime, SegmentationDescriptorType segmentationDescriptorType, String acquisitionPointIdentity) {
		if (matchedMedia == null || recievedTime == null || matchedMedia.getMediaPoints() == null || matchedMedia.getMediaPoints().isEmpty()) {
			return null;
		}
		if(SegmentType.isProgramEndSignal(segmentationDescriptorType.getSegmentTypeId())) {
			//Presence of ledger is enough to identify that a program was started using this Media(MediaPoint)
			boolean wasAProgramStartedWithThisMedia = DataManagerFactory.getSCTE224DataManager()
					.getAcquisitionPointMediaLedger(acquisitionPointIdentity, matchedMedia.getSignalid()) != null;
			if(!wasAProgramStartedWithThisMedia) {
				LOGGER.debug(() -> "There was no Program Started with this Media " + matchedMedia.getMediaId() + ", "
						+ "and hence skipping the processing of this Media for the Program End Signal.");
				return null;
			}
		}
		for (MediaPoint mediaPoint : matchedMedia.getMediaPoints()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(() -> "MediaPoint Under Consideration: \n"
						+ "MediaPoint Signal ID      : " + mediaPoint.getSignalId() + "\n"
						+ "MediaPoint Effective Time : " + mediaPoint.getEffectiveTimeInMS() + "\n"
						+ "Received Signal Time      : " + recievedTime.getTime() + "\n"
						+ "MediaPoint Expire Time    : " + mediaPoint.getExpiresTimeInMS() + "\n"
						+ "MediaPoint Match Time     : " + mediaPoint.getMatchTimeInMS()
						);
				LOGGER.debug(() -> "Is request time falling in Media Point Time window? : " 
						+ (	mediaPoint.getEffectiveTimeInMS() != null 
							&& mediaPoint.getExpiresTimeInMS() != null 
							&& recievedTime.getTime() >= mediaPoint.getEffectiveTimeInMS() 
							&& recievedTime.getTime() <= mediaPoint.getExpiresTimeInMS()));
				LOGGER.debug(() -> "Has MatchSignal? : " + (mediaPoint.getMatchSignal() != null));
			}

			if (mediaPoint.getEffectiveTimeInMS() != null && mediaPoint.getExpiresTimeInMS() != null && mediaPoint.getMatchSignal() != null
					&& (recievedTime.getTime() >= mediaPoint.getEffectiveTimeInMS() && recievedTime.getTime() <= mediaPoint.getExpiresTimeInMS())
			) {
				//In cases where match time is present then a MediaPoint is only considered for the In-Band signals up to(matchTime + signalTolerance.)
				if(mediaPoint.getMatchTimeInMS() !=null && mediaPoint.getMatchTimeInMS() > 0) {
					long matchTimeWithSignalTolerance = mediaPoint.getMatchTimeInMS() + (mediaPoint.getMatchSignal()
							.getSignalToleranceDurationInMS() == null ? 0 : mediaPoint.getMatchSignal().getSignalToleranceDurationInMS());
					if(recievedTime.getTime() > matchTimeWithSignalTolerance) {
						LOGGER.debug(() -> "MediaPoint with signal id " + mediaPoint.getSignalId() + " is being discarded as the "
									+ "time received in the In-Band signal is beyond the MediaPoint's match time + signal tolerance." );
						continue;
					}
				}
				
				if (matchSignalAssert(mediaPoint, segmentationDescriptorType)) {
					return mediaPoint;
				}
			}
		}
		return null;
	}
	
	/*
	 * processSignal-->Step2.1 apply asserts on media point
	 */
	private boolean matchSignalAssert(MediaPoint mediaPoint, SegmentationDescriptorType segmentationDescriptorType) {
		MatchSignal matchSignal = mediaPoint.getMatchSignal();
		boolean matched = false;
		// Now match the asserts ALL
		if (matchSignal != null) {
			matched = matchSignal.matches(segmentationDescriptorType);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Is Assert Matched with Media Point("+ mediaPoint.getSignalId() +") MatchSignal ? " + matched);
		}
		return matched;
	}

	/*
	 *
	 * processSignal--->Step1
	 * Find possible media point
	 */
	protected Media tryMatchingMedia(CompactMediaInfo media, String feedId, Date date) {
		Media matchedMedia = null;
		if (media != null) {
			if (LOGGER.isDebugEnabled()) {
				if (media.getEffectiveTimeInMS() != null && media.getExpiresTimeInMS() != null) {
					LOGGER.debug(() -> "Checking Media -->" + media.getSignalid() + "requestTime = " + date.getTime() + "\nmedia.getEffectiveTimeInMS() = "
							+ media.getEffectiveTimeInMS() + "\nmedia.getExpiresTimeInMS()=" + media.getExpiresTimeInMS());
				}
			}
			if (media.getEffectiveTimeInMS() != null && media.getExpiresTimeInMS() != null && date.getTime() >= media.getEffectiveTimeInMS()
					&& date.getTime() <= media.getExpiresTimeInMS()) {
				matchedMedia = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(feedId, media.getSignalid());
			}
		}
		return matchedMedia;
	}

	protected void decorateNoOPorDeleteResponse(SignalProcessingNotificationType notificationResponse, ResponseSignalAction actionNoOPOrDeleter) {
		for (ResponseSignalType responseSignalType : notificationResponse.getResponseSignal()) {
			switch (actionNoOPOrDeleter) {
			case NOOP:
				responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
				break;
			case DELETE:
				responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
				break;
			default:
				break;

			}
			if (responseSignalType.getBinaryData() != null) {
				responseSignalType.setSCTE35PointDescriptor(null);
			}
			//do not adjust any offset in either UTC point or PTS time.
		}
	}

	private ConditioningInfoType getConditioningInfo(final String acquisitionSignalId, final boolean isMediaEnded, Long contentDuration, final AcquisitionPoint aqpt) throws DatatypeConfigurationException {
		ConditioningInfoType conditioningInfo = new ConditioningInfoType();
		conditioningInfo.setAcquisitionSignalIDRef(acquisitionSignalId);
		if ((contentDuration != null && contentDuration < 0) || contentDuration == null) {
			contentDuration = 0l;
		}
		if (contentDuration != null) {
			Duration duration = JavaxUtil.getDatatypeFactory().newDuration(contentDuration);
			conditioningInfo.setDuration(duration);
		}
		return conditioningInfo;
	}

	private ResponseSignalType createContentIndentficationRespSignal(AcquisitionPoint aqpt, String signalPointId, XMLGregorianCalendar startUTCTimeWithAddedFreq,
			XMLGregorianCalendar stopUtcTimeWithDuration, final String acquisitionSignalId, final boolean binary, final Long segmentEventId,
			final ResponseSignalAction contentIdentificationSignalAction, HashMap<String, String> ptsTimes, String pts_adjustment, final Long contentDuration,
			final Map<QName, String> attributes, final SegmentType requestSegmentTypeIdEnum, SegmentType responseSegmentTypeId) throws DatatypeConfigurationException {
		SCTE35PointDescriptorType scte35Pnt;
		ResponseSignalType respSignalContentIdentification = new ResponseSignalType();
		String upidStr = ESAMHelper.generateUpidString(signalPointId);
		byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		respSignalContentIdentification.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalContentIdentification.setAcquisitionSignalID(acquisitionSignalId);
		respSignalContentIdentification.setAction(contentIdentificationSignalAction == null ? null : contentIdentificationSignalAction.toString());
		respSignalContentIdentification.setSignalPointID(signalPointId);

		if (startUTCTimeWithAddedFreq != null) {
			EventScheduleType eventSchedule = getEventSchedule(startUTCTimeWithAddedFreq.toGregorianCalendar().getTimeInMillis(),
					stopUtcTimeWithDuration != null ? stopUtcTimeWithDuration.toGregorianCalendar().getTimeInMillis() : null, aqpt.getContentIDFrequency() * 1000, aqpt,
					requestSegmentTypeIdEnum);

			respSignalContentIdentification.setEventSchedule(eventSchedule);
			respSignalContentIdentification.setUTCPoint(eventSchedule.getStartUTC()); // need set UTCpoint
		}

		scte35Pnt = new SCTE35PointDescriptorType();
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(segmentEventId); //System.currentTimeMillis() & 0x3fffffff);  // event id from alter event
		segment.setSegmentTypeId(responseSegmentTypeId.getSegmentTypeId());
		// duration of this content identification segmentation
		if (contentDuration != null) {
			attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
			segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(contentDuration));
		} else {
			attributes.remove(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
			segment.setDuration(null);
		}
		ESAMObjectCreationHelper.setBasicSegmentInfoForContentIDSignal(upid, segment);
		segment.getOtherAttributes().putAll(attributes);
		setDeviceRestrictionsAndNoRegionalBlackoutFlags(scte35Pnt);

		setBinaryOrXMLSegmentDescriptorInResponseSignal(ptsTimes, pts_adjustment, respSignalContentIdentification, scte35Pnt, binary, aqpt);

		return respSignalContentIdentification;
	}

	private EventScheduleType getEventSchedule(final long startUTCTimeWithAddedFreq, Long stopUtcTimeWithDuration, long frequency, final AcquisitionPoint aqpt,
			final SegmentType requestSegmentTypeIdEnum) throws DatatypeConfigurationException {

		//fix the issue that endTime is less than start time, when getting the program_end immediately after program start.
		if (stopUtcTimeWithDuration != null && stopUtcTimeWithDuration <= startUTCTimeWithAddedFreq) {
			stopUtcTimeWithDuration = startUTCTimeWithAddedFreq;
			frequency = 0;
		}

		EventScheduleType eventSchedule = new EventScheduleType();
		eventSchedule.setStartUTC(SCCResponseUtil.generateUTCPoint(startUTCTimeWithAddedFreq));
		eventSchedule.setInterval(JavaxUtil.getDatatypeFactory().newDuration(frequency));
		if (stopUtcTimeWithDuration != null) {
			eventSchedule.setStopUTC(SCCResponseUtil.generateUTCPoint(stopUtcTimeWithDuration));
		}
		return eventSchedule;
	}

	protected void generateConditioningInfo(SignalProcessingNotificationType notificationResponse, String acquisitionSignalId, AcquisitionPoint aqpt,
			SegmentType segmentTypeId, MediaLedger mediaLedger) throws DatatypeConfigurationException {
		List<ConditioningInfoType> conditioningInfoList = notificationResponse.getConditioningInfo();
		Long duration = getDurationFromLedger(mediaLedger);
		ConditioningInfoType conditioningInfo = getConditioningInfo(acquisitionSignalId, mediaLedger.isMediaEnded(), duration, aqpt);
		conditioningInfoList.add(conditioningInfo);
	}

	protected void appendContentIdSection(HashMap<String, String> ptsTimes, String pts_adjustment, AcquisitionPoint aqpt,
			SignalProcessingNotificationType notificationResponse, ResponseSignalAction actionName, MediaLedger mediaLedger, SegmentType responseSegmentTypeId,
			ResponseSignalType baseResponseSignal, SegmentationDescriptorType supportedSegment) throws DatatypeConfigurationException {
		if (!CppConfigurationBean.getInstance().isScte224CadentContentIDGenerationEnabled()) {
			return;
		}
		//Step1: generate Cadent Signal Id
		boolean binary = baseResponseSignal.getBinaryData() != null;

		Map<QName, String> attributes = supportedSegment.getOtherAttributes();
		XMLGregorianCalendar startUTCTimeWithAddedFreq = null;
		XMLGregorianCalendar stopUtcTimeWithDuration = null;
		Long duration = null;
		//Retrieve any information from previous entered response signal and then utilize it for Content Id signal generation

		//For Program Start, the content id starts with Program start signal.
		//For program End, Content ID starts and ends at Program end signal. So that the end time can be shown properly with signaltime having delta applied.
		
		Long eventScheduleStartTime = getStartTimeFromLedgerForEventSchedule(aqpt, mediaLedger);
		
		if (eventScheduleStartTime != null) {
			startUTCTimeWithAddedFreq = SCCResponseUtil.generateUTCPoint(eventScheduleStartTime).getUtcPoint();
		}
		duration = getDurationFromLedger(mediaLedger);
		if (duration != null) {
			long eventStopTimeAddedDelta = getStopTimeFromLedgerForEventSchedule(baseResponseSignal, duration, mediaLedger, aqpt);
			stopUtcTimeWithDuration = SCCResponseUtil.generateUTCPoint(eventStopTimeAddedDelta).getUtcPoint();
		}

		String findContentIDAqSignalId = getOrAddContentIdAcquisitionSignalIdFromLedger();
		ResponseSignalType respSignalContentIdentification = createContentIndentficationRespSignal(aqpt, matchedMediaObject.getSignalid(), startUTCTimeWithAddedFreq,
				stopUtcTimeWithDuration, findContentIDAqSignalId, binary, supportedSegment.getSegmentEventId(), actionName, ptsTimes, pts_adjustment, duration, attributes,
				responseSegmentTypeId, responseSegmentTypeId);
		notificationResponse.getResponseSignal().add(respSignalContentIdentification);

	}

	private String getOrAddContentIdAcquisitionSignalIdFromLedger() {
		String findContentIDAqSignalId = matchedMediaLedger.getAcquisitionSignalIds().get(SegmentType.CONTENT_IDENTIFICATION);
		if (StringUtils.isBlank(findContentIDAqSignalId)) {
			findContentIDAqSignalId = UUIDUtils.getBase64UrlEncodedUUID();
			matchedMediaLedger.addAcquisitionSignalId(SegmentType.CONTENT_IDENTIFICATION, findContentIDAqSignalId);//Create new acquisitionSignalId for content id
		}
		return findContentIDAqSignalId;
	}


	protected void generateProgramEndSection(HashMap<String, String> ptsTimes, String pts_adjustment, AcquisitionPoint aqpt,
			SignalProcessingNotificationType notificationResponse, String actionName, MediaLedger mediaLedger, SegmentType responseSegmentTypeId,
			ResponseSignalType responseSignalType, Date currentSystemTimeWithAddedDelta, SegmentationDescriptorType segment) throws DatatypeConfigurationException {
		//Step1: generate Cadent Signal Id
		final String upidStr = ESAMHelper.generateUpidString(matchedMediaObject.getSignalid());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

		//ModifyBaseResponseSignal if sent, if not then create new and add in Notification Response Signal List
		if (responseSignalType == null) {
			responseSignalType = new ResponseSignalType();
			notificationResponse.getResponseSignal().add(responseSignalType);
		}

		if (responseSignalType != null) {
			SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
			responseSignalType.setAction(actionName);
			responseSignalType.setSignalPointID(matchedMediaObject.getSignalid());
			scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
			scte35Pnt.getSegmentationDescriptorInfo().add(segment);
			segment.setSegmentTypeId(responseSegmentTypeId.getSegmentTypeId());
			Map<QName, String> attributes = segment.getOtherAttributes();
			ESAMObjectCreationHelper.setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upid, segment);
			
			XMLGregorianCalendar stopUTCTimeWithDelta = SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta.getTime()).getUtcPoint();

			// set UTC for program start ResponseSignal
			responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(stopUTCTimeWithDelta.toGregorianCalendar().getTimeInMillis()));
			setDeviceRestrictionsAndNoRegionalBlackoutFlags(scte35Pnt);
			boolean binary = responseSignalType.getBinaryData() != null;
			setBinaryOrXMLSegmentDescriptorInResponseSignal(ptsTimes, pts_adjustment, responseSignalType, scte35Pnt, binary, aqpt);

		}

	}

	protected void generateProgramStartSection(HashMap<String, String> ptsTimes, String pts_adjustment, AcquisitionPoint aqpt, String actionName, MediaLedger mediaLedger,
			SegmentType responseSegmentTypeId,
			ResponseSignalType responseSignalType, Date currentSystemTimeWithAddedDelta, SegmentationDescriptorType segment) throws DatatypeConfigurationException {
		//Step1: generate Cadent Signal Id
		final String upidStr = ESAMHelper.generateUpidString(matchedMediaObject.getSignalid());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

		Long duration = getDurationFromLedger(mediaLedger);//startTime

		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();

		scte35Pnt.getSegmentationDescriptorInfo().add(segment);

		responseSignalType.setAction(actionName);
		responseSignalType.setSignalPointID(matchedMediaObject.getSignalid());
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());

		segment.setSegmentTypeId(responseSegmentTypeId.getSegmentTypeId());
		Map<QName, String> attributes = segment.getOtherAttributes();
		ESAMObjectCreationHelper.setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upid, segment);
		if (duration != null) {
			attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
			segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		} else {
			attributes.remove(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
			segment.setDuration(null);
		}
		
		long signalTime = getStartTimeFromLedgerIfExists(mediaLedger, currentSystemTimeWithAddedDelta.getTime());

		// set UTC for program start ResponseSignal
		responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(signalTime));
		setDeviceRestrictionsAndNoRegionalBlackoutFlags(scte35Pnt);
		boolean binary = responseSignalType.getBinaryData() != null;
		setBinaryOrXMLSegmentDescriptorInResponseSignal(ptsTimes, pts_adjustment, responseSignalType, scte35Pnt, binary, aqpt);

	}

	protected SegmentationDescriptorType findDescriptorFromRequest(SegmentType responseSegmentTypeId, ResponseSignalType baseResponseSignalType) {
		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		//Take the Segmentation Descriptor from correct request segmentation descriptor list
		if (baseResponseSignalType.getSCTE35PointDescriptor() != null && !baseResponseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().isEmpty()) {
			Optional<SegmentationDescriptorType> descInfo = baseResponseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().stream()
					.filter(desInfo -> (desInfo.getSegmentTypeId() != null && desInfo.getSegmentTypeId().shortValue() == responseSegmentTypeId.getSegmentTypeId())).findFirst();
			if (descInfo.isPresent()) {
				segment = CppUtil.deepCopy(descInfo.get());
			}
		}
		return segment;
	}
	


	/*
	 * Reset Binary only in SCC Response (On Last Layer0
	 */
	protected void setBinaryOrXMLSegmentDescriptorInResponseSignal(final HashMap<String, String> ptsTimes, final String pts_adjustment,
			ResponseSignalType responseSignalType, SCTE35PointDescriptorType scte35Pnt, boolean binary, AcquisitionPoint aqpt) {
		long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);

		String ptsTimeInBinary = (ptsTimes != null && StringUtils.isNotBlank(ptsTimes.get(responseSignalType.getAcquisitionSignalID())))
				? ptsTimes.get(responseSignalType.getAcquisitionSignalID()) : EMPTY_STRING;

		if (StringUtils.isNotBlank(ptsTimeInBinary)) {
			LOGGER.debug(() -> "ptsTimeInBinary has been received for AcquisitionSignalID = " + responseSignalType.getAcquisitionSignalID()
					+ ", hence acquistion point signal time offset will be added in the pts time.");

			Map<String, String> ptsTimePlusOffsetMap = Scte35BinaryUtil.adjustAQPointSignalOffsetInPTS(ptsTimes, responseSignalType.getAcquisitionSignalID(), false, signalTimeOffset);
			ptsTimeInBinary = ptsTimePlusOffsetMap.get(CppConstants.PTS_TIME_PLUS_OFFSET_IN_BINARY);
		} else {
			LOGGER.debug(() -> "ptsTimeInBinary has not been received for AcquisitionSignalID " + responseSignalType.getAcquisitionSignalID()
					+ ", hence acquistion point signal time offset will not be added in the pts time.");
		}

		if (binary) {
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pnt, ptsTimeInBinary, pts_adjustment);
			BinarySignalType binarySignal = new BinarySignalType();
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
			binarySignal.setSignalType("SCTE35");
			responseSignalType.setBinaryData(binarySignal);
		} else {
			responseSignalType.setSCTE35PointDescriptor(scte35Pnt);
		}
		Scte35BinaryUtil.adjustPtsTimeInStream(responseSignalType.getStreamTimes(), signalTimeOffset);
	}

	private void setDeviceRestrictionsAndNoRegionalBlackoutFlags(SCTE35PointDescriptorType scte35Pnt) {
		scte35Pnt.getSegmentationDescriptorInfo().stream().forEach(sdi -> {
			sdi.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG),
					matchedMediaPointObject.getNoRegionalBlackoutFlag() != null
							&& matchedMediaPointObject.getNoRegionalBlackoutFlag() ? "1" : "0");
			sdi.getOtherAttributes().put(new QName(CppConstants.DEVICE_RESTRICTIONS),
					String.valueOf(matchedMediaPointObject.getDeviceRestrictions()));
			sdi.getOtherAttributes().put(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG), "0");
		});
	}

	private long getStartTimeFromLedgerIfExists(MediaLedger mediaLedger, long signalTime) {
		//ToDO By Avnee
		if (mediaLedger != null && mediaLedger.getMediaTransactions() != null && !mediaLedger.getMediaTransactions().isEmpty()) {
			MediaTransaction programStartTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			
			if (programStartTransaction != null) {
				
				signalTime = programStartTransaction.getSignalTimeInMS();			}
		}
		return signalTime;
	}


	/*
	 * (non-Javadoc)
	 * This function is only overwritten in CONFIRMED CASE. You may find the implementation only in Program START derived classed at this moment.
	 * It is used to serve the SignalStateRequest at this time, Customer only want to know what was the LAST confirmed event that we acknowledged to Acquisition point. 
	 * In future they may/may not like to see any. Based on need, it function can be overwritten for other signals.
	 */
	@Override
	public void saveConfirmedSignalIDInContextForSSR(String acquisitionSignalId, String acquisitionPointId, MuleEventContext context) {
		// TODO NO DEFAULT BEHAVIOR		
	}


	protected Map<String, I03ResponseModelDelta> addAlternateContentUrlForGivenZone(String responseSignalPointId, String alternateContentLocation, String zoneIdentity) {
		Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap = new HashMap<String, I03ResponseModelDelta>();
		I03ResponseModelDelta i03ResponseModelDelta = new I03ResponseModelDelta();
		AlternateContentTypeModel delta = new AlternateContentTypeModel();
		i03ResponseModelDelta.getAlternateContentIDList().add(delta);
		delta.setAltContentIdentity(alternateContentLocation);
		delta.setZoneIdentity(zoneIdentity);
		responseToI03ResponseModelDeltaMap.put(responseSignalPointId, i03ResponseModelDelta);
		return responseToI03ResponseModelDeltaMap;
	}

	protected Map<String, I03ResponseModelDelta> addAlternateContentUrlForAllZones(String signalPointID, List<ApplyorRemove> applyList) {
		Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap = new HashMap<String, I03ResponseModelDelta>();
		I03ResponseModelDelta i03ResponseModelDelta = new I03ResponseModelDelta();
		for (ApplyorRemove apply : applyList) {
			if (apply.getServiceZones() != null) {
				for (String serviceZone : apply.getServiceZones()) {
					AlternateContentTypeModel delta = new AlternateContentTypeModel();
					delta.setAltContentIdentity(apply.getAltSourceValue());
					delta.setZoneIdentity(serviceZone);
					i03ResponseModelDelta.getAlternateContentIDList().add(delta);
				}
			}
		}
		responseToI03ResponseModelDeltaMap.put(signalPointID, i03ResponseModelDelta);
		return responseToI03ResponseModelDeltaMap;
	}

	public void setMatchedMediaObject(Media matchedMediaObject) {
		this.matchedMediaObject = matchedMediaObject;
	}

	public void setMatchedMediaPointObject(MediaPoint matchedMediaPointObject) {
		this.matchedMediaPointObject = matchedMediaPointObject;
	}

	public void setMatchedMediaLedger(MediaLedger matchedMediaLedger) {
		this.matchedMediaLedger = matchedMediaLedger;
	}

	@Override
	public Long getDurationForPersistingInMediaLedger(MediaPoint matchedMediaPoint, Media matchedMedia, MediaLedger matchedMediaLedger, String acquisitionPointId, long signalTime,
			Short segmentTypeId) {
		//For contentId we send noop/delete, so we never calculate duration 
		return 0l;
	}

	@Override
	public Long getDurationFromLedger(MediaLedger mediaLedger) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, MediaLedger mediaLedger) {
		return null;
	}
	
	@Override
	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration, MediaLedger mediaLedger, AcquisitionPoint aqpt) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void getOrCreateMediaLedger(String acquisitionPointIdentity) {
		matchedMediaLedger = DataManagerFactory.getSCTE224DataManager().getAcquisitionPointMediaLedger(acquisitionPointIdentity, matchedMediaObject.getSignalid());
		if (matchedMediaLedger == null) {
			matchedMediaLedger = MediaLedger.build(acquisitionPointIdentity, false);
		}
	}

}
