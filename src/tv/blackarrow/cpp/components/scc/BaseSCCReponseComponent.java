package tv.blackarrow.cpp.components.scc;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.scc.schedulelessaltevent.SCCResponseComponent;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.SCCResponseUtil;

public class BaseSCCReponseComponent implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(SCCResponseComponent.class);
	private static final boolean INFO_ENABLED = LOGGER.isInfoEnabled();

	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext ctx) throws Exception {
		final SignalProcessingNotificationType responseNotification = (SignalProcessingNotificationType) ctx.getMessage().getOutboundProperty(SIGNAL_RESPONSE);
		String requestSchema = ctx.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);
		Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = (Map<String, I03ResponseModelDelta>) ctx.getMessage()
				.getOutboundProperty(ContextConstants.I03_MODEL_DELTA);
		AcquisitionPoint aqpt = ctx.getMessage().getProperty(ACQUISITION_POINT, PropertyScope.OUTBOUND);
		String confirmedEventSignalId = ctx.getMessage().getOutboundProperty(ContextConstants.CONFIRMED_EVENT_SIGNAL_ID, null);
		Schema schema = Schema.getSchema(requestSchema);
		String response = "";
		try {
			ESAMObjectCreationHelper.setResponseStatusCode(responseNotification, ctx);
			if (responseNotification != null) {
				response = objectToXML(responseNotification, schema, AltContentIdentityResponseModelDelta);
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, responseNotification));

				// clean all head properties
				RequestContext.getEventContext().getMessage().clearProperties();

				return response;

			} else {//Sending system error code
				String errorMessage = (String) ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
				SignalProcessingNotificationType note = new SignalProcessingNotificationType();
				note.setStatusCode(SCCResponseUtil.generateErrorStatusCode(errorMessage));
				response = objectToXML(note, schema, AltContentIdentityResponseModelDelta);
				if (INFO_ENABLED) {
					LOGGER.debug(()->"Server internal error happened, no SCC response generated");
				}
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, responseNotification));
				return response;
			}
		} finally {
			//Just before exiting save final response for SignalStateRequestComponent
			if (aqpt != null && confirmedEventSignalId != null && response != null) {
				DataManagerFactory.getInstance().putAPConfirmedSignal(aqpt.getAcquisitionPointIdentity(), confirmedEventSignalId, (String) response);
			}
			SCCMCCThreadLocalCache.clearMyCache();
			if (INFO_ENABLED) {
				LOGGER.debug("SCC Response: \n" + response);
			}
		}
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema, Map<String, I03ResponseModelDelta> i03Deltas) {
		return schema.getResponseHandler().generateSCCResponse(signalNotification, i03Deltas);
	}
}
