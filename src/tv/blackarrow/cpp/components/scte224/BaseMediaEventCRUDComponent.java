/**
 * 
 */
package tv.blackarrow.cpp.components.scte224;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tv.blackarrow.cpp.components.util.ESSClusterLockUtil;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler.MediaCRUDNotificationsHandler;
import tv.blackarrow.cpp.utils.EventAction;

/**
 * @author yrajoria
 * 
 * This components handles real time updates to the Media event data in POIS due to any actions performed by Media by SCTE224 interface
 *
 */
public class BaseMediaEventCRUDComponent {

	private static final Logger LOGGER = LogManager.getLogger(BaseMediaEventCRUDComponent.class);
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	private static final DataManager dataManager = DataManagerFactory.getInstance();
	private static final SCTE224DataManager scte224DataManager = DataManagerFactory.getSCTE224DataManager();
	private static final boolean IS_INFO_ENABLED = LOGGER.isInfoEnabled();

	public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	private final DataManager DATA_MANAGER = DataManagerFactory.getInstance();

	/**
	 * @param medias
	 * @param action
	 * @param feedId
	 */
	protected void handleRealtimeEventCUDOperations(ArrayList<Media> medias, ArrayList<Media> mediasWithRestrictions, EventAction action, String feedId) {

		//Fetch all the acquisition points associated to this event.
		Set<String> acquisitionPointIdsForThisMedia = dataManager.getFeedToAcquistionMap(feedId);
		if (acquisitionPointIdsForThisMedia == null || acquisitionPointIdsForThisMedia.isEmpty()) {
			infoLog("No acquisition point found for feed id \"" + feedId + "\". So ignoring this message with no update to runtime.");
			return;
		}

		StringDocument lockedDocument = null;
		try {
			// Synchronize the actions between the loader and real-time updates to POIS across cluster.
			lockedDocument = ESSClusterLockUtil.acquireClusterWideLockWithFeedExternalRef(feedId);
			//updateThisEventInDataStore(feedId, medias, mediasWithRestrictions, action);//*****************Remove this in 9.4 Release *****************
			updateThisEventInDataStoreV1(feedId, medias, action);
		} catch (Exception ex) {
			errorLog("POIS real-time update failed to complete the process for event for configured for feed \"" + feedId + "\". Following unexpected error occured: "
					+ ex.getMessage(), ex);
		} finally {
			ESSClusterLockUtil.releaseClusterWideLock(lockedDocument);
		}
	}

	private void updateThisEventInDataStoreV1(final String feedId, final ArrayList<Media> mediasReceivedInRealtimeMessage, final EventAction action) {
		LOGGER.debug("Updating Event in DataStore");
		Set<String> acqPointids = DATA_MANAGER.getFeedToAcquistionMap(feedId);
		List<AcquisitionPoint> acqPoints = DATA_MANAGER.getAcquisitionPoints(acqPointids);
		switch (action) {
		case CREATE:
			createMediasV1(feedId, mediasReceivedInRealtimeMessage, acqPoints);
			break;
		case UPDATE:
			updateMediasV1(feedId, mediasReceivedInRealtimeMessage, acqPoints, EventAction.COMPLETE);
			break;
		case STOP_NOW:
			mediasReceivedInRealtimeMessage.forEach(media -> media.setInflMediaDeleted(true));
			updateMediasV1(feedId, mediasReceivedInRealtimeMessage, acqPoints, EventAction.STOP_NOW);
			break;
		case DELETE:
			deleteMediasV1(feedId, mediasReceivedInRealtimeMessage, acqPoints);
			break;
		}

	}

	/*
	 * Making sure Update/New has been both comes in this flow
	 */
	private void updateMediasV1(final String feedId, final ArrayList<Media> mediasReceivedInRealtimeMessage, final List<AcquisitionPoint> acqPoints, final EventAction action) {

		final List<Media> mediasToBeUpdated = new ArrayList<>();
		final List<Media> mediasToBeNewlyAdded = new ArrayList<>();
		List<Media> mediasToBeDeleted = new ArrayList<>();

		//2018 SCTE-224 
		scte224DataManager.saveMediasDuringRealTimeUpdateV1(feedId, mediasReceivedInRealtimeMessage, false, mediasToBeNewlyAdded, mediasToBeUpdated, mediasToBeDeleted);
		MediaCRUDNotificationsHandler.notify(acqPoints, action,mediasToBeNewlyAdded,mediasToBeUpdated, mediasToBeDeleted);
	}

	/*
	 * Add the new media points to existing list
	 */
	private void createMediasV1(final String feedId, final List<Media> mediasReceivedInRealtimeMessage, List<AcquisitionPoint> acqPointsForAGivenFeed) {
		//2018 SCTE-224 

		final List<Media> mediasToBeUpdated = new ArrayList<>();
		final List<Media> mediasToBeNewlyAdded = new ArrayList<>();
		List<Media> mediasToBeDeleted = new ArrayList<>();
		scte224DataManager.saveMediasDuringRealTimeUpdateV1(feedId, mediasReceivedInRealtimeMessage, false, mediasToBeNewlyAdded, mediasToBeUpdated, mediasToBeDeleted);

		//There will not be any Updated/Delete in this case. So we are not notifying
		MediaCRUDNotificationsHandler.notify(acqPointsForAGivenFeed, null, mediasToBeNewlyAdded, mediasToBeUpdated, mediasToBeDeleted);
	}

	/*
	 * Delete the media points from existing list
	 */
	private void deleteMediasV1(final String feedId, final List<Media> mediasReceivedInRealtimeMessage, final List<AcquisitionPoint> acqPointsForAGivenFeed) {

		final List<Media> mediasToBeUpdated = new ArrayList<>();
		final List<Media> mediasToBeNewlyAdded = new ArrayList<>();
		List<Media> mediasToBeDeleted = new ArrayList<>();
		//2018 SCTE-224 
		scte224DataManager.deleteMediasV1(feedId, mediasReceivedInRealtimeMessage, mediasToBeDeleted);

		//There will not be any Create/Update in this case. So we are not notifying
		MediaCRUDNotificationsHandler.notify(acqPointsForAGivenFeed, null, mediasToBeNewlyAdded, mediasToBeUpdated, mediasToBeDeleted);

	}
	
	/**
	 * Appends the message to cpp.log at info level.
	 * 
	 * @param message
	 */
	protected static void infoLog(String message) {
		if (IS_INFO_ENABLED) {
			LOGGER.info(message);
		}
	}

	/**
	 * Appends the message to cpp.log at error level.
	 * 
	 * @param message
	 * @param exception
	 **/
	protected static void errorLog(String message, Throwable exception) {
		LOGGER.error(message, exception);
	}

	/**
	 * Appends the message to cpp.log at error level.
	 * 
	 * @param message
	 */
	protected static void errorLog(String message) {
		LOGGER.error(message);
	}

}
