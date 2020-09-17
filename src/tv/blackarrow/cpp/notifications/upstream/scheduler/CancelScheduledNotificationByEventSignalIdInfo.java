/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.scheduler;

/**
 * @author Amit Kumar Sharma
 *
 */
public class CancelScheduledNotificationByEventSignalIdInfo extends ScheduleInfo {

	private String eventSignalId;
	
	public CancelScheduledNotificationByEventSignalIdInfo(final int notificationTimeInSeconds,
			final String eventSignalId) {
		super(notificationTimeInSeconds);
		this.eventSignalId = eventSignalId;
	}

	/**
	 * @return the eventSignalId
	 */
	public String getEventSignalId() {
		return eventSignalId;
	}

	/**
	 * @param eventSignalId the eventSignalId to set
	 */
	public void setEventSignalId(String eventSignalId) {
		this.eventSignalId = eventSignalId;
	}
}
