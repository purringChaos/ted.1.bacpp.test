package tv.blackarrow.cpp.managers;

import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.RequestCancelledException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.datastructures.MutationOptionBuilder;
import com.couchbase.client.java.document.JsonArrayDocument;
import com.couchbase.client.java.document.StringDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.util.retry.RetryBuilder;
import com.couchbase.client.java.util.retry.RetryWhenFunction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import tv.blackarrow.cpp.cb.CouchbaseUtil;
import tv.blackarrow.cpp.components.signalstate.model.SignalStateModel;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.RuntimeEnvironmentState;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.model.ServerConfig;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.SegmentType;

//TODO This class still has some basic functionality that is used by SCTE-224 classes, 224Classes must use SCTE224DataManager. Extract out common functionalities.
public class DataManagerCouchbaseImpl implements DataManager {

    private static final Logger LOGGER = LogManager.getLogger(DataManagerCouchbaseImpl.class);

	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	@SuppressWarnings("unchecked")
	private static final RetryWhenFunction RETRY_POLICY = 
			RetryBuilder.anyOf( TimeoutException.class, 
								TemporaryFailureException.class, 
								RequestCancelledException.class, 
								BackpressureException.class,
								CASMismatchException.class)
			.delay(Delay.exponential(TimeUnit.MILLISECONDS, 50))
			.max(10)//Increase the Retry specially would be useful for BackPressureException
			.build();
	public static final String AP_SCHEDULELESS_ALTEVENT_LEDGER_NAME_SPACE = "APSLE:";

    // Namespace values for each type of object. These must be unique to ensure
    // that different types of objects that might have the same key value will
    // not collide in the shared map. This will definitely happen in the case of
    // confirmed placement opportunities since one of the PO key values will be
    // used as the signal ID.
    private static final String ACQUISITION_POINT_NAMESPACE = "A:";
    private static final String BLACKOUT_EVENT_NAMESPACE = "B:";
    private static final String BLACKOUT_EVENT_COMPILATION_TIME_NAMESPACE = "BC:";

    // confirmed blackout will share the same name space as confirmed PO space, so that MCC component can process blackout and po in the same way
    private static final String CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE = "C:";
    private static final String CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_ID_NAMESPACE = "C:";

    private static final String CONFIRMED_BLACKOUT_EVENT_SIGNAL_TIME_NAMESPACE = "BET:";
    private static final String CONFIRMED_BLACKOUT_ACQUISITION_POINT_EVENT_SIGNAL_NAMESPACE = "CBAP:";
    private static final String CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_TIME_NAMESPACE = "T:";
    private static final String LOADER_CURSOR_NAMESPACE = "L:";
    private static final String PLACEMENT_OPPORTUNITY_NAMESPACE = "O:";
    private static final String RUNTIME_ENVIRONMENT_STATE_NAMESPACE = "RUNTIME_ENVIRONMENT_STATE";
    private static final String SIGNAL_PROCESSOR_CURSOR_NAMESPACE = "S:";
    private static final String LOCK_NAMESPACE = "K:";
    private static final String ACT_FEED_LIST = "ACT_FEED_LIST";

    // out-of-band notification related
    private static final String BLACKOUT_EVENT_RECORD_NAMESPACE = "BER:";
    private static final String BLACKOUT_EVENT_RECORD_ID_NAMESPACE = "BRI:";
    public static final String OUT_OF_BAND_RECORD_NAMESPACE = "OFB:";
    public static final String HOSTED_NOTIFICTION_RECORD_NAMESPACE = "NOTE:";
    public static final String FEED_ACQUISITION_POINT_MAP = "FAP:";
    public static final String RUNTIME_NOTIFICATION_NAMESPACE = "RN:";
	private static final String NOTIFICATION_CONFIRMED_SIGNAL_NAME_SPACE = "APCS:";
	private static final String REALTIME_DELETED_EVENTS = "RDE:";

	private static final String NAME_SPACE_VALUE_SEPARATOR = ":";

    public static final String IN_POINT_SIGNAL = "IN:";

    private static final String BLACKOUT_MCC_START_TIME = "BST:";

    //key For SCC Request Fields.
    private static final String SCC_REQUEST_FIELDS_NAMESPACE = "SCTE35SD:";

    //Locks expiration time.
    private static final int LOCK_KEY_REMOVE_WAIT_TIME_IN_MILLIS = 5 * 60 * 1000; //1 Hour (chosen randomly to be a bit large value.)

    //active data center
    public static final String ACTIVE_DATA_CENTER = "ACTIVE_DATA_CENTER";
    
    // gson is thread safe so we only need one instance to use everywhere
    private static final Gson gson = new Gson();

    // default setting is to not expire any data automatically
    private int defaultDataExpirationSeconds = 0;

    // if threshold values in milliseconds to compare DB query/update time
    private static final int DB_OP_TIME_THRESHOLD = 150;
   
    public DataManagerCouchbaseImpl() {
        warmUp();
    }

    private void warmUp() {
        // to reduce latency during requests, we will run every object through gson
        // one time during initialization
        gson.fromJson(gson.toJson(new AcquisitionPoint()), AcquisitionPoint.class);
        gson.fromJson(gson.toJson(new LoaderCursor()), LoaderCursor.class);
        gson.fromJson(gson.toJson(new SignalProcessorCursor()), SignalProcessorCursor.class);
        gson.fromJson(gson.toJson(new PlacementOpportunity()), PlacementOpportunity.class);
        gson.fromJson(gson.toJson(new ConfirmedPlacementOpportunity()), ConfirmedPlacementOpportunity.class);
        gson.fromJson(gson.toJson(new RuntimeEnvironmentState()), RuntimeEnvironmentState.class);
        gson.fromJson(gson.toJson(new BlackoutEvent()), BlackoutEvent.class);
        gson.fromJson(gson.toJson(new HashMap<String, String>()),new TypeToken<Map<String, String>>(){}.getType());
        gson.fromJson(gson.toJson(new HashSet<String>()), new TypeToken<Set<String>>(){}.getType());
        gson.fromJson(gson.toJson(new ArrayList<BlackoutEvent>()), new TypeToken<ArrayList<BlackoutEvent>>(){}.getType());
        gson.fromJson(gson.toJson(new ServerConfig()), ServerConfig.class);
    }

    private static Bucket getClient() {
        return CouchbaseUtil.getInstance().getCouchbaseClient(CppConstants.COUCHBASE_CLUSTER_LINEAR_BUCKET_NAME);
    }

    private int getExpiration() {
        if (defaultDataExpirationSeconds == 0) {
            return 0;
        }

        // Couchbase supports two formats for expiration, just use epoch for everything to keep it simple
        return ((int) (System.currentTimeMillis() / 1000)) + defaultDataExpirationSeconds;
    }

    private StringDocument add(final String key, int expirationSeconds, String value) {
        int expiration = ((int) (System.currentTimeMillis() / 1000)) + expirationSeconds;
        long start = System.currentTimeMillis();
        StringDocument savedDocument = null;
        try {
        	savedDocument = getClient().async().insert(StringDocument.create(key, expiration, value)).toBlocking().single();
        } catch (Throwable throwable) {
        	if(throwable instanceof DocumentAlreadyExistsException){
			    LOGGER.debug(()->"Document already exist with key \"" + key + "\"");
			}else{
				String msg = "System are not able to create a document due to issue = "+ throwable.getMessage();
	        	LOGGER.debug(()->msg);
	            throw new RuntimeException(msg, throwable);
			}
        }

        long duration = System.currentTimeMillis() - start;

        // record anything longer than 150 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow add for key \"" + key + "\" duration: " + duration + "ms");
        }
        return savedDocument;
    }

	@Override
    public void set(final String key, final String value, final int expirationTime) {
    	long start = System.currentTimeMillis();
    	StringDocument document = StringDocument.create(key, expirationTime, value);

        final CountDownLatch latch = new CountDownLatch(1);

    	getClient().async()
        	.upsert(document)
        	.retryWhen(RETRY_POLICY)
        	.doOnError(new Action1<Throwable>() {
				@Override
				public void call(Throwable exception) {
		        	String msg = "ESS was not able to update a document in the persistene store due to the following issue = " + exception.getMessage();
		        	LOGGER.error(()->msg);
				}
			})
        	.doAfterTerminate(new Action0() {
				@Override
				public void call() {
					latch.countDown();
				}
        	}).subscribe();

        try {
			latch.await();
		} catch (InterruptedException e) {
			String msg = "Error setting item with key \"" + key + "\"";
            LOGGER.error(()->msg, e);
            throw new RuntimeException(msg, e);
		}

        long duration = System.currentTimeMillis() - start;

        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow set for key \"" + key + "\" duration: " + duration + "ms");
        }
    }
    
    @Override
	public void set(final String key, final String value) {
    	set(key, value, getExpiration());
    }

    @Override
	public String get(String key) {

    	final List<String> document = new LinkedList<String>();
        final CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        getClient()
        .async()
        .get(key, StringDocument.class)
        .retryWhen(RETRY_POLICY)
        .doAfterTerminate(new Action0() {
			@Override
			public void call() {
				latch.countDown();
			}
    	}).subscribe(new Action1<StringDocument>() {
            @Override
            public void call(StringDocument stringDocument) {
            	if(stringDocument!=null){
            		document.add(stringDocument.content());
            	}
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
        	LOGGER.error(()->e.getMessage());
        }
        // record anything longer than 50 milliseconds as a warning
        long duration = System.currentTimeMillis() - start;
        if (duration >  DB_OP_TIME_THRESHOLD) {
            LOGGER.debug(()->"Slow get for key \"" + key + "\" duration: " + duration + "ms");
        }
        if(document.isEmpty()){
        	//logger.debug("No document found for key \"" + key + "\"");
        	return null;
        } else {
        	return document.get(0);
        }
    }

    @Override
    public StringDocument getDocument(String key) {
        long start = System.currentTimeMillis();
        StringDocument document = getClient().get(key,StringDocument.class);
        long duration = System.currentTimeMillis() - start;

        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow gets for key \"" + key + "\" duration: " + duration + "ms");
        }
        return document;
    }

    private boolean cas(String key, Long casId, String value) {
        if (casId == null) {
            LOGGER.warn(()->"Attempted cas with no casId for key \"" + key + "\"");
            return false;
        }
        long start = System.currentTimeMillis();
        StringDocument document = StringDocument.create(key, getExpiration(), value, casId);
        try{
        	getClient().replace(document);
        }catch(CASMismatchException e){
        	LOGGER.warn(()->"CASMismatchException for key \"" + key);
        	return false;
        }
        long duration = System.currentTimeMillis() - start;
        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow cas for key \"" + key + "\" duration: " + duration + "ms");
        }
        return true;
    }

    @Override
    public void delete(String key) {
        long start = System.currentTimeMillis();
        try{
        	getClient().remove(key);
        }catch(DocumentDoesNotExistException e){
        	 if(DEBUG_ENABLED) {
        		 LOGGER.debug(()->"Delete did not have impact. Document was not present with key \"" + key + "\"");
        	 }
        }catch(Exception e){
        	 String msg = "Error deleting item with key \"" + key + "\"";
             LOGGER.error(()-> msg, e);
             throw new RuntimeException(msg, e);
        }

        long duration = System.currentTimeMillis() - start;

        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow delete for key \"" + key + "\" duration: " + duration + "ms");
        }
    }

    @Override
    public AcquisitionPoint getAcquisitionPoint(String acquisitionPointIdentity) {
    	return SCCMCCThreadLocalCache.get(this, ACQUISITION_POINT_NAMESPACE + acquisitionPointIdentity, AcquisitionPoint.class);
    }

    @Override
	public void putAcquisitionPoint(AcquisitionPoint acquisitionPoint) {
		if (DEBUG_ENABLED) {
			StringBuilder sb = new StringBuilder();
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				sb.append(ste).append("-->");
			}
			LOGGER.debug(()->"acquisitionPoint changing to " + acquisitionPoint + "Path: " + sb);
		}
		SCCMCCThreadLocalCache.put(this, ACQUISITION_POINT_NAMESPACE + acquisitionPoint.getAcquisitionPointIdentity(), acquisitionPoint);
    }

    @Override
    public void deleteAcquisitionPoint(String acquisitionPointIdentity) {
    	SCCMCCThreadLocalCache.delete(this, ACQUISITION_POINT_NAMESPACE + acquisitionPointIdentity);
    	SCCMCCThreadLocalCache.delete(this, SIGNAL_PROCESSOR_CURSOR_NAMESPACE + acquisitionPointIdentity);
    }

    @Override
    public LoaderCursor getLoaderCursor(String feedExternalRef) {
        StringDocument document = getDocument(LOADER_CURSOR_NAMESPACE + feedExternalRef);
        if (document == null) {
            return null;
        }
        LoaderCursor loaderCursor = gson.fromJson(document.content().toString(), LoaderCursor.class);
        loaderCursor.setCasId(document.cas());
        return loaderCursor;
    }

    @Override
    public void putLoaderCursor(LoaderCursor loaderCursor) {
        // don't store any cas id value as part of the json
        loaderCursor.setCasId(null);
        set(LOADER_CURSOR_NAMESPACE + loaderCursor.getFeedExternalRef(), gson.toJson(loaderCursor));
    }

    @Override
    public boolean casLoaderCursor(LoaderCursor loaderCursor) {
        // get the cas id and clear it out so it does not get stored as part
        // of the actual json string.  note: this means you can only try cas
        // one time on a value which is true with cas in general anyways.
        Long casId = loaderCursor.getCasId();
        loaderCursor.setCasId(null);
        String json = gson.toJson(loaderCursor);
        return cas(LOADER_CURSOR_NAMESPACE + loaderCursor.getFeedExternalRef(), casId, json);
    }

    @Override
    public void deleteLoaderCursor(String feedExternalRef) {
        delete(LOADER_CURSOR_NAMESPACE + feedExternalRef);
    }

    @Override
    public SignalProcessorCursor getSignalProcessorCursor(String acquisitionPointIdentity) {
    	StringDocument document = getDocument(SIGNAL_PROCESSOR_CURSOR_NAMESPACE + acquisitionPointIdentity);
        if (document == null) {
        	if(getAcquisitionPoint(acquisitionPointIdentity) != null){
        		SignalProcessorCursor cursor = new SignalProcessorCursor();
        		cursor.setAcquisitionPointIdentity(acquisitionPointIdentity);
        		cursor.setNextPOKeyByZone(new HashMap<String, String>());
        		putSignalProcessorCursor(cursor);
        		return cursor;
        	}
        	return null;
        }
        SignalProcessorCursor signalProcessorCursor = gson.fromJson(document.content().toString(), SignalProcessorCursor.class);
        signalProcessorCursor.setCasId(document.cas());
        return signalProcessorCursor;

    }

    @Override
    public void putSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
        // don't store any cas id value as part of the json
        signalProcessorCursor.setCasId(null);
        set(SIGNAL_PROCESSOR_CURSOR_NAMESPACE + signalProcessorCursor.getAcquisitionPointIdentity(),
                gson.toJson(signalProcessorCursor));
    }

    @Override
    public boolean casSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
        // get the cas id and clear it out so it does not get stored as part
        // of the actual json string.  note: this means you can only try cas
        // one time on a value which is true with cas in general anyways.
        Long casId = signalProcessorCursor.getCasId();
        signalProcessorCursor.setCasId(null);
        String json = gson.toJson(signalProcessorCursor);
        return cas(SIGNAL_PROCESSOR_CURSOR_NAMESPACE + signalProcessorCursor.getAcquisitionPointIdentity(), casId, json);
    }

    @Override
    public void mergeSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
        // going to do a compare-and-set so we need to handle retrying
        int retries = 0;
        while (true) {
            SignalProcessorCursor persistedValue = getSignalProcessorCursor(signalProcessorCursor.getAcquisitionPointIdentity());
            if (persistedValue == null) {
                putSignalProcessorCursor(signalProcessorCursor);
                break;
            } else {
                Map<String, String> nextPOKeyByZone = signalProcessorCursor.getNextPOKeyByZone();
                Map<String, String> oldNextPOKeyByZone = persistedValue.getNextPOKeyByZone();
                Set<String> zoneSet = nextPOKeyByZone.keySet();
                boolean modified = false;
                for (String zoneExtRef : zoneSet) {
                    if (oldNextPOKeyByZone.get(zoneExtRef) == null) {
                        oldNextPOKeyByZone.put(zoneExtRef, nextPOKeyByZone.get(zoneExtRef));
                        modified = true;
                    }
                }
                if (!modified) {
                    break;
                }
                if (casSignalProcessorCursor(persistedValue)) {
                    break;
                }
            }
            retries++;
            if (retries > 10) {
                throw new RuntimeException("Unable to update signal processor cursor for acquisition point \""
                        + persistedValue.getAcquisitionPointIdentity() + "\" after multiple retries");
            }
            LOGGER.debug(()->"Compare-and-set failed while updating signal processor cursor for acquisition point \""
                    + persistedValue.getAcquisitionPointIdentity() + "\", retrying");
        }
    }

    @Override
    public void deleteSignalProcessorCursor(String acquisitionPointIdentity) {
        delete(SIGNAL_PROCESSOR_CURSOR_NAMESPACE + acquisitionPointIdentity);
    }

    @Override
    public PlacementOpportunity getPlacementOpportunity(String POKey) {
    	 StringDocument document = getDocument(PLACEMENT_OPPORTUNITY_NAMESPACE + POKey);
    	 if (document == null) {
             return null;
         }
        PlacementOpportunity placementOpportunity = gson.fromJson(document.content().toString(), PlacementOpportunity.class);
        placementOpportunity.setCasId(document.cas());
        return placementOpportunity;

    }

    @Override
    public void putPlacementOpportunity(PlacementOpportunity placementOpportunity) {
        // don't store any cas id value as part of the json
        placementOpportunity.setCasId(null);
        set(PLACEMENT_OPPORTUNITY_NAMESPACE + placementOpportunity.getPOKey(), gson.toJson(placementOpportunity));
    }

    @Override
    public boolean casPlacementOpportunity(PlacementOpportunity placementOpportunity) {
        // get the cas id and clear it out so it does not get stored as part
        // of the actual json string.  note: this means you can only try cas
        // one time on a value which is true with cas in general anyways.
        Long casId = placementOpportunity.getCasId();
        placementOpportunity.setCasId(null);
        String json = gson.toJson(placementOpportunity);
        return cas(PLACEMENT_OPPORTUNITY_NAMESPACE + placementOpportunity.getPOKey(), casId, json);
    }

    @Override
    public void deletePlacementOpportunity(String POKey) {
        delete(PLACEMENT_OPPORTUNITY_NAMESPACE + POKey);
    }

    @Override
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String signalId) {
        String json = get(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_ID_NAMESPACE + signalId);
        return gson.fromJson(json, ConfirmedPlacementOpportunity.class);
    }

    @Override
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String acquisitionPointIdentity, long utcSignalTime) {
        String json = get(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_TIME_NAMESPACE + acquisitionPointIdentity + ":" + utcSignalTime);
        return gson.fromJson(json, ConfirmedPlacementOpportunity.class);
    }


    @Override
    public void putConfirmedPlacementOpportunity(ConfirmedPlacementOpportunity confirmedPlacementOpportunity) {
        String json = gson.toJson(confirmedPlacementOpportunity);

        // need to store this confirmed placement opportunity under two
        // keys to support looking it up both ways.
        set(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_ID_NAMESPACE + confirmedPlacementOpportunity.getSignalId(), json);
        set(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_TIME_NAMESPACE + confirmedPlacementOpportunity.getAcquisitionPointIdentity()
                + ":" + confirmedPlacementOpportunity.getUtcSignalTime(), json);
    }

    @Override
    public void deleteConfirmedPlacementOpportunity(String signalId) {
        String json = get(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_ID_NAMESPACE + signalId);
        if (json == null) {
            return;
        }
        ConfirmedPlacementOpportunity confirmedPlacementOpportunity = gson.fromJson(json, ConfirmedPlacementOpportunity.class);

        // this entity is stored under two different keys so delete both of them
        if (confirmedPlacementOpportunity != null) {
            delete(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_ID_NAMESPACE + signalId);
            delete(CONFIRMED_PLACEMENT_OPPORTUNITY_BY_SIGNAL_TIME_NAMESPACE
                    + confirmedPlacementOpportunity.getAcquisitionPointIdentity() + ":"
                    + confirmedPlacementOpportunity.getUtcSignalTime());
        }
    }

    @Override
    public ConfirmedPlacementOpportunity getConfirmedBlackoutCommonAcrossAFeedAPs(String signalId) {
    	return SCCMCCThreadLocalCache.get(this, CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE + signalId, ConfirmedPlacementOpportunity.class);
    }


    @Override
    public ConfirmedPlacementOpportunity getConfirmedBlackoutForGivenAP(String acquisitionPointIdentity, String blackoutSignalId) {
    	return SCCMCCThreadLocalCache.get(this, CONFIRMED_BLACKOUT_ACQUISITION_POINT_EVENT_SIGNAL_NAMESPACE + acquisitionPointIdentity + ":" + blackoutSignalId, ConfirmedPlacementOpportunity.class);
    }

    @Override
    public void putConfirmedBlackout(ConfirmedPlacementOpportunity confirmedPlacementOpportunity) {
        // need to store this confirmed placement opportunity under two keys to support looking it up both ways.
    	SCCMCCThreadLocalCache.put(this, CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE + confirmedPlacementOpportunity.getSignalId(), confirmedPlacementOpportunity);
    	SCCMCCThreadLocalCache.put(this, CONFIRMED_BLACKOUT_ACQUISITION_POINT_EVENT_SIGNAL_NAMESPACE + confirmedPlacementOpportunity.getAcquisitionPointIdentity()
    			+ ":" + confirmedPlacementOpportunity.getSignalId(), confirmedPlacementOpportunity);
    }

    @Override
    public void putConfirmedBlackout(ConfirmedPlacementOpportunity confirmedPlacementOpportunity,String signalId) {
        // need to store this confirmed placement opportunity under two keys to support looking it up both ways.
    	SCCMCCThreadLocalCache.put(this, CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE + signalId, confirmedPlacementOpportunity);
    	SCCMCCThreadLocalCache.put(this, CONFIRMED_BLACKOUT_ACQUISITION_POINT_EVENT_SIGNAL_NAMESPACE + confirmedPlacementOpportunity.getAcquisitionPointIdentity()
    			+ ":" + signalId, confirmedPlacementOpportunity);
    }


    @Override
    public void deleteConfirmedBlackout(String signalId) {
    	// this entity is stored under two different keys so delete both of them
    	ConfirmedPlacementOpportunity confirmedPlacementOpportunity = SCCMCCThreadLocalCache.get(this, CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE + signalId,
    			ConfirmedPlacementOpportunity.class);
    	if(confirmedPlacementOpportunity != null){
        	SCCMCCThreadLocalCache.delete(this, CONFIRMED_BLACKOUT_EVENT_SIGNAL_ID_NAMESPACE + signalId);
        	SCCMCCThreadLocalCache.delete(this, CONFIRMED_BLACKOUT_ACQUISITION_POINT_EVENT_SIGNAL_NAMESPACE + confirmedPlacementOpportunity.getAcquisitionPointIdentity() + ":"
                    + confirmedPlacementOpportunity.getSignalId());
    	}
    }


    @Override
    public RuntimeEnvironmentState getRuntimeEnvironmentState() {
        String json = get(RUNTIME_ENVIRONMENT_STATE_NAMESPACE);
        return gson.fromJson(json, RuntimeEnvironmentState.class);
    }

    @Override
    public void putRuntimeEnvironmentState(RuntimeEnvironmentState runtimeEnvironmentState) {
        set(RUNTIME_ENVIRONMENT_STATE_NAMESPACE, gson.toJson(runtimeEnvironmentState));
    }

    @Override
    public void setDefaultDataExpirationSeconds(int expirationSeconds) {
        LOGGER.debug(()->"Setting default data expiration time to " + expirationSeconds + " seconds");
        defaultDataExpirationSeconds = expirationSeconds;
    }

    @Override
	public boolean lock(String lockName, int expirationSeconds) {
		StringDocument cbDocument = add(LOCK_NAMESPACE + lockName, expirationSeconds, "LOCKED");
		return cbDocument != null ? true : false;
	}

  
    @Override
    public void unlock(String lockName) {
        delete(LOCK_NAMESPACE + lockName);
    }

    /*
     * (non-Javadoc)
     * @see tv.blackarrow.cpp.managers.DataManager#isLockTaken(java.lang.String)
     */
    @Override
    public boolean isLockTaken(String lockName) {
       return get(LOCK_NAMESPACE + lockName) != null;
    }

    private String getBlackoutEventKey(String feedName) {
    	return BLACKOUT_EVENT_NAMESPACE + feedName;
    }

    private String getBlackoutEventCompilationTimeKey(String feedName) {
    	return BLACKOUT_EVENT_COMPILATION_TIME_NAMESPACE + feedName;
    }

    @Override
	public void putBlackoutEvents(String feedName, String blackoutEvents, long compilationTime) {
        set(getBlackoutEventKey(feedName), blackoutEvents);
        set(getBlackoutEventCompilationTimeKey(feedName), String.valueOf(compilationTime));
	}

    @Override
	public void putBlackoutEvents(String feedName, String blackoutEvents) {
        set(getBlackoutEventKey(feedName), blackoutEvents);
	}

	@Override
	public long getBlackoutEventCompilationTime(String feedName) {
        String value = get(getBlackoutEventCompilationTimeKey(feedName));

        if ((value != null) && !value.isEmpty()) {
	        Long time = Long.parseLong(value);
			return time.longValue();
        } else {
			return 0;
		}
	}

	@Override
	public ArrayList<BlackoutEvent> getAllBlackoutEventsOnFeed(String feedName)  {
		String value = get(getBlackoutEventKey(feedName));
		if ((value != null) && !value.isEmpty()) {
			try {
				ArrayList<BlackoutEvent> events= gson.fromJson(value, new TypeToken<ArrayList<BlackoutEvent>>(){}.getType());
				return events;
			} catch (Exception ex) {
				LOGGER.fatal(()->"Fail to retrieve blackout even for " + feedName, ex);
				throw new RuntimeException("Fail to convert blackout event", ex);
			}
		} else {
			LOGGER.debug(()->"No blackout event not found for feed : " + feedName);
			return new ArrayList<BlackoutEvent>(0);
		}
	}

	@Override
	public void putSingleBlackoutEvent(final String signalId, final BlackoutEvent event) {
		SCCMCCThreadLocalCache.put(this, BLACKOUT_EVENT_RECORD_NAMESPACE + signalId, event, BlackoutEvent.class);
	}

	@Override
	public BlackoutEvent getSingleBlackoutEvent(String signalId) {
		return SCCMCCThreadLocalCache.get(this, BLACKOUT_EVENT_RECORD_NAMESPACE + signalId, BlackoutEvent.class);
	}

	@Override
	public void putBlackoutEventByEventId(String eventlId, final BlackoutEvent event) {
		SCCMCCThreadLocalCache.put(this, BLACKOUT_EVENT_RECORD_ID_NAMESPACE + eventlId, event);
	}

	@Override
	public BlackoutEvent getBlackoutEventByEventId(String eventId) {
		return SCCMCCThreadLocalCache.get(this, BLACKOUT_EVENT_RECORD_ID_NAMESPACE + eventId, BlackoutEvent.class);
	}

	@Override
	public void putRealtimeNotificationStatus(String eventId, String acquistionPointIdentity, long utcStartTime, long utcEndtime, String status) {
		set(RUNTIME_NOTIFICATION_NAMESPACE + acquistionPointIdentity + ":" + eventId + ":" + utcStartTime + ":" + utcEndtime, status);
	}

	@Override
	public String getReatimeNotificationStatus(String eventId, String acquistionPointIdentity, long utcStartTime, long utcEndtime) {
		return get(RUNTIME_NOTIFICATION_NAMESPACE + acquistionPointIdentity + ":" + eventId + ":" + utcStartTime + ":" + utcEndtime);
	}

	@Override
	public void putFeedToAcquistionMap(String feedId,
			Set<String> AcquisitionPointSet) {
		set(FEED_ACQUISITION_POINT_MAP + feedId, gson.toJson(AcquisitionPointSet));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getFeedToAcquistionMap(String feedId) {
		Set<String> acqSet = new HashSet<String>();
		String acqStr = get(FEED_ACQUISITION_POINT_MAP + feedId); // json format
		if(acqStr != null) {
			acqSet = gson.fromJson(acqStr, Set.class);
		} else {
			LOGGER.warn(()->"Cannot find the acquisition point set related to the feed " + feedId);
		}
		return acqSet;
	}

    @Override
    public void putActiveFeedList(String feedlist) {
    	set(ACT_FEED_LIST, feedlist);
    }

	@Override
	public String getActiveFeedList() {
		return get(ACT_FEED_LIST);

	}

	@Override
	public void putBlackoutEvent(BlackoutEvent event){
		if(event == null) {return;}
		putBlackoutEvent(event.getEventId(), event.getSignalId(), event);
	}

	@Override
	public void putBlackoutEvent(final String eventId, final String signalId, final BlackoutEvent event){
		putBlackoutEventByEventId(eventId, event);
	    putSingleBlackoutEvent(signalId, event);
	}

	@Override
	public void deleteBlackoutEvent(BlackoutEvent event) {
		if(event == null){return;}
		deleteConfirmedBlackout(event.getSignalId());
		SCCMCCThreadLocalCache.delete(this, BLACKOUT_EVENT_RECORD_ID_NAMESPACE + event.getEventId());
		SCCMCCThreadLocalCache.delete(this, BLACKOUT_EVENT_RECORD_NAMESPACE + event.getSignalId());
	}

	@Override
	public String getHostedNotificationStatus(String eventSignalId, String eventState) {
		return get(HOSTED_NOTIFICTION_RECORD_NAMESPACE+eventSignalId+"_"+eventState);
	}

	@Override
	public void putHostedNotificationStatus(String eventSignalId, String eventState, String status) {
		set(HOSTED_NOTIFICTION_RECORD_NAMESPACE + eventSignalId+"_"+eventState, status);
	}

	@Override
	public void removeHostedNotificationStatus(String eventSignalId, String eventState) {
		delete(HOSTED_NOTIFICTION_RECORD_NAMESPACE + eventSignalId + "_" + eventState);
	}

	@Override
	public void putInPointsSignal(String inPointSignalId, String confirmedCpoSignalId) {
		set(IN_POINT_SIGNAL + inPointSignalId, confirmedCpoSignalId);
	}

	@Override
	public String getInPointsSignal(String inPointSignalId) {
		return get(IN_POINT_SIGNAL + inPointSignalId);
	}


	@Override
	public long getBlackoutMccStartTime(String acquisitionPointTd, String signalId) {
		String startTime = get(BLACKOUT_MCC_START_TIME + acquisitionPointTd+ ":"+ signalId);
		if((startTime !=null) && !startTime.isEmpty()){
			try{
				return Long.parseLong(startTime);
			}catch(Exception e){}
		}

		return 0;
	}

	@Override
	public void putBlackoutMccStartTime(String acquisitionPointTd, String signalId, long startTime) {
		set(BLACKOUT_MCC_START_TIME + acquisitionPointTd+ ":"+ signalId, String.valueOf(startTime));
	}

	@Override
	public void putSegmentationDescriptor(String signalId, SegmentationDescriptor segmentationDescriptor){
		set(SCC_REQUEST_FIELDS_NAMESPACE + signalId, gson.toJson(segmentationDescriptor));
	}

	@Override
	public SegmentationDescriptor getSegmentationDescriptor(String signalId){
		 String json = get(SCC_REQUEST_FIELDS_NAMESPACE + signalId);
	     return gson.fromJson(json, SegmentationDescriptor.class);
	}

	@Override
	public StringDocument lock(String lockName) {
		StringDocument document = StringDocument.create(LOCK_NAMESPACE + lockName, getExpiration(), "LOCKED");
		try {
			return getClient().insert(document);
		} catch(DocumentAlreadyExistsException documentAlreadyExistsException){
			LOGGER.debug(()->"Couldn't take the lock as it is already taken by some other thread. Retry again.");
			return null;
		}
	}

	@Override
	public void unlock(StringDocument lockedDocument) {
		getClient().remove(lockedDocument, LOCK_KEY_REMOVE_WAIT_TIME_IN_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
    public List<AcquisitionPoint> getAcquisitionPoints(final Collection<String> acquisitionPointIds){
    	return Observable
    	        .from(acquisitionPointIds)
    	        .flatMap(new AsyncAcquisitionPointGetAction())
    	        .onErrorResumeNext(new AsyncGetErrorHandler())
    	        .map(new AsyncAcquisitionPointGetMapper())
    	        .onErrorResumeNext(new AsyncGetAcquisitionPointErrorHandler())
    	        .toList()
    	        .toBlocking()
    	        .single();
    }

	@Override
    public void putActiveDataCenter(String dataCenter) {
    	set(ACTIVE_DATA_CENTER, dataCenter);
    }

	@Override
	public String getActiveDataCenter() {
		return get(ACTIVE_DATA_CENTER);
	}

	@Override
	public ServerConfig getServerConfig(){
		ServerConfig serverConfig = null;
		String serverConfigJson = getActiveDataCenter();
		return StringUtils.isNotBlank(serverConfigJson) ? gson.fromJson(serverConfigJson, ServerConfig.class) : serverConfig;
	}

	@Override
	public boolean isServerInActiveDataCenter(){
		ServerConfig serverConfig = getServerConfig();
		return serverConfig == null ? true : serverConfig.getActiveDataCenter().equalsIgnoreCase(CppConfigurationBean.getInstance().getDataCenter());
	}

	class AsyncGetAction implements Func1<String, Observable<StringDocument>>{
		final String savedEntityNamespace;
		protected AsyncGetAction(String savedEntityNamespace){
			this.savedEntityNamespace = savedEntityNamespace;
		}
        @Override
        public Observable<StringDocument> call(final String id) {
            return getClient()
            		.async()
            		.get(savedEntityNamespace + id,StringDocument.class)
            		.onErrorResumeNext(new AsyncGetErrorHandler());
        }
    }

	class AsyncAcquisitionPointGetAction extends AsyncGetAction{
		protected AsyncAcquisitionPointGetAction() {
			super(ACQUISITION_POINT_NAMESPACE);
		}
	}

	class AsyncGetErrorHandler implements Func1<Throwable, Observable<StringDocument>>{
		@Override
		public Observable<StringDocument> call(Throwable exception) {
			LOGGER.debug(()->exception.getMessage());
			return Observable.empty();
		}
	}

	class AsyncGetAcquisitionPointErrorHandler implements Func1<Throwable, Observable<AcquisitionPoint>>{
		@Override
		public Observable<AcquisitionPoint> call(Throwable exception) {
			LOGGER.debug(()->exception.getMessage());
			return Observable.empty();
		}
	}

	class AsyncAcquisitionPointGetMapper implements Func1<StringDocument,AcquisitionPoint>{
		@Override
		public AcquisitionPoint call(StringDocument acquisitionPointJSONDocument) {
			if((acquisitionPointJSONDocument !=null) && StringUtils.isNotBlank(acquisitionPointJSONDocument.content())){
				return gson.fromJson(acquisitionPointJSONDocument.content(), AcquisitionPoint.class);
			}
			return null;
		}
	}

	@Override
	public void putAPConfirmedSignal(String apId, String signalId, String signalResponses) {
		SignalStateModel signalState = new SignalStateModel(signalId, signalResponses);
		set(NOTIFICATION_CONFIRMED_SIGNAL_NAME_SPACE + apId, gson.toJson(signalState));
	}

	@Override
	public SignalStateModel getLastConfirmedEvent(String apId) {
		String object = get(NOTIFICATION_CONFIRMED_SIGNAL_NAME_SPACE + apId);
		SignalStateModel signalState = StringUtils.isNotBlank(object) ? gson.fromJson(object, SignalStateModel.class) : null;
		return signalState;
	}
	
	@Override
	public void appendToQueue(final String queueName, final String queueElement, final int expirationTime) {
		long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(()->"Adding in " + queueName + " element " + queueElement);
		}
    	getClient().async()
    		.queuePush(queueName, queueElement, MutationOptionBuilder.builder().createDocument(true).expiry(expirationTime))
    		.retryWhen(RETRY_POLICY)
        	.doOnError(new Action1<Throwable>() {
				@Override
				public void call(Throwable exception) {
		        	LOGGER.error(() -> "ESS was not able to add element '"+ queueElement +"' in queue '" + queueName + "' in the persistene store due to the following issue = " + exception.getMessage());
				}
			})
        	.doAfterTerminate(new Action0() {
				@Override
				public void call() {
					latch.countDown();
				}
        	}).subscribe();

        try {
			latch.await();
		} catch (InterruptedException e) {
			LOGGER.error(() -> "ESS was not able to element '"+ queueElement +"' in queue '" + queueName + "' in the persistene store due to the following issue = " + e.getMessage());
            throw new RuntimeException("ESS was not able to element '"+ queueElement +"' in queue '" + queueName + "' in the persistene store due to the following issue = " + e.getMessage(), e);
		}

        long duration = System.currentTimeMillis() - start;

        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(() -> "Slow addition into queue with key \"" + queueName + "\" duration: " + duration + "ms. Element saved: " + queueElement);
        }
	}
	
	@Override
	public void appendToQueue(final String queueName, final String queueElement) {
		appendToQueue(queueName, queueElement, getExpiration());
	}
	
	

	@Override
	public String popFromQueue(final String queueName) {
		long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> document = new LinkedList<>();

    	getClient().async()
    		.queuePop(queueName, String.class)
    		.retryWhen(RETRY_POLICY)
        	.doAfterTerminate(latch::countDown)
        	.subscribe(
    			document::add, 
    			exception -> {
					if(exception instanceof DocumentDoesNotExistException) {
						//Just Ignore.
						LOGGER.trace(()-> "There was no queue present with the queue name: " + queueName+ ". Or the queue was empty.");
					} else if(exception instanceof CASMismatchException) {
						//Just Ignore.
						LOGGER.debug(()-> "Pop failed(due to concurrency), the couchbase client should retry(default 10 times) from : " + queueName+ ".");
					} else {
						LOGGER.error(() -> "ESS was not able to pop the element from queue '" + queueName + "' from the persistene store due to the following issue: ", exception);
					}
                }                   
        	);

        try {
			latch.await();
		} catch (InterruptedException e) {
			LOGGER.error(() -> "ESS was not able to pop the element from queue '" + queueName + "' from the persistene store due to the following issue = " + e.getMessage());
            throw new RuntimeException("ESS was not able to pop the element from queue '" + queueName + "' from the persistene store due to the following issue = " + e.getMessage(), e);
		}

        final long duration = System.currentTimeMillis() - start;

        if(document.isEmpty() || document.get(0) == null){
        	return null;
        } else {
            // record anything longer than 50 milliseconds as a warning
            if (duration > DB_OP_TIME_THRESHOLD) {
                LOGGER.warn(()-> "Slow fetch from the queue with key \"" + queueName + "\" duration: " + duration + "ms. Element fetched: " + document.get(0));
            }
            String element = document.get(0);
            if(LOGGER.isDebugEnabled() && element!=null) {
            	LOGGER.debug(()-> "Processing message from queue: " + element);
            }
        	return element;
        } 
	}
	
	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#unscheduleNotification(java.lang.String, java.lang.String)
	 */
	@Override
	public void unscheduleNotification(String queueName, String eventSignalId) {
		short maxRetries = 5;
		short retryCount = 0;
		if(queueName == null || queueName.isEmpty() || eventSignalId == null || eventSignalId.trim().isEmpty()) {
			return;
		}
		while(retryCount <= maxRetries) {
			try {
				
				final List<JsonArrayDocument> document = fetchTheLatestUSNQ(queueName);
		        
		      //2. Remove the notification message for the cancelled notification refs.
				if(document.isEmpty()) {
					LOGGER.debug(()-> "Queue " + queueName + " does not exists. So nothing to cancel schedule here.");
				} else {
					List<HashSet<String>> updatedNotificationBatches = new ArrayList<>();
					JsonArrayDocument notificationQueueJSONArrayDocument = document.get(0);
					if(notificationQueueJSONArrayDocument!=null) {
						JsonArray notificationBatches = notificationQueueJSONArrayDocument.content();
						if(notificationBatches != null) {
							notificationBatches.forEach(notificationBatch -> {
						    	  HashSet<String> thisBatchMessageRefs = gson.fromJson(notificationBatch.toString(), 
						    			  new TypeToken<HashSet<String>>(){}.getType());
								if (thisBatchMessageRefs != null) {
									//A given batch always contains the message refs from only one Media/Event. So as long as one matches, all of them
									// may be considered as matching or belonging to the same media.
									//No process should remove any job ending with #@#I (Immediate jobs), 
									//All immediate must be executed normally. So never unschedule it. Since added !thisBatchMessageRef.endsWith(NotificationServiceConfig.IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER) clause.
									if (thisBatchMessageRefs.stream().anyMatch(thisBatchMessageRef -> (thisBatchMessageRef.startsWith(eventSignalId)
											&& !thisBatchMessageRef.endsWith(NotificationServiceConfig.IMMEDIATE_HOSTED_JOB_NAME_IDENTIFIEER)))) {
										thisBatchMessageRefs.clear();
									}
									if (!thisBatchMessageRefs.isEmpty()) {
										updatedNotificationBatches.add(thisBatchMessageRefs);
									}
								}
						      }
							);
						}
						
						saveUSNQ(queueName, updatedNotificationBatches, notificationQueueJSONArrayDocument);
					}
				}
				return;
			} catch(Throwable ex) {
				if(ex instanceof CASMismatchException) {
					LOGGER.debug("Some other process has updated the same queue as this thread was processing. "
							+ "So fetching the queue again and retrying to update. Retry Count: " + ++retryCount);
				} else {
					LOGGER.warn(()-> "Following exception occured while persisting the updated notifcation queue: " + queueName + ".", ex);
					return;
				}
			}
		}
	}

	@Override
	public void forcefullyUnscheduleAllNotifications(String queueName) {
		LOGGER.debug(()-> "Removing Queue " + queueName + " from the persistence store.");
		
		short maxRetries = 5;
		short retryCount = 0;
		while(retryCount <= maxRetries) {
			try {
				//1. Fetch the latest queue from Couchbase.
				final List<JsonArrayDocument> document = new LinkedList<>();
		        final CountDownLatch latch = new CountDownLatch(1);
		        getClient()
		        	.async()
		        	.get(queueName, JsonArrayDocument.class)
		        	.doAfterTerminate(latch::countDown)
		        	.subscribe(document::add);
		        latch.await();
		        //2. Remove it if it existed.
		        if(document.isEmpty()) {
					LOGGER.debug(()-> "Queue " + queueName + " does not exists. So nothing to remove here.");
				} else {
			        try{
			        	getClient().remove(document.get(0));
			        }catch(DocumentDoesNotExistException e){
			        	LOGGER.debug(()->"Delete did not have impact. Document was not present with key \"" + queueName + "\"");
			        }
				}
			} catch(Throwable ex) {
				if(ex instanceof CASMismatchException) {
					LOGGER.debug("Some other process has updated the same queue as this thread was processing. "
							+ "So trying to delete it again. Retry Count: " + ++retryCount);
				} else {
					LOGGER.warn(()-> "Following exception occured while removing the queue: " + queueName + ".", ex);
					return;
				}
			}
		}
	}
	
	@Override
	public long getQueueSize(final String queueName) {
		int queueSize = -1;
		try {
			return getClient().async().queueSize(queueName).toBlocking().single();
		} catch(DocumentDoesNotExistException e){
        	LOGGER.debug(()->"Queue \"" + queueName + "\" doesn't exists.");
        }
		return queueSize;
	}
	
	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.SCTE224DataManager#getEventScheduledNotificationQueueTimes(java.lang.String)
	 */
	@Override
	public Set<Integer> getEventScheduledNotificationQueueTimes(final String mediaSignalId) {
		String scheduledTimesStr = this.get(EVENT_SCHEDULED_NOTIFICATION_QUEUE_TIMES + mediaSignalId);
		if (scheduledTimesStr != null && !scheduledTimesStr.trim().isEmpty()) {
			return GSON.fromJson(scheduledTimesStr, new TypeToken<HashSet<Integer>>() {
			}.getType());
		}
		return new HashSet<>();
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.SCTE224DataManager#saveMediaScheduledNotificationQueueTimes(java.lang.String, java.util.List)
	 */
	@Override
	public void saveEventScheduledNotificationQueueTimes(final String mediaSignalId, final Set<Integer> scheduledTimes) {
		this.set(EVENT_SCHEDULED_NOTIFICATION_QUEUE_TIMES + mediaSignalId, GSON.toJson(scheduledTimes, new TypeToken<HashSet<Integer>>() {
		}.getType()));
	}
	
	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.SCTE224DataManager#saveMediaScheduledNotificationQueueTimes(java.lang.String, java.util.List)
	 */
	@Override
	public void deleteEventScheduledNotificationQueueTimes(final String eventSignalId) {
		this.delete(EVENT_SCHEDULED_NOTIFICATION_QUEUE_TIMES + eventSignalId);
	}

	@Override
	public void unscheduleNotificationByNotificationId(String queueName, String scheduledNotificationId) {
		short maxRetries = 5;
		short retryCount = 0;
		if(queueName == null || queueName.isEmpty() || scheduledNotificationId == null || scheduledNotificationId.trim().isEmpty()) {
			return;
		}
		while(retryCount <= maxRetries) {
			try {
				
				//1. Fetch the latest queue from Couchbase.
				final List<JsonArrayDocument> document = fetchTheLatestUSNQ(queueName);
		        
		      //2. Remove the notification message for the cancelled notification refs.
				if(document.isEmpty()) {
					LOGGER.debug(()-> "Queue " + queueName + " does not exists. So nothing to cancel schedule here.");
				} else {
					List<HashSet<String>> updatedNotificationBatches = new ArrayList<>();
					JsonArrayDocument notificationQueueJSONArrayDocument = document.get(0);
					if (notificationQueueJSONArrayDocument != null) {
						JsonArray notificationBatches = notificationQueueJSONArrayDocument.content();
						boolean removed = false;
						if (notificationBatches != null) {
							for (Object notificationBatch : notificationBatches) {
								HashSet<String> thisBatchMessageRefs = gson.fromJson(notificationBatch.toString(), new TypeToken<HashSet<String>>() {
								}.getType());
								if (thisBatchMessageRefs != null && !removed) {
									removed = thisBatchMessageRefs.removeIf(messageInBatch -> messageInBatch.equalsIgnoreCase(scheduledNotificationId));
								}
								if (!thisBatchMessageRefs.isEmpty()) {
									updatedNotificationBatches.add(thisBatchMessageRefs);
								}
							}
						}
						saveUSNQ(queueName, updatedNotificationBatches, notificationQueueJSONArrayDocument);
					}
				}
				return;
			} catch(Throwable ex) {
				if(ex instanceof CASMismatchException) {
					LOGGER.debug("Some other process has updated the same queue as this thread was processing. "
							+ "So fetching the queue again and retrying to update. Retry Count: " + ++retryCount);
				} else {
					LOGGER.warn(()-> "Following exception occured while persisting the updated notifcation queue: " + queueName + ".", ex);
					return;
				}
			}
		}
	}

	private List<JsonArrayDocument> fetchTheLatestUSNQ(String queueName) {
		final List<JsonArrayDocument> document = new LinkedList<>();
		final CountDownLatch latch = new CountDownLatch(1);
		long start = System.currentTimeMillis();
		getClient()
			.async()
			.get(queueName, JsonArrayDocument.class)
			.retryWhen(RETRY_POLICY)
			.doAfterTerminate(latch::countDown)
			.subscribe(document::add);
		try {
		    latch.await();
		} catch (InterruptedException e) {
			LOGGER.error(()->e.getMessage());
		}
		// record anything longer than 50 milliseconds as a warning
		long duration = System.currentTimeMillis() - start;getClient().get(queueName, JsonArrayDocument.class);
		if (duration > 50) {
		    LOGGER.warn(()->"Slow get for key \"" + queueName + "\" duration: " + duration + "ms");
		}
		return document;
	}

	private void saveUSNQ(String queueName, List<HashSet<String>> updatedNotificationBatches, JsonArrayDocument notificationQueueJSONArrayDocument) {
		long start;
		long duration;
		//2. Remove the notification queue as it is empty and no longer needed.
		if(updatedNotificationBatches.isEmpty()) {
			LOGGER.debug(()-> "Queue " + queueName + " is empty now and hence deleting it from the persistence store.");
			
		    start = System.currentTimeMillis();
		    try{
		    	getClient().remove(notificationQueueJSONArrayDocument);
		    }catch(DocumentDoesNotExistException e){
		    	LOGGER.debug(()->"Delete did not have impact. Document was not present with key \"" + queueName + "\"");
		    }catch(Exception e){
		    	 String msg = "Error deleting item with key \"" + queueName + "\"";
		         LOGGER.error(()->msg, e);
		         throw new RuntimeException(msg, e);
		    }
		    duration = System.currentTimeMillis() - start;
		    if (duration > 50) {
		        LOGGER.warn(()->"Slow delete for key \"" + queueName + "\" duration: " + duration + "ms");
		    }
		} else {
			//3. If it still has the data update it.
			LOGGER.debug(()-> "Queue " + queueName + " still has some notifications remaining, so updating it in the persistence store.");
		    start = System.currentTimeMillis();
		    List<String> updatedNotificationBatchesList = new ArrayList<>();
		    updatedNotificationBatches.forEach(updatedNotificationBatch -> {
		    	updatedNotificationBatchesList.add(GSON.toJson(updatedNotificationBatch,new TypeToken<HashSet<String>>(){}.getType()));
		    });
		    
		    JsonArrayDocument notificationQueueUpdatedDocument = JsonArrayDocument.create(
		    		notificationQueueJSONArrayDocument.id(), 
		    		notificationQueueJSONArrayDocument.expiry(), 
		    		JsonArray.from(updatedNotificationBatchesList),
		    		notificationQueueJSONArrayDocument.cas());
		    getClient().replace(notificationQueueUpdatedDocument);
		    duration = System.currentTimeMillis() - start;
		    // record anything longer than 50 milliseconds as a warning
		    if (duration > 50) {
		        LOGGER.warn(()->"Slow cas for key \"" + queueName + "\" duration: " + duration + "ms");
		    }
		}
	}
	
	@Override
	public void recordEventDeletionViaRealtimeUpdates(final long lastRuleLoadingTime, final String feedId, final String eventSignalId) {
		long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);
        final String deletedEventsRecordKey = getDeletedEventsRecordKey(lastRuleLoadingTime, feedId);
    	getClient().async()
    		.setAdd(deletedEventsRecordKey, eventSignalId, MutationOptionBuilder.builder().createDocument(true).expiry(getExpiration()))
    		.retryWhen(RETRY_POLICY)
        	.doOnError( exception -> {
        		LOGGER.error(() -> "ESS was not able to add element '"+ eventSignalId +	"' in list '" + deletedEventsRecordKey + 
		        			"' in the persistene store due to the following issue = " +	exception.getMessage());
			})
        	.doAfterTerminate(latch::countDown).subscribe();

        try {
			latch.await();
		} catch (InterruptedException e) {
			LOGGER.error(() -> "ESS was not able to element '"+ eventSignalId +"' in list '" + deletedEventsRecordKey + "' in the persistene store due to the following issue = " + e.getMessage());
            throw new RuntimeException("ESS was not able to element '"+ eventSignalId +"' in list '" + deletedEventsRecordKey + "' in the persistene store due to the following issue = " + e.getMessage(), e);
		}

        long duration = System.currentTimeMillis() - start;

        // record anything longer than 50 milliseconds as a warning
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(() -> "Slow addition into list with key \"" + deletedEventsRecordKey + "\" duration: " + duration + "ms. Element saved: " + eventSignalId);
        }
	}
	
	@Override
	public Set<String> getEventsDeletedSinceLastRulesLoading(final long lastRuleLoadingTime, final String feedId) {
		final String deletedEventsRecordsKey = getDeletedEventsRecordKey(lastRuleLoadingTime, feedId);
		
		final List<JsonArrayDocument> document = new LinkedList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        long start = System.currentTimeMillis();
        getClient()
        	.async()
        	.get(deletedEventsRecordsKey, JsonArrayDocument.class)
        	.retryWhen(RETRY_POLICY)
        	.doAfterTerminate(latch::countDown).subscribe(jsonArrayDocument -> {
            	if(jsonArrayDocument!=null){
            		document.add(jsonArrayDocument);
            	}
	        });
        try {
            latch.await();
        } catch (InterruptedException e) {
        	LOGGER.error(()->e.getMessage());
        }
        // record anything longer than 50 milliseconds as a warning
        long duration = System.currentTimeMillis() - start;getClient().get(deletedEventsRecordsKey, JsonArrayDocument.class);
        if (duration > DB_OP_TIME_THRESHOLD) {
            LOGGER.warn(()->"Slow get for key \"" + deletedEventsRecordsKey + "\" duration: " + duration + "ms");
        }
        
      //2. Remove the notification message for the cancelled notification refs.
		if(document.isEmpty()) {
			LOGGER.debug(()-> "Set " + deletedEventsRecordsKey + " does not exists. That implies no events were deleted for this feed since the last rules loading.");
		} else {
			JsonArrayDocument deletedEventsSignalIdsJSONArrayDocument = document.get(0);
			if(deletedEventsSignalIdsJSONArrayDocument!=null) {
				JsonArray deletedEventsSignalIdsSet = deletedEventsSignalIdsJSONArrayDocument.content();
				if(deletedEventsSignalIdsSet != null) {
					try {
						return gson.fromJson(deletedEventsSignalIdsSet.toString(), new TypeToken<HashSet<String>>(){}.getType());
					} catch (Exception ex) {
						LOGGER.fatal(()->"Fail to retrieve deleted blackout events for " + feedId, ex);
						throw new RuntimeException("Failed to convert JSON List to Java List for " + feedId, ex);
					}
				}
			}
		}
		LOGGER.debug("No deleted events found for this feed that are deleted post last rules loading time. Feed Id" + feedId);
		return new HashSet<>(0);
	}

	private String getDeletedEventsRecordKey(long lastRuleLoadingTime, String feedId) {
		return REALTIME_DELETED_EVENTS + lastRuleLoadingTime + NAME_SPACE_VALUE_SEPARATOR + feedId;
	}

	/*
	 * Following are used by the AL Pretrigger HA availability
	 */
	@Override
	public String getNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType) {
		return get(OUT_OF_BAND_RECORD_NAMESPACE + acquistionPointIdentity +":"+eventId+":"+notificationType);
	}

    @Override
    public boolean caslock(String lockName, int expirationSeconds) {

    	String lockKey = LOCK_NAMESPACE + lockName;
    	if(add(lockKey, expirationSeconds, "TRY_LOCK") != null){
    		return false;
    	}
    	StringDocument cbDocument = getClient().getAndLock(lockKey, expirationSeconds,StringDocument.class);
    	return cas(lockKey, cbDocument.cas(), "LOCKED");
    }
    
	@Override
	public void putNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType, String status) {
		set(OUT_OF_BAND_RECORD_NAMESPACE + acquistionPointIdentity + ":" +eventId + ":" + notificationType, status);
	}

	
}

