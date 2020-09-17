/**
 * 
 */
package tv.blackarrow.cpp.components;

import static tv.blackarrow.cpp.utils.EventAction.DELETE;
import static tv.blackarrow.cpp.utils.EventAction.STOP_NOW;
import static tv.blackarrow.cpp.utils.EventAction.TERRITORY_UPDATE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.components.util.ESSClusterLockUtil;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.RealtimeBlackoutEventMessage;
import tv.blackarrow.cpp.model.Restriction;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventCRUDNotificationHandlerInvokerType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventCRUDNotificationsHandler;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.TerritoryUpdateNotificationTask;

/**
 * @author amit
 * 
 * This components handles real time updates to the blackout event data in POIS due to any actions performed by user on blackout events.
 *
 */
public class RealtimeBlackoutEventBase {
	
	private static final Logger LOGGER = LogManager.getLogger(RealtimeBlackoutEventBase.class);
	protected static final DataManager dataManager = DataManagerFactory.getInstance();
	private static final boolean IS_INFO_ENABLED = LOGGER.isInfoEnabled();
	private static final boolean IS_DEBUG_ENABLED = LOGGER.isDebugEnabled();
	protected static final String EMPTY_STRING = "";
	
	public static final int WAIT_TIME_FOR_LOCK_RETRY = 1;
	public static final int HTTP_CONNECTION_WAIT_TIME_TEN_SECOND = 10 * 1000;
	public static final String SEPARATOR_COLON = ":";
	public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	
	/**
	 * @param blackoutEvent
	 * @param timeBufferToDetermineIfNotificationNeedsToHandleImmediate
	 * @param eventAction
	 * @return
	 */
	protected void handleRealtimeEventCUDOperations(BlackoutEvent blackoutEvent, Integer timeBufferToDetermineIfNotificationNeedsToHandleImmediate) {
		
		//Fetch all the acquisition points associated to this event.
		Set<String> acquisitionPointIdsForThisBlackout = dataManager.getFeedToAcquistionMap(blackoutEvent.getFeedExtRef());
		if(acquisitionPointIdsForThisBlackout== null || acquisitionPointIdsForThisBlackout.isEmpty()){
			if (IS_INFO_ENABLED) {
				LOGGER.info(()->"No acquisition point found for feed id \"" + blackoutEvent.getFeedExtRef() + "\". So ignoring this message with no update to runtime.");
			}
			return;
		}
		
		/** 
		 * 1. Take required locks.  
		 */
		StringDocument lockedDocument = null;
		try {
			// Synchronize the actions between the loader and real-time updates to POIS across cluster.
			lockedDocument = ESSClusterLockUtil.acquireClusterWideLockWithFeedExternalRef(blackoutEvent.getFeedExtRef());
			
   	    	boolean isInFlight = false;
   	    	BlackoutEvent existingBlackoutEvent = dataManager.getSingleBlackoutEvent(blackoutEvent.getSignalId());
			//To override Program start UTC Time for update and end now case.
			final ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());

			final long currentSystemTimeInUTC = System.currentTimeMillis();
   	    	if(existingBlackoutEvent!=null){
   	    		isInFlight = (BlackoutEvent.getActualBlackoutStartTime(cpo, existingBlackoutEvent) < currentSystemTimeInUTC) && 
   	    				(BlackoutEvent.getActualBlackoutStopTime(cpo, existingBlackoutEvent) > currentSystemTimeInUTC);
   	    		if(isInFlight && blackoutEvent.getEventAction() == DELETE){
	   	    		blackoutEvent.setEventAction(STOP_NOW);
	   	    	}
   	    	}
   	    	
   	    	//1. If this user action has already been taken care of then return from here.
   	    	if((blackoutEvent.getEventAction() == DELETE && existingBlackoutEvent == null) || (existingBlackoutEvent!= null &&
   	    			existingBlackoutEvent.getLastUpdateTime() >= blackoutEvent.getLastUpdateTime())){
   	    		if (IS_INFO_ENABLED) {
   	    			LOGGER.info(()->"This request has already been served by another POIS so skipping ahead. Event ID: " + blackoutEvent.getEventId());
   	    		}
   	    		return;
   	    	}
   	    	
   	    	final boolean isEndTimeUpdated = updateThisEventInDataStore(blackoutEvent);
   	    	
   	    	//3. Notify transcoders of the change.
   	    	List<AcquisitionPoint> acquisitionPoints = dataManager.getAcquisitionPoints(acquisitionPointIdsForThisBlackout);
			if (blackoutEvent.getEventAction() == TERRITORY_UPDATE) {
				CppConfigurationBean cppConfigBean = CppConfigurationBean.getInstance();
				boolean sendTerritorryUpdateConfirmation = cppConfigBean.isSendTerritoryUpdateConfirmation();
				ExecutorService executorService = null;
				BlackoutSearcher blackoutSearcher = null;
				if(sendTerritorryUpdateConfirmation){
					int numThreads = cppConfigBean.getTerritoryUpdateNotificationThreadPoolSize();
					executorService = Executors.newFixedThreadPool(numThreads);
					blackoutSearcher = new BlackoutSearcher();
				}
				for (AcquisitionPoint acquisitionPoint : acquisitionPoints) {
					if (acquisitionPoint == null) {
						return;
					}
					boolean isContentIdentificationReceived = false;
					if (acquisitionPoint.isIpAcquisitionPoint()) {
						SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(acquisitionPoint.getAcquisitionPointIdentity());
						ConfirmedPlacementOpportunity acquisitionPointCPO = dataManager.getConfirmedBlackoutForGivenAP(acquisitionPoint.getAcquisitionPointIdentity(),blackoutEvent.getSignalId());
						acquisitionPointCPO = acquisitionPointCPO == null ? cpo: acquisitionPointCPO;
						if(acquisitionPointCPO == null && cursor.getLastConfirmedBlackoutSignalId() == null){
							LOGGER.warn(()->"The Blackout is not yet Confirmed for the Acquisation point"+acquisitionPoint);
							continue;
						}
						if(acquisitionPointCPO.getTerritoryUpdateSignalId() != null && !(acquisitionPointCPO.getTerritoryUpdateSignalId().equals(acquisitionPointCPO.getSignalId())) 
								&& cursor != null && acquisitionPointCPO.getTerritoryUpdateSignalId() !=null && cursor.getLastConfirmedBlackoutSignalId().equals(acquisitionPointCPO.getTerritoryUpdateSignalId())) {
							isContentIdentificationReceived = true;
						}
						acquisitionPointCPO.setTerritoryUpdateSignalId(blackoutEvent.getTerritorySignalId());
						dataManager.putConfirmedBlackout(acquisitionPointCPO);//Update The Existing CPO.
						if (sendTerritorryUpdateConfirmation) { // send the Content Identification Updation on Each Acquisation Point
							dataManager.putConfirmedBlackout(acquisitionPointCPO,blackoutEvent.getTerritorySignalId());// Put the New CPO corresponding to the New Signal ID.
							TerritoryUpdateNotificationTask task = new TerritoryUpdateNotificationTask(acquisitionPoint, acquisitionPointCPO,currentSystemTimeInUTC, blackoutEvent,dataManager);
							executorService.submit(task);
							cursor.setLastConfirmedBlackoutSignalId(blackoutEvent.getTerritorySignalId());
							blackoutSearcher.updateSignalProcessorCursor(cursor);
							continue;
						}
					}
					if(!sendTerritorryUpdateConfirmation && isEndTimeUpdated){
						handleTranscoderNotifications(blackoutEvent, timeBufferToDetermineIfNotificationNeedsToHandleImmediate,acquisitionPoints, isInFlight, cpo, currentSystemTimeInUTC, isEndTimeUpdated,isContentIdentificationReceived);
					}
				}
				

			} else{
				handleTranscoderNotifications(blackoutEvent, timeBufferToDetermineIfNotificationNeedsToHandleImmediate,acquisitionPoints, isInFlight, cpo, currentSystemTimeInUTC, isEndTimeUpdated,false);
			}
	   	    
		} catch(Exception ex){
			LOGGER.error(()->"POIS real-time update failed to complete the process for event \"" + blackoutEvent.getEventId() + "\" configured for feed \"" +
					blackoutEvent.getFeedExtRef() +"\". Following unexpected error occured: " + ex.getMessage(), ex);
	   	} finally {
	   		ESSClusterLockUtil.releaseClusterWideLock(lockedDocument);
	   	}
	}
	
	
	/**
	 * Updates the given event in the Couchbase data store.
	 * 
	 * @param blackoutEventAction
	 * @return 
	 */
	protected boolean updateThisEventInDataStore(final BlackoutEvent blackoutEvent) {
		final String LOG_IDENTIFIER = "Thread: " + Thread.currentThread().getId() + ", Event: " + blackoutEvent.getEventId()+"(" + blackoutEvent.getSignalId() + ") ::-->> ";
				if(IS_DEBUG_ENABLED){
			LOGGER.debug(()->"Updating Event in DataStore");
		}
		BlackoutEvent existingEvent = null;
    	List<BlackoutEvent> blackoutEvents = dataManager.getAllBlackoutEventsOnFeed(blackoutEvent.getFeedExtRef());
    	if(blackoutEvents==null){
    		blackoutEvents = new ArrayList<>();
    	}
    	Iterator<BlackoutEvent> iterator = blackoutEvents.iterator();
    	while(iterator.hasNext()){
    		BlackoutEvent event = iterator.next();
    		if(event.getSignalId().equals(blackoutEvent.getSignalId())){
    			existingEvent = event;
    			iterator.remove();
				break;
    		}
    	}
    	if(existingEvent!=null && blackoutEvent.getEventAction() == EventAction.STOP_NOW ){
    		blackoutEvent.setRestrictions(existingEvent.getRestrictions());
    	}
    	if(blackoutEvent.getEventAction() == DELETE){
			//a) Delete this Blackout Event with the blackout event id and signal id as keys. 
    		if(IS_INFO_ENABLED) {
    			LOGGER.info(()->LOG_IDENTIFIER + "Deleting individual event from DataStore.");
			}
    		final long lastBlackoutRulesCompilationTime = dataManager.getBlackoutEventCompilationTime(blackoutEvent.getFeedExtRef());
			dataManager.deleteBlackoutEvent(blackoutEvent);
			dataManager.recordEventDeletionViaRealtimeUpdates(lastBlackoutRulesCompilationTime, blackoutEvent.getFeedExtRef(), 
					blackoutEvent.getSignalId());
    	} else {
    		if(IS_DEBUG_ENABLED) {
    			LOGGER.debug(()->LOG_IDENTIFIER + "Updating individual event into DataStore.");
			}
		
			//a) Put blackout event with the blackout event id and signal id as keys. 
	    	dataManager.putBlackoutEvent(blackoutEvent.getEventId(), blackoutEvent.getSignalId(), blackoutEvent);
	    	blackoutEvents.add(blackoutEvent);
	    	}
    	if(IS_DEBUG_ENABLED) {
			LOGGER.debug(()->LOG_IDENTIFIER + "Saving updated feed level event list back in the datastore.");
		}
    	dataManager.putBlackoutEvents(blackoutEvent.getFeedExtRef(), GSON.toJson(blackoutEvents, new TypeToken<ArrayList<BlackoutEvent>>(){}.getType()));
		boolean isEndTimeUpdated = existingEvent!= null && blackoutEvent != null && existingEvent.getUtcStopTime() != blackoutEvent.getUtcStopTime();
    	return isEndTimeUpdated;
	}
	

	/**
	 * Handles the scheduled or immediate POIS notifications.
	 * 
	 * @param blackoutEventAction
	 * @param isInFlight 
	 * @param cpo 
	 * @param isEndTimeUpdated 
	 * @throws DatatypeConfigurationException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void handleTranscoderNotifications(final BlackoutEvent newBlackoutEvent, final Integer timeBufferToDetermineIfNotificationNeedsToHandleImmediate, final List<AcquisitionPoint> acquisitionPoints, final boolean isInFlight, 
			ConfirmedPlacementOpportunity cpo, final long currentSystemTime, final boolean isEndTimeUpdated, final boolean isContentIdentificationReceived)
			throws DatatypeConfigurationException, InterruptedException, ExecutionException {
		
		final List<BlackoutEvent> realtimeBlackoutsReceived = new ArrayList<>();
		realtimeBlackoutsReceived.add(newBlackoutEvent);
		//START_NOW Determination
		long currentSystemTimeInUTC = System.currentTimeMillis();
		long aqMaxWindowForAheadTime = Math.max(acquisitionPoints.stream().mapToInt(AcquisitionPoint::getProgramStartBuffer).max().orElse(0),
				NotificationServiceConfig.ESS_PROCESSING_TIME_IN_SECS + 1);//+1 to avoid being on last millis and that resulting in scheduling Upstream notification on past second.It's just hypothetical fail safe.
		//For. E.g. at 999th millis it said yes it's not START_NOW, and went to regular path of scheduling that will schedule for that 1st second and Watcher thread has already crossed that. (Hypothetical:-))
		//Determining Start_Now
		boolean isStartNow = !isInFlight && newBlackoutEvent != null && (newBlackoutEvent.getUtcStartTime() < currentSystemTimeInUTC + aqMaxWindowForAheadTime
				+ TimeUnit.SECONDS.toMillis(timeBufferToDetermineIfNotificationNeedsToHandleImmediate));
		boolean isStopNow = isInFlight ? true : false;
			
			switch (newBlackoutEvent.getEventAction()) {
		case START_NOW:
				case CREATE: //This is a brand new blackout event.
			EventCRUDNotificationsHandler.handleNotification(acquisitionPoints, EventAction.CREATE, realtimeBlackoutsReceived, isStartNow, EventCRUDNotificationHandlerInvokerType.REAL_TIME_MESSAGE);
					break;

				case UPDATE://This is an update to an existing event. This case handles Shortening, Lengthening and Start Now case.
				case TERRITORY_UPDATE:
			EventCRUDNotificationsHandler.handleNotification(acquisitionPoints, EventAction.UPDATE, realtimeBlackoutsReceived, isStartNow, EventCRUDNotificationHandlerInvokerType.REAL_TIME_MESSAGE);
			break;

				case STOP_NOW://This handles the case of ending an in-flight event with immediate effect.
		case DELETE:
					if(isInFlight){ //Although this request should always come for in-flight events this check is just to safeguard.
				EventCRUDNotificationsHandler.handleNotification(acquisitionPoints, EventAction.STOP_NOW, realtimeBlackoutsReceived, isStopNow, EventCRUDNotificationHandlerInvokerType.REAL_TIME_MESSAGE);
						} else {
				if (IS_INFO_ENABLED) {
   	    			LOGGER.info(()->"1. Event id: " + newBlackoutEvent.getEventId() + " ,STOP_NOW: This Blackout Event is not in flight so not sending any notifications for AP \"" + "\".");
							}
				EventCRUDNotificationsHandler.handleNotification(acquisitionPoints, EventAction.DELETE, realtimeBlackoutsReceived, isStopNow, EventCRUDNotificationHandlerInvokerType.REAL_TIME_MESSAGE);
					}
					break;
				default:
					String responseMessage = "Message received is in invalid format, invalid action found. Exiting with no updates.";
			LOGGER.error(()->responseMessage);
					throw new RuntimeException(responseMessage);
			}
	
		}
		
		
			
	/**
	 * Validates the JSON message sent by the client to verify that all the mandatory fields exists.
	 * 
	 * @param blackoutEventAction
	 */
	protected String validateMessage(RealtimeBlackoutEventMessage blackoutEventAction) {
		String validationResponse = EMPTY_STRING;
		List<String> invalidFields = new ArrayList<String>();
		
		if(blackoutEventAction.getTimeBuffer() == null){
			blackoutEventAction.setTimeBuffer(0);
		}
		
		List<BlackoutEvent> blackoutEvents = blackoutEventAction.getLinearAltEvents();
		
		if(blackoutEvents == null){
			invalidFields.add("linearAltEvents");
		} else {
			for(BlackoutEvent blackoutEvent : blackoutEvents){
				List<String> invalidFieldsForThisEvent =  new ArrayList<String>();
				if(blackoutEvent.getEventAction() == null){
					invalidFields.add("eventAction");
				}
				if(isStringNullOrEmpty(blackoutEvent.getEventId())){
					invalidFieldsForThisEvent.add("eventId");
				}
				if(isStringNullOrEmpty(blackoutEvent.getSignalId())){
					invalidFieldsForThisEvent.add("signalId");
				}
				if(isStringNullOrEmpty(blackoutEvent.getFeedExtRef())){
					invalidFieldsForThisEvent.add("feedExtRef");
				}
				if(blackoutEvent.getUtcStartTime() <= 0){
					invalidFieldsForThisEvent.add("utcStartTime");
				}
				if(blackoutEvent.getUtcStartTime() <= 0){
					invalidFieldsForThisEvent.add("utcStopTime");
				}
				if(blackoutEvent.getLastUpdateTime() <= 0){
					invalidFieldsForThisEvent.add("lastUpdateTime");
				}
				if(!invalidFieldsForThisEvent.isEmpty()){
					invalidFields.add("Event \"" + blackoutEvent.getEventId() + "\" : " + invalidFieldsForThisEvent.toString() + ". ");
				}
			}
		}
		
		if(!invalidFields.isEmpty()){
			validationResponse = "Message received is in invalid format, following fields have invalid or no values specified: " + invalidFields.toString();
		}
		
		return validationResponse;
	}
	
	private boolean isStringNullOrEmpty(String stringToValidate){
		return stringToValidate == null || stringToValidate.trim().isEmpty();
	}
	
}
