package test.tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel.NotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class NotificationMessageBuilderImplTest {

	NotificationMessage notificationMessage = new NotificationMessage();

	
	
	tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel.NotificationMessageBuilderImpl builder = 
			new NotificationMessageBuilderImpl();
	@Test
	public void testEventStartMessage() {

		buildNotificationMessage(notificationMessage);
		notificationMessage.setEventAction(EventAction.CONFIRMED);

		String message = builder.getNotificationMessage(notificationMessage);
		System.out.println(message);
	}
	void buildNotificationMessage(NotificationMessage message) {
		message.setSchema(Schema.i03);
		HashMap<SegmentType, String> aq = new HashMap<>();
		aq.put(SegmentType.PROGRAM_START, "4Rkw8P06QpW5O2CIkiZt4g");
		aq.put(SegmentType.CONTENT_IDENTIFICATION, "4Rkw8P06QpW5O2CIkiZt4g");
		message.setAcquisitionSignalIds(aq);
		message.setEventSignalId("bea6D6uWR-WEalzumVikcg");
		message.setEventAltSourceValue("www.google.com");
		message.setEventSignalUTCStartTime(1560760456000l);
		message.setEventSignalUTCStopTime(1560765456000l);	
		message.setCurrentTime(System.currentTimeMillis());
		message.setContentDuration(1000);
		message.setAqContentFrequency(10);
		message.setEventType(EventType.SCTE224);
		message.setStreamId("AQ_IP_Inband_103_Regular");
		message.setEventId("101");
		message.setNotificationScheduledTime(1561969953);
		message.setTransactionSegmentType(Arrays.asList(SegmentType.PROGRAM_START));
	}
}
