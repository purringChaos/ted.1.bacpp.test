package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.encoderlevel.cadentschema;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.i03.signaling.AlternateContentType;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.SCTE224NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.SegmentType;

public class NotificationMessageBuilderImpl extends SCTE224NotificationMessageBuilderImpl {
	private static final Logger log = LogManager.getLogger(NotificationMessageBuilderImpl.class);
	private static NotificationMessageBuilderImpl instance = new NotificationMessageBuilderImpl();
	private static final ObjectFactory I03_SIGNALING_OBJECT_FACTORY = new ObjectFactory();

	public static NotificationMessageBuilderImpl getInstance() {
		return instance;
	}

	@Override
	protected String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		String message = null;
		SignalProcessingNotificationType spNotification = null;
		long eventStartTimeInMsWithDelta = notificationMessage.getEventSignalUTCStartTime();
		long eventStopTimeInMsWithDelta = notificationMessage.getEventSignalUTCStopTime();
		switch (notificationMessage.getEventAction()) {
		case CONFIRMED:
			spNotification = getEncoderLevelStartEventIPNotification(notificationMessage.getStreamId(), notificationMessage.getZoneIdentity(),
					notificationMessage.getEventSignalId(),
					eventStartTimeInMsWithDelta, eventStopTimeInMsWithDelta,
					notificationMessage.getEventAltSourceValue(), notificationMessage.getAcquisitionSignalIds());
			message = Schema.i03.getResponseHandler().generateSCCResponse(spNotification);
			break;
		case STOP_NOW:
		case COMPLETE:
			spNotification = getEncoderLevelEndEventIPNotification(notificationMessage.getStreamId(), notificationMessage.getZoneIdentity(), notificationMessage.getEventSignalId(),
					eventStartTimeInMsWithDelta, eventStopTimeInMsWithDelta,
					notificationMessage.getAcquisitionSignalIds());
			message = Schema.i03.getResponseHandler().generateSCCResponse(spNotification);
			break;
		default:
			break;

		}
		// TODO Auto-generated method stub
		return message;
	}

	private SignalProcessingNotificationType getEncoderLevelStartEventIPNotification(String acquisitionPointId, String zoneIdentity, String signalID,
			long programStartUTCTimeWithAddedDelta, long programStopUTCTimeWithAddedDelta, String eventAltSourceValue, Map<SegmentType, String> acquisitionSignalIds) {

		final SignalProcessingNotificationType encoderLevelProgramStartNotification = I03_SIGNALING_OBJECT_FACTORY.createSignalProcessingNotificationType();
		encoderLevelProgramStartNotification.setAcquisitionPointIdentity(acquisitionPointId);

		// 1. Create program start component of the notification.
		final ResponseSignalType respSignalProgramStart = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		encoderLevelProgramStartNotification.getResponseSignal().add(respSignalProgramStart);
		respSignalProgramStart.setAcquisitionPointIdentity(acquisitionPointId);
		respSignalProgramStart.setAcquisitionSignalID(acquisitionSignalIds.get(SegmentType.PROGRAM_START));
		respSignalProgramStart.setAction(ResponseSignalAction.CREATE.toString());
		respSignalProgramStart.setSignalPointID(signalID);

		//2. Calculate the Start UTC time for the Program Start Signal.
		UTCPointDescriptorType programStartUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		programStartUTCPoint.setUtcPoint(SCCResponseUtil.generateUTC(programStartUTCTimeWithAddedDelta));
		respSignalProgramStart.setUTCPoint(programStartUTCPoint);

		//3. Add the zone and alt source values.
		AlternateContentType alternateContenttype = I03_SIGNALING_OBJECT_FACTORY.createAlternateContentType();
		alternateContenttype.setAltContentIdentity(eventAltSourceValue);
		alternateContenttype.setZoneIdentity(zoneIdentity);
		respSignalProgramStart.getAlternateContent().add(alternateContenttype);

		// 4. Create program end component of the notification.
		final ResponseSignalType respSignalProgramEnd = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		encoderLevelProgramStartNotification.getResponseSignal().add(respSignalProgramEnd);
		respSignalProgramEnd.setAcquisitionPointIdentity(acquisitionPointId);
		respSignalProgramEnd.setAcquisitionSignalID(acquisitionSignalIds.get(SegmentType.PROGRAM_END));
		respSignalProgramEnd.setAction(ResponseSignalAction.CREATE.toString());
		respSignalProgramEnd.setSignalPointID(signalID);

		//5. Calculate the Stop UTC time for the Program End Signal.

		UTCPointDescriptorType programEndUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		programEndUTCPoint.setUtcPoint(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));
		respSignalProgramEnd.setUTCPoint(programEndUTCPoint);

		//6. Add the zone and alt source values.
		AlternateContentType alternateContenttypeEnd = I03_SIGNALING_OBJECT_FACTORY.createAlternateContentType();
		alternateContenttypeEnd.setAltContentIdentity(StringUtils.EMPTY);
		alternateContenttypeEnd.setZoneIdentity(zoneIdentity);
		respSignalProgramEnd.getAlternateContent().add(alternateContenttypeEnd);

		return encoderLevelProgramStartNotification;
	}

	private static SignalProcessingNotificationType getEncoderLevelEndEventIPNotification(String acquisitionPointId, String zoneIdentity, String signalID,
			long programStartUTCTimeWithAddedDelta, long programStopUTCTimeWithAddedDelta, final Map<SegmentType, String> acquisitionSignalIds) {

		final String acquisitionStopSignalId = acquisitionSignalIds.get(SegmentType.PROGRAM_END);

		final SignalProcessingNotificationType notification = I03_SIGNALING_OBJECT_FACTORY.createSignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(acquisitionPointId);

		final ResponseSignalType respSignalProgramEnd = I03_SIGNALING_OBJECT_FACTORY.createResponseSignalType();
		respSignalProgramEnd.setAcquisitionPointIdentity(acquisitionPointId);
		respSignalProgramEnd.setAcquisitionSignalID(acquisitionStopSignalId);
		respSignalProgramEnd.setAction(ResponseSignalAction.REPLACE.toString());
		respSignalProgramEnd.setSignalPointID(signalID);

		UTCPointDescriptorType programEndUTCPoint = I03_SIGNALING_OBJECT_FACTORY.createUTCPointDescriptorType();
		programEndUTCPoint.setUtcPoint(SCCResponseUtil.generateUTC(programStopUTCTimeWithAddedDelta));
		respSignalProgramEnd.setUTCPoint(programEndUTCPoint);

		AlternateContentType alternateContenttypeEnd = I03_SIGNALING_OBJECT_FACTORY.createAlternateContentType();
		alternateContenttypeEnd.setAltContentIdentity(StringUtils.EMPTY);
		alternateContenttypeEnd.setZoneIdentity(zoneIdentity);
		respSignalProgramEnd.getAlternateContent().add(alternateContenttypeEnd);

		notification.getResponseSignal().add(respSignalProgramEnd);

		return notification;
	}

}
