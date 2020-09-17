package tv.blackarrow.cpp.mcctemplate;

import java.util.ArrayList;

/**
 * @author shwetanks
 *
 */
public class HLSTemplateSegmentVO {
	
	private ArrayList<HLSTemplateSegmentTagVO> tags = new ArrayList<HLSTemplateSegmentTagVO>();

	public HLSTemplateSegmentVO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ArrayList<HLSTemplateSegmentTagVO> getTags() {
		return tags;
	}

	public void setTags(ArrayList<HLSTemplateSegmentTagVO> tags) {
		this.tags = tags;
	}

	public HLSTemplateSegmentVO(ArrayList<HLSTemplateSegmentTagVO> tags) {
		super();
		this.tags = tags;
	}
	
	public void addTag(HLSTemplateSegmentTagVO tag){
		tags.add(tag);
	}

}
