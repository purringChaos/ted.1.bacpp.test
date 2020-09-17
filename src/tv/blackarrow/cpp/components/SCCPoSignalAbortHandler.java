package tv.blackarrow.cpp.components;

import static tv.blackarrow.cpp.utils.SpliceCommandType.SPLICE_INSERT;
import static tv.blackarrow.cpp.utils.SpliceCommandType.TIME_SIGNAL;
import static tv.blackarrow.cpp.utils.SpliceCommandType.valueOf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;
import tv.blackarrow.cpp.utils.SpliceCommandType;


//Handler for PO Signal Abort
public class SCCPoSignalAbortHandler {

	private static Logger LOGGER = LogManager.getLogger(SCCPoSignalAbortHandler.class);


	public static void handleSignalAbortDecision(final ResponseSignalType responseSignalType, final SCTE35PointDescriptorType scte35Pt, final Map<String, String> ptsMap,
			final HashMap<String, ConfirmedPlacementOpportunity> decisions, final String ptsTime, AcquisitionPoint aqpt) {

		if(responseSignalType == null || scte35Pt == null || ptsMap == null ||  decisions == null || aqpt == null){
			LOGGER.error(()->"Invalid request for PO Signal Abort Desicion.");
			return;
		}

		String signalId = responseSignalType.getSignalPointID();
		DataManager dataManager = DataManagerFactory.getInstance();

		ConfirmedPlacementOpportunity abortCpo = null;
		//Identify the last confirmed PO to be aborted.
		if(signalId == null) {
			LOGGER.debug(()->"SignalId not in the signal abort request. Retriving the last PO to abort");
			SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
			if(cursor==null) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.debug(()->"No signal processor cursor created for AP: " + aqpt.getAcquisitionPointIdentity() + ". So deleting/skipping the signal abort request.");
				return;
			}
			if(cursor.getLastConfirmedPOUTCTime() <= 0) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.debug(()->"No PO comfirmed for AP: " + aqpt.getAcquisitionPointIdentity() + ". Deleting/Skipping the signal abort request.");
				return;
			}

			abortCpo = dataManager.getConfirmedPlacementOpportunity(aqpt.getAcquisitionPointIdentity(), cursor.getLastConfirmedPOUTCTime());
			if(abortCpo == null) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.debug(()->"Unable to find CPO at AP: " + aqpt.getAcquisitionPointIdentity() + " at UTC time "+ cursor.getLastConfirmedPOUTCTime() +". Deleting/Skipping the signal abort request.");
				return;
			}
			signalId = abortCpo.getSignalId();
		}

		if(abortCpo == null){
			abortCpo = dataManager.getConfirmedPlacementOpportunity(signalId);
		}

		if(abortCpo==null) {
			setResponseSignalAction (responseSignalType, aqpt);;
			LOGGER.debug("CPO for signal id: " + signalId + " is not found. Sending a DELETE/NOOP.");
			return;
		}
		LOGGER.debug("Servicing Signal Abort request for PO: " + signalId);

		long abortUTCTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime().getTime();

		long poStopTime = abortCpo.getUtcSignalTime() + abortCpo.getLongestDuration();

//		if(System.currentTimeMillis() > poStopTime) {
//			logger.debug("The PO requested for signal abort has ended already. Sending a NOOP. The SignalId is "+ signalId);
//			responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
//			return;
//		}

		// request abort time is beyond the PO ending time
		if (abortUTCTime > poStopTime){
			LOGGER.debug("The PO abort time is passed PO end time. Sending a DELETE/NOOP. The SignalId is "+ signalId);
			setResponseSignalAction (responseSignalType, aqpt);
			return;
		}

		// request abort time is before the PO starting time
		if (abortUTCTime < abortCpo.getUtcSignalTime()){
			LOGGER.debug("The PO abort time is before PO starting time. Sending a DELETE/NOOP. The SignalId is "+ signalId);
			setResponseSignalAction (responseSignalType, aqpt);
			return;
		}


		if(!abortCpo.isAborted()) {
			abortCpo.setAbortTime(abortUTCTime);
			dataManager.putConfirmedPlacementOpportunity(abortCpo);
		}

		ptsMap.put(signalId, ptsTime);
		responseSignalType.setSignalPointID(signalId);
		decisions.put(signalId, abortCpo);
	}

	public static void processSignalAbort(SignalProcessingNotificationType notification,
										  ResponseSignalType responseSignalType,
										  SCTE35PointDescriptorType scte35,
										  String ptsTime,
										  String pts_adjustment,
										  ConfirmedPlacementOpportunity cpo,
										  List<ResponseSignalType> additionalResponseSignalTypes,
			long currentSystemTimeWithAddedDelta,
										  DataManager dataManager, AcquisitionPoint aqpt) {


		//		byte[] upid = (ESAMHelper.UPID_PREFIX + cpo.getSignalId()).getBytes();
		final boolean customizeResponseForTWC = CppConfigurationBean.getInstance().isSendCustomAbortSCCResponse();
		Long  segmentEventId = SCCResponseUtil.getIdForCountDownQueueCheck(responseSignalType);
		// non-binary case
		SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();

		//long pts = ptsTime == null? 0:Long.parseLong(ptsTime, 2);
		long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);

		//retrieve signal id
		SegmentationDescriptor segmentationDescriptor = dataManager.getSegmentationDescriptor(responseSignalType.getSignalPointID());
		Short segmentTypeId = segmentationDescriptor.getSegmentationTypeId();
		// binary case
		if(scte35Des == null){
			scte35Des = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(responseSignalType.getBinaryData().getValue());
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Des, new StringBuilder(), new StringBuilder());
		}

		if(SPLICE_INSERT == valueOf(scte35Des.getSpliceCommandType())) {

			//Check spliceImmediateFlag
			boolean isSpliceImmediate = false;

			if(scte35Des.getSpliceInsert().getOtherAttributes() != null &&
					!scte35Des.getSpliceInsert().getOtherAttributes().isEmpty()) {
				String value = scte35Des.getSpliceInsert().getOtherAttributes().get(QName.valueOf("spliceImmediateFlag"));
				if (value != null && (value.equals("1") || Boolean.valueOf(value))){
					isSpliceImmediate = true;
				}
			}

			//CS2-387 UTC is set to current system time with added delta
			if(!Boolean.valueOf(String.valueOf(scte35Des.getSpliceInsert().isSpliceEventCancelIndicator())) &&
			   !Boolean.valueOf(String.valueOf(scte35Des.getSpliceInsert().isOutOfNetworkIndicator())) &&
					!isSpliceImmediate) {
				responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta));
			}

			// we need to replace the SpliceInsert section with TimeSignal section
			scte35Des.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());
			scte35Des.setSpliceInsert(null);

			// clear the segmentation descriptor coming in the request
			scte35Des.getSegmentationDescriptorInfo().clear();
			// add one segmentation descriptor
			if(aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD){
				segmentTypeId = SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId();
			}

			SegmentationDescriptorType segment = generateSegmentForAbort(cpo, segmentEventId, segmentTypeId, customizeResponseForTWC);
			scte35Des.getSegmentationDescriptorInfo().add(segment);
			if(aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD){
				for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
					if(seg.getSegmentTypeId() == SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId() || seg.getSegmentTypeId() == SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()){
						seg.setSegmentTypeId(SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId());
					}
				}
			}
		} else if(TIME_SIGNAL == valueOf(scte35Des.getSpliceCommandType())) {
			//CS2-387 UTC is set to current system time with added delta

			responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta));
			for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
				updateSegmentForAbort(seg, segmentTypeId, cpo, segmentEventId, customizeResponseForTWC);
			}
		}

		// out point binary case
		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		if(binarySignal != null) {
			byte[] binary = generateBinaryDataForAbort(ptsTime, pts_adjustment, scte35Des, cpo, customizeResponseForTWC, aqpt);
			binarySignal.setValue(binary);
		}

		List<ConditioningInfoType> conditioningInfoList = notification.getConditioningInfo();
		conditioningInfoList.add(getConditioningInfoForAbort(responseSignalType.getAcquisitionSignalID(), cpo, false/*it was decided to send the duration as zero always*/));
	}

	public static void updateSegmentForAbort(final SegmentationDescriptorType segment, final short segmentTypeId, final ConfirmedPlacementOpportunity cpo,
			final Long segmentEventId, final boolean customizeResponseForTWC) {
		if(customizeResponseForTWC) {
			segment.setSegmentationEventCancelIndicator(null);
			setSegmentationFieldValuesForAbort(cpo, CppConstants.SEGMENTATION_TYPE_MAP.get(segmentTypeId), segmentEventId, segment);
		} else {//Remove all other fields
			segment.setUpid(null);
			segment.setSegmentTypeId(null);
			segment.setSegmentNum(null);
			segment.setSegmentsExpected(null);
			segment.setUpidType(null);
			segment.setSegmentEventId(segmentEventId);

			try {
				segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(0));
			} catch (DatatypeConfigurationException e) {
				LOGGER.error(()->e.getMessage() + "duration " + 0, e);
			}
		}
	}

	public static SegmentationDescriptorType generateSegmentForAbort(ConfirmedPlacementOpportunity cpo, Long segmentEventId,
			final short segmentTypeId, final boolean customizeResponseForTWC) {
		SegmentationDescriptorType segment = null;
		if(customizeResponseForTWC) {
			//segment = generateCustomizedSegmentForAbort(cpo, PLACEMENT_OPPORTUNITY_END.getSegmentTypeId(), segmentEventId);
			segment = generateCustomizedSegmentForAbort(cpo, CppConstants.SEGMENTATION_TYPE_MAP.get(segmentTypeId), segmentEventId);
		} else {
			segment = SCCResponseUtil.generateSegment(null, new Long(0), CppConstants.SEGMENTATION_TYPE_MAP.get(segmentTypeId), true, segmentEventId, (short)0, (short)0, (short)9);
		}
		return segment;
	}

	private static SegmentationDescriptorType generateCustomizedSegmentForAbort(ConfirmedPlacementOpportunity cpo, short segmentTypeId, Long segmentEventId) {
		final SegmentationDescriptorType segment = new SegmentationDescriptorType();
		setSegmentationFieldValuesForAbort(cpo, segmentTypeId, segmentEventId, segment);
		return segment;
	}

	private static void setSegmentationFieldValuesForAbort(ConfirmedPlacementOpportunity cpo, short segmentTypeId, Long segmentEventId, final SegmentationDescriptorType segment) {
		final String upidStr = ESAMHelper.generateUpidString(cpo.getSignalId());
		final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
		SCCResponseUtil.setSegmentationFieldValues(upid, cpo.getRemainingDuration(), segmentTypeId, false, segmentEventId, (short)0, (short)0, (short)9, segment);
	}

	private static ConditioningInfoType getConditioningInfoForAbort(String acquisitionSignalID, ConfirmedPlacementOpportunity cpo
			, final boolean customizeResponseForTWC) {
		ConditioningInfoType conditioningInfoType = new ConditioningInfoType();
		conditioningInfoType.setAcquisitionSignalIDRef(acquisitionSignalID);
		long duration = customizeResponseForTWC ? cpo.getRemainingDuration() : 0;
		try {
			conditioningInfoType.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		} catch (DatatypeConfigurationException e) {
			LOGGER.error(()->e.getMessage() + "duration " + duration, e);
		}
		return conditioningInfoType;
	}

	private static byte[] generateBinaryDataForAbort(final String ptsTime, final String pts_adjustment, SCTE35PointDescriptorType scte35Des, final ConfirmedPlacementOpportunity cpo
			, final boolean customizeResponseForTWC, AcquisitionPoint acquisitionPoint) {
		byte[] binary = null;
		if(customizeResponseForTWC) {
			final String upidStr = ESAMHelper.generateUpidString(cpo.getSignalId());
			final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
			binary = SCCResponseUtil.generateBinaryData(upid, cpo.getRemainingDuration(), scte35Des, ptsTime, pts_adjustment, false, acquisitionPoint);
		} else {
			binary = SCCResponseUtil.generateBinaryData(null, new Long(0), scte35Des, ptsTime, pts_adjustment, true, acquisitionPoint);
		}
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Generated binary: " + binary);
		}
		return binary;
	}

	//change request item 1: BAS-26525
	private static void setResponseSignalAction (final ResponseSignalType responseSignalType, final AcquisitionPoint aqpt){
		responseSignalType.setAction((aqpt == null || aqpt.isSccDeleteEmptyBreak()) ?
				CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
	}
}
