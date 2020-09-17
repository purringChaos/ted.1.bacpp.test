package tv.blackarrow.cpp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfirmedPlacementOpportunity {

    public static final long SIGNAL_TIME_OFFSET_DEFAULT_VALUE = 0; //Long.MAX_VALUE;
    private String acquisitionPointIdentity;
    private long utcSignalTime;
    private long signalTimeOffset = SIGNAL_TIME_OFFSET_DEFAULT_VALUE;
    private String signalId;
    private long abortTime;
    private boolean isAbortedViaESNIOrUI;
    

    //This will be used just by open ended blackouts case to know what was the time sent by the first confirmation request to support count down cue.
    private long firstTimeProgramConfirmationTime;
    
    //These two fields will be populated only for blackout events
	//Current Branch Pycelle.1===>
	//Information about below change: ActualSTARTNow and ActualEndNow Rules (From baseline ned.1)
	//Please note ActualSTARTTIME and ACTUAL ENDTIME Are saved without signal offset in AQ CPO(verified in ned.1, You may check)
	//In ned.1 in following classes
	//1. SCCResponseComponent.java (For Actual End Time)- Line number 828
	//2. BlackoutDecisionComponent.java/BlackoutDecisionComponentAiringIDComponent.java (For Actual Start Time) - Line number 483/351 respectively	
    private long actualUtcStartTime;
    private long actualUtcStopTime;


    // intentionally using a concrete type here so that deserialization works cleanly
    private HashMap<String, String> poKeyByZone;

    // intentionally using a concrete type here so that deserialization works cleanly
    private ArrayList<BreakInfo> breakInfos;
    
    private boolean considerActualUtcStopTimeAsProgramEnd;
    
    private String territoryUpdateSignalId;

    public Set<Integer> getPlacementsDurationsInMilliseconds() {
        Set<Integer> durations = new HashSet<Integer>();
        if (this.breakInfos == null || this.breakInfos.isEmpty())
            return durations;


        for (BreakInfo info : breakInfos) {
            durations.add(info.getDuration());
        }
        return durations;
    }
    
    public long getLongestDuration(){
    	 Integer longestDuration = 0;
         if (this.breakInfos == null || this.breakInfos.isEmpty())
             return longestDuration;

         for (BreakInfo info : breakInfos) {
        	 if (info!=null && info.getDuration() != null && info.getDuration().compareTo(longestDuration) > 0) {
                 longestDuration = info.getDuration();
             }
         }
        return longestDuration.longValue();
    }

    public String getAcquisitionPointIdentity() {
        return acquisitionPointIdentity;
    }

    public void setAcquisitionPointIdentity(String acquisitionPointIdentity) {
        this.acquisitionPointIdentity = acquisitionPointIdentity;
    }

    public long getUtcSignalTime() {
        return utcSignalTime;
    }

    public void setUtcSignalTime(long utcSignalTime) {
        this.utcSignalTime = utcSignalTime;
    }

    public String getSignalId() {
        return signalId;
    }

    public void setSignalId(String signalId) {
        this.signalId = signalId;
    }

    public Map<String, String> getPoKeyByZone() {
        return poKeyByZone;
    }

    public void setPoKeyByZone(HashMap<String, String> poKeyByZone) {
        this.poKeyByZone = poKeyByZone;
    }

    public List<BreakInfo> getBreakInfos() {
        return breakInfos;
    }

    public void setBreakInfos(ArrayList<BreakInfo> breakInfos) {
        this.breakInfos = breakInfos;
    }

    public long getSignalTimeOffset() {
        return signalTimeOffset;
    }

    public void setSignalTimeOffset(long signalTimeOffset) {
        this.signalTimeOffset = signalTimeOffset;
    }

	public boolean isAborted() {
		return abortTime > 0;
	}
	
	public boolean hasProgramEndReceived(){
		return actualUtcStopTime > 0;
	}
	
	public boolean isProgramEnded(){
		return (hasProgramEndReceived() && isConsiderActualUtcStopTimeAsProgramEnd() && getActualUtcStopTime() <= System.currentTimeMillis() ) ||
			   (isAborted() && getAbortTime() <= System.currentTimeMillis());
	}

	/**
	 * @return the abortTime
	 */
	public long getAbortTime() {
		return abortTime;
	}

	/**
	 * @param abortTime the abortTime to set
	 */
	public void setAbortTime(long abortTime) {
		this.abortTime = abortTime;
	}

	public long getActualUtcStartTime() {
		return actualUtcStartTime;
	}

	public void setActualUtcStartTime(long actualUtcStartTime) {
		this.actualUtcStartTime = actualUtcStartTime;
	}

	public long getActualUtcStopTime() {
		return actualUtcStopTime;
	}

	public void setActualUtcStopTime(long actualUtcStopTime) {
		this.actualUtcStopTime = actualUtcStopTime;
	}
	
	public long getFirstTimeProgramConfirmationTime() {
		return firstTimeProgramConfirmationTime;
	}

	public void setFirstTimeProgramConfirmationTime(
			long firstTimeProgramConfirmationTime) {
		this.firstTimeProgramConfirmationTime = firstTimeProgramConfirmationTime;
	}

	public long getRemainingDuration() {
		final long abortUTCTime = getAbortTime();
		final long poStopTime = getUtcSignalTime() +  getLongestDuration();
		long remainingDuration = poStopTime - abortUTCTime;
		if(remainingDuration < 0){
			remainingDuration = 0;
		}
		return remainingDuration;
	}

	public boolean isConsiderActualUtcStopTimeAsProgramEnd() {
		return considerActualUtcStopTimeAsProgramEnd;
	}

	public void setConsiderActualUtcStopTimeAsProgramEnd(boolean considerActualUtcStopTimeAsProgramEnd) {
		this.considerActualUtcStopTimeAsProgramEnd = considerActualUtcStopTimeAsProgramEnd;
	}

	public String getTerritoryUpdateSignalId() {
		return territoryUpdateSignalId;
	}

	public void setTerritoryUpdateSignalId(String territoryUpdateSignalId) {
		this.territoryUpdateSignalId = territoryUpdateSignalId;
	}

	/**
	 * @return the isAbortedViaESNIOrUI
	 */
	public boolean isAbortedViaESNIOrUI() {
		return isAbortedViaESNIOrUI;
	}

	/**
	 * @param isAbortedViaESNIOrUI the isAbortedViaESNIOrUI to set
	 */
	public void setAbortedViaESNIOrUI(boolean isAbortedViaESNIOrUI) {
		this.isAbortedViaESNIOrUI = isAbortedViaESNIOrUI;
	}

}
