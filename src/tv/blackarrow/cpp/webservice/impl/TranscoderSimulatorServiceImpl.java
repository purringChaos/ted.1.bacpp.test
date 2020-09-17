
//
// Copyright (c) 2013 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.webservice.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.setting.SettingUtils;
import tv.blackarrow.cpp.utils.UUIDUtils;

/**
 * simulate a transcoder to accept ESAM out-of-band notification message   
 * 
 * one use case : a POIS notifies a transcoder (FOR QA only)
 *  
 */

@Path("/")
public class TranscoderSimulatorServiceImpl {
	private static final Logger LOGGER = LogManager.getLogger(TranscoderSimulatorServiceImpl.class);
	private static ExecutorService executor;
	private static Properties prop;
	private static AtomicBoolean firstTime = new AtomicBoolean(true);
	private String fileName = SettingUtils.getConfigurationPath() + "test/" + "aq_zone.properties";
	private String poolSize = System.getProperty("transcoder.threads.size", "5");
	private static final String LOCALHOST = "localhost";
	private static final String HEADER_HTTP_REMOTE_ADDRESS = "http.remote.address";

	@POST
	@Path("/notify")
	public Response notify(@Context ContainerRequestContext requestContext, final String message) {
		String ipAddressToResendRequest = getIpAddress(requestContext);

		LOGGER.info(() -> "Thread " + Thread.currentThread().getId() + "IP TranscoderSimulator: Received message from :" + ipAddressToResendRequest + message);

		final Response response = Response.status(Status.OK).build();
		waitForConfigurableTime();
		return response;
	}

	@POST
	@Path("/qam/notify")
	public Response notifyQAM(@Context ContainerRequestContext requestContext, final String message) {

		if (firstTime.getAndSet(false)) {
			executor = Executors.newFixedThreadPool(Integer.parseInt(poolSize));
			prop = readPropertiesFromFile(fileName);
		}
		String ipAddressToResendRequest = getIpAddress(requestContext);
		LOGGER.info(() -> "QAM TranscoderSimulator: Received notfication from " +ipAddressToResendRequest + ", Message = " + message);
		Response response = Response.status(Status.OK).build();
		waitForConfigurableTime();
		executor.execute(new QAMOOBTranscoderSimulator(ipAddressToResendRequest, message, prop));
		return response;

	}

	private void waitForConfigurableTime() {
		LOGGER.info(() -> "Thread " + Thread.currentThread().getId() + ", Waiting for : " + System.getProperty("ess.simulator.delay", "10") + "ms");
		long startTime = System.currentTimeMillis();
		try {
			TimeUnit.MILLISECONDS.sleep(Long.valueOf(System.getProperty("ess.simulator.delay", "10")));
		} catch (NumberFormatException | InterruptedException e) {
			LOGGER.error(e);
		}
		LOGGER.info(() -> "Thread " + Thread.currentThread().getId() + ", Waited for " + ( System.currentTimeMillis() - startTime ) +" ms.");
	}

	private String getIpAddress(ContainerRequestContext requestContext) {
		List<String> remoteAddrvalue = requestContext.getHeaders().get(HEADER_HTTP_REMOTE_ADDRESS);
		String ipAddressToResendRequest = LOCALHOST;
		String remoteIpAdd = remoteAddrvalue != null && !remoteAddrvalue.isEmpty() ? remoteAddrvalue.get(0) : null;
		if (StringUtils.isNotBlank(remoteIpAdd)) {
			String[] splitOnPort = remoteIpAdd.split(":");
			remoteIpAdd = splitOnPort != null && splitOnPort.length > 0 ? splitOnPort[0].replace("/", "") : LOCALHOST;
			ipAddressToResendRequest = remoteIpAdd;
		}
		return ipAddressToResendRequest;
	}

	@PreDestroy
	public void destroy() {
		if (executor != null) {
			executor.shutdown();
			try {
				executor.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executor = null;
		}
		LOGGER.info(() -> "TranscoderSimulator: Shutting down");
	}

	/**
	 * this method will give a non OK response. POIS will process it and see if it will retry
	 * the service
	 *  
	 * @param message
	 * @return
	 */
	@POST
	@Path("/failed")
	public Response notifyFailed(final String message) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(() -> "TranscoderSimulator: Received message:\n" + message);
		}
		final Response response = Response.status(Status.SERVICE_UNAVAILABLE).entity("Our service is not available, please try again later").build();

		return response;
	}

	private static Properties readPropertiesFromFile(String file) {
		File propFile = new File(file);
		InputStream is = null;
		if (propFile.exists()) {
			try {
				is = new FileInputStream(propFile);
			} catch (Exception e) {
				LOGGER.error("Failed to load configuration from " + propFile.getAbsolutePath(), e.getMessage());
			}
		}
		if (is == null) {
			LOGGER.error("TranscoderSimulator missing qam AQ to Zone Identity Matching File, please provider /opt/blackarrow/ess/conf/test/aq_zone.properties file and provide entries" + "like, " + "QAM_AQ1=ServiceZone1" + "QAM_AQ2=ServiceZone2 "
					+ propFile.getAbsolutePath());
			return null;
		}

		Properties out = new Properties();
		try {
			out.load(is);
		} catch (IOException e) {
			LOGGER.error(() -> "load " + propFile.getAbsolutePath() + " failed: " + e.getMessage());
			return null;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				return null;
			}
		}
		return out;
	}

}

class QAMOOBTranscoderSimulator implements Runnable {
	final static Logger LOGGER = LogManager.getLogger(QAMOOBTranscoderSimulator.class);
	private String message;
	private static final int MILLISECONDS_IN_A_SECOND = 1000;
	private static final short TIMEOUT_INTERVAL_IN_MILLIS = 10 * MILLISECONDS_IN_A_SECOND;
	private Properties prop;
	private String remoteIpAdd;

	public QAMOOBTranscoderSimulator(String remoteIpAdd, String message, Properties prop) {
		super();
		this.message = message;
		this.prop = prop;
		this.remoteIpAdd = remoteIpAdd;

	}

	@Override
	public void run() {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(SignalProcessingNotificationType.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			SignalProcessingNotificationType receivedNotification = (SignalProcessingNotificationType) ((JAXBElement) jaxbUnmarshaller.unmarshal(new StringReader(message))).getValue();
			ResponseSignalType firstResponseSignalType = receivedNotification.getResponseSignal().get(0);

			final int currentSystemTimeUptoSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

			String APID = firstResponseSignalType.getAcquisitionPointIdentity();
			String messageToBePrinted = APID + " with zoneIdentity= " + prop.getProperty(firstResponseSignalType.getAcquisitionPointIdentity(), "");

			String messageRequest = creatSignalProcessingEvent(receivedNotification, firstResponseSignalType, currentSystemTimeUptoSeconds);
			boolean isSuccess = postMessage("http://" + remoteIpAdd + ":6640/i03/scc/signal", messageRequest, messageToBePrinted);
			if (isSuccess) {
				Response response = Response.status(Status.OK).build();
			}

		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}

	private String creatSignalProcessingEvent(SignalProcessingNotificationType receivedNotification, ResponseSignalType firstResponseSignalType, final int currentSystemTimeUptoSeconds) throws DatatypeConfigurationException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(SignalProcessingEventType.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		// format the XML output
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		ObjectFactory objectFactory = new ObjectFactory();
		SignalProcessingEventType event = objectFactory.createSignalProcessingEventType();
		SignalProcessingEventType.AcquiredSignal acquiredSignal = objectFactory.createSignalProcessingEventTypeAcquiredSignal();
		event.getAcquiredSignal().add(acquiredSignal);
		acquiredSignal.setAcquisitionPointIdentity(receivedNotification.getAcquisitionPointIdentity());

		acquiredSignal.setAcquisitionSignalID(UUIDUtils.getBase64UrlEncodedUUID());
		acquiredSignal.setAcquisitionTime(firstResponseSignalType.getUTCPoint().getUtcPoint());
		acquiredSignal.setUTCPoint(firstResponseSignalType.getUTCPoint());

		String EMPTY_STR = "";
		acquiredSignal.getOtherAttributes().put(new QName(EMPTY_STR, "zoneIdentity"), prop.getProperty(firstResponseSignalType.getAcquisitionPointIdentity(), EMPTY_STR));

		acquiredSignal.setBinaryData(firstResponseSignalType.getBinaryData());

		StringWriter sw = new StringWriter();

		JAXBElement<SignalProcessingEventType> root = objectFactory.createSignalProcessingEvent(event);
		jaxbMarshaller.marshal(root, sw);
		String messageRequest = sw.toString();
		return messageRequest;
	}

	public static boolean postMessage(final String endPoint, final String message, String messageToBePrinted) {
		LOGGER.info(() -> "QAM TranscoderSimulator: Sending request " + messageToBePrinted + " to " + endPoint + ", Message:" + message);
		final HttpClient client = new HttpClient();
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(TIMEOUT_INTERVAL_IN_MILLIS));
		boolean isSuccess = false;
		PostMethod post = null;
		try {
			post = new PostMethod(endPoint);
			post.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(message.getBytes())));
			int statusCode = client.executeMethod(post);
			if (statusCode == HttpStatus.SC_OK) {
				isSuccess = true;
			}
		} catch (Exception ex) {
		} finally {
			post.releaseConnection();
		}
		return isSuccess;
	}

}
