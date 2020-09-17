package tv.blackarrow.cpp.notifications.upstream.messages.runtime;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

public class UpStreamNotificationMessageFactory {
	public static String getUpstreamNoitificationMessage(final NotificationMessage notificationMessage) {
		String message = null;
		if (notificationMessage != null) {
			UpStreamNotificationMessageType type = notificationMessage.getUpStreamNotificationMessageType() != null ? notificationMessage.getUpStreamNotificationMessageType()
					: UpStreamNotificationMessageType.OLDER_MESSAGE_FROM_9_1_RELEASE;
			switch (type) {
			case I02_IP_Inband:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.regularaltcon.InbandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			case I02_IP_OOB:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.regularaltcon.OutOfBandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			////////////////////////////////////////
			case I02_QAM_Inband:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.InbandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			case I02_QAM_OOB:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.OutOfBandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			////////////////////////////////////////
			case I02_OpendEnded_IP_Inband:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.openendedaltcon.InbandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			case I02_OpendEnded_QAM_Inband:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.openendedaltcon.InbandAQNotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			////////////////////////////////////////
			case SCTE224_IP_Manifest_Level:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel.NotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			case SCTE224_IP_EncoderLevel:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.encoderlevel.cadentschema.NotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			case SCTE224_IP_ENCODER_LEVEL_INBAND:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.encoderlevel.standard.NotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			////////////////////////////////////////	
			case SCTE224_QAM:
				message = tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.qam.NotificationMessageBuilderImpl.getInstance()
						.getNotificationMessage(notificationMessage);
				break;
			default://9.1 backward compatibility. if we do not receive the upStreamNotificationMessageType (considering null from ned.1), then message should have been prepopulated.
				message = notificationMessage.getScheduledUpStreamMessage();
				break;
			}
		}
		return message;
	}
}
