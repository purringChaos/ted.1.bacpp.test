package tv.blackarrow.cpp.pretrigger.model;

public class MediaPoint {
	private String breakId;
	private String spotId;
	private String matchTime;
	private String duration;
	private String breakUuid;

	public String getBreakId() {
		return breakId;
	}

	public void setBreakId(String breakId) {
		this.breakId = breakId;
	}

	public String getSpotId() {
		return spotId;
	}

	public void setSpotId(String spotId) {
		this.spotId = spotId;
	}

	public String getMatchTime() {
		return matchTime;
	}

	public void setMatchTime(String matchTime) {
		this.matchTime = matchTime;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getBreakUuid() {
		return breakUuid;
	}

	public void setBreakUuid(String breakUuid) {
		this.breakUuid = breakUuid;
	}

	@Override
	public String toString() {
		return "MediaPoint [breakId=" + breakId + ", spotId=" + spotId + ", matchTime=" + matchTime + ", duration="
				+ duration + ", breakUuid=" + breakUuid + "]";
	}

}
