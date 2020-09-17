package tv.blackarrow.cpp.log.model;

public class PoisAuditLogVO {
	/**
	 * @param ipAddressOfClient
	 * @param acquisitionSignalID
	 * @param altEventId
	 */
	public PoisAuditLogVO(String ipAddressOfClient, String acquisitionSignalID, String altEventId) {
		super();
		this.ipAddressOfClient = ipAddressOfClient;
		this.acquisitionSignalID = acquisitionSignalID;
		this.altEventId = altEventId;
	}

	public PoisAuditLogVO() {

	}

	private static final String EMPTY_STRING = "";
	private String ipAddressOfClient;
	private String acquisitionSignalID;
	private String altEventId;

	public String getIpAddressOfClient() {
		return ipAddressOfClient != null ? ipAddressOfClient.trim() : EMPTY_STRING;
	}

	public void setIpAddressOfClient(String ipAddressOfClient) {
		this.ipAddressOfClient = ipAddressOfClient;
	}

	public String getAcquisitionSignalID() {
		return acquisitionSignalID != null ? acquisitionSignalID.trim() : EMPTY_STRING;
	}

	public void setAcquisitionSignalID(String acquisitionSignalID) {
		this.acquisitionSignalID = acquisitionSignalID;
	}

	public String getAltEventId() {
		return altEventId;
	}

	public void setAltEventId(String altEventId) {
		this.altEventId = altEventId;
	}
}
