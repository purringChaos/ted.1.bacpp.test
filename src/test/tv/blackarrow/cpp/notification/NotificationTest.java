/**
 * 
 */
package test.tv.blackarrow.cpp.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.ESS_PROCESSING_TIME_IN_SECS;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.UPSTREAM_NOTIFICATION_QUEUE_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;
import tv.blackarrow.cpp.notifications.upstream.scheduler.CancelScheduledNotificationByEventSignalIdInfo;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationScheduler;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;
import tv.blackarrow.cpp.notifications.upstream.scheduler.ScheduleNotificationInfo;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.ds.common.util.UUIDUtils;

/**
 * @author Amit Kumar Sharma
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NotificationTest {
	
	private static final NotificationScheduler NOTIFICATION_SCHEDULER_SERVICE = NotificationSchedulerService.getInstance();
	private static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();
	private static final int DELTA_FROM_CURRENT_TIME = 20;// In Seconds
	
	/**
	 * Test method for {@link tv.blackarrow.cpp.notification.scheduler.NotificationSchedulerService#getInstance()}.
	 */
	@Test
	public void testGetInstance() {
		NotificationScheduler notificationSchedulerService = NotificationSchedulerService.getInstance();
		assertNotNull(notificationSchedulerService);
		assertEquals(notificationSchedulerService, NotificationSchedulerService.getInstance());
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.notification.scheduler.NotificationSchedulerService#scheduleNotification(java.util.List)}.
	 */
	@Test
	public void testScheduleNotificationListOfScheduleNotificationInfo() {
		final int currentSystemTimeUptoSeconds = (int)TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		List<ScheduleNotificationInfo> scheduledInfo = new ArrayList<>();
		
		//OneQueue
		final int notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 25;
		final int numberOfMessagesToPutInQueue = 8;
		ScheduleNotificationInfo scheduleInfo = getScheduledInfoForTime(notificationTimeInSeconds, 
				numberOfMessagesToPutInQueue);
		scheduledInfo.add(scheduleInfo);
		
		//SecondQueue
		final int notificationTimeInSecondsSecond = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 30;
		final int numberOfMessagesToPutInQueueSecond = 12;
		ScheduleNotificationInfo scheduleInfoSecond = getScheduledInfoForTime(notificationTimeInSecondsSecond, 
				numberOfMessagesToPutInQueueSecond);
		scheduledInfo.add(scheduleInfoSecond);
		
		NOTIFICATION_SCHEDULER_SERVICE.scheduleNotification(scheduledInfo, "dummyForLog");
		
		assertEquals((int)Math.ceil((double)scheduleInfo.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		assertEquals((int)Math.ceil((double)scheduleInfoSecond.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSecondsSecond)));
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.notification.scheduler.NotificationSchedulerService#scheduleNotification(tv.blackarrow.cpp.notification.scheduler.ScheduleNotificationInfo)}.
	 */
	@Test
	public void testScheduleNotificationScheduleNotificationInfo() {
		final int currentSystemTimeUptoSeconds = (int)TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final int notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 35;
		final int numberOfMessagesToPutInQueue = 10;
		ScheduleNotificationInfo scheduleInfo = getScheduledInfoForTime(notificationTimeInSeconds, numberOfMessagesToPutInQueue);
		NOTIFICATION_SCHEDULER_SERVICE.scheduleNotification(scheduleInfo, "dummyForLog");
		
		assertEquals((int)Math.ceil((double)scheduleInfo.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
	}

	/**
	 * @param notificationTimeInSeconds
	 * @param numberOfMessagesToPutInQueue 
	 * @return
	 */
	private ScheduleNotificationInfo getScheduledInfoForTime(int notificationTimeInSeconds, int numberOfMessagesToPutInQueue) {
		int signalUTCTimeForProgramStart = notificationTimeInSeconds + ESS_PROCESSING_TIME_IN_SECS;
		int signalUTCTimeForProgramEnd = notificationTimeInSeconds + ESS_PROCESSING_TIME_IN_SECS + ESS_PROCESSING_TIME_IN_SECS;
		Map<String, NotificationMessage> notificationMessages = new HashMap<>();
		final Map<SegmentType, String> acquisitionSignalIds = new LinkedHashMap<>();
		acquisitionSignalIds.put(SegmentType.PROGRAM_START, UUIDUtils.getBase64UrlEncodedUUID());
		acquisitionSignalIds.put(SegmentType.PROGRAM_END, UUIDUtils.getBase64UrlEncodedUUID());
		String eventSignalID = UUIDUtils.getBase64UrlEncodedUUID();		
		String eventId = "Test_Event_" + eventSignalID;
		for(int i=0; i < numberOfMessagesToPutInQueue; i++) {
			String APID = "AP" + i;
			String scheduledNotificationId = APID + "@#@" + eventSignalID;
			NotificationMessage notificationMessage = new NotificationMessage(
					scheduledNotificationId, 
					eventId, 
					eventSignalID,
					APID,
					0,
					"http://localhost:6650/transcoder/notify", 
					notificationTimeInSeconds, 
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
					"<ns0:SignalProcessingNotification acquisitionPointIdentity=\"MUSE7_DISC_EAST_OOH\" xmlns:ns0=\"urn:cablelabs:iptvservices:esam:xsd:signal:1\" xmlns:ns1=\"urn:cablelabs:iptvservices:esam:xsd:common:1\" xmlns:ns2=\"urn:cablelabs:md:xsd:signaling:3.0\">\n" + 
					"    <ns0:ResponseSignal acquisitionPointIdentity=\"MUSE7_DISC_EAST_OOH\" acquisitionSignalID=\"B0D44C51-7BAB-4DC4-96CB-C91D616AFC56\" action=\"create\" signalPointID=\"MemVgpCnSVSpjM_JZlJIxg\">\n" + 
					"        <ns2:UTCPoint utcPoint=\"2018-10-11T21:52:30.000Z\"/>\n" + 
					"        <ns0:AlternateContent zoneIdentity=\"OOH\" altContentIdentity=\"MUSE7_SLATE_OOB\"/>\n" + 
					"    </ns0:ResponseSignal>\n" + 
					"    <ns0:ResponseSignal acquisitionPointIdentity=\"MUSE7_DISC_EAST_OOH\" acquisitionSignalID=\"781F8C6C-411B-415C-95BF-9F13775A84E4\" action=\"create\" signalPointID=\"MemVgpCnSVSpjM_JZlJIxg\">\n" + 
					"        <ns2:UTCPoint utcPoint=\"2018-10-11T21:59:40.000Z\"/>\n" + 
					"        <ns0:AlternateContent zoneIdentity=\"OOH\" altContentIdentity=\"\"/>\n" + 
					"    </ns0:ResponseSignal>\n" + 
					"</ns0:SignalProcessingNotification>", 
					EventAction.CONFIRMED, 
					EventType.SCTE224, 
					signalUTCTimeForProgramStart,
					signalUTCTimeForProgramEnd,
					false,
					acquisitionSignalIds, UpStreamNotificationMessageType.SCTE224_IP_EncoderLevel, Schema.i03, "MUSE7_DISC_EAST_OOH", "OOH");
			notificationMessages.put(scheduledNotificationId, notificationMessage);
		}
		ScheduleNotificationInfo scheduleInfo = new ScheduleNotificationInfo(notificationTimeInSeconds, notificationMessages);
		return scheduleInfo;
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.notification.scheduler.NotificationSchedulerService#getUpstreamNotificationQueueName(long)}.
	 */
	@Test
	public void testGetUpstreamNotificationQueueName() {
		long currentSystemTimeUptoSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		String upstreamQueueName = NotificationSchedulerService.getUpstreamNotificationQueueName(currentSystemTimeUptoSeconds);
		assertTrue(String.valueOf(UPSTREAM_NOTIFICATION_QUEUE_PREFIX+currentSystemTimeUptoSeconds)
				.equals(upstreamQueueName));
	}
	
	@Test
	public void testRescheduling() {
		final int currentSystemTimeUptoSeconds = (int)TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final int notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 45;
		final int numberOfMessagesToPutInQueue = 10;
		ScheduleNotificationInfo scheduleInfoOne = getScheduledInfoForTime(notificationTimeInSeconds, numberOfMessagesToPutInQueue);
		NOTIFICATION_SCHEDULER_SERVICE.scheduleNotification(scheduleInfoOne, "dummyForLog");
		
		assertEquals((int)Math.ceil((double)scheduleInfoOne.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		ScheduleNotificationInfo scheduleInfoTwo = getScheduledInfoForTime(notificationTimeInSeconds, numberOfMessagesToPutInQueue);
		NOTIFICATION_SCHEDULER_SERVICE.scheduleNotification(scheduleInfoOne, "dummyForLog");
		
		assertEquals((int)Math.ceil((double)scheduleInfoOne.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE) +
				(int)Math.ceil((double)scheduleInfoTwo.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		//Now cancel scheduling for one of the Medias.
		NOTIFICATION_SCHEDULER_SERVICE.cancelScheduledNotificationBySignalId(new CancelScheduledNotificationByEventSignalIdInfo(scheduleInfoOne.getNotificationTimeInSeconds(),
				scheduleInfoOne.getNotificationMessages().entrySet().iterator().next().getKey()));
		
		assertEquals((int)Math.ceil((double)scheduleInfoTwo.getNotificationMessages().size()/UPSTREAM_NOTIFICATION_MESSAGE_BATCH_SIZE),
				DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		DATA_MANAGER.popFromQueue(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds));		
	}

}
