/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel;

import java.util.Collections;
import java.util.Map;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class OutOfBandNotificationInfo {
	
	private SegmentType transactionSegmentType = null;
	private XMLGregorianCalendar programStartUTCWithAddedDelta = null;
	private XMLGregorianCalendar contentIdUTCWithAddedDelta = null;
	private XMLGregorianCalendar contentIDScheduleStartUTCWithAddedDelta = null;
	private Duration contentIdScheduleInterval = null;
	private Duration contentDuration = null;
	private Long contentDurationInMS = null;
	private XMLGregorianCalendar contentIDScheduleStopUTCWithAddedDelta = null;
	private String mediaSignalId = null;
	private String acquisitionPointId = null;
	private Long currentSystemTime = null;
	private Map<SegmentType, String> acquisitionSignalIds = Collections.emptyMap();
	private int noRegionalBlackout;
	private int deviceRestrictions;
	private SegmentType segmentType = null;
	
	/**
	 * @return the transactionSegmentType
	 */
	public SegmentType getTransactionSegmentType() {
		return transactionSegmentType;
	}
	/**
	 * @param transactionSegmentType the transactionSegmentType to set
	 */
	public void setTransactionSegmentType(SegmentType transactionSegmentType) {
		this.transactionSegmentType = transactionSegmentType;
	}
	/**
	 * @return the programStartUTCWithDelta
	 */
	public XMLGregorianCalendar getProgramStartUTCWithAddedDelta() {
		return programStartUTCWithAddedDelta;
	}
	/**
	 * @param programStartUTCWithDelta the programStartUTCWithDelta to set
	 */
	public void setProgramStartUTCWithAddedDelta(XMLGregorianCalendar programStartUTCWithAddedDelta) {
		this.programStartUTCWithAddedDelta = programStartUTCWithAddedDelta;
	}
	/**
	 * @return the contentIDUTCWithDelta
	 */
	public XMLGregorianCalendar getContentIdUTCWithAddedDelta() {
		return contentIdUTCWithAddedDelta;
	}
	/**
	 * @param contentIDUTCWithDelta the contentIDUTCWithDelta to set
	 */
	public void setContentIdUTCWithAddedDelta(XMLGregorianCalendar contentIdUTCWithAddedDelta) {
		this.contentIdUTCWithAddedDelta = contentIdUTCWithAddedDelta;
	}
	/**
	 * @return the contentIDScheduleStartUTCWithDelta
	 */
	public XMLGregorianCalendar getContentIDScheduleStartUTCWithAddedDelta() {
		return contentIDScheduleStartUTCWithAddedDelta;
	}
	/**
	 * @param contentIDScheduleStartUTCWithDelta the contentIDScheduleStartUTCWithDelta to set
	 */
	public void setContentIDScheduleStartUTCWithAddedDelta(XMLGregorianCalendar contentIDScheduleStartUTCWithAddedDelta) {
		this.contentIDScheduleStartUTCWithAddedDelta = contentIDScheduleStartUTCWithAddedDelta;
	}
	/**
	 * @return the contentIdScheduleInterval
	 */
	public Duration getContentIdScheduleInterval() {
		return contentIdScheduleInterval;
	}
	/**
	 * @param contentIdScheduleInterval the contentIdScheduleInterval to set
	 */
	public void setContentIdScheduleInterval(Duration contentIdScheduleInterval) {
		this.contentIdScheduleInterval = contentIdScheduleInterval;
	}
	/**
	 * @return the contentDuration
	 */
	public Duration getContentDuration() {
		return contentDuration;
	}
	/**
	 * @param contentDuration the contentDuration to set
	 */
	public void setContentDuration(Duration contentDuration) {
		this.contentDuration = contentDuration;
	}
	/**
	 * @return the contentDurationInMS
	 */
	public Long getContentDurationInMS() {
		return contentDurationInMS;
	}
	/**
	 * @param contentDurationInMS the contentDurationInMS to set
	 */
	public void setContentDurationInMS(Long contentDurationInMS) {
		this.contentDurationInMS = contentDurationInMS;
	}
	/**
	 * @return the contentIDScheduleStopUTCWithDelta
	 */
	public XMLGregorianCalendar getContentIDScheduleStopUTCWithAddedDelta() {
		return contentIDScheduleStopUTCWithAddedDelta;
	}
	/**
	 * @param contentIDScheduleStopUTCWithDelta the contentIDScheduleStopUTCWithDelta to set
	 */
	public void setContentIDScheduleStopUTCWithAddedDelta(XMLGregorianCalendar contentIDScheduleStopUTCWithAddedDelta) {
		this.contentIDScheduleStopUTCWithAddedDelta = contentIDScheduleStopUTCWithAddedDelta;
	}
	/**
	 * @return the mediaSignalId
	 */
	public String getMediaSignalId() {
		return mediaSignalId;
	}
	/**
	 * @param mediaSignalId the mediaSignalId to set
	 */
	public void setMediaSignalId(String mediaSignalId) {
		this.mediaSignalId = mediaSignalId;
	}
	/**
	 * @return the acquisitionPointId
	 */
	public String getAcquisitionPointId() {
		return acquisitionPointId;
	}
	/**
	 * @param acquisitionPointId the acquisitionPointId to set
	 */
	public void setAcquisitionPointId(String acquisitionPointId) {
		this.acquisitionPointId = acquisitionPointId;
	}
	/**
	 * @return the currentSystemTime
	 */
	public Long getCurrentSystemTime() {
		return currentSystemTime;
	}
	/**
	 * @param currentSystemTime the currentSystemTime to set
	 */
	public void setCurrentSystemTime(Long currentSystemTime) {
		this.currentSystemTime = currentSystemTime;
	}
	/**
	 * @return the acquisitionSignalIds
	 */
	public Map<SegmentType, String> getAcquisitionSignalIds() {
		return acquisitionSignalIds;
	}
	/**
	 * @param acquisitionSignalIds the acquisitionSignalIds to set
	 */
	
	public void setAcquisitionSignalIds(Map<SegmentType, String> acquisitionSignalIds) {
		this.acquisitionSignalIds = acquisitionSignalIds;
	}

	public int getNoRegionalBlackout() {
		return noRegionalBlackout;
	}

	public void setNoRegionalBlackout(int noRegionalBlackout) {
		this.noRegionalBlackout = noRegionalBlackout;
	}

	public int getDeviceRestrictions() {
		return deviceRestrictions;
	}

	public void setDeviceRestrictions(int deviceRestrictions) {
		this.deviceRestrictions = deviceRestrictions;
	}
	public SegmentType getSegmentType() {
		return segmentType;
	}
	public void setSegmentType(SegmentType segmentType) {
		this.segmentType = segmentType;
	}

}
