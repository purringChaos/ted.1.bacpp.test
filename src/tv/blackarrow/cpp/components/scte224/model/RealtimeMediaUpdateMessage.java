package tv.blackarrow.cpp.components.scte224.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.utils.EventAction;

/**
 * @author yrajoria
 *
 */
public class RealtimeMediaUpdateMessage {

	@Expose
	private String feedId;

	@Expose
	Map<EventAction, ArrayList<Media>> mediasChanged;

	public RealtimeMediaUpdateMessage(Map<EventAction, ArrayList<Media>> mediasChanged) {
		this.mediasChanged = mediasChanged;
	}

	/**
	 * @return the mediasChanged
	 */
	public Map<EventAction, ArrayList<Media>> getMediasChanged() {
		if (mediasChanged == null) {
			mediasChanged = new HashMap<EventAction, ArrayList<Media>>();
		}
		return mediasChanged;
	}

	/**
	 * @param mediasChanged the mediasChanged to set
	 */
	public void setMediasChanged(Map<EventAction, ArrayList<Media>> mediasChanged) {
		this.mediasChanged = mediasChanged;
	}

	/**
	 * @return the feedId
	 */
	public String getFeedId() {
		return feedId;
	}

	/**
	 * @param feedId the feedId to set
	 */
	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

}