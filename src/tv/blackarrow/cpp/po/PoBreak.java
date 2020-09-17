package tv.blackarrow.cpp.po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PoBreak {

	private int positionInWindow;
	private String signalId;
	private Date eventTime;
	private List<PlacementOpportunity> spots = new ArrayList<PlacementOpportunity>();

	/**
	 * @return the positionInWindow
	 */
	public int getPositionInWindow() {
		return positionInWindow;
	}
	/**
	 * @param positionInWindow the positionInWindow to set
	 */
	public void setPositionInWindow(int positionInWindow) {
		this.positionInWindow = positionInWindow;
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
	 * @return the eventTime
	 */
	public Date getEventTime() {
		return eventTime;
	}
	/**
	 * @param eventTime the eventTime to set
	 */
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}
	/**
	 * @return the spots
	 */
	public List<PlacementOpportunity> getSpots() {
		return spots;
	}

}
