package tv.blackarrow.cpp.notifications.hosted.model.i02;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.notifications.hosted.model.HostedAppEventStatusNotificationModel;
import tv.blackarrow.cpp.utils.EventAction;

public class HostedAppEventStatusI02NotifyModel extends HostedAppEventStatusNotificationModel {

	@Expose
	private EventAction eventAction = null;
	@Expose
	private String feedExtRef;
	@Expose
	private String eventTypeName; // ALT_CONTENT or SIMSUB
	@Expose
	private long utcStartTime; // blackout start time
	@Expose
	private long utcStopTime; // blackout stop time
	@Expose
	private String signalId; // blackout event signal id
	@Expose
	private String eventId; // blackout event id

	@Expose
	private String acquisitionPointId;
	
	//TODO Stream2: After red.2 SCTE-224 goes on Production. Move actualUTCStartTime/actualUTCStopTime in HostedAppEventStatusI02NotifyModel class
	@Expose
	private Long actualUTCStartTime = null;
	@Expose
	private Long actualUTCStopTime = null;

	public HostedAppEventStatusI02NotifyModel() {
		super();
	}

	public HostedAppEventStatusI02NotifyModel(String feedExtRef, String eventTypeName, Long utcStartTime, Long utcStopTime, String signalId, String acquisitionPointId,
			String eventId) {
		super();

		this.feedExtRef = feedExtRef;
		this.eventTypeName = eventTypeName;
		this.utcStartTime = utcStartTime;
		this.utcStopTime = utcStopTime;
		this.signalId = signalId;
		this.eventId = eventId;
		this.acquisitionPointId = acquisitionPointId;
	}

	/**
	 * @return the feedExtRef
	 */
	public String getFeedExtRef() {
		return feedExtRef;
	}

	/**
	 * @param feedExtRef the feedExtRef to set
	 */
	public void setFeedExtRef(String feedExtRef) {
		this.feedExtRef = feedExtRef;
	}

	/**
	 * @return the eventTypeName
	 */
	public String getEventTypeName() {
		return eventTypeName;
	}

	/**
	 * @param eventTypeName the eventTypeName to set
	 */
	public void setEventTypeName(String eventTypeName) {
		this.eventTypeName = eventTypeName;
	}

	/**
	 * @return the utcStartTime
	 */
	public long getUtcStartTime() {
		return utcStartTime;
	}

	/**
	 * @param utcStartTime the utcStartTime to set
	 */
	public void setUtcStartTime(long utcStartTime) {
		this.utcStartTime = utcStartTime;
	}

	/**
	 * @return the utcStopTime
	 */
	public long getUtcStopTime() {
		return utcStopTime;
	}

	/**
	 * @param utcStopTime the utcStopTime to set
	 */
	public void setUtcStopTime(long utcStopTime) {
		this.utcStopTime = utcStopTime;
	}

	/**
	 * @return the signalId
	 */
	public String getSignalId() {
		return signalId;
	}

	/**
	 * @param signalId the signalId to set
	 */
	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	/**
	 * @return the eventId
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}	

	/**
	 * @return the acquisitionPointId
	 */
	public String getAcquisitionPointId() {
		return acquisitionPointId;
	}

	/**
	 * @param acquisitionPointId the acquisitionPointId to set
	 */
	public void setAcquisitionPointId(String acquisitionPointId) {
		this.acquisitionPointId = acquisitionPointId;
	}

	/**
	 * @return the eventAction
	 */
	public EventAction getEventAction() {
		return eventAction;
	}

	/**
	 * @param eventAction the eventAction to set
	 */
	public void setEventAction(EventAction eventAction) {
		this.eventAction = eventAction;
	}
	
	
	/**
	 * @return the actualUTCStartTime
	 */
	public Long getActualUTCStartTime() {
		return actualUTCStartTime;
	}

	/**
	 * @param actualUTCStartTime the actualUTCStartTime to set
	 */
	public void setActualUTCStartTime(Long actualUTCStartTime) {
		this.actualUTCStartTime = actualUTCStartTime;
	}

	/**
	 * @return the actualUTCStopTime
	 */
	public Long getActualUTCStopTime() {
		return actualUTCStopTime;
	}

	/**
	 * @param actualUTCStopTime the actualUTCStopTime to set
	 */
	public void setActualUTCStopTime(Long actualUTCStopTime) {
		this.actualUTCStopTime = actualUTCStopTime;
	}
	

}
