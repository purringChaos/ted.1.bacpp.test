package tv.blackarrow.cpp.model.scte224;

import com.google.gson.annotations.Expose;

public class CompactMediaInfo {
	@Expose
	private Long effectiveTimeInMS = null;
	@Expose
	private Long expiresTimeInMS = null;
	@Expose
	private Long lastUpdateTimeInMS = null;
	@Expose
	private String mediaSignalId = null;
	@Expose
	private String signalid = null;
	@Expose
	private Boolean matchSignalExists = null;
	
	public CompactMediaInfo(Long effectiveTimeInMS, Long expiresTimeInMS, Long lastUpdateTimeInMS, String signalid, String mediaSignalId, Boolean matchSignalExists) {
		super();
		this.effectiveTimeInMS = effectiveTimeInMS;
		this.expiresTimeInMS = expiresTimeInMS;
		this.lastUpdateTimeInMS = lastUpdateTimeInMS;
		this.signalid = signalid;
		this.mediaSignalId = mediaSignalId;
		this.matchSignalExists = matchSignalExists;
		
	}

	public Long getEffectiveTimeInMS() {
		return effectiveTimeInMS;
	}

	public void setEffectiveTimeInMS(Long effectiveTimeInMS) {
		this.effectiveTimeInMS = effectiveTimeInMS;
	}

	public Long getExpiresTimeInMS() {
		return expiresTimeInMS;
	}

	public void setExpiresTimeInMS(Long expiresTimeInMS) {
		this.expiresTimeInMS = expiresTimeInMS;
	}

	public Long getLastUpdateTimeInMS() {
		return lastUpdateTimeInMS;
	}

	public void setLastUpdateTimeInMS(Long lastUpdateTimeInMS) {
		this.lastUpdateTimeInMS = lastUpdateTimeInMS;
	}

	public String getSignalid() {
		return signalid;
	}

	public void setSignalid(String signalid) {
		this.signalid = signalid;
	}

	public Boolean getMatchSignalExists() {
		return matchSignalExists;
	}

	public void setMatchSignalExists(Boolean matchSignalExists) {
		this.matchSignalExists = matchSignalExists;
	}

	@Override
	public String toString() {
		return "CompactMediaInfo [effectiveTimeInMS=" + effectiveTimeInMS + ", expiresTimeInMS=" + expiresTimeInMS + ", lastUpdateTimeInMS=" + lastUpdateTimeInMS + ", signalid="
				+ signalid + ", matchSignalExists=" + matchSignalExists + "]";
	}

	public String getMediaSignalId() {
		return mediaSignalId;
	}

	public void setMediaSignalId(String mediaSignalId) {
		this.mediaSignalId = mediaSignalId;
	}

}
