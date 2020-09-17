package tv.blackarrow.cpp.loader.po;

import java.util.regex.Pattern;


public enum POCSVColumn {

	ZoneExtRef 		("Zone External Ref",	true, 	1, 		80,	"[0-9a-zA-Z]{1,80}", "Alphanumeric, 80 characters or less"),
	WindowStartTime	("Window Start Time", 	true,	13,		15, "[0-9]{13,15}",		"numeric, 13-15 digits"),
	WindowDuration	("Window Duration",		true,	1,		11,	"[0-9]{1,11}",		"numeric, 1-11 digits"),
	BreakID			("Break ID",			true,	36,		36,	".{22,36}",		"hex GUID, 36 characters"),
	BreakDuration	("Break Duration",		true,	1,		11,	"[0-9]{1,11}",		"numeric, 1-11 digits"),
	OutSignalId		("Out Signal Id",		true,	24,		24,	".{22,36}",			"Base64 encoded string"),
	InSignalId		("In Signal Id",		true,	24,		24,	".{22,36}",			"Base64 codeded string"),
	BreakOrder		("Break Order",			true,	1,		3,	"[0-9]{1,3}",			"numeric, 3 digits or less");		
	
	private String name;
	private boolean required;
	private int minLength;
	private int maxLength;
	private Pattern pattern;
	private String patternDescription;
	
	private int columnIndex;
	
	private POCSVColumn(String name, boolean required, 
			int minLength, int maxLength, String regex, String patternDescription) {
		this.name = name;
		this.required = required;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.pattern = Pattern.compile(regex);
		this.patternDescription = patternDescription;
		this.columnIndex = -1;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public int getMinLength() {
		return minLength;
	}
	
	public int getMaxLength() {
		return maxLength;
	}
	
	public Pattern getPattern() {
		return pattern;
	}

	public String getPatternDescription() {
		return patternDescription;
	}
	
 	public int getColumnIndex() {
		return columnIndex;
	}
	
 	public static int getNumRequiredColumns() {
 		int totalNum = 0;
		POCSVColumn[] columnDefs = POCSVColumn.values();
		for(POCSVColumn columnDef : columnDefs) {
			if(columnDef.required) {
				totalNum ++;
			}
		}
		return totalNum;
 	}
 	
	public static POCSVColumn getColumnDef(int idx) {
		POCSVColumn[] columnDefs = POCSVColumn.values();
		for(POCSVColumn columnDef : columnDefs) {
			if(columnDef.columnIndex == idx) {
				return columnDef;
			}
		}
		return null;
	}
	
	public static boolean setColumnIndex(String[] names) {
		int idxSum = 0;
		int expectedSum = 0;
		for (int idx = 0; idx < names.length; idx++) {
			if (ZoneExtRef.getName().equals(names[idx])) {
				ZoneExtRef.columnIndex = idx;
				idxSum += idx;
			} else if (WindowStartTime.getName().equals(names[idx])) {
				WindowStartTime.columnIndex = idx;
				idxSum += idx;
			} else if (WindowDuration.getName().equals(names[idx])) {
				WindowDuration.columnIndex = idx;
				idxSum += idx;
			} else if (BreakID.getName().equals(names[idx])) {
				BreakID.columnIndex = idx;
				idxSum += idx;
			} else if (BreakDuration.getName().equals(names[idx])) {
				BreakDuration.columnIndex = idx;
				idxSum += idx;
			} else if (OutSignalId.getName().equals(names[idx])) {
				OutSignalId.columnIndex = idx;
				idxSum += idx;
			} else if (InSignalId.getName().equals(names[idx])) {
				InSignalId.columnIndex = idx;
				idxSum += idx;
			} else if (BreakOrder.getName().equals(names[idx])) {
				BreakOrder.columnIndex = idx;
				idxSum += idx;
			}
			expectedSum += idx;
		}
		return idxSum == expectedSum;
	}

}
