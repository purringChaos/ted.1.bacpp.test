/**
 * 
 */
package tv.blackarrow.cpp.service;

import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;

/**
 * @author Amit Kumar Sharma
 *
 */
public class NotificationResult {

	final private boolean isNotificationSent;
	final private ConfirmedPlacementOpportunity acquisitionPointConfirmedBlackout;
	
	public NotificationResult(boolean isNotificationSent, ConfirmedPlacementOpportunity acquisitionPointConfirmedBlackout) {
		super();
		this.isNotificationSent = isNotificationSent;
		this.acquisitionPointConfirmedBlackout = acquisitionPointConfirmedBlackout;
	}

	public boolean isNotificationSent() {
		return isNotificationSent;
	}

	public ConfirmedPlacementOpportunity getAcquisitionPointConfirmedBlackout() {
		return acquisitionPointConfirmedBlackout;
	}
	
}
