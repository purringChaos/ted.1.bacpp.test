package tv.blackarrow.cpp.components.util;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.BlackoutEvent;

public class DataUpdateHelper {
	private static final Logger LOGGER = LogManager.getLogger(DataUpdateHelper.class);
	public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	/*
	 * Feed Reference List((B:feedId) for blackouts are important to be updated otherwise they will have wrong values in reference.
	 */
	public static void updateFeedExtRefInCouchbase(DataManager dataManager, BlackoutEvent updatedBlackoutEvent, ArrayList<BlackoutEvent> blackoutEvents) {
		for (int i = 0; i < blackoutEvents.size(); i++) {
			BlackoutEvent currentEvent = blackoutEvents.get(i);
			if (currentEvent.getEventId().equals(updatedBlackoutEvent.getEventId())) {
				blackoutEvents.set(i, updatedBlackoutEvent);
				break;
			}
		}
		StringDocument lockedDocument = null;
		try {
			lockedDocument = ESSClusterLockUtil.acquireClusterWideLockWithFeedExternalRef(updatedBlackoutEvent.getFeedExtRef());
			if (lockedDocument != null) {
				dataManager.putBlackoutEvents(updatedBlackoutEvent.getFeedExtRef(), GSON.toJson(blackoutEvents, 
						new TypeToken<ArrayList<BlackoutEvent>>() {}.getType()));
			} else {
				errorLog("This ESS node was not able to take a lock required for updating the blackout event's list against the feed '" + updatedBlackoutEvent.getFeedExtRef() + 
						"'. So failed to update the blackout list for this feed.");
			}
		} catch (Exception ex) {
			errorLog("POIS real-time update failed to complete the process for event \"" + updatedBlackoutEvent.getEventId() + "\" configured for feed \""
					+ updatedBlackoutEvent.getFeedExtRef() + "\". Following unexpected error occured: " + ex.getMessage(), ex);
		} finally {
			ESSClusterLockUtil.releaseClusterWideLock(lockedDocument);
		}
	}
	
	private static void errorLog(String message) {
		errorLog(message, null);
	}

	private static void errorLog(String message, Throwable exception) {
		LOGGER.error(()->message, exception);
	}
}
