package tv.blackarrow.cpp.components;

import static tv.blackarrow.cpp.utils.SpliceCommandType.SPLICE_INSERT;
import static tv.blackarrow.cpp.utils.SpliceCommandType.TIME_SIGNAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.exeptions.ResourceNotFoundException;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventRuntimeNotificationsHandler;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.BlackoutSCCResponseHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class BlackOutDecisionAiringIDComponent extends BlackoutDecisionBase implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(BlackOutDecisionAiringIDComponent.class);

	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext context) throws Exception {


		final HashMap<String,String> ptsTimes = (HashMap<String,String>) context.getMessage().getProperty("ptsTimes", PropertyScope.OUTBOUND);

		// setup Mule context message for all decisions
		HashMap<String,ConfirmedPlacementOpportunity> decisions = new HashMap<String,ConfirmedPlacementOpportunity>();
		Map<String,String> ptsTimesBySignalId = new HashMap<String,String>();
		Boolean hasBlackoutEndTimeChanged = Boolean.FALSE;

		//Alternate Content for QAM acquisition points
		Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = new HashMap<>();

		SignalProcessingNotificationType notification = context.getMessage().getProperty("signal_response", PropertyScope.OUTBOUND);
		HashMap<String,Map<QName, String>> attributes = (HashMap<String,Map<QName, String>>)context.getMessage().getProperty("acquisition_attributes", PropertyScope.OUTBOUND);
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context,notification);

		// retrieve AcquisitionPoint object
		// for all the configuration parameters
    	DataManager dataManager = DataManagerFactory.getInstance();
    	AcquisitionPoint aqpt = null;

		if (notification != null) {
	    	aqpt = (AcquisitionPoint) context.getMessage().getProperty(CppConstants.ACQUISITION_POINT,PropertyScope.OUTBOUND);
	    	if (aqpt == null) {
	    		String msg = "Unknown acquisition point identity : " + notification.getAcquisitionPointIdentity();
	    		LOGGER.warn(()->msg);
	    		context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
	    		return "";
	    	}
	    	//if requested Signal id is not for runover check if outband word as previously other else skip this
	    	Short segmentTypeId = context.getMessage().getProperty("segmentTypeId", PropertyScope.OUTBOUND);
	    	if(!SegmentType.isSignalApplicableForBothInbandAndOutOfBandBlackouts(segmentTypeId)){
	    		if(!aqpt.isInBand()){
	    			String msg = "Invalid SCC message, the acquisition point is configured not as 'in-band'";
	    			LOGGER.warn(msg + ": " + aqpt.getAcquisitionPointIdentity());
	    			context.getMessage().setProperty(CppConstants.INVALID_SCC_REQUEST, new Integer(4), PropertyScope.OUTBOUND);
	    			return msg;
	    		}
	    	}

			List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
			if (responseSignalTypes != null && responseSignalTypes.size() > 0) {
				for (ResponseSignalType responseSignalType : responseSignalTypes) {

					SCTE35PointDescriptorType scte35Pt = responseSignalType.getSCTE35PointDescriptor();

					// use segementTypeId for combined request signals (PROGRAM_START/PROGRAM_END)
					SegmentationDescriptorType segmentationDescriptorType = scte35Pt.getSegmentationDescriptorInfo().get(0);
					segmentTypeId = segmentationDescriptorType.getSegmentTypeId();

					// ignore splice insert with outOfNetworkIndicator to be false
					if (scte35Pt != null && scte35Pt.getSpliceCommandType() == SPLICE_INSERT.getCommandtype() && !scte35Pt.getSpliceInsert().isOutOfNetworkIndicator()) {
						// is splice insert case and outOfNetworkIndicator is false
						LOGGER.info(()->"Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() +
								",acquisitionSignalID=" + responseSignalType.getAcquisitionSignalID() + ") because it is a splice insert with outOfNetworkIndicator false.");
						continue;
					} else if (scte35Pt != null && scte35Pt.getSpliceInsert() != null &&
							scte35Pt.getSpliceInsert().isSpliceEventCancelIndicator() != null && scte35Pt.getSpliceInsert().isSpliceEventCancelIndicator() == true) {
						// splice cancel indicator is set and we don't support it. So we skip it
						LOGGER.info(()->"Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() +
								",acquisitionSignalID=" + responseSignalType.getAcquisitionSignalID() + ") because it is has unsupported spliceEventIndicator is set.");
						continue;
					} else if (scte35Pt != null && (scte35Pt.getSpliceCommandType() < SPLICE_INSERT.getCommandtype() || scte35Pt.getSpliceCommandType() > TIME_SIGNAL.getCommandtype())) {
						// unsupported spliceCommand type. So we skip it
						LOGGER.info(()->"Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() +
								",acquisitionSignalID=" + responseSignalType.getAcquisitionSignalID() + ") because it is has unsupported Splice Command type: "+
								scte35Pt.getSpliceCommandType()+"in the request.");
						continue;
					} else if (scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null &&  !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
						boolean toContinue = false;
						for (SegmentationDescriptorType seg : scte35Pt.getSegmentationDescriptorInfo()) {
							if (seg.getSegmentTypeId()!= null && !SegmentType.isValidBlackoutSignal(segmentTypeId)) {
								// unsupported Segmenation_type_id. So we skip it.
								LOGGER.info(()->"Blackout SCC requires segmentTypeId of 16/17/18/21/22. Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() +
								",acquisitionSignalID=" + responseSignalType.getAcquisitionSignalID() + ") because it is has unsupported Segmenation_type_id: "+
										seg.getSegmentTypeId()+" in the request.");
								toContinue = true;
								continue;
							}
						}
						if (toContinue) {
							continue;
						}
					}

					final String acqPointId = responseSignalType.getAcquisitionPointIdentity();
					if (responseSignalType.getUTCPoint() == null || responseSignalType.getUTCPoint().getUtcPoint() == null ||
							responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar() == null) {
						LOGGER.error(()->"Passed in UTCPoint: "+responseSignalType.getUTCPoint()+" is invalid");
						context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
						return "";
					}
					Long eventUTCTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime().getTime();
					final long currentSystemTime = System.currentTimeMillis();
					// If the UTC signal Time has passed the current system time The program will be started at the current Time.
					if(currentSystemTime > eventUTCTime) {
						eventUTCTime = currentSystemTime;
					}
					LOGGER.info("Request properties. Acquision Point Identity=" + acqPointId + ", eventTime:" + eventUTCTime + ", currentSystemTime" + currentSystemTime);

					SegmentationDescriptorType segDescType = null;
					if (scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null &&  !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
						segDescType = segmentationDescriptorType;
						boolean isAbort = segDescType.isSegmentationEventCancelIndicator() != null && segDescType.isSegmentationEventCancelIndicator();
						//FOR PH-200 removing PROGRAM_EARLY_TERMINATION
						if(isAbort) {
							hasBlackoutEndTimeChanged = Boolean.TRUE;
							handleSignalAbort(dataManager, responseSignalType, scte35Pt, ptsTimesBySignalId, decisions, ptsTimes.get(responseSignalType.getAcquisitionPointIdentity()), context, aqpt, AltContentIdentityResponseModelDelta, attributes,eventUTCTime);
							continue;
						}

						if(aqpt.isQAMAcquisitionPoint() && BlackoutQAMHandler.isQamSwitchBackRequest(scte35Pt, eventUTCTime <= currentSystemTime)){
							boolean doFurtherProcessing = BlackoutQAMHandler.handleSwitchBack(aqpt, responseSignalType, scte35Pt, ptsTimesBySignalId, decisions,
									ptsTimes.get(responseSignalType.getAcquisitionPointIdentity()),	context, AltContentIdentityResponseModelDelta, attributes.get(responseSignalType.getAcquisitionSignalID()));
							if(!doFurtherProcessing){
								continue;
							}
						}

						if(SegmentType.valueOf(segmentTypeId) == SegmentType.CONTENT_IDENTIFICATION){
							BlackoutHandler.handleContentIdSignal(aqpt, scte35Pt, responseSignalType,decisions);
							continue;
						}
					}

					BlackoutSearcher blackoutSearcher = null;
					String eventID = null;
					ConfirmedPlacementOpportunity  acquisitionPointCPO = null;
					BlackoutEvent blackoutEvent = null;
					SignalProcessorCursor cursor = null;
					byte[] upidBinary = segmentationDescriptorType.getUpid();
					if(upidBinary!=null) {
						eventID = new HexBinaryAdapter().marshal(upidBinary);
					}
					try {
						if(!(aqpt.isFeedAllowsOpenEndedBlackouts() && aqpt.isFeedTriggerEventsByEventID())){
							LOGGER.info(()->"Either this is not a Open Ended Feed or Event Is not have been triggered by Event ID, so sending a NOOP/Delete .");
							continue;
						}
						if(SegmentType.isBlackoutRunoverSignal(segmentTypeId)) {
							LOGGER.info(()->"Runnover Planned and Runnover UnPlanned are not supported Triggering Event By Airing ID, so sending a NOOP/Delete .");
							continue;
						}
						//Handling Of Program End Signal and Early Termination
						if(SegmentType.isProgramEndSignal(segmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(segmentTypeId) ){
							cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
							String lastConfirmedBlackoutSignalId = cursor.getLastConfirmedBlackoutSignalId();
							if(StringUtils.isBlank(lastConfirmedBlackoutSignalId)){
								setResponseSignalAction (responseSignalType, aqpt);
								context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
								LOGGER.info(()->"Either there is no Blackout for the signal received so sending a NOOP/Delete .");
								continue;
							}
							boolean isInFlight = false;
							blackoutSearcher = new BlackoutSearcher(aqpt, cursor.getLastConfirmedBlackoutUTCTime(), segmentTypeId);
							String signalId = responseSignalType.getSignalPointID();
							acquisitionPointCPO = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), lastConfirmedBlackoutSignalId);

							if(acquisitionPointCPO == null){
								setResponseSignalAction (responseSignalType, aqpt);
								context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
								LOGGER.info(()->"Either there is no Blackout for the signal received so sending a NOOP/Delete .");
								continue;
							}
							if(StringUtils.isNotBlank(signalId)){
								blackoutEvent = dataManager.getSingleBlackoutEvent(signalId);
								} else {
									blackoutEvent = dataManager.getSingleBlackoutEvent(acquisitionPointCPO.getSignalId());
								}

								if(blackoutEvent==null) {
									setResponseSignalAction (responseSignalType, aqpt);
									context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
									LOGGER.info("No Blackout event found for the given signal id: " + acquisitionPointCPO.getSignalId() + ". Sending a NOOP/Delete.");
									continue;
								}

								if(acquisitionPointCPO.hasProgramEndReceived() && SegmentType.isProgramEndSignal(segmentTypeId) && acquisitionPointCPO.getActualUtcStopTime() + aqpt.getProgramStartBuffer() < eventUTCTime ){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"The blackout is already ended, so sending a NOOP/Delete to PROGRAM_END.");
									continue;
								}

								if(acquisitionPointCPO.getActualUtcStartTime() > eventUTCTime){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"The blackout has not started yet, so sending a NOOP/Delete .");
									continue;
								}

								long blackoutActualStartTime = acquisitionPointCPO!=null && acquisitionPointCPO.getActualUtcStartTime()>0?acquisitionPointCPO.getActualUtcStartTime():0;
								isInFlight = blackoutActualStartTime <= currentSystemTime && !acquisitionPointCPO.hasProgramEndReceived();
								if(!isInFlight && !SegmentType.isProgramEndSignal(segmentTypeId)){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"Either there is no Blackout for the signal received or it is not in flight so sending a NOOP/Delete .");
									continue;
								}
								if(acquisitionPointCPO !=null && acquisitionPointCPO.hasProgramEndReceived()){
									eventUTCTime = acquisitionPointCPO.getActualUtcStopTime();
								}

								if(BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) != eventUTCTime){
									hasBlackoutEndTimeChanged = Boolean.TRUE;
								}

								long breakDuration = eventUTCTime - blackoutActualStartTime;
								if(breakDuration < 0){
									breakDuration = 0;
								}
								ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
								BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) breakDuration);
								breaks.add(breakInfo);
								acquisitionPointCPO.setBreakInfos(breaks);

								// This variable will be used in the case of Early Termination Request to Check if its STOP NOW Case or Not.
								final boolean isComplete = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) == eventUTCTime;

								// Set The Actual Stop Time Same as Event UTC Time.
								acquisitionPointCPO.setActualUtcStopTime(eventUTCTime);

								if (eventUTCTime <= currentSystemTime) {
									context.getMessage().setOutboundProperty(MULE_CONTEXT_PROPERTY_IS_END_NOW, Boolean.TRUE);
									if(aqpt.isFeedAllowsOpenEndedBlackouts() && SegmentType.isProgramEndSignal(segmentTypeId)){
										EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent, EventAction.COMPLETE.name());
									} else {
										EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent,	isComplete ? EventAction.COMPLETE.name() : EventAction.STOP_NOW.name());
									}
								} else{
									EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO, blackoutEvent, EventAction.COMPLETE.name());
								}

								//Moved this line to end so that we will have right commonCPO. Below decision ending will be added only for QAMEnd Now case, This code has been moved from BlackoutQAMHandler to here.
								if (aqpt.isQAMAcquisitionPoint() && BlackoutQAMHandler.isQamSwitchBackRequest(scte35Pt, eventUTCTime <= currentSystemTime)) {
									decisions.put(blackoutEvent.getSignalId(), acquisitionPointCPO);
								}
								acquisitionPointCPO.setConsiderActualUtcStopTimeAsProgramEnd(true);

						} else {
							long firstTimeProgramConfirmationTime = -1;
							if(decisions.isEmpty()){
								blackoutSearcher = new BlackoutSearcher(aqpt,eventUTCTime,segmentTypeId,eventID);
								firstTimeProgramConfirmationTime = eventUTCTime;
							}
							else{
								// handle combined signals, in this case, the decision for PROGRAM_END is already in the decision.
								ConfirmedPlacementOpportunity cpoForProgramEnd = decisions.entrySet().iterator().next().getValue();
								// currently we do not support abort signal in the combined signal so, if the CPO is aborted that implies it was aborted earlier
								// by a single standard abort signal. So don't consider that time as the start time for the new Program Start for this request.
								long previousEndTime = cpoForProgramEnd == null || !cpoForProgramEnd.hasProgramEndReceived() || cpoForProgramEnd.isAborted() ?
										eventUTCTime : cpoForProgramEnd.getActualUtcStopTime();

								blackoutSearcher =  new BlackoutSearcher(aqpt, previousEndTime, segmentTypeId,eventID);
								firstTimeProgramConfirmationTime = previousEndTime;
							}
							if(!SegmentType.isProgramStartSignal(segmentTypeId)){
								firstTimeProgramConfirmationTime = -1;
							}
							blackoutEvent = blackoutSearcher.getBlackoutEvent();
							blackoutEvent = getBlackoutEventForQAMStartNowProgramStart(dataManager, aqpt, segmentTypeId, responseSignalType, scte35Pt, blackoutEvent);
							acquisitionPointCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), blackoutEvent.getSignalId());
							if(acquisitionPointCPO==null) {
								acquisitionPointCPO = blackoutSearcher.getBlackout(poisAuditLogVO, firstTimeProgramConfirmationTime);
							}
						}

						if(blackoutEvent != null) {
							context.getMessage().setProperty("blackoutEvent", blackoutEvent, PropertyScope.OUTBOUND);
						}

						context.getMessage().setProperty("isblackoutEventPresent", blackoutEvent != null, PropertyScope.OUTBOUND);

						if (acquisitionPointCPO != null) { //persist
							boolean qualify = isQamApQualify4Blackout(aqpt, acquisitionPointCPO, blackoutEvent, AltContentIdentityResponseModelDelta, attributes, responseSignalType, segmentTypeId);
							if(!qualify){
								if(SegmentType.isProgramStartSignal(segmentTypeId)) {
									EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent, EventAction.LINEAR_EVENT_NA.name());
								}
								continue;
							}

							// set signalTimeOffset, however, if there is already a value, do not set it.
							if (aqpt != null && acquisitionPointCPO.getSignalTimeOffset() == ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE ) {
								acquisitionPointCPO.setSignalTimeOffset(aqpt.getSignalTimeOffset());
							}

							if(acquisitionPointCPO!=null && acquisitionPointCPO.isAborted()) {//This is the case where a SCC confirmation request comes after the BO event was aborted already.
								responseSignalType.setSignalPointID(acquisitionPointCPO.getSignalId());
								setResponseSignalAction (responseSignalType, aqpt);
								LOGGER.info(()->"Blackout corresponsing to this signal was already aborted so sending a DELETE/NOOP to this request.");
							} else {//Normal SCC Confirmation.
								 if(SegmentType.isProgramStartSignal(segmentTypeId)){
									if(decisions.containsKey(acquisitionPointCPO.getSignalId())){
										responseSignalType.setSignalPointID(null);
										continue;
									}
								}
								long actualUtcStartTime = BlackoutSCCResponseHelper.getBlackoutStartTimeWithoutOffset(acquisitionPointCPO, aqpt,
											responseSignalType,segmentTypeId);
								saveActualStartTimeInAqCPO(acquisitionPointCPO, blackoutEvent, actualUtcStartTime,aqpt);
								if (acquisitionPointCPO!=null && SegmentType.isProgramStartSignal(segmentTypeId)) {
									// / ***Rule:
									// First SCC Confirmation.
									// Update the ConfirmedPlacementOpportunity object about actualUtcStartTime and Notify the hosted environment about event being CONFIRMED.
									blackoutEvent = dataManager.getSingleBlackoutEvent(acquisitionPointCPO.getSignalId());
									EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO, blackoutEvent, EventAction.CONFIRMED.toString());
								}
								poisAuditLogVO.setAltEventId(blackoutEvent.getEventId());
								AuditLogger.auditLogConfirmedBlackoutEvent(acqPointId, acquisitionPointCPO.getUtcSignalTime(), acquisitionPointCPO.getSignalId(), poisAuditLogVO);

								/**
								 * Just making sure that the time in signal process cursor is same as in CPO. Also it needs to be maintained
								 * across all APs that confirm the same AP as this time will be used for identifying BO event in abort requests.
								 **/
								if(cursor == null ){
									cursor = dataManager.getSignalProcessorCursor(acqPointId);
								}
								if (cursor != null) {
									cursor.setLastConfirmedBlackoutUTCTime(acquisitionPointCPO.getUtcSignalTime());
									cursor.setLastConfirmedBlackoutSignalId(acquisitionPointCPO.getSignalId());
									LOGGER.debug("Input: (AQPT=" + acqPointId + " UTCSignal: " + eventUTCTime + ") and returned BlackoutEvent " + acquisitionPointCPO.getSignalId());
									blackoutSearcher.updateSignalProcessorCursor(cursor);
								}
							}
						}
					} catch (ResourceNotFoundException rfe) {
						LOGGER.error("Passed in Acquisition Point: "+acqPointId+", EventTime: "+eventUTCTime+" is invalid");
						context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(rfe.getErrorCode()), PropertyScope.OUTBOUND);
					}

					if (acquisitionPointCPO != null) {
						// this is the signal id from a new decision
						String signalId = acquisitionPointCPO.getSignalId();
						ptsTimesBySignalId.put(signalId, ptsTimes.get(responseSignalType.getAcquisitionPointIdentity()));
						// link the decision to this response section by signal id


						responseSignalType.setSignalPointID(signalId);
						// also we need to link this signal id to the actual decision
						// and save them in a HashMap in Mule context for late use
						decisions.put(signalId, acquisitionPointCPO);
					} else {
			    		String msg = "No blackout event is found for this signal : " + responseSignalType.getAcquisitionSignalID();
			    		LOGGER.warn(()->msg);
					}
				} // responseSignalTypes for loop
			}
		}

		// Let's get the interface Type and pass it to the downstream flow.
    	boolean includeInPoints = true; //cDVR case as default
    	if (aqpt != null) {
			includeInPoints = CppConstants.INTERFACE_COMCAST_CDVR.equals(aqpt.getBaIntefactTypeExternalRef());
		}
    	context.getMessage().setProperty("includeInPoints", new Boolean(includeInPoints), PropertyScope.OUTBOUND);
    	context.getMessage().setProperty("acquisitionPoint", aqpt, PropertyScope.OUTBOUND);
		context.getMessage().setProperty("decisions", decisions, PropertyScope.OUTBOUND);
		context.getMessage().setProperty("ptsTimesBySignalId", ptsTimesBySignalId, PropertyScope.OUTBOUND);
		context.getMessage().setProperty(ContextConstants.I03_MODEL_DELTA, AltContentIdentityResponseModelDelta, PropertyScope.OUTBOUND);
		context.getMessage().setProperty(MULE_CONTEXT_PROPERTY_HAS_EVENT_END_TIME_CHANGED, hasBlackoutEndTimeChanged, PropertyScope.OUTBOUND);

		return "";

		}

	/**
	 * @param dataManager
	 * @param responseSignalType
	 * @param scte35Pt
	 * @param decisions
	 * @param ptsTimesBySignalId
	 * @param context
	 * @param aqpt
	 * @param string
	 * @param aqpt
	 */
	private void handleSignalAbort(final DataManager dataManager, final ResponseSignalType responseSignalType, final SCTE35PointDescriptorType scte35Pt, final Map<String, String> ptsTimesBySignalId,
				final HashMap<String, ConfirmedPlacementOpportunity> decisions, final String ptsTime, MuleEventContext context, AcquisitionPoint aqpt,
				Map<String, I03ResponseModelDelta> alternateContents, HashMap<String,Map<QName, String>> attributes,long abortUTCTime) {

		String signalId = responseSignalType.getSignalPointID();
		BlackoutEvent blackoutEvent = null;
		ConfirmedPlacementOpportunity acquisitionPointCPO = null;
		if(signalId == null) {//Identify the last confirmed BO as the blackout to be aborted.
			LOGGER.info(()->"Couldn't find the signal id in the acquired signal. Looking for last confirmed blackout to abort.");
			SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
			if(cursor==null) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.info(()->"No signal processor cursor created for AP: " + aqpt.getAcquisitionPointIdentity() + ". So deleting/skipping this abort request.");
				return;
			}
			if(StringUtils.isBlank(cursor.getLastConfirmedBlackoutSignalId())) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.info(()->"No blackout event has been confirmed yet for AP: " + aqpt.getAcquisitionPointIdentity() + ". So deleting/skipping this abort request.");
				return;
			}
			acquisitionPointCPO = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), cursor.getLastConfirmedBlackoutSignalId());
			if(acquisitionPointCPO == null) {
				setResponseSignalAction (responseSignalType, aqpt);
				LOGGER.info(()->"No blackout event has been confirmed yet for AP: " + aqpt.getAcquisitionPointIdentity() + ". So deleting/skipping this abort request.");
				return;
			}
			signalId = acquisitionPointCPO.getSignalId();
		}

		blackoutEvent = dataManager.getSingleBlackoutEvent(signalId);

		if(blackoutEvent==null) {
			responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			LOGGER.info("No Blackout event found for the given signal id: " + signalId + ". Sending a Delete.");
			return;
		}
		LOGGER.info("Servicing Signal Abort request for Blackout event: " + blackoutEvent.getEventId());

		ConfirmedPlacementOpportunity commonCPO = dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(blackoutEvent.getSignalId());

		if (commonCPO == null){ //implies this signal has not been confirmed yet.
			LOGGER.info(()->"Blackout event has not been confirmed yet. So sending a DELETE/NOOP.");
			setResponseSignalAction (responseSignalType, aqpt);
			return;
		}

		if(acquisitionPointCPO == null && commonCPO != null){
			acquisitionPointCPO = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), commonCPO.getSignalId());
			if(acquisitionPointCPO == null){
				acquisitionPointCPO = commonCPO;
			}
		}

		boolean processSignalAbort =
				aqpt.isFeedAllowsOpenEndedBlackouts() && abortUTCTime > acquisitionPointCPO.getActualUtcStartTime() &&
				(
						!acquisitionPointCPO.hasProgramEndReceived() ||
						acquisitionPointCPO.isAborted() && acquisitionPointCPO.getAbortTime() == abortUTCTime ||
						acquisitionPointCPO.hasProgramEndReceived() && acquisitionPointCPO.getActualUtcStopTime() >= abortUTCTime
				) ;

		if(!processSignalAbort){
			setResponseSignalAction (responseSignalType, aqpt);
			LOGGER.info(()->"Event time is out of Blackout Scheduled time interval and hence can not be processed so sending a NOOP/Delete.");
			return;
		}
			boolean qualify = isQamApQualify4Blackout(aqpt, acquisitionPointCPO, blackoutEvent, alternateContents, attributes, responseSignalType, null);
			if(!qualify){
				return;
			}
			//implies that this is the first abort request for this event on this AP. (we don't support multiple Abort requests per AP for a single BO).
			if(!acquisitionPointCPO.isAborted()) {
				acquisitionPointCPO.setAbortTime(commonCPO!= null && commonCPO.isAborted() ? commonCPO.getAbortTime(): abortUTCTime);
				if(aqpt.isFeedAllowsOpenEndedBlackouts()){
					acquisitionPointCPO.setActualUtcStopTime(commonCPO!= null && commonCPO.isAborted() ? commonCPO.getAbortTime(): abortUTCTime);
				}
				acquisitionPointCPO.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
				acquisitionPointCPO.setSignalTimeOffset(aqpt.getSignalTimeOffset());
				dataManager.putConfirmedBlackout(acquisitionPointCPO);
			}

			context.getMessage().setProperty("blackoutEvent", blackoutEvent, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("isAbort", Boolean.TRUE, PropertyScope.OUTBOUND);

			ptsTimesBySignalId.put(signalId, ptsTime);
			responseSignalType.setSignalPointID(signalId);
			decisions.put(signalId, acquisitionPointCPO);
		}

	}
