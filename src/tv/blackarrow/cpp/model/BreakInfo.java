package tv.blackarrow.cpp.model;

import java.util.HashMap;

public class BreakInfo implements Comparable<BreakInfo> {

    private String inSignalId;
	private String breakId;
	private Integer duration;

	// intentionally using a concrete type here so that deserialization works cleanly
	private HashMap<String, String> qualifiers;
	
	public BreakInfo(String inSignalId, String breakId, Integer duration) {
		this(inSignalId, breakId, duration, null);
	}
	
	public BreakInfo(String inSignalId, String breakId, Integer duration, HashMap<String, String> qualifiers) {
		this.inSignalId = inSignalId;
		this.breakId = breakId;
		this.duration = duration;
		this.qualifiers = qualifiers;
	}
	
	public String getInSignalId() {
		return inSignalId;
	}
	public void setInSignalId(String inSignalId) {
		this.inSignalId = inSignalId;
	}
	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	
	public String getBreakId() {
		return breakId;
	}

	public void setBreakId(String breakId) {
		this.breakId = breakId;
	}

	public int compareTo(BreakInfo breakInfo) {
		
		Integer compDuration = breakInfo.getDuration(); 
 
		//ascending order
		return this.duration.intValue() - compDuration.intValue();
	}

	public HashMap<String, String> getQualifiers() {
		return qualifiers;
	}

	public void setQualifiers(HashMap<String, String> qualifiers) {
		this.qualifiers = qualifiers;
	}	
}
