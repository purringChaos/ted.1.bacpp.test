package tv.blackarrow.cpp.utils;

public enum BOEventUIorESNIAction {
	CREATE, UPDATE, DELETE, NONE;

	//	CREATE, UPDATE, STOP_NOW, DELETE, SIGNAL_ABORT, CONFIRMED, ERROR, COMPLETE;
	public static BOEventUIorESNIAction fromEventAction(EventAction action) {
		BOEventUIorESNIAction result = null;
		if (action == null) {
			result = NONE;
		}
		switch (action) {
		case CREATE:
			result = BOEventUIorESNIAction.CREATE;
			break;
		case UPDATE:
			result = BOEventUIorESNIAction.UPDATE;
			break;
		case STOP_NOW:
		case DELETE:
			result = BOEventUIorESNIAction.DELETE;
			break;
		default:
			result = BOEventUIorESNIAction.NONE;
			break;
		}
		return result;
	}
}
