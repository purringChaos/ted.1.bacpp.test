package tv.blackarrow.cpp.components;

import static tv.blackarrow.cpp.utils.SpliceCommandType.SPLICE_INSERT;
import static tv.blackarrow.cpp.utils.SpliceCommandType.TIME_SIGNAL;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;
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


public class BlackOutDecisionComponent extends BlackoutDecisionBase implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(BlackOutDecisionComponent.class);

	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext context) throws Exception {

		final HashMap<String,String> ptsTimes = (HashMap<String,String>) context.getMessage().getProperty("ptsTimes", PropertyScope.OUTBOUND);

		// setup Mule context message for all decisions
		HashMap<String,ConfirmedPlacementOpportunity> decisions = new HashMap<String,ConfirmedPlacementOpportunity>();
		Map<String,String> ptsTimesBySignalId = new HashMap<String,String>();
		Boolean hasBlackoutEndTimeChanged = Boolean.FALSE;

		//Alternate Content for QAM acquisition points
		Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = new HashMap();

		SignalProcessingNotificationType notification = context.getMessage().getProperty("signal_response", PropertyScope.OUTBOUND);
		HashMap<String,Map<QName, String>> attributes = (HashMap<String,Map<QName, String>>)context.getMessage().getProperty("acquisition_attributes", PropertyScope.OUTBOUND);
		try {
		PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context,notification);

		// retrieve AcquisitionPoint object
		// for all the configuration parameters
    	DataManager dataManager = DataManagerFactory.getInstance();
    	AcquisitionPoint aqpt = null;

		if (notification != null) {
	    	aqpt = dataManager.getAcquisitionPoint(notification.getAcquisitionPointIdentity());
	    	if (aqpt == null) {
	    		String msg = "Unknown acquisition point identity : " + notification.getAcquisitionPointIdentity();
	    		LOGGER.warn(()->msg);
	    		context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
	    		return "";
	    	}
	    	context.getMessage().setProperty("acquisition_point", aqpt, PropertyScope.OUTBOUND);
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
					segmentTypeId = scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId();

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
					LOGGER.debug("Request properties. Acquision Point Identity=" + acqPointId + ", eventTime:" + eventUTCTime + ", currentSystemTime" + currentSystemTime);

					SegmentationDescriptorType segDescType = null;
					if (scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null &&  !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
						segDescType = scte35Pt.getSegmentationDescriptorInfo().get(0);
						boolean isAbort = segDescType.isSegmentationEventCancelIndicator() != null && segDescType.isSegmentationEventCancelIndicator();
						//FOR PH-200 removing PROGRAM_EARLY_TERMINATION
						if(isAbort) {
							hasBlackoutEndTimeChanged = Boolean.TRUE;
							handleSignalAbort(dataManager, responseSignalType, scte35Pt, ptsTimesBySignalId, decisions, ptsTimes.get(responseSignalType.getAcquisitionPointIdentity()), context, aqpt, AltContentIdentityResponseModelDelta, attributes);
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
					ConfirmedPlacementOpportunity  acquisitionPointCPO = null;
					BlackoutEvent blackoutEvent = null;
					SignalProcessorCursor cursor = null;

						//Handling new Blackout Signals.
						//FOR PH-200 added PROGRAM_EARLY_TERMINATION check
						if(SegmentType.isBlackoutRunoverSignal(segmentTypeId) || SegmentType.isProgramEndSignal(segmentTypeId) ||
								SegmentType.isProgramEarlyTerminationSignal(segmentTypeId) ){
							cursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
							blackoutSearcher = new BlackoutSearcher(aqpt, cursor.getLastConfirmedBlackoutUTCTime(), segmentTypeId);
							String signalId = responseSignalType.getSignalPointID();
							boolean isInFlight = false;

							//Moving up Adding
							String lastConfirmedBlackoutSignalId = cursor.getLastConfirmedBlackoutSignalId();
							if(StringUtils.isBlank(lastConfirmedBlackoutSignalId)){
								setResponseSignalAction (responseSignalType, aqpt);
								context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
								LOGGER.info(()->"Either there is no Blackout for the signal received or it is not in flight so sending a NOOP/Delete .");
								continue;
							}

							acquisitionPointCPO = dataManager.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), lastConfirmedBlackoutSignalId);

							if(SegmentType.isProgramEndSignal(segmentTypeId) && acquisitionPointCPO !=null && acquisitionPointCPO.hasProgramEndReceived() &&
									acquisitionPointCPO.getActualUtcStopTime() - aqpt.getProgramStartBuffer() < eventUTCTime &&
									eventUTCTime < acquisitionPointCPO.getActualUtcStopTime() + aqpt.getProgramStartBuffer()){
								eventUTCTime = acquisitionPointCPO.getActualUtcStopTime();
							}

							//End
							if(StringUtils.isNotBlank(signalId)){
								blackoutEvent = dataManager.getSingleBlackoutEvent(signalId);
								if(acquisitionPointCPO==null || blackoutEvent == null){
									isInFlight = false;
									context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
								} else {
									long blackoutStartTime = acquisitionPointCPO.getActualUtcStartTime() > 0 ? acquisitionPointCPO.getActualUtcStartTime() : blackoutEvent.getUtcStartTime();
									long blackoutEndTime = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent);
									//Fixed isInFlight case added end time check
									isInFlight = blackoutStartTime <= currentSystemTime &&
											(aqpt.isFeedAllowsOpenEndedBlackouts() && !acquisitionPointCPO.isProgramEnded() ||
											!aqpt.isFeedAllowsOpenEndedBlackouts() && currentSystemTime <= blackoutEndTime);
								}//Program end can come after the actual program end as well may be say from a different AP.
								if(!isInFlight && !SegmentType.isProgramEndSignal(segmentTypeId)){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"Either there is no Blackout for the signal received or it is not in flight so sending a NOOP/Delete .");
									continue;
								}
								long blackoutActualStart = BlackoutEvent.getActualBlackoutStartTime(acquisitionPointCPO, blackoutEvent);
								long blackoutEndAllowedBoundary = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getProgramStartBuffer() + aqpt.getSignalTimeOffset();
								if( eventUTCTime < blackoutActualStart ||
										//For non open ended / regular one.
										!aqpt.isFeedAllowsOpenEndedBlackouts() &&
												eventUTCTime > blackoutEndAllowedBoundary||
										//For open ended
										aqpt.isFeedAllowsOpenEndedBlackouts() && acquisitionPointCPO != null &&
											(acquisitionPointCPO.isConsiderActualUtcStopTimeAsProgramEnd() &&
														eventUTCTime > blackoutEndAllowedBoundary ||
											acquisitionPointCPO.isAborted() && eventUTCTime > acquisitionPointCPO.getAbortTime() + aqpt.getProgramStartBuffer()
										)
								){
									setResponseSignalAction (responseSignalType, aqpt);

									//below conditions are from the if above, we are gonna log them to understand exact reason.
									boolean cond1 = eventUTCTime < BlackoutEvent.getActualBlackoutStartTime(acquisitionPointCPO, blackoutEvent);
									boolean cond2 = !aqpt.isFeedAllowsOpenEndedBlackouts()
											&& eventUTCTime > BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getProgramStartBuffer() + aqpt.getSignalTimeOffset();
									boolean cond3 = aqpt.isFeedAllowsOpenEndedBlackouts() && acquisitionPointCPO != null &&
											(acquisitionPointCPO.isConsiderActualUtcStopTimeAsProgramEnd() &&
													eventUTCTime > BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getProgramStartBuffer() ||
											acquisitionPointCPO.isAborted() && eventUTCTime > acquisitionPointCPO.getAbortTime() + aqpt.getProgramStartBuffer()
										);
									final String logMessage = eventUTCTime + " "
											+ (cond1 ? " Event time is smaller than Blackout Actual Start = " + blackoutActualStart
													: (cond2 ? " Event time is greater than Blackout End Boundary=" + blackoutEndAllowedBoundary
															: (cond3 ? " Open ended Blackout: event time is greater than Blackout End Boundary= " + blackoutEndAllowedBoundary
																	+ " or is already aborted?(" + acquisitionPointCPO.isAborted() + ")" : "")));
									LOGGER.info(() -> "Event time is out of Blackout Scheduled time interval and hence can not be processed so sending a NOOP/Delete." + " Reason: eventUTCTime="
											+ logMessage);

									continue;
								}
							} else {
								if(acquisitionPointCPO == null){
									setResponseSignalAction (responseSignalType, aqpt);
									context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
									LOGGER.info(()->"Either there is no Blackout for the signal received or it is not in flight so sending a NOOP/Delete .");
									continue;
								}
								blackoutEvent = dataManager.getSingleBlackoutEvent(acquisitionPointCPO.getSignalId());
								if(blackoutEvent==null) {
									setResponseSignalAction (responseSignalType, aqpt);
									context.getMessage().setProperty("isblackoutEventPresent", false, PropertyScope.OUTBOUND);
									LOGGER.info("No Blackout event found for the given signal id: " + acquisitionPointCPO.getSignalId() + ". Sending a NOOP/Delete.");
									continue;
								}
								//Check the program boundaries.
								if( eventUTCTime < BlackoutEvent.getActualBlackoutStartTime(acquisitionPointCPO, blackoutEvent) ||
									//For non open ended / regular one.
									!aqpt.isFeedAllowsOpenEndedBlackouts() &&
											eventUTCTime > BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getProgramStartBuffer() + aqpt.getSignalTimeOffset() ||
									//For open ended
									aqpt.isFeedAllowsOpenEndedBlackouts() && acquisitionPointCPO != null &&
										(acquisitionPointCPO.isConsiderActualUtcStopTimeAsProgramEnd() &&
												eventUTCTime > BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getProgramStartBuffer() + aqpt.getSignalTimeOffset()||
										acquisitionPointCPO.isAborted() && eventUTCTime > acquisitionPointCPO.getAbortTime() + aqpt.getProgramStartBuffer()
									)
								){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"Event time is out of Blackout Scheduled time interval and hence can not be processed so sending a NOOP/Delete.");
									continue;
								}

								long blackoutStartTime = acquisitionPointCPO.getActualUtcStartTime() > 0 ? acquisitionPointCPO.getActualUtcStartTime() : blackoutEvent.getUtcStartTime();
								long blackoutEndTime =  BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent);

								//Fixed isInFlight case added end time check
								isInFlight = blackoutStartTime <= currentSystemTime &&
										(aqpt.isFeedAllowsOpenEndedBlackouts() && !acquisitionPointCPO.isProgramEnded() ||
										!aqpt.isFeedAllowsOpenEndedBlackouts() && currentSystemTime <= blackoutEndTime);
								//Program end can come after the actual program end as well may be say from a different AP.
								if(!isInFlight && !SegmentType.isProgramEndSignal(segmentTypeId)){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"Either there is no Blackout for the signal received or it is not in flight so sending a NOOP/Delete .");
									continue;
								}

								if(acquisitionPointCPO.isProgramEnded() && SegmentType.isProgramEndSignal(segmentTypeId)
										&& acquisitionPointCPO.getActualUtcStopTime() + aqpt.getProgramStartBuffer() + aqpt.getSignalTimeOffset() < eventUTCTime ){
									setResponseSignalAction (responseSignalType, aqpt);
									LOGGER.info(()->"The blackout is already ended, so sending a NOOP/Delete to PROGRAM_END.");
									continue;
								}
							}
						} else {
							long firstTimeProgramConfirmationTime = -1;
							if(decisions.isEmpty()){
								blackoutSearcher = new BlackoutSearcher(aqpt, eventUTCTime, segmentTypeId);
								firstTimeProgramConfirmationTime = eventUTCTime;
							}
							else{
								// handle combined signals, in this case, the decision for PROGRAM_END is already in the decision.
								ConfirmedPlacementOpportunity cpoForProgramEnd = decisions.entrySet().iterator().next().getValue();
								// currently we do not support abort signal in the combined signal so, if the CPO is aborted that implies it was aborted earlier
								// by a single standard abort signal. So don't consider that time as the start time for the new Program Start for this request.
								long previousEndTime = cpoForProgramEnd == null || !cpoForProgramEnd.hasProgramEndReceived() || cpoForProgramEnd.isAborted() ?
										eventUTCTime : cpoForProgramEnd.getActualUtcStopTime();

								blackoutSearcher =  new BlackoutSearcher(aqpt, previousEndTime, segmentTypeId);
								firstTimeProgramConfirmationTime = previousEndTime;
							}
							if(!SegmentType.isProgramStartSignal(segmentTypeId)){
								firstTimeProgramConfirmationTime = -1;
							}
							blackoutEvent = blackoutSearcher.getBlackoutEvent();
							blackoutEvent = getBlackoutEventForQAMStartNowProgramStart(dataManager, aqpt, segmentTypeId, responseSignalType, scte35Pt, blackoutEvent);
							
							if(blackoutEvent!=null && StringUtils.isNotBlank(blackoutEvent.getSignalId())) {
								acquisitionPointCPO = DATA_MANAGER.getConfirmedBlackoutForGivenAP(aqpt.getAcquisitionPointIdentity(), blackoutEvent.getSignalId());
							}
							if(acquisitionPointCPO==null) {
								acquisitionPointCPO = blackoutSearcher.getBlackout(poisAuditLogVO, firstTimeProgramConfirmationTime);
							}		
							if(acquisitionPointCPO==null) {//THis code is really bad, we should not do it. Keeping it here for old unknown flows
								acquisitionPointCPO = getCPOForQAMStartNowProgramStart(dataManager, aqpt, segmentTypeId, blackoutEvent, acquisitionPointCPO);
							}
						}


						if(blackoutEvent != null) {
							context.getMessage().setProperty("blackoutEvent", blackoutEvent, PropertyScope.OUTBOUND);
						}

						context.getMessage().setProperty("isblackoutEventPresent", blackoutEvent != null, PropertyScope.OUTBOUND);

						populateEventId(poisAuditLogVO, blackoutEvent);
						if (acquisitionPointCPO != null) { //persist

							boolean qualify = isQamApQualify4Blackout(aqpt, acquisitionPointCPO, blackoutEvent, AltContentIdentityResponseModelDelta, attributes, responseSignalType, segmentTypeId);
							if(!qualify){
								if(SegmentType.isProgramStartSignal(segmentTypeId)) {
									acquisitionPointCPO.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
									EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO, blackoutEvent, EventAction.LINEAR_EVENT_NA.name());
								}
								continue;
							}

							// set signalTimeOffset, however, if there is already a value, do not set it.
							if (aqpt != null && acquisitionPointCPO.getSignalTimeOffset() == ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE ) {
								acquisitionPointCPO.setSignalTimeOffset(aqpt.getSignalTimeOffset());
							}

							SegmentationDescriptorType segmentationDescriptorType = scte35Pt.getSegmentationDescriptorInfo().get(0);

							if(acquisitionPointCPO!=null && acquisitionPointCPO.isAborted()) {//This is the case where a SCC confirmation request comes after the BO event was aborted already.
								responseSignalType.setSignalPointID(acquisitionPointCPO.getSignalId());
								setResponseSignalAction (responseSignalType, aqpt);
								LOGGER.info(()->"Blackout corresponsing to this signal was already aborted so sending a DELETE/NOOP to this request.");
							} else {//Normal SCC Confirmation.

								//update stop time if Runover_Planned
								if (SegmentType.isBlackoutRunoverSignal(segmentationDescriptorType.getSegmentTypeId())) {

									//Calculate current blackout event's newly proposed end time.
									Long duration = 0l;
									String segDurationFlag = segmentationDescriptorType.getOtherAttributes().get(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
									if(segDurationFlag ==null || segDurationFlag!=null && segDurationFlag.equals("1")){
										duration = calculateDurationInMilliSeconds(segmentationDescriptorType.getDuration());
									}
									long newProposedEndTimeForCurrentEvent = eventUTCTime + duration;
									if(newProposedEndTimeForCurrentEvent < currentSystemTime){
										newProposedEndTimeForCurrentEvent = currentSystemTime;
									}

									ArrayList<BlackoutEvent> blackoutEvents = blackoutSearcher.getEventList();
									if(blackoutEvents == null){
										blackoutEvents = dataManager.getAllBlackoutEventsOnFeed(aqpt.getFeedExternalRef());
									}

									//verify that this is not overlapping with other events on the same feed as a result of this signal processing.
									if(isEndTimeUpdateCausingAnOverlap(dataManager, blackoutEvent, acquisitionPointCPO, blackoutEvents, segmentationDescriptorType,
											responseSignalType, currentSystemTime, newProposedEndTimeForCurrentEvent)){
										setResponseSignalAction (responseSignalType, aqpt);
										LOGGER.info(()->"Extending Blackout with the given duration makes it overlap with another blackout in the system so can not process this signal. Sending a NOOP/delete.");
										continue;
									}
									if(BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) != newProposedEndTimeForCurrentEvent){
										hasBlackoutEndTimeChanged = Boolean.TRUE;
									}

									acquisitionPointCPO.setActualUtcStopTime(newProposedEndTimeForCurrentEvent);
									//FOR PH-200
									long breakActualStartTime = 0;
									breakActualStartTime = acquisitionPointCPO!=null && acquisitionPointCPO.getActualUtcStartTime()>0?acquisitionPointCPO.getActualUtcStartTime():blackoutEvent.getUtcStartTime();
									long breakDuration = newProposedEndTimeForCurrentEvent - breakActualStartTime;
									if(breakDuration < 0){
										breakDuration = 0;
									}
									ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
									BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) breakDuration);
									breaks.add(breakInfo);
									acquisitionPointCPO.setBreakInfos(breaks);

									if (newProposedEndTimeForCurrentEvent <= currentSystemTime) {
										acquisitionPointCPO.setConsiderActualUtcStopTimeAsProgramEnd(true);
										context.getMessage().setOutboundProperty(MULE_CONTEXT_PROPERTY_IS_END_NOW, Boolean.TRUE);
										EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent, EventAction.STOP_NOW.name());
									} else {
										EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent, EventAction.UPDATE.name());
									}
									populateEventId(poisAuditLogVO, blackoutEvent);
									AuditLogger.auditLogConfirmedBlackoutEvent(acqPointId, acquisitionPointCPO.getActualUtcStartTime(), acquisitionPointCPO.getSignalId(), poisAuditLogVO);
								}
								else if(SegmentType.isProgramEndSignal(segmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(segmentTypeId)){ //FOR PH-200 added PROGRAM_EARLY_TERMINATION check

									long newProposedEndTimeForCurrentEvent = eventUTCTime;
									//Program End can come multiple times and from multiple APs too so even if the program is ended entertain the new Program End requests, just send back the original
									// program end time.
									if(newProposedEndTimeForCurrentEvent < currentSystemTime){
										if(acquisitionPointCPO !=null && acquisitionPointCPO.isProgramEnded()){
											newProposedEndTimeForCurrentEvent = acquisitionPointCPO.getActualUtcStopTime();
										}
										//For Non Open Ended Blackout, if it is already ended.
										else if(!aqpt.isFeedAllowsOpenEndedBlackouts() && blackoutEvent!= null &&
												BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) < currentSystemTime){
											newProposedEndTimeForCurrentEvent = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent);
										} else {
											newProposedEndTimeForCurrentEvent = currentSystemTime;
										}
									}

									//Calculate current blackout event's current actual end time.
									boolean isProposedEndTimeAppropriate = verifyIfBOProsposedEndTimeIsAppropriate(dataManager, acquisitionPointCPO,blackoutEvent, aqpt,
											newProposedEndTimeForCurrentEvent, responseSignalType,segmentTypeId);
									if(!isProposedEndTimeAppropriate){
										continue;
									}
									ArrayList<BlackoutEvent> blackoutEvents = blackoutSearcher.getEventList();
									if(blackoutEvents == null){
										blackoutEvents = dataManager.getAllBlackoutEventsOnFeed(aqpt.getFeedExternalRef());
									}

									//No Overlap case exists for program end and early termination as these signals can only shorten a blackout and can't lengthen them so removed the
									//overlap check conditions from this place which was there similar to the runover signals.
									if(BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) != newProposedEndTimeForCurrentEvent){
										hasBlackoutEndTimeChanged = Boolean.TRUE;
									}

									final boolean isComplete = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) <= newProposedEndTimeForCurrentEvent;
									//@Sumit We can Set the ActualStopTime for the case of END NOW. Code
									//@Yaminee(PRI-12249): In Pycelle.1, while rewriting the new notification API. All the notifications that were sent from Cadent system already has signalTimeOffset added.
									//@Yaminee: In ned.1, it was not consistent, during start we were adding offset but not during end. Vice versa, start SPE, we were not adding, but End SPE we were adding. In Pycelle.1 this behavior is stream lined
									//after internal discussion of consistency(Dev/QA were consult).
									//@Yaminee: QAM Switchback SPE, do not add the offset again. (Since we sent it already during notification)
									if (aqpt.isQAMAcquisitionPoint() && aqpt.isOutBand() && SegmentType.isProgramEndSignal(segmentTypeId)) {//It's Switch back SPE.
										//Then any inband coming Program End SPE is Switch back, (That already had the offset added when we notified to transcoder, we do not need to add offset again)
										//As per Charter's encoder usecase, they send the same UTC back that we sent them.That's why we should remove the added signal time offset while setting in ActualStopTime.
										acquisitionPointCPO.setActualUtcStopTime(newProposedEndTimeForCurrentEvent - aqpt.getSignalTimeOffset());
									} else {
										acquisitionPointCPO.setActualUtcStopTime(newProposedEndTimeForCurrentEvent);
									}

									//FOR PH-200
									long blackoutActualStartTime = 0;
									blackoutActualStartTime = acquisitionPointCPO!=null && acquisitionPointCPO.getActualUtcStartTime()>0?acquisitionPointCPO.getActualUtcStartTime():blackoutEvent.getUtcStartTime();
									long breakDuration = newProposedEndTimeForCurrentEvent - blackoutActualStartTime;
									if(breakDuration < 0){
										breakDuration = 0;
									}
									ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
									BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) breakDuration);
									breaks.add(breakInfo);
									acquisitionPointCPO.setBreakInfos(breaks);

									if (newProposedEndTimeForCurrentEvent <= currentSystemTime) {
										context.getMessage().setOutboundProperty(MULE_CONTEXT_PROPERTY_IS_END_NOW, Boolean.TRUE);
										if(aqpt.isFeedAllowsOpenEndedBlackouts() && SegmentType.isProgramEndSignal(segmentTypeId)){
											EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent, EventAction.COMPLETE.name());
										} else {
											EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO,blackoutEvent,	isComplete ? EventAction.COMPLETE.name() :
												EventAction.STOP_NOW.name());
										}
									} else{
										EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO, blackoutEvent, EventAction.COMPLETE.name());
									}

									//Moved this line to end so that we will have right commonCPO. Below decision ending will be added only for QAMEnd Now case, This code has been moved from BlackoutQAMHandler to here.
									if (aqpt.isQAMAcquisitionPoint() && BlackoutQAMHandler.isQamSwitchBackRequest(scte35Pt, newProposedEndTimeForCurrentEvent <= currentSystemTime)) {
										decisions.put(blackoutEvent.getSignalId(), acquisitionPointCPO);
									}
									acquisitionPointCPO.setConsiderActualUtcStopTimeAsProgramEnd(true);
									populateEventId(poisAuditLogVO, blackoutEvent);
									if(isComplete){
										AuditLogger.auditLogCompleteBlackoutEvent(acqPointId, acquisitionPointCPO.getActualUtcStopTime(), acquisitionPointCPO.getSignalId(), poisAuditLogVO);
									} else{
										AuditLogger.auditLogConfirmedBlackoutEvent(acqPointId, acquisitionPointCPO.getActualUtcStartTime(), acquisitionPointCPO.getSignalId(), poisAuditLogVO);
									}
								// for combined signal (PROGRAM_START and PROGRAM_END), if cpo are same for both, then we shouldn't confirm program_start
								}else if(SegmentType.isProgramStartSignal(segmentTypeId)){
									if(decisions.containsKey(acquisitionPointCPO.getSignalId())){
										responseSignalType.setSignalPointID(null);
										continue;
									}
									populateEventId(poisAuditLogVO, blackoutEvent);
									AuditLogger.auditLogConfirmedBlackoutEvent(acqPointId, acquisitionPointCPO.getActualUtcStartTime(), acquisitionPointCPO.getSignalId(), poisAuditLogVO);
								}
								long actualUtcStartTime = BlackoutSCCResponseHelper.getBlackoutStartTimeWithoutOffset(acquisitionPointCPO, aqpt,
										responseSignalType,segmentTypeId);//If it is already set in CPO, it will be again set the same value
								
								saveActualStartTimeInAqCPO(acquisitionPointCPO, blackoutEvent, actualUtcStartTime, aqpt);
								if (acquisitionPointCPO!=null && SegmentType.isProgramStartSignal(segmentTypeId)) {
									// / ***Rule:
									// First SCC Confirmation.
									// Update the ConfirmedPlacementOpportunity object about actualUtcStartTime and Notify the hosted environment about event being CONFIRMED.
									blackoutEvent = dataManager.getSingleBlackoutEvent(acquisitionPointCPO.getSignalId());
									
									EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(acquisitionPointCPO, blackoutEvent, EventAction.CONFIRMED.toString());
								}

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
			    		String msg = "No Confirmed event found for this signal : " + responseSignalType.getAcquisitionSignalID();
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
		} catch (ResourceNotFoundException rfe) {
			context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(rfe.getErrorCode()), PropertyScope.OUTBOUND);
		} catch (Exception ex) {
			try {
				LOGGER.error(ex.getMessage(), ex);
			} catch (Exception e) {
				//Very unfortunate but just in case log4j2 API bombed while logging the error.
				ex.printStackTrace();
				e.printStackTrace();
			}	
		}
		return "";
	}

	private void populateEventId(PoisAuditLogVO poisAuditLogVO, BlackoutEvent blackoutEvent) {
		if(StringUtils.isBlank(poisAuditLogVO.getAltEventId()) && blackoutEvent != null && StringUtils.isNotBlank(blackoutEvent.getEventId())){
			poisAuditLogVO.setAltEventId(blackoutEvent.getEventId());
		}
	}

	/*
	 * This function retrives the Event information from C: that holds the common/finalized information of blackout
	 */
	private boolean verifyIfBOProsposedEndTimeIsAppropriate(DataManager dataManager, ConfirmedPlacementOpportunity acquisitionPointCPO, BlackoutEvent blackoutEvent, AcquisitionPoint aqpt,
		long newProposedEndTimeForCurrentEvent, ResponseSignalType responseSignalType, Short segmentTypeId) {
		//QAM:This is the time that was sent to Transcoder in START UTC, With PRODISSUE_14389(Email attachment, document 052319_cpp.log),
		//We could notice that's its the same time sent in UTC value from Transcoder back with restriction that we notified to them during switch notification
		//That's why adding offset to it. To make sure comparison is between apple to apple
		//IP: We do not expect Program End, if it comes will get compare with the right offset added value
		long oldActualEndTimeOfCurrentEventWithAQOffset = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) + aqpt.getSignalTimeOffset();
		if (!aqpt.isFeedAllowsOpenEndedBlackouts() && newProposedEndTimeForCurrentEvent > oldActualEndTimeOfCurrentEventWithAQOffset ||
			aqpt.isFeedAllowsOpenEndedBlackouts() && acquisitionPointCPO.hasProgramEndReceived() && acquisitionPointCPO.isConsiderActualUtcStopTimeAsProgramEnd()
					&& newProposedEndTimeForCurrentEvent > oldActualEndTimeOfCurrentEventWithAQOffset) {
			setResponseSignalAction(responseSignalType, aqpt);
			if (SegmentType.isProgramEarlyTerminationSignal(segmentTypeId)) {
				LOGGER.info(()->"UTC time received for Program Early Termination is after the original end time so nothing to process here, sending a NOOP/delete.");
			} else {
				LOGGER.info(()->"UTC time received for Program End is after the original end time so nothing to process here, sending a NOOP/delete.");
			}
			return false;
		}
		return true;
	}

	private boolean isEndTimeUpdateCausingAnOverlap(final DataManager dataManager, final BlackoutEvent currentEvent, final ConfirmedPlacementOpportunity  cpoForCurrentEvent,
			final List<BlackoutEvent> allEventsOnCurrentEventsFeed, final SegmentationDescriptorType segmentationDescriptorType, final ResponseSignalType responseSignalType,
			final long currentSystemTime, final long newProposedEndTimeForCurrentEvent){

		//Note here that all the crap following this line is being done to avoid any overlap that may be caused by these signals. So first point first
		//There can be an overlap only when the end time of the current blackout is being increased, reducing the end time will not cause any overlap.
		//So let's check that.

		//Calculate current blackout event's actual start time.
		long actualStartTimeForCurrentEvent = cpoForCurrentEvent !=null && cpoForCurrentEvent.getActualUtcStartTime()>0 ? cpoForCurrentEvent.getActualUtcStartTime() :
			currentEvent.getUtcStartTime();

		//Calculate current blackout event's current actual end time.
		long oldActualEndTimeOfCurrentEvent = BlackoutEvent.getActualBlackoutStopTime(cpoForCurrentEvent, currentEvent);

		if(newProposedEndTimeForCurrentEvent > oldActualEndTimeOfCurrentEvent){
			//Now that we have both the actual start time of this event and the proposed end time for the current event let's compare this with the remaining
			//events in the list
			boolean startConsideringBOsForOverlap = false;
			//Save some time here and avoid unnecessary Couch base calls. So filter as many blackouts here as you can.
			for(BlackoutEvent otherEvent: allEventsOnCurrentEventsFeed){

				//These blackouts are sorted by their start date so any blackout coming before the current blackout can not overlap with it because that is why this blackout was
				//created with this start time and was confirmed too. So ignore all the blackout coming before this blackout blindly.

				if(!startConsideringBOsForOverlap && otherEvent.getSignalId().equalsIgnoreCase(currentEvent.getSignalId())){
					startConsideringBOsForOverlap = true;
					//Ignore itself too.
					continue;
				}
				if(!startConsideringBOsForOverlap){
					//Keep ignoring.
					continue;
				}

				// Now we just need to compare this one blackout (which is next to the current blackout for which this signal came). As comparing the end time of the blackout
				// for which this signal came with the next blackout's start time is enough to conclude whether the current blackout is overlapping with any other blackout or not.
				// That is because all the blackouts are stored in the sorted order of their start date.
				BlackoutEvent blackoutEventToCompareForOverlap = otherEvent;
				ConfirmedPlacementOpportunity cpoForBlackoutToCompareForOverlap = dataManager.getConfirmedBlackoutCommonAcrossAFeedAPs(otherEvent.getSignalId());

				//Now that we have found the blackout event for comparison lets compare it and find out whether an overlap exists.
				long actualStartTimeOfOtherEvent = cpoForBlackoutToCompareForOverlap!=null && cpoForBlackoutToCompareForOverlap.getActualUtcStartTime()>0 ?
						cpoForBlackoutToCompareForOverlap.getActualUtcStartTime(): blackoutEventToCompareForOverlap.getUtcStartTime();

				long actualEndTimeOfOtherEvent = BlackoutEvent.getActualBlackoutStopTime(cpoForBlackoutToCompareForOverlap, blackoutEventToCompareForOverlap);

				return actualStartTimeOfOtherEvent <= newProposedEndTimeForCurrentEvent && actualEndTimeOfOtherEvent > actualStartTimeForCurrentEvent;
			}
		}

		return false;
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
				Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta, HashMap<String,Map<QName, String>> attributes) {

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

		long abortUTCTime = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();

		boolean processSignalAbort =
				aqpt.isFeedAllowsOpenEndedBlackouts() && abortUTCTime > blackoutEvent.getUtcStartTime() &&
						(
								!acquisitionPointCPO.hasProgramEndReceived() ||
								acquisitionPointCPO.isAborted() && acquisitionPointCPO.getAbortTime() == abortUTCTime ||
								acquisitionPointCPO.hasProgramEndReceived() && acquisitionPointCPO.getActualUtcStopTime() >= abortUTCTime
						)
				||
				!aqpt.isFeedAllowsOpenEndedBlackouts() && abortUTCTime > blackoutEvent.getUtcStartTime() &&
						(
								!acquisitionPointCPO.hasProgramEndReceived() && !acquisitionPointCPO.isAborted() && abortUTCTime  <= BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent) ||
								acquisitionPointCPO.isAborted() && acquisitionPointCPO.getAbortTime() == abortUTCTime ||
								acquisitionPointCPO.hasProgramEndReceived() && acquisitionPointCPO.getActualUtcStopTime() >= abortUTCTime
						);

		if(!processSignalAbort){
			setResponseSignalAction (responseSignalType, aqpt);
			LOGGER.info(()->"Event time is out of Blackout Scheduled time interval and hence can not be processed so sending a NOOP/Delete.");
			return;
		}

		long actualEndTime = BlackoutEvent.getActualBlackoutStopTime(acquisitionPointCPO, blackoutEvent);


		if(System.currentTimeMillis() > actualEndTime + aqpt.getProgramStartBuffer()) { //implies blackout event is ended already.
			LOGGER.info(()->"Blackout event has ended already. So sending a DELETE/NOOP.");
			setResponseSignalAction (responseSignalType, aqpt);
		} else { //Abort this BO

			boolean qualify = isQamApQualify4Blackout(aqpt, acquisitionPointCPO, blackoutEvent, AltContentIdentityResponseModelDelta, attributes, responseSignalType, null);
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

	/**
	 * This method is used to calculate the xml.duration value in seconds
	 *
	 * @param duration
	 *            java.xml.datatype.duration
	 * @return the length in seconds
	 */
	public static Long calculateDurationInMilliSeconds(Duration duration) {
		if (duration == null) {
			return Long.valueOf(0);
		}
		Date baseDate = Calendar.getInstance().getTime();
		long baseTimeInMillis = baseDate.getTime();
		duration.addTo(baseDate);
		long baseTimePlusDurationInMillis = baseDate.getTime();
		return Long.valueOf(baseTimePlusDurationInMillis - baseTimeInMillis);
	}

}
