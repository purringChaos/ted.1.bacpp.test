package tv.blackarrow.cpp.model;

import java.util.HashMap;

public class POInfo {

	private String poKey;
	private String inSignalId;
	private int duration;
	
    // each PO has its own metadata
    // intentionally using a concrete type here so that deserialization works cleanly
	private HashMap<String, String> metadata;

	public String getInSignalId() {
		return inSignalId;
	}
	public void setInSignalId(String inSignalId) {
		this.inSignalId = inSignalId;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public String getPoKey() {
		return poKey;
	}
	public void setPoKey(String poKey) {
		this.poKey = poKey;
	}
	public HashMap<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(HashMap<String, String> metadata) {
		this.metadata = metadata;
	}
		
}
