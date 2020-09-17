package tv.blackarrow.cpp.managers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public final class DataManagerFactory {

    private static final Logger LOGGER = LogManager.getLogger(DataManagerFactory.class);

    // all users should use a single instance with the current implementation
    // being an in-memory hash map with persistent backing
    private static DataManager instance = null;

    private DataManagerFactory() {}

    /**
     * Returns the correct instance of the data manager that the caller should
     * use. Currently there is a single instance of a Couchbase implementation.
     * If an exception occurs getting the instance the problem will be logged
     * and null will be returned. No exception is re-thrown in this case to
     * reduce the amount of exception handling logic that will be needed by
     * callers, especially since this scenario is likely unrecoverable.
     *
     * @return Instance of the data manager that should be used.
     */
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            LOGGER.debug(()->"Creating Couchbase data manager implementation instance");
            instance = new DataManagerCouchbaseImpl();
            int expirationSeconds = CppConfigurationBean.getInstance().getDefaultDataExpirationSeconds();
            instance.setDefaultDataExpirationSeconds(expirationSeconds);
        }
        return instance;
    }

    /**
     * Use this Datamanger to access SCTE 224 specific data. Nothing special just you will find all the 224 related methods at one place.
     * @return
     */
    public static SCTE224DataManager getSCTE224DataManager() {
    	return SCTE224DataManagerImplV1.getInstance();
    }

    public static SchedulelessAltEventDataManager getSchedulelessAltEventDataManager() {
    	return SchedulelessAltEventDataManagerImpl.getInstance();
    }
}
