/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.scheduler;

/**
 * @author Amit Kumar Sharma
 *
 */
public class ScheduleInfo {
	
	private int notificationTimeInSeconds;

	public ScheduleInfo(final int notificationTimeInSeconds) {
		this.setNotificationTimeInSeconds(notificationTimeInSeconds);
	}

	public int getNotificationTimeInSeconds() {
		return notificationTimeInSeconds;
	}

	public void setNotificationTimeInSeconds(int notificationTimeInSeconds) {
		if(notificationTimeInSeconds <=0) {
			throw new IllegalArgumentException("'notificationTimeInSeconds' can not be a -ve number or zero. "
					+ "Please provide a +ve integral value that represents time in second from Epoch.");
		}
		this.notificationTimeInSeconds = notificationTimeInSeconds;
	}

	@Override
	public String toString() {
		return String.format("ScheduleInfo [notificationTimeInSeconds=%s]", notificationTimeInSeconds);
	}
	
}
