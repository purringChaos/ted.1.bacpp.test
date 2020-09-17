/**
 * 
 */
package tv.blackarrow.cpp.notifications.upstream.messages.queue;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Amit Kumar Sharma
 *
 */
public class AcquisitionPointNotificationService {

	private static final Logger LOGGER = LogManager.getLogger(AcquisitionPointNotificationService.class);
	private static final boolean IS_INFO_ENABLED = LOGGER.isInfoEnabled();
	private static final short DEFAULT_NUMBER_OF_RETRIES = 3;
	private static final short DEFAULT_RETRY_INTERVAL_IN_MSECS = 10;
	private static final int MILLISECONDS_IN_A_SECOND = 1000;
	private static final short TIMEOUT_INTERVAL_IN_MILLIS = 10 * MILLISECONDS_IN_A_SECOND;

	public static boolean notify(NotificationMessage notificationMessage) {
		final HttpClient client = new HttpClient();
		String endPoint = notificationMessage.getStreamURL();
		String message = notificationMessage.getOnFlyCreatedScheduledUpStreamMessage();
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		int retryCounter = 0;
		boolean isSuccess = false;
		PostMethod post = null;
		final Thread CURRENT_THREAD = Thread.currentThread();
		final String LOG_IDENTIFIER = "Thread: " + CURRENT_THREAD.getName() + "_" + CURRENT_THREAD.getId() + ": Event:" + notificationMessage.getEventId() + "("
				+ notificationMessage.getScheduledNotificationId() + ")" + ", action:" + notificationMessage.getEventAction();

		while (retryCounter <= DEFAULT_NUMBER_OF_RETRIES) {
			try {
				if (retryCounter++ > 0) {
					TimeUnit.MILLISECONDS.sleep(DEFAULT_RETRY_INTERVAL_IN_MSECS);					
				}
				if (IS_INFO_ENABLED) {
					LOGGER.info(LOG_IDENTIFIER + ", Attempt: " + retryCounter + ", posting to URL:" + endPoint+ ", message: " + message);
				}
				post = new PostMethod(endPoint);
				post.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(message.getBytes())));
				int statusCode = client.executeMethod(post);
				final String response = post.getResponseBodyAsString();
				if (statusCode == HttpStatus.SC_OK) {
					if (IS_INFO_ENABLED)
						LOGGER.info(LOG_IDENTIFIER + ", Attempt: " + retryCounter + ", Response from URL:" + endPoint + " : '" + response + "'(" + statusCode + ")");
					isSuccess = true;
					break;
				} else {
					LOGGER.warn(LOG_IDENTIFIER + ", Attempt: " + retryCounter + ", Response from URL:" + endPoint + " : '" + response + "'(" + statusCode + ")");
				}
			} catch (Exception ex) {
				LOGGER.warn(LOG_IDENTIFIER + "Posting message to \"" + endPoint + "\" failed, following exception occured: ", ex);
			} finally {
				if(post!=null) {
					post.releaseConnection();
				}
			}
		}
		if (!isSuccess) {
			LOGGER.error(LOG_IDENTIFIER + "Failed to post message to endpoint: " + endPoint + " after "+ DEFAULT_NUMBER_OF_RETRIES + " trials.");
		}
		return isSuccess;
	}
}
