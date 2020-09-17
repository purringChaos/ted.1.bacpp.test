/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.model.EventType;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.UpStreamNotificationMessageType;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class NotificationMessage {
	@Expose
	private String scheduledNotificationId = null;
	@Expose
	private String feedExtRef = null;
	@Expose
	private String eventId = null;
	@Expose
	private String eventSignalId = null;
	@Expose
	private String mediaPointSignalId = null;
	@Expose
	private String programEndMediaPointSignalId = null;
	@Expose
	private String streamId = null;
	@Expose
	private long streamSignalTimeOffset = -1;
	@Expose
	private String streamURL = null;
	@Expose
	private int notificationScheduledTime = -1;

	private String onFlyCreatedScheduledUpStreamMessage = null;
	@Expose
	private Schema schema = null;
	@Expose
	private UpStreamNotificationMessageType upStreamNotificationMessageType = null;
	@Expose
	private EventAction eventAction = null;
	@Expose
	private EventType eventType = null;
	@Expose
	private long eventSignalUTCStartTime = -1;//Just look for removing this later
	@Expose
	private long eventSignalUTCStopTime = -1;//Just look for removing this later
	@Expose
	private long signalTime = -1;
	////////////////For Message
	@Expose
	private long contentDuration = -1;
	@Expose
	private long currentTime = -1;
	@Expose
	private String eventAltSourceValue = null;
	@Expose
	private String zoneIdentity = null;
	@Expose
	private int aqContentFrequency = -1;
	@Expose
	private List<SegmentType> transactionSegmentType = null;
	//////////////////////////////
	@Expose
	private boolean isExecutedAtEncoderLevel = false;
	@Expose
	private Map<SegmentType, String> acquisitionSignalIds = new LinkedHashMap<>();
	
	private Boolean noRegionalBlackout;
	
	private int deviceRestrictions;
	
	private boolean programRunoverUnplanned;
	private boolean blackoutOverride;
	private int territoryUpdateCounter;
	
	//Backward compatibility, for 9.1 message used to be preloaded in Notification message, adding below holder to hold that. Later 
	//TODO 9.1 Backward compatibilty introduce below code. Please remove it once all Linear Customer move to 9.2.2 onwards. We should clean it.
	@Expose
	@Deprecated
	private String scheduledUpStreamMessage = null;
	

	public NotificationMessage(final String scheduledNotificationId, final String eventId, final String eventSignalId, final String streamId, final long streamSignalTimeOffset,
			final String streamURL, final int scheduledTime, final String scheduledUpStreamMessage, final EventAction eventAction, final EventType eventType,
			final long eventSignalUTCStartTime, final long eventSignalUTCStopTime, final boolean isExecutedAtEncoderLevel, final Map<SegmentType, String> acquisitionSignalIds,
			final UpStreamNotificationMessageType upStreamNotificationMessageType, final Schema schema, final String eventAltSourceValue, final String zoneIdentity) {
		super();
		this.setScheduledNotificationId(scheduledNotificationId);
		this.setEventId(eventId);
		this.setEventSignalId(eventSignalId);
		this.setStreamId(streamId);
		this.setStreamSignalTimeOffset(streamSignalTimeOffset);
		this.setStreamURL(streamURL);
		this.setNotificationScheduledTime(scheduledTime);
		this.setOnFlyCreatedScheduledUpStreamMessage(scheduledUpStreamMessage);
		this.setEventAction(eventAction);
		this.setEventType(eventType);
		this.setEventSignalUTCStartTime(eventSignalUTCStartTime);
		this.setEventSignalUTCStopTime(eventSignalUTCStopTime);
		this.setAcquisitionSignalIds(acquisitionSignalIds);
		this.setUpStreamNotificationMessageType(upStreamNotificationMessageType);
		this.setSchema(schema);
		this.eventAltSourceValue = eventAltSourceValue;
		this.zoneIdentity = zoneIdentity;
	}

	public NotificationMessage() {
		// TODO Auto-generated constructor stub
	}

	public String getFeedExtRef() {
		return feedExtRef;
	}

	public void setFeedExtRef(String feedExtRef) {
		this.feedExtRef = feedExtRef;
	}

	public String getScheduledNotificationId() {
		return scheduledNotificationId;
	}

	public void setScheduledNotificationId(String scheduledNotificationId) {
		if (scheduledNotificationId == null || scheduledNotificationId.trim().isEmpty()) {
			throw new IllegalArgumentException("'scheduledNotificationId' can not be null or empty. Please provide a valid value.");
		}
		this.scheduledNotificationId = scheduledNotificationId;
	}

	public String getStreamURL() {
		return streamURL;
	}

	public void setStreamURL(String streamURL) {
		if (streamURL == null || streamURL.trim().isEmpty()) {
			throw new IllegalArgumentException("'streamURL' can not be null or empty. Please provide a valid value.");
		}
		this.streamURL = streamURL;
	}

	/**
	 * @return the notificationScheduledTime
	 */
	public int getNotificationScheduledTime() {
		return notificationScheduledTime;
	}

	/**
	 * @param notificationScheduledTime the notificationScheduledTime to set
	 */
	public void setNotificationScheduledTime(int notificationScheduledTime) {
		if (notificationScheduledTime <= 0) {
			throw new IllegalArgumentException(
					"'notificationScheduledTime' can not be -ve or 0. Please provide a valid +ve value" + "that represent the time when this message needs to be notified.");
		}
		this.notificationScheduledTime = notificationScheduledTime;
	}

	public String getOnFlyCreatedScheduledUpStreamMessage() {
		return StringUtils.isNotBlank(onFlyCreatedScheduledUpStreamMessage) ? onFlyCreatedScheduledUpStreamMessage : "";
	}

	public void setOnFlyCreatedScheduledUpStreamMessage(String scheduledUpStreamMessage) {
		this.onFlyCreatedScheduledUpStreamMessage = scheduledUpStreamMessage;
	}

	public EventAction getEventAction() {
		return eventAction;
	}

	public void setEventAction(final EventAction eventAction) {
		if (eventAction == null) {
			throw new IllegalArgumentException("'eventAction' can not be null. Please provide a valid value.");
		}
		this.eventAction = eventAction;
	}

	/**
	 * @return the streamId
	 */
	public String getStreamId() {
		return streamId;
	}

	/**
	 * @param streamId the streamId to set
	 */
	public void setStreamId(String streamId) {
		if (streamId == null || streamId.trim().isEmpty()) {
			throw new IllegalArgumentException("'streamId' can not be null or empty. Please provide a valid value.");
		}
		this.streamId = streamId;
	}

	/**
	 * @return the eventId
	 */
	public String getEventSignalId() {
		return eventSignalId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventSignalId(String eventSignalId) {
		if (eventSignalId == null || eventSignalId.trim().isEmpty()) {
			throw new IllegalArgumentException("'eventSignalId' can not be null or empty. Please provide a valid value.");
		}
		this.eventSignalId = eventSignalId;
	}

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return eventType;
	}

	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(EventType eventType) {
		if (eventType == null) {
			throw new IllegalArgumentException("'eventType' can not be null. Please provide a valid value.");
		}
		this.eventType = eventType;
	}

	/**
	 * @return the eventSignalUTCStartTime
	 */
	public long getEventSignalUTCStartTime() {
		return eventSignalUTCStartTime;
	}

	/**
	 * @param eventSignalUTCStartTime the eventSignalUTCStartTime to set
	 */
	public void setEventSignalUTCStartTime(long eventSignalUTCStartTime) {
		if (eventSignalUTCStartTime <= 0) {
			throw new IllegalArgumentException(
					"'eventSignalUTCStartTime' can not be -ve or 0. Please provide a valid +ve value" + "that represent the time when this message needs to be notified.");
		}
		this.eventSignalUTCStartTime = eventSignalUTCStartTime;
	}

	/**
	 * @return the eventSignalUTCStopTime
	 */
	public long getEventSignalUTCStopTime() {
		return eventSignalUTCStopTime;
	}

	/**
	 * @param eventSignalUTCStopTime the eventSignalUTCStopTime to set
	 */
	public void setEventSignalUTCStopTime(long eventSignalUTCStopTime) {
		if (eventSignalUTCStopTime <= 0) {
			throw new IllegalArgumentException(
					"'eventSignalUTCStopTime' can not be -ve or 0. Please provide a valid +ve value" + "that represent the time when this message needs to be notified.");
		}
		this.eventSignalUTCStopTime = eventSignalUTCStopTime;
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
		if (acquisitionSignalIds == null || acquisitionSignalIds.isEmpty()) {
			throw new IllegalArgumentException("'acquisitionSignalIds' can not be null or empty. Please provide a valid value.");
		}
		this.acquisitionSignalIds.clear();
		this.acquisitionSignalIds.putAll(acquisitionSignalIds);
	}

	/**
	 * @return the isExecutedAtEncoderLevel
	 */
	public boolean isExecutedAtEncoderLevel() {
		return isExecutedAtEncoderLevel;
	}

	/**
	 * @param isExecutedAtEncoderLevel the isExecutedAtEncoderLevel to set
	 */
	public void setExecutedAtEncoderLevel(boolean isExecutedAtEncoderLevel) {
		this.isExecutedAtEncoderLevel = isExecutedAtEncoderLevel;
	}

	/**
	 * @return the streamSignalTimeOffset
	 * SCTE-224(CS2-387) : The Stream/ Acquisition point offset should be added only in the PTS streamTime in binary/xml. It should not be used for any other purpose.
	 * I02 layer : This has not been cleaned.
	 */
	public long getStreamSignalTimeOffset() {
		return streamSignalTimeOffset;
	}

	/**
	 * @param streamSignalTimeOffset the streamSignalTimeOffset to set
	 */
	public void setStreamSignalTimeOffset(long streamSignalTimeOffset) {
		this.streamSignalTimeOffset = streamSignalTimeOffset;
	}

	/**
	 * @return the eventId
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * @param eventId the eventId to set
	 */
	public void setEventId(String eventId) {
		if (eventId == null || eventId.trim().isEmpty()) {
			throw new IllegalArgumentException("'eventId' can not be null or empty. Please provide a valid value.");
		}
		this.eventId = eventId;
	}

	public UpStreamNotificationMessageType getUpStreamNotificationMessageType() {
		return upStreamNotificationMessageType;
	}

	public void setUpStreamNotificationMessageType(UpStreamNotificationMessageType upStreamnotificationMessageType) {
		this.upStreamNotificationMessageType = upStreamnotificationMessageType;
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}	

	public String getEventAltSourceValue() {
		return eventAltSourceValue;
	}

	public void setEventAltSourceValue(String eventAltSourceValue) {
		this.eventAltSourceValue = eventAltSourceValue;
	}

	public String getZoneIdentity() {
		return zoneIdentity;
	}

	public void setZoneIdentity(String zoneIdentity) {
		this.zoneIdentity = zoneIdentity;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public long getContentDuration() {
		return contentDuration > 0 ? contentDuration : 0;
	}

	public void setContentDuration(long contentDuration) {
		this.contentDuration = contentDuration;
	}

	public int getAqContentFrequency() {
		return aqContentFrequency;
	}

	public void setAqContentFrequency(int aqContentFrequency) {
		this.aqContentFrequency = aqContentFrequency;
	}

	
	/**
	 * @return the transactionSegmentType
	 */
	public List<SegmentType> getTransactionSegmentType() {
		return transactionSegmentType;
	}

	/**
	 * @param transactionSegmentType the transactionSegmentType to set
	 */
	public void setTransactionSegmentType(List<SegmentType> transactionSegmentType) {
		this.transactionSegmentType = transactionSegmentType;
	}

	@Deprecated
	public String getScheduledUpStreamMessage() {
		return scheduledUpStreamMessage;
	}

	@Deprecated
	public void setScheduledUpStreamMessage(String scheduledUpStreamMessage) {
		this.scheduledUpStreamMessage = scheduledUpStreamMessage;
	}

	

	@Override
	public String toString() {
		return "NotificationMessage [scheduledNotificationId=" + scheduledNotificationId + ", feedExtRef=" + feedExtRef
				+ ", eventId=" + eventId + ", eventSignalId=" + eventSignalId + ", mediaPointSignalId="
				+ mediaPointSignalId + ", programEndMediaPointSignalId=" + programEndMediaPointSignalId + ", streamId="
				+ streamId + ", streamSignalTimeOffset=" + streamSignalTimeOffset + ", streamURL=" + streamURL
				+ ", notificationScheduledTime=" + notificationScheduledTime + ", onFlyCreatedScheduledUpStreamMessage="
				+ onFlyCreatedScheduledUpStreamMessage + ", schema=" + schema + ", upStreamNotificationMessageType="
				+ upStreamNotificationMessageType + ", eventAction=" + eventAction + ", eventType=" + eventType
				+ ", eventSignalUTCStartTime=" + eventSignalUTCStartTime + ", eventSignalUTCStopTime="
				+ eventSignalUTCStopTime + ", signalTime=" + signalTime + ", contentDuration=" + contentDuration + ", currentTime=" + currentTime
				+ ", eventAltSourceValue=" + eventAltSourceValue + ", zoneIdentity=" + zoneIdentity
				+ ", aqContentFrequency=" + aqContentFrequency + ", transactionSegmentType=" + transactionSegmentType
				+ ", isExecutedAtEncoderLevel=" + isExecutedAtEncoderLevel + ", acquisitionSignalIds="
				+ acquisitionSignalIds + ", noRegionalBlackout=" + noRegionalBlackout + ", deviceRestrictions="
				+ deviceRestrictions + ", programRunoverUnplanned=" + isProgramRunoverUnplanned() + ", blacoutOverride="
				+ isBlackoutOverride() + ", scheduledUpStreamMessage=" + scheduledUpStreamMessage + "]";
	}

	public String getMediaPointSignalId() {
		return mediaPointSignalId;
	}

	public void setMediaPointSignalId(String mediaPointSignalId) {
		this.mediaPointSignalId = mediaPointSignalId;
	}

	public long getSignalTime() {
		return signalTime;
	}

	public void setSignalTime(long signalTime) {
		this.signalTime = signalTime;
	}

	public String getProgramEndMediaPointSignalId() {
		return programEndMediaPointSignalId;
	}

	public void setProgramEndMediaPointSignalId(String programEndMediaPointSignalId) {
		this.programEndMediaPointSignalId = programEndMediaPointSignalId;
	}

	public Boolean getNoRegionalBlackout() {
		return noRegionalBlackout;
	}

	public void setNoRegionalBlackout(Boolean noRegionalBlackout) {
		this.noRegionalBlackout = noRegionalBlackout;
	}

	public int getDeviceRestrictions() {
		return deviceRestrictions;
	}

	public void setDeviceRestrictions(int deviceRestrictions) {
		this.deviceRestrictions = deviceRestrictions;
	}

	/**
	 * @return the territoryUpdateCounter
	 */
	public int getTerritoryUpdateCounter() {
		return territoryUpdateCounter;
	}

	/**
	 * @param territoryUpdateCounter the territoryUpdateCounter to set
	 */
	public void setTerritoryUpdateCounter(int territoryUpdateCounter) {
		this.territoryUpdateCounter = territoryUpdateCounter;
	}

	public boolean isProgramRunoverUnplanned() {
		return programRunoverUnplanned;
	}

	public void setProgramRunoverUnplanned(boolean programRunoverUnplanned) {
		this.programRunoverUnplanned = programRunoverUnplanned;
	}

	public boolean isBlackoutOverride() {
		return blackoutOverride;
	}

	public void setBlackoutOverride(boolean blackoutOverride) {
		this.blackoutOverride = blackoutOverride;
	}

	
	
}
