/**
 * 
 */
package tv.blackarrow.cpp.utils;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;

/**
 * @author amit
 *
 */
public class HttpMessagePoster {

	private static final Logger LOGGER = LogManager.getLogger(HttpMessagePoster.class);
	
	private static final short DEFAULT_NUMBER_OF_RETRIES = 3;
	private static final short DEFAULT_RETRY_INTERVAL_IN_SECS = 30;
	private static final int MILLISECONDS_IN_A_SECOND = 1000;
	private static final short TIMEOUT_INTERVAL_IN_MILLIS = 10 * MILLISECONDS_IN_A_SECOND;
	private static final String EMPTY_STRING = "";
	public static final Gson GSON = new Gson();
	
	private HttpMessagePoster() {GSON.toJson(new Message());}
	
	
	/*public static boolean postMessage(final String endPoint, final Message message) {
		if(endPoint==null || EMPTY_STRING.equals(endPoint.trim())) {
			LOGGER.warn("No Endpoint specified.");
			throw new IllegalArgumentException("Endpoint is mandatory.");
		}
		return postMessage(endPoint, DEFAULT_NUMBER_OF_RETRIES, DEFAULT_RETRY_INTERVAL_IN_SECS, message);
	}*/

	/**
	 * Note: Big assumption inside this method is that it is assuming that it is sending messages with application/json media type.
	 * 
	 * @param endPoint
	 * @param numberOfRetries
	 * @param retryIntervalInMillis
	 * @param message
	 * @return
	 */
	/*public boolean postMessage(final String endPoint, final int numberOfRetries, final long retryIntervalInMillis, final Message message) {
		final HttpClient client = new HttpClient();         
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		int retryCounter = 0;
		boolean isSuccess = false;
		PostMethod post = null;
		final String jsonMessage = GSON.toJson(message);
		String anyException = "";
		String response = null;
		long threadID = Thread.currentThread().getId();			
		
		while(retryCounter <= numberOfRetries){
			try {
				if(retryCounter++ > 0) {
					Thread.sleep(retryIntervalInMillis);										
				}
				LOGGER.info("ThreadID:"+threadID + ", Trying to post the message attempt " + retryCounter + ", and message: " + jsonMessage + "\n to URL: " + endPoint);
				post = new PostMethod(endPoint);
				post.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(jsonMessage.getBytes())));
				post.setRequestHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
				post.setRequestHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				int statusCode = client.executeMethod(post);
				response = post.getResponseBodyAsString();
				if(statusCode == HttpStatus.SC_OK) {
					LOGGER.info("ThreadID:"+threadID + ", response from \"" + endPoint + "\" is: \"" + response + "\", and status code: " + statusCode);
					isSuccess = true;
					break;
				} else {
					LOGGER.info("ThreadID:"+threadID + ", response from \"" + endPoint + "\" is: " + response);
				}
			} catch(Exception ex) {
				anyException = "\n" + ex.getMessage() != null ? ex.getMessage() : (ex.getCause() != null ? ex.getCause().getMessage() : ex.getLocalizedMessage());
				LOGGER.warn("ThreadID:"+threadID + ", Posting message to \""+ endPoint + "\" failed, following exception occured: ", ex);
			} finally {
				post.releaseConnection();
			}
		}
		if(!isSuccess){
			StringBuilder sb = new StringBuilder();
			if (StringUtils.isNotBlank(anyException)) {
				sb.append("ThreadID:"+threadID + ", Exception Happened: " + anyException);
			}
			if (StringUtils.isNotBlank(response)) {
				sb.append("ThreadID:"+threadID + ", Response Received: " + response);
			}
			LOGGER.error("ThreadID:"+threadID + ", Posting message to \"" + endPoint + "\" failed after " + retryCounter + " times retry, Message: " + jsonMessage + sb.toString());
		}
		return isSuccess;
	}*/
	
	
	
	public static boolean postMessage(final String endPoint, final int numberOfRetries, final long retryIntervalInMillis, final String jsonMessage) {
		final HttpClient client = new HttpClient();         
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		int retryCounter = 0;
		boolean isSuccess = false;
		PostMethod post = null;
		String anyException = "";
		String response = null;
		long threadID = Thread.currentThread().getId();			
		
		while(retryCounter <= numberOfRetries){
			try {
				if(retryCounter++ > 0) {
					Thread.sleep(retryIntervalInMillis);										
				}
				LOGGER.info("ThreadID:" + threadID + ", Trying to post the message attempt " + retryCounter + ", to URL: " + endPoint + " message: \n" + jsonMessage);
				post = new PostMethod(endPoint);
				post.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(jsonMessage.getBytes())));
				post.setRequestHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
				post.setRequestHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				int statusCode = client.executeMethod(post);
				response = post.getResponseBodyAsString();
				if(statusCode == HttpStatus.SC_OK) {
					LOGGER.info("ThreadID:"+threadID + ", response from \"" + endPoint + "\" is: \"" + response + "\", and status code: " + statusCode);
					isSuccess = true;
					break;
				} else {
					LOGGER.info("ThreadID:"+threadID + ", response from \"" + endPoint + "\" is: " + response);
				}
			} catch(Exception ex) {
				anyException = "\n" + ex.getMessage() != null ? ex.getMessage() : (ex.getCause() != null ? ex.getCause().getMessage() : ex.getLocalizedMessage());
				LOGGER.warn("ThreadID:"+threadID + ", Posting message:"+jsonMessage+" to \""+ endPoint + "\" failed, following exception occured: ", ex);
			} finally {
				post.releaseConnection();
			}
		}
		if(!isSuccess){
			StringBuilder sb = new StringBuilder();
			if (StringUtils.isNotBlank(anyException)) {
				sb.append("ThreadID:"+threadID + ", Exception Happened: " + anyException);
			}
			if (StringUtils.isNotBlank(response)) {
				sb.append("ThreadID:"+threadID + ", Response Received: " + response);
			}
			LOGGER.error("ThreadID:"+threadID + ", Posting message to \"" + endPoint + "\" failed after " + retryCounter + " times retry, Message: " + jsonMessage + sb.toString());
		}
		return isSuccess;
	}
}
