package tv.blackarrow.cpp.setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public final class AcquisitionConfigBean {
	private static final String EMPTY_STRING = "";

	private Logger LOG = LogManager.getLogger(AcquisitionConfigBean.class);

	private static ConfigurableApplicationContext ctx = null;
    private static AcquisitionConfigBean instance = null;

    private Map<String, Map<String, String>> acquisitionPointMap;
    
    private Map<String, Boolean> acquisitionDeleteEmptyBreakMap;
    private Map<String, Boolean> acquisitionInterfaceTypeMap;
    private Map<String, String> acquisitionFeedMap;
    private Map<String, String> acquisitionHLSInterfaceTypeMap;
    private Map<String, String> acquisitionHSSInterfaceTypeMap;
    private Map<String, String> acquisitionSchedulesInterfaceTypeMap;
    private Map<String, Long> acquisitionSignalTimeOffsetMap;
    private Map<String, Long> acquisitionLastUpdatedTimeMap;
    
    private Map<String, Set<String>> feedToAcquisitionPointMap;  
    private Map<String, List<String>> feedToTranscoderUrlsMap;
    
    
    private AcquisitionConfigBean() {
    }
 
	public static void load() {

	    if(ctx == null) {
			ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/acquisition_feed_mapping.xml");
	    } else {
	    	ctx.refresh();
	    }
	    AcquisitionConfigBean newInstance = (AcquisitionConfigBean) ctx.getBean("acquisitionPointBean");
	    instance = newInstance;
	}

    public static AcquisitionConfigBean getInstance() {
    	if(instance ==null) {
			load();
		}
        return instance;
    }

    public Map<String, Map<String, String>> getAcquisitionPointMap() {
    	return acquisitionPointMap;
    }
    
    public void setAcquisitionPointMap(Map<String, Map<String, String>> map) {
    	acquisitionPointMap = map;
    }
    
    public Map<String, Boolean> getAcquisitionDeleteEmptyBreakMap() {
    	if(acquisitionDeleteEmptyBreakMap == null ) {
    		acquisitionDeleteEmptyBreakMap = new HashMap<String, Boolean>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.SCC_DELETE_EMPTY_BREAK);
    			if(value != null && (value.trim().equalsIgnoreCase("true") 
    					|| value.trim().equalsIgnoreCase("y"))) {
    				acquisitionDeleteEmptyBreakMap.put(acquisitionPointID, true);
    			} else {
    				acquisitionDeleteEmptyBreakMap.put(acquisitionPointID, false);
    			}
    		}
    	}
    	return acquisitionDeleteEmptyBreakMap;
    }
    
    public Map<String, Boolean> getAcquisitionInterfaceTypeMap() {
    	if(acquisitionInterfaceTypeMap == null ) {
    		acquisitionInterfaceTypeMap = new HashMap<String, Boolean>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.INCLUDE_IN_POINT);
    			if(value != null && value.trim().equalsIgnoreCase("y") ) {   					
    				acquisitionInterfaceTypeMap.put(acquisitionPointID, true);
    			} else {
    				acquisitionInterfaceTypeMap.put(acquisitionPointID, false);
    			}
    				
    		}
    	}
    	return acquisitionInterfaceTypeMap;
    }
    
    public Map<String, String> getAcquisitionFeedMap() {
    	if(acquisitionFeedMap == null ) {
    		acquisitionFeedMap = new HashMap<String, String>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.FEED_ID);
    			if(value != null ) {
    				acquisitionFeedMap.put(acquisitionPointID, value);
    			} 
    		}
    	}
    	return acquisitionFeedMap;
    }

    public Map<String, String> getAcquisitionHLSInterfaceTypeMap() {
    	if(acquisitionHLSInterfaceTypeMap == null ) {
    		acquisitionHLSInterfaceTypeMap = new HashMap<String, String>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.HLS_INTERFACE_TYPE);
    			if(value != null ) {
    				acquisitionHLSInterfaceTypeMap.put(acquisitionPointID, value);
    			} 
    		}
    	}
    	return acquisitionHLSInterfaceTypeMap;
    }

    public Map<String, String> getAcquisitionHSSInterfaceTypeMap() {
    	if(acquisitionHSSInterfaceTypeMap == null ) {
    		acquisitionHSSInterfaceTypeMap = new HashMap<String, String>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.HSS_INTERFACE_TYPE);
    			if(value != null ) {
    				acquisitionHSSInterfaceTypeMap.put(acquisitionPointID, value);
    			} 
    		}
    	}
    	return acquisitionHSSInterfaceTypeMap;
    }
    
    public Map<String, String> getAcquisitionSchedulesInterfaceTypeMap() {
    	if(acquisitionSchedulesInterfaceTypeMap == null ) {
    		acquisitionSchedulesInterfaceTypeMap = new HashMap<String, String>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.SCHEDULES_INTERFACE_TYPE);
    			if(value != null ) {
    				acquisitionSchedulesInterfaceTypeMap.put(acquisitionPointID, value);
    			} 
    		}
    	}
    	return acquisitionSchedulesInterfaceTypeMap;
    }
    
    public Map<String, Long> getAcquisitionSignalTimeOffsetMap() {
    	if(acquisitionSignalTimeOffsetMap == null ) {
    		acquisitionSignalTimeOffsetMap = new HashMap<String, Long>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
				Long value = acqMap.get(SettingUtils.SIGNAL_TIME_OFFSET) != null ? Long.parseLong(acqMap.get(SettingUtils.SIGNAL_TIME_OFFSET)) : null;
    			if(value != null ) {
    				acquisitionSignalTimeOffsetMap.put(acquisitionPointID, value);
    			} 
    		}
    	}
    	return acquisitionSignalTimeOffsetMap;
    }

    public Map<String, Long> getAcquisitionLastUpdatedTimeMap() {
    	if(acquisitionLastUpdatedTimeMap == null ) {
    		acquisitionLastUpdatedTimeMap = new HashMap<String, Long>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String value = acqMap.get(SettingUtils.LAST_UPDATED_TIME);
    			if(value != null && !value.trim().isEmpty()) {
    				acquisitionLastUpdatedTimeMap.put(acquisitionPointID, Long.valueOf(value));
    			}
    		}
    	}
    	return acquisitionSignalTimeOffsetMap;
    }
    public Map<String, String> getAcquisitionPointConfig(String acquisitionPointId) {
    	return acquisitionPointMap.get(acquisitionPointId);
    }

    /**
     * it will include feed name mapping to multiple acquisition points
     * for example:  feed1 - [acq1, acq2, acq3]   (acq - acquisition point)
     *               feed2 - [acq4, acq5, acq6]
     * 
     * @return
     */
	public Map<String, Set<String>> getFeedToAcquisitionPointMap() {
		if(feedToAcquisitionPointMap == null) {  // populate the records
			feedToAcquisitionPointMap = new HashMap<String, Set<String>>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String key = acqMap.get(SettingUtils.FEED_ID);
    			if(key != null) {
    				Set<String> acqSet = feedToAcquisitionPointMap.get(key);
    				if(acqSet == null) {
    					acqSet = new HashSet<String>();
    				} 
					acqSet.add(acquisitionPointID);
    				feedToAcquisitionPointMap.put(key, acqSet);
    			} 
    		}
		}
		
		return feedToAcquisitionPointMap;
	}

	public String getValueFromAcquistionPointMap(String acquisitionPointID, String key) {
		Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
		return acqMap.get(key)!=null?acqMap.get(key):EMPTY_STRING;
	}
	
	public String getValueFromAcquistionPointMapIfPresent(String acquisitionPointID, String key) {
		Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
		return acqMap.get(key)!=null?acqMap.get(key): null;
	}
	
    /**
     * the method will returns the mapping between the feed ID to the list 
     * of the Transcoder URLs
     * 
     * feed ID --> a list of URLs (transcoder)
     * 
     * @return map of the mapping
     */
	public Map<String, List<String>> getFeedToTranscoderUrlMap() {
		if(feedToTranscoderUrlsMap == null) {  // populate the records
			feedToTranscoderUrlsMap = new HashMap<String, List<String>>();
    		Iterator<String> it = acquisitionPointMap.keySet().iterator();
    		while(it.hasNext()) {
    			String acquisitionPointID = it.next();
    			Map<String, String> acqMap = acquisitionPointMap.get(acquisitionPointID);
    			String key = acqMap.get(SettingUtils.FEED_ID);
    			if(key != null) {
    				List<String> urlList = feedToTranscoderUrlsMap.get(key);
    				if(urlList == null) {
    					urlList = new ArrayList<String>();
    				} 
    				String url = acqMap.get(SettingUtils.TRANSCODER_ENDPOINT);
    				if(url != null && !url.isEmpty()) {
    					urlList.add(url);
    					feedToTranscoderUrlsMap.put(key, urlList);
    				}
    			} 
    		}
		}
		
		return feedToTranscoderUrlsMap;
	}
}
