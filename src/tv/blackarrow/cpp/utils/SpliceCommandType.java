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
public enum SpliceCommandType {
	//These values are taken from SCTE 35 2013
	SPLICE_NULL(0x00),SPLICE_SCHEDULE(0x04),SPLICE_INSERT(0x05),TIME_SIGNAL(0x06),BANDWIDTH_RESERVATION(0x07);
	
	private static final Logger LOGGER = LogManager.getLogger(SpliceCommandType.class);
	private static final Map<Long,SpliceCommandType> lookup = new HashMap<Long, SpliceCommandType>();

	static {
	      for(SpliceCommandType spliceCommandType : SpliceCommandType.values()){
	           lookup.put(spliceCommandType.getCommandtype(), spliceCommandType);
	      }
	 }
	
	private final long commandtype;
	
	private SpliceCommandType(final long commandtype){
		this.commandtype = commandtype;
	}
	
	public long getCommandtype() {
		return commandtype;
	}

	public static SpliceCommandType valueOf(long commandtype){
		SpliceCommandType spliceCommandType = lookup.get(commandtype);
		if(spliceCommandType==null){
			//throw new IllegalArgumentException("This command type is not supported.");
			LOGGER.info("Unsupported splice command type: " + commandtype);
		}
		return spliceCommandType;
	}
}
