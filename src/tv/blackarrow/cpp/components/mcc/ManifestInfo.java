package tv.blackarrow.cpp.components.mcc;

import tv.blackarrow.cpp.handler.Schema;

public 	class ManifestInfo {
	String apid = "";
	String feed = "";
	String network = "";
	String utc = "";
	String signalId = "";
	String inSignalId = "";
	Integer largestDuration = 0;
	Schema schema;
	
	public String getApid() {
		return apid;
	}
	public void setApid(String apid) {
		this.apid = apid;
	}
	public String getFeed() {
		return feed;
	}
	public void setFeed(String feed) {
		this.feed = feed;
	}
	public String getNetwork() {
		return network;
	}
	public void setNetwork(String network) {
		this.network = network;
	}
	public String getUtc() {
		return utc;
	}
	public void setUtc(String utc) {
		this.utc = utc;
	}
	public String getSignalId() {
		return signalId;
	}
	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}
	public String getInSignalId() {
		return inSignalId;
	}
	public void setInSignalId(String inSignalId) {
		this.inSignalId = inSignalId;
	}
	public Integer getLargestDuration() {
		return largestDuration;
	}
	public void setLargestDuration(Integer largestDuration) {
		this.largestDuration = largestDuration;
	}
	public Schema getSchema() {
		return schema;
	}
	public void setSchema(Schema schema) {
		this.schema = schema;
	}	
}