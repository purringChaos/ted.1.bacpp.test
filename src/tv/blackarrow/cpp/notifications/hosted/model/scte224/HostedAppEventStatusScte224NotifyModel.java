/**
 * 
 */
package tv.blackarrow.cpp.notifications.hosted.model.scte224;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.model.scte224.SCTE224EventStatus;
import tv.blackarrow.cpp.notifications.hosted.enums.scte224.NotificationSignalTrigger;
import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;

/**
 * @author Amit Kumar Sharma
 *
 */
public class HostedAppEventStatusScte224NotifyModel extends HostedAppEventStatusNotificationModel {

	@Expose
	private String eventSignalId = null;
	@Expose
	private String streamId = null;
	@Expose
	private long scheduledTime;
	@Expose
	private NotificationSignalTrigger notificationSignalTrigger = NotificationSignalTrigger.SIGNAL;
	
	@Expose
	private String mediaPointSignalId = null;
	@Expose
	private Long signalTime = null;
	@Expose 
	private SCTE224EventStatus eventStatus = null;	


	public HostedAppEventStatusScte224NotifyModel() {
		super();
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

	/**
	 * @return the streamId
	 */
	public String getStreamId() {
		return streamId;
	}

	/**
	 * @param streamId the streamId to set
	 */
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	/**
	 * @return the scheduledTime
	 */
	public long getScheduledTime() {
		return scheduledTime;
	}

	/**
	 * @param scheduledTime the scheduledTime to set
	 */
	public void setScheduledTime(long scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	public NotificationSignalTrigger getNotificationSignalTrigger() {
		return notificationSignalTrigger;
	}

	public void setNotificationSignalTrigger(NotificationSignalTrigger notificationSignalTrigger) {
		this.notificationSignalTrigger = notificationSignalTrigger;
	}

	public String getMediaPointSignalId() {
		return mediaPointSignalId;
	}

	public void setMediaPointSignalId(String mediaPointSignalId) {
		this.mediaPointSignalId = mediaPointSignalId;
	}

	public Long getSignalTime() {
		return signalTime;
	}

	public void setSignalTime(Long signalTime) {
		this.signalTime = signalTime;
	}
	
	public SCTE224EventStatus getEventStatus() {
		return eventStatus;
	}


	public void setEventStatus(SCTE224EventStatus eventStatus) {
		this.eventStatus = eventStatus;
	}


	@Override
	public String toString() {
		return "HostedAppEventStatusScte224NotifyModel [eventSignalId=" + eventSignalId + ", streamId=" + streamId + ", scheduledTime=" + scheduledTime + ", notificationSignalTrigger=" + notificationSignalTrigger + ", mediaPointSignalId="
				+ mediaPointSignalId + ", signalTime=" + signalTime + ", eventStatus=" + eventStatus + ", toString()=" + super.toString() + "]";
	}	

}
