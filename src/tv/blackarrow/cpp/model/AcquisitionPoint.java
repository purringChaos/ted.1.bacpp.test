package tv.blackarrow.cpp.model;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.BlackoutConfirmationType;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;
import tv.blackarrow.cpp.utils.StreamExecutionType;

public class AcquisitionPoint {
	private static final Logger LOGGER = LogManager.getLogger(AcquisitionPoint.class);
    private String acquisitionPointIdentity;
    private String providerExternalRef;
    private boolean sccDeleteEmptyBreak;
    private String feedExternalRef;
    private String networkExternalRef;
    private String baIntefactTypeExternalRef;
    private String baHlsInterfaceTypeExternalRef;
    private String baHssInterfaceTypeExternalRef;
    private String baSchedulesInterfaceTypeExternalRef;
    private long signalTimeOffset;
    private boolean signalAbortEnabled;
    private String deliveryType;
    private String esamVersion;
    private boolean feedAllowsOpenEndedBlackouts;
    private SignalHandlingConfiguration spliceInsertConfiguredValue;
    private SignalHandlingConfiguration providerAdsConfiguredValue;
    private SignalHandlingConfiguration distributorAdsConfiguredValue;
    private SignalHandlingConfiguration poSignalsConfiguredValue;
    private SignalHandlingConfiguration inBandContentIdConfiguredValue;
	private AlternateContentVersion feedsAlternateContentVersion;
	private boolean feedHasAlternateContentEnabled;
	private String schedulelessAlternateContentLocation;

	private boolean useInbandOpportunitySignal;

    private String blackoutConfirmationType; // the value is in-band or out-of-band

    private boolean feedTriggerEventsByEventID;

    /**
     * Following fields added for out-of-band blackout support;
     */
    private String transcoderEndpoint;
    private int contentIDFrequency;
    private long notificationRetryInterval;
    @Deprecated
    private int sccNotificationBuffer;  
	private int feedSCCNotificationBuffer;
    private long lastUpdatedTime;

    /**
     *
     * Following fields added for in-band blackout support
     */
    private int programStartBuffer;

    /**
     * Used only by Ad Requests, no use in the Blackout Requests.
     */
    private String adSlateLocation;
    
    private String zoneIdentity;
    
    private String executionType;

    public boolean isIpAcquisitionPoint(){
    	return deliveryType == null || !deliveryType.equalsIgnoreCase(BlackoutConfirmationType.QAM.name());
    }

    public boolean isQAMAcquisitionPoint(){
    	return !isIpAcquisitionPoint();
    }

    public boolean isOutBand(){
    	return BlackoutConfirmationType.OUT_OF_BAND.getConfirmationType().equalsIgnoreCase(blackoutConfirmationType);
    }

    public boolean isInBand(){
		return BlackoutConfirmationType.IN_BAND.getConfirmationType().equalsIgnoreCase(blackoutConfirmationType)
				|| BlackoutConfirmationType.QAM.name().equalsIgnoreCase(deliveryType);
    }

    public boolean isInBandQAMAcquisitionPoint() {
		return isQAMAcquisitionPoint() && isInBand();
	}

    public String getAcquisitionPointIdentity() {
        return acquisitionPointIdentity;
    }

    public void setAcquisitionPointIdentity(String acquisitionPointIdentity) {
        this.acquisitionPointIdentity = acquisitionPointIdentity;
    }

    public String getProviderExternalRef() {
        return providerExternalRef;
    }

    public void setProviderExternalRef(String providerExternalRef) {
        this.providerExternalRef = providerExternalRef;
    }

    public boolean isSccDeleteEmptyBreak() {
        return sccDeleteEmptyBreak;
    }

    public void setSccDeleteEmptyBreak(boolean sccDeleteEmptyBreak) {
        this.sccDeleteEmptyBreak = sccDeleteEmptyBreak;
    }

    public String getFeedExternalRef() {
        return feedExternalRef;
    }

    public void setFeedExternalRef(String feedExternalRef) {
        this.feedExternalRef = feedExternalRef;
    }

    public String getNetworkExternalRef() {
        return networkExternalRef;
    }

    public void setNetworkExternalRef(String networkExternalRef) {
        this.networkExternalRef = networkExternalRef;
    }

    public String getBaIntefactTypeExternalRef() {
        return baIntefactTypeExternalRef;
    }

    public void setBaIntefactTypeExternalRef(String baIntefactTypeExternalRef) {
        this.baIntefactTypeExternalRef = baIntefactTypeExternalRef;
    }

    public String getBaHlsInterfaceTypeExternalRef() {
        return baHlsInterfaceTypeExternalRef;
    }

    public void setBaHlsInterfaceTypeExternalRef(String baHlsInterfaceTypeExternalRef) {
        this.baHlsInterfaceTypeExternalRef = baHlsInterfaceTypeExternalRef;
    }

    public String getBaSchedulesInterfaceTypeExternalRef() {
        return baSchedulesInterfaceTypeExternalRef;
    }

    public void setBaSchedulesInterfaceTypeExternalRef(String baSchedulesInterfaceTypeExternalRef) {
        this.baSchedulesInterfaceTypeExternalRef = baSchedulesInterfaceTypeExternalRef;
    }

    public long getSignalTimeOffset() {
        return signalTimeOffset;
    }

    public void setSignalTimeOffset(long signalTimeOffset) {
        this.signalTimeOffset = signalTimeOffset;
    }

    public String getBaHssInterfaceTypeExternalRef() {
        return baHssInterfaceTypeExternalRef;
    }

    public void setBaHssInterfaceTypeExternalRef(String baHssInterfaceTypeExternalRef) {
        this.baHssInterfaceTypeExternalRef = baHssInterfaceTypeExternalRef;
    }

	public String getBlackoutConfirmationType() {
		return blackoutConfirmationType;
	}

	public void setBlackoutConfirmationType(String blackoutConfirmationType) {
		this.blackoutConfirmationType = blackoutConfirmationType;
	}	

	public int getContentIDFrequency() {
		return contentIDFrequency;
	}

	public void setContentIDFrequency(int contentIDFrequency) {
		this.contentIDFrequency = contentIDFrequency;
	}

	public long getNotificationRetryInterval() {
		return notificationRetryInterval;
	}

	public void setNotificationRetryInterval(long notificationRetryInterval) {
		this.notificationRetryInterval = notificationRetryInterval;
	}

	public int getProgramStartBuffer() {
		return programStartBuffer;
	}

	public void setProgramStartBuffer(int programStartBuffer) {
		this.programStartBuffer = programStartBuffer;
	}

	public String getTranscoderEndpoint() {
		return transcoderEndpoint;
	}

	public void setTranscoderEndpoint(String transcoderEndpoint) {
		this.transcoderEndpoint = transcoderEndpoint;
	}

	@Deprecated
	/*
	 * It's only used by ned.1 code
	 */
        public void setSCCNotificationBuffer(int sccNotificationBuffer) {
                this.sccNotificationBuffer = sccNotificationBuffer;
        }

	public boolean isSignalAbortEnabled() {
		return signalAbortEnabled;
	}

	public void setSignalAbortEnabled(boolean signalAbortEnabled) {
		this.signalAbortEnabled = signalAbortEnabled;
	}

	public String getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	public String getEsamVersion() {
		return esamVersion;
	}

	public void setEsamVersion(String esamVersion) {
		this.esamVersion = esamVersion;
	}

	public boolean isFeedAllowsOpenEndedBlackouts() {
		return feedAllowsOpenEndedBlackouts;
	}

	public void setFeedAllowsOpenEndedBlackouts(boolean feedAllowsOpenEndedBlackouts) {
		this.feedAllowsOpenEndedBlackouts = feedAllowsOpenEndedBlackouts;
	}

	public SignalHandlingConfiguration getSpliceInsertConfiguredValue() {
		return spliceInsertConfiguredValue;
	}

	public void setSpliceInsertConfiguredValue(SignalHandlingConfiguration spliceInsertConfiguredValue) {
		this.spliceInsertConfiguredValue = spliceInsertConfiguredValue;
	}

	public SignalHandlingConfiguration getProviderAdsConfiguredValue() {
		return providerAdsConfiguredValue;
	}

	public void setProviderAdsConfiguredValue(SignalHandlingConfiguration providerAdsConfiguredValue) {
		this.providerAdsConfiguredValue = providerAdsConfiguredValue;
	}

	public SignalHandlingConfiguration getDistributorAdsConfiguredValue() {
		return distributorAdsConfiguredValue;
	}

	public void setDistributorAdsConfiguredValue(SignalHandlingConfiguration distributorAdsConfiguredValue) {
		this.distributorAdsConfiguredValue = distributorAdsConfiguredValue;
	}

	public SignalHandlingConfiguration getPoSignalsConfiguredValue() {
		return poSignalsConfiguredValue;
	}

	public void setPoSignalsConfiguredValue(SignalHandlingConfiguration poSignalsConfiguredValue) {
		this.poSignalsConfiguredValue = poSignalsConfiguredValue;
	}

	public SignalHandlingConfiguration getInBandContentIdConfiguredValue() {
		return inBandContentIdConfiguredValue;
	}

	public void setInBandContentIdConfiguredValue(SignalHandlingConfiguration inBandContentIdConfiguredValue) {
		this.inBandContentIdConfiguredValue = inBandContentIdConfiguredValue;
	}

	public SegmentType getDefaultSegmentTypeForAdStart(){
		if(this.getSpliceInsertConfiguredValue() != null && this.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD){
			return SegmentType.DISTRIBUTOR_ADVERTISEMENT_START;
		}
		return SegmentType.PLACEMENT_OPPORTUNITY_START;
	}

	public SegmentType getDefaultSegmentTypeForAdEnd(){
		if(this.getSpliceInsertConfiguredValue() != null && this.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD){
			return SegmentType.DISTRIBUTOR_ADVERTISEMENT_END;
		}
		return SegmentType.PLACEMENT_OPPORTUNITY_END;
	}

	public static SegmentType getDefaultSegmentTypeForAdStart(AcquisitionPoint acquisitionPoint){
		if(acquisitionPoint != null){
			return acquisitionPoint.getDefaultSegmentTypeForAdStart();
		}
		return SegmentType.PLACEMENT_OPPORTUNITY_START;
	}

	public static SegmentType getDefaultSegmentTypeForAdEnd(AcquisitionPoint acquisitionPoint){
		if(acquisitionPoint != null){
			return acquisitionPoint.getDefaultSegmentTypeForAdEnd();
		}
		return SegmentType.PLACEMENT_OPPORTUNITY_END;
	}

    public boolean isFeedTriggerEventsByEventID() {
		return feedTriggerEventsByEventID;
	}

	public void setFeedTriggerEventsByEventID(boolean feedTriggerEventsByEventID) {
		this.feedTriggerEventsByEventID = feedTriggerEventsByEventID;
	}

	public AlternateContentVersion getFeedsAlternateContentVersion() {
		return feedsAlternateContentVersion;
	}

	public void setFeedsAlternateContentVersion(AlternateContentVersion feedsAlternateContentVersion) {
		this.feedsAlternateContentVersion = feedsAlternateContentVersion;
	}

	public boolean isFeedHasAlternateContentEnabled() {
		return feedHasAlternateContentEnabled;
	}

	public void setFeedHasAlternateContentEnabled(boolean feedHasAlternateContentEnabled) {
		this.feedHasAlternateContentEnabled = feedHasAlternateContentEnabled;
	}

	public String getSchedulelessAlternateContentLocation() {
		return schedulelessAlternateContentLocation;
	}

	public void setSchedulelessAlternateContentLocation(String schedulelessAlternateContentLocation) {
		this.schedulelessAlternateContentLocation = schedulelessAlternateContentLocation;
	}
	
	public boolean isUseInbandOpportunitySignal() {
		return useInbandOpportunitySignal;
	}

	public void setUseInbandOpportunitySignal(boolean useInbandOpportunitySignal) {
		this.useInbandOpportunitySignal = useInbandOpportunitySignal;
	}

	public String getAdSlateLocation() {
		return StringUtils.isNotBlank(adSlateLocation) ? adSlateLocation.trim() : null;
	}

	public void setAdSlateLocation(String adSlateLocation) {
		this.adSlateLocation = adSlateLocation;
	}
	
	public boolean isConfiguredForSchedulelessAltContent() {
		return isFeedHasAlternateContentEnabled() && AlternateContentVersion.SCHEDULELESS == getFeedsAlternateContentVersion() 
				&& StringUtils.isNotBlank(getSchedulelessAlternateContentLocation());
	}
	
	public boolean isConfiguredForSchedulelessInbandPOs() {
		return isUseInbandOpportunitySignal() && StringUtils.isNotBlank(getAdSlateLocation());
	}

	/**
	 * @return the zoneIdentity
	 */
	public String getZoneIdentity() {
		return zoneIdentity;
	}

	/**
	 * @param zoneIdentity the zoneIdentity to set
	 */
	public void setZoneIdentity(String zoneIdentity) {
		this.zoneIdentity = zoneIdentity;
	}

	/**
	 * @return the lastUpdatedTime
	 */
	public long getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	/**
	 * @param lastUpdatedTime the lastUpdatedTime to set
	 */
	public void setLastUpdatedTime(Long lastUpdatedTime) {
		this.lastUpdatedTime = (lastUpdatedTime == null ? -1 : lastUpdatedTime);
	}


	public int getFeedSCCNotificationBuffer() {
		return feedSCCNotificationBuffer;
	}

	public void setFeedSCCNotificationBuffer(int feedSCCNotificationBuffer) {
		this.feedSCCNotificationBuffer = feedSCCNotificationBuffer;
	}

	public StreamExecutionType getExecutionType() {
		StreamExecutionType execution = StreamExecutionType.ENCODER_LEVEL;
		try {
			execution = StreamExecutionType.fromValue(this.executionType);
		} catch (Exception e) {
			LOGGER.info("Acquisition point " + acquisitionPointIdentity
					+ " is not having Encoder/Viewer level execution type defined appropriately. Defaulting the execution type to ENCODER_LEVEL");
		}
		return execution;
	}

	public void setExecutionType(String executionType) {
		this.executionType = executionType;
	}

	@Override
	public String toString() {
		return "AcquisitionPoint [acquisitionPointIdentity=" + acquisitionPointIdentity + ", providerExternalRef="
				+ providerExternalRef + ", sccDeleteEmptyBreak=" + sccDeleteEmptyBreak + ", feedExternalRef="
				+ feedExternalRef + ", networkExternalRef=" + networkExternalRef + ", baIntefactTypeExternalRef="
				+ baIntefactTypeExternalRef + ", baHlsInterfaceTypeExternalRef=" + baHlsInterfaceTypeExternalRef
				+ ", baHssInterfaceTypeExternalRef=" + baHssInterfaceTypeExternalRef
				+ ", baSchedulesInterfaceTypeExternalRef=" + baSchedulesInterfaceTypeExternalRef + ", signalTimeOffset="
				+ signalTimeOffset + ", signalAbortEnabled=" + signalAbortEnabled + ", deliveryType=" + deliveryType
				+ ", esamVersion=" + esamVersion + ", feedAllowsOpenEndedBlackouts=" + feedAllowsOpenEndedBlackouts
				+ ", spliceInsertConfiguredValue=" + spliceInsertConfiguredValue + ", providerAdsConfiguredValue="
				+ providerAdsConfiguredValue + ", distributorAdsConfiguredValue=" + distributorAdsConfiguredValue
				+ ", poSignalsConfiguredValue=" + poSignalsConfiguredValue + ", inBandContentIdConfiguredValue="
				+ inBandContentIdConfiguredValue + ", feedsAlternateContentVersion=" + feedsAlternateContentVersion
				+ ", feedHasAlternateContentEnabled=" + feedHasAlternateContentEnabled
				+ ", schedulelessAlternateContentLocation=" + schedulelessAlternateContentLocation
				+ ", useInbandOpportunitySignal=" + useInbandOpportunitySignal + ", blackoutConfirmationType="
				+ blackoutConfirmationType + ", feedTriggerEventsByEventID=" + feedTriggerEventsByEventID
				+ ", transcoderEndpoint=" + transcoderEndpoint + ", contentIDFrequency=" + contentIDFrequency
				+ ", notificationRetryInterval=" + notificationRetryInterval + ", sccNotificationBuffer="
				+ sccNotificationBuffer + ", feedSCCNotificationBuffer=" + feedSCCNotificationBuffer
				+ ", lastUpdatedTime=" + lastUpdatedTime + ", programStartBuffer=" + programStartBuffer
				+ ", adSlateLocation=" + adSlateLocation + ", zoneIdentity=" + zoneIdentity + ", executionType="
				+ executionType + "]";
	}
	
	

}