
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
//

package tv.blackarrow.cpp.components;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.UUIDUtils;
import tv.blackarrow.cpp.webservice.scte130_5_2010.BasicQueryResultType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.ObjectFactory;
import tv.blackarrow.cpp.webservice.scte130_5_2010.POISNotificationType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.QualifierSetType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.QualifierType;
import tv.blackarrow.cpp.webservice.scte130_5_2010.QueryResultType;

/**
 * 
 * POIS notify ADS any confirmed signal ID and POs
 *
 */
public class UpdateADSComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(UpdateADSComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {
		final HashMap<String,ConfirmedPlacementOpportunity> decisions = (HashMap<String,ConfirmedPlacementOpportunity>) context.getMessage().getProperty("decisions", PropertyScope.OUTBOUND);
		final AcquisitionPoint aqpt = (AcquisitionPoint) context.getMessage().getProperty("acquisitionPoint", PropertyScope.OUTBOUND);
		final Map<String,String> ptsTimesBySignalId = (Map<String,String>) context.getMessage().getProperty("ptsTimesBySignalId", PropertyScope.OUTBOUND);
		Boolean newConfirmation = (Boolean) context.getMessage().getProperty("newConfirmation", PropertyScope.OUTBOUND);
		SignalProcessingNotificationType notification = context.getMessage().getProperty("signal_response", PropertyScope.OUTBOUND);
		
		if (aqpt == null) 
			return "";
		boolean notFromSchedules = CppConstants.NO_SCHEDULES.equals(aqpt.getBaSchedulesInterfaceTypeExternalRef());
		if (notFromSchedules || aqpt.isUseInbandOpportunitySignal()) 
			return ""; //we don't need to do P2 notification in the case of Comcast TVE mode
		if (decisions != null && !decisions.isEmpty() && (newConfirmation != null && newConfirmation.booleanValue() == true)) {
			CppConfigurationBean config = CppConfigurationBean.getInstance();
			String poNotificationUrl = config.getLinearPoisToExternalAdsNotificationUrl();
			if (!poNotificationUrl.isEmpty()) {
				String notificationXml = generateP2CompliantPoNotificationXml(decisions, aqpt, ptsTimesBySignalId);
				notifyLinearAds(notificationXml, poNotificationUrl, context, notification);
			}
			poNotificationUrl = config.getLinearPoisToAdsNotificationUrl();
			if (!poNotificationUrl.isEmpty()) {
				String notificationXml = generatePoNotificationXml(decisions, aqpt);
				notifyLinearAds(notificationXml, poNotificationUrl, context, notification);
			}
		}
		return "";
	}

	private String generatePoNotificationXml(
			HashMap<String, ConfirmedPlacementOpportunity> decisions, AcquisitionPoint aqpt) {
		
		POISNotificationType poisNotificationType = new POISNotificationType();
		poisNotificationType.setIdentity(UUIDUtils.getUUID().toString());
		poisNotificationType.setMessageId(UUIDUtils.getUUID().toString());
		poisNotificationType.setNoticeType("new");
		poisNotificationType.setSystem(CppConstants.POIS_SYSTEM_ID);
		poisNotificationType.setVersion(CppConstants.POIS_P2_VERSION_ID);
		
		QueryResultType queryResultType = new QueryResultType();
		poisNotificationType.setQueryResult(queryResultType);

		QualifierType qt = null;
		QualifierSetType qst = null;
		
		BasicQueryResultType basicQueryResultType = new BasicQueryResultType();

		Collection<ConfirmedPlacementOpportunity> cpos = decisions.values();
		if (cpos != null && !cpos.isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			for (ConfirmedPlacementOpportunity cpo : cpos) {
				// for each decision of AquiredSignal
				// we will generate a corresponding <gis:QualifierSet> section in the POISNotification request XML
				qst = new QualifierSetType();
				basicQueryResultType.getQualifierSet().add(qst);
				
				// utcSignalTime
				qt = new QualifierType();
				qt.setName("utcSignalTime");
				qt.setValue(sdf.format(new Date(cpo.getUtcSignalTime())));
				qst.getQualifier().add(qt);
				
				// signalId
				qt = new QualifierType();
				qt.setName("signalId");
				qt.setValue(cpo.getSignalId());
				qst.getQualifier().add(qt);
				
				// ad zone external ref to its specific signalId map
				Map <String, String> adZoneMap = cpo.getPoKeyByZone();
				Set<Map.Entry<String,String>> entries = adZoneMap.entrySet();
				if (entries != null && !entries.isEmpty()) {
					for (Map.Entry<String,String> entry : entries) {
						qt = new QualifierType();
						qt.setName(entry.getKey());
						qt.setValue(entry.getValue());
						qst.getQualifier().add(qt);
					}
				}
								
				qt = new QualifierType();
				qt.setName("feedExternalRef");
				qt.setValue(aqpt.getFeedExternalRef());
				qst.getQualifier().add(qt);
			}
		}
		
		ObjectFactory factory = new ObjectFactory();
		JAXBElement basicQueryResultTypeJaxbElement = factory.createBasicQueryResult(basicQueryResultType);
		queryResultType.setBasicQueryResultAbstract(basicQueryResultTypeJaxbElement);
		
		return objectToXML(poisNotificationType);
	}

	private String objectToXML(final POISNotificationType notification) {
		JAXBContext jaxbCxt;
		JAXBElement<POISNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPONotificationJAXBContext();
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createPOISNotification(notification);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->"Can't convert POISNotificationType object to XML: " + e.getMessage());
		}
		
		return writer.toString();
	}

	private String generateP2CompliantPoNotificationXml(
			Map<String, ConfirmedPlacementOpportunity> decisions, AcquisitionPoint aqpt, Map<String, String> ptsTimes) {
		
		POISNotificationType poisNotificationType = new POISNotificationType();
		poisNotificationType.setMessageId(UUIDUtils.getUUID().toString());
		poisNotificationType.setNoticeType("new"); //default
		poisNotificationType.setSystem(CppConfigurationBean.getInstance().getPoisSystemId());
		poisNotificationType.setIdentity(CppConfigurationBean.getInstance().getPoisIdentity());
		poisNotificationType.setVersion(CppConstants.POIS_P2_VERSION_ID); //default
		
		QueryResultType queryResultType = new QueryResultType();
		poisNotificationType.setQueryResult(queryResultType);

		QualifierType qt = null;
		QualifierSetType qst = null;
		
		BasicQueryResultType basicQueryResultType = new BasicQueryResultType();

		Collection<ConfirmedPlacementOpportunity> cpos = decisions.values();
		
		
		int resultSize = 0;
		if (cpos != null && !cpos.isEmpty()) {
			for (ConfirmedPlacementOpportunity cpo : cpos) {
				// for each decision of AquiredSignal
				// we will generate a corresponding <gis:QualifierSet> section in the POISNotification request XML
				if (cpo != null && cpo.getBreakInfos() != null) {
					for (BreakInfo breakInfo : cpo.getBreakInfos()) {
						qst = new QualifierSetType();
						basicQueryResultType.getQualifierSet().add(qst);
						
						// POID
						qt = new QualifierType();
						qt.setName("POID");
						qt.setValue(breakInfo.getBreakId());
						qst.getQualifier().add(qt);
						
						// PAID
						qt = new QualifierType();
						qt.setName("PAID");
						if (aqpt != null) qt.setValue(aqpt.getProviderExternalRef()+CppConstants.POID_DELIMITER+aqpt.getFeedExternalRef());
						else qt.setValue(CppConstants.POID_DELIMITER);
						qst.getQualifier().add(qt);
						
						//ADI specific key and value pairs.
						Map<String,String> qualifiers = breakInfo.getQualifiers();
						if (qualifiers != null) {
							Set<String> keys = qualifiers.keySet();
							for (String key : keys) {
								qt = new QualifierType();
								qt.setName(key);
								qt.setValue(qualifiers.get(key));
								qst.getQualifier().add(qt);
							}
						}
						
						//UTC signal time
						qt = new QualifierType();
						qt.setName("UTC_POINT_START");
						qt.setValue(cpo.getUtcSignalTime()+"");
						qst.getQualifier().add(qt);
						
						//Sum of UTC signal time and break duration
						qt = new QualifierType();
						qt.setName("UTC_POINT_END");
						qt.setValue(cpo.getUtcSignalTime()+breakInfo.getDuration()+"");
						qst.getQualifier().add(qt);
						
						//Out signal Id
						qt = new QualifierType();
						qt.setName("SIGNAL_ID_START");
						qt.setValue(cpo.getSignalId());
						qst.getQualifier().add(qt);
						
						//In Signal id
						qt = new QualifierType();
						qt.setName("SIGNAL_ID_END");
						qt.setValue(breakInfo.getInSignalId());
						qst.getQualifier().add(qt);
						
						/*//BASED ON COMCAST CLIENT< THIS IS NOT NEEDED
						//Stream times
						qt = new QualifierType();
						qt.setName("STREAM_TIMES");
						StringBuffer streamTimes = new StringBuffer();
						streamTimes.append("<StreamTimes><StreamTime TimeType=\"Smooth\" TimeValue=\"");
						streamTimes.append(""); //TODO: Ask Comcast on this smooth time and implement. e.g. 1512489602888
						streamTimes.append("\"/><StreamTime TimeType=\"PTS\" TimeValue=\"");
						String ptsTime = ptsTimes.get(cpo.getSignalId());
						if (ptsTime == null) streamTimes.append(""); 
						else {
							try {
								streamTimes.append(Long.parseLong(ptsTime, 2)); 
							} catch (NumberFormatException nfe) {
								streamTimes.append(""); 
							}
						}
						
						streamTimes.append("\"/><StreamTime TimeType=\"Signal\" TimeValue=\"");
						streamTimes.append(cpo.getSignalId()).append("\"/></StreamTimes>");
						
						qt.setValue(streamTimes.toString());
						qst.getQualifier().add(qt);
						
						//Stream Times End
						qt = new QualifierType();
						qt.setName("STREAM_TIMES_END");
						
						StringBuffer streamEnd = new StringBuffer();
						streamEnd.append("<StreamTimes><StreamTime TimeType=\"Signal\"");
						streamEnd.append("TimeValue=\"").append(breakInfo.getInSignalId()).append("\"/></StreamTimes>");
						
						qt.setValue(streamEnd.toString());
						qst.getQualifier().add(qt);
						*/
						
						//Break duration
						qt = new QualifierType();
						qt.setName("DURATION");
						String duration = "";
						try {
							duration = JavaxUtil.getDatatypeFactory().newDuration(breakInfo.getDuration()).toString();
						} catch (DatatypeConfigurationException e) {
							LOGGER.error(()->"Duration Conversion Error", e);
						}
						qt.setValue(duration);
						qst.getQualifier().add(qt);
						
						qt = new QualifierType();
						qt.setName("feedExternalRef");
						qt.setValue(aqpt.getFeedExternalRef());
						qst.getQualifier().add(qt);
						
						++resultSize;
					}
				}
			}
		}
		
		queryResultType.setQueryRef(UUIDUtils.getUUID().toString()); //As a notifier of placements, let's sent a unique query reference as UUID.
		queryResultType.setResultSetSize(BigInteger.valueOf(resultSize));
		
		ObjectFactory factory = new ObjectFactory();
		JAXBElement basicQueryResultTypeJaxbElement = factory.createBasicQueryResult(basicQueryResultType);
		queryResultType.setBasicQueryResultAbstract(basicQueryResultTypeJaxbElement);
		
		return objectToXML(poisNotificationType);
	}
	
	private void notifyLinearAds(String xml, String endPoint, MuleEventContext context, SignalProcessingNotificationType notification) {
		
		// log the po notification xml sent to ADS
		LOGGER.debug(()->"POISNotification XML: " + xml);
		
		//Log into message log.
		AuditLogger.auditMessage("POISNotification Request: \n"+xml, AuditLogHelper.populateAuditLogVO(context, notification));
	
        // Prepare HTTP post
        PostMethod post = new PostMethod(endPoint);
        
        // set the XML content to the PostMethod
        try {
			post.setRequestEntity(new StringRequestEntity(xml,"text/xml; charset=iso-8859-1", null));
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
            
            String response = post.getResponseBodyAsString();
            
    		//Log into message log.
    		AuditLogger.auditMessage("POISNotification Response: \n"+response, AuditLogHelper.populateAuditLogVO(context, notification));
    		
            // log status code
            LOGGER.debug(()->"Response status code: " + result);
    		// log response
            LOGGER.debug(()->"Response body: \n"+response);
    
            //Below check is mandated by P2 Spec of SCTE 130-5.
            if (post.getStatusCode() != CppConstants.HTTP_SUCCESS_CODE) {
            	if (post.getStatusCode() == CppConstants.HTTP_FAILED_VALIDATION_CODE) {
            		LOGGER.error(()->"PO Notification Request Failed due to Validation or Parse Error."+
            				"Received Http Code="+post.getStatusCode()+" for message "+xml);
            	} else  if (post.getStatusCode() == CppConstants.HTTP_NO_END_POINT_CODE) {
            		LOGGER.error(()->"PO Notification Request Failed because of no ADS End-Point Running at: "+endPoint+
            				". Received Http Code="+post.getStatusCode()+" for message "+xml);
            	} else {
            		LOGGER.error(()->"PO Notification Request Failed for unknown reason. Received Http Code="+post.getStatusCode()+" for message "+xml);
            	}
            }
            
        }
		catch (HttpException e) {
			LOGGER.error(()->"POIS Notification post failed due to HttpException: " + e.getMessage());
		}
		catch (IOException e) {
			LOGGER.error(()->"POIS Notification post failed due to IOException: " + e.getMessage());
		}
        finally {
            // Release current connection to the connection pool 
            // once you are done
            post.releaseConnection();
        }
	}
	
	public static void main(String [] args) {
		/*
		String xml = "" + 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
			"<POISNotification " + 
			"	messageId=\"req-123\" system=\"BAPOIServer\" identity=\"ab80db91-059f-4234-90ed-f3a2ba1f1ea4\" version=\"1.0\" noticeType=\"new\"" + 
			"	xmlns=\"http://www.scte.org/schemas/130-5/2010/pois\"" + 
			"	xmlns:gis=\"http://www.scte.org/schemas/130-8/2010a/gis\"" + 
			"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + 
			"	<gis:QueryResult queryRef=\"1\" totalResultSetSize=\"1\" resultSetSize=\"1\">" + 
			"		<gis:BasicQueryResult>" + 
			"			<gis:QualifierSet>" + 
			"				<gis:Qualifier name=\"utcSignalTime\" value=\"2012-12-10T01:35:20Z\"/>" + 
			"				<gis:Qualifier name=\"signalId\" value=\"b23f39d9-b03e-4602-ad85-37b15fb978ac\"/>" + 
			"				<gis:Qualifier name=\"079\" value=\"41d09a1f-2522-4197-a64b-774b056bab2f\"/>" + 
			"				<gis:Qualifier name=\"088\" value=\"d1c36365-6589-44dd-be63-04632f6ef422\"/>" + 
			"				<gis:Qualifier name=\"093\" value=\"0c69a08d-c4b7-4feb-a56b-672b9d5dd116\"/>" + 
			"			</gis:QualifierSet>" + 
			"		</gis:BasicQueryResult>" + 
			"	</gis:QueryResult>" + 
			"</POISNotification>";
		*/
		
		UpdateADSComponent thisComponent = new UpdateADSComponent();
		
		HashMap<String, ConfirmedPlacementOpportunity> decisions = new HashMap<String, ConfirmedPlacementOpportunity>();
		String signalId = UUIDUtils.getUUID().toString();
		ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
		decisions.put(signalId, cpo);
		cpo.setSignalId(signalId);
		cpo.setUtcSignalTime(Calendar.getInstance().getTime().getTime());
		HashMap<String,String> adZoneMap = new HashMap<String,String>();
		cpo.setPoKeyByZone(adZoneMap);
		adZoneMap.put("079", UUIDUtils.getUUID().toString());
		adZoneMap.put("088", UUIDUtils.getUUID().toString());
		adZoneMap.put("093", UUIDUtils.getUUID().toString());
		
		signalId = UUIDUtils.getUUID().toString();
		cpo = new ConfirmedPlacementOpportunity();
		decisions.put(signalId, cpo);
		cpo.setSignalId(signalId);
		
		HashMap<String, String> qualifierMap = new HashMap<String, String>();
		/* for now it is hard-coded based on P2 example xml */
		
		qualifierMap.put("TYPE_OF_OPPORTUNITY", "LinearVideoAd");
		qualifierMap.put("FIXED_DURATION", "true");
		qualifierMap.put("OWNERSHIP", "Distributor");
		qualifierMap.put("CONFIRMED", "true");
		
		
		cpo.setUtcSignalTime(Calendar.getInstance().getTime().getTime());
		ArrayList<BreakInfo> breakInfos = new ArrayList<BreakInfo>();
		BreakInfo breakInfo1 = new BreakInfo("insignalId1", "breakId1", 30000, qualifierMap);
		BreakInfo breakInfo2 = new BreakInfo("insignalId2", "breakId2", 60000, qualifierMap);
		BreakInfo breakInfo3 = new BreakInfo("insignalId3", "breakId3", 90000, qualifierMap);
		breakInfos.add(breakInfo1);
		breakInfos.add(breakInfo2);
		breakInfos.add(breakInfo3);
		cpo.setBreakInfos(breakInfos);
		adZoneMap = new HashMap<String,String>();
		cpo.setPoKeyByZone(adZoneMap);
		adZoneMap.put("091", UUIDUtils.getUUID().toString());
		adZoneMap.put("092", UUIDUtils.getUUID().toString());
		adZoneMap.put("093", UUIDUtils.getUUID().toString());
		
		
		
		//test P2 compliant interface
		AcquisitionPoint aq = new AcquisitionPoint();
		aq.setAcquisitionPointIdentity("id1");
		aq.setBaIntefactTypeExternalRef("includeInPoints");
		aq.setFeedExternalRef("feedExternalRef");
		aq.setProviderExternalRef("providerExternalRef");
		
		String xml = thisComponent.generatePoNotificationXml(decisions, aq);
		
		Map<String, String> ptsTimes = new HashMap<String, String>();
		ptsTimes.put(signalId, "343434343");
		
		xml = thisComponent.generateP2CompliantPoNotificationXml(decisions, aq, ptsTimes);
		System.out.println(xml);
		//thisComponent.notifyLinearAds(xml, "");
    }
	
	
}
