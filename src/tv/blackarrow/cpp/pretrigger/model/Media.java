package tv.blackarrow.cpp.pretrigger.model;

import java.util.List;

public class Media {
	private String id;
	private List<MediaPoint> mediaPoints;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<MediaPoint> getMediaPoints() {
		return mediaPoints;
	}

	public void setMediaPoints(List<MediaPoint> mediaPoints) {
		this.mediaPoints = mediaPoints;
	}

	@Override
	public String toString() {
		return "Media [id=" + id + ", mediaPoints=" + mediaPoints + "]";
	}
}
