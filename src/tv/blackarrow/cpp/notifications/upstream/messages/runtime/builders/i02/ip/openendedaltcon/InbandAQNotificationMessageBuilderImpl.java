package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.openendedaltcon;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.BaseNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.UpStreamNotificationMessageByESAMFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI01NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI03NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class InbandAQNotificationMessageBuilderImpl extends BaseNotificationMessageBuilderImpl {
	private static final int CONDITIONING_INFO_INDEX = 0;
	private static final int CONTENT_IDENTIFICATION_SIGNAL_INDEX = 1;
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
			break;
		case i03:
			message = getI03SchemaUpstreamNotificatonMessage(notificationMessage);
			break;
		default:
			break;

		}
		
		
		return message;
	}

	/**
	 * Creates a notification message for I03 {@link SignalProcessingNotificationType}
	 * 
	 * @param notificationMessage
	 * @return
	 */
	private String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType signalProcessingNotificationType = new tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType();
		if (EventAction.CONFIRMED.equals(notificationMessage.getEventAction())) {
			try {
				long contentDuration = notificationMessage.getContentDuration();

				EsamI03NotificationMessageBuilderImpl esamI03Builder = (EsamI03NotificationMessageBuilderImpl) UpStreamNotificationMessageByESAMFactory
						.getUpstreamNoitificationMessageByESAM(tv.blackarrow.cpp.handler.Schema.i03);

				XMLGregorianCalendar blackoutEventStartTimeWithOffset = getXMLGregorianCalenderTime(
						esamI03Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getStreamSignalTimeOffset()));

				esamI03Builder.setProgramStartResponseSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						ResponseSignalAction.CREATE.toString(), SegmentType.PROGRAM_START, contentDuration);

				XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset = getXMLGregorianCalenderTime(
						esamI03Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStopTime(), notificationMessage.getStreamSignalTimeOffset()));

				esamI03Builder.setContentIndentficationRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						blackoutEventStopTimeAsXMLGCWithOffset, ResponseSignalAction.CREATE.toString(), SegmentType.PROGRAM_START, contentDuration);
				signalProcessingNotificationType.getResponseSignal().get(CONTENT_IDENTIFICATION_SIGNAL_INDEX).getEventSchedule().setStopUTC(null);

				esamI03Builder.setConditioningInfo(notificationMessage, signalProcessingNotificationType, contentDuration);
				signalProcessingNotificationType.getConditioningInfo().get(CONDITIONING_INFO_INDEX).setDuration(null);

				return esamI03Builder.marshalMessage(signalProcessingNotificationType);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOG.error(() -> "The notification message could not be sent to " + notificationMessage.getStreamId() + " and the type of this message was "
						+ notificationMessage.getUpStreamNotificationMessageType() + " due to " + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Creates a notification message for I01 {@link SignalProcessingNotificationType}
	 * 
	 * @param notificationMessage
	 * @return
	 */
	private String getI01SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {

		tv.blackarrow.cpp.signaling.SignalProcessingNotificationType signalProcessingNotificationType = new tv.blackarrow.cpp.signaling.SignalProcessingNotificationType();
		if (EventAction.CONFIRMED.equals(notificationMessage.getEventAction())) {

			try {
				long contentDuretion = notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime();

				EsamI01NotificationMessageBuilderImpl esamI01Builder = (EsamI01NotificationMessageBuilderImpl) UpStreamNotificationMessageByESAMFactory
						.getUpstreamNoitificationMessageByESAM(tv.blackarrow.cpp.handler.Schema.i01);

				XMLGregorianCalendar blackoutEventStartTimeWithOffset = getXMLGregorianCalenderTime(
						esamI01Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getStreamSignalTimeOffset()));

				esamI01Builder.setProgramStartResponseSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						ResponseSignalAction.CREATE.toString(), SegmentType.PROGRAM_START, contentDuretion);

				XMLGregorianCalendar blackoutEventStopTimeAsXMLGCWithOffset = getXMLGregorianCalenderTime(
						esamI01Builder.getTimeInMillisWithAQOffset(notificationMessage.getEventSignalUTCStopTime(), notificationMessage.getStreamSignalTimeOffset()));

				esamI01Builder.setContentIndentficationRespSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset,
						blackoutEventStopTimeAsXMLGCWithOffset, ResponseSignalAction.CREATE.toString(), SegmentType.CONTENT_IDENTIFICATION, contentDuretion);

				signalProcessingNotificationType.getResponseSignal().get(1).getEventSchedule().setStopUTC(null);

				esamI01Builder.setConditioningInfo(notificationMessage, signalProcessingNotificationType, contentDuretion);

				signalProcessingNotificationType.getConditioningInfo().get(0).setDuration(null);

				return esamI01Builder.marshalMessage(signalProcessingNotificationType);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOG.error(() -> "The notification message could not be sent to " + notificationMessage.getStreamId() + " and the type of this message was "
						+ notificationMessage.getUpStreamNotificationMessageType() + " due to " + e.getMessage());
			}
		}
		return null;

	}
}
