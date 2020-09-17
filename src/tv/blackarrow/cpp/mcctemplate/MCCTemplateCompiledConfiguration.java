package tv.blackarrow.cpp.mcctemplate;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.mccresponse.DASHTemplate;
import tv.blackarrow.cpp.model.mccresponse.HLSTemplate;
import tv.blackarrow.cpp.model.mccresponse.Locality;
import tv.blackarrow.cpp.model.mccresponse.MccTemplates;
import tv.blackarrow.cpp.model.mccresponse.Replace;
import tv.blackarrow.cpp.model.mccresponse.Replacements;
import tv.blackarrow.cpp.model.mccresponse.Segments;
import tv.blackarrow.cpp.model.mccresponse.Tag;

/**
 * @author shwetanks
 *
 */
public class MCCTemplateCompiledConfiguration {

	private static final Logger LOGGER = LogManager.getLogger(MCCTemplateCompiledConfiguration.class);
	private static volatile MCCTemplateCompiledConfiguration instance = new MCCTemplateCompiledConfiguration();
	public static final Pattern MACRO_VALUE_PATTERN = Pattern.compile("[macroValue]", Pattern.LITERAL);
	
	private Map<String, MCCTemplateMacro> textReplacementKeyVsMacroUsedInValue = new HashMap<String, MCCTemplateMacro>();
	private Map<String, String> textReplacementKeyVsValueWithPlaceholder = new HashMap<String, String>();
	private Map<String, Pattern> textReplacementKeyVsCompiledPattern = new HashMap<String, Pattern>();
	private Map<HLSTemplateType, HLSTemplateVO> hlsTemplateTypeVsTemplate = new HashMap<HLSTemplateType, HLSTemplateVO>();
	private Map<DASHTemplateType, DASHTemplateVO> dashTemplateTypeVsTemplate = new HashMap<DASHTemplateType, DASHTemplateVO>();

	private MCCTemplateCompiledConfiguration() {
	}

	public static MCCTemplateCompiledConfiguration getInstance() {
		return instance;
	}

	public void compileMCCTemplate(MccTemplates templates){

		LOGGER.debug(()->"Start Compilation mcc configuration.");
		//1. Load the replacements and corresponding macros for all the configured keys.
		Map<String, MCCTemplateMacro> textReplacementKeyVsMacroUsedInValue = new HashMap<String, MCCTemplateMacro>();
		Map<String, String> textReplacementKeyVsValueWithPlaceholder = new HashMap<String, String>();
		Map<String, Pattern> textReplacementKeyVsCompiledPattern = new HashMap<String, Pattern>();
		Replacements replacements = templates.getReplacements();
		for(Replace replace : replacements.getReplace()){
			textReplacementKeyVsMacroUsedInValue.put(replace.getKey(), MCCTemplateMacro.fromString(replace.getMacro()));
			textReplacementKeyVsValueWithPlaceholder.put(replace.getKey(), replace.getValue());
			textReplacementKeyVsCompiledPattern.put(replace.getKey(), Pattern.compile(replace.getKey(), Pattern.LITERAL));
		}

		// 2. Fetch the configured HLS templates.
		Map<HLSTemplateType, HLSTemplateVO> hlsTemplateTypeVsTemplate = new HashMap<HLSTemplateType, HLSTemplateVO>();
		List<HLSTemplate> hlsTemplates = templates.getHlsTemplate();
		for(HLSTemplate hlsTemplate : hlsTemplates){
			HLSTemplateVO hlsTemplateVO = new HLSTemplateVO();
			Segments confirmationSegments = hlsTemplate.getConfirmationSegments();
			Segments abortSegments = hlsTemplate.getAbortSegments();
			HLSTemplateSegmentsVO confirmationSegmentsVO = constructSegements(confirmationSegments);
			HLSTemplateSegmentsVO abortSegmentsVO = constructSegements(abortSegments);
			hlsTemplateVO.setConfirmationSegments(confirmationSegmentsVO);
			hlsTemplateVO.setAbortSegments(abortSegmentsVO);  
			hlsTemplateTypeVsTemplate.put(HLSTemplateType.fromValue(hlsTemplate.getType().value()), hlsTemplateVO);
		}

		// 3. Fetch the configured DASH templates.
		Map<DASHTemplateType, DASHTemplateVO> dashTemplateTypeVsTemplate = new HashMap<DASHTemplateType, DASHTemplateVO>();
		List<DASHTemplate> dashTemplates = templates.getDashTemplate();
		for(DASHTemplate dashTemplate : dashTemplates){
			String templateText = dashTemplate.getTemplateResponse().getValue();
			DASHTemplateVO dashTemplateVO = new DASHTemplateVO(templateText == null ? "" : templateText, dashTemplate.getTemplateResponse().getTemplateType());
			dashTemplateTypeVsTemplate.put(DASHTemplateType.fromValue(dashTemplate.getType().value()), dashTemplateVO);
		}
		
		// 4. Populate new configuration object with the latest configurations received.
		MCCTemplateCompiledConfiguration newInstance = new MCCTemplateCompiledConfiguration();
		newInstance.textReplacementKeyVsMacroUsedInValue = textReplacementKeyVsMacroUsedInValue;
		newInstance.textReplacementKeyVsValueWithPlaceholder = textReplacementKeyVsValueWithPlaceholder;
		newInstance.textReplacementKeyVsCompiledPattern = textReplacementKeyVsCompiledPattern;
		newInstance.hlsTemplateTypeVsTemplate = hlsTemplateTypeVsTemplate;
		newInstance.dashTemplateTypeVsTemplate = dashTemplateTypeVsTemplate;
		
		// 5. Replace current configuration with the latest configuration.
		instance = newInstance;
		
		LOGGER.debug(()->"End Compilation configuration.");
	}

	private static HLSTemplateSegmentsVO constructSegements(Segments segments) {
		HLSTemplateSegmentsVO innerSegments = new HLSTemplateSegmentsVO();
		if(segments != null){
			HLSTemplateSegmentVO firstSegment = new HLSTemplateSegmentVO();
			HLSTemplateSegmentVO spanSegment = new HLSTemplateSegmentVO();
			HLSTemplateSegmentVO lastSegment = new HLSTemplateSegmentVO();
			
			LOGGER.debug(()->"Start First segment Compilation.");
			if(segments.getFirstSegment() != null ){
				List<Tag> tags = segments.getFirstSegment().getTag();
				for (Tag tag : tags) {
					HLSTemplateSegmentTagVO custom = new HLSTemplateSegmentTagVO();
					Boolean adapt = tag.isAdapt();
					if(adapt != null){
						custom.setAdapt(adapt);
					}
					Locality locality = tag.getLocality();
					if(locality != null && locality.value() != null && !locality.value().isEmpty()){
						custom.setLocality(locality.value());
					}
					custom.setValueWithPlaceholders(tag.getValue());
					firstSegment.addTag(custom);
					innerSegments.setFirstSegment(firstSegment);
				}
				
			}
			LOGGER.debug(()->"End First segment Compilation.");
			LOGGER.debug(()->"Start Span segment Compilation.");
			if(segments.getSpanSegment() != null ){
				List<Tag> tags = segments.getSpanSegment().getTag();
				for (Tag tag : tags) {
					HLSTemplateSegmentTagVO custom = new HLSTemplateSegmentTagVO();
					Boolean adapt = tag.isAdapt();
					if(adapt != null){
						custom.setAdapt(adapt);
					}
					Locality locality = tag.getLocality();
					if(locality != null && locality.value() != null && !locality.value().isEmpty()){
						custom.setLocality(locality.value());
					}
					custom.setValueWithPlaceholders(tag.getValue());
					spanSegment.addTag(custom);
					innerSegments.setSpanSegment(spanSegment);
				}
				
			}
			LOGGER.debug(()->"End Span segment Compilation.");
			LOGGER.debug(()->"Start Last segment Compilation.");
			if(segments.getLastSegment() != null ){
				List<Tag> tags = segments.getLastSegment().getTag();
				for (Tag tag : tags) {
					HLSTemplateSegmentTagVO custom = new HLSTemplateSegmentTagVO();
					Boolean adapt = tag.isAdapt();
					if(adapt != null){
						custom.setAdapt(adapt);
					}
					Locality locality = tag.getLocality();
					if(locality != null && locality.value() != null && !locality.value().isEmpty()){
						custom.setLocality(locality.value());
					}
					custom.setValueWithPlaceholders(tag.getValue());
					lastSegment.addTag(custom);
					innerSegments.setLastSegment(lastSegment);
				}
			}
			LOGGER.debug(()->"End Last segment Compilation.");
		}
		return innerSegments;
	}
	
	public enum MCCTemplateMacro {

		SCTE35_SPLICE_COMMAND_TYPE(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SPLICE_COMMAND_TYPE),
		SCTE35_FEED_PROVIDER_ID_FEED_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID_FEED_ID),
		SCTE35_SEGMENTATION_EVENT_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_EVENT_ID),
		SCTE35_SEGMENTATION_TYPE_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_TYPE_ID),
		SCTE35_WEB_DELIVERY_ALLOWED_FLAG(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_WEB_DELIVERY_ALLOWED_FLAG),
		SCTE35_NO_REGIONAL_BLACKOUT_FLAG(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_NO_REGIONAL_BLACKOUT_FLAG),
		SCTE35_ARCHIVE_ALLOWED_FLAG(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_ARCHIVE_ALLOWED_FLAG),
		SCTE35_DEVICE_RESTRICTIONS(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_DEVICE_RESTRICTIONS),
		SCTE35_SEGMENTATION_DURATION(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_DURATION),
		SCTE35_SEGMENTATION_UPID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID),
		SCTE35_SEGMENTATION_UPID_TYPE(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_TYPE),
		SCTE35_SEGMENTATION_SIGNAL_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_SIGNAL_ID),
		SCTE35_FEED_FEED_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_FEED_ID),
		SCTE35_FEED_PROVIDER_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID),
		SCTE35_UTC_POINT(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_UTC_POINT),
		SCTE35_ACQUISITION_POINT_IDENTITY(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_ACQUISITION_POINT_IDENTITY),
		SCTE35_SEGMENTATION_NUM(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_NUM),
		SCTE35_SEGMENTATION_COUNT(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_COUNT),
		SCTE35_SEGMENTATION_UPID_PROGRAM_ID(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_PROGRAM_ID);

		private String macroValue;

		private MCCTemplateMacro(final String macroValue) {
			this.macroValue = macroValue;
		}

		public String getMacroValue() {
			return macroValue;
		}
		
		private static final Map<String, MCCTemplateMacro> allStaticMacros = getAllStaticMacros();
		private static final Map<String, MCCTemplateMacro> getAllStaticMacros(){
			Map<String, MCCTemplateMacro> allConstantMacros = new HashMap<String, MCCTemplateMacro>();
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SPLICE_COMMAND_TYPE,SCTE35_SPLICE_COMMAND_TYPE);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID_FEED_ID,SCTE35_FEED_PROVIDER_ID_FEED_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_EVENT_ID,SCTE35_SEGMENTATION_EVENT_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_TYPE_ID,SCTE35_SEGMENTATION_TYPE_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_WEB_DELIVERY_ALLOWED_FLAG,SCTE35_WEB_DELIVERY_ALLOWED_FLAG);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_NO_REGIONAL_BLACKOUT_FLAG,SCTE35_NO_REGIONAL_BLACKOUT_FLAG);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_ARCHIVE_ALLOWED_FLAG,SCTE35_ARCHIVE_ALLOWED_FLAG);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_DEVICE_RESTRICTIONS,SCTE35_DEVICE_RESTRICTIONS);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_DURATION,SCTE35_SEGMENTATION_DURATION);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID,SCTE35_SEGMENTATION_UPID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_TYPE,SCTE35_SEGMENTATION_UPID_TYPE);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_SIGNAL_ID,SCTE35_SEGMENTATION_SIGNAL_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_FEED_ID,SCTE35_FEED_FEED_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID,SCTE35_FEED_PROVIDER_ID);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_UTC_POINT,SCTE35_UTC_POINT);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_ACQUISITION_POINT_IDENTITY,SCTE35_ACQUISITION_POINT_IDENTITY);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_NUM,SCTE35_SEGMENTATION_NUM);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_COUNT,SCTE35_SEGMENTATION_COUNT);
			allConstantMacros.put(MCCTemplateConstants.MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_PROGRAM_ID,SCTE35_SEGMENTATION_UPID_PROGRAM_ID);


			return allConstantMacros;
		}
		public boolean is(String macroTemplate){
			return this.macroValue.equalsIgnoreCase(macroTemplate); 
		}
		
		public static MCCTemplateMacro fromString(String macroValue){
			MCCTemplateMacro macro = allStaticMacros.get(macroValue);
			if(macro!=null){
				return macro;
			}else if (MCCTemplateMacro.SCTE35_SPLICE_COMMAND_TYPE.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SPLICE_COMMAND_TYPE;
			}else if (MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID_FEED_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID_FEED_ID;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_EVENT_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_EVENT_ID;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_TYPE_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_TYPE_ID;
			}else if (MCCTemplateMacro.SCTE35_WEB_DELIVERY_ALLOWED_FLAG.is(macroValue)){
				return MCCTemplateMacro.SCTE35_WEB_DELIVERY_ALLOWED_FLAG;
			}else if (MCCTemplateMacro.SCTE35_NO_REGIONAL_BLACKOUT_FLAG.is(macroValue)){
				return MCCTemplateMacro.SCTE35_NO_REGIONAL_BLACKOUT_FLAG;
			}else if (MCCTemplateMacro.SCTE35_ARCHIVE_ALLOWED_FLAG.is(macroValue)){
				return MCCTemplateMacro.SCTE35_ARCHIVE_ALLOWED_FLAG;
			}else if (MCCTemplateMacro.SCTE35_DEVICE_RESTRICTIONS.is(macroValue)){
				return MCCTemplateMacro.SCTE35_DEVICE_RESTRICTIONS;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_DURATION.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_DURATION;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_UPID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_UPID;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_UPID_TYPE.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_UPID_TYPE;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_SIGNAL_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_SIGNAL_ID;
			}else if (MCCTemplateMacro.SCTE35_FEED_FEED_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_FEED_FEED_ID;
			}else if (MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_FEED_PROVIDER_ID;
			}else if (MCCTemplateMacro.SCTE35_UTC_POINT.is(macroValue)){
				return MCCTemplateMacro.SCTE35_UTC_POINT;
			}else if (MCCTemplateMacro.SCTE35_ACQUISITION_POINT_IDENTITY.is(macroValue)){
				return MCCTemplateMacro.SCTE35_ACQUISITION_POINT_IDENTITY;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_NUM.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_NUM;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_COUNT.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_COUNT;
			}else if (MCCTemplateMacro.SCTE35_SEGMENTATION_UPID_PROGRAM_ID.is(macroValue)){
				return MCCTemplateMacro.SCTE35_SEGMENTATION_UPID_PROGRAM_ID;
			}else {
				return null;
			}
		}
	}
	
	public  Map<String, String> getTextReplacementKeyVsValueWithPlaceholder() {
		return textReplacementKeyVsValueWithPlaceholder;
	}
	
	public  Map<String, MCCTemplateMacro> getTextReplacementKeyVsMacroUsedInValue() {
		return textReplacementKeyVsMacroUsedInValue;
	}
	
	public  Map<HLSTemplateType, HLSTemplateVO> getHLSTemplates() {
		return hlsTemplateTypeVsTemplate;
	}

	public Map<DASHTemplateType, DASHTemplateVO> getDashTemplateTypeVsTemplate() {
		return dashTemplateTypeVsTemplate;
	}

	public Map<String, Pattern> getTextReplacementKeyVsCompiledPattern() {
		return textReplacementKeyVsCompiledPattern;
	}
	
}
