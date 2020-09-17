package tv.blackarrow.cpp.model.scte224;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

public class FeedMediaCompactedDetail {

	@Expose
	private String feedId = null;
	@Expose
	Map<String, CompactMediaInfo> basicMediaInfo = new HashMap<String, CompactMediaInfo>();

	public FeedMediaCompactedDetail(String feedId) {
		super();
		this.feedId = feedId;
	}

	public String getFeedId() {
		return feedId;
	}

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public Map<String, CompactMediaInfo> getBasicMediaInfo() {
		return basicMediaInfo == null ? new HashMap<>() : basicMediaInfo;
	}

	public void setBasicMediaInfo(Map<String, CompactMediaInfo> basicMediaInfo) {
		this.basicMediaInfo = basicMediaInfo;
	}

	@Override
	public String toString() {
		return "FeedMediaMapping [feedId=" + feedId + ", basicMediaInfo=" + basicMediaInfo + "]";
	}

}
