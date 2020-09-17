/**
 *
 */
package tv.blackarrow.cpp.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;

/**
 * @author amit
 *
 */
public enum SegmentType {

	//These values are taken from SCTE 35 2013
	PROGRAM_START(0x10), //16
	PROGRAM_END(0x11), //17
	PROGRAM_EARLY_TERMINATION(0x12),//18
	PROGRAM_RUNOVER_PLANNED(0x15),//21
	PROGRAM_RUNOVER_UNPLANNED(0x16),//22
	PROGRAM_OVERLAP_START(0x17),//23
	PROGRAM_BLACKOUT_OVERRIDE(0x18), //24
	PROVIDER_ADVERTISEMENT_START(0x30),//48
	PROVIDER_ADVERTISEMENT_END(0x31),//49
	DISTRIBUTOR_ADVERTISEMENT_START(0x32),//50
	DISTRIBUTOR_ADVERTISEMENT_END(0x33),//51
	PLACEMENT_OPPORTUNITY_START(0x34), //52
	PLACEMENT_OPPORTUNITY_END(0x35),//53
	CONTENT_IDENTIFICATION(0x01), //1
	CHAPTER_START(0x20), //32
	CHAPTER_END(0x21);//33

	private static final Logger LOGGER = LogManager.getLogger(SegmentType.class);
	private static final Map<Short,SegmentType> lookup = new HashMap<Short, SegmentType>();
	private static final Set<SegmentType> blackoutSignals;
	private static final Set<SegmentType> poSignals;
	private static final Set<SegmentType> inbandSignalsApplicableForOutOfBandBlackouts;

	private static final Set<SegmentType> scte224SCCEventSignals;
	private static final Set<SegmentType> scte224MCCEventSignals;

	private static final Set<SegmentType> schedulelessAltEventSignals;

	static {
	      for(SegmentType segmentType : SegmentType.values()){
	           lookup.put(segmentType.getSegmentTypeId(), segmentType);
	      }
	      Set<SegmentType> signalsValidForBothInbandAndOutOfBandBlackouts = new HashSet<SegmentType>(3);
	      signalsValidForBothInbandAndOutOfBandBlackouts.add(PROGRAM_RUNOVER_PLANNED);
	      signalsValidForBothInbandAndOutOfBandBlackouts.add(PROGRAM_RUNOVER_UNPLANNED);
	      signalsValidForBothInbandAndOutOfBandBlackouts.add(PROGRAM_EARLY_TERMINATION);
	      inbandSignalsApplicableForOutOfBandBlackouts = Collections.<SegmentType>unmodifiableSet(signalsValidForBothInbandAndOutOfBandBlackouts);

	      Set<SegmentType> validBlackoutSignals = new HashSet<SegmentType>(5);
	      validBlackoutSignals.add(PROGRAM_START);
	      validBlackoutSignals.add(PROGRAM_END);
	      validBlackoutSignals.add(CONTENT_IDENTIFICATION);
	      validBlackoutSignals.add(PROGRAM_BLACKOUT_OVERRIDE);
	      validBlackoutSignals.addAll(signalsValidForBothInbandAndOutOfBandBlackouts);
	      blackoutSignals = Collections.<SegmentType>unmodifiableSet(validBlackoutSignals);

	      Set<SegmentType> schedulelessSupportedAltEventSignals = new HashSet<SegmentType>(5);
	      schedulelessSupportedAltEventSignals.add(PROGRAM_START);
	      schedulelessSupportedAltEventSignals.add(PROGRAM_END);
	      schedulelessSupportedAltEventSignals.add(CONTENT_IDENTIFICATION);
	      schedulelessAltEventSignals = Collections.<SegmentType>unmodifiableSet(schedulelessSupportedAltEventSignals);

		Set<SegmentType> validScte224SCCSignals = new HashSet<SegmentType>(5);
		validScte224SCCSignals.add(PROGRAM_START);
		validScte224SCCSignals.add(PROGRAM_OVERLAP_START);
		validScte224SCCSignals.add(CONTENT_IDENTIFICATION);
		validScte224SCCSignals.add(PROGRAM_END);
		validScte224SCCSignals.add(PROGRAM_RUNOVER_UNPLANNED);
		validScte224SCCSignals.add(PROGRAM_BLACKOUT_OVERRIDE);
		validScte224SCCSignals.add(PROGRAM_EARLY_TERMINATION);
		scte224SCCEventSignals = Collections.<SegmentType> unmodifiableSet(validScte224SCCSignals);

		Set<SegmentType> validScte224MCCSignals = new HashSet<SegmentType>(5);
		validScte224MCCSignals.add(PROGRAM_START);
		validScte224MCCSignals.add(CONTENT_IDENTIFICATION);
		validScte224MCCSignals.add(PROGRAM_END);
		scte224MCCEventSignals = Collections.<SegmentType> unmodifiableSet(validScte224MCCSignals);

	      Set<SegmentType> validPOSignals = new HashSet<SegmentType>(5);
	      validPOSignals.add(PLACEMENT_OPPORTUNITY_START);
	      validPOSignals.add(PLACEMENT_OPPORTUNITY_END);
	      validPOSignals.add(PROVIDER_ADVERTISEMENT_START);
	      validPOSignals.add(PROVIDER_ADVERTISEMENT_END);
	      validPOSignals.add(DISTRIBUTOR_ADVERTISEMENT_START);
	      validPOSignals.add(DISTRIBUTOR_ADVERTISEMENT_END);
	      poSignals = Collections.<SegmentType>unmodifiableSet(validPOSignals);
	 }

	private final Integer segmentTypeId;

	private SegmentType(final Integer segmentTypeId){
		this.segmentTypeId = segmentTypeId;
	}

	public short getSegmentTypeId() {
		return segmentTypeId.shortValue();
	}

	public static SegmentType valueOf(Short segmentTypeId){
		SegmentType segmentType = lookup.get(segmentTypeId);
		if(segmentType==null){
			// throw new IllegalArgumentException("This segment type is not supported.");
			LOGGER.debug(()->"Unsupported segment type: " + segmentTypeId);
		}
		return segmentType;
	}

	public static boolean isSignalApplicableForBothInbandAndOutOfBandBlackouts(final SegmentType segmentType){
		return inbandSignalsApplicableForOutOfBandBlackouts.contains(segmentType);
	}

	public static boolean isSignalApplicableForBothInbandAndOutOfBandBlackouts(final Short segmentTypeId){
		return isSignalApplicableForBothInbandAndOutOfBandBlackouts(valueOf(segmentTypeId));
	}

	public static boolean isValidBlackoutSignal(final Short segmentTypeId){
		return blackoutSignals.contains(valueOf(segmentTypeId));
	}

	public static boolean isValidSchedulelessAltEventSignal(final Set<Short> segmentIdList) {
		boolean valid = false;
		if ((segmentIdList != null) && (segmentIdList.size() == 1) && SegmentType.isContentIdentificationSignal(segmentIdList.iterator().next())) {
			valid = true;
		} else {
			for (Short s : segmentIdList) {
				SegmentType segmentType = valueOf(s);
				if (segmentType != null && !SegmentType.CONTENT_IDENTIFICATION.equals(segmentType)) {
					valid = schedulelessAltEventSignals.contains(segmentType);
					break;
				}
			}
		}
		return valid;
	}

	public static boolean isValidPOSignal(final Short segmentTypeId){
		return (segmentTypeId!=null) && poSignals.contains(valueOf(segmentTypeId));
	}

	public static boolean isBlackoutRunoverSignal(final Short segmentTypeId){
		return (valueOf(segmentTypeId) == PROGRAM_RUNOVER_PLANNED) || (valueOf(segmentTypeId) == PROGRAM_RUNOVER_UNPLANNED);
	}

	public static boolean isValidSCTE224SCCEventSignal(final Short segmentTypeId){
		return scte224SCCEventSignals.contains(valueOf(segmentTypeId));
	}

	public static boolean isValidSCTE224MCCEventSignal(final Short segmentTypeId){
		return scte224MCCEventSignals.contains(valueOf(segmentTypeId));
	}

	public static boolean isProgramEndSignal(final Short segmentTypeId) {
		return (segmentTypeId != null) && (PROGRAM_END == valueOf(segmentTypeId));
	}

	public static boolean isProgramStartSignal(final Short segmentTypeId) {
		return (segmentTypeId != null) && (PROGRAM_START == valueOf(segmentTypeId));
	}

	public static boolean isProgramOverlapStartSignal(final Short segmentTypeId) {
		return (segmentTypeId != null) && (PROGRAM_OVERLAP_START == valueOf(segmentTypeId));
	}

	public static boolean isProgramEarlyTerminationSignal(final Short segmentTypeId) {
		return (segmentTypeId != null) && (PROGRAM_EARLY_TERMINATION == valueOf(segmentTypeId));
	}

	public static boolean isProviderAdSignal(final Short segmentTypeId){
		return ((segmentTypeId != null) && (PROVIDER_ADVERTISEMENT_START == valueOf(segmentTypeId))) || (PROVIDER_ADVERTISEMENT_END == valueOf(segmentTypeId));
	}

	public static boolean isDistributorAdSignal(final Short segmentTypeId){
		return ((segmentTypeId != null) && (DISTRIBUTOR_ADVERTISEMENT_START == valueOf(segmentTypeId))) || (DISTRIBUTOR_ADVERTISEMENT_END == valueOf(segmentTypeId));
	}

	public static boolean isContentIdentificationSignal(final Short segmentTypeId){
		return (segmentTypeId != null) && (CONTENT_IDENTIFICATION == valueOf(segmentTypeId)) ;
	}

	public static boolean isPoSignal(final Short segmentTypeId){
		return ((segmentTypeId != null) && (PLACEMENT_OPPORTUNITY_START == valueOf(segmentTypeId))) || (PLACEMENT_OPPORTUNITY_END == valueOf(segmentTypeId));
	}

	public static boolean isAdStartSignal(final Short segmentTypeId){
		return segmentTypeId != null && (PROVIDER_ADVERTISEMENT_START == valueOf(segmentTypeId) || DISTRIBUTOR_ADVERTISEMENT_START == valueOf(segmentTypeId)
				|| PLACEMENT_OPPORTUNITY_START == valueOf(segmentTypeId));
	}

	public static boolean isValidPOSignal(Short segmentTypeId, SCTE35PointDescriptorType scte35Pt, AcquiredSignal signal) {
		List<SegmentationDescriptorType> descriptorInfo = scte35Pt.getSegmentationDescriptorInfo();
		boolean result = SegmentType.isValidPOSignal(segmentTypeId)
				|| ((descriptorInfo.size() > 0) && (descriptorInfo.get(0) != null) && (descriptorInfo.get(0).isSegmentationEventCancelIndicator() != null)
						&& descriptorInfo.get(0).isSegmentationEventCancelIndicator() && !isBlackoutAbortRequest(signal, scte35Pt))
				|| (scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype());
		return result;

	}

	//CS2:474: Delete the Chapter Start/End SCC Signals for All Feeds Implementations
	public static boolean isChapterStartSignal(final Short segmentTypeId) {
		return segmentTypeId != null && (CHAPTER_START == valueOf(segmentTypeId));
	}

	public static boolean isChapterEndSignal(final Short segmentTypeId) {
		return segmentTypeId != null && (CHAPTER_END == valueOf(segmentTypeId));
	}

	public static boolean isBlackoutAbortRequest(final SignalProcessingEventType.AcquiredSignal signal, final SCTE35PointDescriptorType scte35Pt) {
		return isBlackoutAbortRequestWithSignalId(signal) || isBlackoutAbortRequestWithoutSignalId(signal, scte35Pt);
	}

	public static boolean isBlackoutAbortRequestWithSignalId(final SignalProcessingEventType.AcquiredSignal signal) {
		return StringUtils.isNotBlank(signal.getSignalPointID()) && (DataManagerFactory.getInstance().getSingleBlackoutEvent(signal.getSignalPointID().trim()) != null);
	}

	public static boolean isBlackoutAbortRequestWithoutSignalId(final SignalProcessingEventType.AcquiredSignal signal, final SCTE35PointDescriptorType scte35Pt) {
		if (// In case of Time Signal Binary Blackout Abort request, when the request does not have signal id or segment type in the request.
		(scte35Pt != null) && (scte35Pt.getSegmentationDescriptorInfo() != null) && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()
				&& (scte35Pt.getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator() != null)
				&& scte35Pt.getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator()) {

			SignalProcessorCursor cursor = DataManagerFactory.getInstance().getSignalProcessorCursor(signal.getAcquisitionPointIdentity());
			if (cursor == null) {
				return false;
			}
			return cursor.getLastConfirmedBlackoutUTCTime() > cursor.getLastConfirmedPOUTCTime();

		}
		return false;
	}

	public static boolean isValidPOStartSignal(final Short segmentTypeId) {
		return isAdStartSignal(segmentTypeId);
	}

	public static boolean isValidPOEndSignal(final Short segmentTypeId, SCTE35PointDescriptorType scte35Pt, AcquisitionPoint ap, AcquiredSignal signal) {
		return isAdEndSignal(segmentTypeId) || isPOAbortRequest(scte35Pt, ap, signal.getSignalPointID());
	}

	public static boolean isAdEndSignal(final Short segmentTypeId) {
		return segmentTypeId != null && (PROVIDER_ADVERTISEMENT_END == valueOf(segmentTypeId) || DISTRIBUTOR_ADVERTISEMENT_END == valueOf(segmentTypeId)
				|| PLACEMENT_OPPORTUNITY_END == valueOf(segmentTypeId));
	}

	public static boolean isPOAbortRequest(SCTE35PointDescriptorType scte35Pt, AcquisitionPoint ap, String signalId) {
		Logger logger = LogManager.getLogger(SegmentType.class.getName());

		if (scte35Pt == null || ap == null) {
			return false;
		}

		if (scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {
			SpliceInsertType spliceInsert = scte35Pt.getSpliceInsert();
			if (spliceInsert == null) {
				return false;
			}

			if (Boolean.valueOf(String.valueOf(spliceInsert.isSpliceEventCancelIndicator()))) {
				if (logger.isDebugEnabled()) {
					logger.debug(()->"Received SPLICE_INSERT signal abort request on Acqusition Point " + ap.getAcquisitionPointIdentity() + " on SingleId " + signalId);
				}
				return true;
			}

			if (spliceInsert.isOutOfNetworkIndicator() != null && !spliceInsert.isOutOfNetworkIndicator()) {
				if (logger.isDebugEnabled()) {
					logger.debug(()->"Received out_of_network_indicator is false  Acqusition Point " + ap.getAcquisitionPointIdentity());
				}

				if (ap.isSignalAbortEnabled()) {
					if (logger.isDebugEnabled()) {
						logger.debug(()->"Signal Abort is enabled for Acquisition Point " + ap.getAcquisitionPointIdentity());
						logger.debug(()->"Received SPLICE_INSERT signal abort request on Acqusition Point " + ap.getAcquisitionPointIdentity() + " on SingleId " + signalId);
					}
					return true;
				}
			} // IF isSpliceEventCancelIndicator = false , noSchedule = true and only one segmentation descriptor is present then we will check isSegmentationEventCancelIndicator flag PH-198
			if (!(Boolean.valueOf(String.valueOf(spliceInsert.isSpliceEventCancelIndicator())))
					&& (Boolean.valueOf(String.valueOf(CppConstants.NO_SCHEDULES.equalsIgnoreCase(ap.getBaSchedulesInterfaceTypeExternalRef()))))) {
				if (scte35Pt.getSegmentationDescriptorInfo() != null && scte35Pt.getSegmentationDescriptorInfo().size() == 1) {
					Boolean segmentEventCancelIndicator = scte35Pt.getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator();
					if (segmentEventCancelIndicator != null && segmentEventCancelIndicator) {
						if (logger.isDebugEnabled()) {

							logger.debug(()->"Received SPLICE_INSERT signal abort request on Acqusition Point " + ap.getAcquisitionPointIdentity() + " on SingleId " + signalId);
						}
						return true;
					}
				} else if (scte35Pt.getSegmentationDescriptorInfo() != null && scte35Pt.getSegmentationDescriptorInfo().size() > 1) { // For multiple segmentDescriptors we will check each descriptor if all are having segmentationEventCancelIndicator = true we will abort the signal.
					List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35Pt.getSegmentationDescriptorInfo();
					boolean isEventCancel = true;
					for (SegmentationDescriptorType segmentationDescriptorType : segmentationDescriptorInfo) {
						if (!Boolean.valueOf(String.valueOf(segmentationDescriptorType.isSegmentationEventCancelIndicator()))) {
							isEventCancel = false;
							break;
						}
					}
					return isEventCancel;
				}
			}
		}
		if (SpliceCommandType.TIME_SIGNAL.getCommandtype() == scte35Pt.getSpliceCommandType()) {
			if (scte35Pt.getSegmentationDescriptorInfo() != null && scte35Pt.getSegmentationDescriptorInfo().get(0) != null
					&& Boolean.valueOf(String.valueOf(scte35Pt.getSegmentationDescriptorInfo().get(0).isSegmentationEventCancelIndicator()))) {
				if (logger.isDebugEnabled()) {
					logger.debug(()->"Received TIME_SIGNAL signal abort request on Acqusition Point " + ap.getAcquisitionPointIdentity() + " on SingleId " + signalId);
				}
				return true;
			}

		}
		return false;
	}
}
