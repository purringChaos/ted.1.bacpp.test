/**
 * 
 */
package tv.blackarrow.cpp.components.scte224;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import com.google.gson.JsonSyntaxException;

import tv.blackarrow.cpp.components.scte224.model.RealtimeMediaUpdateMessage;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.utils.EventAction;

/**
 * @author amit
 * 
 * This components handles real time updates to the blackout event data in POIS due to any actions performed by user on blackout events.
 *
 */
public class RealtimeMediaEventCRUDComponent extends BaseMediaEventCRUDComponent implements Callable {

	@Override
	public Object onCall(final MuleEventContext muleEventContext) throws Exception {

		long eventRuntimeUpdateStartTime = System.currentTimeMillis();

		String responseMessage = "Success";
		String mediaJSON = muleEventContext.getMessageAsString();
		infoLog("Following message received for Media Event:\n" + mediaJSON);

		RealtimeMediaUpdateMessage realtimeMediaUpdateMessage = null;
		RealtimeMediaUpdateMessage realtimeMediaUpdateMessageWithIncldueNonExpose = null;
		
		try {
			realtimeMediaUpdateMessage = SCTE224DataManager.GSON_WITH_EXCLUDE_EXPOSE.fromJson(mediaJSON, RealtimeMediaUpdateMessage.class);
			realtimeMediaUpdateMessageWithIncldueNonExpose = SCTE224DataManager.GSON.fromJson(mediaJSON, RealtimeMediaUpdateMessage.class);
		} catch (JsonSyntaxException jsonSyntaxException) {
			responseMessage = "Message received is in invalid format.";
			errorLog(responseMessage + "\n" + mediaJSON);
			return responseMessage;
		}

		for (EventAction action : realtimeMediaUpdateMessage.getMediasChanged().keySet()) {
			ArrayList<Media> medias = realtimeMediaUpdateMessage.getMediasChanged().get(action);
			ArrayList<Media> mediasWithRestrictions = realtimeMediaUpdateMessageWithIncldueNonExpose.getMediasChanged().get(action);
			Map<String, List<Media>> mediaPerFeed = medias.stream().collect(Collectors.groupingBy(m -> m.getFeedId()));
			Map<String, List<Media>> mediaPerFeedWithRestrictions = mediasWithRestrictions.stream().collect(Collectors.groupingBy(m -> m.getFeedId()));
			for(String feedId : mediaPerFeed.keySet()) {
				handleRealtimeEventCUDOperations((ArrayList)mediaPerFeed.get(feedId), (ArrayList)mediaPerFeedWithRestrictions.get(feedId),  action, feedId);
			}
		}

		infoLog("POIS took " + (System.currentTimeMillis() - eventRuntimeUpdateStartTime) + " ms to perform the realtime media CRUD action.");

		return responseMessage;
	}

}
