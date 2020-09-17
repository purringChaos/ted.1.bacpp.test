package tv.blackarrow.cpp.components;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;
import org.mule.util.UUID;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.webservice.scte130_5_2010.ObjectFactory;
import tv.blackarrow.cpp.webservice.scte130_5_2010.ServiceCheckRequestType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.ServiceCheckResponseType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.StatusCodeType;

/**
 * Handle ADS accept notification requests simulation
 */
public class POISServiceCheckComponent implements Callable {
    private static final Logger LOGGER = LogManager.getLogger(POISServiceCheckComponent.class);
	
    @Override
	public Object onCall(final MuleEventContext context) throws Exception {
    	String requestMessage = context.getMessageAsString();
    	LOGGER.debug(()->"Service Check Request: \n"+ requestMessage);
    	String response = "";
    	ServiceCheckResponseType responseType = new ServiceCheckResponseType();
    	try {
			final StringReader reader = new StringReader(requestMessage);
			final JAXBContext jaxbCxt = JAXBUtil.getLinearPOServiceCheckJAXBContext();
			final Unmarshaller unmarshaller	= jaxbCxt.createUnmarshaller();
			JAXBElement<ServiceCheckRequestType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), ServiceCheckRequestType.class);			
			ServiceCheckRequestType event = jxbElement.getValue();
			responseType.setMessageId(UUID.getUUID().toString()); //Generated
			responseType.setMessageRef(event.getMessageId());
			responseType.setVersion(event.getVersion());
			responseType.setIdentity(CppConfigurationBean.getInstance().getPoisIdentity());  			
			responseType.setSystem(CppConfigurationBean.getInstance().getPoisSystemId());
			StatusCodeType statusCode = new StatusCodeType();
// this is not working as of comcast sprint 3.  this active testing of the
// endpoints isn't really necessary anyways so removing for now.
//			if (!isServerUp(CppConfigurationBean.getInstance().getSccEndPointMonitorUrl())) {// SCC interface. 
//				BigInteger failureCode = BigInteger.valueOf(1);
//				statusCode.setClazz(failureCode);
//				statusCode.setDetail(failureCode);
//				NoteType note = new NoteType();
//				note.setValue("Warning. Network connection lost to P3 interface. POIS Server may be down.");
//				statusCode.getNote().add(note);
//			} 
//			
//			if (!isServerUp(CppConfigurationBean.getInstance().getMccEndPointMonitorUrl())) {// MCC interface
//				BigInteger failureCode = BigInteger.valueOf(2);
//				statusCode.setClazz(failureCode);
//				statusCode.setDetail(failureCode);
//				NoteType note = new NoteType();
//				note.setValue("Warning. Network connection lost to P5 interface. POIS Server may be down.");
//				statusCode.getNote().add(note);
//			} else { //Success
				BigInteger successCode = BigInteger.valueOf(0);
				statusCode.setClazz(successCode);
				statusCode.setDetail(successCode);
//			}
			responseType.setStatusCode(statusCode);
			response  = objectToXML(responseType);
    	} catch (JAXBException e) {
			String errorMessage = e.getLinkedException().getMessage();
			LOGGER.error(()->errorMessage, e);
			context.getMessage().setProperty("http.status", CppConstants.HTTP_FAILED_VALIDATION_CODE, PropertyScope.OUTBOUND);
		}
		catch(Exception e) {
			LOGGER.error(()->e.getMessage(), e);
			context.getMessage().setProperty("http.status", CppConstants.HTTP_FAILED_VALIDATION_CODE, PropertyScope.OUTBOUND);
		}
		
		return response;
    }
    
    private String objectToXML(final ServiceCheckResponseType serviceResponse) {
		JAXBContext jaxbCxt;
		JAXBElement<ServiceCheckResponseType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPOServiceCheckJAXBContext();
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createServiceCheckResponse(serviceResponse);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		
		return writer.toString();
	}
    
    private boolean isServerUp(String endPoint) {
		// Prepare HTTP post
        PostMethod post = new PostMethod(endPoint);
        
        // set the XML content to the PostMethod
        try {
			post.setRequestEntity(new StringRequestEntity("","text/xml; charset=iso-8859-1", null));
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(()->"Can't create the post XML entity due to encoding issue: " + e.getMessage());
		}
        
        // Specify content type and encoding
        // If content encoding is not explicitly specified
        // ISO-8859-1 is assumed
        post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
        
        // Get HTTP client
        HttpClient httpclient = new HttpClient();
        
        // Execute request
        try {
            int result = httpclient.executeMethod(post);
            
            // log response
            String response = post.getResponseBodyAsString();
           
            //Below check is mandated by P2 Spec of SCTE 130-5.
            if (post.getStatusCode() == CppConstants.HTTP_NO_END_POINT_CODE) {
            	LOGGER.error(()->"PO Health Check end point is down. Received Http Code="+post.getStatusCode());
            	return false;
            }
            
        }
		catch (HttpException e) {
			LOGGER.error(()->"PO Health Check request failed due to HttpException: " + e.getMessage());
			return false;
		}
		catch (IOException e) {
			LOGGER.error(()->"PO Health Check request failed due to IOException: " + e.getMessage());
			return false;
		}
        finally {
            // Release current connection to the connection pool 
            // once you are done
            post.releaseConnection();
        }
        
        return true;
	}
}
