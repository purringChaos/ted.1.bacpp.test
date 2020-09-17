package tv.blackarrow.cpp.cb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.core.config.ConfigurationException;
import com.couchbase.client.core.env.KeyValueServiceConfig;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.BucketDoesNotExistException;

import tv.blackarrow.cpp.setting.SettingUtils;
import tv.blackarrow.cpp.utils.CppConstants;


public class CouchbaseUtil {
	
	static private final Logger LOGGER = LogManager.getLogger(CouchbaseUtil.class.getName());

	/*
	 * Couchbase client API upgrade from 1.1.8 to 2.1.4
	 */
	public static final Cluster CLUSTER = getCluster();

    // Default RetryStrategy of Builder is BestEffortRetryStrategy
	private static CouchbaseCluster getCluster() {
		CouchbaseCluster couchbaseCluster = null;
		try{
			//10000ms = 10s, default is 5s
			CouchbaseEnvironment env = null;
			int passedKVEndPoints = SettingUtils.getCouchbaseKvEndpointse();
			if (passedKVEndPoints == 1) {
				LOGGER.debug(()->"Starting ESS Node...");
				env = DefaultCouchbaseEnvironment.builder().connectTimeout(CppConstants.COUCHBASE_CLUSTER_CONNECTION_TIMEOUT).build();
			} else {
				env = DefaultCouchbaseEnvironment.builder().keyValueServiceConfig(KeyValueServiceConfig.create(SettingUtils.getCouchbaseKvEndpointse()))
						.connectTimeout(CppConstants.COUCHBASE_CLUSTER_CONNECTION_TIMEOUT).build();
			}

			couchbaseCluster = CouchbaseCluster.create(env,Arrays.asList(CppConstants.COUCHBASE_CLUSTER_IPS.split(",")));
			couchbaseCluster.authenticate(CppConstants.COUCHBASE_CLUSTER_BUCKET_USER, CppConstants.COUCHBASE_CLUSTER_BUCKET_PASSWORD);
		}catch(ConfigurationException ce){
			LOGGER.debug(()->"ConfigurationException occured while creating couchbase cluster connection.");
			LOGGER.fatal(()->ce.getMessage() , ce);
		}catch(Exception e){
			LOGGER.fatal(()->e.getMessage() , e);
		}
		return couchbaseCluster;
		
	}
	
	
	static private CouchbaseUtil instance = new CouchbaseUtil();
	
	public static CouchbaseUtil getInstance() {
		return instance;
	}
	
	private HashMap <String, Bucket> cbBucketInstances = new HashMap <String, Bucket>();
	private Timer timer = null;
	
	private CouchbaseUtil() {
			// initialize CouchbaseClient to use log4j logging
			//Properties systemProperties = System.getProperties();
		    //systemProperties.put("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
		    //System.setProperties(systemProperties);
	}
	
	public Bucket getCouchbaseClient(String bucketName) {
		Bucket bucket = cbBucketInstances.get(bucketName);
		if (bucket == null) {
			synchronized (this){
				// only synchronized when CouchbaseClient for this bucket is not initialized yet
				// so that to deal with the potential race condition on Couchbase Client initialization
				bucket = cbBucketInstances.get(bucketName);
				if (bucket == null) {
					// this is to prevent the duplication on initialization
					// for all the following Couchbase Client initialization requests
					try{
						bucket = CLUSTER.openBucket(bucketName);
						cbBucketInstances.put(bucketName, bucket);
					}catch(BucketDoesNotExistException be){
						LOGGER.debug(()->"Bucket does not exist in couchbase.");
						LOGGER.fatal(()->be.getMessage() , be);
					}catch(Exception e){
						LOGGER.fatal(()->e.getMessage() , e);
					}
				}
			}
		}
		return bucket;
	}
}
