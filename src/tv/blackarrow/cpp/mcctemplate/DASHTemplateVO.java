package tv.blackarrow.cpp.mcctemplate;

import java.util.regex.Pattern;

/**
 * 
 * @author Amit Kumar Sharma
 *
 */
public class DASHTemplateVO {
	
	private Pattern pattern;
	private String templateType;
	private String templateValue;

	/**
	 * @param pattern
	 */
	public DASHTemplateVO(String tagContent, String templateType) {
		super();
		this.setPattern(tagContent);
		this.setTemplateType(templateType);
	}

	/**
	 * @param pattern the pattern to set
	 */
	private void setPattern(String tagContent) {
		this.pattern = Pattern.compile(tagContent, Pattern.LITERAL);;
	}

	/**
	 * @param templateType the templateType to set
	 */
	private void setTemplateType(String templateType) {
		this.templateType = templateType;
	}
	
	/**
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}
	
	/**
	 * @return the templateType
	 */
	public String getTemplateType() {
		return templateType;
	}

	/**
	 * @return the templateValue
	 */
	public String getTemplateValue() {
		return templateValue;
	}

	/**
	 * @param templateValue the templateValue to set
	 */
	public void setTemplateValue(String templateValue) {
		this.templateValue = templateValue;
	}

}
