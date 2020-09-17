/**
 * 
 */
package tv.blackarrow.cpp.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import com.google.gson.JsonSyntaxException;

import tv.blackarrow.cpp.components.filter.RestrictionFilter;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.RealtimeBlackoutEventMessage;


/**
 * @author amit
 * 
 * This components handles real time updates to the blackout event data in POIS due to any actions performed by user on blackout events.
 *
 */
public class RealtimeBlackoutEventCRUDComponent extends RealtimeBlackoutEventBase implements Callable {
	
	private static final Logger LOGGER = LogManager.getLogger(RealtimeBlackoutEventCRUDComponent.class);
	private static final boolean IS_INFO_ENABLED = LOGGER.isInfoEnabled();
	@Override
	public Object onCall(final MuleEventContext muleEventContext) throws Exception {
		
		long eventRuntimeUpdateStartTime = System.currentTimeMillis();
		
		String responseMessage = "Success";
		String blackoutEventJSON =  muleEventContext.getMessageAsString();
		if(IS_INFO_ENABLED) {
			LOGGER.info("Thread: " + Thread.currentThread().getId() + ", Following message received for Blackout Event via near realtime compilation:" + blackoutEventJSON);
		}
		
		RealtimeBlackoutEventMessage realtimeBlackoutEventMessage = null;
		try {
			realtimeBlackoutEventMessage = GSON.fromJson(blackoutEventJSON, RealtimeBlackoutEventMessage.class);
		} catch(JsonSyntaxException jsonSyntaxException){
			responseMessage = "Message received is in invalid format.";
			LOGGER.error( responseMessage + "\n" + blackoutEventJSON);
			return responseMessage;
		}
		
		String validationResponse = validateMessage(realtimeBlackoutEventMessage);
		if(!EMPTY_STRING.equals(validationResponse)){
			LOGGER.error(validationResponse);
			return validationResponse;
		}
		RestrictionFilter.cleanupRestrictions(realtimeBlackoutEventMessage.getLinearAltEvents());
		for(BlackoutEvent blackoutEvent: realtimeBlackoutEventMessage.getLinearAltEvents()){
			LOGGER.info("Message received for Blackout Event via near realtime compilation:" + blackoutEvent.getEventId() + "(" + blackoutEvent.getSignalId() + ") for Action "
					+ blackoutEvent.getEventAction().toString());
			handleRealtimeEventCUDOperations(blackoutEvent, realtimeBlackoutEventMessage.getTimeBuffer());
		}
		if(IS_INFO_ENABLED) {
			LOGGER.info("POIS took " + (System.currentTimeMillis() - eventRuntimeUpdateStartTime) + " ms to perform the realtime I02 event CRUD action.");
		}
		
		return responseMessage;
	}
	
}
