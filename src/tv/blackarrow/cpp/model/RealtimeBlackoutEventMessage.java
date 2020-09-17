/**
 * 
 */
package tv.blackarrow.cpp.model;

import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * @author amit
 *
 */
public class RealtimeBlackoutEventMessage {
	@Expose
	private Integer timeBuffer = null;
	@Expose
	private List<BlackoutEvent> linearAltEvents = null;
	
	public RealtimeBlackoutEventMessage(Integer timeBuffer, List<BlackoutEvent> linearAltEvents) {
		super();
		this.timeBuffer = timeBuffer;
		this.linearAltEvents = linearAltEvents;
	}

	/**
	 * @return the timeBuffer
	 */
	public Integer getTimeBuffer() {
		return timeBuffer;
	}

	/**
	 * @param timeBuffer the timeBuffer to set
	 */
	public void setTimeBuffer(Integer timeBuffer) {
		this.timeBuffer = timeBuffer;
	}

	/**
	 * @return the linearAltEvents
	 */
	public List<BlackoutEvent> getLinearAltEvents() {
		return linearAltEvents;
	}

	/**
	 * @param linearAltEvents the linearAltEvents to set
	 */
	public void setLinearAltEvents(List<BlackoutEvent> linearAltEvents) {
		this.linearAltEvents = linearAltEvents;
	}
}