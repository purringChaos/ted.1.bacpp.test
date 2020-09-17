/**
 * 
 */
package tv.blackarrow.cpp.managers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

/**
 * @author akumar
 *
 */
public class SCCMCCThreadLocalCache {
	
	private static final ThreadLocal<Boolean> needsCaching = new InheritableThreadLocal<Boolean>();
	private static final ThreadLocal<Map<String, Object>> threadLocalCache = new InheritableThreadLocal<Map<String, Object>>();
    private static final Gson gson = new Gson();
	
	private static void putInThreadLocalCache(final String couchbaseSearchKey, final Object couchbaseValueObject){
		Map<String, Object> valuesAvailableForThisThread = threadLocalCache.get();
		if(valuesAvailableForThisThread==null){
			valuesAvailableForThisThread = new HashMap<String, Object>();
		}
		valuesAvailableForThisThread.put(couchbaseSearchKey, couchbaseValueObject);
		threadLocalCache.set(valuesAvailableForThisThread);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T getFromThreadLocalCache(final String couchbaseSearchKey){
		Map<String, Object> valuesAvailableForThisThread = threadLocalCache.get();
		if(valuesAvailableForThisThread==null){
			return null;
		}
		return (T)valuesAvailableForThisThread.get(couchbaseSearchKey);
	}
	
	private static void deleteFromThreadLocalCache(final String couchbaseSearchKey){
		Map<String, Object> valuesAvailableForThisThread = threadLocalCache.get();
		if(valuesAvailableForThisThread!=null){
			valuesAvailableForThisThread.remove(couchbaseSearchKey);
		}
	}
	
	
	public static <T> T get(final DataManager dataManager, final String couchbaseSearchKey, final Class<T> couchbaseValueObjectClass){
		if(needsCaching.get()!=null && needsCaching.get()){
			T result = SCCMCCThreadLocalCache.<T>getFromThreadLocalCache(couchbaseSearchKey);
			if(result == null){
				String latestFromCouchbase = dataManager.get(couchbaseSearchKey);
				if(latestFromCouchbase!=null){
					result = gson.fromJson(latestFromCouchbase, couchbaseValueObjectClass);
					putInThreadLocalCache(couchbaseSearchKey, result);
				}
			}
			return result;
		} else {
			String latestFromCouchbase = dataManager.get(couchbaseSearchKey);
			if(latestFromCouchbase!=null){
				T result = gson.fromJson(latestFromCouchbase, couchbaseValueObjectClass);
				return result;
			}
			return null;
		}
	}
	
	public static void put(final DataManager dataManager, final String couchbaseSearchKey, final Object couchbaseValueObject, final Type typeOfObject){
		dataManager.set(couchbaseSearchKey, typeOfObject != null ? gson.toJson(couchbaseValueObject, typeOfObject) : gson.toJson(couchbaseValueObject));
		if(needsCaching.get()!=null && needsCaching.get()){
			putInThreadLocalCache(couchbaseSearchKey, couchbaseValueObject);
		}
	}
	
	public static void put(final DataManager dataManager, final String couchbaseSearchKey, final Object couchbaseValueObject){
		put(dataManager, couchbaseSearchKey, couchbaseValueObject, null);
	}
	
	public static void delete(final DataManager dataManager, final String couchbaseSearchKey){
		dataManager.delete(couchbaseSearchKey);
		if(needsCaching.get()!=null && needsCaching.get()){
			deleteFromThreadLocalCache(couchbaseSearchKey);
		}
	}

	public static void clearMyCache(){
		needsCaching.remove();
		threadLocalCache.remove();
	}
	
	public static void cachingRequired(){
		needsCaching.set(Boolean.TRUE);
	}
}
