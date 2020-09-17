package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI01NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamI03NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam.EsamNotificationMessageBuilder;

public class UpStreamNotificationMessageByESAMFactory {
	public static EsamNotificationMessageBuilder getUpstreamNoitificationMessageByESAM(final Schema schema) {
		EsamNotificationMessageBuilder esamImplementer = null;
		if (schema != null) {
			switch (schema) {
			case i01:
				esamImplementer = new EsamI01NotificationMessageBuilderImpl();
				break;
			case i03:
				esamImplementer = new EsamI03NotificationMessageBuilderImpl();
				break;
			default:
				break;
			}
		}
		return esamImplementer;
	}

}
