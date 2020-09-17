package tv.blackarrow.cpp.notifications.upstream.executor;

import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.upstream.executor.sender.i02.ip.I02IPNotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.executor.sender.i02.qam.I02QamNotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.executor.sender.scte224.SCTE224NotificationSenderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;

public class NotificationSenderFactory {
	private static NotificationSenderFactory instance = new NotificationSenderFactory();

	private NotificationSenderFactory() {

	}

	public static NotificationSenderFactory getInstance() {
		return instance;
	}

	public NotificationSender getSender(EventType type, UpStreamNotificationMessageType upStreamNotificationMessageType) {
		NotificationSender sender = null;
		switch (type) {
		case I02:
			switch (upStreamNotificationMessageType) {
			case I02_IP_Inband:
			case I02_IP_OOB:
			case I02_OpendEnded_IP_Inband:
			case I02_OpendEnded_QAM_Inband:
				sender = new I02IPNotificationSenderImpl();
				break;
			case I02_QAM_Inband:
			case I02_QAM_OOB:
				sender = new I02QamNotificationSenderImpl();
				break;
			default:
				break;
			}
			break;
		case SCTE224:
			sender = new SCTE224NotificationSenderImpl();//So far only IP are supported for 224
			break;
		default:
			break;

		}

		return sender;
	}
}
