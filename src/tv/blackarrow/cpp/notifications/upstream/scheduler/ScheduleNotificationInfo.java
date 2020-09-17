/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.scheduler;

import java.util.Map;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

/**
 * @author Amit Kumar Sharma
 * 
 */
public class ScheduleNotificationInfo extends ScheduleInfo{
	private Map<String, NotificationMessage> notificationMessages;

	public ScheduleNotificationInfo(final int notificationTimeInSeconds, final Map<String, NotificationMessage> notificationMessages) {
		super(notificationTimeInSeconds);
		this.setNotificationMessages(notificationMessages);
	}

	public Map<String, NotificationMessage> getNotificationMessages() {
		return notificationMessages;
	}

	public void setNotificationMessages(Map<String, NotificationMessage> notificationMessages) {
		if(notificationMessages == null || notificationMessages.isEmpty()) {
			throw new IllegalArgumentException("'notificationMessages' can not be null or empty. "
					+ "Please provide a valid Map containing the Notification Message Id as key and Notification Message as value.");
		}
		this.notificationMessages = notificationMessages;
	}

	@Override
	public String toString() {
		return String.format("ScheduleNotificationInfo [notificationTimeInSeconds=%s, notificationMessages=%s]",
				getNotificationTimeInSeconds(), getNotificationMessages());
	}
	
	public String printable() {
		StringBuilder sb = new StringBuilder();
		if (notificationMessages != null && notificationMessages.values() != null && !notificationMessages.values().isEmpty()) {
			sb.append("[");
			for (NotificationMessage message : notificationMessages.values()) {
				sb.append(message.getScheduledNotificationId()).append("-->");
			}
			sb.append("]");
		}
		return sb.toString();
	}
	
}
