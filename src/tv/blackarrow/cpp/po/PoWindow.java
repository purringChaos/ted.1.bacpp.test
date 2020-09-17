package tv.blackarrow.cpp.po;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PoWindow {

	private Date startTime;
	private Date endTime;
	private List<PoBreak> breaks = new ArrayList<PoBreak>();

	/**
	 * @return the startTime
	 */
	public Date getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	/**
	 * @return the endTime
	 */
	public Date getEndTime() {
		return endTime;
	}
	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	/**
	 * @return the breaks
	 */
	public List<PoBreak> getBreaks() {
		return breaks;
	}

}
