/**
 * 
 */
package tv.blackarrow.cpp.notifications.hosted.scheduler;

/**
 * @author asharma
 *
 */
public interface HostedNotificationsSchedulerFactory {

	public static HostedNotificationScheduler getHostedNotificationScheduler(boolean immediateNotification) {
		if(immediateNotification) {
			return ImmediateHostedNotificationSchedulerService.getInstance();
		} else {
			return ScheduledHostedNotificationSchedulerService.getInstance();
		}
	}

}
