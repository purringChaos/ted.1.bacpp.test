package tv.blackarrow.cpp.setting;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SettingUtils {

	private static final Logger LOGGER = LogManager.getLogger(SettingUtils.class);
	
    public final static String ACQUISITION_POINT_ID = "Acquisition Point ID";
    public final static String FEED_ID = "Feed ID";
    public final static String NETWORK_ID = "Network ID";
    public final static String PROVIDER_ID = "Provider ID";
    public final static String SIGNAL_ABORT_ENABLED = "Signal Abort Enabled";
    public final static String SCC_DELETE_EMPTY_BREAK = "SCC Delete Empty Break";
    public final static String INCLUDE_IN_POINT = "Include In Point";
    public final static String HLS_INTERFACE_TYPE = "HLS Interface Type";
    public final static String HSS_INTERFACE_TYPE = "HSS Interface Type";
    public final static String SCHEDULES_INTERFACE_TYPE = "Schedules";
    public final static String SIGNAL_TIME_OFFSET = "Signal Time Offset";
    public final static String TRANSCODER_ENDPOINT = "Encoder Endpoint";
    public final static String BLACKOUT_CONFIRMATION_TYPE = "Blackout Confirmation";
    public final static String PROGRAM_START_BUFFER = "Program Start Buffer";
    public final static String CONTENT_ID_FREQUENCY= "Content ID Frequency";
	public static final String EXECUTION_TYPE = "Execution Type";
    public final static String DELIVERY_TYPE= "Delivery Type";
    public final static String ESAM_VERSION= "ESAM Version";
    public final static String STREAM_SCC_NOTIFICATION_BUFFER = "SCC Notification Buffer";//Only for Backward 9.1 compatibility, after 9.2.2 upgrade remove this.
	//Underdog
	public final static String SPLICE_INSERT = "Splice Insert";
	public final static String PROVIDER_ADS = "Provider Ads";
	public final static String DISTIRIBUTOR_ADS = "Distributor Ads";
	public final static String PO_SIGNALS = "PO Signals";
	public final static String INBAND_CONTENT_ID = "Inband Content ID";
	
	//ned.2
	public final static String ZONE_IDENTITY = "Zone Identity";
	public final static String LAST_UPDATED_TIME = "Last Update Time";


	private static final String SYS_PROPERTY_COM_COUCHBASE_KVENDPOINTS = "com.couchbase.kvEndpoints";
	private static final int SYS_ENV_COM_COUCHBASE_KVENDPOINTS_DEFAULT_VALUE = 1;

    private SettingUtils() {}
    
    /**
     * Try the system property to get the parent directory where log4j is
     * configured.
     * 
     * @return the configuration pathname
     */

    public static String getConfigurationPath() {
        String productDir = getProductName();
        if (!productDir.endsWith("/")) {
            productDir = productDir + "/";
        }
        String blackarrowDir = System.getProperty("blackarrow.home");
        if (blackarrowDir == null || blackarrowDir.trim().isEmpty()) {
            blackarrowDir = "/opt/blackarrow/";
        }
        if (!blackarrowDir.endsWith("/")) {
            blackarrowDir = blackarrowDir + "/";
        }

        return blackarrowDir + productDir + "conf/";
    }


    public static int getCouchbaseKvEndpointse() {
        String kvEndpoints = System.getProperty(SYS_PROPERTY_COM_COUCHBASE_KVENDPOINTS);

        if (StringUtils.isNotBlank(kvEndpoints)) {
            return Integer.parseInt(kvEndpoints);
        }
        return SYS_ENV_COM_COUCHBASE_KVENDPOINTS_DEFAULT_VALUE;
    }

    /**
     * First try the java environment, for ba.linear.config.home which should
     * have been set by the start-up argument.
     * 
     * @return the product name directory
     */

    public static String getProductName() {
        String productName = System.getProperty("ba.product.name");

        if (productName == null || productName.trim().isEmpty()) {
            productName = "ess";
        }
        return productName;
    }
    
    public static long getThreadLockAcquireWaitTime(){
    	String configuredLockAcquireWaitTime = System.getProperty("ess.lock.acquire.wait.time.millis");
    	final long DEFAULT_ACQUIRE_LOCK_WAIT_TIME = 5;//5 MILLISECONDS
    	long lockAcquireWaitTime = DEFAULT_ACQUIRE_LOCK_WAIT_TIME;
    	try {
    		if(StringUtils.isNotBlank(configuredLockAcquireWaitTime)){
    			lockAcquireWaitTime = Long.parseLong(configuredLockAcquireWaitTime);
    		}
    	} catch (Exception ex){
    		LOGGER.error("Invalid lock wait time passed in the system property. Please correct that and restart. For now continuing with default wait time of " + DEFAULT_ACQUIRE_LOCK_WAIT_TIME + "ms.");
    	}
    	return lockAcquireWaitTime;
    }
}

