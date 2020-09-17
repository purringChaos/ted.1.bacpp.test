package tv.blackarrow.cpp.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import com.google.gson.Gson;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventRuntimeNotificationsHandler;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.ds.acs.model.AcsQueryRequest;
import tv.blackarrow.ds.acs.model.AcsResponse;
import tv.blackarrow.ds.acs.model.Restriction;
import tv.blackarrow.integration.IntegrationServicesException;

public class BlackoutQAMHandler {

	private static Logger LOGGER = LogManager.getLogger(BlackoutQAMHandler.class);

	private static Client jerseyClient = null;
	static{
		jerseyClient = ClientBuilder.newClient();
	}

	private static Gson gson = new Gson();

	public static I03ResponseModelDelta retrieveZoneInfo(BlackoutEvent event, ConfirmedPlacementOpportunity commonCPO, Map<QName, String> attributes)
			throws IntegrationServicesException {

		try {
			if(event.getRestrictions()!= null && !event.getRestrictions().isEmpty()) {
				return getRestrictions(event, attributes);
			} 
		} catch (Exception e) {
			throw new IntegrationServicesException("Error Retrieving Zone.", e);
		}
		return new I03ResponseModelDelta();
	}

	private static I03ResponseModelDelta getRestrictions(BlackoutEvent event, Map<QName, String> attributes) {
		I03ResponseModelDelta i03ResponseModelDelta = new I03ResponseModelDelta();
		 String restrictionType = CppConfigurationBean.getInstance().getQamZoneIdentityType();
		 Set<String> restrictionValuesReceivedinRequest = (attributes != null && !attributes.isEmpty()) ? new HashSet<>(attributes.values()) : new HashSet<>();
		 for(tv.blackarrow.cpp.model.Restriction restriction: event.getRestrictions()) {
			 if(restrictionType.equalsIgnoreCase(restriction.getRestrictionType())) {
				 if(!restrictionValuesReceivedinRequest.isEmpty() && !restrictionValuesReceivedinRequest.contains(restriction.getRestrictionValue())) {
					 continue;
				 }
				AlternateContentTypeModel delta = new AlternateContentTypeModel();
				 delta.setAltContentIdentity(restriction.getAltSourceValue());
				 delta.setZoneIdentity(restriction.getRestrictionValue());
				i03ResponseModelDelta.getAlternateContentIDList().add(delta);
			 }
		 }
		return i03ResponseModelDelta;
	}


	private static I03ResponseModelDelta parse(AcsResponse queryResponse) throws IntegrationServicesException {
		 if(queryResponse == null){ return null;}
		 if(queryResponse.getStatusCode()!=0){
			 LOGGER.error(()->"Error happens while query ACS, status code is " + queryResponse.getStatusCode());
			 throw new IntegrationServicesException("Error querying ACS: " + queryResponse.getMessage());
		 }

		 List<Restriction> restictions = queryResponse.getRestrictions();
		 if(restictions == null || restictions.isEmpty()){
			 if(LOGGER.isInfoEnabled()){
				 LOGGER.info(()->"No retriction found from acs");
			 }
			 return null;
		 }

		I03ResponseModelDelta i03ResponseModelDelta = new I03ResponseModelDelta();
		 for(Restriction r: queryResponse.getRestrictions()){
			AlternateContentTypeModel t = new AlternateContentTypeModel();
			 t.setAltContentIdentity(r.getAltSourceValue());
			 t.setZoneIdentity(r.getRestrictionValue());
			i03ResponseModelDelta.getAlternateContentIDList().add(t);
		 }

		return i03ResponseModelDelta;
	}


	private static Client getJerseyClient() {
	    	return jerseyClient;
	    }

	public static AcsQueryRequest getACSQuery(String singleId, Map<QName, String> attributes) {
		AcsQueryRequest query = new AcsQueryRequest();
		query.setSignalId(singleId);
		query.setNumberSamples(Integer.MAX_VALUE);

		String restrictionType = CppConfigurationBean.getInstance().getQamZoneIdentityType();

		List<Restriction> restrictions = new ArrayList<Restriction>();
		if(attributes != null && !attributes.isEmpty()){
			for(Entry<QName, String> attri: attributes.entrySet()){
				Restriction r = new Restriction();
				r.setRestrictionType(restrictionType);
				r.setRestrictionValue(attri.getValue());
				restrictions.add(r);
			}
		}else{
			LOGGER.warn("@zoneIdentity missing - cannot determine whether alternate content should be used.");
			Restriction r = new Restriction();
			r.setRestrictionType(restrictionType);
			r.setRestrictionValue("*");
			restrictions.add(r);
		}
		query.setRestriction(restrictions);
		return query;
	}

	public static boolean isQamSwitchBackRequest(SCTE35PointDescriptorType scte35, final boolean hasReceivedTimeAlreadyPassed){
		if(scte35 == null || scte35.getSegmentationDescriptorInfo() == null || scte35.getSegmentationDescriptorInfo().get(0)== null
		   || scte35.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() == null){
			return false;
		}

		boolean isQAMSwitchBackRequest = SegmentType.valueOf(scte35.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()) == SegmentType.PROGRAM_END;
		boolean isQAMSwitchBackCausedByEarlyTermination =
				SegmentType.valueOf(scte35.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()) == SegmentType.PROGRAM_EARLY_TERMINATION && hasReceivedTimeAlreadyPassed;

		return isQAMSwitchBackRequest || isQAMSwitchBackCausedByEarlyTermination;
	}


	public static boolean handleSwitchBack(final AcquisitionPoint aqpt, final ResponseSignalType responseSignalType, final SCTE35PointDescriptorType scte35Pt,  final Map<String, String> ptsTimesBySignalId,
			final HashMap<String, ConfirmedPlacementOpportunity> decisions, final String ptsTime, MuleEventContext context,
			Map<String, I03ResponseModelDelta> alternateContents, Map<QName, String> attributes) {

		String signalId = responseSignalType.getSignalPointID();
		if(signalId == null) {
			byte[] upid = scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid();
			if(upid!= null){
				signalId = new String(upid);
				if(signalId.startsWith(ESAMHelper.UPID_PREFIX)){
					signalId = signalId.substring(signalId.indexOf(ESAMHelper.UPID_PREFIX) + ESAMHelper.UPID_PREFIX.length() );
				} else {
					signalId = null;//not our signal id.
				}
			}
		}

		DataManager dataManager = DataManagerFactory.getInstance();
		BlackoutEvent blackoutEvent = null;
		if(signalId == null){
	        SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
			ConfirmedPlacementOpportunity lastConfirmedBlackoutOnThisAP = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), cursor.getLastConfirmedBlackoutSignalId());
			if(lastConfirmedBlackoutOnThisAP!=null){
				blackoutEvent = dataManager.getSingleBlackoutEvent(lastConfirmedBlackoutOnThisAP.getSignalId());

				if(lastConfirmedBlackoutOnThisAP.isProgramEnded() || lastConfirmedBlackoutOnThisAP.isAborted()){
					if(LOGGER.isInfoEnabled()){
						LOGGER.info(()->"Last confirmed Blackout event " + lastConfirmedBlackoutOnThisAP.getSignalId() + " has been ended/aborted, returning noop/delete.");
					}
					return false;
				}
			}
		} else {
			blackoutEvent = dataManager.getSingleBlackoutEvent(signalId);
		}

		if(blackoutEvent==null) {
			if(LOGGER.isInfoEnabled()){
				LOGGER.info(()->"No Blackout event found for the given signal for switching back. Sending a Delete.");
			}
			return false;
		}

		signalId = blackoutEvent.getSignalId();

		ConfirmedPlacementOpportunity commonCPO = dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());
		if(commonCPO==null) {
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("The Blackout event is not confirmed with signal id: " + blackoutEvent.getSignalId() + " for switch back. Sending a Delete.");
			}
			return false;
		}

		if(LOGGER.isInfoEnabled()){
			LOGGER.info("Servicing switch back request for Blackout event: " + blackoutEvent.getEventId());
		}

		I03ResponseModelDelta i03ResponseModelDelta = null;
		try{
			i03ResponseModelDelta = BlackoutQAMHandler.retrieveZoneInfo(blackoutEvent, commonCPO, attributes);
		}catch(Exception e){
			LOGGER.error(()->"Error while retrieve Zone info ", e);
		}

		if (i03ResponseModelDelta == null || i03ResponseModelDelta.getAlternateContentIDList().isEmpty()) {
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("No restrications found for blackout " +  blackoutEvent.getEventId() + "("+blackoutEvent.getSignalId()+") on QAM AP " + responseSignalType.getAcquisitionPointIdentity() + " while handling QAM switch back.");
			}
			return false;
		}

		// set alter content identity to "" for switch back
		for (AlternateContentTypeModel alternate : i03ResponseModelDelta.getAlternateContentIDList()) {
			alternate.setAltContentIdentity("");
		}

		//On this QAM acquisition point Program has already ended, then un-schedule any job on this.(It's only FailSafe/They never send Program End on their own)
		EventRuntimeNotificationsHandler.cancelUpStreamQamSwitchBackJob(blackoutEvent, aqpt);//TODO YAMINEE
		alternateContents.put(signalId, i03ResponseModelDelta);

		context.getMessage().setProperty("blackoutEvent", blackoutEvent, PropertyScope.OUTBOUND);

		ptsTimesBySignalId.put(signalId, ptsTime);
		responseSignalType.setSignalPointID(signalId);

		return true;
	}
}
