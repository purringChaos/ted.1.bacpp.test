package tv.blackarrow.cpp.model;


public class FeedQueryResponse {
	private boolean status;
	private String signalId;

	public FeedQueryResponse(boolean status, String signalId) {
		this.status = status;
		this.signalId = signalId;
	}
	
	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getSignalId() {
		return signalId;
	}

	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	@Override
	public String toString() {
		return "FeedQueryResponse [status=" + status + ", signalId=" + signalId
				+ "]";
	}

}
