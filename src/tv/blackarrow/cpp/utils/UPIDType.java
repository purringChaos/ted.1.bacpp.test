/**
 * 
 */
package tv.blackarrow.cpp.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author akumar
 *
 */
public enum UPIDType {
	CABLELAB_ADI(0x09),
	EIDR_COMPACT_BINARY(0x0A);
	
	private static final Logger LOGGER = LogManager.getLogger(UPIDType.class);
	private static final Map<Short,UPIDType> lookup = new HashMap<Short, UPIDType>();
	static {
		for(UPIDType upidType : UPIDType.values()){
		       lookup.put(upidType.getUPIDTypeId(), upidType);
		}
	}
	
	private final Integer upidTypeId;

	private UPIDType(Integer upidTypeId) {
		this.upidTypeId = upidTypeId;
	}
	
	public Short getUPIDTypeId(){
		return upidTypeId.shortValue();
	}
	
	public static UPIDType valueOf(Short upidTypeId){
		UPIDType upidType = lookup.get(upidTypeId);
		if(upidType==null){
			// throw new IllegalArgumentException("This segment type is not supported.");
			LOGGER.info("Unsupported upid type: " + upidType);
		}
		return upidType;
	}
}
