/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.model.scte224.asserts.Assert;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class MatchSignal {

	@Expose
	private Long signalToleranceDurationInMS = 0l;
	@Expose
	private MatchType match = null;
	@Expose
	private List<String> asserts = new LinkedList<String>();
	@Expose
	private Integer segmentationUPIDType = null;
	@Expose
	private String segmentationUPID = null;
	@Expose
	List<Short> segmentationTypeIds = new LinkedList<Short>();
	@Expose
	List<Assert>  parsedAsserts = new ArrayList<Assert>();

	/**
	 * 
	 */
	public MatchSignal() {
		super();
	}

	/**
	 * @param signalToleranceInMS
	 * @param match
	 * @param asserts
	 * @param segmentationUPIDType
	 * @param segmentationUPID
	 * @param segmentationTypeIds
	 */
	public MatchSignal(Long signalToleranceDurationInMS, MatchType match, List<String> asserts, Integer segmentationUPIDType, String segmentationUPID,
			List<Short> segmentationTypeIds) {
		this();
		this.signalToleranceDurationInMS = signalToleranceDurationInMS;
		this.match = match;
		this.asserts = asserts;
		this.segmentationUPIDType = segmentationUPIDType;
		this.segmentationUPID = segmentationUPID;
		this.segmentationTypeIds = segmentationTypeIds;
	}

	/**
	 * @return the signalToleranceDurationInMS
	 */
	public Long getSignalToleranceDurationInMS() {
		return signalToleranceDurationInMS;
	}

	/**
	 * @param signalToleranceDurationInMS the signalToleranceDurationInMS to set
	 */
	public void setSignalToleranceDurationInMS(Long signalToleranceDurationInMS) {
		this.signalToleranceDurationInMS = signalToleranceDurationInMS;
	}

	/**
	 * @return the match
	 */
	public MatchType getMatch() {
		return match;
	}

	/**
	 * @param match the match to set
	 */
	public void setMatch(MatchType match) {
		this.match = match;
	}

	/**
	 * @return the asserts
	 */
	public List<String> getAsserts() {
		return asserts;
	}

	/**
	 * @param asserts the asserts to set
	 */
	public void setAsserts(List<String> asserts) {
		this.asserts = asserts;
	}

	/**
	 * @return the segmentationUPIDType
	 */
	public Integer getSegmentationUPIDType() {
		return segmentationUPIDType;
	}

	/**
	 * @param segmentationUPIDType the segmentationUPIDType to set
	 */
	public void setSegmentationUPIDType(Integer segmentationUPIDType) {
		this.segmentationUPIDType = segmentationUPIDType;
	}

	/**
	 * @return the segmentationUPID
	 */
	public String getSegmentationUPID() {
		return segmentationUPID;
	}

	/**
	 * @param segmentationUPID the segmentationUPID to set
	 */
	public void setSegmentationUPID(String segmentationUPID) {
		this.segmentationUPID = segmentationUPID;
	}

	/**
	 * @return the segmentationTypeIds
	 */
	public List<Short> getSegmentationTypeIds() {
		return segmentationTypeIds;
	}

	/**
	 * @param segmentationTypeIds the segmentationTypeIds to set
	 */
	public void setSegmentationTypeIds(List<Short> segmentationTypeIds) {
		this.segmentationTypeIds = segmentationTypeIds;
	}
	

	/**
	 * @return the parsedAsserts
	 */
	public List<Assert> getParsedAsserts() {
		return parsedAsserts;
	}

	/**
	 * @param parsedAssert the parsedAsserts to set
	 */
	public void setParsedAsserts(List<Assert> parsedAsserts) {
		this.parsedAsserts = parsedAsserts;
	}
	
	public boolean matches(SegmentationDescriptorType segmentationDescriptorType) {
		if(parsedAsserts!=null) {
			switch(match) {
			case ALL:
				for(Assert assertToValidate: parsedAsserts) {
					//If at-least one assert fails, matches returns false as all assert should match in this case.
					if(!assertToValidate.isTrue(segmentationDescriptorType)) {
						return false;
					}
				}
				return true;
			case ANY:
				for(Assert assertToValidate: parsedAsserts) {
					//If at-least one assert passes, matches returns true as any assert should match in this case.
					if(assertToValidate.isTrue(segmentationDescriptorType)) {
						return true;
					}
				}
				return false;
			case NONE:
				for(Assert assertToValidate: parsedAsserts) {
					//If at-least one assert passes, matches returns false as no assert should match in this case.
					if(assertToValidate.isTrue(segmentationDescriptorType)) {
						return false;
					}
				}
				return true;			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("MatchSignal [signalToleranceInMS=%s, match=%s, asserts=%s, segmentationUPIDType=%s, segmentationUPID=%s, segmentationTypeIds=%s, parsedAsserts=%s]",
				signalToleranceDurationInMS, match, asserts, segmentationUPIDType, segmentationUPID, segmentationTypeIds, parsedAsserts);  
	}

}
