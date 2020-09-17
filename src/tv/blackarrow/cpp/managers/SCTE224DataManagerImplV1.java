/**
 * 
 */
package tv.blackarrow.cpp.managers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.loader.bo.MediaPointAssertsParser;
import tv.blackarrow.cpp.model.scte224.ApplyorRemove;
import tv.blackarrow.cpp.model.scte224.CompactMediaInfo;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.MatchSignal;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class SCTE224DataManagerImplV1 implements SCTE224DataManager {

	private static final String COLON_SEPARATOR = ":";
	// gson is thread safe so we only need one instance to use everywhere
	private static final SCTE224DataManager SCTE224_DATA_MANAGER = new SCTE224DataManagerImplV1();

	private SCTE224DataManagerImplV1() {
		super();
		warmup();
	}

	public static SCTE224DataManager getInstance() {
		return SCTE224_DATA_MANAGER;
	}

	@Override
	public FeedMediaCompactedDetail getFeedMediaCompactedDetailV1(String feedID) {
		return getObjectFromDataStore(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID, new TypeToken<FeedMediaCompactedDetail>() {
		}.getType());
	}

	@Override
	public void deleteMediasV1(String feedID, List<Media> medias, List<Media> mediasToBeDeleted) {
		FeedMediaCompactedDetail feedMediaCompactedDetails = getFeedMediaCompactedDetailV1(feedID);
		for (Media media : medias) {
			if (feedMediaCompactedDetails != null && feedMediaCompactedDetails.getBasicMediaInfo() != null) {
				Iterator<Entry<String, CompactMediaInfo>> iterator = feedMediaCompactedDetails.getBasicMediaInfo().entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, CompactMediaInfo> entry = iterator.next();
					if (entry != null && entry.getValue() != null && (entry.getValue().getMediaSignalId().equals(media.getMediaSignalid())|| entry.getValue().getSignalid().equals(media.getSignalid()))) {
						CompactMediaInfo compactMediaInfo = entry.getValue();
						if (compactMediaInfo != null) {
							mediasToBeDeleted.add(getMediaBySignalIdV1(feedID, compactMediaInfo.getSignalid())); //Add in Deleted List				
							DATAMANAGER.delete(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedID + COLON_SEPARATOR + compactMediaInfo.getSignalid());// Remove FMD
							iterator.remove();// Remove from Feed Detail
						}
					}
				}
			}
		}

		if (feedMediaCompactedDetails.getBasicMediaInfo() != null && feedMediaCompactedDetails.getBasicMediaInfo().size() == 0) {
			DATAMANAGER.delete(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID); //Either delete FMC
		} else {
			saveObjectInDataStore(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID, feedMediaCompactedDetails, new TypeToken<FeedMediaCompactedDetail>() {
			}.getType());//Or save it
		}
	}

	/*
	 * FMC:Feed and Medias reference
	 * FMD:Actual Media Detail
	 * (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.SCTE224DataManager#saveMediaV1(java.lang.String, java.util.LinkedList)
	 */
	@Override
	public void saveMediasDuringRuleFileLoadV1(String feedID, List<Media> medias, FeedMediaCompactedDetail feedMediaCompactedDetailInDataStoreCB, boolean loadingFromRuleFile, Long currentBlackoutRulesCompilationTime) {
		loadRuleFile(feedID, medias, feedMediaCompactedDetailInDataStoreCB, currentBlackoutRulesCompilationTime);
	}

	/*
	 * FMC:Feed and Medias reference
	 * FMD:Actual Media Detail
	 * (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.SCTE224DataManager#saveMediasDuringRealTimeUpdateV1(java.lang.String, java.util.List, tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail, boolean, java.util.List, java.util.List, java.util.List)
	 */
	@Override
	public void saveMediasDuringRealTimeUpdateV1(String feedID, List<Media> medias, boolean loadingFromRuleFile, List<Media> mediasToBeNewlyAdded, List<Media> mediasToBeUpdated,
			List<Media> mediasToBeDeleted) {
		loadRealTimeUpdate(feedID, medias, mediasToBeNewlyAdded, mediasToBeUpdated, mediasToBeDeleted);
	}
	//Recommended : Get of  media ledger should be called at the start of the workflow. Avoid multiple retrieval 
	@Override
	public MediaLedger getAcquisitionPointMediaLedger(String acquisitionPointIdentity, String mediaSignalId) {
		MediaLedger apMediaLedger = getObjectFromDataStore(AP_MEDIA_LEDGER_NAME_SPACE + acquisitionPointIdentity + NAME_SPACE_VALUE_SEPARATOR + mediaSignalId, MediaLedger.class);

		return apMediaLedger;
	}
	//Recommended : After end of the request flow save media ledger should be called. Avoid intermediate saving
	@Override
	public void saveAcquisitionPointMediaLedger(final MediaLedger apMediaLedger, final String acquisitionPointId, final String mediaSignalId) {
		saveObjectInDataStore(AP_MEDIA_LEDGER_NAME_SPACE + acquisitionPointId + NAME_SPACE_VALUE_SEPARATOR + mediaSignalId, apMediaLedger, MediaLedger.class);
	}

	@Override
	public long getLastMediaCompilationTime(String feedId) {
		String value = DATAMANAGER.get(MEDIA_FILE_COMPILATION_TIME_NAMESPACE + feedId);
		if (value != null && !value.isEmpty()) {
			Long time = Long.parseLong(value);
			return time.longValue();
		} else {
			return 0;
		}
	}

	@Override
	public void saveCompilationTimeForTheLoadedMediaData(String feedId, long compilationTime) {
		DATAMANAGER.set(MEDIA_FILE_COMPILATION_TIME_NAMESPACE + feedId, String.valueOf(compilationTime));
	}

	@Override
	public void putOutOfBandNotificationStatus(String triggerId, String status) {
		DATAMANAGER.set(MEDIA_OUT_OF_BAND_NOTIFICATION_STATUS_NAMESPACE + NAME_SPACE_VALUE_SEPARATOR + triggerId, status);
	}

	@Override
	public String getOutOfBandNotificationStatus(String triggerId) {
		return DATAMANAGER.get(MEDIA_OUT_OF_BAND_NOTIFICATION_STATUS_NAMESPACE + NAME_SPACE_VALUE_SEPARATOR + triggerId);
	}

	@Override
	public void removeOutOfBandNotificationStatus(String triggerId) {
		DATAMANAGER.delete(MEDIA_OUT_OF_BAND_NOTIFICATION_STATUS_NAMESPACE + NAME_SPACE_VALUE_SEPARATOR + triggerId);
	}

	@Override
	public Media getMediaBySignalIdV1(String feedId, String signalId) {
		return getObjectFromDataStore(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedId + COLON_SEPARATOR + signalId, new TypeToken<Media>() {
		}.getType());
	}
	
	private <T> T getObjectFromDataStore(final String couchbaseSearchKey, final Class<T> couchbaseValueObjectClass) {
		String objectJsonStringFromDataStore = DATAMANAGER.get(couchbaseSearchKey);
		if (StringUtils.isNotBlank(objectJsonStringFromDataStore)) {
			return GSON.fromJson(objectJsonStringFromDataStore, couchbaseValueObjectClass);
		}
		return null;
	}

	@Override
	public <T> T getObjectFromDataStore(final String couchbaseSearchKey, final Type couchbaseValueObjectType) {
		String objectJsonStringFromDataStore = DATAMANAGER.get(couchbaseSearchKey);
		if (StringUtils.isNotBlank(objectJsonStringFromDataStore)) {
			return GSON.fromJson(objectJsonStringFromDataStore, couchbaseValueObjectType);
		}
		return null;
	}

	@Override
	public void saveObjectInDataStore(final String couchbaseSearchKey, final Object couchbaseValueObject, final Type typeOfObject) {
		DATAMANAGER.set(couchbaseSearchKey, typeOfObject != null ? GSON.toJson(couchbaseValueObject, typeOfObject) : GSON.toJson(couchbaseValueObject));
	}

	protected void warmup() {
		// to reduce latency during requests, we will run every object through gson one time during initialization.
		GSON.fromJson(GSON.toJson(new Media()), Media.class);
		GSON.fromJson(GSON.toJson(new MediaPoint()), MediaPoint.class);
		GSON.fromJson(GSON.toJson(new MatchSignal()), MatchSignal.class);
		GSON.fromJson(GSON.toJson(new ApplyorRemove()), ApplyorRemove.class);
		GSON.fromJson(GSON.toJson(MediaLedger.getEmptyLedger()), MediaLedger.class);
		GSON.fromJson(GSON.toJson(MediaTransaction.getEmptyTransaction()), MediaTransaction.class);

	}
	
	private boolean isProgramRunOver(Media media, String feedID) {
		Media feedMediaDetails = getMediaBySignalIdV1(feedID, media.getSignalid());
		if (feedMediaDetails != null) {
			MediaPoint endMediaPoint = getEndMediaPoint(media);
			MediaPoint existingEndMediaPoint = getEndMediaPoint(feedMediaDetails);
			if (endMediaPoint != null && existingEndMediaPoint != null && (existingEndMediaPoint.getExpiresTimeInMS() < endMediaPoint.getExpiresTimeInMS())
					|| (existingEndMediaPoint.getMatchTimeInMS() < endMediaPoint.getMatchTimeInMS())) {
				return true;
			}
		}
		return false;
	}

	private void loadRealTimeUpdate(String feedID, List<Media> medias, List<Media> mediasToBeNewlyAdded, List<Media> mediasToBeUpdated, List<Media> mediasToBeDeleted) {
		
		if (medias != null && !medias.isEmpty()) {
			FeedMediaCompactedDetail feedMediaCompactedDetailInDataStoreCB = getFeedMediaCompactedDetailV1(feedID);
			if (feedMediaCompactedDetailInDataStoreCB == null) {
				feedMediaCompactedDetailInDataStoreCB = new FeedMediaCompactedDetail(feedID);
			}
			
			String mediaSignalIDPrefix = null;
			
			//Step1: Find Existing Compact Info, that needs to be erased from DB
			Map<String, CompactMediaInfo> currentCompactedMediaInfo = feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo();

			//Step1: Add/Update the incoming in DB
			for (Media each : medias) {
				Boolean programRunover =false;
				populateAssert(each);
				mediaSignalIDPrefix = each.getMediaSignalid() + MEDIA_TO_SIGNALID_SEPARATOR;//Storing the Prefix to know which Media updates/creation did we receive
				if(CppUtil.isInflightMedia(each.getEffectiveTimeInMS(), each.getExpiresTimeInMS(), System.currentTimeMillis())) {
					 programRunover = isProgramRunOver(each, feedID);
				}
				saveObjectInDataStore(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedID + COLON_SEPARATOR + each.getSignalid(), each, new TypeToken<Media>() {
				}.getType());//Saved the new Detail to Name Space
				
				String media_signalID_key = each.getMediaSignalid() + MEDIA_TO_SIGNALID_SEPARATOR + each.getSignalid();
				CompactMediaInfo newCompactInfo = new CompactMediaInfo(each.getEffectiveTimeInMS(), each.getExpiresTimeInMS(), each.getLastUpdateTimeInMS(), each.getSignalid(),
						each.getMediaSignalid(), each.getMediaPoints().get(0).getMatchSignal() != null);
				each.setProgramRunover(programRunover);
				if (currentCompactedMediaInfo.get(media_signalID_key) != null) {
					mediasToBeUpdated.add(each);
				} else {
					mediasToBeNewlyAdded.add(each);
				}

				currentCompactedMediaInfo.put(media_signalID_key, newCompactInfo);// Added the detail to Feed Level Store FMC
			}

			
			Map<String, CompactMediaInfo> toBeDeletedMediaDetails = new HashMap<>();
			//verify on this media, which signals are no more valid. Partial Signals will be deleted. (This is specially used for Realtime Update)
			Iterator<Entry<String, CompactMediaInfo>> iterator = currentCompactedMediaInfo.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, CompactMediaInfo> entry = iterator.next();
				String key = entry.getKey();
				if (key.startsWith(mediaSignalIDPrefix) && !keyIsInThisMediaList(key, mediasToBeNewlyAdded) && !keyIsInThisMediaList(key, mediasToBeUpdated)) {
					toBeDeletedMediaDetails.put(entry.getKey(), entry.getValue());//Add it in the list to delete FMD name space
					
					mediasToBeDeleted.add(getMediaBySignalIdV1(feedID, entry.getValue().getSignalid()));// Find that deleted Media from Couchbase to handle RealTimeUpdate
					iterator.remove();// Remove from Feed Detail
				} 
			}

			saveObjectInDataStore(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID, feedMediaCompactedDetailInDataStoreCB, new TypeToken<FeedMediaCompactedDetail>() {
			}.getType());

			//Step3: Clean after older Medias
			deletePreviousMediasV1(feedID, toBeDeletedMediaDetails);

		}

	}

	private boolean keyIsInThisMediaList(String key, List<Media> mediasToBeNewlyAdded) {
		boolean found = false;
		for(Media m: mediasToBeNewlyAdded) {
			if(key.equals(m.getMediaSignalid() + MEDIA_TO_SIGNALID_SEPARATOR + m.getSignalid())){
				found = true;
			}
		}
		return found;
	}

	private List<Media> loadRuleFile(String feedID, List<Media> medias, FeedMediaCompactedDetail feedMediaCompactedDetailInDataStoreCB, Long currentBlackoutRulesCompilationTime) {
		List<Media> existingMediaToBeRemoved = new ArrayList<Media>();
		//Save The Received List
		if (medias != null && !medias.isEmpty()) {
			if (feedMediaCompactedDetailInDataStoreCB == null) {
				feedMediaCompactedDetailInDataStoreCB = new FeedMediaCompactedDetail(feedID);
			}
			//Step1: Find Existing Compact Info, that needs to be erased from DB
			Map<String, CompactMediaInfo> existingMediaDetailsToBeRemoved = new HashMap<>(feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo());

			//Step2: Add New media's in DB
			feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo().clear();
			for (Media each : medias) {
				if(each==null) {
					continue;
				}
				populateAssert(each);
				saveObjectInDataStore(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedID + COLON_SEPARATOR + each.getSignalid(), each, new TypeToken<Media>() {}.getType());
				existingMediaDetailsToBeRemoved.remove(each.getMediaSignalid() + MEDIA_TO_SIGNALID_SEPARATOR + each.getSignalid());
				CompactMediaInfo newCompactInfo = new CompactMediaInfo(each.getEffectiveTimeInMS(), each.getExpiresTimeInMS(), each.getLastUpdateTimeInMS(), each.getSignalid(),
						each.getMediaSignalid(), each.getMediaPoints().get(0).getMatchSignal() != null);
				feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo().put(each.getMediaSignalid() + MEDIA_TO_SIGNALID_SEPARATOR + each.getSignalid(), newCompactInfo);
			}
			saveObjectInDataStore(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID, feedMediaCompactedDetailInDataStoreCB, new TypeToken<FeedMediaCompactedDetail>() {
			}.getType());
			
			//These will be deleted
			if (existingMediaDetailsToBeRemoved.values() != null
					&& !existingMediaDetailsToBeRemoved.values().isEmpty()) {
				Iterator<CompactMediaInfo> it = existingMediaDetailsToBeRemoved.values().iterator();
				while (it.hasNext()) {
					CompactMediaInfo mediaInfo = it.next();
					if (mediaInfo.getLastUpdateTimeInMS() != null && currentBlackoutRulesCompilationTime != null
							&& currentBlackoutRulesCompilationTime > mediaInfo.getLastUpdateTimeInMS()) {
						existingMediaToBeRemoved.add(getMediaBySignalIdV1(feedID, mediaInfo.getSignalid()));
					} else {
						it.remove();
					}
				}
			}
			//Step3: Clean after older Medias
			deletePreviousMediasV1(feedID, existingMediaDetailsToBeRemoved);
		} //Delete If no more 
		else {
			if (feedMediaCompactedDetailInDataStoreCB != null) {
				if (feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo() != null) {
					for (String signalId : feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo().keySet()) {
						existingMediaToBeRemoved.add(getMediaBySignalIdV1(feedID, feedMediaCompactedDetailInDataStoreCB.getBasicMediaInfo().get(signalId).getSignalid()));
						DATAMANAGER.delete(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedID + COLON_SEPARATOR + signalId);
						
					}
				}
				DATAMANAGER.delete(FEED_MEDIA_COMPACT_INFO_NAME_SPACE + feedID);
			}
		}
		return existingMediaToBeRemoved;
	}

	private static void populateAssert(Media each) {
		for (MediaPoint mediaPoint : each.getMediaPoints()) {
			MediaPointAssertsParser.parse(mediaPoint);
		}
	}

	private void deletePreviousMediasV1(String feedID, Map<String, CompactMediaInfo> medias) {
		for (CompactMediaInfo media : medias.values()) {
			DATAMANAGER.delete(FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE + feedID + COLON_SEPARATOR + media.getSignalid());
		}
		
	}
	private MediaPoint getEndMediaPoint(Media media) {
		Optional<MediaPoint> mediapoint = media.getMediaPoints().stream().filter(mp -> mp.getMatchSignal()
				.getSegmentationTypeIds().contains(SegmentType.PROGRAM_END.getSegmentTypeId())).findFirst();
		return mediapoint.get();
	}
}
