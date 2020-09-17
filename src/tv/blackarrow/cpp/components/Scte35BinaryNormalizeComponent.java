package tv.blackarrow.cpp.components;

import java.util.Collections;
import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.ALEssAuditLogVO;
import tv.blackarrow.cpp.normalize.NormalizationVersion;
import tv.blackarrow.cpp.normalize.Scte35NormalizationHandlerFactory;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;

/**
 * The component will normalize SCTE 35 binary data, convert Splcie_inert into
 * Time Signal signal (using existing PTS and Duration)
 *  * 
 * requirement background for LG (VM): 
 *         SCTE 35 time_signal with segmentation descriptor 0x36
 *         (pre-trigger),In VM AL enabled channels, SCTE 35 markers are used to
 *         indicate signal advertisement opportunities, in combination with
 *         third-party ad services that insert the actual ads. For the purposes
 *         of linear ad insertion, VM only requires a small subset of the larger
 *         set of signals be present in the transport stream. For the purposes
 *         of linear ad insertion, VM only requires the following signals:
 * 
 *         - SCTE 35 time_signal with segmentation descriptor 0x36 (VM timing
 *         trigger)
 *         - SCTE 35 timing signal as provided by the content partner,
 *         and transformed in the ESS 
 *         
 *         Other signals may be present on the stream but will be ignored for 
 *         the purposes of linear ad insertion.
 * 
 *         VM implementation only uses time_signal type SCTE 35 marker. However,
 *         some content partners may choose to use SCTE 35 splice_insert to
 *         indicate the position of an opportunity (timing trigger). In this
 *         case, VM requires Cadent side normalize splice_insert type SCTE 35 to
 *         the VM format timing trigger (time_signal splice command type).
 *
 * solution design doc can be found at 
 * 		https://confluence.crossmw.com/display/ALP/SCTE+35+Signal+Normalization+Solution
 * 
 * @author jwang
 */
public class Scte35BinaryNormalizeComponent implements Callable {
	private static final Logger LOG = LogManager.getLogger(Scte35BinaryNormalizeComponent.class);
	public final static String DELIVERY_NOT_RESTRICTED_FLAG = "deliveryNotRestrictedFlag";
	public final static String WEB_DELIVERY_ALLOW_FLAG = "webDeliveryAllowedFlag";
	public final static String NO_REGIONAL_BLACKOUT_FLAG = "no_regional_blackout_flag";
	public final static String REQUEST_TYPE = "I";
	public final static String RESPONSE_TYPE = "O";
	
	@Override
	public Object onCall(final MuleEventContext context) throws Exception {
		String response;
		final String originalMessage =  context.getMessageAsString();
		LOG.debug(()->"SCC request:\n" + originalMessage);

		// right now, Cadent support ESAM i01 and i03
		Schema schema;
		NormalizationVersion version;  // default
		String path = context.getEndpointURI().getPath();
		if(path.endsWith("scc/i03/normalize/v2")) {
			schema = Schema.getSchema("i03"); //  based on i03
			version = NormalizationVersion.V2;
		} else if(path.endsWith("scc/normalize/v2")) {
			schema = Schema.getSchema("i01"); //  based on i03
			version = NormalizationVersion.V2;
		} else if(path.endsWith("scc/i03/normalize")) {
			schema = Schema.getSchema("i03"); //  based on i03
			version = NormalizationVersion.V1;
		} else {
			schema = Schema.getSchema("i01");  // default
			version = NormalizationVersion.V1;
		}
		
		final ALEssAuditLogVO alEssAuditLogVO = AuditLogHelper.populateALNormalizeAuditLogVO(context);
	    AuditLogger.auditNormalizeMessage(originalMessage,alEssAuditLogVO,REQUEST_TYPE);
		try {
			SignalProcessingEventType event = schema.getRequestHandler().parseSCCRequest(originalMessage);			
			SignalProcessingNotificationType notification = 
					Scte35NormalizationHandlerFactory.getNormalizationHandler(version).normalizeSignal(event);

			Collections.<ResponseSignalType>sort(notification.getResponseSignal(), new Comparator<ResponseSignalType>() {
			    @Override
				public int compare(final ResponseSignalType responseSignalType1, final ResponseSignalType responseSignalType2){
			    	final long utctime1 = responseSignalType1.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
			    	final long utctime2 = responseSignalType2.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
			    	return utctime1 < utctime2 ? -1 : utctime1 == utctime2 ? 0 : 1;
			    }});
			
			response =  schema.getResponseHandler().generateSCCResponse(notification, null);
			LOG.info(()->"=================== response =================");
			LOG.info(response);
			AuditLogger.auditNormalizeMessage(response,alEssAuditLogVO, RESPONSE_TYPE);
		} catch(Exception ex) {
			LOG.warn(ex.getMessage(), ex);
			response="";
		}
		
		return response;
		
	}


}
