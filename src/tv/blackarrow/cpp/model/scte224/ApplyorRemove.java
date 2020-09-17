/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * @author Amit Kumar Sharma
 *
 */
public class ApplyorRemove {

	@Expose
	private Long durationInMillis = null;
	@Expose
	private List<String> serviceZones = new ArrayList<String>();
	@Expose
	private String altSourceValue = null;

	/**
	 * 
	 */
	public ApplyorRemove() {
		super();
	}

	/**
	 * @param durationInMillis
	 */
	public ApplyorRemove(Long durationInMillis) {
		this();
		this.durationInMillis = durationInMillis;
	}

	/**
	 * @return the durationInMillis
	 */
	public Long getDurationInMillis() {
		return durationInMillis;
	}

	/**
	 * @param durationInMillis the durationInMillis to set
	 */
	public void setDurationInMillis(Long durationInMillis) {
		this.durationInMillis = durationInMillis;
	}

	

	public List<String> getServiceZones() {
		return serviceZones;
	}

	public void setServiceZones(List<String> serviceZones) {
		this.serviceZones = serviceZones;
	}

	public String getAltSourceValue() {
		return altSourceValue;
	}

	public void setAltSourceValue(String altSourceValue) {
		this.altSourceValue = altSourceValue;
	}

	@Override
	public String toString() {
		return "Apply [durationInMillis=" + durationInMillis + ", serviceZones=" + serviceZones + ", altSourceValue=" + altSourceValue + "]";
	}

}
