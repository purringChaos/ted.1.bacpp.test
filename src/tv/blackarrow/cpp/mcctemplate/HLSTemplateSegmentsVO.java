package tv.blackarrow.cpp.mcctemplate;

/**
 * @author shwetanks
 *
 */
public class HLSTemplateSegmentsVO {

	private HLSTemplateSegmentVO firstSegment;
	private HLSTemplateSegmentVO spanSegment;
	private HLSTemplateSegmentVO lastSegment;
	
	public HLSTemplateSegmentsVO(HLSTemplateSegmentVO firstSegment, HLSTemplateSegmentVO spanSegment,
			HLSTemplateSegmentVO lastSegment) {
		super();
		this.firstSegment = firstSegment;
		this.spanSegment = spanSegment;
		this.lastSegment = lastSegment;
	}
	public HLSTemplateSegmentsVO() {
		// TODO Auto-generated constructor stub
	}
	public HLSTemplateSegmentVO getFirstSegment() {
		return firstSegment;
	}
	public void setFirstSegment(HLSTemplateSegmentVO firstSegment) {
		this.firstSegment = firstSegment;
	}
	public HLSTemplateSegmentVO getSpanSegment() {
		return spanSegment;
	}
	public void setSpanSegment(HLSTemplateSegmentVO spanSegment) {
		this.spanSegment = spanSegment;
	}
	public HLSTemplateSegmentVO getLastSegment() {
		return lastSegment;
	}
	public void setLastSegment(HLSTemplateSegmentVO lastSegment) {
		this.lastSegment = lastSegment;
	}
	
	
}
