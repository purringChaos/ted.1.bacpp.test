package tv.blackarrow.cpp.components.util;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.setting.SettingUtils;

/**
 * 
 * @author akumar
 *
 */
public class ESSClusterLockUtil {
	private static final Logger LOGGER = LogManager.getLogger(ESSClusterLockUtil.class);
	public static final DataManager dataManager = DataManagerFactory.getInstance();
	public static final long WAIT_TIME_FOR_LOCK_RETRY_IN_MILLISECONDS = SettingUtils.getThreadLockAcquireWaitTime();
	public static final int GENERIC_MAX_LOCKING_TIME_FOR_LOCK = 60000; // 1 minute
    public static final String FEED_LOCK_PREFIX = "FEED_LOCK_";
    public static final String SCHEDULE_EVENT_LOCK = "SCHEDULE_EVENT_LOCK";
	
	public static StringDocument acquireClusterWideLock(final String lockName){
		if(StringUtils.isBlank(lockName)){
			return null;
		}
		StringDocument lockedDocument = null;

		long startTime = System.currentTimeMillis();
		while ((lockedDocument = dataManager.lock(lockName)) == null) {
			debugLog("Unable to take the lock '" + lockName + "' as it has been acquired by some other process already. " + 
					 "Waiting for next " + WAIT_TIME_FOR_LOCK_RETRY_IN_MILLISECONDS + " seconds before next retry.");
			try {
				TimeUnit.MILLISECONDS.sleep(WAIT_TIME_FOR_LOCK_RETRY_IN_MILLISECONDS);
			} catch (InterruptedException exception) {
				warnLog("Thread interrupted while waiting for the lock '" + lockName + "' to be released by the other process."	+ 
						" Ignoring interruption and continuing with usual flow.");
			}
			//we are checking that handleRealtimeEventCUDOperations is not able to take lock last 1 minute, 
			//so we are throwing an exception and we will try to load it force fully in retry section
			long lockTryTime = System.currentTimeMillis() - startTime;
			if (lockTryTime >= GENERIC_MAX_LOCKING_TIME_FOR_LOCK && (lockedDocument = dataManager.lock(lockName)) == null) {
				warnLog("This ESS node was not able to take a lock '" + lockName + "' from last 1 minute. " + 
						"So we are removing that lock entry forcefully from Couchbase and next time it should be able to take the lock successfully.");
				dataManager.unlock(lockName);
			} else if (lockedDocument != null) {// locked already taken so breaking while loop
				break;
			}
		}
		return lockedDocument;
	}
	
	public static void releaseClusterWideLock(final StringDocument lockedDocument) {
		if (lockedDocument != null) {
			dataManager.unlock(lockedDocument);
		}
	}
	
	public static StringDocument acquireClusterWideLockWithFeedExternalRef(final String feedExtRef) {
		return acquireClusterWideLock(FEED_LOCK_PREFIX + feedExtRef);
	}
	
	public static StringDocument acquireClusterWideLockForScheduledEvents() {
		return acquireClusterWideLock(SCHEDULE_EVENT_LOCK);
	}
	
	protected static void debugLog(String string) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(()->string);
		}
	}

	protected static void infoLog(String message) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(()->message);
		}
	}

	protected static void warnLog(String message) {
		LOGGER.warn(()->message);
	}
}
