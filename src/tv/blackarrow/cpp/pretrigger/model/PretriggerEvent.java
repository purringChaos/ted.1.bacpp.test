package tv.blackarrow.cpp.pretrigger.model;

public class PretriggerEvent {
	private String breakId;
	private String breakUuid;
	private long startTime;
	private long lastSpotStartTime;
	private long duration;
	private long signalOffset;
	private String acqusitionId; 
	private String feedId; 
	private String transcoderUrl;
	private String esamVersion;

	public String getBreakId() {
		return breakId;
	}

	public void setBreakId(String breakId) {
		this.breakId = breakId;
	}

	public String getBreakUuid() {
		return breakUuid;
	}

	public void setBreakUuid(String breakUuid) {
		this.breakUuid = breakUuid;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getLastSpotStartTime() {
		return lastSpotStartTime;
	}

	public void setLastSpotStartTime(long lastSpotStartTime) {
		this.lastSpotStartTime = lastSpotStartTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getAcqusitionId() {
		return acqusitionId;
	}

	public void setAcqusitionId(String acqusitionId) {
		this.acqusitionId = acqusitionId;
	}

	public String getFeedId() {
		return feedId;
	}

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public String getTranscoderUrl() {
		return transcoderUrl;
	}

	public void setTranscoderUrl(String transcoderUrl) {
		this.transcoderUrl = transcoderUrl;
	}

	public String getEsamVersion() {
		return esamVersion;
	}

	public void setEsamVersion(String esamVersion) {
		this.esamVersion = esamVersion;
	}

	public long getSignalOffset() {
		return signalOffset;
	}

	public void setSignalOffset(long signalOffset) {
		this.signalOffset = signalOffset;
	}

	@Override
	public String toString() {
		return "PretriggerEvent [breakId=" + breakId + ", breakUuid=" + breakUuid + ", startTime=" + startTime
				+ ", duration=" + duration + ", signalOffset=" + signalOffset + ", acqusitionId=" + acqusitionId
				+ ", feedId=" + feedId + ", transcoderUrl=" + transcoderUrl + ", esamVersion=" + esamVersion + "]";
	}

}
