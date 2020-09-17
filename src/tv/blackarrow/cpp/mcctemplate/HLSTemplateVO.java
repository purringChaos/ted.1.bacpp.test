package tv.blackarrow.cpp.mcctemplate;

/**
 * @author shwetanks
 *
 */
public class HLSTemplateVO {

	private HLSTemplateSegmentsVO confirmationSegments;
	private HLSTemplateSegmentsVO abortSegments;
	
	public HLSTemplateVO(HLSTemplateSegmentsVO confirmationSegments, HLSTemplateSegmentsVO abortSegments) {
		super();
		this.confirmationSegments = confirmationSegments;
		this.abortSegments = abortSegments;
	}
	
	public HLSTemplateVO() {
		// TODO Auto-generated constructor stub
	}

	public HLSTemplateSegmentsVO getConfirmationSegments() {
		return confirmationSegments;
	}
	public void setConfirmationSegments(HLSTemplateSegmentsVO confirmationSegments) {
		this.confirmationSegments = confirmationSegments;
	}
	public HLSTemplateSegmentsVO getAbortSegments() {
		return abortSegments;
	}
	public void setAbortSegments(HLSTemplateSegmentsVO abortSegments) {
		this.abortSegments = abortSegments;
	}
	
}
