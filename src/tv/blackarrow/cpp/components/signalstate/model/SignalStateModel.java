package tv.blackarrow.cpp.components.signalstate.model;

public class SignalStateModel {

	private String signalId;
	private String actualSignalStr;

	public SignalStateModel(String signalId, String actualSignalStr) {
		this.signalId = signalId;
		this.actualSignalStr = actualSignalStr;
	}

	/**
	 * @return the signalId
	 */
	public String getSignalId() {
		return signalId;
	}

	/**
	 * @param signalId the signalId to set
	 */
	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	/**
	 * @return the actualSignalStr
	 */
	public String getActualSignalStr() {
		return actualSignalStr;
	}

	/**
	 * @param actualSignalStr the actualSignalStr to set
	 */
	public void setActualSignalStr(String actualSignalStr) {
		this.actualSignalStr = actualSignalStr;
	}

}
