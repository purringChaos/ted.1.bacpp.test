/**
 * 
 */
package tv.blackarrow.cpp.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author amit
 *
 */
public enum ResponseSignalAction {
	
	CREATE(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_CREATE), REPLACE(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_REPLACE), 
	NOOP(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP), DELETE(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);

	private static final Logger LOGGER = LogManager.getLogger(ResponseSignalAction.class);
	private static final Map<String,ResponseSignalAction> lookup = new HashMap<String, ResponseSignalAction>();

	static {
	      for(ResponseSignalAction responseSignalAction : ResponseSignalAction.values()){
	           lookup.put(responseSignalAction.toString().toUpperCase(), responseSignalAction);
	      }
	 }
	
	private String action;
	
	private ResponseSignalAction(final String action){
		this.action = action;
	}
	
	@Override
	public String toString() {
		return this.action;
	}

	public static ResponseSignalAction getAction(String action){
		if(action==null) {
			return null;
		}
		ResponseSignalAction responseSignalAction = lookup.get(action.toUpperCase());
		if(responseSignalAction==null){
			// throw new IllegalArgumentException("This action type is not supported.");
			LOGGER.info("Unsupported action value for response signal action: " + action);
		}
		return responseSignalAction;
	}
}
