package tv.blackarrow.cpp.loader.po;

import java.util.ArrayList;
import java.util.List;

public class POValidationResult {

	
	private List<String> errorMsgList = new ArrayList<String>();
	private List<String> warningMsgList = new ArrayList<String>();
	
	public int getFatalErrors() {
		return errorMsgList.size();
	}
	
	public int getWarnings() {
		return warningMsgList.size();
	}
	
	private void addError(String msg) {
		errorMsgList.add(msg);
	}
	
	private void addWarning(String msg) {
		warningMsgList.add(msg);
	}
	
	public void addFieldMissing(String fieldName) {
		addError(fieldName + " is required.");
	}
		
	public void addMaximumLenghExceeded(String fieldName, int maxLength) {
		addError(fieldName + " should not exceed " + maxLength + " characters.");
	}

	public void addPatternViolation(String fieldName, String pattern) {
		addError(fieldName + " does not conform to the required pattern: " + pattern);
	}
		
	public String presentErrorMsgs() {
		StringBuffer result = new StringBuffer("\n\t"); 
		for(String s : errorMsgList) {
			result.append("\t" + s + "\n");
		}
		return result.toString();
	}
	
}
