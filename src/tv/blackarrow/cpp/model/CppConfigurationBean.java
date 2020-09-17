package tv.blackarrow.cpp.model;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.setting.SettingUtils;

public class CppConfigurationBean {

    private static final Logger LOGGER = LogManager.getLogger(CppConfigurationBean.class);

    private String dataCenter = "";
    private int linearRunTimeID;
    private int nodeID;
    private int defaultDataExpirationSeconds;
    private String poRepoPath;
    private String poInProcessDir;
    private String poProcessedDir;
    private String poErrorDir;
    private String linearPoisToAdsNotificationUrl;
    private boolean sendCustomAbortSCCResponse;
    private String linearPoisToExternalAdsNotificationUrl;
    private String sccEndPointMonitorUrl;
    private String mccEndPointMonitorUrl;
    private String poisIdentity;
    private String poisSystemId;
    private String defaultHLSInterfaceType;
    private String defaultHSSInterfaceType;
    private String cbClusterUsername;
    private String cbClusterPassword;
    private String cbClusterDataPath;
    private int poBreakReconfirmationWindowInMillis;
    private int linearPoisProcessingTimeInSeconds;
    private boolean serviceEnabled; // it will indicate if this service (POIS) will be enabled
    private String poisClusterHostNames;
    private String hostedBlackoutEventStatusUrl;
    private String sccInpointReponse;
    private String acsUrl;
    private String qamZoneIdentityType;
    private String cbHostNames;
    private String cbBucketName;
    private String cbBucketAccessUserName;
    private String cbBucketAccessUsersPassword;
	private String deliveryType;
    private int connectionTimeout;
    private boolean sendTerritoryUpdateConfirmation;
    private int territoryUpdateNotificationThreadPoolSize;
	private int esamResponseUTCTimeDeltaInMillis;
	private boolean scte224CadentContentIDGenerationEnabled;
	
	/**
	 * Currently only used for Program End for handling back to back blackout events.PRODISSUE-1513
	 */
    private int esamProgramEndNotifyAheadDeltaInMillis;
    
	private static ApplicationContext CTX;
    private static CppConfigurationBean instance = null;

    static {
        try {
            String configurationFileName = SettingUtils.getConfigurationPath() + "cpp_bean.xml";
            File configurationFile = new File(configurationFileName);
            if (configurationFile.exists()) {
                LOGGER.debug(()->"Loading configuration from " + configurationFileName);
            } else {
                LOGGER.error(()->"Configuration file not found at " + configurationFileName);
            }
            CTX = new FileSystemXmlApplicationContext("file:" + configurationFileName);
            instance = (CppConfigurationBean) CTX.getBean("configBean");
        } catch (Exception e) {
            LOGGER.error(()->"Exception loading configuration", e);
        }
    }

    public static CppConfigurationBean getInstance() {
        return instance;
    }

    public String getPoisIdentity() {
        return poisIdentity;
    }

    public void setPoisIdentity(String poisIdentity) {
        this.poisIdentity = poisIdentity;
    }

    public String getPoisSystemId() {
        return poisSystemId;
    }

    public void setPoisSystemId(String poisSystemId) {
        this.poisSystemId = poisSystemId;
    }

    public int getLinearRunTimeID() {
        return linearRunTimeID;
    }

    public void setLinearRunTimeID(int linearRunTimeID) {
        this.linearRunTimeID = linearRunTimeID;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getPoRepoPath() {
        return poRepoPath;
    }

    public void setPoRepoPath(String poRepoPath) {
        this.poRepoPath = poRepoPath;
    }

    public String getPoInProcessDir() {
        return poInProcessDir;
    }

    public void setPoInProcessDir(String poInProcessDir) {
        this.poInProcessDir = poInProcessDir;
    }

    public String getPoProcessedDir() {
        return poProcessedDir;
    }

    public void setPoProcessedDir(String poProcessedDir) {
        this.poProcessedDir = poProcessedDir;
    }

    public String getPoErrorDir() {
        return poErrorDir;
    }

    public void setPoErrorDir(String poErrorDir) {
        this.poErrorDir = poErrorDir;
    }

    public String getLinearPoisToAdsNotificationUrl() {
        return linearPoisToAdsNotificationUrl;
    }

    public void setLinearPoisToAdsNotificationUrl(String linearPoisToAdsNotificationUrl) {
        this.linearPoisToAdsNotificationUrl = linearPoisToAdsNotificationUrl;
    }

    public String getSccEndPointMonitorUrl() {
        return sccEndPointMonitorUrl;
    }

    public void setSccEndPointMonitorUrl(String sccEndPointMonitorUrl) {
        this.sccEndPointMonitorUrl = sccEndPointMonitorUrl;
    }

    public String getMccEndPointMonitorUrl() {
        return mccEndPointMonitorUrl;
    }

    public void setMccEndPointMonitorUrl(String mccEndPointMonitorUrl) {
        this.mccEndPointMonitorUrl = mccEndPointMonitorUrl;
    }

    public String getLinearPoisToExternalAdsNotificationUrl() {
        return linearPoisToExternalAdsNotificationUrl;
    }

    public void setLinearPoisToExternalAdsNotificationUrl(String linearPoisToExternalAdsNotificationUrl) {
        this.linearPoisToExternalAdsNotificationUrl = linearPoisToExternalAdsNotificationUrl;
    }

    public String getDefaultHLSInterfaceType() {
        return defaultHLSInterfaceType;
    }

    public void setDefaultHLSInterfaceType(String defaultHLSInterfaceType) {
        this.defaultHLSInterfaceType = defaultHLSInterfaceType;
    }

    public String getDefaultHSSInterfaceType() {
        return defaultHSSInterfaceType;
    }

    public void setDefaultHSSInterfaceType(String defaultHSSInterfaceType) {
        this.defaultHSSInterfaceType = defaultHSSInterfaceType;
    }

    public int getDefaultDataExpirationSeconds() {
        return defaultDataExpirationSeconds;
    }

    public void setDefaultDataExpirationSeconds(int defaultDataExpirationSeconds) {
        this.defaultDataExpirationSeconds = defaultDataExpirationSeconds;
    }

    public String getCbClusterUsername() {
        return cbClusterUsername;
    }

    public void setCbClusterUsername(String cbClusterUsername) {
        this.cbClusterUsername = cbClusterUsername;
    }

    public String getCbClusterPassword() {
        return cbClusterPassword;
    }

    public void setCbClusterPassword(String cbClusterPassword) {
        this.cbClusterPassword = cbClusterPassword;
    }

    public String getCbClusterDataPath() {
		return cbClusterDataPath;
	}

	public void setCbClusterDataPath(String cbClusterDataPath) {
		this.cbClusterDataPath = cbClusterDataPath;
	}


    public boolean isServiceEnabled() {
		return serviceEnabled;
	}

	public void setServiceEnabled(boolean serviceEnabled) {
		this.serviceEnabled = serviceEnabled;
	}

	/**
	 * @return the poisClusterHostNames
	 */
	public String getPoisClusterHostNames() {
		return poisClusterHostNames;
	}

	/**
	 * @param poisClusterHostNames the poisClusterHostNames to set
	 */
	public void setPoisClusterHostNames(String poisClusterHostNames) {
		this.poisClusterHostNames = poisClusterHostNames;
	}


	/**
	 * @return the hostedBlackoutEventStatusUrl
	 */
	public String getHostedBlackoutEventStatusUrl() {
		return hostedBlackoutEventStatusUrl;
	}

	/**
	 * @param hostedBlackoutEventStatusUrl the hostedBlackoutEventStatusUrl to set
	 */
	public void setHostedBlackoutEventStatusUrl(String hostedBlackoutEventStatusUrl) {
		this.hostedBlackoutEventStatusUrl = hostedBlackoutEventStatusUrl;
	}

	/**
	 * @return the sendCustomAbortSCCResponse
	 */
	public final boolean isSendCustomAbortSCCResponse() {
		return sendCustomAbortSCCResponse;
	}

	/**
	 * @param sendCustomAbortSCCResponse the sendCustomAbortSCCResponse to set
	 */
	public final void setSendCustomAbortSCCResponse(boolean sendCustomAbortSCCResponse) {
		this.sendCustomAbortSCCResponse = sendCustomAbortSCCResponse;
	}

	public String getSccInpointReponse() {
		return sccInpointReponse;
	}

	public void setSccInpointReponse(String sccInpointReponse) {
		this.sccInpointReponse = sccInpointReponse;
	}

	public String getAcsUrl() {
		return acsUrl;
	}

	public void setAcsUrl(String acsUrl) {
		this.acsUrl = acsUrl;
	}

	public String getQamZoneIdentityType() {
		if(qamZoneIdentityType == null){
			qamZoneIdentityType = "private:adzone";
		}
		return qamZoneIdentityType;
	}

	public void setQamZoneIdentityType(String qamZoneIdentityType) {
		this.qamZoneIdentityType = qamZoneIdentityType;
	}
	
	public String getCbHostNames() {
		return cbHostNames;
	}
	
	public void setCbHostNames(String cbHostName) {
		this.cbHostNames = cbHostName;
	}
	
	public String getCbBucketName() {
		return cbBucketName;
	}
	
	public void setCbBucketName(String cbBucketName) {
		this.cbBucketName = cbBucketName;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return the dataCenter
	 */
	public String getDataCenter() {
		return dataCenter;
	}

	/**
	 * @param dataCenter the dataCenter to set
	 */
	public void setDataCenter(String dataCenter) {
		this.dataCenter = dataCenter;
	}

	public boolean isSendTerritoryUpdateConfirmation() {
		return sendTerritoryUpdateConfirmation;
	}

	public void setSendTerritoryUpdateConfirmation(boolean sendTerritoryUpdateConfirmation) {
		this.sendTerritoryUpdateConfirmation = sendTerritoryUpdateConfirmation;
	}

	public int getTerritoryUpdateNotificationThreadPoolSize() {
		return territoryUpdateNotificationThreadPoolSize;
	}

	public void setTerritoryUpdateNotificationThreadPoolSize(int territoryUpdateNotificationThreadPoolSize) {
		this.territoryUpdateNotificationThreadPoolSize = territoryUpdateNotificationThreadPoolSize;
	}

	/**
	 * @return the cbBucketAccessUserName
	 */
	public String getCbBucketAccessUserName() {
		return cbBucketAccessUserName;
	}

	/**
	 * 
	 * @param cbBucketAccessUserName the cbBucketAccessUserName to set
	 */
	public void setCbBucketAccessUserName(String cbBucketAccessUserName) {
		this.cbBucketAccessUserName = cbBucketAccessUserName;
	}

	/**
	 * @return the cbBucketAccessUsersPassword
	 */
	public String getCbBucketAccessUsersPassword() {
		return cbBucketAccessUsersPassword;
	}

	/**
	 * @param cbBucketAccessUsersPassword the cbBucketAccessUsersPassword to set
	 */
	public void setCbBucketAccessUsersPassword(String cbBucketAccessUsersPassword) {
		this.cbBucketAccessUsersPassword = cbBucketAccessUsersPassword;
	}

	public String getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	public int getLinearPoisProcessingTimeInSeconds() {
		return linearPoisProcessingTimeInSeconds;
	}

	public void setLinearPoisProcessingTimeInSeconds(int linearPoisProcessingTimeInSeconds) {
		this.linearPoisProcessingTimeInSeconds = linearPoisProcessingTimeInSeconds;
	}

	public int getPoBreakReconfirmationWindowInMillis() {
		return poBreakReconfirmationWindowInMillis;
	}

	public void setPoBreakReconfirmationWindowInMillis(int poBreakReconfirmationWindowInMillis) {
		this.poBreakReconfirmationWindowInMillis = poBreakReconfirmationWindowInMillis;
	}

	public int getEsamResponseUTCTimeDeltaInMillis() {
		return esamResponseUTCTimeDeltaInMillis;
	}

	public void setEsamResponseUTCTimeDeltaInMillis(int esamResponseUTCTimeDeltaInMillis) {
		this.esamResponseUTCTimeDeltaInMillis = esamResponseUTCTimeDeltaInMillis;
	}

	public int getEsamProgramEndNotifyAheadDeltaInMillis() {
		return esamProgramEndNotifyAheadDeltaInMillis;
	}

	public void setEsamProgramEndNotifyAheadDeltaInMillis(int esamProgramEndNotifyAheadDeltaInMillis) {
		this.esamProgramEndNotifyAheadDeltaInMillis = esamProgramEndNotifyAheadDeltaInMillis;
	}

	/**
	 * @return the scte224CadentContentIDGenerationEnabled
	 */
	public boolean isScte224CadentContentIDGenerationEnabled() {
		return scte224CadentContentIDGenerationEnabled;
	}

	/**
	 * @param scte224CadentContentIDGenerationEnabled the scte224CadentContentIDGenerationEnabled to set
	 */
	public void setScte224CadentContentIDGenerationEnabled(boolean scte224CadentContentIDGenerationEnabled) {
		this.scte224CadentContentIDGenerationEnabled = scte224CadentContentIDGenerationEnabled;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CppConfigurationBean [dataCenter=" + dataCenter + ", linearRunTimeID=" + linearRunTimeID + ", nodeID=" + nodeID + ", defaultDataExpirationSeconds=" + defaultDataExpirationSeconds
				+ ", poRepoPath=" + poRepoPath + ", poInProcessDir=" + poInProcessDir + ", poProcessedDir=" + poProcessedDir + ", poErrorDir=" + poErrorDir + ", linearPoisToAdsNotificationUrl="
				+ linearPoisToAdsNotificationUrl + ", sendCustomAbortSCCResponse=" + sendCustomAbortSCCResponse + ", linearPoisToExternalAdsNotificationUrl=" + linearPoisToExternalAdsNotificationUrl
				+ ", sccEndPointMonitorUrl=" + sccEndPointMonitorUrl + ", mccEndPointMonitorUrl=" + mccEndPointMonitorUrl + ", poisIdentity=" + poisIdentity + ", poisSystemId=" + poisSystemId
				+ ", defaultHLSInterfaceType=" + defaultHLSInterfaceType + ", defaultHSSInterfaceType=" + defaultHSSInterfaceType + ", cbClusterUsername=" + cbClusterUsername + ", cbClusterPassword="
				+ cbClusterPassword + ", cbClusterDataPath=" + cbClusterDataPath + ", poBreakReconfirmationWindowInMillis=" + poBreakReconfirmationWindowInMillis
				+ ", linearPoisProcessingTimeInSeconds=" + linearPoisProcessingTimeInSeconds + ", serviceEnabled=" + serviceEnabled + ", poisClusterHostNames=" + poisClusterHostNames
				+ ", hostedBlackoutEventStatusUrl=" + hostedBlackoutEventStatusUrl + ", sccInpointReponse=" + sccInpointReponse + ", acsUrl=" + acsUrl + ", qamZoneIdentityType=" + qamZoneIdentityType
				+ ", cbHostNames=" + cbHostNames + ", cbBucketName=" + cbBucketName + ", cbBucketAccessUserName=" + cbBucketAccessUserName + ", cbBucketAccessUsersPassword="
				+ cbBucketAccessUsersPassword + ", deliveryType=" + deliveryType + ", connectionTimeout=" + connectionTimeout + ", sendTerritoryUpdateConfirmation=" + sendTerritoryUpdateConfirmation
				+ ", territoryUpdateNotificationThreadPoolSize=" + territoryUpdateNotificationThreadPoolSize + ", esamResponseUTCTimeDeltaInMillis=" + esamResponseUTCTimeDeltaInMillis
				+ ", scte224CadentContentIDGenerationEnabled=" + scte224CadentContentIDGenerationEnabled + ", esamProgramEndNotifyAheadDeltaInMillis=" + esamProgramEndNotifyAheadDeltaInMillis + "]";
	}
	
}
