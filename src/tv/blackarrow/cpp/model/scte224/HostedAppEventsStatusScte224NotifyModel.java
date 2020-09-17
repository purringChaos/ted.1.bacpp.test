package tv.blackarrow.cpp.model.scte224;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;

public class HostedAppEventsStatusScte224NotifyModel {

	@Expose
	private HostedAppEventStatusScte224NotifyModel hostedAppEventStatusScte224NotifyModel;
	@Expose
	private Media media;
	@Expose
	private MediaPoint mediaPoint;

	public HostedAppEventStatusScte224NotifyModel getHostedAppEventStatusScte224NotifyModel() {
		return hostedAppEventStatusScte224NotifyModel;
	}

	public void setHostedAppEventStatusScte224NotifyModel(
			HostedAppEventStatusScte224NotifyModel hostedAppEventStatusScte224NotifyModel) {
		this.hostedAppEventStatusScte224NotifyModel = hostedAppEventStatusScte224NotifyModel;
	}

	public Media getMedia() {
		return media;
	}

	public void setMedia(Media media) {
		this.media = media;
	}

	public MediaPoint getMediaPoint() {
		return mediaPoint;
	}

	public void setMediaPoint(MediaPoint mediaPoint) {
		this.mediaPoint = mediaPoint;
	}

	@Override
	public String toString() {
		return "A [hostedAppEventStatusScte224NotifyModel=" + hostedAppEventStatusScte224NotifyModel + ", media="
				+ media + ", mediaPoint=" + mediaPoint + "]";
	}
	
}
