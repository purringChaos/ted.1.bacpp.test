package test.tv.blackarrow.cpp.notification.hosted;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.model.scte224.SCTE224EventStatus;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.notifications.hosted.model.i02.HostedAppEventStatusI02NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationScheduler;
import tv.blackarrow.cpp.notifications.hosted.scheduler.HostedNotificationsSchedulerFactory;
import tv.blackarrow.cpp.utils.EventAction;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HostedNotificationTest {
	
	private static final int DELTA_FROM_CURRENT_TIME = 60;// In Seconds
	private static final int DELTA_FOR_END_TIME = 60;// In Seconds
	
	private static final HostedNotificationScheduler immediateQueueScheduler = HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(true);
	private static final HostedNotificationScheduler scheduledQueueScheduler = HostedNotificationsSchedulerFactory.getHostedNotificationScheduler(false);
	
	public static void main(String[] args) {
		HostedNotificationTest test = new HostedNotificationTest();
		test.testScheduleNotificationListOfScheduleNotificationInfo();
	}
	
	@Test
	public void testScheduleNotificationListOfScheduleNotificationInfo() {
		immediateQueueScheduler.scheduleHostedNotification(createHostedNotifications(true, 2));
		//scheduledQueueScheduler.scheduleHostedNotification(createHostedNotifications(false, 2));
	}
	
	public Map<String, HostedAppEventStatusNotificationModel> createHostedNotifications(boolean immediate, int numberOfMessage){
		long currentTimeInSeconds = Instant.now().getEpochSecond();
		Map<String, HostedAppEventStatusNotificationModel> hostedNotificationModalMap = new HashMap<>();
		createI02Message(immediate, numberOfMessage, hostedNotificationModalMap, currentTimeInSeconds);
		//create224Message(immediate, numberOfMessage, hostedNotificationModalMap, currentTimeInSeconds);
		
		return hostedNotificationModalMap; 
	}

	private void createI02Message(boolean immediate, int numberOfMessage, Map<String, HostedAppEventStatusNotificationModel> hostedNotificationModalMap, long currentTimeInSeconds) {
		String streamId = "I02AQ0";
		String signalId = "I02XMzGTtbDRvKYrO1nNR4A0A";
		String key = "%s#@#%s#@#%s";
		HostedAppEventStatusI02NotifyModel modal = null;
		for(int index = 1; index <= numberOfMessage; index++) {
			if(!immediate) {
				modal = new HostedAppEventStatusI02NotifyModel("ESPN_FEED_N", "ALT_CONTENT", 0L, 0L, "I"+signalId+index, streamId+index, "29_40");
				modal.setEventAction(EventAction.ERROR);
				modal.setEventType(EventType.I02);
				modal.setActualUTCStartTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME+index);
				modal.setActualUTCStopTime(currentTimeInSeconds+DELTA_FOR_END_TIME+index);
				modal.setHostedNotificationScheduleTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME);
				modal.setHostedNotificationTime(currentTimeInSeconds);
				
				String entrykey = String.format(key, "I"+signalId+index, "ERROR_CHECK", "CONFIRMED");
				hostedNotificationModalMap.put(entrykey, modal);
			}
		}
		
		for(int index = 1; index <= numberOfMessage; index++) {
			if(immediate) {				
				modal = new HostedAppEventStatusI02NotifyModel("ESPN_FEED_N", "ALT_CONTENT", 0L, 0L, "S"+signalId+index, streamId+index, "29_40");
				modal.setEventAction(EventAction.CONFIRMED);
				modal.setEventType(EventType.I02);
				modal.setActualUTCStartTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME+index);
				modal.setActualUTCStopTime(currentTimeInSeconds+DELTA_FOR_END_TIME+index);
				modal.setHostedNotificationTime(currentTimeInSeconds);
				
				String entrykey = String.format(key, "S"+signalId+index, streamId+index, "CONFIRMED");
				hostedNotificationModalMap.put(entrykey, modal);
			}
		}
	}
	
	private void create224Message(boolean immediate, int numberOfMessage, Map<String, HostedAppEventStatusNotificationModel> hostedNotificationModalMap, long currentTimeInSeconds) {
		String streamId = "SCTE224_AQ0";
		String signalId = "SCTE224_XMzGTtbDRvKYrO1nNR4A0A";
		String key = "%s#@#%s#@#%s";
		HostedAppEventStatusScte224NotifyModel modal = null;
		for(int index = 1; index <= numberOfMessage; index++) {
			if(!immediate) {
				modal = new HostedAppEventStatusScte224NotifyModel();
				modal.setEventSignalId(signalId + index);
				modal.setStreamId(streamId + index);
				modal.setScheduledTime(currentTimeInSeconds + DELTA_FROM_CURRENT_TIME);

				modal.setEventStatus(SCTE224EventStatus.ERROR);
				modal.setEventType(EventType.SCTE224);
				modal.setSignalTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME+index);
				modal.setHostedNotificationScheduleTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME);
				modal.setHostedNotificationTime(currentTimeInSeconds);
				modal.setHostedNotificationScheduleTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME);
				modal.setHostedNotificationTime(currentTimeInSeconds);
				String entrykey = String.format(key, "I"+signalId+index, "ERROR_CHECK", "CONFIRMED");
				
				hostedNotificationModalMap.put(entrykey, modal);
			}
		}
		
		for(int index = (numberOfMessage+1); index <= 2*numberOfMessage; index++) {
			if(immediate) {
				modal = new HostedAppEventStatusScte224NotifyModel();

				modal.setEventSignalId(signalId + index);
				modal.setStreamId(streamId + index);
				modal.setScheduledTime(currentTimeInSeconds + DELTA_FROM_CURRENT_TIME);
				
				modal.setEventStatus(SCTE224EventStatus.CONFIRMED);
				modal.setEventType(EventType.SCTE224);
				modal.setSignalTime(currentTimeInSeconds+DELTA_FROM_CURRENT_TIME+index);
				String entrykey = String.format(key, "S"+signalId+index, streamId+index, "CONFIRMED");
				hostedNotificationModalMap.put(entrykey, modal);
			}
		}
	}

}
