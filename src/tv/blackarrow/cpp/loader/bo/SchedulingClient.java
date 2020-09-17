/**
 * 
 */
package tv.blackarrow.cpp.loader.bo;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventCRUDNotificationHandlerInvokerType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventCRUDNotificationsHandler;
import tv.blackarrow.cpp.utils.EventAction;

/**
 * @author 
 *
 */
public class SchedulingClient {

	private static final Logger LOGGER = LogManager.getLogger(SchedulingClient.class.getName());
	private static final DataManager DATAMANAGER = DataManagerFactory.getInstance();
	private static final String EMPTY_STRING = "";

	public static void unScheduleAllNotificationsForRulesLoading(String feedExternalRef, List<AcquisitionPoint> acquistionPointList, List<BlackoutEvent> blackoutEvents) {
		infoLog("Trying to unSchedule notifications Across the cluster for blackouts: " + blackoutEvents);
		EventCRUDNotificationsHandler.handleNotification(acquistionPointList, EventAction.DELETE, blackoutEvents, false, EventCRUDNotificationHandlerInvokerType.COMPILED_RULE_LOADING);
	}

	public static void scheduleAllNotificationsForRulesLoading(String feedExternalRef, List<AcquisitionPoint> acquistionPointList, List<BlackoutEvent> blackoutEvents) {
		infoLog("Trying to schedule notifications Across the cluster for blackouts: " + blackoutEvents);
		EventCRUDNotificationsHandler.handleNotification(acquistionPointList, EventAction.UPDATE, blackoutEvents, false, EventCRUDNotificationHandlerInvokerType.COMPILED_RULE_LOADING);
	}

	private static final void infoLog(String messagePrefix, Object objectForMessageSuffix) {
		if (LOGGER.isInfoEnabled()) {
			String messageToBeLogged = EMPTY_STRING;
			if (messagePrefix != null) {
				messageToBeLogged = messagePrefix;
			}
			if (objectForMessageSuffix != null) {
				messageToBeLogged += objectForMessageSuffix.toString();
			}
			LOGGER.info(messageToBeLogged);
		}
	}

	private static final void infoLog(String messageToBeLogged) {
		infoLog(messageToBeLogged, null);
	}

}
