package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.BaseNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.UpStreamNotificationMessageByESAMFactory;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI03NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class InbandAQNotificationMessageBuilderImpl extends BaseNotificationMessageBuilderImpl {
	private static InbandAQNotificationMessageBuilderImpl instance = new InbandAQNotificationMessageBuilderImpl();
	private static final Logger LOG = LogManager.getLogger(InbandAQNotificationMessageBuilderImpl.class);

	public static InbandAQNotificationMessageBuilderImpl getInstance() {
		return instance;
	}

	@Override
	public String getNotificationMessage(NotificationMessage notificationMessage) {
		return getI03SchemaUpstreamNotificatonMessage(notificationMessage);
	}

	/**
	 * 
	 * @param notificationMessage
	 * @return
	 */
	private String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		notificationMessage.setAcquisitionSignalIds(getAcquisitionSingalIds());
		switch (notificationMessage.getEventAction()) {
		case CONFIRMED:
			return createI03SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_START, ResponseSignalAction.CREATE.toString(),
					notificationMessage.getEventSignalUTCStartTime(), notificationMessage.getEventSignalUTCStopTime() - notificationMessage.getEventSignalUTCStartTime());
		case UPDATE:
		case STOP_NOW:
		case COMPLETE:
			return createI03SchemaUpstreamNotificatonMessage(notificationMessage, SegmentType.PROGRAM_END, ResponseSignalAction.CREATE.toString(),
					notificationMessage.getEventSignalUTCStopTime(), 0l);
		default:
			break;
		}
		return null;
	}

	/**
	 * 
	 * @param notificationMessage
	 * @param segmentType
	 * @param action
	 * @param blackoutEventTime
	 * @param contentDuration
	 * @return
	 */
	private String createI03SchemaUpstreamNotificatonMessage(final NotificationMessage notificationMessage, final SegmentType segmentType, String action, long blackoutEventTime,
			long contentDuration) {

		String message = null;
		tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType signalProcessingNotificationType = new tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType();
		try {

			EsamI03NotificationMessageBuilderImpl esamI03Builder = (EsamI03NotificationMessageBuilderImpl) UpStreamNotificationMessageByESAMFactory
					.getUpstreamNoitificationMessageByESAM(tv.blackarrow.cpp.handler.Schema.i03);

			XMLGregorianCalendar blackoutEventStartTimeWithOffset = getXMLGregorianCalenderTime(
					esamI03Builder.getTimeInMillisWithAQOffset(blackoutEventTime, notificationMessage.getStreamSignalTimeOffset()));

			esamI03Builder.setProgramStartResponseSignal(notificationMessage, signalProcessingNotificationType, blackoutEventStartTimeWithOffset, action, segmentType,
					contentDuration);
			esamI03Builder.setConditioningInfo(notificationMessage, signalProcessingNotificationType, contentDuration);
			message = esamI03Builder.marshalMessage(signalProcessingNotificationType);

		} catch (Exception e) {
			LOG.error(() -> "The notification message could not be sent to " + notificationMessage.getStreamId() + " and the type of this message was "
					+ notificationMessage.getUpStreamNotificationMessageType() + " due to " + e.getMessage());
		}
		return message;
	}

}
