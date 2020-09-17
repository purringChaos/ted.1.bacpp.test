package tv.blackarrow.cpp.model;

import java.util.ArrayList;
import java.util.HashMap;

public class PlacementOpportunity {

	private String poKey;
    private long utcWindowStartTime;
    private int windowDurationMilliseconds;
    private int placementsDurationMilliseconds;
    private String outSignalId;
    private String inSignalId;
    private int breakOrder;
    private Long utcSignalTime;
    private Long casId;
    
    // this is added to accommodate ADI 3.0 schedule ingest
    // each PlacementOpportunity will serve the default PO
    // 		In CCMS case, it will be zone specific PO (break)
    //		In ADI 3.0 case, it will the master zone PO (break)
    
    // this meta data is associated with the default PO
    // intentionally using a concrete type here so that deserialization works cleanly
	private HashMap<String, String> metadata;
	
	// this variations captures the list of variations in ADI 3.0 case only
	// this way we don't need to change the existing code for CCMS case
	// this newly added variations is variations of each master break in ADI 3.0
	// they share all info as the default break except for the following:
	// 		inSignalId
	// 		duration
	//		poid (poKey)
    // intentionally using a concrete type here so that deserialization works cleanly
	private ArrayList<POInfo> variations;

	private String nextPOKey;

    public String getPOKey() {
        return poKey;
    }

    public void setPOKey(String poKey) {
        this.poKey = poKey;
    }

    public long getUtcWindowStartTime() {
        return utcWindowStartTime;
    }

    public void setUtcWindowStartTime(long utcWindowStartTime) {
        this.utcWindowStartTime = utcWindowStartTime;
    }

    public int getWindowDurationMilliseconds() {
        return windowDurationMilliseconds;
    }

    public void setWindowDurationMilliseconds(int windowDurationMilliseconds) {
        this.windowDurationMilliseconds = windowDurationMilliseconds;
    }

    public int getPlacementsDurationMilliseconds() {
        return placementsDurationMilliseconds;
    }

    public void setPlacementsDurationMilliseconds(int placementsDurationMilliseconds) {
        this.placementsDurationMilliseconds = placementsDurationMilliseconds;
    }

    public String getNextPOKey() {
        return nextPOKey;
    }

    public void setNextPOKey(String nextPOKey) {
        this.nextPOKey = nextPOKey;
    }

	public String getOutSignalId() {
		return outSignalId;
	}

	public void setOutSignalId(String outSignalId) {
		this.outSignalId = outSignalId;
	}

	public String getInSignalId() {
		return inSignalId;
	}

	public void setInSignalId(String inSignalId) {
		this.inSignalId = inSignalId;
	}

	public int getBreakOrder() {
		return breakOrder;
	}

	public void setBreakOrder(int breakOrder) {
		this.breakOrder = breakOrder;
	}

	public HashMap<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(HashMap<String, String> metadata) {
		this.metadata = metadata;
	}

	public ArrayList<POInfo> getVariations() {
		return variations;
	}

	public void setVariations(ArrayList<POInfo> variations) {
		this.variations = variations;
	}

    public Long getCasId() {
        return casId;
    }

    public void setCasId(Long casId) {
        this.casId = casId;
    }

    public Long getUtcSignalTime() {
        return utcSignalTime;
    }

    public void setUtcSignalTime(Long utcSignalTime) {
        this.utcSignalTime = utcSignalTime;
    }

}
