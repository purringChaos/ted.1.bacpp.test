package tv.blackarrow.cpp.model;

import java.util.List;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.utils.EventAction;

public class BlackoutEvent{
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
	private long lastUpdateTime; // the last time this event was updated.
	@Expose(serialize = false)
	private EventAction eventAction; // Used only in real time message from hosted to runtime on event create/update/delete .
	@Expose
	private String territorySignalId;
	@Expose
	private List<Restriction> restrictions;
	
	public String getFeedExtRef() {
		return feedExtRef;
	}

	public void setFeedExtRef(String feedExtRef) {
		this.feedExtRef = feedExtRef;
	}

	public String getEventTypeName() {
		return eventTypeName;
	}

	public void setEventTypeName(String eventTypeName) {
		this.eventTypeName = eventTypeName;
	}

	public long getUtcStartTime() {
		return utcStartTime;
	}

	public void setUtcStartTime(long utcStartTime) {
		this.utcStartTime = utcStartTime;
	}

	public long getUtcStopTime() {
		return utcStopTime;
	}

	public void setUtcStopTime(long utcStopTime) {
		this.utcStopTime = utcStopTime;
	}

	public String getSignalId() {
		return signalId;
	}

	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	
	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	/**
	 * @return the lastUpdateTime
	 */
	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	/**
	 * @param lastUpdateTime the lastUpdateTime to set
	 */
	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
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
	public void setEventAction(String eventAction) {
		this.eventAction = EventAction.valueOf(eventAction);
	}
	
	/**
	 * @param eventAction the eventAction to set
	 */
	public void setEventAction(EventAction eventAction) {
		this.eventAction = eventAction;
	}

	@Override
	public String toString() {
		return "BlackoutEvent [feedExtRef=" + feedExtRef + ", eventTypeName="
				+ eventTypeName + ", utcStartTime=" + utcStartTime
				+ ", utcStopTime=" + utcStopTime + ", signalId=" + signalId
				+ ", eventId=" + eventId + ", lastUpdateTime=" + lastUpdateTime +"]";
	}

	@Override
	public boolean equals(Object o) {
		if(this == o){return true;}
		if(o instanceof BlackoutEvent){
			BlackoutEvent e = (BlackoutEvent)o;
			if(!eventId.equals(e.getEventId())){
				return false;
			}
			if(!signalId.equals(e.getSignalId())){
				return false;
			}
			if(utcStartTime != e.getUtcStartTime()){
				return false;
			}
			if(utcStopTime != e.getUtcStopTime()){
				return false;
			}
			return true;
		}
		return false;
	}
	
	public static BlackoutEvent getCopy(final BlackoutEvent originalBlackout){
		if(originalBlackout == null){
			return null;
		}
		BlackoutEvent clonedBlackout = new BlackoutEvent();
		clonedBlackout.setEventAction(originalBlackout.getEventAction());
		clonedBlackout.setEventId(originalBlackout.getEventId());
		clonedBlackout.setEventTypeName(originalBlackout.getEventTypeName());
		clonedBlackout.setFeedExtRef(originalBlackout.getFeedExtRef());
		clonedBlackout.setLastUpdateTime(originalBlackout.getLastUpdateTime());
		clonedBlackout.setSignalId(originalBlackout.getSignalId());
		clonedBlackout.setUtcStartTime(originalBlackout.getUtcStartTime());
		clonedBlackout.setUtcStopTime(originalBlackout.getUtcStopTime());
		clonedBlackout.setRestrictions(originalBlackout.getRestrictions());
		clonedBlackout.setTerritorySignalId(originalBlackout.getTerritorySignalId());
		return clonedBlackout;
	}
	
	public static long getActualBlackoutStopTime(ConfirmedPlacementOpportunity cpo, BlackoutEvent blackoutEvent) {
		return cpo == null ? blackoutEvent.getUtcStopTime() : 
			(cpo.isAborted() ? cpo.getAbortTime() : 
				(cpo.hasProgramEndReceived() ? cpo.getActualUtcStopTime() : blackoutEvent.getUtcStopTime()));
	}
	
	public static long getActualBlackoutStartTime(ConfirmedPlacementOpportunity cpo, BlackoutEvent blackoutEvent) {
		return cpo != null && cpo.getActualUtcStartTime() > 0 ? cpo.getActualUtcStartTime() : blackoutEvent.getUtcStartTime();
	}

	public String getTerritorySignalId() {
		return territorySignalId;
	}

	public void setTerritorySignalId(String territorySignalId) {
		this.territorySignalId = territorySignalId;
	}

	public List<Restriction> getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(List<Restriction> restrictions) {
		this.restrictions = restrictions;
	}
}
