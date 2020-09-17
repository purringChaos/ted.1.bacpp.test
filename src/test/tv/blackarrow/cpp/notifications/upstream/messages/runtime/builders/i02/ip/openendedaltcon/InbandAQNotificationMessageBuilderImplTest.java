package test.tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.openendedaltcon;

import java.util.HashMap;

import org.junit.Test;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.regularaltcon.InbandAQNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.regularaltcon.OutOfBandAQNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;

public class InbandAQNotificationMessageBuilderImplTest {

	InbandAQNotificationMessageBuilderImpl ip = new InbandAQNotificationMessageBuilderImpl();
	
	tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.InbandAQNotificationMessageBuilderImpl qam = 
			new tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.InbandAQNotificationMessageBuilderImpl();
	
	OutOfBandAQNotificationMessageBuilderImpl ipOut = new OutOfBandAQNotificationMessageBuilderImpl();
	
	tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.OutOfBandAQNotificationMessageBuilderImpl qamOut = 
			new tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.qam.regularaltcon.OutOfBandAQNotificationMessageBuilderImpl();
	NotificationMessage notificationMessage = new NotificationMessage();

	
	
	
	
	
	tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.openendedaltcon.InbandAQNotificationMessageBuilderImpl openEnded =
			new tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.i02.ip.openendedaltcon.InbandAQNotificationMessageBuilderImpl();
/*	@Test
	public void testss() {

		buildNotificationMessage(notificationMessage);
		String message = qam.getNotificationMessage(notificationMessage);
		System.out.println(message);
	}
	@Test
	public void testss2() {

		buildNotificationMessage(notificationMessage);
		String message = qamOut.getNotificationMessage(notificationMessage);
		System.out.println(message);
	}*/
	@Test
	public void testOpenEnded() {

		buildNotificationMessage(notificationMessage);
		String message = ip.getNotificationMessage(notificationMessage);
		System.out.println(message);
	}
	void buildNotificationMessage(NotificationMessage message) {
		message.setSchema(Schema.i03);
		message.setEventAction(EventAction.UPDATE);
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
		message.setEventType(EventType.I02);
		message.setStreamId("AQ_IP_Inband_103_Regular");
		message.setEventId("101");
		message.setNotificationScheduledTime(1561969953);
	}

}
