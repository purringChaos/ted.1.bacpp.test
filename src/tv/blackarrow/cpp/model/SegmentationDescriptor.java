package tv.blackarrow.cpp.model;


public class SegmentationDescriptor {

	private Short segmentationTypeId;
	private String noRegionalBlackoutFlag;
	private String segmentationUpid;
	private Short upidType;
	
	private Long breakDuration;
	private Short breakNumber; //segmentNum
	private Short totalBreak;//segmentsExpected
	private String programId;//Viacom program Id
	
	
	public Short getSegmentationTypeId() {
		return segmentationTypeId;
	}
	public void setSegmentationTypeId(Short segmentationTypeId) {
		this.segmentationTypeId = segmentationTypeId;
	}
	public String getNoRegionalBlackoutFlag() {
		return noRegionalBlackoutFlag;
	}
	public void setNoRegionalBlackoutFlag(String noRegionalBlackoutFlag) {
		this.noRegionalBlackoutFlag = noRegionalBlackoutFlag;
	}
	public String getSegmentationUpid() {
		return segmentationUpid;
	}
	public void setSegmentationUpid(String segmentationUpid) {
		this.segmentationUpid = segmentationUpid;
	}
	public Short getUpidType() {
		return upidType;
	}
	public void setUpidType(Short upidType) {
		this.upidType = upidType;
	}
	/**
	 * @return the breakDuration
	 */
	public Long getBreakDuration() {
		return breakDuration;
	}
	/**
	 * @param breakDuration the breakDuration to set
	 */
	public void setBreakDuration(Long breakDuration) {
		this.breakDuration = breakDuration;
	}
	/**
	 * @return the breakNumber
	 */
	public Short getBreakNumber() {
		return breakNumber;
	}
	/**
	 * @param breakNumber the breakNumber to set
	 */
	public void setBreakNumber(Short breakNumber) {
		this.breakNumber = breakNumber;
	}
	/**
	 * @return the totalBreak
	 */
	public Short getTotalBreak() {
		return totalBreak;
	}
	/**
	 * @param totalBreak the totalBreak to set
	 */
	public void setTotalBreak(Short totalBreak) {
		this.totalBreak = totalBreak;
	}
	/**
	 * @return the programId
	 */
	public String getProgramId() {
		return programId;
	}
	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(String programId) {
		this.programId = programId;
	}
}
