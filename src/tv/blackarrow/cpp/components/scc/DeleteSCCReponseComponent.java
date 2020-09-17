/**
 * 
 */
package tv.blackarrow.cpp.components.scc;

import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;

/**
 * @author Amit Kumar Sharma
 *
 */
public class DeleteSCCReponseComponent implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(NoOpSCCReponseComponent.class);
	private static final boolean INFO_ENABLED = LOGGER.isInfoEnabled();
	
	@Override
	public Object onCall(MuleEventContext ctx) throws Exception {
		final SignalProcessingNotificationType responseNotification = (SignalProcessingNotificationType) ctx.getMessage().getOutboundProperty(SIGNAL_RESPONSE);
		String requestSchema = ctx.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);
		Schema schema = Schema.getSchema(requestSchema);
		String response = "";
		try {
			ESAMObjectCreationHelper.setResponseStatusCode(responseNotification, ctx);
			if (responseNotification != null && responseNotification.getResponseSignal() != null) {
				for(ResponseSignalType responseSignalType : responseNotification.getResponseSignal()) {
					responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
					boolean isBinaryRequest = responseSignalType.getBinaryData() != null;
					if (isBinaryRequest) {
						responseSignalType.setSCTE35PointDescriptor(null);
					}
				}
				response = objectToXML(responseNotification, schema);
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, responseNotification));
				return response;
			} else {
				String errorMessage = (String) ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
				SignalProcessingNotificationType note = new SignalProcessingNotificationType();
				note.setStatusCode(SCCResponseUtil.generateErrorStatusCode(errorMessage));
				response = objectToXML(note, schema);
				if (INFO_ENABLED) {
					LOGGER.debug(()->"Server internal error happened, no SCC response generated");
				}
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, responseNotification));
				return response;
			}
		}finally {
			SCCMCCThreadLocalCache.clearMyCache();
			if(ctx != null && ctx.getMessage() != null) {
				ctx.getMessage().clearProperties(PropertyScope.OUTBOUND);
			}
			if (INFO_ENABLED) {
				LOGGER.debug("SCC Response: \n" + response);
			}
		}
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema) {
		return schema.getResponseHandler().generateSCCResponse(signalNotification, null);
	}

}
