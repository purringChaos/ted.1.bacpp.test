/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.scheduler;

import java.util.List;

/**
 * @author Amit Kumar Sharma
 *
 */
public interface NotificationScheduler {
	
	public void cancelScheduledNotificationBySignalId(CancelScheduledNotificationByEventSignalIdInfo cancelScheduledNotificationInfo);
	
	public void cancelScheduledNotificationBySignalId(List<CancelScheduledNotificationByEventSignalIdInfo> cancelScheduledNotificationInfos);

	public void scheduleNotification(List<ScheduleNotificationInfo> scheduleInfos, String eventIdForLogging);
	
	public void scheduleNotification(ScheduleNotificationInfo scheduleInfo, String eventIdForLogging);

	public void cancelScheduledNotificationByJobId(String blackoutSignalId, String scheduledNotificationId);

	public void reScheduledHostedNotificationByJobId(String string);
	
}
