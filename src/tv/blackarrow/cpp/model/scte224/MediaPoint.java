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
public class MediaPoint {
	@Expose
	private String signalId = null;
	@Expose
	private Integer order = null;
	@Expose
	private Long effectiveTimeInMS = null;
	@Expose
	private Long expiresTimeInMS = null;
	@Expose
	private Long matchTimeInMS = null;
	@Expose
	private Long matchOffsetDurationInMS = null;
	@Expose
	private MatchSignal matchSignal = null;
	@Expose
	private LinkedList<ApplyorRemove> apply = new LinkedList<ApplyorRemove>();	
	@Expose
	private LinkedList<ApplyorRemove> remove = new LinkedList<ApplyorRemove>();
	@Expose
	private List<String> zones;//This is Cadent Zones, the Zone that is inserted by cadent:zone on media.	
	@Expose
	private String altSourceValue;
	@Expose
	private Boolean noRegionalBlackoutFlag = false;
	@Expose
	private int deviceRestrictions;

	/**
	 * 
	 */
	public MediaPoint() {
		super();
	}

	/**
	 * @param signalId
	 * @param order
	 * @param effectiveTimeInMS
	 * @param expiresTimeInMS
	 * @param matchTimeInMS
	 * @param matchOffsetInMS
	 * @param matchSignal
	 * @param apply
	 */
	public MediaPoint(String signalId, Integer order, Long effectiveTimeInMS, Long expiresTimeInMS, Long matchTimeInMS, Long matchOffsetDurationInMS, MatchSignal matchSignal,
			LinkedList<ApplyorRemove> apply, LinkedList<ApplyorRemove> remove , List<String> zoneList, Boolean noRegionalBlackoutFlag, int deviceRestrictions) {
		super();
		this.signalId = signalId;
		this.order = order;
		this.effectiveTimeInMS = effectiveTimeInMS;
		this.expiresTimeInMS = expiresTimeInMS;
		this.matchTimeInMS = matchTimeInMS;
		this.matchOffsetDurationInMS = matchOffsetDurationInMS;
		this.matchSignal = matchSignal;
		this.apply = apply;
		this.remove = remove;
		this.zones=zoneList;
		this.noRegionalBlackoutFlag = noRegionalBlackoutFlag;
		this.deviceRestrictions = deviceRestrictions;
	}

	/**
	 * @return the signalId
	 */
	public String getSignalId() {
		return signalId;
	}

	/**
	 * @param signalId the signalId to set
	 */
	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	/**
	 * @return the order
	 */
	public Integer getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 */
	public void setOrder(Integer order) {
		this.order = order;
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
	 * @return the matchTimeInMS
	 */
	public Long getMatchTimeInMS() {
		return matchTimeInMS;
	}

	/**
	 * @param matchTimeInMS the matchTimeInMS to set
	 */
	public void setMatchTimeInMS(Long matchTimeInMS) {
		this.matchTimeInMS = matchTimeInMS;
	}

	/**
	 * @return the matchOffsetDurationInMS
	 */
	public Long getMatchOffsetDurationInMS() {
		return matchOffsetDurationInMS;
	}

	/**
	 * @param matchOffsetDurationInMS the matchOffsetDurationInMS to set
	 */
	public void setMatchOffsetDurationInMS(Long matchOffsetDurationInMS) {
		this.matchOffsetDurationInMS = matchOffsetDurationInMS;
	}

	/**
	 * @return the matchSignal
	 */
	public MatchSignal getMatchSignal() {
		return matchSignal;
	}

	/**
	 * @param matchSignal the matchSignal to set
	 */
	public void setMatchSignal(MatchSignal matchSignal) {
		this.matchSignal = matchSignal;
	}

	/**
	 * @return the apply
	 */
	public List<ApplyorRemove> getApply() {
		return apply;
	}

	/**
	 * @param apply the apply to set
	 */
	public void setApply(LinkedList<ApplyorRemove> apply) {
		this.apply = apply;
	}
	

	

	/**
	 * @return the remove
	 */
	public LinkedList<ApplyorRemove> getRemove() {
		return remove;
	}

	/**
	 * @param remove the remove to set
	 */
	public void setRemove(LinkedList<ApplyorRemove> remove) {
		this.remove = remove;
	}

	/**
	 * @return the zones
	 */
	public List<String> getZones() {
		return zones;
	}

	/**
	 * @param zones the zones to set
	 */
	public void setZones(List<String> zones) {
		this.zones = zones;
	}
	

	/**
	 * @return the altSourceValue
	 */
	public String getAltSourceValue() {
		return altSourceValue;
	}

	/**
	 * @param altSourceValue the altSourceValue to set
	 */
	public void setAltSourceValue(String altSourceValue) {
		this.altSourceValue = altSourceValue;
	}

	
	/**
	 * @return the noRegionalBlackoutFlag
	 */
	public Boolean getNoRegionalBlackoutFlag() {
		return noRegionalBlackoutFlag;
	}

	/**
	 * @param noRegionalBlackoutFlag the noRegionalBlackoutFlag to set
	 */
	public void setNoRegionalBlackoutFlag(Boolean noRegionalBlackoutFlag) {
		this.noRegionalBlackoutFlag = noRegionalBlackoutFlag;
	}

	/**
	 * @return the deviceRestrictions
	 */
	public int getDeviceRestrictions() {
		return deviceRestrictions;
	}

	/**
	 * @param deviceRestrictions the deviceRestrictions to set
	 */
	public void setDeviceRestrictions(int deviceRestrictions) {
		this.deviceRestrictions = deviceRestrictions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("MediaPoint [signalId=%s, order=%s, effectiveTimeInMS=%s, expiresTimeInMS=%s, matchTimeInMS=%s, matchOffsetInMS=%s, matchSignal=%s, apply=%s, zoneList=%s, noRegionalBlackoutFlag=%s, deviceRestrictions=%s]",
				signalId, order, effectiveTimeInMS, expiresTimeInMS, matchTimeInMS, matchOffsetDurationInMS, matchSignal, apply, zones, noRegionalBlackoutFlag, deviceRestrictions);
	}

}
