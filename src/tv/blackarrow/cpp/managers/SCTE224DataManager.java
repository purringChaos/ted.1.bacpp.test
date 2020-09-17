/**
 * 
 */
package tv.blackarrow.cpp.managers;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.asserts.Assert;
import tv.blackarrow.cpp.model.scte224.asserts.CompositeAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationTypeIDAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationUpidTypeAssert;
import tv.blackarrow.cpp.model.scte224.asserts.SegmentationUpidValueAssert;

/**
 * @author Amit Kumar Sharma
 *
 */
public interface SCTE224DataManager {

	public static final RuntimeTypeAdapterFactory<Assert> RUNTIME_TYPE_ADAPTER_FACTORY = RuntimeTypeAdapterFactory
		    .of(Assert.class, "type")
		    .registerSubtype(SegmentationTypeIDAssert.class, SegmentationTypeIDAssert.class.getSimpleName())
		    .registerSubtype(SegmentationUpidTypeAssert.class, SegmentationUpidTypeAssert.class.getSimpleName())
			.registerSubtype(SegmentationUpidValueAssert.class, SegmentationUpidValueAssert.class.getSimpleName())
			.registerSubtype(CompositeAssert.class, CompositeAssert.class.getSimpleName());
	
	public static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(RUNTIME_TYPE_ADAPTER_FACTORY).create();
	public static final Gson GSON_WITH_EXCLUDE_EXPOSE = new GsonBuilder()
			.registerTypeAdapterFactory(RUNTIME_TYPE_ADAPTER_FACTORY).excludeFieldsWithoutExposeAnnotation().create();
	
	public static final DataManager DATAMANAGER = DataManagerFactory.getInstance();
	public static final String AP_MEDIA_LEDGER_NAME_SPACE = "APML:";

	public static final String FEED_MEDIA_COMPACT_INFO_NAME_SPACE = "FMC:";//2018 SCTE-224 This will contain the compacted detail about any feed/Media Mapping
	public static final String FEED_MEDIA_MAPPING_DETAIL_NAME_SPACE = "FMD:";//2018 SCTE-224 This will contain the actual Media Detail

	public static final String NAME_SPACE_VALUE_SEPARATOR = ":";
	public static final String MEDIA_FILE_COMPILATION_TIME_NAMESPACE = "MC:";
	public static final String MEDIA_OUT_OF_BAND_NOTIFICATION_STATUS_NAMESPACE = "MOOBNS:";
	
    
    public static final String MEDIA_TO_SIGNALID_SEPARATOR = "_$$$$_";

	/**
	 * 2018 SCTE-224 
	 * Get all the compacted media details present for a given Feed.
	 * 
	 * @param feedID id of the feed for which we want to fetch the Medias.
	 * @return a FeedMediaCompactedDetail for the given feed(if present).
	 */
	public FeedMediaCompactedDetail getFeedMediaCompactedDetailV1(String feedID);	
	
	
	/**
     * 2018 SCTE-224
	 * Deletes the passed Media from SCTE-224 namespace
	 * 
	 * @param feedID Feed to which these medias belong to
	 * @param medias medias to be deleted for the given feed.
	 * @param mediasToBeDeleted 
	 */
	public void deleteMediasV1(String feedID, List<Media> medias, List<Media> mediasToBeDeleted);
	
	/**
     * 2018 SCTE-224
	 * Save the given media list for the given feed id.
	 * 
	 * @param feedID Feed to which these medias belong to
	 * @param medias medias to save for the given feed.
	 * @param currentBlackoutRulesCompilationTime 
	 * @param feedMediaCompactedDetail 
	 */
	public void saveMediasDuringRuleFileLoadV1(String feedID, List<Media> medias, FeedMediaCompactedDetail feedMediaCompactedDetailInDataStoreCB, boolean fromCompilationFile, Long currentBlackoutRulesCompilationTime);

	
	/**
	 * Fetch the compilation time for the last media rule file loaded in datastore.
	 * 
	 * @param feedId id of the feed for which we need to fetch the latest compilation time for which the rules are already loaded in to the system.
	 * @return the compilation time that we store in datastore on every load. This time is for the latest media rule file loaded on the given feed.
	 */
	public long getLastMediaCompilationTime(String feedId);

	/**
	 * Fetch the compilation time for the last media rule file loaded in datastore.
	 * 
	 * @param feedId id of the feed for which we need to fetch the latest compilation time for which the rules are already loaded in to the system.
	 * @param compilationTime compilation time of the latest Media rule file thats being loaded in to Datastore.
	 */
	public void saveCompilationTimeForTheLoadedMediaData(String feedId, long compilationTime);

	/**
	 * Returns the MediaLedger available for the provided acquisition point and MediaSignal ID.
	 */
	public MediaLedger getAcquisitionPointMediaLedger(String acquisitionPointIdentity, String mediaSignalId);

	/**
	 * save an entry for a signal for the given media.
	 * 
	 * @param acquisitionPointId AcquisitionPoint on which this transaction happened.
	 * @param mediaSignalId signal id of the Media for which we want to store this transaction.
	 * @param matchedMediaPoint 
	 * @param mediaTransaction the transaction vo object that contains the signal time, type and total duration of the media after this signal. 
	 */
	public void saveAcquisitionPointMediaLedger(final MediaLedger apMediaLedger, final String acquisitionPointId, final String mediaSignalId);

	/**
	 * Puts the Notification status for a scheduled Job.
	 * 
	 * @param triggerId trigger id of the job that has been scheduled or got executed.
	 * @param status status of the notification.
	 */
	public void putOutOfBandNotificationStatus(String triggerId, String status);
	
	/**
	 * Returns the notification status, it could be NEW, STARTED, SENT or FAIL. 
	 * 
	 * @param triggerId trigger id of the job that has been scheduled or got executed.
	 * @return status of the notification.
	 */
	public String getOutOfBandNotificationStatus(String triggerId);
	
	/**
	 * Remove the Notification status entry from persistence store for a scheduled Job.
	 * 
	 * @param triggerId trigger id of the job that has got deleted.
	 */
	public void removeOutOfBandNotificationStatus(String triggerId);

	
	/**
	 * Fetches the Media for the given Media Signal ID.
	 * 
	 * @param feedId id of the feed that is supposed to have this Media created for.
	 * @param mediaSignalId signal id of the Media.
	 * @return Media if found.
	 */
	public Media getMediaBySignalIdV1(String feedId, String mediaSignalId);
	
	
	void saveMediasDuringRealTimeUpdateV1(String feedID, List<Media> medias, boolean loadingFromRuleFile,
			List<Media> mediasToBeNewlyAdded, List<Media> mediasToBeUpdated, List<Media> mediasToBeDeleted);
	
	public <T> T getObjectFromDataStore(final String couchbaseSearchKey, final Type couchbaseValueObjectType);
	public void saveObjectInDataStore(final String couchbaseSearchKey, final Object couchbaseValueObject, final Type typeOfObject) ;
}
