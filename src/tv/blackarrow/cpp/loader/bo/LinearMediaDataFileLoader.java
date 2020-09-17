/**
 * 
 */
package tv.blackarrow.cpp.loader.bo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.components.util.ESSClusterLockUtil;
import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.scte224.CompactMediaInfo;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler.MediaCRUDNotificationsHandler;
import tv.blackarrow.cpp.setting.FeedConfigBean;
import tv.blackarrow.cpp.utils.EventAction;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class LinearMediaDataFileLoader {

	private static final Logger LOGGER = LogManager.getLogger(LinearMediaDataFileLoader.class);
	private static final LinearMediaDataFileLoader linearMediaDataFileLoader = new LinearMediaDataFileLoader();
	private static final String MEDIA_FEED_DATA_FILE_SUFFIX = "_media.json";
	private static final String MEDIA_FEED_DATA_FILE_DATEFORMAT = "yyyyMMddHHmmss";
	private static final int MAX_RETRY = 10;
	private static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();
	private static final SCTE224DataManager SCTE224_DATA_MANAGER = DataManagerFactory.getSCTE224DataManager();


	private LinearMediaDataFileLoader() {
		super();
	}

	public static LinearMediaDataFileLoader getInstance() {
		return linearMediaDataFileLoader;
	}

	public void load() {
		File dataDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir());
		if (!dataDir.exists()) {
			LOGGER.fatal(()->"Inprocess data directory does not exist: " + dataDir.getAbsolutePath());
			return;
		}

		if(LOGGER.isInfoEnabled()) {
			LOGGER.info(()->"Refreshing the Acquisition Point & Feed mapiings from acquisition_feed_mapping.xml.");
		}

		FeedConfigBean.load();

		if(LOGGER.isInfoEnabled()) {
			LOGGER.info(()->"Finished getting the latest Acquisition Point & Feed mapiings from the acquisition_feed_mapping.xml.");
			LOGGER.info(()->"Starting the Loading process for Media data files.");
		}
		Map<String, Integer> retryFeeds = new HashMap<String, Integer>();
		Set<String> loadedFeeds = new HashSet<String>();
		FeedConfigBean feedConfigBean = null;
		while (true) {
			feedConfigBean = FeedConfigBean.getInstance();
			if(feedConfigBean != null && feedConfigBean.getFeedConfigMap()!= null && !feedConfigBean.getFeedConfigMap().isEmpty()) {
				for (String feedId : feedConfigBean.getFeedConfigMap().keySet()) {
					if(feedConfigBean.isSCTE224Feed(feedId)) {
						try {
							if(LOGGER.isInfoEnabled()){
								LOGGER.info(()->"loading blackout(served by 224 media) data for feed " + feedId);
							}

							String[] datafiles = getDataFilesToLoad(feedId);
							if(loadedFeeds.contains(feedId)){
								if(LOGGER.isDebugEnabled()){
									LOGGER.info(()->"Feed " + feedId + " has been loaded, skipping it.");
								}
								continue;
							}
							if (datafiles == null || datafiles.length == 0) {
								loadedFeeds.add(feedId);
								continue;
							}

							/** 
							 * 1. Take required locks.  
							 */
							StringDocument lockedDocument = null;
							try {
								// other nodes in the cluster may be loading at the same time so use a lock to make sure only one node works with each feed at one time. Also            			
								// Synchronize the actions between the loader and real-time updates to POIS across cluster.
								lockedDocument = ESSClusterLockUtil.acquireClusterWideLockWithFeedExternalRef(feedId);

								// Load the data file.
								loadDataFile(feedId, datafiles);
								loadedFeeds.add(feedId);
								retryFeeds.remove(feedId);
							} finally {
								ESSClusterLockUtil.releaseClusterWideLock(lockedDocument);
							}
						} catch (Exception e) {
							// if the data store is in a failure mode, we may get an exception but we
							// don't want to give up on the load so just skip this one and we'll retry it
							LOGGER.warn(()->"Unexpected exception trying to load data for feed " + feedId + ", will retry", e);
							int retryCount = retryFeeds.get(feedId)==null?0:retryFeeds.get(feedId);
							loadedFeeds.remove(feedId);
							retryFeeds.put(feedId, ++retryCount);
						}
					} else {
						continue;
					}
				}
			} else {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info(()->"no Feed or Acquisition Point mappings/configuration exists. Either there are no feed or APs in the system or no rules have been pushed yet.");
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
					LOGGER.info(()->"Enough retry for loading, stop the media data loading.");
					break;
				}
				// wait for some time to allow other nodes to finish and keep this process
				// from swamping the server in case of a failure.  
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info(()->"Some feeds that needed to be loaded were skipped, waiting before retrying");
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					//Just Ignore, eat it, gulp it whatever.
				}
			} else {
				break;
			}
		}
		if(LOGGER.isInfoEnabled()) {
			LOGGER.info(()->"Loading process finished for Media data files.");
	}
	}
	
	private void loadDataFile(final String feedId, final String[] datafiles) {

		if (datafiles == null || datafiles.length == 0){
			return;
		}

		final String datafile = datafiles[datafiles.length - 1];
		final String feedDir = LoaderUtil.getFeedDataDirectory(feedId);
		//Delete all files but the latest 
		for (int i = 0; i < datafiles.length - 1; i++) {
			final String filename = datafiles[i];
			// remove all data files after loading the data 
			LoaderUtil.deleteFile(feedDir, filename);
		}

		final long lastMediaCompilationTime = SCTE224_DATA_MANAGER.getLastMediaCompilationTime(feedId);

		FileInputStream instream = null;

		try {
			final long currentBlackoutRulesCompilationTime = getCompileTime(datafile);
			if (currentBlackoutRulesCompilationTime <= lastMediaCompilationTime) {
				if(LOGGER.isInfoEnabled()) {
					LOGGER.info(()->"Media feed data file already loaded: " + datafile);
				}
			} else {
				File mediaRulesFile = new File(feedDir + File.separator + datafile);
				instream = new FileInputStream(mediaRulesFile);
				String mediaJSONFromMediaRuleFile = IOUtils.toString(instream);
				LinkedList<Media> mediasReceivedInTheNewMediaRuleFile = SCTE224DataManager.GSON.fromJson(mediaJSONFromMediaRuleFile, 
						new TypeToken<LinkedList<Media>>(){}.getType());

				if (mediasReceivedInTheNewMediaRuleFile != null && !mediasReceivedInTheNewMediaRuleFile.isEmpty()) {
					//saveForBackwardCompatibility(feedId, mediaJSONFromMediaRuleFile, mediasReceivedInTheNewMediaRuleFile); //*****************Remove this in 9.4 Release *****************	
					saveForV1(feedId, mediaJSONFromMediaRuleFile, mediasReceivedInTheNewMediaRuleFile, currentBlackoutRulesCompilationTime);
				}

				SCTE224_DATA_MANAGER.saveCompilationTimeForTheLoadedMediaData(feedId, currentBlackoutRulesCompilationTime);
			}
			LoaderUtil.deleteFile(feedDir, datafile); 

		} catch (Exception ex) {    	
			LOGGER.error(()->"invalid blackout feed data file format: " + datafile, ex);
			LoaderUtil.moveFile(LoaderUtil.getFeedDataDirectory(feedId) + File.separator + datafile, LoaderUtil.getErrorDirectory(), datafile);
		} finally{
			if(instream != null){
				try {
					instream.close();
				} catch (Exception e) {}
			}
		}
	}
	

	private void saveForV1(String feedId, String mediaJSONFromMediaRuleFile, LinkedList<Media> mediasReceivedInTheNewMediaRuleFile, long currentBlackoutRulesCompilationTime) {
		FeedMediaCompactedDetail feedMediaCompactedDetailInDataStoreCB = SCTE224_DATA_MANAGER.getFeedMediaCompactedDetailV1(feedId);		
		
		List<Media> mediasThatAreUpdated = new ArrayList<Media>();//update case
		List<Media> mediasThatAreDeleted = new ArrayList<Media>();//delete case:
		List<Media> mediasThatAreNewlyAdded = new ArrayList<>();//Schedule Case:		
		populateNewUpdatedDeletedMediasBeforePersistingInCB(feedMediaCompactedDetailInDataStoreCB, currentBlackoutRulesCompilationTime, feedId, mediasReceivedInTheNewMediaRuleFile, mediasThatAreNewlyAdded, mediasThatAreUpdated, mediasThatAreDeleted);
		
		SCTE224_DATA_MANAGER.saveMediasDuringRuleFileLoadV1(feedId, mediasReceivedInTheNewMediaRuleFile, feedMediaCompactedDetailInDataStoreCB, true, currentBlackoutRulesCompilationTime);
		//fetch the acquisition points by the feed: 
		Set<String> acquisitionPoints = DATA_MANAGER.getFeedToAcquistionMap(feedId);
		List<AcquisitionPoint> acqPoints = DATA_MANAGER.getAcquisitionPoints(acquisitionPoints);

		MediaCRUDNotificationsHandler.notify(acqPoints, EventAction.COMPLETE, mediasThatAreNewlyAdded, mediasThatAreUpdated, mediasThatAreDeleted);		
	}


	private void populateNewUpdatedDeletedMediasBeforePersistingInCB(FeedMediaCompactedDetail existingFeedMediaCompactedDetail, long currentBlackoutRulesCompilationTime,
			String feedId, List<Media> mediasReceivedInTheNewMediaRuleFile, List<Media> mediasThatAreNewlyAdded, List<Media> mediasThatAreUpdated,
			List<Media> mediasThatAreDeleted) {

		//New Case
		if (existingFeedMediaCompactedDetail == null || existingFeedMediaCompactedDetail.getBasicMediaInfo() == null
				|| existingFeedMediaCompactedDetail.getBasicMediaInfo().isEmpty()) {
			mediasThatAreNewlyAdded.addAll(mediasReceivedInTheNewMediaRuleFile);
		}
		//Delete Case
		else if ((mediasReceivedInTheNewMediaRuleFile == null || mediasReceivedInTheNewMediaRuleFile.isEmpty()) && existingFeedMediaCompactedDetail != null
				&& existingFeedMediaCompactedDetail.getBasicMediaInfo() != null && !existingFeedMediaCompactedDetail.getBasicMediaInfo().isEmpty()) {
			List<CompactMediaInfo> existingMediasUpdatedInDataStore = null;
			if (existingFeedMediaCompactedDetail != null && existingFeedMediaCompactedDetail.getBasicMediaInfo() != null
					&& !existingFeedMediaCompactedDetail.getBasicMediaInfo().isEmpty()) {
				existingMediasUpdatedInDataStore = existingFeedMediaCompactedDetail.getBasicMediaInfo().values().stream()
						.filter(mediasFromDataStore -> (currentBlackoutRulesCompilationTime > mediasFromDataStore.getLastUpdateTimeInMS())).collect(Collectors.toList());

			}
			for (CompactMediaInfo mdiaInfo : existingMediasUpdatedInDataStore) {
				Media m = SCTE224_DATA_MANAGER.getMediaBySignalIdV1(feedId, mdiaInfo.getSignalid());
				if (m != null) {
					mediasThatAreDeleted.add(m);
				}
			}
		} else {
			//New/Update/Delete
			Map<String, Media> mediasFromRuleFile = mediasReceivedInTheNewMediaRuleFile.stream().collect(Collectors.toMap(Media::getSignalid, Function.identity()));
			Map<String, CompactMediaInfo> mediasFromDataStore = existingFeedMediaCompactedDetail.getBasicMediaInfo().values().stream()
					.collect(Collectors.toMap(CompactMediaInfo::getSignalid, Function.identity()));

			for (CompactMediaInfo mediaFromDataStore : existingFeedMediaCompactedDetail.getBasicMediaInfo().values()) {
				if (currentBlackoutRulesCompilationTime < mediaFromDataStore.getLastUpdateTimeInMS()) {
					Media m = SCTE224_DATA_MANAGER.getMediaBySignalIdV1(feedId, mediaFromDataStore.getSignalid());
					mediasReceivedInTheNewMediaRuleFile.add(m);
					LOGGER.info(() -> "Compilation happened before media ingested; so no action taken on media: " + mediaFromDataStore.getSignalid());
					continue;
				}

				//If the same Media is present at both the places then we might need the rescheduling.
				Media mediaFromRulesFile = mediasFromRuleFile.get(mediaFromDataStore.getSignalid());
				if (mediaFromRulesFile != null) {
					//Reschedule only if the Media has changed since last time it was updated in the persistence store.
					if (mediaFromRulesFile.getLastUpdateTimeInMS() > mediaFromDataStore.getLastUpdateTimeInMS()) {
						mediasThatAreUpdated.add(mediasFromRuleFile.get(mediaFromDataStore.getSignalid()));
					} else if (mediaFromRulesFile.getLastUpdateTimeInMS() == mediaFromDataStore.getLastUpdateTimeInMS()) {
						//Reschedule if the Feed or AP has changed even though the Media has not changed.
						if (FeedConfigBean.getInstance().getLastUpdatedTime(feedId) > mediaFromRulesFile.getLastUpdateTimeInMS()) {
							mediasThatAreUpdated.add(mediasFromRuleFile.get(mediaFromDataStore.getSignalid()));
						} else {
							LOGGER.info(() -> "Media " + mediaFromRulesFile.getSignalid() + " with an updated version already present "
									+ "in the persistence store so not updating it again. And neither touching scheduling for this Media.");
						}
					} else {
						LOGGER.info(() -> "Media " + mediaFromRulesFile.getSignalid() + " with an updated version already present "
								+ "in the persistence store so not updating it again. And neither touching scheduling for this Media.");
					}
				} else {
					//If the media from Data store is not present in the rules file, then we need to cancel the scheduled event for that Media if any.
					Media m = SCTE224_DATA_MANAGER.getMediaBySignalIdV1(feedId, mediaFromDataStore.getSignalid());
					mediasThatAreDeleted.add(m);
				}
			}

			mediasThatAreNewlyAdded.addAll(mediasReceivedInTheNewMediaRuleFile.stream()
					.filter(media -> !mediasFromDataStore.containsKey(media.getSignalid()))
					.collect(Collectors.toList()));
	}

	}

	/**
	 * @param datafile
	 * @return
	 * @throws ParseException
	 */
	private long getCompileTime(String datafile) throws ParseException {
		int index = datafile.indexOf("_");
		String datestr = datafile.substring(0, index);
		SimpleDateFormat format = new SimpleDateFormat(MEDIA_FEED_DATA_FILE_DATEFORMAT);
		TimeZone tzone = TimeZone.getTimeZone("UTC");
		format.setTimeZone(tzone);
		Date date = format.parse(datestr);
		return date.getTime();
	}

	private String[] getDataFilesToLoad(final String feedId) {
		File feedDir = new File(LoaderUtil.getFeedDataDirectory(feedId));

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File file, String name) {
				return name.endsWith(MEDIA_FEED_DATA_FILE_SUFFIX); 
			}
		};

		String[] datafiles = feedDir.list(filter);
		if (datafiles != null && datafiles.length > 0) {        
			Arrays.sort(datafiles);
		}
		return datafiles;
	}
	
}
