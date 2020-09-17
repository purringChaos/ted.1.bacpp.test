package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.qam;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.SCTE224NotificationMessageBuilderImpl;

public class NotificationMessageBuilderImpl extends SCTE224NotificationMessageBuilderImpl {
	private static NotificationMessageBuilderImpl instance = new NotificationMessageBuilderImpl();

	public static NotificationMessageBuilderImpl getInstance() {
		return instance;
	}

	@Override
	protected String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage) {
		// TODO Auto-generated method stub
		return null;
	}

}
