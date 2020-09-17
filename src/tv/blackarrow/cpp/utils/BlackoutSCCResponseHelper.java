package tv.blackarrow.cpp.utils;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;


public class BlackoutSCCResponseHelper {

	public static long getBlackoutStartTimeWithoutOffset(ConfirmedPlacementOpportunity cpo, AcquisitionPoint aqpt, ResponseSignalType respSignalType, Short requestedSegmentTypeId) {
		long originalRequestSignalTime = respSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		long blackoutStartTime = 0l;
		if (cpo.isAborted() || SegmentType.isBlackoutRunoverSignal(requestedSegmentTypeId) || SegmentType.isProgramEndSignal(requestedSegmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId)) {// Abort has special requirements.
			blackoutStartTime = cpo.getActualUtcStartTime() > 0 ? cpo.getActualUtcStartTime() : cpo.getUtcSignalTime();
		} else if(SegmentType.isProgramStartSignal(requestedSegmentTypeId) && cpo!= null && cpo.getActualUtcStartTime() > 0){
			//Offset if required gets added outside.
			blackoutStartTime = cpo.getActualUtcStartTime();
		} else {
			// This already have the offset added into it.
			blackoutStartTime = originalRequestSignalTime;
		}
		return blackoutStartTime;
	}
	
	public static long getBlackoutStartTimeWithOffset(ConfirmedPlacementOpportunity cpo, AcquisitionPoint aqpt, ResponseSignalType respSignalType, Short requestedSegmentTypeId) {
		long originalRequestSignalTime = respSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		long blackoutStartTime = 0l;
		if (cpo.isAborted() || SegmentType.isBlackoutRunoverSignal(requestedSegmentTypeId) || SegmentType.isProgramEndSignal(requestedSegmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId)) {// Abort has special requirements.
			blackoutStartTime = cpo.getUtcSignalTime();
		} else if(SegmentType.isProgramStartSignal(requestedSegmentTypeId) && cpo!= null && cpo.getActualUtcStartTime() > 0){
			//Offset if required gets added outside.
			blackoutStartTime = cpo.getActualUtcStartTime();
		} else {
			// This already have the offset added into it.
			blackoutStartTime = originalRequestSignalTime;
		}
		blackoutStartTime += aqpt.getSignalTimeOffset();
		return blackoutStartTime;
	}

	public static long getBlackoutEndTimeWithOffset(ConfirmedPlacementOpportunity cpo, AcquisitionPoint aqpt, BlackoutEvent blackoutEvent, ResponseSignalType respSignalType) {
		long originalRequestSignalTime = respSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		long blackoutStopTime = 0l;
		// Abort has special requirements.
		if (cpo.isAborted()) {
			blackoutStopTime = originalRequestSignalTime;
		} else {
			blackoutStopTime = BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent);
			blackoutStopTime += aqpt.getSignalTimeOffset();
		}
		return blackoutStopTime;
	}
	
	public static long getBlackoutStartTimeWithoutOffset(ConfirmedPlacementOpportunity cpo, AcquisitionPoint aqpt, SignalProcessingEventType.AcquiredSignal respSignalType, Short requestedSegmentTypeId) {
		long originalRequestSignalTime = respSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
		long blackoutStartTime = 0l;
		if (cpo.isAborted() || SegmentType.isBlackoutRunoverSignal(requestedSegmentTypeId) || SegmentType.isProgramEndSignal(requestedSegmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId)) {// Abort has special requirements.
		blackoutStartTime = cpo.getActualUtcStartTime() > 0 ? cpo.getActualUtcStartTime() : cpo.getUtcSignalTime();
		} else if(SegmentType.isProgramStartSignal(requestedSegmentTypeId) && cpo!= null && cpo.getActualUtcStartTime() > 0){
		//Offset if required gets added outside.
		blackoutStartTime = cpo.getActualUtcStartTime();
		} else {
		// This already have the offset added into it.
		blackoutStartTime = originalRequestSignalTime;
		}
		return blackoutStartTime;
		}

}
