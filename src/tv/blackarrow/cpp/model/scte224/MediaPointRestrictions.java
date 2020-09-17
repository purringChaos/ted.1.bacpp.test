/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

/**
 * @author Amit Kumar Sharma
 *
 */
public class MediaPointRestrictions {
	
	@Expose
	Map<String, List<String>> restrictions = new HashMap<>();
	@Expose
	private String altSourceValue;

	public void addRestrictions(String feedMediapointZoneRestrictionKey, List<String> zones) {
		if(zones==null) {
			restrictions.computeIfPresent(feedMediapointZoneRestrictionKey, (k,v) -> null);
		} else {
			restrictions.computeIfAbsent(feedMediapointZoneRestrictionKey, k -> new ArrayList<String>()).addAll(zones);	
		}
	}

	/**
	 * @return the restrictions
	 */
	public Map<String, List<String>> getRestrictions() {
		return restrictions;
	}

	/**
	 * @param restrictions the restrictions to set
	 */
	public void setRestrictions(Map<String, List<String>> restrictions) {
		this.restrictions = restrictions;
	}

	/**
	 * @return the altSourceValue
	 */
	public String getAltSourceValue() {
		return altSourceValue;
	}

	/**
	 * @param altSourceValue the altSourceValue to set
	 */
	public void setAltSourceValue(String altSourceValue) {
		this.altSourceValue = altSourceValue;
	}
	
}
