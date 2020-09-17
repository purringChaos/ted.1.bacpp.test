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
public enum BlackoutConfirmationType {
	IN_BAND("In-Band"), OUT_OF_BAND("Out-of-Band"), QAM("QAM");

	private static final Logger LOGGER = LogManager.getLogger(BlackoutConfirmationType.class);
	private static final Map<String,BlackoutConfirmationType> lookup = new HashMap<String, BlackoutConfirmationType>();

	static {
	      for(BlackoutConfirmationType segmentType : BlackoutConfirmationType.values()){
	           lookup.put(segmentType.getConfirmationType(), segmentType);
	      }
	 }
	
	private String confirmationType;
	
	private BlackoutConfirmationType(final String confirmationType){
		this.confirmationType = confirmationType;
	}
	
	/**
	 * @return the confirmationType
	 */
	public String getConfirmationType() {
		return confirmationType;
	}

	public static BlackoutConfirmationType type(String confirmationType){
		BlackoutConfirmationType confirmType = lookup.get(confirmationType);
		if(confirmType==null){
			LOGGER.debug("Unsupported blackout confirmation type: " + confirmationType);
		}
		return confirmType;
	}
}
