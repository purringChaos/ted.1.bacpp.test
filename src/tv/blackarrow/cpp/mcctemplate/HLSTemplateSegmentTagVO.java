package tv.blackarrow.cpp.mcctemplate;


public class HLSTemplateSegmentTagVO {
	private String locality;
	private Boolean adapt;
	private String valueWithPlaceholders;
	
	public HLSTemplateSegmentTagVO(String locality, Boolean adapt, String valueWithPlaceholders) {
		super();
		this.locality = locality;
		this.adapt = adapt;
		this.valueWithPlaceholders = valueWithPlaceholders;
	}

	public HLSTemplateSegmentTagVO() {
		// TODO Auto-generated constructor stub
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getValueWithPlaceholders() {
		return valueWithPlaceholders;
	}

	public void setValueWithPlaceholders(String valueWithPlaceholders) {
		this.valueWithPlaceholders = valueWithPlaceholders == null ? "" : valueWithPlaceholders;
	}

	public Boolean getAdapt() {
		return adapt;
	}

	public void setAdapt(Boolean adapt) {
		this.adapt = adapt;
	}
	
}
