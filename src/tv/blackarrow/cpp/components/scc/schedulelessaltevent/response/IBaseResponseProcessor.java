package tv.blackarrow.cpp.components.scc.schedulelessaltevent.response;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.SegmentType;

public interface IBaseResponseProcessor {
	public void generateResponse(SignalProcessingNotificationType notificationResponse, ResponseSignalType responseSignal, AcquisitionPoint aqpt,
			SegmentType requestSegmentTypeIdEnum, String acquisitionSignalId, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments, MuleEventContext context,
			SchedulelessAltEventLedger ledger, Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext, Date requestTime) throws DatatypeConfigurationException;

	public void adjustPtsTime(SignalProcessingNotificationType notification);

	public Long getDurationFromLedger(SchedulelessAltEventLedger schedulelessAltEventLedger);

	public long getStartTimeFromLedger(SchedulelessAltEventLedger schedulelessAltEventLedger, ResponseSignalType responseSignalType);

	public Long getStartTimeFromLedgerForEventSchedule(AcquisitionPoint aqpt, SchedulelessAltEventLedger schedulelessAltEventLedger);

	public Long getStopTimeFromLedgerForEventSchedule(ResponseSignalType responseSignalType, Long duration);

	public void processSignal(MuleEventContext context, AcquiredSignal acquiredSignal, AcquisitionPoint aqpt, SegmentationDescriptorType descriptorInfo, 
			SchedulelessAltEventLedger ledger, Date requestTime);

}
