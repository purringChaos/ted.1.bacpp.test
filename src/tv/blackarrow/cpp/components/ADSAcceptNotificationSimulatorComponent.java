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
import org.mule.api.transport.PropertyScope;
import org.mule.util.UUID;

import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.webservice.scte130_5_2010.ObjectFactory;
import tv.blackarrow.cpp.webservice.scte130_5_2010.POISNotificationAcknowledgementType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.POISNotificationType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.StatusCodeType;

/**
 * Handle ADS accept notification requests simulation
 */
public class ADSAcceptNotificationSimulatorComponent implements Callable {
    private static final Logger LOGGER = LogManager.getLogger(ADSAcceptNotificationSimulatorComponent.class);
    private static boolean failNextMessage = false;

    @Override
    public Object onCall(final MuleEventContext context) throws Exception {
        String requestMessage = context.getMessageAsString();
        LOGGER.debug(()->"POISNotification Request received within ADSAcceptNotificationSimulatorComponent: \n" + requestMessage);
        String response = "";
        if (failNextMessage) {
            LOGGER.debug(()->"Returning http validation failure for this message as requested");
            failNextMessage = false;
            context.getMessage().setProperty("http.status", CppConstants.HTTP_FAILED_VALIDATION_CODE, PropertyScope.OUTBOUND);
            return response;
        }
        if (requestMessage.equalsIgnoreCase("fail next message")) {
            failNextMessage = true;
            return "OK";
        }

        POISNotificationAcknowledgementType responseType = new POISNotificationAcknowledgementType();
        try {
            final StringReader reader = new StringReader(requestMessage);
            final JAXBContext jaxbCxt = JAXBUtil.getLinearPOServiceCheckJAXBContext();
            final Unmarshaller unmarshaller = jaxbCxt.createUnmarshaller();
            JAXBElement<POISNotificationType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader),
                    POISNotificationType.class);
            POISNotificationType event = jxbElement.getValue();
            responseType.setMessageId(UUID.getUUID().toString()); //Generated
            responseType.setMessageRef(event.getMessageId());
            responseType.setVersion(event.getVersion());
            responseType.setIdentity(UUID.getUUID().toString()); //Generated
            responseType.setSystem(CppConstants.POIS_ADS_SIMULATOR_SYSTEM_ID); ////hard-coded for now;

            //Success.
            StatusCodeType statusCode = new StatusCodeType();
            BigInteger successCode = BigInteger.valueOf(0);
            statusCode.setClazz(successCode);
            responseType.setStatusCode(statusCode);
            response = objectToXML(responseType);
        } catch (JAXBException e) {
            String errorMessage = e.getLinkedException().getMessage();
            LOGGER.error(()->errorMessage, e);
            context.getMessage().setProperty("http.status", CppConstants.HTTP_FAILED_VALIDATION_CODE, PropertyScope.OUTBOUND);
        } catch (Exception e) {
            LOGGER.error(()->e.getMessage(), e);
            context.getMessage().setProperty("http.status", CppConstants.HTTP_FAILED_VALIDATION_CODE, PropertyScope.OUTBOUND);
        }
        LOGGER.debug("POISNotification Response Sent from ADSAcceptNotificationSimulatorComponent: \n" + response);
        return response;
    }

    private String objectToXML(final POISNotificationAcknowledgementType serviceResponse) {
        JAXBContext jaxbCxt;
        JAXBElement<POISNotificationAcknowledgementType> jxbElement = null;
        StringWriter writer = new StringWriter();

        try {
            jaxbCxt = JAXBUtil.getLinearPOServiceCheckJAXBContext();
            Marshaller marshaller = jaxbCxt.createMarshaller();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ObjectFactory factory = new ObjectFactory();
            jxbElement = factory.createPOISNotificationAcknowledgement(serviceResponse);
            marshaller.marshal(jxbElement, writer);
        } catch (JAXBException e) {
            LOGGER.error(()->e.getMessage());
        }

        return writer.toString();
    }
}
