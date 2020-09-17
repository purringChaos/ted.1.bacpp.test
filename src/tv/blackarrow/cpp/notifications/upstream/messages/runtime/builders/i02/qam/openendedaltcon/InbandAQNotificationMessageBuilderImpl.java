package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.openendedaltcon;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.BaseNotificationMessageBuilderImpl;

public class InbandAQNotificationMessageBuilderImpl extends BaseNotificationMessageBuilderImpl {
	private static InbandAQNotificationMessageBuilderImpl instance = new InbandAQNotificationMessageBuilderImpl();
	
	public static InbandAQNotificationMessageBuilderImpl getInstance() {
		return instance;
	}
	
	@Override
	public String getNotificationMessage(NotificationMessage notificationMessage) {
		throw new UnsupportedOperationException("For Open ended Feed on QAM Streams no out of band notifications are supported. For more details refer to (PRODISSUE-1120).");
	}
	
}
