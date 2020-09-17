//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
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

package tv.blackarrow.cpp.components;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.handler.I03RequestHandler;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ProcessStatusAcknowledgementType;
import tv.blackarrow.cpp.i03.signaling.ProcessStatusNotificationType;
import tv.blackarrow.cpp.i03.signaling.StatusCodeType;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;

/**
 * 
 * take ESAM PSN request
 * @author snagpal
 *
 */
public class PSNComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(PSNComponent.class);
	public static final String ERROR_MESSAGE = "ESAM I03 error. Please ensure messages sent to this endpoint comply with ESAM I03 schema.";
	
	@Override
	public Object onCall(final MuleEventContext context) throws Exception {

		
		String response = "";
		String message = context.getMessageAsString();
		LOGGER.debug(()->"ESAM PSN request:\n" + message);
		ProcessStatusAcknowledgementType acknowlegement = new ProcessStatusAcknowledgementType();
		JAXBContext linearPOISPsnJAXBContext = I03RequestHandler.linearPOISSccJAXBContext;
		ProcessStatusNotificationType notification = null;
		try {
			notification = parsePSNRequest(message,linearPOISPsnJAXBContext);
			
			//Log into message log.
			AuditLogger.auditMessage(message, AuditLogHelper.populateAuditLogVO(context, notification));
			
			if (notification != null) {
				BigInteger statusCode = notification.getStatusCode().getClassCode();
				// Checking the status code if its not equal to 0 then log the WARN level message.
				if (statusCode == null || statusCode.intValue() != 0) {
					LOGGER.warn(()->"The Status Code for this request is not valid " + message);
				}
				setProcessStatusAcknowledgement(notification, acknowlegement);
			} else {
				LOGGER.error(()->ERROR_MESSAGE);
				acknowlegement.setStatusCode(generateErrorStatusCode(ERROR_MESSAGE));
			}
		} catch (JAXBException jaxb) {
			LOGGER.error(jaxb.getMessage(), jaxb);
			acknowlegement.setStatusCode(generateErrorStatusCode(jaxb.getCause().getMessage()));
		} catch (Exception e) {
			LOGGER.error(()->e.getMessage(), e);
			acknowlegement.setStatusCode(generateErrorStatusCode(e.getMessage()));
		} finally {
			response = objectToXML(acknowlegement, linearPOISPsnJAXBContext);
		}
		
		//Log into message log.
		AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(context, notification));
		return response;
	}

	private ProcessStatusNotificationType parsePSNRequest(String requestXml,JAXBContext linearPOISPsnJAXBContext) throws Exception {
		if (!requestXml.contains(CppConstants.IO3_NAMESPACE_SIGNALING)) {
			throw new RuntimeException(ERROR_MESSAGE);
		}
		final StringReader reader = new StringReader(requestXml);
		final Unmarshaller unmarshaller = linearPOISPsnJAXBContext.createUnmarshaller();
		JAXBElement<ProcessStatusNotificationType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader),ProcessStatusNotificationType.class);
		return jxbElement.getValue();
	}

	private String objectToXML(final ProcessStatusAcknowledgementType acknowlegement,JAXBContext linearPOISPsnJAXBContext) throws JAXBException {
		JAXBElement<ProcessStatusAcknowledgementType> jxbElement = null;
		StringWriter writer = new StringWriter();
		Marshaller marshaller = I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		ObjectFactory factory = new ObjectFactory();
		jxbElement = factory.createProcessStatusAcknowledgement(acknowlegement);
		marshaller.marshal(jxbElement, writer);
		return writer.toString();
	}
	
	private StatusCodeType generateErrorStatusCode(String errorMessage){
		StatusCodeType statusCode = new StatusCodeType();
		statusCode.setClassCode(BigInteger.ONE);
		statusCode.setDetailCode(BigInteger.ONE);
		statusCode.getNote().add(errorMessage);
		return statusCode;
	}
	
	private void setProcessStatusAcknowledgement(ProcessStatusNotificationType notification, ProcessStatusAcknowledgementType acknowlegement){
		DataManager dataManager = DataManagerFactory.getInstance();
		AcquisitionPoint aqpt = null;
		aqpt = dataManager.getAcquisitionPoint(notification.getAcquisitionPointIdentity());
		if (aqpt == null) {
			String msg = "Unknown acquisition point identity : " + notification.getAcquisitionPointIdentity();
			LOGGER.warn(()->msg);
			acknowlegement.setStatusCode(generateErrorStatusCode(msg));
		} else{
			acknowlegement.setAcquisitionPointIdentity(notification.getAcquisitionPointIdentity());
			acknowlegement.setAcquisitionSignalID(notification.getAcquisitionSignalID());
			acknowlegement.setBatchId(notification.getBatchId());
		}
	}
	
}
