package tv.blackarrow.cpp.loader.po;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class POValidator {
	
	public POValidationResult validate(String[] data) {
		POValidationResult result = new POValidationResult();
		for(int i=0; i<data.length; i++) {
			validateSingleField(data[i], POCSVColumn.getColumnDef(i), result);
		}
		return result;
	}
	
	private void validateSingleField(String value, POCSVColumn columnDef, 
			POValidationResult result) {
		String fieldName = columnDef.getName();
		if (StringUtils.isEmpty(value)) {
			if(columnDef.isRequired()) {
				result.addFieldMissing(fieldName);
			}
			return;
		}
		if( columnDef.getMaxLength() > 0 && value.length() > columnDef.getMaxLength() ) {
			result.addMaximumLenghExceeded(fieldName, columnDef.getMaxLength());
		}
		if( columnDef.getPattern() != null ) {
			Pattern pattern = columnDef.getPattern();
			Matcher matcher = pattern.matcher(value);
			if(!matcher.matches()) {
				result.addPatternViolation(fieldName, columnDef.getPatternDescription());
			}
		}
	}	

}
