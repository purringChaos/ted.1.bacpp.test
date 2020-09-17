package tv.blackarrow.cpp.components.util;

public class ContextConstants {
	//Constants needed to pass through various ess components
	public static final String SIGNAL_RESPONSE = "signal_response";
	public static final String ACQUISITION_ATTRIBUTES = "acquisition_attributes";
	public static final String ACQUISITION_TIMES = "acquisition_times";
	public static final String TRUE_STR = "true";
	public static final String SEGMENT_TYPE_ID = "segmentTypeId";
	public static final String ACQUISITION_POINT = "acquisition_point";
	public static final String CONFIRMED_EVENT_SIGNAL_ID = "confirmedEventSignalId";
	public static final String ACQUISITION_SIGNAL_ID = "acquisitionSignalId";
	public static final String PTS_ADJUSTMENTS = "ptsAdjustments";
	public static final String PTS_TIMES = "ptsTimes";
	public static final String RESPONSE = "response";
	public static final String SCHEMA = "schema";
	public static final String NOTIFICATION_EVENT = "notification_event";

	public static final String I03_MODEL_DELTA = "i03ResponseModelDelta";
	public static final String NONE_SIGNAL_POINT_ID = "NONE_SIGNAL_POINT_ID";

	//XML attributes based constants
	public static final String ACTION_NAME_CREATE = "create";
	public static final String ACTION_NAME_REPLACE = "replace";
	public static final String SUCCESS_STATUS_CODE = "0";

	public enum ESSRequestType {
		SCC, MCC;
	}
}
