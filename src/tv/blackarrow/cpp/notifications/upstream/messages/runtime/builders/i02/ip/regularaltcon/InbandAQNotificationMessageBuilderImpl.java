package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.regularaltcon;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.BaseNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.UpStreamNotificationMessageByESAMFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI01NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI03NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamNotificationMessageBuilder;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class InbandAQNotificationMessageBuilderImpl extends BaseNotificationMessageBuilderImpl implements EsamNotificationMessageBuilder {

	private static InbandAQNotificationMessageBuilderImpl instance = new InbandAQNotificationMessageBuilderImpl();

	private static final Logger LOG = LogManager.getLogger(InbandAQNotificationMessageBuilderImpl.class);

	public static InbandAQNotificationMessageBuilderImpl getInstance() {
		return instance;
	}

	@Override
	public String getNotificationMessage(NotificationMessage notificationMessage) {
		String message = "";
		notificationMessage.setAcquisitionSignalIds(getAcquisitionSingalIds());
		switch (notificationMessage.getSchema()) {
		case i01:
			message = getI01SchemaUpstreamNotificatonMessage(notificationMessage);
		case i03:
			message = getI03SchemaUpstreamNotificatonMessage(notificationMessage);
		default:
			break;

		}
		
		
		return message;
	}

	/**
	 * 
	 * @param notificationMessage
	 * @return
	 */
	private String getI01SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage ) {
		switch (notificationMessage.getEventAction()) {
		case CONFIRMED:
			return createI01SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.CREATE.toString(), false, false);
		case UPDATE:
			return createI01SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.REPLACE.toString(), true, true);
		case STOP_NOW:	
		case COMPLETE:
			return createI01SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.REPLACE.toString(), false, true);
		default:
			break;
		}
		return null;
	}

	/**
	 * Creates a notification message for I01 {@link SignalProcessingNotificationType}
	 * @param notificationMessage
	 * @param segmentType
	 * @param action
	 * @param isProgramRunoverSignal
	 * @return
	 */
	private String createI01SchemaUpstreamNotificatonMessage(final NotificationMessage notificationMessage, final SegmentType segmentType, final String action,
			boolean isProgramRunoverSignal, boolean isProgramEnd) {

		tv.blackarrow.cpp.signaling.SignalProcessingNotificationType signalProcessingNotificationType = new tv.blackarrow.cpp.signaling.SignalProcessingNotificationType();
		try {

			long contentDuration = notificationMessage.getContentDuration();

			EsamI01NotificationMessageBuilderImpl esamI01Builder = (EsamI01NotificationMessageBuilderImpl) UpStreamNotificationMessageByESAMFactory
					.getUpstreamNoitificationMessageByESAM(tv.blackarrow.cpp.handler.Schema.i01);

			XMLGregorianCalendar blackoutEventStartTimeWithOffset = getXMLGregorianCalenderTime(
					esamI01Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getStreamSignalTimeOffset()));

			if (!isProgramEnd) {//Not adding the program start & conditioning info to program end response 
				esamI01Builder.setProgramStartResponseSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset, action, segmentType,
						contentDuration);
				esamI01Builder.setConditioningInfo(notificationMessage, signalProcessingNotificationType, contentDuration);
			}

			XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset = getXMLGregorianCalenderTime(
					esamI01Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStopTime(), notificationMessage.getStreamSignalTimeOffset()));

			esamI01Builder.setContentIndentficationRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
					blackoutEventStopTimeAsXMLGCWithOffset, action, SegmentType.CONTENT_IDENTIFICATION, contentDuration);

			if (isProgramRunoverSignal) {
				//long notifiactionTimeInSecs = notificationMessage.getNotificationScheduledTime();
				//XMLGregorianCalendar notificationScheduledTime = getXMLGregorianCalenderTime(notifiactionTimeInSecs * 1000);
				esamI01Builder.setProgramRunoverUnplannedRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						blackoutEventStopTimeAsXMLGCWithOffset, blackoutEventStartTimeWithOffset, ResponseSignalAction.CREATE.toString(), SegmentType.PROGRAM_RUNOVER_UNPLANNED);
			}
			return esamI01Builder.marshalMessage(signalProcessingNotificationType);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error(() -> "The notification message could not be sent to " + notificationMessage.getStreamId() + " and the type of this message was "
					+ notificationMessage.getUpStreamNotificationMessageType() + " due to " + e.getMessage());
		}

		return null;
	}

	/**
	 * 
	 * @param notificationMessage
	 * @return
	 */
	private String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		switch (notificationMessage.getEventAction()) {
		case CONFIRMED:
			return createI03SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.CREATE.toString(), false, false);
		case UPDATE:
			return createI03SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.REPLACE.toString(), true, true);
		case STOP_NOW:
		case COMPLETE:
			return createI03SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.REPLACE.toString(), false, true);
		default:
			break;
		}
		return null;
	}

	/**
	 * Creates a notification message for I01 {@link SignalProcessingNotificationType}
	 * @param notificationMessage
	 * @param segmentType
	 * @param action
	 * @param isProgramRunoverSignal
	 * @return
	 */
	private String createI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage, SegmentType segmentType, String action, boolean isProgramRunoverSignal,
			boolean isProgramEnded) {
		tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType signalProcessingNotificationType = new tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType();
		try {
			long contentDuration = notificationMessage.getContentDuration();

			EsamI03NotificationMessageBuilderImpl esamI03Builder = (EsamI03NotificationMessageBuilderImpl) UpStreamNotificationMessageByESAMFactory
					.getUpstreamNoitificationMessageByESAM(tv.blackarrow.cpp.handler.Schema.i03);

			XMLGregorianCalendar blackoutEventStartTimeWithOffset = getXMLGregorianCalenderTime(
					esamI03Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getStreamSignalTimeOffset()));

			if (!isProgramEnded) {//Not adding the program start & conditioning info to program end response 
				esamI03Builder.setProgramStartResponseSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset, action, segmentType,
						contentDuration);
				esamI03Builder.setConditioningInfo(notificationMessage, signalProcessingNotificationType, contentDuration);
			}
			XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset = getXMLGregorianCalenderTime(
					esamI03Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStopTime(), notificationMessage.getStreamSignalTimeOffset()));

			esamI03Builder.setContentIndentficationRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
					blackoutEventStopTimeAsXMLGCWithOffset, action, SegmentType.CONTENT_IDENTIFICATION, contentDuration);

			if (isProgramRunoverSignal) {
				//long notifiactionTimeInSecs = notificationMessage.getNotificationScheduledTime();
				//XMLGregorianCalendar notificationScheduledTime = getXMLGregorianCalenderTime(notifiactionTimeInSecs * 1000);
				esamI03Builder.setProgramRunoverUnplannedRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						blackoutEventStopTimeAsXMLGCWithOffset, blackoutEventStartTimeWithOffset, ResponseSignalAction.CREATE.toString(), SegmentType.PROGRAM_RUNOVER_UNPLANNED);
			}
			return esamI03Builder.marshalMessage(signalProcessingNotificationType);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error(() -> "The notification message could not be sent to " + notificationMessage.getStreamId() + " and the type of this message was "
					+ notificationMessage.getUpStreamNotificationMessageType() + " due to " + e.getMessage());
		}

		return null;
	}

}
