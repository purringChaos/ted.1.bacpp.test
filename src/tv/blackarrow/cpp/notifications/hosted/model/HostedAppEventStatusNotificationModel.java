package tv.blackarrow.cpp.notifications.hosted.model;

import java.util.Objects;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.EventType;

public class HostedAppEventStatusNotificationModel {


	@Expose
	private EventType eventType = null;

	@Expose
	private long hostedNotificationTime;

	private long hostedNotificationScheduleTime;

	public HostedAppEventStatusNotificationModel() {
		// TODO Auto-generated constructor stub
	}

	

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return eventType;
	}

	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the hostedNotificationTime
	 */
	public long getHostedNotificationTime() {
		return hostedNotificationTime;
	}

	/**
	 * @param hostedNotificationTime the hostedNotificationTime to set
	 */
	public void setHostedNotificationTime(long hostedNotificationTime) {
		this.hostedNotificationTime = hostedNotificationTime;
	}

	/**
	 * @return the hostedNotificationScheduleTime
	 */
	public long getHostedNotificationScheduleTime() {
		return hostedNotificationScheduleTime;
	}

	/**
	 * @param hostedNotificationScheduleTime the hostedNotificationScheduleTime to set
	 */
	public void setHostedNotificationScheduleTime(long hostedNotificationScheduleTime) {
		this.hostedNotificationScheduleTime = hostedNotificationScheduleTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */

	
	
	public static long getActualBlackoutStopTime(ConfirmedPlacementOpportunity cpo) {
		long utcStopTime = 0L;
		if(Objects.nonNull(cpo)) {
			if(cpo.isAborted()) {
				utcStopTime = cpo.getAbortTime();
			}else if(cpo.hasProgramEndReceived()) {
				utcStopTime = cpo.getActualUtcStopTime();
			}
		}
		return utcStopTime;
	}
	
	public static long getActualBlackoutStartTime(ConfirmedPlacementOpportunity cpo) {
		if(Objects.nonNull(cpo)) {
			return cpo.getActualUtcStartTime();
		}
		return 0L;
	}


	@Override
	public String toString() {
		return "HostedAppEventStatusNotificationModel [eventType=" + eventType + ", hostedNotificationTime=" + hostedNotificationTime + ", hostedNotificationScheduleTime=" + hostedNotificationScheduleTime + "]";
	}

}
