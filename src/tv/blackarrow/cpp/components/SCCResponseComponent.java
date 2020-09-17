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
import static tv.blackarrow.cpp.components.BlackoutDecisionBase.MULE_CONTEXT_PROPERTY_HAS_EVENT_END_TIME_CHANGED;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;
import static tv.blackarrow.cpp.utils.EventAction.SIGNAL_ABORT;
import static tv.blackarrow.cpp.utils.ResponseSignalAction.REPLACE;
import static tv.blackarrow.cpp.utils.SCCResponseUtil.getIdForCountDownQueueCheck;
import static tv.blackarrow.cpp.utils.SCCResponseUtil.getSegmentationTypeId;
import static tv.blackarrow.cpp.utils.SegmentType.CONTENT_IDENTIFICATION;
import static tv.blackarrow.cpp.utils.SegmentType.DISTRIBUTOR_ADVERTISEMENT_END;
import static tv.blackarrow.cpp.utils.SegmentType.DISTRIBUTOR_ADVERTISEMENT_START;
import static tv.blackarrow.cpp.utils.SegmentType.PLACEMENT_OPPORTUNITY_END;
import static tv.blackarrow.cpp.utils.SegmentType.PLACEMENT_OPPORTUNITY_START;
import static tv.blackarrow.cpp.utils.SegmentType.PROGRAM_START;
import static tv.blackarrow.cpp.utils.SegmentType.PROVIDER_ADVERTISEMENT_END;
import static tv.blackarrow.cpp.utils.SegmentType.PROVIDER_ADVERTISEMENT_START;
import static tv.blackarrow.cpp.utils.SpliceCommandType.SPLICE_INSERT;
import static tv.blackarrow.cpp.utils.SpliceCommandType.TIME_SIGNAL;
import static tv.blackarrow.cpp.utils.SpliceCommandType.valueOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.components.util.DataUpdateHelper;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.i02.EventRuntimeNotificationsHandler;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.BlackoutSCCResponseHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.ESAMObjectCreationHelper;
import tv.blackarrow.cpp.utils.EventAction;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.ResponseSignalAction;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;
import tv.blackarrow.cpp.utils.SpliceCommandType;

/**
 *
 * generate ESAM SCC response
 *
 */
public class SCCResponseComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(SCCResponseComponent.class);
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext ctx) throws Exception {
		// we need to come up with the notification response for all decisions
		SignalProcessingNotificationType notification = null;
		Schema schema = null;
		try {
			// we need to come up with the notification response for all decisions
		notification = (SignalProcessingNotificationType) ctx.getMessage().getOutboundProperty(SIGNAL_RESPONSE);
		String requestSchema = ctx.getMessage().getProperty("schema", PropertyScope.INVOCATION);

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug(()->"generate SCC response in schema " + requestSchema);
		}

		schema = Schema.getSchema(requestSchema);
		final HashMap<String,ConfirmedPlacementOpportunity> decisions = (HashMap<String,ConfirmedPlacementOpportunity>) ctx.getMessage().getOutboundProperty("decisions");
		final HashMap<String,String> ptsTimes = (HashMap<String,String>) ctx.getMessage().getOutboundProperty("ptsTimes");
		final HashMap<String,String> ptsAdjustments = (HashMap<String,String>) ctx.getMessage().getOutboundProperty("ptsAdjustments");
		final Boolean duplicateSignal =  (Boolean)ctx.getMessage().getOutboundProperty("duplicate_signal");
		final String blackout = ctx.getMessage().getOutboundProperty(CppConstants.INTERNAL_FLAG_ALTCONTENT_REQUEST);
		final Boolean unsupportedSCCRequestFlag = ctx.getMessage().getOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC_SIGNAL) != null ? Boolean.parseBoolean((String)ctx.getMessage().getOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC_SIGNAL)) : false;
		//By default existing functionality takes care of returning noop in this case
		final HashMap<String,Long> acquisiitionTimes = (HashMap<String,Long>) ctx.getMessage().getOutboundProperty("acquisition_times");
			final Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap = (Map<String, I03ResponseModelDelta>) ctx.getMessage()
					.getOutboundProperty(ContextConstants.I03_MODEL_DELTA);
		final Boolean isCombinedSignal = ctx.getMessage().getOutboundProperty(CppConstants.IS_COMBINED_SIGNAL, Boolean.FALSE);
		List<String> blackoutProcessingRequests = new ArrayList<String>();
		AcquisitionPoint aqpt = (AcquisitionPoint)ctx.getMessage().getProperty("acquisition_point",PropertyScope.OUTBOUND);
		// retrieve AcquisitionPoint object for all the configuration parameters
		DataManager dataManager = DataManagerFactory.getInstance();
		if(aqpt ==null){
			aqpt = dataManager.getAcquisitionPoint(notification.getAcquisitionPointIdentity());
		}		

		if (notification != null) {

			// Error handling.
			String error = (String)ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
			if(error != null){
				List<ResponseSignalType> signals = notification.getResponseSignal();
				if(signals!= null){
					for (ResponseSignalType signal : notification.getResponseSignal()) {
						signal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
					}
				}
				notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode(error));
				String response = objectToXML(notification, schema);
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
				return response;
			}


				String response  = processForSpliceInsertNoopAndDeleteConfiguration(ctx, notification, aqpt, schema);
				if(StringUtils.isNotBlank(response)){
					AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
					return response;
				}

				boolean includeInPoints = true; //cDVR case as default
				if (aqpt != null) {
					includeInPoints = CppConstants.INTERFACE_COMCAST_CDVR.equals(aqpt.getBaIntefactTypeExternalRef());
				}

				List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
				List<ResponseSignalType> additionalResponseSignalTypes = new ArrayList<ResponseSignalType>();
				List<ResponseSignalType> removeStartResponseSiganlTypeInEndSignalResponses = new ArrayList<ResponseSignalType>();
				boolean isProgramStartNEndRequest = false;
				boolean isContentIdentificationRequest = false;
				boolean isProgramStartSignalExistInResponse = false;
				boolean isProgramEndNTerminationNRunoverSignalRequest = false;
				String eventId = null;

				if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0)) {
					for (ResponseSignalType responseSignalType : responseSignalTypes) {

						SCTE35PointDescriptorType scte35 = responseSignalType.getSCTE35PointDescriptor();

						Short segmentTypeId = (scte35.getSegmentationDescriptorInfo() != null) && !scte35.getSegmentationDescriptorInfo().isEmpty() ?
								scte35.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() : null;

						isProgramStartNEndRequest = SegmentType.isProgramEndSignal(segmentTypeId) || SegmentType.isProgramStartSignal(segmentTypeId);
						isProgramEndNTerminationNRunoverSignalRequest = SegmentType.isProgramEndSignal(segmentTypeId) 
								|| SegmentType.isProgramEarlyTerminationSignal(segmentTypeId)
								|| SegmentType.isBlackoutRunoverSignal(segmentTypeId);
						Integer invalidReq = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.INVALID_SCC_REQUEST);
						Integer invalidAcquisitionPoint = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.RESOURCE_NOT_FOUND);

						String signalId = (invalidReq == null) && (invalidAcquisitionPoint == null) ? responseSignalType.getSignalPointID() : null;
						// get the confirmed placement opportunity
						ConfirmedPlacementOpportunity cpo = null;
						if((signalId != null) && (signalId.length() > 0)) {  // confirmed signal ID
							if(decisions!=null) {
								cpo = decisions.get(signalId);
							}
						}

						// cancel response process if cpo is not confirmed.
						if(cpo == null){
							signalId = null;
						}

						long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);

						String ptsTimePlusOffsetInBinary = null;
						long ptsTimePlusOffsetInMillis = 0l;	
						boolean isCPOAborted = (cpo != null) && cpo.isAborted();
						//unsupportedSCCRequestFlag determines that the incoming SCC request was never supported by our system, in such case no need to adjust time.
						if (!unsupportedSCCRequestFlag && !validBlackoutSignalWithInvalidAttributes(scte35, segmentTypeId) && (ptsTimes != null)) {
							Map<String, String> ptsTimePlusOffsetMap = Scte35BinaryUtil.adjustAQPointSignalOffsetInPTS(ptsTimes, responseSignalType.getAcquisitionSignalID(), isCPOAborted, signalTimeOffset);
							ptsTimePlusOffsetInBinary = ptsTimePlusOffsetMap != null ? ptsTimePlusOffsetMap.get(CppConstants.PTS_TIME_PLUS_OFFSET_IN_BINARY) : ptsTimePlusOffsetInBinary;
							ptsTimePlusOffsetInMillis = ptsTimePlusOffsetMap != null ? Long.valueOf(ptsTimePlusOffsetMap.get(CppConstants.PTS_TIME_PLUS_OFFSET_IN_MILLIS)).longValue() : ptsTimePlusOffsetInMillis;
						}				

						String pts_adjustment = (ptsAdjustments == null) || ptsAdjustments.isEmpty()?  Scte35BinaryUtil.toBitString(0l, 33): ptsAdjustments.get(responseSignalType.getAcquisitionSignalID());

						UTCPointDescriptorType utcPointSignal = responseSignalType.getUTCPoint();

						//adjust the utc time with signalOffset
						responseSignalType.setUTCPoint(SCCResponseUtil.adjustUTCPoint(utcPointSignal.getUtcPoint(), signalTimeOffset));

						// confirmed signal ID and checking if runover request and BO is in flight
						if(((signalId != null) && (signalId.length() > 0) && ((duplicateSignal == null) || !duplicateSignal) && (scte35!=null))) {
							final String upidStr = ESAMHelper.generateUpidString(signalId);
							final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
							if(aqpt.isUseInbandOpportunitySignal()){
								saveSegmentTypedSegmentationDescriptor(responseSignalType.getSCTE35PointDescriptor(),dataManager,responseSignalType.getSignalPointID(),upid, SegmentType.CONTENT_IDENTIFICATION);
							}else{
							// save and persist data in couchbase.Save the upid from request if present otherwise save the generated upid
							saveSegmentationDescriptor(responseSignalType.getSCTE35PointDescriptor(),dataManager,responseSignalType.getSignalPointID(),upid);
							}
							if ((blackout != null) && blackout.equalsIgnoreCase("true")) {
								BlackoutEvent blackoutEvent = dataManager.getSingleBlackoutEvent(cpo.getSignalId());
								if(blackoutEvent == null) {
									LOGGER.debug(()->"Unable to find blackout event " + blackout);
									continue;
								}
								String acquisitionSignalId = ctx.getMessage().getOutboundProperty("acquisitionSignalId", null);
								if(SegmentType.valueOf(responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId()) == SegmentType.CONTENT_IDENTIFICATION) {
									isContentIdentificationRequest = true;
								}
								if(SegmentType.valueOf(responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId()) == SegmentType.PROGRAM_START) {
									isProgramStartSignalExistInResponse = true;
								}
								// id AlternateContent is available, then this is for QAM, no content Id signal needed.
								processBlackout(responseSignalType, aqpt, acquisitionSignalId, blackoutEvent, notification, additionalResponseSignalTypes,
										cpo, ptsTimePlusOffsetInBinary, pts_adjustment, signalTimeOffset, ctx, ptsTimePlusOffsetInMillis, aqpt.isIpAcquisitionPoint(),
										dataManager, responseToI03ResponseModelDeltaMap, isCombinedSignal);

								//program start type

								blackoutProcessingRequests.add(signalId);

								eventId = blackoutEvent.getEventId();
								// PO signal abort
							}else if (cpo.isAborted() && SegmentType.isPOAbortRequest(scte35, aqpt, responseSignalType.getSignalPointID())){
								Long acqsitionTime = System.currentTimeMillis();
								if(acquisiitionTimes != null){
									acqsitionTime = acquisiitionTimes.get(responseSignalType.getAcquisitionSignalID());
								}
								SCCPoSignalAbortHandler.processSignalAbort(notification,responseSignalType,scte35,ptsTimePlusOffsetInBinary,
										pts_adjustment,cpo,additionalResponseSignalTypes, acqsitionTime,dataManager, aqpt);
							}else if(!cpo.isAborted()){
								if (includeInPoints) {
									// Need to include in points in the notification
									processSignalWithInPoints(notification, responseSignalType, scte35, ptsTimePlusOffsetInBinary, pts_adjustment, signalTimeOffset, cpo,
											additionalResponseSignalTypes, dataManager, aqpt);
								} else {
									// default linear parity case
									processSignalLinearParity(notification,responseSignalType,scte35,ptsTimePlusOffsetInBinary,pts_adjustment,cpo,false, aqpt);
								}
							}else{
								boolean isDelete = (aqpt != null)?aqpt.isSccDeleteEmptyBreak() : false;
								String action = isDelete ? CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
								responseSignalType.setAction(action);
							}

						} else {

							boolean noOp = false;
							if ((scte35 != null) && (scte35.getSegmentationDescriptorInfo() != null) &&  !scte35.getSegmentationDescriptorInfo().isEmpty()) {
								for (SegmentationDescriptorType seg : scte35.getSegmentationDescriptorInfo()) {
									if(Boolean.valueOf(String.valueOf(seg.isSegmentationEventCancelIndicator()))){
										break;
									}
									segmentTypeId = seg.getSegmentTypeId();

									// processing placement ,provider and distributor opportunity start signals.
									if ((segmentTypeId != PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()) && (segmentTypeId != PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()) && (segmentTypeId != DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId()) &&
											(!(aqpt.isUseInbandOpportunitySignal() && seg.getSegmentTypeId() == SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId()))) {
										if ((segmentTypeId  == PLACEMENT_OPPORTUNITY_END.getSegmentTypeId()) || (segmentTypeId == PROVIDER_ADVERTISEMENT_END.getSegmentTypeId()) || (segmentTypeId == DISTRIBUTOR_ADVERTISEMENT_END.getSegmentTypeId())) {
											if(CppConstants.SCC_INPOINTS_COMCAST_P3.equals(CppConfigurationBean.getInstance().getSccInpointReponse())){
												String inPointsSignalId = "";
												byte[] upidBinary = seg.getUpid();
												if(upidBinary!=null) {
													String upidHex = new HexBinaryAdapter().marshal(upidBinary);
													inPointsSignalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
												} else {
													inPointsSignalId = responseSignalType.getSignalPointID();
												}
												SCCInpointHandler.processInPointResponse(inPointsSignalId, notification, responseSignalType, ptsTimePlusOffsetInBinary, pts_adjustment, additionalResponseSignalTypes, aqpt);
												noOp = true;
											}
											break;
										} else if (seg.getSegmentTypeId() == PROGRAM_START.getSegmentTypeId()) { // blackout duplicate signal
											noOp = false;
										} else if (seg.getSegmentTypeId() == CONTENT_IDENTIFICATION.getSegmentTypeId()) { // blackout duplicate signal
											noOp = true;
										} else {
											//Unsupported segmentation type id.
											String action = CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
											if(isCombinedSignal){
												//For combined signals we never send NOOP or Delete PR-256
												action = SegmentType.isProgramStartSignal(segmentTypeId) ? ResponseSignalAction.CREATE.toString() : ResponseSignalAction.REPLACE.toString();
												String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(responseSignalType.getSCTE35PointDescriptor(), ptsTimePlusOffsetInBinary, pts_adjustment);
												BinarySignalType binarySignal = new BinarySignalType();
												binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
												binarySignal.setSignalType("SCTE35");
												responseSignalType.setBinaryData(binarySignal);
												if(StringUtils.isBlank(responseSignalType.getSignalPointID()) && SegmentType.isProgramStartSignal(segmentTypeId)) {
													responseSignalType.setUTCPoint(ctx.getMessage().getOutboundProperty("utcPointForCombinedSignal", responseSignalType.getUTCPoint()));
												}
											}

											responseSignalType.setAction(action);
											noOp = true;
											break;
										}
									}
								}
							} else {
								if(invalidReq != null){
									noOp = true;
									String msg = "Invalid SCC message, the acquisition point is configured as 'out-of-band'";
									notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode(msg));
									responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
									//Log into message log.
									AuditLogger.auditMessage(msg + ": "+ notification.getAcquisitionPointIdentity(),
											AuditLogHelper.populateAuditLogVO(ctx, notification));
								}
							}

							Boolean isActinoSet = ctx.getMessage().getOutboundProperty("isActionSet") == null ? false: true;
							if (!noOp && !CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP.equals(responseSignalType.getAction()) && !isActinoSet) {
								// this default action comes from the configuration, if the flag is true: "delete" otherwise "noop"
								boolean isDelete = (aqpt != null)?aqpt.isSccDeleteEmptyBreak() : false;
								String action = isDelete ? CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
								if(isCombinedSignal){
									//For combined signals we never send NOOP or Delete PR-256
									action = SegmentType.isProgramStartSignal(segmentTypeId) ? ResponseSignalAction.CREATE.toString() : ResponseSignalAction.REPLACE.toString();
									String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(responseSignalType.getSCTE35PointDescriptor(), ptsTimePlusOffsetInBinary, pts_adjustment);
									BinarySignalType binarySignal = new BinarySignalType();
									binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
									binarySignal.setSignalType("SCTE35");
									responseSignalType.setBinaryData(binarySignal);
									if(StringUtils.isBlank(responseSignalType.getSignalPointID()) && SegmentType.isProgramStartSignal(segmentTypeId)) {
										responseSignalType.setUTCPoint(ctx.getMessage().getOutboundProperty("utcPointForCombinedSignal", responseSignalType.getUTCPoint()));
									}
								}
								responseSignalType.setAction(action);
							}
							if (includeInPoints && (scte35 != null) && (valueOf(scte35.getSpliceCommandType()) == SPLICE_INSERT) && (scte35.getSpliceInsert() != null)) {
								// replace SpliceInsert with TimeSignal
								convertSpliceInsertWithSegmentDescriptor(responseSignalType,scte35,ptsTimePlusOffsetInBinary, pts_adjustment, aqpt);
							}
						}


						StreamTimesType stts = responseSignalType.getStreamTimes();
						//unsupportedSCCRequestFlag determines that the incoming SCC request was never supported by our system, in such case no need to adjust time.
						if (!unsupportedSCCRequestFlag && !validBlackoutSignalWithInvalidAttributes(scte35, segmentTypeId) && (stts != null)) {
							List<StreamTimeType> sttList = stts.getStreamTime();
							for (StreamTimeType stt : sttList) {
								if (stt.getTimeType().equalsIgnoreCase("PTS")) {
									long time = Long.parseLong(stt.getTimeValue());
									time += (signalTimeOffset * 90);
									stt.setTimeValue(Long.toString(time));
								}
							}
						}

						if(responseSignalType.getBinaryData()!=null){
							responseSignalType.setSCTE35PointDescriptor(null);
						}
							if (responseSignalType.getSignalPointID() != null && aqpt.isIpAcquisitionPoint()) {
								if (isProgramEndNTerminationNRunoverSignalRequest
										|| ctx.getMessage().getOutboundProperty("isAbort") != null) {
									removeStartResponseSiganlTypeInEndSignalResponses.add(responseSignalType);
									notification.getConditioningInfo().clear();
								}
							}
					}

						if (isContentIdentificationRequest) {
							responseSignalTypes.clear();
						}
						if (removeStartResponseSiganlTypeInEndSignalResponses.size() > 0) {
							responseSignalTypes.removeAll(removeStartResponseSiganlTypeInEndSignalResponses);
							
						}
						
						responseSignalTypes.addAll(additionalResponseSignalTypes);
					}

				// add all conditioning info to the response
				// this conditioning info section is across all response signals

				if(notification.getStatusCode() == null){
					final StatusCodeType statusCode = new StatusCodeType();
					Integer errorCode = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.RESOURCE_NOT_FOUND);
					if (errorCode == null) {
						statusCode.setClassCode("0");
					} else {
						statusCode.setClassCode("1");
						statusCode.setDetailCode("1");
					}
					notification.setStatusCode(statusCode);
				}

				boolean isInbandBlackoutAbortRequest = ctx.getMessage().getOutboundProperty("isAbort") !=null;
				boolean isblackoutEventPresent = ctx.getMessage().getOutboundProperty("isblackoutEventPresent",true);

				boolean isHLSTemplateAqpt = false;
				if((aqpt != null) && (aqpt.getBaHlsInterfaceTypeExternalRef() !=null) &&  CppConstants.INTERFACE_TEMPLATE.equals(aqpt.getBaHlsInterfaceTypeExternalRef())){
					isHLSTemplateAqpt = true;
				}
				boolean isOnlyHLSStreamType = false;
				if(((notification != null) & (notification.getResponseSignal().size() > 0)) && (notification.getResponseSignal().get(0).getStreamTimes() != null)){
					isOnlyHLSStreamType = ESAMHelper.isOnlyHLSStreamType(notification.getResponseSignal().get(0).getStreamTimes().getStreamTime());
				}else{
					isOnlyHLSStreamType = true;
				}

				if(isProgramStartNEndRequest && !isInbandBlackoutAbortRequest && !isblackoutEventPresent && isHLSTemplateAqpt && isOnlyHLSStreamType){
					// Set all Noop/delete to Replace when blackout event is not presented
					LOGGER.debug("Is ProgramEnd/ProgramStart request "+isProgramStartNEndRequest+", Is abort request "+isInbandBlackoutAbortRequest+", Is blackoutEventPresent "+isblackoutEventPresent+".");
					LOGGER.debug(()->"Converting all NOOP/Delete to Replace");
					List<ResponseSignalType> responseSignal = notification.getResponseSignal();
					for (ResponseSignalType responseSignalType : responseSignal) {
						if(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE.equalsIgnoreCase(responseSignalType.getAction()) ||
								CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP.equalsIgnoreCase(responseSignalType.getAction())){
							responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_REPLACE);
						}
					}
				}

				//Change Request for this(PRODISSUE-844), remove the first signal's alternate content id for QAM AP response.
				if(isCombinedSignal && (aqpt!= null) && aqpt.isQAMAcquisitionPoint()){
					if ((responseToI03ResponseModelDeltaMap != null) && !responseToI03ResponseModelDeltaMap.isEmpty() && (responseToI03ResponseModelDeltaMap.size() > 1)) {
						List<ResponseSignalType> responseSignals = notification.getResponseSignal();
						if((responseSignals!=null) && !responseSignals.isEmpty() && (responseSignals.size() > 1)){
							if (responseToI03ResponseModelDeltaMap.containsKey(responseSignals.get(0).getSignalPointID())) {
								responseToI03ResponseModelDeltaMap.remove(responseSignals.get(0).getSignalPointID());
							}
						}
					}
				}

				//Change Request for this(PRODISSUE-845), send the same case of the zone identity as received.
				if ((aqpt != null) && aqpt.isQAMAcquisitionPoint() && (responseToI03ResponseModelDeltaMap != null) && !responseToI03ResponseModelDeltaMap.isEmpty()) {
					HashMap<String,Map<QName, String>> aquisitionAttributes = ctx.getMessage().getOutboundProperty("acquisition_attributes");
					if((aquisitionAttributes != null) && !aquisitionAttributes.isEmpty()){
						for(Map<QName, String> aquisitionAttribute : aquisitionAttributes.values()){
							String zoneIdentity = aquisitionAttribute.get(new QName(CppConstants.SERVINCE_ZONE_IDENTITY_ATTRIBUTE));
							if(StringUtils.isBlank(zoneIdentity)){
								continue;
							}
							for (I03ResponseModelDelta i03ResponseModelDelta : responseToI03ResponseModelDeltaMap.values()) {
								if (i03ResponseModelDelta != null) {
									for (AlternateContentTypeModel alternateContent : i03ResponseModelDelta.getAlternateContentIDList()) {
										if(StringUtils.isNotBlank(alternateContent.getZoneIdentity()) && zoneIdentity.equalsIgnoreCase(alternateContent.getZoneIdentity())){
											alternateContent.setZoneIdentity(zoneIdentity);
										}
									}
								}
							}
						}
					}
				}

				String resp = objectToXML(notification, schema, responseToI03ResponseModelDeltaMap);
				// NAMESPACE_HACK : ought to be removed once not needed
				Boolean hacked = (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.NAMESPACE_HACK);
				if ((hacked != null) && hacked.booleanValue()) {
					resp = resp.replace(CppConstants.NEW_SCC_NAMESPACE, CppConstants.OLD_SCC_NAMESPACE);
				}

				if(blackoutProcessingRequests!=null && !blackoutProcessingRequests.isEmpty() && !isContentIdentificationRequest && isProgramStartSignalExistInResponse) {
					dataManager.putAPConfirmedSignal(aqpt.getAcquisitionPointIdentity(), blackoutProcessingRequests.get(0), resp);
				}


				//Log into message log.
				if(eventId != null){
					AuditLogger.auditMessage(resp, AuditLogHelper.populateAuditLogVO(ctx, notification), eventId);
				} else {
					AuditLogger.auditMessage(resp, AuditLogHelper.populateAuditLogVO(ctx, notification));
				}

				LOGGER.debug("SCC Response: \n" + resp);

				// clean all head properties
				RequestContext.getEventContext().getMessage().clearProperties();
				return resp;
			
		} else {
				String errorMessage = (String) ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
				SignalProcessingNotificationType note = new SignalProcessingNotificationType();
				note.setStatusCode(SCCResponseUtil.generateErrorStatusCode(errorMessage));
				String response = objectToXML(note, schema);
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
				return response;
		}
		} catch (Exception e) {
			return logErrorAndSendErrorResponse(ctx, notification, schema, e);
		} finally {
			//Clear the cache maintained for this request if any.
			SCCMCCThreadLocalCache.clearMyCache();
		}
	}
	
	private Object logErrorAndSendErrorResponse(MuleEventContext ctx, SignalProcessingNotificationType notification, Schema schema, Exception e) {
		try {
			LOGGER.error("Unexpected error ", e);
		}catch (Exception ex) {
			// Don't trip on log4j failure here. Write to the standard error stream as the log4j seems to be not working at this point.
			e.printStackTrace();
			ex.printStackTrace();
		}
		String errMsg = e.getMessage()==null?e.toString():e.getMessage();
		if(notification == null) {
			notification = new SignalProcessingNotificationType();
		}
		notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode("Unexpected error while processing SCC request, error: "+ errMsg));
		//Log into message log.
		if(schema==null) {
			schema = Schema.i03;
		}
		String response = objectToXML(notification, schema);
		try {
			AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
		}catch (Exception ex) {
			// Don't trip on log4j failure here. Write to the standard error stream as the log4j seems to be not working at this point.
			ex.printStackTrace();
		}
		return response;
	}

	private boolean validBlackoutSignalWithInvalidAttributes(SCTE35PointDescriptorType scte35, Short segmentTypeId) {
		return SegmentType.isValidBlackoutSignal(segmentTypeId) && !isDeliveryRestricted(scte35);
	}

	private boolean isDeliveryRestricted(final SCTE35PointDescriptorType scte35PointDescriptorType) {
		SegmentationDescriptorType segmentDescType = scte35PointDescriptorType.getSegmentationDescriptorInfo().get(0);
		String deliveryNotRestrictedFlag = segmentDescType.getOtherAttributes().get(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG));
		return "false".equals(deliveryNotRestrictedFlag) || "0".equals(deliveryNotRestrictedFlag);
	}

	/**
	 * @param ctx
	 * @param notification
	 * @param aqpt
	 * @param schema
	 */
	private String processForSpliceInsertNoopAndDeleteConfiguration(
			MuleEventContext ctx,
			final SignalProcessingNotificationType notification,
			AcquisitionPoint aqpt, Schema schema) {
		if(ctx.getMessage().getOutboundProperty(SpliceCommandType.SPLICE_INSERT.name()) != null){

			//Set the status code.
			if(notification.getStatusCode() == null){
				final StatusCodeType statusCode = new StatusCodeType();
				Integer errorCode = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.RESOURCE_NOT_FOUND);
				if (errorCode == null) {
					statusCode.setClassCode("0");
				} else {
					statusCode.setClassCode("1");
					statusCode.setDetailCode("1");
				}
				notification.setStatusCode(statusCode);
			}

			List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
			if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0) && (aqpt != null)) {
				for (ResponseSignalType responseSignalType : responseSignalTypes) {
					// Set the response signal's action
					SignalHandlingConfiguration signalHandlingConfiguration =  ctx.getMessage().getOutboundProperty(SpliceCommandType.SPLICE_INSERT.name());
					if(signalHandlingConfiguration == SignalHandlingConfiguration.NOOP){
						responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
					} else if(signalHandlingConfiguration == SignalHandlingConfiguration.DELETE){
						responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
					}

					// Adjust the UTC Time.
					responseSignalType.setUTCPoint(SCCResponseUtil.adjustUTCPoint(responseSignalType.getUTCPoint().getUtcPoint(), aqpt.getSignalTimeOffset()));

					if(responseSignalType.getBinaryData()!=null){
						responseSignalType.setSCTE35PointDescriptor(null);
					}

					// Adjust the PTS time.
					StreamTimesType stts = responseSignalType.getStreamTimes();
					if (stts != null) {
						List<StreamTimeType> sttList = stts.getStreamTime();
						for (StreamTimeType stt : sttList) {
							if (stt.getTimeType().equalsIgnoreCase("PTS")) {
								long time = Long.parseLong(stt.getTimeValue());
								time += (aqpt.getSignalTimeOffset() * 90);
								stt.setTimeValue(Long.toString(time));
							}
						}
					}
				}
			}

			String resp = objectToXML(notification, schema);

			// NAMESPACE_HACK : ought to be removed once not needed
			Boolean hacked = (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.NAMESPACE_HACK);
			if ((hacked != null) && hacked.booleanValue()) {
				resp = resp.replace(CppConstants.NEW_SCC_NAMESPACE, CppConstants.OLD_SCC_NAMESPACE);
			}

			//Log into message log.
			AuditLogger.auditMessage(resp, AuditLogHelper.populateAuditLogVO(ctx, notification));

			LOGGER.debug("SCC Response: \n" + resp);
			RequestContext.getEventContext().getMessage().clearProperties();
			return resp;
		}
		return "";
	}


	private void convertSpliceInsertWithSegmentDescriptor(ResponseSignalType responseSignalType, SCTE35PointDescriptorType scte35,String ptsTime,
			String pts_adjustment, AcquisitionPoint aqpt) {
		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		// there is no upid from the request
		// and there is no PO decison made, so no signal id available
		byte[] upid = (ESAMHelper.UPID_PREFIX).getBytes();
		if(binarySignal != null) {
			generateBinaryDataWithInPoints(upid, null, binarySignal,scte35,ptsTime, pts_adjustment, aqpt);
			responseSignalType.setSCTE35PointDescriptor(null);
		}
		else {
			SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
			scte35Des.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
			scte35Des.setSpliceInsert(null);
			Long countDownCueID = getIdForCountDownQueueCheck(responseSignalType);
			// add one segmentation descriptor
			if((scte35Des.getSegmentationDescriptorInfo() != null)&& scte35Des.getSegmentationDescriptorInfo().isEmpty()){
				final SegmentationDescriptorType segment = generateSegment(upid, null, AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(),
						false,countDownCueID, (short) 0, (short) 0, (short) 9, false);
				scte35Des.getSegmentationDescriptorInfo().add(segment);
			}
		}
	}


	private void processBlackout(ResponseSignalType respSignalType, AcquisitionPoint aqpt, String acquisitionSignalId, BlackoutEvent blackoutEvent, SignalProcessingNotificationType signalNotification,
			List<ResponseSignalType> additionalResponseTypes, final ConfirmedPlacementOpportunity cpo, String ptsTimePlusOffsetInBinary, String pts_adjustment,
			final long signalTimeOffset, final MuleEventContext ctx, final long ptsTimePlusOffsetInMillis, boolean needContentId, DataManager dataManager,
			Map<String, I03ResponseModelDelta> alternateContents, final Boolean isCombinedSignal) throws DatatypeConfigurationException {

		boolean isInbandBlackoutAbortRequest = ctx.getMessage().getOutboundProperty("isAbort") != null;
		boolean isSCCConfirmationRequestForAbortedBlackout = cpo.isAborted() && !isInbandBlackoutAbortRequest;
		Short requestedSegmentTypeId = respSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId();

		final ResponseSignalAction programStartAction = isCombinedSignal && SegmentType.isProgramStartSignal(requestedSegmentTypeId) ? ResponseSignalAction.CREATE :
			ResponseSignalAction.REPLACE;
		final ResponseSignalAction contentIdAction = isCombinedSignal && SegmentType.isProgramEndSignal(requestedSegmentTypeId) ? ResponseSignalAction.REPLACE :
			(SegmentType.isProgramStartSignal(requestedSegmentTypeId) ? ResponseSignalAction.CREATE : ResponseSignalAction.REPLACE);

		final boolean isEndNow = (ctx.getMessage().getOutboundProperty(BlackOutDecisionComponent.MULE_CONTEXT_PROPERTY_IS_END_NOW) != null) &&
				(Boolean) ctx.getMessage().getOutboundProperty(BlackOutDecisionComponent.MULE_CONTEXT_PROPERTY_IS_END_NOW);

		final boolean isEndNowByProgramEndOrEarlyTermination = isEndNow &&
				(SegmentType.isProgramEndSignal(requestedSegmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId));

		final boolean isQAMSwitchBackCausedByEarlyTermination = aqpt.isQAMAcquisitionPoint() && isEndNowByProgramEndOrEarlyTermination;

		final boolean isQamBlackoutSwitchBackRequest = (SegmentType.isProgramEndSignal(requestedSegmentTypeId) || isInbandBlackoutAbortRequest ||
				isQAMSwitchBackCausedByEarlyTermination) && aqpt.isQAMAcquisitionPoint();

		/*
		 * Currently, For QAM switch back case, there was no specific requirement of what to return in case of end Now from SalesSuite UI. That's why skipping below if condition and
		 * letting it pass as if it was ended normally(making it to fall under else part)
		 * https://confluence.blackarrow-corp.com/display/PPS/TWC+QAM+supports
		 * Look in section, "Zone Identity Found:"  "Zone Identity Not Found:"
		 */
		ResponseSignalType respSignalContentIdentification=null;
		if(!isQamBlackoutSwitchBackRequest && ((blackoutEvent.getUtcStartTime() == 0) || isSCCConfirmationRequestForAbortedBlackout)) { // send NOOP response
			LOGGER.debug(()->"send noop response");
			signalNotification.getResponseSignal().get(0).setAction(isCombinedSignal ? (SegmentType.isProgramStartSignal(requestedSegmentTypeId) ? ResponseSignalAction.CREATE.toString() :
				ResponseSignalAction.REPLACE.toString()) :	CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
			signalNotification.getResponseSignal().get(0).setSCTE35PointDescriptor(null);
		} else {
			Map<QName, String> attributes = null;
			SCTE35PointDescriptorType scte35DesType = respSignalType.getSCTE35PointDescriptor();
			if((scte35DesType!=null) && !scte35DesType.getSegmentationDescriptorInfo().isEmpty()){
				attributes = scte35DesType.getSegmentationDescriptorInfo().get(0).getOtherAttributes();
				String segDurationFlag = attributes.get(new QName(CppConstants.SEGMENTATION_DURATION_FLAG));
				if((segDurationFlag!=null) && "1".equals(segDurationFlag) && (scte35DesType.getSegmentationDescriptorInfo().get(0).getDuration()==null)){
					attributes.put(new QName(CppConstants.SEGMENTATION_DURATION_FLAG), "0");
				}
			}
			final long originalRequestSignalTimeWithOffset = respSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
			Long segmentEventId = getIdForCountDownQueueCheck(respSignalType);
			final String upidStr = ESAMHelper.generateUpidString(blackoutEvent.getSignalId());
			final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
			long blackoutStartTimeWithOffset = BlackoutSCCResponseHelper.getBlackoutStartTimeWithOffset(cpo, aqpt, respSignalType,requestedSegmentTypeId);
			long blackoutStopTimeWithOffset = BlackoutSCCResponseHelper.getBlackoutEndTimeWithOffset(cpo, aqpt, blackoutEvent, respSignalType);

			//Blackout duration should always be BlackoutStopTime - blackoutEvent Start Time(Note: These both don;t have offset easy to calculate).
			//Also in Underdog decision was taken that on UI we do not show offset either for Program Start/Program End
			long contentDurationForProgramStart = blackoutStopTimeWithOffset - blackoutStartTimeWithOffset;
			long contentDurationForContentIdentification = contentDurationForProgramStart;
			if(cpo.isAborted() || isQamBlackoutSwitchBackRequest){
				// set altContent url to ""
				if((alternateContents!=null) && !alternateContents.isEmpty()){
					I03ResponseModelDelta i03ResponseModelDelta = alternateContents.get(cpo.getSignalId());
					if (i03ResponseModelDelta.getAlternateContentIDList() != null) {
						for (AlternateContentTypeModel content : i03ResponseModelDelta.getAlternateContentIDList()) {
							content.setAltContentIdentity("");
						}
					}
				}
			}

			if(cpo.isAborted() && CppConfigurationBean.getInstance().isSendCustomAbortSCCResponse()) {//For TWC BAS-25832
				contentDurationForContentIdentification = 0;
				isInbandBlackoutAbortRequest = false;// To make the response as a normal confirmation response rather than the abort response.
			}
			//Duration should be always based on Actual/Scheduled Blackout start time and end(abort) time.

			long abortTimeWithOffset = originalRequestSignalTimeWithOffset;
			if(!cpo.isAborted()) {//In all other cases we don't send the pts time in the binary. processBlackout
				ptsTimePlusOffsetInBinary = "";
			}else{
				// If requested abort time is less than the blackout start time than use Blackout start time as Abort time.
				if(cpo.isAborted() && cpo.isAbortedViaESNIOrUI()) {
					abortTimeWithOffset = cpo.getAbortTime() + signalTimeOffset;
				} else {
					abortTimeWithOffset = originalRequestSignalTimeWithOffset < blackoutStartTimeWithOffset ?
							 cpo.getAbortTime() + signalTimeOffset : //In BlackoutDecisionComponent for this condition it sets the Blackout start time in the abort time, so just add the offset to it.
							(cpo.getAbortTime() + (ptsTimePlusOffsetInMillis < 0 ? signalTimeOffset : ptsTimePlusOffsetInMillis)); //add the PTS time too.
				}
			}

            short segmentTypeId = SegmentType.PROGRAM_START.getSegmentTypeId();

			// handling QAM switch back PRODISSUE-569
            XMLGregorianCalendar startUTCTimeWithOffset = null;
            if(!isQamBlackoutSwitchBackRequest){
            	startUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(blackoutStartTimeWithOffset).getUtcPoint();
            } else{
				segmentTypeId = SegmentType.PROGRAM_END.getSegmentTypeId();
				long endTimeWithOffset = blackoutStopTimeWithOffset;
				if(cpo.isAborted()) {
					endTimeWithOffset = abortTimeWithOffset;
				} else if (cpo.isProgramEnded() && SegmentType.isProgramEndSignal(requestedSegmentTypeId)){
					endTimeWithOffset = cpo.getActualUtcStopTime() + aqpt.getSignalTimeOffset();
				}
				startUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(endTimeWithOffset).getUtcPoint();
				contentDurationForProgramStart = 0;
			}
            XMLGregorianCalendar stopUTCTimeWithOffset = SCCResponseUtil.generateUTCPoint(blackoutStopTimeWithOffset).getUtcPoint();
            if(aqpt.isIpAcquisitionPoint() && (SegmentType.valueOf(requestedSegmentTypeId) == SegmentType.CONTENT_IDENTIFICATION) && !CppConfigurationBean.getInstance().isSendTerritoryUpdateConfirmation() ){
            	final String upidStr1 = ESAMHelper.generateUpidString(cpo.getTerritoryUpdateSignalId());
    			final byte[] upid1 = new HexBinaryAdapter().unmarshal(upidStr1);
            	respSignalContentIdentification = ESAMObjectCreationHelper.createContentIndentficationRespSignal( aqpt, upid1, startUTCTimeWithOffset, stopUTCTimeWithOffset, acquisitionSignalId, respSignalType.getBinaryData() != null,
						segmentEventId, contentIdAction, ptsTimePlusOffsetInBinary, pts_adjustment, isInbandBlackoutAbortRequest, contentDurationForContentIdentification,
						attributes, requestedSegmentTypeId, cpo.isAborted(), isEndNowByProgramEndOrEarlyTermination);
            	additionalResponseTypes.add(respSignalContentIdentification);
            }
            else {


			ESAMObjectCreationHelper.setProgramStartResponseSignal(respSignalType, aqpt, upid, startUTCTimeWithOffset, stopUTCTimeWithOffset, acquisitionSignalId, programStartAction,
					segmentEventId, ptsTimePlusOffsetInBinary, pts_adjustment, isInbandBlackoutAbortRequest, contentDurationForProgramStart, segmentTypeId, attributes,
					isQamBlackoutSwitchBackRequest, requestedSegmentTypeId,isEndNowByProgramEndOrEarlyTermination);

			if(needContentId){
				//if request is for lengthning
				// need to change code
				//FOR PH-200
				if(SegmentType.isBlackoutRunoverSignal(requestedSegmentTypeId) || SegmentType.isProgramEndSignal(requestedSegmentTypeId) || SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId)){
					ArrayList<Long> contentDurationList = new ArrayList<Long>();
					respSignalContentIdentification = SCCInbandSignalBlackoutUpdateHandler.generateContentIdentificationForBlackoutEndTimeUpdate(aqpt, cpo, upid, segmentEventId, respSignalType.getBinaryData() != null,
							ptsTimePlusOffsetInBinary, pts_adjustment, contentDurationList, blackoutEvent, attributes, requestedSegmentTypeId, acquisitionSignalId, blackoutStartTimeWithOffset,
							blackoutStopTimeWithOffset, isEndNowByProgramEndOrEarlyTermination);

					//getting contentDuration after lengthning
					contentDurationForProgramStart = contentDurationList.get(0);
				} else {
					respSignalContentIdentification = ESAMObjectCreationHelper.createContentIndentficationRespSignal( aqpt, upid, startUTCTimeWithOffset, stopUTCTimeWithOffset, acquisitionSignalId, respSignalType.getBinaryData() != null,
							segmentEventId, contentIdAction, ptsTimePlusOffsetInBinary, pts_adjustment, isInbandBlackoutAbortRequest, contentDurationForContentIdentification,
							attributes, requestedSegmentTypeId, cpo.isAborted(), isEndNowByProgramEndOrEarlyTermination);
				}

				if(cpo.isAborted()) {
					//FOR PH-200 removing PROGRAM_EARLY_TERMINATION code
					modifyContentIdentificationForAbort(cpo, respSignalContentIdentification, abortTimeWithOffset, aqpt, requestedSegmentTypeId, isEndNowByProgramEndOrEarlyTermination);
				}
				else if((cpo.isProgramEnded() && SegmentType.isProgramEndSignal(requestedSegmentTypeId)) || isEndNowByProgramEndOrEarlyTermination){
					modifyContentIdentificationForAbort(cpo, respSignalContentIdentification, cpo.getActualUtcStopTime() + aqpt.getSignalTimeOffset(), aqpt,
							requestedSegmentTypeId, isEndNowByProgramEndOrEarlyTermination);
				}
				additionalResponseTypes.add(respSignalContentIdentification);
			}

			// list of ConditioningInfo of this notification
			List<ConditioningInfoType> conditioningInfoList = signalNotification.getConditioningInfo();
			ConditioningInfoType conditioningInfo = BlackoutResponseDurationHandler.getConditioningInfo(acquisitionSignalId, cpo, contentDurationForProgramStart,
					aqpt, isQamBlackoutSwitchBackRequest, requestedSegmentTypeId, isEndNowByProgramEndOrEarlyTermination);
			conditioningInfoList.add(conditioningInfo);
			
			if(cpo.isAborted()){
				// sending status message to hosted server
				BlackoutEvent event = dataManager.getSingleBlackoutEvent(cpo.getSignalId());
				// not sending status change message since it has been done
				if ((event == null) || (event.getEventAction() != null && EventAction.SIGNAL_ABORT.name().equals(event.getEventAction().name()))) {
					return;
				}
				event.setEventAction(SIGNAL_ABORT);
				dataManager.putBlackoutEvent(event);

				//Feed Reference List((B:feedId) for blackouts are important to be updated otherwise they will have wrong values in reference.
				//-----------------start------------
				ArrayList<BlackoutEvent> blackoutEvents = dataManager.getAllBlackoutEventsOnFeed(aqpt.getFeedExternalRef());
				DataUpdateHelper.updateFeedExtRefInCouchbase(dataManager, blackoutEvent, blackoutEvents);
				//-----------------end--------------
				ConfirmedPlacementOpportunity aqCPO = BlackoutDecisionBase.saveActualUtcEndInAqCPOp(aqpt, dataManager, blackoutEvent, EventAction.SIGNAL_ABORT.toString(), abortTimeWithOffset - aqpt.getSignalTimeOffset());
				EventRuntimeNotificationsHandler.notifyStatusToHostedImmediately(aqCPO, event, EventAction.SIGNAL_ABORT.toString());

				EventRuntimeNotificationsHandler.cancelUpStreamQamSwitchBackJob(blackoutEvent, aqpt);//TODO YAMINEE
				return;
			}
			reScheduleNotifications(aqpt, blackoutEvent, ctx, dataManager, requestedSegmentTypeId, isEndNow, isEndNowByProgramEndOrEarlyTermination, isQamBlackoutSwitchBackRequest);
            }
		}

		if(isCombinedSignal && SegmentType.isProgramEndSignal(requestedSegmentTypeId)) {
			// Will be used when There is no Blackout Start for Combined Signal Request.
			UTCPointDescriptorType utcPointForCombinedSignal = respSignalType.getUTCPoint();
			if(aqpt.isIpAcquisitionPoint() && (respSignalContentIdentification !=null) ) {
				utcPointForCombinedSignal = respSignalContentIdentification.getEventSchedule().getStopUTC();
			}
			ctx.getMessage().setOutboundProperty("utcPointForCombinedSignal",utcPointForCombinedSignal);
		}
	}


	private void reScheduleNotifications(AcquisitionPoint aqpt, BlackoutEvent blackoutEvent, final MuleEventContext ctx, DataManager dataManager, Short requestedSegmentTypeId, final boolean isEndNow,
			final boolean isEndNowByProgramEndOrEarlyTermination, final boolean isQamBlackoutSwitchBackRequest) {
		final boolean hasBlackoutEndTimeChanged = ctx.getMessage().getOutboundProperty(MULE_CONTEXT_PROPERTY_HAS_EVENT_END_TIME_CHANGED, Boolean.FALSE);
		if(aqpt.isFeedAllowsOpenEndedBlackouts()) {
			if(aqpt.isQAMAcquisitionPoint()) {
				// Only when the Program Early Termination signal has changed the Blackout end time in future we schedule a QAM switch back event,
				// Otherwise QAM switch back never gets scheduled on open ended feeds.
				if(isEndNow) {
				// If the blackout is ended then cancel all scheduled events as for end now BDC already sent that notification to hosted right away.
					EventRuntimeNotificationsHandler.cancelUpStreamQamSwitchBackJob(blackoutEvent, aqpt);
				} else if(!isQamBlackoutSwitchBackRequest && !isEndNowByProgramEndOrEarlyTermination && SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId) && hasBlackoutEndTimeChanged) {
					//In all other cases reschedule the QAM switch back if it was ever scheduled. And reschedule the event complete notification.
					EventRuntimeNotificationsHandler.rescheduleUpStreamQamSwitchBackJob(blackoutEvent, aqpt);
					}
			} else { // This is For IP AQ Point Handling.
				if (hasBlackoutEndTimeChanged) {
					//If the blackout end time has changed then reschedule complete event.
					EventRuntimeNotificationsHandler.rescheduleHostedErrorCheckCompleteJob(blackoutEvent, aqpt);
				}
			}
		} else {
			if(aqpt.isQAMAcquisitionPoint()) {
				//For QAM AP always adjust the switch back event if the end time has changed.
				if(isEndNow) {
					//If the blackout is ended then cancel all scheduled events as for end now BDC already sent that notification to hosted right away.
					EventRuntimeNotificationsHandler.cancelUpStreamQamSwitchBackJob(blackoutEvent, aqpt);
				} else if(!isQamBlackoutSwitchBackRequest && !isEndNowByProgramEndOrEarlyTermination && SegmentType.isProgramEarlyTerminationSignal(requestedSegmentTypeId) && hasBlackoutEndTimeChanged) {
					//If the blackout end time has changed then reschedule complete event.
					EventRuntimeNotificationsHandler.rescheduleUpStreamQamSwitchBackJob(blackoutEvent, aqpt);
				}
			} else {// This is for IP AQ Point handling.
				if(hasBlackoutEndTimeChanged) {
					//If the blackout end time has changed then reschedule complete event.
					EventRuntimeNotificationsHandler.rescheduleHostedErrorCheckCompleteJob(blackoutEvent, aqpt);
				}
			}
		}
	}

	private void modifyContentIdentificationForAbort(final ConfirmedPlacementOpportunity cpo, final ResponseSignalType respSignalContentIdentification,
			final long abortTime, final AcquisitionPoint aqpt, final Short requestedSegmentTypeId, boolean isEndNow) throws DatatypeConfigurationException {

		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

		UTCPointDescriptorType signalAbortTime = SCCResponseUtil.generateUTCPoint(abortTime);

		//Update the action to replace.
		respSignalContentIdentification.setAction(REPLACE.toString());

		//Update the UTC time to what we received in the Abort request.
		respSignalContentIdentification.setUTCPoint(signalAbortTime);

		//Update the same time in the Event schedule start and stop time.
		//Set content duration to zero.
		BlackoutResponseDurationHandler.setEventSchedule(respSignalContentIdentification.getEventSchedule(), signalAbortTime,
				signalAbortTime, datatypeFactory.newDuration(0), aqpt, requestedSegmentTypeId, cpo.isAborted() || isEndNow);
	}

	private void processSignalWithInPoints(
			SignalProcessingNotificationType notification,
			ResponseSignalType responseSignalType,
			SCTE35PointDescriptorType scte35,
			String ptsTime,
			String pts_adjustment,
			long signalTimeOffset,
			ConfirmedPlacementOpportunity cpo,
			List<ResponseSignalType> responseSignalTypesInPoints,
			DataManager dataManager,
			AcquisitionPoint aqpt) throws DatatypeConfigurationException {

		// list ResponseSignal of this notification
		//List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();

		// list of ConditioningInfo of this notification
		List<ConditioningInfoType> conditioningInfoList = notification.getConditioningInfo();

		byte[] upid = (ESAMHelper.UPID_PREFIX + cpo.getSignalId()).getBytes();
		Long segmentEventId = getIdForCountDownQueueCheck(responseSignalType);

		//long utcSignalTime = cpo.getUtcSignalTime();

		// out point section
		List <BreakInfo> breakInfos = cpo.getBreakInfos();//Already sorted breaks in asc order.
		Integer longestDuration = null;
		if ((breakInfos != null) && (breakInfos.size() > 0)) {
			longestDuration = breakInfos.get(breakInfos.size()-1).getDuration();
		}

		// out point non-binary case
		final SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
		if(scte35Des != null){
			//Handling splice insert Signal
			if(valueOf(scte35Des.getSpliceCommandType()) == SPLICE_INSERT) {
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				scte35Des.setSpliceCommandType(6);
				scte35Des.setSpliceInsert(null);

				// update the segmentation descriptor
				if((scte35Des.getSegmentationDescriptorInfo() != null)&& !scte35Des.getSegmentationDescriptorInfo().isEmpty()){
					updateSegmentationDescriptor(upid, segmentEventId, Long.valueOf(longestDuration),scte35Des,
							(aqpt!= null) && (aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD));
				} else{//add one segmentation descriptor if segmentation descriptor is not present
					final SegmentationDescriptorType segment = generateSegment(upid, Long.valueOf(longestDuration), AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false, segmentEventId, (short)0, (short)0, (short)9, false);
					scte35Des.getSegmentationDescriptorInfo().add(segment);
				}
			} else if(valueOf(scte35Des.getSpliceCommandType()) == TIME_SIGNAL) {//update the segmentation descriptor
				if(aqpt != null && aqpt.isUseInbandOpportunitySignal()){
					//IN case of viacom request, only UPID will be updated.
					updateSegmentationDescriptor(upid, scte35Des);
				}else
				updateSegmentationDescriptor(upid, segmentEventId, Long.valueOf(longestDuration),scte35Des, false);
			}
		}

		// getting segmentation type Id

		Short segmentTypeId = getSegmentationTypeId(responseSignalType.getSCTE35PointDescriptor());
		// if segmentation type Id is null then assign segmentation type Id = PLACEMENT_OPPORTUNITY_START
		if(segmentTypeId == null) {
			segmentTypeId = AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId();
		}
		// out point binarysegmentTypeId case
		boolean isBinary = false;
		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		if(binarySignal != null) {
			isBinary = true;
			generateBinaryDataWithInPoints(upid, Long.valueOf(longestDuration), binarySignal,scte35,ptsTime, pts_adjustment, aqpt);
		}

		// here comes the in points
		HashMap <String,String> conditioInfoAcquisitionPointIdMap = new HashMap <String,String>();
		String previousAcquisitionPointId = responseSignalType.getAcquisitionSignalID();

		if ((breakInfos != null) && (breakInfos.size() > 0)) {


			for (BreakInfo breakInfo : breakInfos) {

				Integer duration = breakInfo.getDuration();
				String signalId = breakInfo.getInSignalId();

				// for each BreakInfo -- break variation on duration
				// we need to generate a new ResponseSignal section
				ResponseSignalType responseSignalTypeInPoint = new ResponseSignalType();
				//responseSignalTypes.add(responseSignalTypeInPoint);
				responseSignalTypesInPoints.add(responseSignalTypeInPoint);

				responseSignalTypeInPoint.setAcquisitionPointIdentity(responseSignalType.getAcquisitionPointIdentity());
				responseSignalTypeInPoint.setAcquisitionSignalID(signalId);
				responseSignalTypeInPoint.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_CREATE);
				responseSignalTypeInPoint.setSignalPointID(signalId);

				//
				conditioInfoAcquisitionPointIdMap.put(signalId, previousAcquisitionPointId);
				previousAcquisitionPointId = signalId;

				// generate UTCPoint for this in point
				// get time in millis
				UTCPointDescriptorType utcPointSignal = responseSignalType.getUTCPoint();
				//long utcSignalTime = utcPointSignal.getUtcPoint().toGregorianCalendar().getTimeInMillis(); // + signalTimeOffset;
				//long inPointUtc = utcSignalTime + duration.intValue();
				UTCPointDescriptorType utcPoint = SCCResponseUtil.adjustUTCPoint(utcPointSignal.getUtcPoint(), duration.intValue());
				responseSignalTypeInPoint.setUTCPoint(utcPoint);

				// generate scte35PointDescriptor for this in point
				byte[] upidInPoint = (ESAMHelper.UPID_PREFIX + signalId).getBytes();

				SCTE35PointDescriptorType scte35Point = new SCTE35PointDescriptorType();
				scte35Point.setSpliceCommandType(6);

				SegmentationDescriptorType inPointSegment = generateSegment(upidInPoint, null, CppConstants.SEGMENTATION_TYPE_MAP.get(segmentTypeId), false, segmentEventId, (short)0, (short)0, (short)9, false);
				//add all additional parameters coming from SCC request segmentation descriptor
				inPointSegment.getOtherAttributes().putAll(responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getOtherAttributes());
				scte35Point.getSegmentationDescriptorInfo().add(inPointSegment);

				// add inpoint info to repository and serve inpoint request when needed.
				if(CppConstants.SCC_INPOINTS_COMCAST_P3.equals(CppConfigurationBean.getInstance().getSccInpointReponse())){
					DataManagerFactory.getInstance().putInPointsSignal(signalId, cpo.getSignalId());
				}

				if (isBinary) {
					// encode scte35Point to binary

					// first calculate pts_time
					long ptstime = 0;
					try {
						ptstime = Long.parseLong(ptsTime, 2);
					}
					catch (NumberFormatException e) {
						ptstime = 0;
					}
					ptstime += 90 * duration;	// duration already in millisecond

					String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Point, Scte35BinaryUtil.toBitString(ptstime, 33), pts_adjustment);
					BinarySignalType bst = new BinarySignalType();
					bst.setValue(Base64.decodeBase64(encodedStr.getBytes()));
					bst.setSignalType("SCTE35");
					responseSignalTypeInPoint.setBinaryData(bst);
					responseSignalTypeInPoint.setSCTE35PointDescriptor(null);
				}
				else {
					responseSignalTypeInPoint.setSCTE35PointDescriptor(scte35Point);
				}
			}
		}


		// here comes the conditioning info section
		int previousDuration = 0;
		if ((breakInfos != null) && (breakInfos.size() > 0)) {
			for (BreakInfo breakInfo : breakInfos) {
				Integer duration = breakInfo.getDuration();
				String signalId = breakInfo.getInSignalId();

				ConditioningInfoType cit = new ConditioningInfoType();
				cit.setAcquisitionSignalIDRef(conditioInfoAcquisitionPointIdMap.get(signalId));
				cit.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration.intValue() - previousDuration));
				conditioningInfoList.add(cit);

				previousDuration = duration.intValue();
			}
		}
	}

	private void saveSegmentationDescriptor(SCTE35PointDescriptorType scte35pds, DataManager dataManager,String signalPointID,byte[] upid) {
		saveSegmentTypedSegmentationDescriptor(scte35pds, dataManager, signalPointID, upid, null);
	}

	private void saveSegmentTypedSegmentationDescriptor(SCTE35PointDescriptorType scte35pds, DataManager dataManager,String signalPointID,byte[] upid, SegmentType segmentTypeDescriptor) {
		SegmentationDescriptor segmentationDescriptor = SCCResponseUtil.getSegmentationDescriptor(scte35pds,upid, segmentTypeDescriptor);
		//segmentTypeDescriptor is not NULL means request belongs to VIACOM
		if ((segmentationDescriptor.getUpidType() != null) && (segmentationDescriptor.getSegmentationTypeId() != null) && (segmentTypeDescriptor != null || segmentationDescriptor.getSegmentationUpid() != null)) {
			dataManager.putSegmentationDescriptor(signalPointID,segmentationDescriptor);
		}
	}

	private void updateSegmentationDescriptor(byte[] upid, Long segmentEventId,
			Long longestDuration, final SCTE35PointDescriptorType scte35Des, boolean replaceSegmentTypeToDistributorAd) {
		for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
			seg.setUpid(upid);
			seg.setSegmentEventId(segmentEventId);
			if(replaceSegmentTypeToDistributorAd &&
					((seg.getSegmentTypeId() == SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()) || (seg.getSegmentTypeId() == SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()))){
				seg.setSegmentTypeId(SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId());
			}
			if (longestDuration != null) {
				try {
					seg.setDuration(JavaxUtil.getDatatypeFactory().newDuration(longestDuration));
				} catch (DatatypeConfigurationException e) {
					LOGGER.warn(()->e.getMessage() + "duration " + longestDuration, e);
				}
			}
		}
	}

	private void updateSegmentationDescriptor(byte[] upid, final SCTE35PointDescriptorType scte35Des) {
		//update only the UPID information
		for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
			seg.setUpid(upid);
		}
	}

	private void processSignalLinearParity(
			SignalProcessingNotificationType notification,
			ResponseSignalType responseSignalType,
			SCTE35PointDescriptorType scte35,
			String ptsTime,
			String pts_adjustment,
			ConfirmedPlacementOpportunity cpo,
			boolean isBinary, AcquisitionPoint aqpt) throws DatatypeConfigurationException {

		List<ConditioningInfoType> conditioningInfoList = notification.getConditioningInfo();

		byte[] upid = (ESAMHelper.UPID_PREFIX + cpo.getSignalId()).getBytes();

		Set<Integer> placementsDurationsInMilliseconds = cpo.getPlacementsDurationsInMilliseconds();

		// find the longest duration
		Integer longestDuration = 0;
		for (Integer i : placementsDurationsInMilliseconds) {
			if (i.compareTo(longestDuration) > 0) {
				longestDuration = i;
			}
		}
		long duration = longestDuration.longValue();
		LOGGER.debug(()->"duration " + duration);

		// remove the longest duration from the original set
//		placementsDurationsInMilliseconds.remove(longestDuration);

		// if not empty, add all entries to Conditioning Info section
		for (Integer i : placementsDurationsInMilliseconds) {
			ConditioningInfoType cit = new ConditioningInfoType();
			cit.setAcquisitionSignalIDRef(responseSignalType.getAcquisitionSignalID());
			cit.setDuration(JavaxUtil.getDatatypeFactory().newDuration(i));
			conditioningInfoList.add(cit);
		}

		final SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
		if((scte35Des != null) && (valueOf(scte35Des.getSpliceCommandType()) == SPLICE_INSERT)) {  // add one segmentation into the response
			// we need to replace the SpliceInsert section with TimeSignal section

			// first update the splice command type
			// and remove the SpliceInsert section
			scte35Des.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
			scte35Des.setSpliceInsert(null);
			Short segmentTypeId = getSegmentationTypeId(responseSignalType.getSCTE35PointDescriptor());
			// if segmentation type Id is null then assign segmentation type Id = PLACEMENT_OPPORTUNITY_START
			if(segmentTypeId == null) {
				segmentTypeId = AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId();
			}
			// add one segmentation descriptor
			final SegmentationDescriptorType segment = generateSegment(upid, new Long(duration), AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
					getIdForCountDownQueueCheck(responseSignalType), (short)0, (short)0, (short)9, false);

			scte35Des.getSegmentationDescriptorInfo().add(segment);
		} else if((scte35Des != null) && (valueOf(scte35Des.getSpliceCommandType()) == TIME_SIGNAL)) {
			for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
				seg.setUpid(upid);
				if(!aqpt.isUseInbandOpportunitySignal())
				seg.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			}
		}

		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		if(binarySignal != null) {
			generateBinaryData(upid, new Long(duration), binarySignal,scte35,ptsTime, pts_adjustment, aqpt);
			responseSignalType.setSCTE35PointDescriptor(null);
		}
	}

	private void generateBinaryDataWithInPoints(final byte[] upid, Long duration,
			final BinarySignalType binarySignal,SCTE35PointDescriptorType scte35Pt, String ptsTime, String pts_adjustment, AcquisitionPoint aqpt) {
		try {
			// set UPID based on the Signal ID
			/*
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuffer pts = new StringBuffer();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts);
			*/
			Long countDownCueId = getIdForCountDownQueueCheck(scte35Pt);

			if((scte35Pt != null) && (valueOf(scte35Pt.getSpliceCommandType()) == SPLICE_INSERT)) { // handling splice insert
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				scte35Pt.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
				scte35Pt.setSpliceInsert(null);

				if((scte35Pt.getSegmentationDescriptorInfo() != null) && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()){
					updateSegmentationDescriptor(upid,countDownCueId,duration,scte35Pt, (aqpt!= null) && (aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD));
				} else {//add one segmentation descriptor if not present
					final SegmentationDescriptorType segment = generateSegment(upid, duration, AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
							countDownCueId, (short)0, (short)0, (short)9, true);
					scte35Pt.getSegmentationDescriptorInfo().add(segment);
				}
			} else if((scte35Pt != null) && (valueOf(scte35Pt.getSpliceCommandType()) == TIME_SIGNAL)) {// handling time signal
				if(!scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
					scte35Pt.getSegmentationDescriptorInfo().get(0).setSegmentEventId(countDownCueId);
					scte35Pt.getSegmentationDescriptorInfo().get(0).setUpid(upid);
					if(!aqpt.isUseInbandOpportunitySignal())
					scte35Pt.getSegmentationDescriptorInfo().get(0).setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
				}
			}

			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, ptsTime, pts_adjustment);
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		} catch(Exception ex) {
			LOGGER.warn(()->"binary data process issue", ex);
		}
	}

	private void generateBinaryData(final byte[] upid, Long duration,
			final BinarySignalType binarySignal, SCTE35PointDescriptorType scte35Pt, String ptsTime,  String pts_adjustment, AcquisitionPoint aqpt) {
		try {
			// set UPID based on the Signal ID
			/*
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuffer pts = new StringBuffer();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts);
			*/
			if((scte35Pt.getSegmentationDescriptorInfo() != null) && (scte35Pt.getSegmentationDescriptorInfo().size() > 0)) {
				scte35Pt.getSegmentationDescriptorInfo().get(0).setUpid(upid);
			} else {
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				Long spliceEventId = getIdForCountDownQueueCheck(scte35Pt);
				scte35Pt.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
				scte35Pt.setSpliceInsert(null);

				// add one segmentation descriptor
				final SegmentationDescriptorType segment = generateSegment(upid, duration, AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
						spliceEventId, (short)0, (short)0, (short)9, true);
				scte35Pt.getSegmentationDescriptorInfo().add(segment);
			}
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, ptsTime, pts_adjustment);
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		} catch(Exception ex) {
			LOGGER.warn(()->"binary data process issue", ex);
		}
	}

	private SegmentationDescriptorType generateSegment(final byte[] upid, final Long duration, short segmentTypeId, boolean segEventCancelIndicator,
				Long segmentEventId, short segmentNum, short segmentExpected, short upidType, boolean isBinary) {
		final SegmentationDescriptorType segment = new SegmentationDescriptorType();

		if (duration != null) {
			try {
				segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			} catch (DatatypeConfigurationException e) {
				LOGGER.error(()->e.getMessage() + "duration " + duration, e);
			}
		}

		segment.setSegmentationEventCancelIndicator(segEventCancelIndicator);
		segment.setSegmentEventId(segmentEventId);
		segment.setSegmentNum(segmentNum);
		segment.setSegmentsExpected(segmentExpected);
		segment.setSegmentTypeId(segmentTypeId);
		segment.setUpid(upid);
		segment.setUpidType(upidType);
		return segment;
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema) {
		return objectToXML(signalNotification, schema, null);
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema, Map<String, I03ResponseModelDelta> alternates) {
		return schema.getResponseHandler().generateSCCResponse(signalNotification, alternates);
	}


}
