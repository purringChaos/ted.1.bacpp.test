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

package tv.blackarrow.cpp.components.po;

import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;
import static tv.blackarrow.cpp.utils.SCCResponseUtil.getIdForCountDownQueueCheck;
import static tv.blackarrow.cpp.utils.SCCResponseUtil.getSegmentationTypeId;
import static tv.blackarrow.cpp.utils.SegmentType.DISTRIBUTOR_ADVERTISEMENT_END;
import static tv.blackarrow.cpp.utils.SegmentType.DISTRIBUTOR_ADVERTISEMENT_START;
import static tv.blackarrow.cpp.utils.SegmentType.PLACEMENT_OPPORTUNITY_END;
import static tv.blackarrow.cpp.utils.SegmentType.PLACEMENT_OPPORTUNITY_START;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.SCCInpointHandler;
import tv.blackarrow.cpp.components.SCCPoSignalAbortHandler;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
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
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
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

		try {
			// we need to come up with the notification response for all decisions
			final SignalProcessingNotificationType notification = (SignalProcessingNotificationType) ctx.getMessage().getOutboundProperty(SIGNAL_RESPONSE);
			final HashMap<String, ConfirmedPlacementOpportunity> decisions = (HashMap<String, ConfirmedPlacementOpportunity>) ctx.getMessage().getOutboundProperty("decisions");
			final HashMap<String, String> ptsTimes = (HashMap<String, String>) ctx.getMessage().getOutboundProperty("ptsTimes");
			final HashMap<String, String> ptsAdjustments = (HashMap<String, String>) ctx.getMessage().getOutboundProperty("ptsAdjustments");
			final Boolean duplicateSignal = (Boolean) ctx.getMessage().getOutboundProperty("duplicate_signal");
			final Boolean isValidPOStartSignal = ctx.getMessage().getOutboundProperty(CppConstants.IS_VALID_PO_START_SIGNAL) != null
					? (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.IS_VALID_PO_START_SIGNAL)
					: false;
			final Boolean isCadentInsertedPOEndSignal = ctx.getMessage().getOutboundProperty(CppConstants.CADENT_PO_END_SIGNAL) != null
					? (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.CADENT_PO_END_SIGNAL)
					: false;

			//By default existing functionality takes care of returning noop in this case
			final HashMap<String, Long> acquisiitionTimes = (HashMap<String, Long>) ctx.getMessage().getOutboundProperty("acquisition_times");
			final Map<String, I03ResponseModelDelta> i03ResponseModelDelta = (Map<String, I03ResponseModelDelta>) ctx.getMessage()
					.getOutboundProperty(ContextConstants.I03_MODEL_DELTA);

			AcquisitionPoint aqpt = (AcquisitionPoint) ctx.getMessage().getProperty("acquisition_point", PropertyScope.OUTBOUND);
			// retrieve AcquisitionPoint object for all the configuration parameters
			DataManager dataManager = DataManagerFactory.getInstance();
			if (aqpt == null) {
				aqpt = dataManager.getAcquisitionPoint(notification.getAcquisitionPointIdentity());
			}
			String requestSchema = ctx.getMessage().getProperty("schema", PropertyScope.INVOCATION);

			if (LOGGER.isInfoEnabled()) {
				LOGGER.debug(()->"generate SCC response in schema " + requestSchema);
			}

			Schema schema = Schema.getSchema(requestSchema);

			if (notification != null) {

				// Error handling.
				String error = (String) ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
				if (error != null) {
					List<ResponseSignalType> signals = notification.getResponseSignal();
					if (signals != null) {
						for (ResponseSignalType signal : notification.getResponseSignal()) {
							signal.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
						}
					}
					notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode(error));
					String response = objectToXML(notification, schema);
					AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
					return response;
				}

				try {

					String response = processNoopAndDeleteConfiguration(ctx, notification, aqpt, schema);
					if (StringUtils.isNotBlank(response)) {
						//Log into message log.
						AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
						return response;
					}

					boolean includeInPoints = true; //cDVR case as default
					if (aqpt != null) {
						includeInPoints = CppConstants.INTERFACE_COMCAST_CDVR.equals(aqpt.getBaIntefactTypeExternalRef());
					}

					List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
					List<ResponseSignalType> additionalResponseSignalTypes = new ArrayList<ResponseSignalType>();

					//boolean isContentIdentificationRequest = false;

					if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0)) {
						for (ResponseSignalType responseSignalType : responseSignalTypes) {

							SCTE35PointDescriptorType scte35 = responseSignalType.getSCTE35PointDescriptor();

							Short segmentTypeId = (scte35.getSegmentationDescriptorInfo() != null) && !scte35.getSegmentationDescriptorInfo().isEmpty()
									? scte35.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()
									: null;
							Boolean isAdEndSignal = SegmentType.isAdEndSignal(segmentTypeId);

							Integer invalidReq = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.INVALID_SCC_REQUEST);
							Integer invalidAcquisitionPoint = (Integer) ctx.getMessage().getOutboundProperty(CppConstants.RESOURCE_NOT_FOUND);

							String signalId = (invalidReq == null) && (invalidAcquisitionPoint == null) ? responseSignalType.getSignalPointID() : null;
							// get the confirmed placement opportunity
							ConfirmedPlacementOpportunity cpo = null;
							if ((signalId != null) && (signalId.length() > 0)) { // confirmed signal ID
								if (decisions != null) {
									cpo = decisions.get(signalId);
								}
							}

							// cancel response process if cpo is not confirmed.
							if (cpo == null) {
								signalId = null;
							}

							long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);
							boolean isCPOAborted = (cpo != null) && cpo.isAborted();
							Map<String, String> ptsTimePlusOffsetMap = Scte35BinaryUtil.adjustAQPointSignalOffsetInPTS(ptsTimes, responseSignalType.getAcquisitionSignalID(), isCPOAborted, signalTimeOffset);
							String ptsTimePlusOffsetInBinary = ptsTimePlusOffsetMap != null ? ptsTimePlusOffsetMap.get(CppConstants.PTS_TIME_PLUS_OFFSET_IN_BINARY) : null;

							String pts_adjustment = (ptsAdjustments == null) || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33)
									: ptsAdjustments.get(responseSignalType.getAcquisitionSignalID());

							UTCPointDescriptorType utcPointSignal = responseSignalType.getUTCPoint();

							//CS2-387: Time in UTC will be set as current system time + delta for PO abort scenarios
							long currentSystemTimeWithAddedDelta = System.currentTimeMillis() + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis();
							if (DEBUG_ENABLED) {
								LOGGER.debug("The current  system time with added delta(" + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis() + ") is: "
										+ currentSystemTimeWithAddedDelta);
							}
							SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
							if (scte35Des.getSpliceInsert() != null && (Boolean.valueOf(String.valueOf(scte35Des.getSpliceInsert().isSpliceEventCancelIndicator()))
									|| (!Boolean.valueOf(String.valueOf(scte35Des.getSpliceInsert().isOutOfNetworkIndicator()))))) {
								responseSignalType.setUTCPoint(SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta));
							}
							else {
								responseSignalType.setUTCPoint(SCCResponseUtil.adjustUTCPoint(utcPointSignal.getUtcPoint(), signalTimeOffset));
							}

							// confirmed signal ID and checking if runover request and BO is in flight
							if (((signalId != null) && (signalId.length() > 0) && ((duplicateSignal == null) || !duplicateSignal) && (scte35 != null))) {
								final String upidStr = ESAMHelper.generateUpidString(signalId);
								final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
								if (aqpt.isUseInbandOpportunitySignal()) {
									saveSegmentTypedSegmentationDescriptor(responseSignalType.getSCTE35PointDescriptor(), dataManager, responseSignalType.getSignalPointID(), upid,
											SegmentType.CONTENT_IDENTIFICATION);
								} else {
									// save and persist data in couchbase.Save the upid from request if present otherwise save the generated upid
									saveSegmentationDescriptor(responseSignalType.getSCTE35PointDescriptor(), dataManager, responseSignalType.getSignalPointID(), upid);
								}
								if (cpo.isAborted() && SegmentType.isPOAbortRequest(scte35, aqpt, responseSignalType.getSignalPointID())) {
									SCCPoSignalAbortHandler.processSignalAbort(notification, responseSignalType, scte35, ptsTimePlusOffsetInBinary, pts_adjustment, cpo,
											additionalResponseSignalTypes, currentSystemTimeWithAddedDelta, dataManager, aqpt);
								} else if (!cpo.isAborted()) {
									if (includeInPoints) {
										// Need to include in points in the notification
										processSignalWithInPoints(notification, responseSignalType, scte35, ptsTimePlusOffsetInBinary, pts_adjustment, signalTimeOffset, cpo,
												additionalResponseSignalTypes, dataManager, aqpt, i03ResponseModelDelta);
									} else {
										// default linear parity case
										processSignalLinearParity(notification, responseSignalType, scte35, ptsTimePlusOffsetInBinary, pts_adjustment, cpo, false, aqpt);
									}
								} else {
									boolean isDelete = (aqpt != null) ? aqpt.isSccDeleteEmptyBreak() : false;
									String action = isDelete ? CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
									responseSignalType.setAction(action);
								}

							} else {
								String inPointsSignalId = "";
								boolean noOp = false;
								if ((scte35 != null) && (scte35.getSegmentationDescriptorInfo() != null) && !scte35.getSegmentationDescriptorInfo().isEmpty()) {
									for (SegmentationDescriptorType seg : scte35.getSegmentationDescriptorInfo()) {
										if (Boolean.valueOf(String.valueOf(seg.isSegmentationEventCancelIndicator()))) {
											break;
										}
										segmentTypeId = seg.getSegmentTypeId();

										// processing placement ,provider and distributor opportunity start signals.
										if ((segmentTypeId != PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()) && (segmentTypeId != PROVIDER_ADVERTISEMENT_START.getSegmentTypeId())
												&& (segmentTypeId != DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId())
												&& (!(aqpt != null && aqpt.isUseInbandOpportunitySignal() && seg.getSegmentTypeId() == SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId()))) {
											if ((segmentTypeId == PLACEMENT_OPPORTUNITY_END.getSegmentTypeId()) || (segmentTypeId == PROVIDER_ADVERTISEMENT_END.getSegmentTypeId())
													|| (segmentTypeId == DISTRIBUTOR_ADVERTISEMENT_END.getSegmentTypeId())) {
												if (CppConstants.SCC_INPOINTS_COMCAST_P3.equals(CppConfigurationBean.getInstance().getSccInpointReponse())) {
													byte[] upidBinary = seg.getUpid();
													if (upidBinary != null) {
														String upidHex = new HexBinaryAdapter().marshal(upidBinary);
														inPointsSignalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
													} else {
														inPointsSignalId = responseSignalType.getSignalPointID();
													}
													SCCInpointHandler.processInPointResponse(inPointsSignalId, notification, responseSignalType, ptsTimePlusOffsetInBinary,
															pts_adjustment, additionalResponseSignalTypes, aqpt);
													noOp = true;
												}
												break;
											} else {
												//Unsupported segmentation type id.
												String action = CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
												responseSignalType.setAction(action);
												noOp = true;
												break;
											}
										}
									}
								} else {
									if (invalidReq != null) {
										noOp = true;
										String msg = "Invalid SCC message, the acquisition point is configured as 'out-of-band'";
										notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode(msg));
										responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
										//Log into message log.
										AuditLogger.auditMessage(msg + ": " + notification.getAcquisitionPointIdentity(), AuditLogHelper.populateAuditLogVO(ctx, notification));
									}
								}

								Boolean isActinoSet = ctx.getMessage().getOutboundProperty(CppConstants.IS_ACTION_SET) == null ? false : true;//This property was set only for Time Signal, don't know why not for
								if (isValidPOStartSignal && !noOp && !CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP.equals(responseSignalType.getAction()) && !isActinoSet) {
									// this default action comes from the configuration, if the flag is true: "delete" otherwise "noop"
									boolean isDelete = (aqpt != null) ? aqpt.isSccDeleteEmptyBreak() : false;
									String action = isDelete ? CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE : CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP;
									responseSignalType.setAction(action);
								}

								//Ad End Signal Response new definition per P821-308, should be like below
								//PRODISSUE-1446 Linear DAI - Need to handle PO End signal properly, is deprecating P821-308.
								//All Ad Signal that were for Process/Convert etc should continue to do same
								setActionForOpportunityEnd(isAdEndSignal, responseSignalType, aqpt, isCadentInsertedPOEndSignal);

								if (includeInPoints && (scte35 != null) && (valueOf(scte35.getSpliceCommandType()) == SPLICE_INSERT) && (scte35.getSpliceInsert() != null)) {
									// replace SpliceInsert with TimeSignal
									convertSpliceInsertWithSegmentDescriptor(responseSignalType, scte35, ptsTimePlusOffsetInBinary, pts_adjustment, aqpt);
								}
							}

							StreamTimesType stts = responseSignalType.getStreamTimes();
							//unsupportedSCCRequestFlag determines that the incoming SCC request was never supported by our system, in such case no need to adjust time.
							if (stts != null) {
								List<StreamTimeType> sttList = stts.getStreamTime();
								for (StreamTimeType stt : sttList) {
									if (stt.getTimeType().equalsIgnoreCase("PTS")) {
										long time = Long.parseLong(stt.getTimeValue());
										time += (signalTimeOffset * 90);
										stt.setTimeValue(Long.toString(time));
									}
								}
							}

							if (responseSignalType.getBinaryData() != null) {
								responseSignalType.setSCTE35PointDescriptor(null);
							}
						}
						responseSignalTypes.addAll(additionalResponseSignalTypes);
					}

					// add all conditioning info to the response
					// this conditioning info section is across all response signals

					if (notification.getStatusCode() == null) {
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

					String resp = objectToXML(notification, schema, i03ResponseModelDelta);
					// NAMESPACE_HACK : ought to be removed once not needed
					Boolean hacked = (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.NAMESPACE_HACK);
					if ((hacked != null) && hacked.booleanValue()) {
						resp = resp.replace(CppConstants.NEW_SCC_NAMESPACE, CppConstants.OLD_SCC_NAMESPACE);
					}

					AuditLogger.auditMessage(resp, AuditLogHelper.populateAuditLogVO(ctx, notification));

					LOGGER.debug("SCC Response: \n" + resp);

					// clean all head properties
					RequestContext.getEventContext().getMessage().clearProperties();
					return resp;
				} catch (Exception e) {
					LOGGER.error(()->"Unexpected error ", e);
					String errMsg = e.getMessage() == null ? e.toString() : e.getMessage();
					notification.setStatusCode(SCCResponseUtil.generateErrorStatusCode("Unexpected error while preparing SCC response: " + errMsg));
					//Log into message log.
					String response = objectToXML(notification, schema);
					AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
					return response;
				}
			} else {
				String errorMessage = (String) ctx.getMessage().getOutboundProperty(CppConstants.SYSTEM_ERROR);
				SignalProcessingNotificationType note = new SignalProcessingNotificationType();
				note.setStatusCode(SCCResponseUtil.generateErrorStatusCode(errorMessage));
				String response = objectToXML(note, schema);
				AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(ctx, notification));
				return response;
			}
		} finally {
			//Clear the cache maintained for this request if any.
			SCCMCCThreadLocalCache.clearMyCache();
		}
	}	

	private void setActionForOpportunityEnd(Boolean isAdEndSignal, ResponseSignalType responseSignalType, AcquisitionPoint aqpt, boolean isCadentInsertedPOEndSignal) {

		if (isAdEndSignal) {
			if (isCadentInsertedPOEndSignal) {
				//PRODISSUE-1446 it overriding P821-486 from action point of view. The new behaior is (Cadent Signal NOOP)/External Signal Delete
				responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
				LOGGER.debug(() -> "Cadent generated Ad end opportunity received (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() + ",acquisitionSignalID="
						+ responseSignalType.getAcquisitionSignalID() + ") responding with " + responseSignalType.getAction());
			} else {
				responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
				LOGGER.debug(() -> "Provider generated Ad end opportunity received (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() + ",acquisitionSignalID="
						+ responseSignalType.getAcquisitionSignalID() + ") responding with " + responseSignalType.getAction());
			}
		}
	}

	/**
	 * @param ctx
	 * @param notification
	 * @param aqpt
	 * @param schema
	 */
	private String processNoopAndDeleteConfiguration(MuleEventContext ctx, final SignalProcessingNotificationType notification, AcquisitionPoint aqpt,
			Schema schema) {
		String resp = "";
		boolean isNoOpOrDelete = false;

		boolean isSpliceInsertNoOpOrDeleteCase = ctx.getMessage().getOutboundProperty(SpliceCommandType.SPLICE_INSERT.name()) != null;
		if (isSpliceInsertNoOpOrDeleteCase) {
			//Set the status code.
			if (notification.getStatusCode() == null) {
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
					SignalHandlingConfiguration signalHandlingConfiguration = aqpt.getSpliceInsertConfiguredValue();
					if (signalHandlingConfiguration == SignalHandlingConfiguration.NOOP) {
						responseSignalType.setAction(ResponseSignalAction.NOOP.toString());
					} else if (signalHandlingConfiguration == SignalHandlingConfiguration.DELETE) {
						responseSignalType.setAction(ResponseSignalAction.DELETE.toString());
					}
				}
			}
		}
		isNoOpOrDelete = isSpliceInsertNoOpOrDeleteCase || isNoOpDeleteInResponse(notification, aqpt);
		if (isNoOpOrDelete) {
			adjustSignalOffsetAndPTSTime(notification, aqpt);
			resp = objectToXML(notification, schema);
			// NAMESPACE_HACK : ought to be removed once not needed
			Boolean hacked = (Boolean) ctx.getMessage().getOutboundProperty(CppConstants.NAMESPACE_HACK);
			if ((hacked != null) && hacked.booleanValue()) {
				resp = resp.replace(CppConstants.NEW_SCC_NAMESPACE, CppConstants.OLD_SCC_NAMESPACE);
			}

			LOGGER.debug("SCC Response: \n" + resp);
			RequestContext.getEventContext().getMessage().clearProperties();
		}

		return resp;
	}

	private boolean isNoOpDeleteInResponse(final SignalProcessingNotificationType notification, AcquisitionPoint aqpt) {
		boolean isNoOpOrDelete = false;
		List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
		if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0) && (aqpt != null)) {
			for (ResponseSignalType responseSignalType : responseSignalTypes) {
				if (responseSignalType.getAction().equalsIgnoreCase(SignalHandlingConfiguration.NOOP.name())
						|| responseSignalType.getAction().equalsIgnoreCase(SignalHandlingConfiguration.DELETE.name())) {
					isNoOpOrDelete = true;
				}
			}
		}
		return isNoOpOrDelete;
	}

	private void adjustSignalOffsetAndPTSTime(final SignalProcessingNotificationType notification, AcquisitionPoint aqpt) {
		List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
		if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0) && (aqpt != null)) {
			for (ResponseSignalType responseSignalType : responseSignalTypes) {
				//Adjust the UTC Time with AQ signal offset and PTS time
				responseSignalType.setUTCPoint(SCCResponseUtil.adjustUTCPoint(responseSignalType.getUTCPoint().getUtcPoint(), aqpt.getSignalTimeOffset()));

				if (responseSignalType.getBinaryData() != null) {
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
	}

	private void convertSpliceInsertWithSegmentDescriptor(ResponseSignalType responseSignalType, SCTE35PointDescriptorType scte35, String ptsTime, String pts_adjustment,
			AcquisitionPoint aqpt) {
		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		// there is no upid from the request
		// and there is no PO decison made, so no signal id available
		byte[] upid = (ESAMHelper.UPID_PREFIX).getBytes();
		if (binarySignal != null) {
			generateBinaryDataWithInPoints(upid, null, binarySignal, scte35, ptsTime, pts_adjustment, aqpt);
			responseSignalType.setSCTE35PointDescriptor(null);
		} else {
			SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
			scte35Des.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
			scte35Des.setSpliceInsert(null);
			Long countDownCueID = getIdForCountDownQueueCheck(responseSignalType);
			// add one segmentation descriptor
			if ((scte35Des.getSegmentationDescriptorInfo() != null) && scte35Des.getSegmentationDescriptorInfo().isEmpty()) {
				final SegmentationDescriptorType segment = generateSegment(upid, null, AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
						countDownCueID, (short) 0, (short) 0, (short) 9, false);
				scte35Des.getSegmentationDescriptorInfo().add(segment);
			}
		}
	}

	private void processSignalWithInPoints(SignalProcessingNotificationType notification, ResponseSignalType responseSignalType, SCTE35PointDescriptorType scte35, String ptsTime,
			String pts_adjustment, long signalTimeOffset, ConfirmedPlacementOpportunity cpo, List<ResponseSignalType> responseSignalTypesInPoints, DataManager dataManager,
			AcquisitionPoint aqpt, Map<String, I03ResponseModelDelta> responseToI03ResponseModelDelta) throws DatatypeConfigurationException {

		// list ResponseSignal of this notification
		//List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();

		// list of ConditioningInfo of this notification
		List<ConditioningInfoType> conditioningInfoList = notification.getConditioningInfo();

		byte[] upid = (ESAMHelper.UPID_PREFIX + cpo.getSignalId()).getBytes();
		Long segmentEventId = getIdForCountDownQueueCheck(responseSignalType);

		//long utcSignalTime = cpo.getUtcSignalTime();

		// out point section
		List<BreakInfo> breakInfos = cpo.getBreakInfos();//Already sorted breaks in asc order.
		Integer longestDuration = null;
		if ((breakInfos != null) && (breakInfos.size() > 0)) {
			longestDuration = breakInfos.get(breakInfos.size() - 1).getDuration();
		}

		// out point non-binary case
		final SCTE35PointDescriptorType scte35Des = responseSignalType.getSCTE35PointDescriptor();
		if (scte35Des != null) {
			//Handling splice insert Signal
			if (valueOf(scte35Des.getSpliceCommandType()) == SPLICE_INSERT) {
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				scte35Des.setSpliceCommandType(6);
				scte35Des.setSpliceInsert(null);

				// update the segmentation descriptor
				if ((scte35Des.getSegmentationDescriptorInfo() != null) && !scte35Des.getSegmentationDescriptorInfo().isEmpty()) {
					updateSegmentationDescriptor(upid, segmentEventId, Long.valueOf(longestDuration), scte35Des,
							(aqpt != null) && (aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD));
				} else {//add one segmentation descriptor if segmentation descriptor is not present
					final SegmentationDescriptorType segment = generateSegment(upid, Long.valueOf(longestDuration),
							AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false, segmentEventId, (short) 0, (short) 0, (short) 9, false);
					scte35Des.getSegmentationDescriptorInfo().add(segment);
				}
			} else if (valueOf(scte35Des.getSpliceCommandType()) == TIME_SIGNAL) {//update the segmentation descriptor
				if (aqpt != null && aqpt.isUseInbandOpportunitySignal()) {
					//IN case of viacom request, only UPID will be updated.
					updateSegmentationDescriptor(upid, scte35Des);
				} else {
					updateSegmentationDescriptor(upid, segmentEventId, Long.valueOf(longestDuration), scte35Des, false);
			}
		}
		}

		// getting segmentation type Id

		Short segmentTypeId = getSegmentationTypeId(responseSignalType.getSCTE35PointDescriptor());
		// if segmentation type Id is null then assign segmentation type Id = PLACEMENT_OPPORTUNITY_START
		if (segmentTypeId == null) {
			segmentTypeId = AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId();
		}
		// out point binarysegmentTypeId case
		boolean isBinary = false;
		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		if (binarySignal != null) {
			isBinary = true;
			generateBinaryDataWithInPoints(upid, Long.valueOf(longestDuration), binarySignal, scte35, ptsTime, pts_adjustment, aqpt);
		}

		// here comes the in points
		HashMap<String, String> conditioInfoAcquisitionPointIdMap = new HashMap<String, String>();
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

				SegmentationDescriptorType inPointSegment = generateSegment(upidInPoint, null, CppConstants.SEGMENTATION_TYPE_MAP.get(segmentTypeId), false, segmentEventId,
						(short) 0, (short) 0, (short) 9, false);
				//add all additional parameters coming from SCC request segmentation descriptor
				inPointSegment.getOtherAttributes().putAll(responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getOtherAttributes());
				scte35Point.getSegmentationDescriptorInfo().add(inPointSegment);

				// add inpoint info to repository and serve in-point request when needed.
				DataManagerFactory.getInstance().putInPointsSignal(signalId, cpo.getSignalId());

				//P821-486 In-point Signal will contain empty AltcontentIdentity Always
				boolean adSlateSetOnAQ = aqpt.getAdSlateLocation() != null ? true : false;
				if (adSlateSetOnAQ) {
					responseToI03ResponseModelDelta.put(signalId, setEmptyUrlForAltContentIdentityTag());
				}

				if (isBinary) {
					// encode scte35Point to binary

					// first calculate pts_time
					long ptstime = 0;
					try {
						ptstime = Long.parseLong(ptsTime, 2);
					} catch (NumberFormatException e) {
						ptstime = 0;
					}
					ptstime += 90 * duration; // duration already in millisecond

					String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Point, Scte35BinaryUtil.toBitString(ptstime, 33), pts_adjustment);
					BinarySignalType bst = new BinarySignalType();
					bst.setValue(Base64.decodeBase64(encodedStr.getBytes()));
					bst.setSignalType("SCTE35");
					responseSignalTypeInPoint.setBinaryData(bst);
					responseSignalTypeInPoint.setSCTE35PointDescriptor(null);
				} else {
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

	private I03ResponseModelDelta setEmptyUrlForAltContentIdentityTag() {
		I03ResponseModelDelta delta = new I03ResponseModelDelta();
		AlternateContentTypeModel alternateURL = new AlternateContentTypeModel();
		delta.getAlternateContentIDList().add(alternateURL);
		String EMPTY_STRING = "";
		alternateURL.setAltContentIdentity(EMPTY_STRING);
		return delta;
	}

	private void saveSegmentationDescriptor(SCTE35PointDescriptorType scte35pds, DataManager dataManager, String signalPointID, byte[] upid) {
		saveSegmentTypedSegmentationDescriptor(scte35pds, dataManager, signalPointID, upid, null);
	}

	private void saveSegmentTypedSegmentationDescriptor(SCTE35PointDescriptorType scte35pds, DataManager dataManager, String signalPointID, byte[] upid,
			SegmentType segmentTypeDescriptor) {
		SegmentationDescriptor segmentationDescriptor = SCCResponseUtil.getSegmentationDescriptor(scte35pds, upid, segmentTypeDescriptor);
		//segmentTypeDescriptor is not NULL means request belongs to VIACOM
		if ((segmentationDescriptor.getUpidType() != null) && (segmentationDescriptor.getSegmentationTypeId() != null)
				&& (segmentTypeDescriptor != null || segmentationDescriptor.getSegmentationUpid() != null)) {
			dataManager.putSegmentationDescriptor(signalPointID, segmentationDescriptor);
		}
	}

	private void updateSegmentationDescriptor(byte[] upid, Long segmentEventId, Long longestDuration, final SCTE35PointDescriptorType scte35Des,
			boolean replaceSegmentTypeToDistributorAd) {
		for (SegmentationDescriptorType seg : scte35Des.getSegmentationDescriptorInfo()) {
			seg.setUpid(upid);
			seg.setSegmentEventId(segmentEventId);
			if (replaceSegmentTypeToDistributorAd && ((seg.getSegmentTypeId() == SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId())
					|| (seg.getSegmentTypeId() == SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()))) {
				seg.setSegmentTypeId(SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId());
			}
			if (longestDuration != null) {
				try {
					seg.setDuration(JavaxUtil.getDatatypeFactory().newDuration(longestDuration));
				} catch (DatatypeConfigurationException e) {
					LOGGER.warn(e.getMessage() + "duration " + longestDuration, e);
				}
			}
		}
	}

	private void updateSegmentationDescriptor(byte[] upid, final SCTE35PointDescriptorType scte35Des) {
		//update only the UPID information
		for (SegmentationDescriptorType seg : scte35Des.getSegmentationDescriptorInfo()) {
			seg.setUpid(upid);
		}
	}

	private void processSignalLinearParity(SignalProcessingNotificationType notification, ResponseSignalType responseSignalType, SCTE35PointDescriptorType scte35, String ptsTime,
			String pts_adjustment, ConfirmedPlacementOpportunity cpo, boolean isBinary, AcquisitionPoint aqpt) throws DatatypeConfigurationException {

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
		if ((scte35Des != null) && (valueOf(scte35Des.getSpliceCommandType()) == SPLICE_INSERT)) { // add one segmentation into the response
			// we need to replace the SpliceInsert section with TimeSignal section

			// first update the splice command type
			// and remove the SpliceInsert section
			scte35Des.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
			scte35Des.setSpliceInsert(null);
			Short segmentTypeId = getSegmentationTypeId(responseSignalType.getSCTE35PointDescriptor());
			// if segmentation type Id is null then assign segmentation type Id = PLACEMENT_OPPORTUNITY_START
			if (segmentTypeId == null) {
				segmentTypeId = AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId();
			}
			// add one segmentation descriptor
			final SegmentationDescriptorType segment = generateSegment(upid, new Long(duration), AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
					getIdForCountDownQueueCheck(responseSignalType), (short) 0, (short) 0, (short) 9, false);

			scte35Des.getSegmentationDescriptorInfo().add(segment);
		} else if ((scte35Des != null) && (valueOf(scte35Des.getSpliceCommandType()) == TIME_SIGNAL)) {
			for (SegmentationDescriptorType seg : scte35Des.getSegmentationDescriptorInfo()) {
				seg.setUpid(upid);
				if (!aqpt.isUseInbandOpportunitySignal()) {
					seg.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
			}
		}
		}

		final BinarySignalType binarySignal = responseSignalType.getBinaryData();
		if (binarySignal != null) {
			generateBinaryData(upid, new Long(duration), binarySignal, scte35, ptsTime, pts_adjustment, aqpt);
			responseSignalType.setSCTE35PointDescriptor(null);
		}
	}

	private void generateBinaryDataWithInPoints(final byte[] upid, Long duration, final BinarySignalType binarySignal, SCTE35PointDescriptorType scte35Pt, String ptsTime,
			String pts_adjustment, AcquisitionPoint aqpt) {
		try {
			// set UPID based on the Signal ID
			/*
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuffer pts = new StringBuffer();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts);
			*/
			Long countDownCueId = getIdForCountDownQueueCheck(scte35Pt);

			if ((scte35Pt != null) && (valueOf(scte35Pt.getSpliceCommandType()) == SPLICE_INSERT)) { // handling splice insert
				// we need to replace the SpliceInsert section with TimeSignal section

				// first update the splice command type
				// and remove the SpliceInsert section
				scte35Pt.setSpliceCommandType(TIME_SIGNAL.getCommandtype());
				scte35Pt.setSpliceInsert(null);

				if ((scte35Pt.getSegmentationDescriptorInfo() != null) && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
					updateSegmentationDescriptor(upid, countDownCueId, duration, scte35Pt,
							(aqpt != null) && (aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD));
				} else {//add one segmentation descriptor if not present
					final SegmentationDescriptorType segment = generateSegment(upid, duration, AcquisitionPoint.getDefaultSegmentTypeForAdStart(aqpt).getSegmentTypeId(), false,
							countDownCueId, (short) 0, (short) 0, (short) 9, true);
					scte35Pt.getSegmentationDescriptorInfo().add(segment);
				}
			} else if ((scte35Pt != null) && (valueOf(scte35Pt.getSpliceCommandType()) == TIME_SIGNAL)) {// handling time signal
				if (!scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
					scte35Pt.getSegmentationDescriptorInfo().get(0).setSegmentEventId(countDownCueId);
					scte35Pt.getSegmentationDescriptorInfo().get(0).setUpid(upid);
					if (!aqpt.isUseInbandOpportunitySignal()) {
						scte35Pt.getSegmentationDescriptorInfo().get(0).setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
				}
			}
			}

			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, ptsTime, pts_adjustment);
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		} catch (Exception ex) {
			LOGGER.warn(()->"binary data process issue", ex);
		}
	}

	private void generateBinaryData(final byte[] upid, Long duration, final BinarySignalType binarySignal, SCTE35PointDescriptorType scte35Pt, String ptsTime,
			String pts_adjustment, AcquisitionPoint aqpt) {
		try {
			// set UPID based on the Signal ID
			/*
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuffer pts = new StringBuffer();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts);
			*/
			if ((scte35Pt.getSegmentationDescriptorInfo() != null) && (scte35Pt.getSegmentationDescriptorInfo().size() > 0)) {
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
						spliceEventId, (short) 0, (short) 0, (short) 9, true);
				scte35Pt.getSegmentationDescriptorInfo().add(segment);
			}
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, ptsTime, pts_adjustment);
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		} catch (Exception ex) {
			LOGGER.warn(()->"binary data process issue", ex);
		}
	}

	private SegmentationDescriptorType generateSegment(final byte[] upid, final Long duration, short segmentTypeId, boolean segEventCancelIndicator, Long segmentEventId,
			short segmentNum, short segmentExpected, short upidType, boolean isBinary) {
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
