package tv.blackarrow.cpp.model;

import java.util.HashMap;

public class SignalProcessorCursor {

    private String acquisitionPointIdentity;

    // intentionally using a concrete type here so that deserialization works cleanly
    private HashMap<String, String> nextPOKeyByZone;

    // intentionally using a concrete type here so that deserialization works cleanly
    private HashMap<String, Boolean> lastProcessedPOKeyByZone;
    
    private long lastConfirmedPOUTCTime;
    private long lastConfirmedBlackoutUTCTime;
    private String lastConfirmedBlackoutSignalId;
    
    private Long casId;
    
    public String getAcquisitionPointIdentity() {
        return acquisitionPointIdentity;
    }

    public void setAcquisitionPointIdentity(String acquisitionPointIdentity) {
        this.acquisitionPointIdentity = acquisitionPointIdentity;
    }

    public HashMap<String, String> getNextPOKeyByZone() {
        return nextPOKeyByZone;
    }

    public void setNextPOKeyByZone(HashMap<String, String> nextPOKeyByZone) {
        this.nextPOKeyByZone = nextPOKeyByZone;
    }

    public HashMap<String, Boolean> getLastProcessedPOKeyByZone() {
        return lastProcessedPOKeyByZone;
    }

    public void setLastProcessedPOKeyByZone(HashMap<String, Boolean> lastProcessedPOKeyByZone) {
        this.lastProcessedPOKeyByZone = lastProcessedPOKeyByZone;
    }

    public long getLastConfirmedPOUTCTime() {
		return lastConfirmedPOUTCTime;
	}

	public void setLastConfirmedPOUTCTime(long lastConfirmedPOUTCTime) {
		this.lastConfirmedPOUTCTime = lastConfirmedPOUTCTime;
	}

    public long getLastConfirmedBlackoutUTCTime() {
		return lastConfirmedBlackoutUTCTime;
	}

	public void setLastConfirmedBlackoutUTCTime(long lastConfirmedBlackoutUTCTime) {
		this.lastConfirmedBlackoutUTCTime = lastConfirmedBlackoutUTCTime;
	}

	public Long getCasId() {
        return casId;
    }

    public void setCasId(Long casId) {
        this.casId = casId;
    }

	public String getLastConfirmedBlackoutSignalId() {
		return lastConfirmedBlackoutSignalId;
	}

	public void setLastConfirmedBlackoutSignalId(String lastConfirmedBlackoutSignalId) {
		this.lastConfirmedBlackoutSignalId = lastConfirmedBlackoutSignalId;
	}

}
