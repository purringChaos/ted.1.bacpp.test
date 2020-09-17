package tv.blackarrow.cpp.model;

public class FeedQueryRequest {
	private String feedId;
	private String eventTime;
	
	public FeedQueryRequest(String feedId, String eventTime) {
		this.feedId = feedId;
		this.eventTime = eventTime;
	}

	public String getFeedId() {
		return feedId;
	}

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public String getEventTime() {
		return eventTime;
	}

	public void setEventTime(String eventTime) {
		this.eventTime = eventTime;
	}
}
