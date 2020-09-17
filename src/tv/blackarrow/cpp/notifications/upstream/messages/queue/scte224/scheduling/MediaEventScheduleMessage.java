/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.scheduling;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.model.scte224.Media;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class MediaEventScheduleMessage {
	
	@Expose
	private MediaNotificationJobAction action;
	@Expose
	private String feedId;
	@Expose
	private List<Media> medias;
	
	/**
	 * @param feedId
	 * @param medias
	 */
	public MediaEventScheduleMessage(final MediaNotificationJobAction action, final String feedId, final List<Media> medias) {
		super();
		if(action == null || feedId == null || medias== null) {
			throw new IllegalArgumentException("All parameters are mandatory to initialize MediaEventScheduleMessage.");
		}
		this.action = action;
		this.feedId = feedId;
		this.medias = Collections.unmodifiableList(medias);
	}
	
	/**
	 * @return the action
	 */
	public MediaNotificationJobAction getAction() {
		return action;
	}

	/**
	 * @return the feedId
	 */
	public String getFeedId() {
		return feedId;
	}
	
	/**
	 * @return the medias
	 */
	public List<Media> getMedias() {
		return medias;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("MediaEventScheduleMessage [action=%s, feedId=%s, medias=%s]", action, feedId, medias);
	}
	
}
