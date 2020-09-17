package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;

public interface NotificationMessageBuilder {
	
    String getNotificationMessage(NotificationMessage notificationMessage);
    
    static String removeTerritoryUpdateCounterFromSignalIdIfPresent(String signalId) {
		String signalIdForFindingMediaLedger = "";
		Boolean isTerritoryUpdateCounterPresent = false;
		if (signalId.contains("_")) {
			String[] signalArray = signalId.split("_");
			String regex = "\\d+";
			String lastValue = signalArray[signalArray.length - 1];
			if (lastValue.matches(regex) && lastValue.length() == 3) {
				signalIdForFindingMediaLedger = signalId.substring(0, signalId.length() - 4);
				isTerritoryUpdateCounterPresent = true;
			}
		}
		if (!isTerritoryUpdateCounterPresent) {
			signalIdForFindingMediaLedger = signalId;
		}
		return signalIdForFindingMediaLedger;
	}
}
