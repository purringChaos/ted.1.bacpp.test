/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tv.blackarrow.cpp.manifest.TagType;
import tv.blackarrow.cpp.mcctemplate.macros.BaseMCCTemplateMacroResolver;

/**
 * @author Amit Kumar Sharma
 *
 */
public class MCCTemplateResponseComponentBuilder {
	
	public static Map<String, List<TagType>> resolveMccHLSTemplateSegment(HLSTemplateSegmentsVO segments, BaseMCCTemplateMacroResolver macroResolver){

		Map<String, List<TagType>> segmentsMap	= new HashMap<String, List<TagType>>();
		if (segments != null) {
			if (segments.getFirstSegment() != null) {
				List<TagType> tagTypeList = new ArrayList<TagType>();
				ArrayList<HLSTemplateSegmentTagVO> tags = segments.getFirstSegment().getTags();
				for (HLSTemplateSegmentTagVO tag : tags) {
					TagType tagType = new TagType();
					tagType.setAdapt(tag.getAdapt());
					tagType.setLocality(tag.getLocality());
					String finalTagValue = macroResolver.replaceTextPlaceholdersWithActualValues(tag.getValueWithPlaceholders());
					tagType.setValue(finalTagValue);
					tagTypeList.add(tagType);
				}
				segmentsMap.put(MCCTemplateConstants.MCC_TEMPLATE_FIRST_SEGMENT, tagTypeList);
			}
			if (segments.getSpanSegment() != null) {
				List<TagType> tagTypeList = new ArrayList<TagType>();
				ArrayList<HLSTemplateSegmentTagVO> tags = segments.getSpanSegment().getTags();
				for (HLSTemplateSegmentTagVO tag : tags) {
					TagType tagType = new TagType();
					tagType.setAdapt(tag.getAdapt());
					tagType.setLocality(tag.getLocality());
					String finalTagValue = macroResolver.replaceTextPlaceholdersWithActualValues(tag.getValueWithPlaceholders());
					tagType.setValue(finalTagValue);
					tagTypeList.add(tagType);
				}
				segmentsMap.put(MCCTemplateConstants.MCC_TEMPLATE_SPAN_SEGMENT, tagTypeList);
			}
			if (segments.getLastSegment() != null) {
				List<TagType> tagTypeList = new ArrayList<TagType>();
				ArrayList<HLSTemplateSegmentTagVO> tags = segments.getLastSegment().getTags();
				for (HLSTemplateSegmentTagVO tag : tags) {
					TagType tagType = new TagType();
					tagType.setAdapt(tag.getAdapt());
					tagType.setLocality(tag.getLocality());
					String finalTagValue = macroResolver.replaceTextPlaceholdersWithActualValues(tag.getValueWithPlaceholders());
					tagType.setValue(finalTagValue);
					tagTypeList.add(tagType);
				}
				segmentsMap.put(MCCTemplateConstants.MCC_TEMPLATE_LAST_SEGMENT, tagTypeList);
			}
		}
		return segmentsMap;
	}

	public static void resolveMccDashTemplateResponse(DASHTemplateVO mccTemplateVO, BaseMCCTemplateMacroResolver macroResolver) {
		mccTemplateVO.setTemplateValue(mccTemplateVO.getPattern() == null ? "" : macroResolver.replaceTextPlaceholdersWithActualValues(mccTemplateVO.getPattern().toString()));
	}

}
