
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

import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.Duration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.exeptions.ResourceNotFoundException;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.AlternateContentTypeModel;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SignalHandlingConfiguration;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UUIDUtils;

/**
 *
 * Search confirmed POs. If available, return the confirmed placement opportunity
 *
 * @author mchakkarapani
 */
public class POISDecisionComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(POISDecisionComponent.class);
	private static HashMap<String, Object> LOCK_OBJECTS_FOR_ACQUISITION_POINTS = new HashMap<String, Object>();
	private static String EMPTY_STRING = "";

	private static synchronized Object getLockObject(String acqPointId) {
		Object locker = LOCK_OBJECTS_FOR_ACQUISITION_POINTS.get(acqPointId);
		if (locker == null) {
			locker = new Object();
			LOCK_OBJECTS_FOR_ACQUISITION_POINTS.put(acqPointId, locker);
		}
		return locker;
	}

	@Override
	public Object onCall(final MuleEventContext context) {
		try {
			boolean isPOAbortRequest = false;
			context.getMessage().setProperty("newConfirmation", new Boolean(false), PropertyScope.OUTBOUND);//To prevent multiple PO notifications

			// setup Mule context message for all decisions
			Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDelta = new HashMap<>();
			final SignalProcessingEventType requestEvent = context.getMessage().getOutboundProperty(ContextConstants.NOTIFICATION_EVENT);
			final Short segmentTypeIdInRequestEvent = context.getMessage().getProperty(SEGMENT_TYPE_ID, PropertyScope.OUTBOUND);
			HashMap<String, ConfirmedPlacementOpportunity> decisions = new HashMap<String, ConfirmedPlacementOpportunity>();
			HashMap<String, SCTE35PointDescriptorType> scte35pds = new HashMap<String, SCTE35PointDescriptorType>();
			HashMap<String, String> ptsTimes = context.getMessage().getProperty("ptsTimes", PropertyScope.OUTBOUND);
			HashMap<String, String> ptsAjustments = context.getMessage().getProperty("ptsAdjustments", PropertyScope.OUTBOUND);
			Map<String, String> ptsTimesBySignalId = new HashMap<String, String>();
			SignalProcessingNotificationType notification = context.getMessage().getProperty("signal_response", PropertyScope.OUTBOUND);
			PoisAuditLogVO poisAuditLogVO = AuditLogHelper.populateAuditLogVO(context, notification);
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

				if (isNoopOrDeleteCandidateOnAQForSpliceInsertSignal(context, notification, aqpt)) {
					return "";
				}

				context.getMessage().setProperty("acquisition_point", aqpt, PropertyScope.OUTBOUND);
				String acqPointId = aqpt.getAcquisitionPointIdentity();

				List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
				if (responseSignalTypes != null && responseSignalTypes.size() > 0) {
					for (ResponseSignalType responseSignalType : responseSignalTypes) {

						SCTE35PointDescriptorType scte35Pt = responseSignalType.getSCTE35PointDescriptor();
						final StringBuilder pts = new StringBuilder();
						final StringBuilder pts_adjustment = new StringBuilder();
						boolean isBinary = false;
						if (scte35Pt == null) {
							// binary case
							ptsTimes = new HashMap<String, String>();
							isBinary = true;
							scte35Pt = new SCTE35PointDescriptorType();
							final byte[] encoded = Base64.encodeBase64(responseSignalType.getBinaryData().getValue());
							Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustment);
							//String output = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, pts.toString());
							//System.out.println(output);
							ptsTimes.put(responseSignalType.getAcquisitionSignalID(), pts.toString());
							ptsAjustments.put(responseSignalType.getAcquisitionSignalID(), pts_adjustment.toString());
						}

						// get SCTE35PointDescriptorType whether it is in binary format or not
						// save it in context so that don't need to decode the binary data again later
						scte35pds.put(responseSignalType.getAcquisitionSignalID(), scte35Pt);

						if (scte35Pt != null && (scte35Pt.getSpliceCommandType() < SpliceCommandType.SPLICE_INSERT.getCommandtype()
								|| scte35Pt.getSpliceCommandType() > SpliceCommandType.TIME_SIGNAL.getCommandtype())) {
							// unsupported spliceCommand type. So we skip it
							if (LOGGER.isInfoEnabled()) {
								LOGGER.info("Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity() + ",acquisitionSignalID="
										+ responseSignalType.getAcquisitionSignalID() + ") because it is has unsupported Splice Command type: " + scte35Pt.getSpliceCommandType()
										+ "in the request.");
							}
							continue;
						}
						long durationForNoSchedules = 0;
						boolean notFromSchedules = CppConstants.NO_SCHEDULES.equalsIgnoreCase(aqpt.getBaSchedulesInterfaceTypeExternalRef());
						if (notFromSchedules || aqpt.isUseInbandOpportunitySignal()) {
							/**
							 * Handling for multiple segmentation descriptor support for Time Signal SCC.
							 */
							durationForNoSchedules = getDuration(scte35Pt, isBinary, aqpt.isUseInbandOpportunitySignal());
							//!aqpt.isUseInbandOpportunitySignal() &&
							if (!aqpt.isUseInbandOpportunitySignal() && scte35Pt.getSpliceCommandType() != SpliceCommandType.SPLICE_INSERT.getCommandtype()
									&& scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
								List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35Pt.getSegmentationDescriptorInfo();
								if (segmentationDescriptorInfo.size() > 1) {//setting priority in case of multiple descriptor
									SegmentationDescriptorType segmenationDescriptor = getSegmenationDescriptor(segmentationDescriptorInfo);
									segmentationDescriptorInfo.clear();
									segmentationDescriptorInfo.add(segmenationDescriptor);
								}
							}
						}

						if (!checkRequest(scte35Pt, aqpt, responseSignalType, context)) {
							continue;
						}

						//If it is a signal abort request
						isPOAbortRequest = SegmentType.isPOAbortRequest(scte35Pt, aqpt, responseSignalType.getSignalPointID());
						if (isPOAbortRequest) {
							SCCPoSignalAbortHandler.handleSignalAbortDecision(responseSignalType, scte35Pt, ptsTimesBySignalId, decisions, pts.toString(), aqpt);
							continue;
						}
						// Handling for multiple segmentation descriptor support for Splice Insert SCC.
						if ((notFromSchedules || aqpt.isUseInbandOpportunitySignal()) && scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()
								&& scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
							List<SegmentationDescriptorType> segmentationDescriptorInfo = scte35Pt.getSegmentationDescriptorInfo();
							if (segmentationDescriptorInfo.size() > 1) {// setting priority in case of multiple descriptors
								SegmentationDescriptorType segmenationDescriptor = getSegmenationDescriptor(segmentationDescriptorInfo);
								segmentationDescriptorInfo.clear();
								segmentationDescriptorInfo.add(segmenationDescriptor);
							}
						}

						if (scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {

							boolean toContinue = false;
							for (SegmentationDescriptorType seg : scte35Pt.getSegmentationDescriptorInfo()) {
								// processing placement opportunity start , provider opportunity start signals for now
								if (seg.getSegmentTypeId() != SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()
										&& seg.getSegmentTypeId() != SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()
										&& seg.getSegmentTypeId() != SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId()
										&& (!(aqpt.isUseInbandOpportunitySignal() && seg.getSegmentTypeId() == SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId()))) {
									if (seg.getSegmentTypeId() == SegmentType.PLACEMENT_OPPORTUNITY_END.getSegmentTypeId()
											|| seg.getSegmentTypeId() == SegmentType.PROVIDER_ADVERTISEMENT_END.getSegmentTypeId()
											|| seg.getSegmentTypeId() == SegmentType.DISTRIBUTOR_ADVERTISEMENT_END.getSegmentTypeId()) {
										//Inpoints handling for COMCAST_P3
										if (CppConstants.SCC_INPOINTS_COMCAST_P3.equals(CppConfigurationBean.getInstance().getSccInpointReponse())) {
											if (LOGGER.isInfoEnabled()) {
												String inPointsSignalId = "";
												byte[] upidBinary = seg.getUpid();
												if (upidBinary != null) {
													String upidHex = new HexBinaryAdapter().marshal(upidBinary);
													inPointsSignalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
												} else {
													inPointsSignalId = responseSignalType.getSignalPointID();
												}
												LOGGER.info("Found one in points request with Signal ID: " + inPointsSignalId);
											}
										}
										toContinue = true;
										continue;
									} else {
										if (LOGGER.isInfoEnabled()) {
											// unsupported Segmenation_type_id. So we skip it.
											LOGGER.info(()->"Skipping this AcquiredSignal (acquisitionPointID=" + responseSignalType.getAcquisitionPointIdentity()
													+ ",acquisitionSignalID=" + responseSignalType.getAcquisitionSignalID()
													+ ") because it is has unsupported Segmenation_type_id: " + seg.getSegmentTypeId() + " in the request.");
										}
										toContinue = true;
										continue;
									}
								}
							}
							if (toContinue) {
								continue;
							}
						}

						GregorianCalendar cal = null;
						if (responseSignalType.getUTCPoint() == null || responseSignalType.getUTCPoint().getUtcPoint() == null
								|| (cal = responseSignalType.getUTCPoint().getUtcPoint().toGregorianCalendar()) == null) {
							LOGGER.error(()->"Passed in UTCPoint: " + responseSignalType.getUTCPoint() + " is invalid");
							context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
							return "";
						}
						final Long eventTime = cal.getTime().getTime();
						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(()->"Request properties. Acquision Point Identity=" + acqPointId + ", eventTime:" + eventTime);
						}

						POProcessingComponent poComponent = new POProcessingComponent(aqpt, eventTime);
						ConfirmedPlacementOpportunity cpo = poComponent.getConfirmedPlacementOpportunity();

						if (cpo != null) {

							if (cpo.getUtcSignalTime() < eventTime) {
								// TWC dup signal case
								context.getMessage().setProperty("duplicate_signal", new Boolean(true), PropertyScope.OUTBOUND);
							} else {
								// decision already made
								String signalId = cpo.getSignalId();
								// link the decision to this response section by signal id
								responseSignalType.setSignalPointID(signalId);
								// also we need to link this signal id to the actual decision
								// and save them in a HashMap in Mule context for late use
								decisions.put(signalId, cpo);
							}
						} else {
							// no decision made on this aquiredSignal yet, call different API to make POIS decision
							// need to synchronize on each aquisitionPoint, there probably won't have more than 1 simultaneous threads
							// asking for decisions from the same acquisition point, but to synchronize just in case, it won't hurt much
							Object locker = getLockObject(acqPointId);
							synchronized (locker) {
								try {
									if (notFromSchedules || aqpt.isUseInbandOpportunitySignal()) {
										cpo = generatePlacementOpportunityForNoSchedule(aqpt, eventTime, durationForNoSchedules, poComponent, scte35Pt, poisAuditLogVO);
									} else {
										cpo = poComponent.getPlacementOpportunity(poisAuditLogVO);
									}
									if (cpo != null) { //persist
										// set signalTimeOffset, however, if there is already a value, do not set it.
										if (aqpt != null && cpo.getSignalTimeOffset() == ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE) {
											cpo.setSignalTimeOffset(aqpt.getSignalTimeOffset());
										}
										dataManager.putConfirmedPlacementOpportunity(cpo);
									}
								} catch (ResourceNotFoundException rfe) {
									LOGGER.error(()->"Passed in Acquisition Point: " + acqPointId + ", EventTime: " + eventTime + " is invalid");
									context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(rfe.getErrorCode()), PropertyScope.OUTBOUND);
								}
								if (cpo != null) {
									context.getMessage().setProperty("newConfirmation", new Boolean(true), PropertyScope.OUTBOUND);
									// this is the signal id from a new decision
									String signalId = cpo.getSignalId();
									ptsTimesBySignalId.put(signalId, pts.toString());
									// link the decision to this response section by signal id
									responseSignalType.setSignalPointID(signalId);
									// also we need to link this signal id to the actual decision
									// and save them in a HashMap in Mule context for late use
									decisions.put(signalId, cpo);
								} else {
									String msg = "No placement opportunity is found for this signal : " + responseSignalType.getAcquisitionSignalID();
									LOGGER.warn(msg);
									context.getMessage().setProperty(CppConstants.PLACEMENT_OPPORTUNITY_NOT_FOUND, msg, PropertyScope.OUTBOUND);
								}
							}
						}
					} // responseSignalTypes for loop
				}
			}
			//Ad Slate URL
			insertAdSlateUrlInAllPoResponseIfProcessSettingOnAQ(AltContentIdentityResponseModelDelta, requestEvent, segmentTypeIdInRequestEvent, notification, aqpt, isPOAbortRequest, context);
			context.getMessage().setProperty(ContextConstants.I03_MODEL_DELTA, AltContentIdentityResponseModelDelta, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("acquisitionPoint", aqpt, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("decisions", decisions, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("scte35s", scte35pds, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("ptsTimes", ptsTimes, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("ptsAdjustments", ptsAjustments, PropertyScope.OUTBOUND);
			context.getMessage().setProperty("ptsTimesBySignalId", ptsTimesBySignalId, PropertyScope.OUTBOUND);
		} catch (Exception e) {
			LOGGER.error(()->"Unexpected error happened during SCC decision: ", e);
			String errMsg = e.getMessage() == null ? e.toString() : e.getMessage();
			context.getMessage().setProperty(CppConstants.SYSTEM_ERROR, "Unexpected error happened during SCC decision:" + errMsg, PropertyScope.OUTBOUND);
		}
		return "";
	}

	private boolean isNoopOrDeleteCandidateOnAQForSpliceInsertSignal(final MuleEventContext context, SignalProcessingNotificationType notification, AcquisitionPoint aqpt) {
		if (aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.DELETE || aqpt.getSpliceInsertConfiguredValue() == SignalHandlingConfiguration.NOOP) {
			List<ResponseSignalType> responseSignalTypes = notification.getResponseSignal();
			if (responseSignalTypes != null && responseSignalTypes.size() > 0) {
				long spliceCommandType = -1;
				for (ResponseSignalType responseSignalType : responseSignalTypes) {
					SCTE35PointDescriptorType scte35Pt = responseSignalType.getSCTE35PointDescriptor();
					if (scte35Pt != null) {
						spliceCommandType = scte35Pt.getSpliceCommandType();
						if (spliceCommandType != 0) {
							break;
						}
					}
				}
				final boolean isSpliceInsert = SpliceCommandType.valueOf(spliceCommandType) == SpliceCommandType.SPLICE_INSERT;
				if (isSpliceInsert) {
					String msg = "Acquisition point is configured to \"" + aqpt.getSpliceInsertConfiguredValue() + "\" the splice insert signal.";
					LOGGER.info(()->msg);
					context.getMessage().setOutboundProperty(SpliceCommandType.SPLICE_INSERT.name(), aqpt.getSpliceInsertConfiguredValue());
					return true;
				}
			}
		}
		return false;
	}

	private long getDuration(SCTE35PointDescriptorType scte35Pt, boolean isBinary, boolean isUseInbandOpportunitySignal) {
		if (scte35Pt != null && scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {//command Type is 5
			Duration duration = null;
			return ((duration = scte35Pt.getSpliceInsert().getDuration()) != null) ? duration.getTimeInMillis(Calendar.getInstance()) : 0;
		} else if (scte35Pt != null && scte35Pt.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) {//command Type is 6  {
			if (scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()) {
				long duration = 0;
				List<SegmentationDescriptorType> segmentationDescriptorType = scte35Pt.getSegmentationDescriptorInfo();
				for (SegmentationDescriptorType segmentationdType : segmentationDescriptorType) {
					if (isUseInbandOpportunitySignal && segmentationdType.getSegmentTypeId() != null
							&& SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId() == segmentationdType.getSegmentTypeId()) {
						continue;
					}
					if (segmentationdType.getDuration() != null) {
						duration = duration + segmentationdType.getDuration().getTimeInMillis(Calendar.getInstance());
					}
				}
				return duration;
			}
		}
		return 0;//default
	}

	private ConfirmedPlacementOpportunity generatePlacementOpportunityForNoSchedule(AcquisitionPoint aqpt, Long signalTime, long duration, POProcessingComponent poComponent,
			SCTE35PointDescriptorType scte35, PoisAuditLogVO poisAuditLogVO) {
		//check that passed-in signal time is within certain time frame
		//if (signalTime != null && signalTime < ())
		DataManager dataManager = DataManagerFactory.getInstance();

		ConfirmedPlacementOpportunity cpo = dataManager.getConfirmedPlacementOpportunity(aqpt.getAcquisitionPointIdentity(), signalTime);
		if (cpo != null) {
			return cpo;
		} else {//Now try to find whether there is a CPO for any other AP related to this AP's Feed for the same time, if yes use the same CPO.
			Set<String> acquisitionPoints = dataManager.getFeedToAcquistionMap(aqpt.getFeedExternalRef());
			if (acquisitionPoints != null) {
				for (String acquisitionPointId : acquisitionPoints) {
					cpo = dataManager.getConfirmedPlacementOpportunity(acquisitionPointId, signalTime);
					if (cpo != null) {
						//Clone it.
						ConfirmedPlacementOpportunity thisAPCPO = new ConfirmedPlacementOpportunity();
						thisAPCPO.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
						thisAPCPO.setUtcSignalTime(cpo.getUtcSignalTime());
						thisAPCPO.setSignalId(cpo.getSignalId());
						thisAPCPO.setBreakInfos((ArrayList<BreakInfo>) cpo.getBreakInfos());

						/**
						 * Update the Signal Process Cursor too.
						 */
						SignalProcessorCursor signalProcessorCursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
						signalProcessorCursor.setLastConfirmedPOUTCTime(thisAPCPO.getUtcSignalTime());
						poComponent.updateSignalProcessorCursor(signalProcessorCursor);
						//Write Confirmation to Audit Log
						if (aqpt.isUseInbandOpportunitySignal()) {
							List<String> breakIds = new ArrayList<String>();
							for (BreakInfo brk : cpo.getBreakInfos()) {
								breakIds.add(brk.getInSignalId());
							}
							poComponent.writeConfirmedPOToAuditLog(aqpt.getAcquisitionPointIdentity(), thisAPCPO, breakIds, poisAuditLogVO);
						}
						return thisAPCPO;
					}
				}
			}
		}

		//No CPO found so create a new one.
		cpo = new ConfirmedPlacementOpportunity();
		cpo.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());
		cpo.setUtcSignalTime(signalTime);
		cpo.setSignalId(UUIDUtils.getBase64UrlEncodedUUID());
		ArrayList<BreakInfo> breaks = new ArrayList<BreakInfo>();
		BreakInfo breakInfo = new BreakInfo(UUIDUtils.getBase64UrlEncodedUUID(), UUIDUtils.getBase64UrlEncodedUUID(), (int) duration);
		breaks.add(breakInfo);
		cpo.setBreakInfos(breaks);

		List<String> breakIds = new ArrayList<String>();
		breakIds.add(breakInfo.getInSignalId());

		/**
		 * Update the Signal Process Cursor too.
		 */
		SignalProcessorCursor signalProcessorCursor = dataManager.getSignalProcessorCursor(aqpt.getAcquisitionPointIdentity());
		signalProcessorCursor.setLastConfirmedPOUTCTime(cpo.getUtcSignalTime());
		poComponent.updateSignalProcessorCursor(signalProcessorCursor);

		//Write Confirmation to Audit Log
		if (aqpt.isUseInbandOpportunitySignal()) {
			poComponent.writeConfirmedPOToAuditLog(aqpt.getAcquisitionPointIdentity(), cpo, breakIds, poisAuditLogVO);
		}

		return cpo;
	}

	/**
	 * FOR PH-162
	 * Method Used to get the SegmentationDescriptorType from the list based on Hierarchy
	 * Provider Ad > PO Start > Distributor Ad
	 * @param segmentationDescriptorInfo
	 * @return SegmentationDescriptorType
	 */
	private SegmentationDescriptorType getSegmenationDescriptor(List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		SegmentationDescriptorType segmentDescriptor = null;
		// To check if all segmentation descriptor are having segmentationEventCancelIndicator = true then return the first descriptor from the list.
		for (SegmentationDescriptorType segmentationDescriptorType : segmentationDescriptorInfo) {
			if (segmentationDescriptorType.isSegmentationEventCancelIndicator() == null) {
				segmentationDescriptorType.setSegmentationEventCancelIndicator(Boolean.FALSE);
			}
			if (!segmentationDescriptorType.isSegmentationEventCancelIndicator() && segmentationDescriptorType.getSegmentTypeId() != null) {
				if (segmentDescriptor == null) {
					segmentDescriptor = segmentationDescriptorType;
				}
				//If multiple descriptor are present of same type i.e having same segmentTypeId the we will return the first occurrence of segmentDesriptor of that type.
				if (segmentDescriptor.getSegmentTypeId() != SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()
						&& segmentationDescriptorType.getSegmentTypeId() == SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()) {
					segmentDescriptor = segmentationDescriptorType;
				} else if (segmentDescriptor.getSegmentTypeId() != SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()
						&& segmentDescriptor.getSegmentTypeId() != SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId()
						&& segmentationDescriptorType.getSegmentTypeId() == SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId()) {
					segmentDescriptor = segmentationDescriptorType;
				}
			}
		}
		if (segmentDescriptor == null) {
			return segmentationDescriptorInfo.get(0);
		}
		return segmentDescriptor;

	}

	private boolean checkRequest(SCTE35PointDescriptorType scte35Pt, AcquisitionPoint ap, ResponseSignalType responseSignal, MuleEventContext context) {
		if (ap == null) {
			return false;
		}

		// check if OutOfNetworkIndicator is received and signal abort is disable for feed, fix issue PRODISSUE-528
		if (scte35Pt.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {
			SpliceInsertType spliceInsert = scte35Pt.getSpliceInsert();
			if (spliceInsert == null) {
				return false;
			}
			if (spliceInsert.isOutOfNetworkIndicator() != null && !spliceInsert.isOutOfNetworkIndicator()) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(()->"Received out_of_network_indicator is false  Acqusition Point " + ap.getAcquisitionPointIdentity());
				}

				if (!ap.isSignalAbortEnabled()) {
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(()->"Signal Abort is disable for Acquisition Point " + ap.getAcquisitionPointIdentity());
						LOGGER.info(()->"This signal shall be reponsed with no-op or delete");
					}
					return false;
				}
			}
		}

		SignalHandlingConfiguration providerAd = ap.getProviderAdsConfiguredValue();
		SignalHandlingConfiguration distributorAd = ap.getDistributorAdsConfiguredValue();
		SignalHandlingConfiguration poAd = ap.getPoSignalsConfiguredValue();
		// IP-Live requirements.
		if (scte35Pt.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) {
			boolean isProviderAd = SegmentType.isProviderAdSignal(scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId());
			boolean isDistributorAd = SegmentType.isDistributorAdSignal(scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId());
			boolean isPoAd = SegmentType.isPoSignal(scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId());

			String action = null;
			if (isProviderAd) {
				action = providerAd.getConfigurationType();
			} else if (isDistributorAd) {
				action = distributorAd.getConfigurationType();
			} else if (isPoAd) {
				action = poAd.getConfigurationType();
			}

			// do nothing, but return original signal
			if (action != null
					&& (action.equalsIgnoreCase(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP) || action.equalsIgnoreCase(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE))) {
				context.getMessage().setProperty(CppConstants.IS_ACTION_SET, true, PropertyScope.OUTBOUND);
				responseSignal.setAction(action.toLowerCase());
				String msg = "Acquisition point " + ap.getAcquisitionPointIdentity() + " is configured to respond with " + action + " for time signal segmentType Id ("+scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()+").";
				LOGGER.info(()->msg);
				return false;
			}

		}
		return true;
	}

	/*
	 * Will be applicable only when
	 * 1. Abort Request
	 * 2. Process and segment type id set for PO,Provider,Distributor ad signal
	 */
	private void insertAdSlateUrlInAllPoResponseIfProcessSettingOnAQ(Map<String, I03ResponseModelDelta> responseToI03ResponseModelMap, final SignalProcessingEventType requestEvent,
			final Short segmentTypeIdInRequestEvent, SignalProcessingNotificationType notification, AcquisitionPoint aqpt, boolean isPOAbortRequest, MuleEventContext context) {

		SignalHandlingConfiguration providerAd = aqpt.getProviderAdsConfiguredValue();
		SignalHandlingConfiguration distributorAd = aqpt.getDistributorAdsConfiguredValue();
		SignalHandlingConfiguration poAd = aqpt.getPoSignalsConfiguredValue();
		SignalHandlingConfiguration spliceInsertAd = aqpt.getSpliceInsertConfiguredValue();

		for (SignalProcessingEventType.AcquiredSignal acquiredSignal : requestEvent.getAcquiredSignal()) {
			SCTE35PointDescriptorType scte35Pt = acquiredSignal.getSCTE35PointDescriptor();

			final boolean isSpliceInsert = SpliceCommandType.valueOf(scte35Pt.getSpliceCommandType()) == SpliceCommandType.SPLICE_INSERT;
			boolean isValidPOStartSignal = SegmentType.isValidPOStartSignal(segmentTypeIdInRequestEvent);
			boolean couldBeSpliceInsertPOStartSignal = (SignalHandlingConfiguration.PROCESS.getConfigurationType().equalsIgnoreCase(spliceInsertAd.getConfigurationType())
					|| SignalHandlingConfiguration.CONVERT_TO_DISTRIBUTOR_AD.getConfigurationType().equalsIgnoreCase(spliceInsertAd.getConfigurationType()) && isSpliceInsert);
			boolean isValidPOEndSignal = SegmentType.isValidPOEndSignal(segmentTypeIdInRequestEvent, scte35Pt, aqpt, acquiredSignal);
			//Only if Its either PO,Distributor,Provider Ad signal and they have been set as Process, the ad slate will be added
			if (isPOAbortRequest || ((SignalHandlingConfiguration.PROCESS.getConfigurationType().equalsIgnoreCase(providerAd.getConfigurationType())
					&& SegmentType.isProviderAdSignal(segmentTypeIdInRequestEvent))

					|| (SignalHandlingConfiguration.PROCESS.getConfigurationType().equalsIgnoreCase(distributorAd.getConfigurationType())
							&& SegmentType.isDistributorAdSignal(segmentTypeIdInRequestEvent))

					|| ((SignalHandlingConfiguration.PROCESS.getConfigurationType().equalsIgnoreCase(poAd.getConfigurationType())
							|| SignalHandlingConfiguration.PROCESS_IN_BAND.getConfigurationType().equalsIgnoreCase(poAd.getConfigurationType()))
							&& SegmentType.isPoSignal(segmentTypeIdInRequestEvent))

					|| couldBeSpliceInsertPOStartSignal)) {

				boolean isFinallyPOStartSignal = (isValidPOStartSignal || (couldBeSpliceInsertPOStartSignal && !isValidPOEndSignal));//Considering Splice Insert/Time SIgnal all possible cases.

				if (isValidPOEndSignal) {
					populateContextIfEndSignalInsertedByCadent(context, scte35Pt);
				}
				if (isFinallyPOStartSignal) {
					context.getMessage().setProperty(CppConstants.IS_VALID_PO_START_SIGNAL, true, PropertyScope.OUTBOUND);
				}

				if (aqpt.getAdSlateLocation() != null) {//That mean on UI ad slate was given
					I03ResponseModelDelta i03ResponseModelDelta = new I03ResponseModelDelta();
					AlternateContentTypeModel deltaForConversionLayer = new AlternateContentTypeModel();

					i03ResponseModelDelta.getAlternateContentIDList().add(deltaForConversionLayer);
					String firstSignalId = notification != null && notification.getResponseSignal() != null && !notification.getResponseSignal().isEmpty()
							? notification.getResponseSignal().get(0).getSignalPointID()
							: null;
					boolean adSlateSetOnAQ = aqpt.getAdSlateLocation() != null ? true : false;
					if (adSlateSetOnAQ) {
						//1. (isValidPOStartSignal + SignalId will not be blank only if we have really served it) or SplicePO Start SIgnal
						if (StringUtils.isNotBlank(firstSignalId) && isFinallyPOStartSignal) {
							deltaForConversionLayer.setAltContentIdentity(aqpt.getAdSlateLocation());
							responseToI03ResponseModelMap.put(firstSignalId, i03ResponseModelDelta);
						}
						//2. isValidPOEndSignal or Abort SIgnal
						else if (isValidPOEndSignal || (aqpt.isSignalAbortEnabled() && isPOAbortRequest)) {
							//In End Signal they may not add SignalPointID (don't know why, it's legacy code that doesn't put SignalPointID in end response.)
							firstSignalId = StringUtils.isNotBlank(firstSignalId) ? firstSignalId : ContextConstants.NONE_SIGNAL_POINT_ID;
							deltaForConversionLayer.setAltContentIdentity(EMPTY_STRING);
							responseToI03ResponseModelMap.put(firstSignalId, i03ResponseModelDelta);
						}
					}
				}
			}
		}
	}

	private boolean populateContextIfEndSignalInsertedByCadent(MuleEventContext context, SCTE35PointDescriptorType scte35Pt) {
		boolean isPOEndSignalInsertedByCadent = false;
		String inPointsSignalId = "";
		byte[] upidBinary = scte35Pt!=null && scte35Pt.getSegmentationDescriptorInfo()!=null && scte35Pt.getSegmentationDescriptorInfo().size()>0? scte35Pt.getSegmentationDescriptorInfo().get(0).getUpid(): null;
		if (upidBinary != null) {
			String upidHex = new HexBinaryAdapter().marshal(upidBinary);
			inPointsSignalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
		}
		if (StringUtils.isNotBlank(inPointsSignalId)) {
			String cadentOpportunityEndSignal = DataManagerFactory.getInstance().getInPointsSignal(inPointsSignalId.trim());
			if (StringUtils.isNotBlank(cadentOpportunityEndSignal)) {
				isPOEndSignalInsertedByCadent = true;
				context.getMessage().setProperty(CppConstants.CADENT_PO_END_SIGNAL, true, PropertyScope.OUTBOUND);
			}
		}
		return isPOEndSignalInsertedByCadent;
	}

}
