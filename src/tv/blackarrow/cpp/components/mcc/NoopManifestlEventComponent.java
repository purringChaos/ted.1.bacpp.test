/**
 * 
 */
package tv.blackarrow.cpp.components.mcc;

import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;

/**
 * @author Amit Kumar Sharma
 *
 */
public class NoopManifestlEventComponent extends BaseManifestlEventComponent implements Callable {

	@Override
	public Object onCall(MuleEventContext muleEventContext) throws Exception {
		try {
			final Schema schema = muleEventContext.getMessage().getProperty(SCHEMA, PropertyScope.OUTBOUND);
			final ManifestConfirmConditionEventType event = muleEventContext.getMessage().getProperty(NOTIFICATION_EVENT, PropertyScope.OUTBOUND);
			final ManifestConfirmConditionNotificationType notification = new ManifestConfirmConditionNotificationType();
			final StatusCodeType statusCode = new StatusCodeType();
			notification.setStatusCode(statusCode);
			return sendErrorResponse(muleEventContext, schema, event, notification, statusCode, 
					"Unsupported signal for feeds configured to serve scheduleless Alternate Content.").toString();
		} finally {
			//Clear the cache maintained for this request if any.
			SCCMCCThreadLocalCache.clearMyCache();
			muleEventContext.getMessage().clearProperties(PropertyScope.OUTBOUND);
		}
	}

}
