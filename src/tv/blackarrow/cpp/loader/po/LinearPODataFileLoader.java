package tv.blackarrow.cpp.loader.po;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.setting.FeedConfigBean;

/**
 * Read file every hour from the inprocess directory
 * 
 * @author hcao
 * 
 */
public class LinearPODataFileLoader {
	private static final Logger LOGGER = LogManager.getLogger(LinearPODataFileLoader.class);

    private static final long ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;
    private static final String FEED_LOCK_PREFIX = "LOADER_FEED_LOCK_";
    private static final int MAX_RETRY = 10;

    private String getNextHourPrefix() {
        long time = (new Date()).getTime() + ONE_HOUR_IN_MILLISECONDS;
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHH");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    public void load() {
        File dataDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir());
        if (!dataDir.exists()) {
            LOGGER.error(()->"PO data directory does not exist, skip loading");
            return;
        }
        
        loadMappingFiles();
        loadPOData();
    }

    public void loadMappingFiles() {
        // currently this process is just allowed to run on every node in the cluster.
        // it does not matter if one node overwrites the other node's changes since they
        // are all updating with the same information. coordinating so only one node does 
        // all the processing would add complexity and make this error prone. it takes 
        // less than one second for this process to run even with a large number of 
        // acquisition points so we are not going to distribute the process for now.
    	
    	AcquisitionConfigBean bean = AcquisitionConfigBean.getInstance(); //old
    	HashMap<String, String> oldMapbyFeed = null;
    	
    	if(bean != null) {
    		Map<String, String> oldMapbyAP = AcquisitionConfigBean.getInstance().getAcquisitionFeedMap();
    		LOGGER.info(()->"oldMapbyAP :" + oldMapbyAP);
    		oldMapbyFeed = convertValuesToKeys(oldMapbyAP);
    	}
    	
		LOGGER.info("oldMapbyFeed :" + oldMapbyFeed);
    	
        AcquisitionConfigBean.load();
        FeedConfigBean.load();
        

        Map<String, String> newMapbyAP = AcquisitionConfigBean.getInstance().getAcquisitionFeedMap();
        HashMap<String, String> newMapbyFeed = convertValuesToKeys(newMapbyAP);
        LOGGER.info(()->"newMapbyAP :" + newMapbyAP);
        LOGGER.info(()->"newMapbyFeed :" + newMapbyFeed);
        
        Iterator<String> currentIterator = newMapbyAP.values().iterator();
    	HashSet<String> set = new HashSet<String>();
        while(currentIterator.hasNext()) {
        	set.add(currentIterator.next());
        }
        
        String existingFeedList = DataManagerFactory.getInstance().getActiveFeedList();
		LOGGER.info(()->"existing feed list was :" + existingFeedList);
		
		String newActiveFeedList = convertSetToString(set);
		LOGGER.info(()->"new active feed list will be :" + newActiveFeedList);
        
        if(existingFeedList != null) {
	    	StringTokenizer st = new StringTokenizer(existingFeedList, ",");
	    	while(st.hasMoreTokens()) {
	    		String feedExternalRef = st.nextToken();
	    		
	    		if(oldMapbyFeed != null) {
	    			 if(!newMapbyAP.containsValue(feedExternalRef)) {/*
	    				 String acqPoints = oldMapbyFeed.get(feedExternalRef);
	    				 ArrayList<String> acqPointList = convertStringToList(acqPoints); 
	    				 
	    				 for (int i = 0; i < acqPointList.size(); i++) {
	    					 DataManagerFactory.getInstance().deleteSignalProcessorCursor(acqPointList.get(i));
						}
	    			
	    				 DataManagerFactory.getInstance().deleteLoaderCursor(feedExternalRef);
					*/
	    				 
	    				 Set<String> acqPoints = DataManagerFactory.getInstance().getFeedToAcquistionMap(feedExternalRef);
                         if (acqPoints != null) {
                           for (String acqPointTobeDeleted : acqPoints) {
                                 DataManagerFactory.getInstance().deleteAcquisitionPoint(acqPointTobeDeleted);
                           }
                         }
                         DataManagerFactory.getInstance().deleteLoaderCursor(feedExternalRef);
	    			} else {
						// feed still exists but may be some acq points were deleted.. then...
						String oldAPs = oldMapbyFeed.get(feedExternalRef);
						String newAPs = newMapbyFeed.get(feedExternalRef);
						
						HashSet<String> oldAPset = convertStringToSet(oldAPs);
						HashSet<String> newAPset = convertStringToSet(newAPs);
						
						Iterator<String> oldIterator = oldAPset.iterator();
						while(oldIterator.hasNext()) {
							String oldAP = oldIterator.next();
							if(!newAPset.contains(oldAP)) {
								//DataManagerFactory.getInstance().deleteSignalProcessorCursor(oldAP);
								DataManagerFactory.getInstance().deleteAcquisitionPoint(oldAP);
							}
						}
					}
	    		} 
	    	}
        }
    	
    	DataManagerFactory.getInstance().putActiveFeedList(newActiveFeedList);
    	
        // just keep trying to load until everything is loaded successfully.
        // if one of the nodes in the cluster has failed, it may take some time 
        // for the situation to be corrected. we need the load process to run 
        // once the system is working again though so keep trying.
        while (true) {
            try {
                LOGGER.info(()->"Starting mapping file load");
                (new LinearPOIndexer()).refreshAcquisitionPoint(bean);
                break;
            } catch (RuntimeException e) {
                // if the data store is in a failure mode, we may get an exception but we
                // don't want to give up on the load so just skip this one and we'll retry it
                LOGGER.error(()->"Unexpected exception trying to load mapping file, waiting before retrying", e);
            }
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
            }
        }
        
        LOGGER.info(()->"Done with mapping file load");
    }

	private void loadPOData() {
        // just keep trying to load until everything is loaded successfully.
        // if one of the nodes in the cluster has failed, it may take some time 
        // for the situation to be corrected. we need the load process to run 
        // once the system is working again though so keep trying.
		Map<String, Integer> retryFeeds = new HashMap<String, Integer>();
		Set<String> loadedFeeds = new HashSet<String>();
        while (true) {
            LOGGER.info(()->"Starting PO data file load");
            Map<String, Map<String, String>> feedConfigMap = FeedConfigBean.getInstance().getFeedConfigMap();
            DataManager dataManager = DataManagerFactory.getInstance();
            for (String feedName : feedConfigMap.keySet()) {
                try {
                	if(LOGGER.isInfoEnabled()){
                		LOGGER.info(()->"loading data for feed " + feedName);
                	}
                	if(loadedFeeds.contains(feedName)){
                		if(LOGGER.isDebugEnabled()){
                			LOGGER.info(()->"Feed " + feedName + " has been loadded, skip.");
                		}
                		continue;
                	}
                	
                	if(retryFeeds.get(feedName)!=null && retryFeeds.get(feedName) >= MAX_RETRY){
                		if(LOGGER.isDebugEnabled()){
                			LOGGER.debug(()->"Data load retry for Feed " + feedName + " reached maximum, skip.");
                		}
                		continue;
                	}
                	
                    List<String> filesToLoad = getDataFilesToLoad(feedName);
                    if (filesToLoad.size() > 0) {
                        // other nodes in the cluster may be loading at the same time so
                        // use a lock to make sure only one node works with each feed at one time.
                        String lockName = FEED_LOCK_PREFIX + feedName;
                        boolean locked = false;
                        try {
                            if (dataManager.lock(lockName, 30)) {
                                locked = true;
                                loadAndIndex(feedName, filesToLoad);
                                if(LOGGER.isInfoEnabled()){
                            		LOGGER.info(()->"Successfully load data for feed " + feedName);
                            	}
                            } else {
                                LOGGER.info(()->"Another node appears to be loading data for feed " + feedName + ", skipping loading.");
                            }
                        } finally {
                            if (locked) {
                                dataManager.unlock(lockName);
                            }
                        }
                    }
                    loadedFeeds.add(feedName);
                    retryFeeds.remove(feedName);
                } catch (RuntimeException e) {
                    // if the data store is in a failure mode, we may get an exception but we
                    // don't want to give up on the load so just skip this one and we'll retry it
                    LOGGER.warn(()->"Unexpected exception trying to load data for feed " + feedName + ", will retry", e);
                    int retryCount = retryFeeds.get(feedName)==null?0:retryFeeds.get(feedName);
                	retryFeeds.put(feedName, ++retryCount);
                }
            }
            if (!retryFeeds.isEmpty()) {
            	boolean shouldBreak = true;
            	for(String feed: retryFeeds.keySet()){
            		if(retryFeeds.get(feed) < MAX_RETRY){
            			shouldBreak = false;
            			break;
            		}
            	}
            	
            	if(shouldBreak){
            		LOGGER.info(()->"Enough retry for loading, stop the data loading.");
            		break;
            	}
            	
                // wait for some time to allow other nodes to finish and keep this process
                // from swamping the server in case of a failure.  
                LOGGER.info(()->"Some feeds that needed to be loaded were skipped, waiting before retrying");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                }
            } else {
                break;
            }
        }
        LOGGER.info(()->"Done with PO data file load");
    }

    private List<String> getDataFilesToLoad(String feedName) {
        final String hrPrefix = getNextHourPrefix();
        File feedDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir() + "/" + feedName);

        // make sure the next hour file is present regardless of what we are loading
        File nextHourFile = new File(feedDir + "/" + hrPrefix + "_" + feedName + ".json");
        if (!nextHourFile.exists()) {
            LOGGER.error(()->"Expected hourly PO data file for hour \"" + hrPrefix + "\" does not exist for this feed - " + feedName);

            // this is a bad situation and if it persists for a long time then the loader
            // cursor might expire so we will just make sure that it stays alive.
            touchLoaderCursor(feedName);
        }

        DataManager dataManager = DataManagerFactory.getInstance();
        
        // check if signal processor cursor available for acquisition points for feed. It can be delete on Acquistion Point swap and deletion.
    	boolean forceLoad = false;
    	Set<String> acqIDList = dataManager.getFeedToAcquistionMap(feedName);
        for (String acqID : acqIDList) {
            SignalProcessorCursor sigCursor = dataManager.getSignalProcessorCursor(acqID);
            if (sigCursor == null) {
            	forceLoad = true;
            	break;
            }
        }
        
        String[] poFileArr = null;
        if(forceLoad){
        	FilenameFilter hrFilter = new FilenameFilter() {
        		@Override
        		public boolean accept(File file, String name) {
        			if(name.indexOf("blackout") >= 0) {
        				return false;
        			}
        			if(name.indexOf("media") >= 0) {
        				return false;
        			}
        			if (name.compareTo(hrPrefix) <= 0 || name.startsWith(hrPrefix)) {
        				return true;
        			}
        			return false;
        		}
        	};
        	poFileArr = feedDir.list(hrFilter);
        }else{
        	LoaderCursor loaderCursor = dataManager.getLoaderCursor(feedName);
        	final String lastLoadedDataFile = (loaderCursor != null) ? loaderCursor.getLastLoadedDataFile() : null;
        	FilenameFilter hrFilter = new FilenameFilter() {
        		@Override
        		public boolean accept(File file, String name) {
        			// if we have already loaded the file previously then skip it
        			if ((lastLoadedDataFile != null) && (name.compareTo(lastLoadedDataFile) <= 0)) {
        				if (LOGGER.isDebugEnabled()) {
        					LOGGER.debug(()->"File \"" + name + "\" was already loaded previously, skipping");
        				}
        				return false;
        			}
        			if(name.indexOf("blackout") >= 0) {
        				return false;
        			}
        			if(name.indexOf("media") >= 0) {
        				return false;
        			}
        			if (name.compareTo(hrPrefix) <= 0 || name.startsWith(hrPrefix)) {
        				return true;
        			}
        			return false;
        		}
        	};
        	poFileArr = feedDir.list(hrFilter);
        }

        List<String> loadList = new ArrayList<String>();
        // we need to make sure that in the same feed earlier POs are loaded before 
        // later ones so sort this list of files first.
        Arrays.sort(poFileArr);
        for (String poFile : poFileArr) {
            loadList.add(feedDir.getAbsolutePath() + File.separator + poFile);
        }
        return loadList;
    }

    private void loadAndIndex(String feedName, List<String> filePathList) {
        for (final String pathName : filePathList) {
            POJsonParser parser = new POJsonParser();
            LinearPOIndexer indexer = new LinearPOIndexer();
            List<ZonePO> poList = parser.parseJson(pathName);
            // note: even if there are no POs in this file we still need to call
            // the indexer so it can record that this file was processed.
            indexer.indexOneFeed(feedName, poList, pathName);
        }
    }

    private void touchLoaderCursor(String feedName) {
        LOGGER.debug(()->"Touching loader cursor for feed " + feedName + " to make sure it does not expire.");
        DataManager dataManager = DataManagerFactory.getInstance();
        LoaderCursor cursor = dataManager.getLoaderCursor(feedName);
        if (cursor == null) {
            LOGGER.debug(()->"Loader cursor does not exist for feed " + feedName + ", loader may not have run yet.");
            return;
        }

        // if another process updates this feed at the same time, we don't really
        // care. we just wanted to make sure it didn't expire so we don't
        // need to check the result of the update here.
        dataManager.casLoaderCursor(dataManager.getLoaderCursor(feedName));
    }
    
    /*public ArrayList<String> convertStringToList(String str) {
    	ArrayList<String> list = new ArrayList<String>();
    	StringTokenizer st = new StringTokenizer(str, ",");
    	while(st.hasMoreTokens()) {
    		list.add(st.nextToken());
    	}
    	return list;
    }*/
    
    private HashSet<String> convertStringToSet(String str) {
    	HashSet<String> set = new HashSet<String>();
    	
    	if(str == null) {
    		return set;
    	}
    	
    	StringTokenizer st = new StringTokenizer(str, ",");
    	while(st.hasMoreTokens()) {
    		set.add(st.nextToken());
    	}
    	return set;
    }
    

	public String convertSetToString(Set<String> set) {
		Iterator<String> iterator = set.iterator();
		StringBuilder st = new StringBuilder();

		while (iterator.hasNext()) {
			String str = iterator.next();
			st.append(str);
			if(iterator.hasNext()) {
				st.append(",");
			}
		}
		
		return st.toString();
	}
	
	private static HashMap<String, String> convertValuesToKeys(Map<String, String> oldMapbyAP) {
    	HashMap<String, String> oldMapbyFeed = new HashMap<String, String>();
    	Iterator<Entry<String, String>> iterator = oldMapbyAP.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			String key = entry.getKey(); // ap1
			String value = entry.getValue(); //feed1
			
			if(oldMapbyFeed.get(value) != null) {
				key = oldMapbyFeed.get(value) + "," + key;
			}
			
			oldMapbyFeed.put(value, key);
		}
		
		return oldMapbyFeed;
		
	}
	
	
/*	public static void main (String args[]) {
		HashMap<String, String> h = new HashMap<String, String>();
		h.put("AP1", "F1");
		h.put("AP2", "F1");

		h.put("AP3", "F2");
		h.put("AP4", "F2");
		
		h.put("AP5", "F3");
		
		convertValuesToKeys(h);
	}*/

}
