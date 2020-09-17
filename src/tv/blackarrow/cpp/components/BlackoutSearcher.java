package tv.blackarrow.cpp.components;

import java.util.ArrayList;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.exeptions.CppException;
import tv.blackarrow.cpp.exeptions.ResourceNotFoundException;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class BlackoutSearcher extends EventProcessor {
    private static final Logger LOGGER = LogManager.getLogger(BlackoutSearcher.class);
    
    protected String acquisitionPointIdentity;
    protected long utcSignalTime = -1;
    private BlackoutEvent blackoutEvent = null;
    private ArrayList<BlackoutEvent> eventList;


    public BlackoutSearcher(){
    	
    }
	public BlackoutSearcher(AcquisitionPoint acquisitionPoint, long utcSignalTime, Short segmentTypeId) {
		this.acquisitionPoint = acquisitionPoint;
		this.utcSignalTime = utcSignalTime;
		if(acquisitionPoint !=null ){
			this.acquisitionPointIdentity = acquisitionPoint.getAcquisitionPointIdentity();
		}
		findMatchingBlackoutEventBySignalTime(segmentTypeId);
    }
	
	public BlackoutSearcher(AcquisitionPoint acquisitionPoint, long utcSignalTime,Short segmentTypeId,String EventID) {
		this.acquisitionPoint = acquisitionPoint;
		this.utcSignalTime = utcSignalTime;
		if(acquisitionPoint !=null ){
			this.acquisitionPointIdentity = acquisitionPoint.getAcquisitionPointIdentity();
		}
		findMatchingBlackoutEventByEventID(segmentTypeId,EventID);
    }
	
    private BlackoutEvent findMatchingBlackoutEventBySignalTime(Short segmentTypeId) {
        DataManager dataManager = DataManagerFactory.getInstance();
        if(acquisitionPoint == null){
        	acquisitionPoint = dataManager.getAcquisitionPoint(acquisitionPointIdentity);
        }
        
        // Get all the events created on the feed to which this blackout belongs.  
        eventList = dataManager.getAllBlackoutEventsOnFeed(acquisitionPoint.getFeedExternalRef());
        int buffer = acquisitionPoint.getProgramStartBuffer();
        
        // Get the cursor for this AP.
        SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
        
        //Get the last blackout confirmation on this acquisition point.
		ConfirmedPlacementOpportunity lastConfirmedBlackoutForThisAP = dataManager.getConfirmedBlackoutForGivenAP(acquisitionPointIdentity, cursor.getLastConfirmedBlackoutSignalId());
		
    	for(BlackoutEvent event : eventList) {
    		
			//If a program has already ended return null to return NOOP
			if( SegmentType.isProgramEndSignal(segmentTypeId) && 
				lastConfirmedBlackoutForThisAP != null && (lastConfirmedBlackoutForThisAP.isProgramEnded() || lastConfirmedBlackoutForThisAP.isAborted()) && 
				utcSignalTime > lastConfirmedBlackoutForThisAP.getActualUtcStopTime() + buffer){
				blackoutEvent = null;
				break;
			}
    		
    		// For feeds allowing open ended blackouts.
    		if(acquisitionPoint.isFeedAllowsOpenEndedBlackouts()){
    			
    			//Return the currently running program if it has not ended yet for all the signals.
    			if( lastConfirmedBlackoutForThisAP != null && !lastConfirmedBlackoutForThisAP.isProgramEnded() && utcSignalTime >= lastConfirmedBlackoutForThisAP.getActualUtcStartTime() - buffer &&
    					//To handle combined signal in future.where next program start is at the same time as previous program end.
    					(!lastConfirmedBlackoutForThisAP.hasProgramEndReceived() || 
    							((!SegmentType.isProgramStartSignal(segmentTypeId) && utcSignalTime <= lastConfirmedBlackoutForThisAP.getActualUtcStopTime()) ||
    							 (SegmentType.isProgramStartSignal(segmentTypeId) && utcSignalTime < lastConfirmedBlackoutForThisAP.getActualUtcStopTime()))
    					)		
    			){
    			
    			//Exception is Program start in the following conditions:
    				if(SegmentType.isProgramStartSignal(segmentTypeId) && 
    						//Give NOOP for all requests others than count down cue/ multi cue requests.
    						!(
    						//The UTC time received should not be out of the actual UTC start time +/- buffer			
    						(lastConfirmedBlackoutForThisAP.getActualUtcStartTime()- buffer <= utcSignalTime && utcSignalTime <= lastConfirmedBlackoutForThisAP.getActualUtcStartTime() + buffer)	||
    						//The UTC time received should not be out of the first program start UTC start time +/- buffer, to handle requests coming from multiple APs with a time
    						//that is slightly in past and due to that we had to take the current system time at the time of first program start request. 
    						(lastConfirmedBlackoutForThisAP.getFirstTimeProgramConfirmationTime() > 0 && lastConfirmedBlackoutForThisAP.getFirstTimeProgramConfirmationTime()- buffer <= utcSignalTime 
    						&& utcSignalTime  <= lastConfirmedBlackoutForThisAP.getFirstTimeProgramConfirmationTime() + buffer)
    						)
    				){
    					blackoutEvent = null;
    					break;
        			}
    				blackoutEvent = dataManager.getSingleBlackoutEvent(lastConfirmedBlackoutForThisAP.getSignalId());
    				break;
    			}
    			
    			//Otherwise if there is no previously running program, return the blackout event whose scheduled running interval has not finished and UTC time received falls in that.
	    		if(event.getUtcStartTime() - buffer <= utcSignalTime &&  utcSignalTime  < event.getUtcStopTime() && 
	    		  //Extra check that either there has not been any blackout confirmed before OR if there is the last confirmed one must have ended by now.
	    		  (lastConfirmedBlackoutForThisAP == null || lastConfirmedBlackoutForThisAP.getActualUtcStopTime() <= utcSignalTime)) {
	    			//Try to fetch the CPO for the current blackout.
	    			ConfirmedPlacementOpportunity cpoForThisBlackout = dataManager.getConfirmedBlackoutForGivenAP(acquisitionPointIdentity, event.getSignalId());
	    			//Pick this BO if either there is no CPO for current BO and scheduled end time has not passed.
	    			if((cpoForThisBlackout==null && event.getUtcStopTime() > System.currentTimeMillis()) ||
	    					// Or there is a CPO for this BO(it might have been confirmed by other AP) and it is not yet ended.
	    					(cpoForThisBlackout!=null && !cpoForThisBlackout.isProgramEnded() && !cpoForThisBlackout.isAborted())){
	    				//Just in case was null or 
	    				if(cpoForThisBlackout==null || 
	    						//CPO is not null and the following condition met then only return the current BO.
	    						// 1. The UTC time received is within +/- buffer of actual UTC start time. 
	    						(cpoForThisBlackout.getActualUtcStartTime()- buffer <= utcSignalTime &&  utcSignalTime  <= cpoForThisBlackout.getActualUtcStartTime() + buffer) ||
	    						// 2. Or The UTC time received is within +/- buffer of actual first program confirmation time, to handle requests coming from multiple APs with a time
	    						//that is slightly in past and due to that we had to take the current system time at the time of first program start request. 
	    						(SegmentType.isProgramStartSignal(segmentTypeId) && cpoForThisBlackout.getFirstTimeProgramConfirmationTime() > 0 && //To support count down cue 
	    								cpoForThisBlackout.getFirstTimeProgramConfirmationTime()- buffer <= utcSignalTime &&  utcSignalTime  <= cpoForThisBlackout.getFirstTimeProgramConfirmationTime() + buffer)){
	    					blackoutEvent = event;
	    					break;
	    				}
	    			}
	    		}
    		} else {//Non Open ended BOs
	    		if(event.getUtcStartTime() - buffer <= utcSignalTime &&  utcSignalTime  <= event.getUtcStartTime() + buffer ) {
	    			blackoutEvent = event;
	    			break;
	    		}
    		}
    	}
    	
    	return blackoutEvent;
    }
    
	private BlackoutEvent findMatchingBlackoutEventByEventID(Short segmentTypeId, String eventID) {
		
		if (eventID != null) {
			DataManager dataManager = DataManagerFactory.getInstance();
			if (acquisitionPoint == null) {
				acquisitionPoint = dataManager.getAcquisitionPoint(acquisitionPointIdentity);
			}
			// Get all the events created on the feed to which this blackout
			// belongs.
			eventList = dataManager.getAllBlackoutEventsOnFeed(acquisitionPoint.getFeedExternalRef());
			int buffer = acquisitionPoint.getProgramStartBuffer();
			
		    SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPointIdentity);
		       
		      //Get the last blackout confirmation on this acquisition point.
			ConfirmedPlacementOpportunity lastConfirmedBlackoutForThisAP = dataManager.getConfirmedBlackoutForGivenAP(acquisitionPointIdentity, cursor.getLastConfirmedBlackoutSignalId());
				
			for (BlackoutEvent event : eventList) {
				if (!acquisitionPoint.isFeedAllowsOpenEndedBlackouts()) {
					break;
				}
				if (SegmentType.isProgramStartSignal(segmentTypeId)) {
					
					// Check if the Last Confirmed Blackout Has Received the Program End Signal.
					if( lastConfirmedBlackoutForThisAP != null && !lastConfirmedBlackoutForThisAP.hasProgramEndReceived() && utcSignalTime >= lastConfirmedBlackoutForThisAP.getActualUtcStartTime() - buffer) {
						blackoutEvent = dataManager.getSingleBlackoutEvent(lastConfirmedBlackoutForThisAP.getSignalId());
						if(blackoutEvent.getEventId().equals(eventID)) {
							break;
						} else{
							LOGGER.debug(()->"The last Confirmed Blackout Has net Been Ended Yet");
							blackoutEvent = null;
							break;
						}
					}
					// Check If the Last Confirmed Blackout is still inFlight.
					if( lastConfirmedBlackoutForThisAP != null && lastConfirmedBlackoutForThisAP.hasProgramEndReceived() && utcSignalTime < lastConfirmedBlackoutForThisAP.getActualUtcStopTime()) {
						blackoutEvent = dataManager.getSingleBlackoutEvent(lastConfirmedBlackoutForThisAP.getSignalId());
						if(blackoutEvent.getEventId().equals(eventID)) {
							break;
						} else{
							LOGGER.debug(()->"The last Confirmed Blackout Has net Been Ended Yet");
							blackoutEvent = null;
							break;
						}
					}
					
					if (event.getEventId().equals(eventID)) {
						//Check if Its an Existing Event
						if(lastConfirmedBlackoutForThisAP !=null && lastConfirmedBlackoutForThisAP.getSignalId().equals(event.getSignalId())) {
							LOGGER.debug(()->"This Blackout Has Already been completed");
							blackoutEvent = null;
							break;
						} else if(isBlackoutEventExpired(event)){
							LOGGER.debug(()->"This Blackout Has been expired");
							blackoutEvent = null;
							break;
						} else{
							blackoutEvent = event;
							break;
						}
					}
				}
			}
		}

		return blackoutEvent;
	}

    public ConfirmedPlacementOpportunity getBlackout(PoisAuditLogVO poisAuditLogVO, long firstTimeProgramConfirmationTime) throws CppException, ResourceNotFoundException {
        if (acquisitionPointIdentity == null || acquisitionPointIdentity.trim().equals("") || utcSignalTime == -1) {
            throw new ResourceNotFoundException(3, "Passed acquisition point identity or utc signal time is null");
        }
        DataManager dataManager = DataManagerFactory.getInstance();
        AcquisitionPoint aqpt = dataManager.getAcquisitionPoint(acquisitionPointIdentity);
        if (aqpt == null) {
            throw new ResourceNotFoundException(2, "Invalid acquisition point identity");
        }
        return getNextConfirmedBlackout(poisAuditLogVO, firstTimeProgramConfirmationTime);
    }

    private boolean isBlackoutEventExpired(BlackoutEvent event) {
    	boolean isBlackoutEventExpired = false;
    	Calendar eventExpirationTime = Calendar.getInstance();
    	eventExpirationTime.add(Calendar.HOUR, -24);
    	if(event.getUtcStopTime() < eventExpirationTime.getTimeInMillis()) {
			LOGGER.debug("Blackout : " +blackoutEvent.getEventId() +" End time is more than 24 Hours in past, so ignoring this Blackout event");
			isBlackoutEventExpired = true;
		}
		return isBlackoutEventExpired;
    }
    
    private ConfirmedPlacementOpportunity getNextConfirmedBlackout(PoisAuditLogVO poisAuditLogVO, long firstTimeProgramConfirmationTime) throws CppException {
        if(blackoutEvent == null) {
        	AuditLogger.auditLogNoBlackoutEvent(acquisitionPointIdentity, utcSignalTime,poisAuditLogVO);
        	return null;
        }
        
        ConfirmedPlacementOpportunity cpo = DataManagerFactory.getInstance().getConfirmedBlackoutForGivenAP(acquisitionPointIdentity, blackoutEvent.getSignalId());
        
        if(cpo != null){
        	LOGGER.debug("Found previously confirmed CPO for Blackout : " + blackoutEvent.getEventId() + " with signal id: " + blackoutEvent.getSignalId());
        	cpo.setAcquisitionPointIdentity(acquisitionPointIdentity);
        	return cpo;
        }
        //Amit: In the entire lifetime of blackout the code should reach at this point only once.
        cpo	= new ConfirmedPlacementOpportunity();
		ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
		BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) (blackoutEvent.getUtcStopTime() - blackoutEvent.getUtcStartTime()));
		breaks.add(breakInfo);
		cpo.setBreakInfos(breaks);
        
        cpo.setAcquisitionPointIdentity(acquisitionPointIdentity);
        cpo.setUtcSignalTime(utcSignalTime);
        if(firstTimeProgramConfirmationTime > 0 && cpo.getFirstTimeProgramConfirmationTime() <= 0){
        	cpo.setActualUtcStartTime(firstTimeProgramConfirmationTime);
        	cpo.setFirstTimeProgramConfirmationTime(firstTimeProgramConfirmationTime);
        }
        cpo.setSignalId(blackoutEvent.getSignalId());
        
        //this means we are successful in processing the break
        LOGGER.debug("Input(Acquition Point=" + acquisitionPointIdentity + ", utcSignalTime=" + utcSignalTime
                + ") and returned Output(" + "SignalId=" + cpo.getSignalId() + ",Durations="
                + cpo.getPlacementsDurationsInMilliseconds() + ")");
        return cpo;
    }

    public BlackoutEvent getBlackoutEvent() {
    	return blackoutEvent;
    }
    

	public ArrayList<BlackoutEvent> getEventList() {
		return eventList;
	}

	public void setEventList(ArrayList<BlackoutEvent> eventList) {
		this.eventList = eventList;
	}
}
