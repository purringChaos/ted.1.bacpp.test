package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

import static tv.blackarrow.cpp.utils.SCCResponseUtil.getIdForCountDownQueueCheck;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventTransaction;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;

public abstract class SCCBaseResponseProcessor implements IBaseResponseProcessor {
	private static Logger logger = LogManager.getLogger(SCCBaseResponseProcessor.class);

	protected void saveLedgerInCouchbase(AcquisitionPoint aqpt, SegmentationDescriptorType descriptorInfo, SchedulelessAltEventLedger ledger, Long duration,
			Date requestTime) {

		byte[] upidBinary = descriptorInfo.getUpid();
		String upidHex = new HexBinaryAdapter().marshal(upidBinary);
		Short segmentTypeId = descriptorInfo.getSegmentTypeId() != null ? descriptorInfo.getSegmentTypeId() : 0;
		SchedulelessAltEventTransaction transaction = SchedulelessAltEventTransaction.build(requestTime.getTime(), segmentTypeId, duration, upidHex, ESSRequestType.SCC);
		ledger.addSignalTransaction(transaction);
		DataManagerFactory.getSchedulelessAltEventDataManager().saveAcquisitionPointSchedulelessAltEventLedger(ledger, aqpt.getAcquisitionPointIdentity(), upidHex);
	}

	@Override
	public Long getDurationFromLedger(SchedulelessAltEventLedger schedulelessAltEventLedger) {
		//Should be null as Program Start doesn't define duration, rather it will be calculated at program end
		return null;
	}

	protected long applyAQSignalOffsetInEventStartTime(AcquisitionPoint aqpt, long requestTime) {
		return requestTime + aqpt.getSignalTimeOffset();
	}

	private long applyAQSignalOffsetInEventEndTime(AcquisitionPoint aqpt, long eventStopTimeWithOffset) {
		long eventEndTime = eventStopTimeWithOffset + aqpt.getSignalTimeOffset();

		return eventEndTime;
	}

	protected Map<String, I03ResponseModelDelta> addAlternateContentUrls(String responseSignalPointId, String alternateContentLocation) {
		Map<String, I03ResponseModelDelta> responseToI03ResponseModelDelta = new HashMap<>();
		I03ResponseModelDelta list = new I03ResponseModelDelta();
		AlternateContentTypeModel delta = new AlternateContentTypeModel();
		list.getAlternateContentIDList().add(delta);
		delta.setAltContentIdentity(alternateContentLocation);
		responseToI03ResponseModelDelta.put(responseSignalPointId, list);
		return responseToI03ResponseModelDelta;
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

			}

			if (responseSignalType.getBinaryData() != null) {
				responseSignalType.setSCTE35PointDescriptor(null);
			}
			//do not adjust any offset in either UTC point or PTS time.
		}
	}

	private ConditioningInfoType getConditioningInfo(final String acquisitionSignalId, final boolean isMediaEnded, Long contentDuration, final AcquisitionPoint aqpt,
			final Short requestedSegmentTypeId) throws DatatypeConfigurationException {
		ConditioningInfoType conditioningInfo = new ConditioningInfoType();
		conditioningInfo.setAcquisitionSignalIDRef(acquisitionSignalId);
		if ((contentDuration != null) && (contentDuration < 0)) {
			contentDuration = 0l;
		}
		if (contentDuration != null) {
			Duration duration = JavaxUtil.getDatatypeFactory().newDuration(contentDuration);
			conditioningInfo.setDuration(duration);
		}
		return conditioningInfo;
	}

	private ResponseSignalType createContentIndentficationRespSignal(AcquisitionPoint aqpt, String signalPointId, XMLGregorianCalendar startTimeWithOffset,
			XMLGregorianCalendar stopTimeWithOffset, final String acquisitionSignalId, final boolean binary, final Long segmentEventId,
			final ResponseSignalAction contentIdentificationSignalAction, final String ptsTimeInBinary, final String pts_adjustment, final Long contentDuration,
			final Map<QName, String> attributes, final SegmentType requestSegmentTypeIdEnum, SegmentType responseSegmentTypeId) throws DatatypeConfigurationException {
		SCTE35PointDescriptorType scte35Pnt;
		ResponseSignalType respSignalContentIdentification = new ResponseSignalType();
		String upidStr = ESAMHelper.generateUpidString(signalPointId);
		byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		respSignalContentIdentification.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		respSignalContentIdentification.setAcquisitionSignalID(acquisitionSignalId);
		respSignalContentIdentification.setAction(contentIdentificationSignalAction == null ? null : contentIdentificationSignalAction.toString());
		respSignalContentIdentification.setSignalPointID(signalPointId);

		EventScheduleType eventSchedule = getEventSchedule(startTimeWithOffset.toGregorianCalendar().getTimeInMillis(),
				stopTimeWithOffset != null ? stopTimeWithOffset.toGregorianCalendar().getTimeInMillis() : null, aqpt.getContentIDFrequency() * 1000, aqpt,
				requestSegmentTypeIdEnum);

		respSignalContentIdentification.setEventSchedule(eventSchedule);
		respSignalContentIdentification.setUTCPoint(eventSchedule.getStartUTC()); // need set UTCpoint

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
		}
		ESAMObjectCreationHelper.setBasicSegmentInfoForContentIDSignal(upid, segment);
		ESAMObjectCreationHelper.setSegmentDescriptorAttributesInResponseSignal(attributes, segment);

		respSignalContentIdentification.setSCTE35PointDescriptor(scte35Pnt);

		return respSignalContentIdentification;
	}

	private EventScheduleType getEventSchedule(final long startOrCurrentTimeWithOffset, Long stopTimeWithOffset, long frequency, final AcquisitionPoint aqpt,
			final SegmentType requestSegmentTypeIdEnum) throws DatatypeConfigurationException {

		//fix the issue that endTime is less than start time, when getting the program_end immediately after program start.
		if ((stopTimeWithOffset != null) && (stopTimeWithOffset <= startOrCurrentTimeWithOffset)) {
			stopTimeWithOffset = startOrCurrentTimeWithOffset;
			frequency = 0;
		}

		EventScheduleType eventSchedule = new EventScheduleType();
		eventSchedule.setStartUTC(SCCResponseUtil.generateUTCPoint(startOrCurrentTimeWithOffset));
		eventSchedule.setInterval(JavaxUtil.getDatatypeFactory().newDuration(frequency));
		if (stopTimeWithOffset != null) {
			eventSchedule.setStopUTC(SCCResponseUtil.generateUTCPoint(stopTimeWithOffset));
		}
		return eventSchedule;
	}

	protected void generateConditioningInfo(SignalProcessingNotificationType notificationResponse, String acquisitionSignalId, AcquisitionPoint aqpt,
			SegmentType requestSegmentTypeIdEnum, SchedulelessAltEventLedger schedulelessAltEventLedger) throws DatatypeConfigurationException {
		List<ConditioningInfoType> conditioningInfoList = notificationResponse.getConditioningInfo();
		Long duration = getDurationFromLedger(schedulelessAltEventLedger);
		ConditioningInfoType conditioningInfo = getConditioningInfo(acquisitionSignalId, schedulelessAltEventLedger.isAltEventEnded(), duration, aqpt,
				requestSegmentTypeIdEnum.getSegmentTypeId());
		conditioningInfoList.add(conditioningInfo);
	}

	protected void generateContentIdSection(SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes,
			HashMap<String, String> ptsAdjustments, AcquisitionPoint aqpt, SignalProcessingNotificationType notificationResponse, ResponseSignalType programStartResponse16Section,
			ResponseSignalAction actionName, SchedulelessAltEventLedger schedulelessAltEventLedger, SegmentType responseSegmentTypeId) throws DatatypeConfigurationException {
		//Step1: generate Cadent Signal Id
		boolean binary = false;
		String pts_adjustment = null;
		String ptsTimePlusOffsetInBinary = null;
		Map<QName, String> attributes = null;
		XMLGregorianCalendar startUTCTimeWithOffset = null;
		XMLGregorianCalendar stopUTCTimeWithOffset = null;
		Long segmentEventId = 0l;
		Long duration = null;
		String signalId = null;
		//Retrieve any information from previous entered response signal and then utilize it for Content Id signal generation
		if (programStartResponse16Section != null) {
			binary = programStartResponse16Section.getBinaryData() != null;
			pts_adjustment = (ptsAdjustments == null) || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33)
					: ptsAdjustments.get(programStartResponse16Section.getAcquisitionSignalID());
			ptsTimePlusOffsetInBinary = getptsTimePlusOffsetInBinary(ptsTimes, programStartResponse16Section, aqpt.getSignalTimeOffset());
			//For Program Start, the content id starts with Program start signal.
			//For program End, Content ID starts and ends at Program end signal. So that the end time can be shown properly with signal offset applied.
			Long eventScheduleStartTime = getStartTimeFromLedgerForEventSchedule(aqpt, schedulelessAltEventLedger);
			if (eventScheduleStartTime != null) {
				startUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(eventScheduleStartTime).getUtcPoint();
			}
			//Do not send duration and stop UTC in response.
			duration = getDurationFromLedger(schedulelessAltEventLedger);
			if (duration != null) {
				long eventStopTimeWithOffset = getStopTimeFromLedgerForEventSchedule(programStartResponse16Section, duration);
				stopUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(eventStopTimeWithOffset).getUtcPoint();
			}
			attributes = extractAttributes(programStartResponse16Section.getSCTE35PointDescriptor());
			segmentEventId = getIdForCountDownQueueCheck(programStartResponse16Section);
			signalId = programStartResponse16Section.getSignalPointID();
		}

		if (programStartResponse16Section != null) {
			ResponseSignalType respSignalContentIdentification = createContentIndentficationRespSignal(aqpt, signalId, startUTCTimeWithOffset, stopUTCTimeWithOffset,
					acquisitionSignalId, binary, segmentEventId, actionName, ptsTimePlusOffsetInBinary, pts_adjustment, duration, attributes, responseSegmentTypeId,
					responseSegmentTypeId);
			notificationResponse.getResponseSignal().add(respSignalContentIdentification);
		}
	}

	@Override
	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration) {
		long originalRequestSignalTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();//This already has AQ offset applied
		originalRequestSignalTime = originalRequestSignalTime + duration;
		return originalRequestSignalTime;
	}

	private Map<QName, String> extractAttributes(SCTE35PointDescriptorType scte35DesType) {
		Map<QName, String> attributes = null;
		if ((scte35DesType != null) && !scte35DesType.getSegmentationDescriptorInfo().isEmpty()) {
			attributes = scte35DesType.getSegmentationDescriptorInfo().get(0).getOtherAttributes();
			String segDurationFlag = attributes.get(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
			if ((segDurationFlag != null) && "1".equals(segDurationFlag) && (scte35DesType.getSegmentationDescriptorInfo().get(0).getDuration() == null)) {
				attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "0");
			}
		}
		return attributes;
	}

	private String getptsTimePlusOffsetInBinary(HashMap<String, String> ptsTimes, ResponseSignalType responseSignalType, long signalTimeOffset) {
		String ptsTimePlusOffsetInBinary = null;
		if (ptsTimes != null) {
			String ptsTimeInBinary = ptsTimes.get(responseSignalType.getAcquisitionSignalID());

			try {
				if ((ptsTimeInBinary != null) && !ptsTimeInBinary.isEmpty()) {//old condition
					ptsTimePlusOffsetInBinary = Scte35BinaryUtil.applySignalTimeOffset(ptsTimeInBinary, signalTimeOffset);
				} else {
					ptsTimePlusOffsetInBinary = Scte35BinaryUtil.applySignalTimeOffset("0", signalTimeOffset);
				}
			} catch (Exception stringIndexOutOfBoundsException) {
				ptsTimePlusOffsetInBinary = Scte35BinaryUtil.toBitString(0l, 33);
			}

		}
		return ptsTimePlusOffsetInBinary;
	}

	protected void generateProgramStartSection(SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes,
			HashMap<String, String> ptsAdjustments, AcquisitionPoint aqpt, ResponseSignalType responseSignalType, String actionName,
			SchedulelessAltEventLedger schedulelessAltEventLedger, SegmentType responseSegmentTypeId) throws DatatypeConfigurationException {
		//Step1: generate Cadent Signal Id
		final String cadentSignalId = schedulelessAltEventLedger.getCadentSignalId();
		final String upidStr = ESAMHelper.generateUpidString(cadentSignalId);
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

		if (responseSignalType != null) {
			Long duration = getDurationFromLedger(schedulelessAltEventLedger);//startTime
			Long segmentEventId = getIdForCountDownQueueCheck(responseSignalType);
			SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
			SegmentationDescriptorType segment = new SegmentationDescriptorType();
			responseSignalType.setAction(actionName);
			responseSignalType.setSignalPointID(cadentSignalId);
			scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
			scte35Pnt.getSegmentationDescriptorInfo().add(segment);
			segment.setSegmentEventId(segmentEventId);
			segment.setSegmentTypeId(responseSegmentTypeId.getSegmentTypeId());
			Map<QName, String> attributes = extractAttributes(responseSignalType.getSCTE35PointDescriptor());
			ESAMObjectCreationHelper.setBasicSegmentInfoForProgramStartOrRunoverUnplannedSignal(upid, segment);
			ESAMObjectCreationHelper.setSegmentDescriptorAttributesInResponseSignal(attributes, segment);
			if (duration != null) {
				attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "1");
				segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			}
			ESAMObjectCreationHelper.setSegmentDescriptorAttributesInResponseSignal(attributes, segment);

			long requestTime = getStartTimeFromLedger(schedulelessAltEventLedger, responseSignalType);
			long eventStartTimeWithOffset = applyAQSignalOffsetInEventStartTime(aqpt, requestTime);
			XMLGregorianCalendar startUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(eventStartTimeWithOffset).getUtcPoint();

			// set UTC for program start ResponseSignal
			responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(startUTCTimeWithOffset.toGregorianCalendar().getTimeInMillis()));

			long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);

			responseSignalType.setSCTE35PointDescriptor(scte35Pnt);

			StreamTimesType stts = responseSignalType.getStreamTimes();

			if (stts != null) {
				List<StreamTimeType> sttList = stts.getStreamTime();
				for (StreamTimeType stt : sttList) {
					if (stt.getTimeType().equalsIgnoreCase("PTS")) {
						long time = Long.parseLong(stt.getTimeValue());
						time += (signalTimeOffset * 90);
						stt.setTimeValue(Long.toString(time));
					}
				}
			}
		}
	}


	@Override
	public long getStartTimeFromLedger(SchedulelessAltEventLedger ledger, ResponseSignalType responseSignalType) {
		long requestTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		if ((ledger != null) && (ledger.getSignalTransactions() != null) && !ledger.getSignalTransactions().isEmpty()) {
			SchedulelessAltEventTransaction programStartTransaction = ledger.getProgramStartSignalTransaction(ESSRequestType.SCC);
			if (programStartTransaction != null) {
				requestTime = programStartTransaction.getSignalTimeInMS();
			}
		}
		return requestTime;
	}

	@Override
	public void adjustPtsTime(SignalProcessingNotificationType notification) {
		// TODO Auto-generated method stub

	}

}
