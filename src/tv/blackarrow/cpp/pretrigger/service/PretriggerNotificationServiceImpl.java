package tv.blackarrow.cpp.pretrigger.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.exeptions.CppException;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.ALEssAuditLogVO;
import tv.blackarrow.cpp.utils.AuditLogHelper;

/**
 * 
 * async process to send the pre-trigger notification messages
 * to the DCM/MUX/transcoders
 * 
 * log in / out message in the audit log
 *
 */
public class PretriggerNotificationServiceImpl implements PretriggerNotificationService {
	private static final Logger LOG = LogManager.getLogger(PretriggerNotificationServiceImpl.class);
	private static final String APPLICATION_XML = "application/xml";
	private static final int DEFAULT_TIMEOUT = 5000;
	private static final String POST = "POST";
	public final static String REQUEST_TYPE = "I";
    public final static String RESPONSE_TYPE = "O";
	
	/**
	 * send out-bound message to the transcoder(s)
	 * The requested format is XMIL
	 * @param url
	 * @param requestStr
	 * @return notification result is successful or not
	 */
	public boolean sendNotificationMesage(final String url, final String requestStr) {
		LOG.info("notification message as below ->" + url);
		LOG.info(requestStr);
		ALEssAuditLogVO alEssAuditLogVO = AuditLogHelper.populateALAuditLogVO();
        AuditLogger.auditPretriggerMessage(requestStr, alEssAuditLogVO,RESPONSE_TYPE);
		boolean result = false;
		try {
			String response = postRequest(url, requestStr, APPLICATION_XML, DEFAULT_TIMEOUT);
			result = true;
			LOG.info("notification response as below ->");
			LOG.info(response);
			AuditLogger.auditPretriggerMessage(response, alEssAuditLogVO,REQUEST_TYPE);
		} catch(Exception ex) {
			LOG.warn(ex.getMessage());
		}
		
		return result;
	}
	
	/**
	 * post message to the specified endpoint
	 * @param url
	 * @param requestStr
	 * @param type
	 * @param timeout
	 * @return response
	 * @throws CppException
	 */
	protected String postRequest(final String url, final String requestStr, final String type, int timeout) throws CppException {
		final StringBuffer response = new StringBuffer();
		
		try {
			final URL obj = new URL(url);
			final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod(POST);
			con.setRequestProperty(HTTP.CONTENT_TYPE, type);
			con.setReadTimeout(timeout);
			con.setConnectTimeout(timeout);

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(requestStr);
			wr.flush();
			wr.close();

			final BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch(Exception ex) {
			String msg = ex.getMessage() + " Ref URL: " + url;
			throw new CppException(msg);
		}
		
		return response.toString();
	}	
}
