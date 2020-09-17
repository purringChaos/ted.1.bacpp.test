/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * @author Amit Kumar Sharma
 *
 */
public class Media {

	@Expose
	private String mediaId = null;
	@Expose
	private String feedId = null;
	@Expose
	private Long effectiveTimeInMS = null;
	@Expose
	private Long expiresTimeInMS = null;
	@Expose
	private Long lastUpdateTimeInMS = null;
	@Expose
	private String source = null;
	@Expose
	private String mediaSignalid = null;
	@Expose
	private String signalid = null;
	@Expose
	LinkedList<MediaPoint> mediaPoints = new LinkedList<MediaPoint>();
	@Expose
	private Boolean blackoutOverride;
	private Boolean programRunover = false;
	@Expose
	private Boolean isInflightMediaDeleted = false;
	
	public Media() {
		super();
	}

	/**
	 * @param mediaId
	 * @param feedId
	 * @param effectiveTimeInMS
	 * @param expiresTimeInMS
	 * @param lastUpdateTimeInMS
	 * @param source
	 * @param signalid
	 * @param mediaPoints
	 */
	public Media(String mediaId, String feedId, Long effectiveTimeInMS, Long expiresTimeInMS, Long lastUpdateTimeInMS, String source, String signalid, String mediaSignalid,
			LinkedList<MediaPoint> mediaPoints,  Boolean blackoutOverride, Boolean programRunover) {
		this();
		this.mediaId = mediaId;
		this.feedId = feedId;
		this.effectiveTimeInMS = effectiveTimeInMS;
		this.expiresTimeInMS = expiresTimeInMS;
		this.lastUpdateTimeInMS = lastUpdateTimeInMS;
		this.source = source;
		this.signalid = signalid;
		this.mediaSignalid = mediaSignalid;
		this.mediaPoints = mediaPoints;
		this.blackoutOverride = blackoutOverride;
		this.programRunover = programRunover;
	}

	/**
	 * @return the mediaId
	 */
	public String getMediaId() {
		return mediaId;
	}

	/**
	 * @param mediaId the mediaId to set
	 */
	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

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
	 * @return the effectiveTimeInMS
	 */
	public Long getEffectiveTimeInMS() {
		return effectiveTimeInMS;
	}

	/**
	 * @param effectiveTimeInMS the effectiveTimeInMS to set
	 */
	public void setEffectiveTimeInMS(Long effectiveTimeInMS) {
		this.effectiveTimeInMS = effectiveTimeInMS;
	}

	/**
	 * @return the expiresTimeInMS
	 */
	public Long getExpiresTimeInMS() {
		return expiresTimeInMS;
	}

	/**
	 * @param expiresTimeInMS the expiresTimeInMS to set
	 */
	public void setExpiresTimeInMS(Long expiresTimeInMS) {
		this.expiresTimeInMS = expiresTimeInMS;
	}

	/**
	 * @return the lastUpdateTimeInMS
	 */
	public Long getLastUpdateTimeInMS() {
		return lastUpdateTimeInMS;
	}

	/**
	 * @param lastUpdateTimeInMS the lastUpdateTimeInMS to set
	 */
	public void setLastUpdateTimeInMS(Long lastUpdateTimeInMS) {
		this.lastUpdateTimeInMS = lastUpdateTimeInMS;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the signalid
	 */
	public String getSignalid() {
		return signalid;
	}

	/**
	 * @param signalid the signalid to set
	 */
	public void setSignalid(String signalid) {
		this.signalid = signalid;
	}

	/**
	 * @return the mediaPoints
	 */
	public List<MediaPoint> getMediaPoints() {
		return mediaPoints;
	}

	/**
	 * @param mediaPoints the mediaPoints to set
	 */
	public void setMediaPoints(LinkedList<MediaPoint> mediaPoints) {
		this.mediaPoints = mediaPoints;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Media) {
			Media m = (Media) obj;
			return this.signalid.equals(m.signalid);
		} else {
			return false;
		}
	}

	public String getMediaSignalid() {
		return mediaSignalid;
	}

	public void setMediaSignalid(String mediaSignalid) {
		this.mediaSignalid = mediaSignalid;
	}

	
	/**
	 * @return the blackoutOverride
	 */
	public Boolean iSBlackoutOverride() {
		return blackoutOverride;
	}

	/**
	 * @param blackoutOverride the blackoutOverride to set
	 */
	public void setBlackoutOverride(Boolean blackoutOverride) {
		this.blackoutOverride = blackoutOverride;
	}
	
	
	/**
	 * @return the programRunover
	 */
	public Boolean isProgramRunover() {
		return programRunover;
	}

	/**
	 * @param programRunover the programRunover to set
	 */
	public void setProgramRunover(Boolean programRunover) {
		this.programRunover = programRunover;
	}

	/**
	 * @return the isInflightMediaDeleted
	 */
	public Boolean isInflightMediaDeleted() {
		return isInflightMediaDeleted;
	}

	/**
	 * @param isInflightMediaDeleted the isInflightMediaDeleted to set
	 */
	public void setInflMediaDeleted(Boolean isInflightMediaDeleted) {
		this.isInflightMediaDeleted = isInflightMediaDeleted;
	}

	@Override
	public String toString() {
		return "Media [mediaId=" + mediaId + ", feedId=" + feedId + ", effectiveTimeInMS=" + effectiveTimeInMS + ", expiresTimeInMS=" + expiresTimeInMS + ", lastUpdateTimeInMS="
				+ lastUpdateTimeInMS + ", source=" + source + ", mediaSignalId=" + mediaSignalid + ", signalid=" + signalid + ", mediaPoints=" + mediaPoints + ", blackoutOverride=" 
				+ blackoutOverride + ", programRunover=" + programRunover + ",isInflightMediaDeleted=" + isInflightMediaDeleted + "]";
	}

}
