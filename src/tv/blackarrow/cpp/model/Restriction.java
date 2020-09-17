/**
 * 
 */
package tv.blackarrow.cpp.model;

import com.google.gson.annotations.Expose;

/**
 * @author asharma
 *
 */
public class Restriction {
    
	@Expose
    private String restrictionType;
	@Expose
    private String restrictionValue;
	@Expose
    private String altSourceValue;
    /**
     * @return the restrictionType
     */
    public String getRestrictionType() {
        return restrictionType;
    }
    /**
     * @param restrictionType the restrictionType to set
     */
    public void setRestrictionType(String restrictionType) {
        this.restrictionType = restrictionType;
    }
    /**
     * @return the restrictionValue
     */
    public String getRestrictionValue() {
        return restrictionValue;
    }
    /**
     * @param restrictionValue the restrictionValue to set
     */
    public void setRestrictionValue(String restrictionValue) {
        this.restrictionValue = restrictionValue;
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
