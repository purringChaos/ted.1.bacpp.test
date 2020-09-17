package tv.blackarrow.cpp.po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PoSchedule {

	private String feedId;
	private String scheduleId;
	private String networkNumber;
	private String zoneNumber;
	private Date scheduleDate;
	private List<PoWindow> windows = new ArrayList<PoWindow>();

	/**
	 * @return the feedId
	 */
	public String getFeedId() {
		return feedId;
	}
	/**
	 * @param feedId the feedId to set
	 */
	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}
	/**
	 * @return the scheduleId
	 */
	public String getScheduleId() {
		return scheduleId;
	}
	/**
	 * @param scheduleId the scheduleId to set
	 */
	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}
	/**
	 * @return the networkNumber
	 */
	public String getNetworkNumber() {
		return networkNumber;
	}
	/**
	 * @param networkNumber the networkNumber to set
	 */
	public void setNetworkNumber(String networkNumber) {
		this.networkNumber = networkNumber;
	}
	/**
	 * @return the zoneNumber
	 */
	public String getZoneNumber() {
		return zoneNumber;
	}
	/**
	 * @param zoneNumber the zoneNumber to set
	 */
	public void setZoneNumber(String zoneNumber) {
		this.zoneNumber = zoneNumber;
	}
	/**
	 * @return the scheduleDate
	 */
	public Date getScheduleDate() {
		return scheduleDate;
	}
	/**
	 * @param scheduleDate the scheduleDate to set
	 */
	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}
	/**
	 * @return the windows
	 */
	public List<PoWindow> getWindows() {
		return windows;
	}
	@Override
	public String toString() {
		return "PoSchedule [feedId=" + feedId + ", scheduleId=" + scheduleId
				+ ", networkNumber=" + networkNumber + ", zoneNumber="
				+ zoneNumber + ", scheduleDate=" + scheduleDate + ", windows="
				+ windows + "]";
	}
	
}
