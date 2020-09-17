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

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_ATTRIBUTES;
import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_TIMES;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;
import static tv.blackarrow.cpp.components.util.ContextConstants.TRUE_STR;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.signalstate.model.SignalStateModel;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.ds.common.util.XmlUtils;

/**
 *
 * take ESAM SCC request
 *
 */
public class SCCRequestComponent implements Callable {
	private static final Log LOGGER = LogFactory.getLog(SCCRequestComponent.class);//LogManager.getLogger(SCCRequestComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {
		boolean isAuditMessageRequestLogged = false;
		String response = "OK";
		String originalMessage = "";
		SignalProcessingNotificationType notification = null;
		SignalProcessingEventType event = null;
		Schema schema = null;
		try {
			//Cache is used to return the documents for a single key from the cache for this request, rather than every time getting it from Couchbase.
			SCCMCCThreadLocalCache.cachingRequired();

			originalMessage = context.getMessageAsString();
			DataManager dataManager = DataManagerFactory.getInstance();
			LOGGER.info("SCC request:\n" + originalMessage);

			// NAMESPACE_HACK : ought to be removed once not needed
			String nameSpaceChangedMessage = originalMessage;
			if (originalMessage.contains(CppConstants.OLD_SCC_NAMESPACE)) {
				nameSpaceChangedMessage = originalMessage.replace(CppConstants.OLD_SCC_NAMESPACE, CppConstants.NEW_SCC_NAMESPACE);
				context.getMessage().setOutboundProperty(CppConstants.NAMESPACE_HACK, Boolean.TRUE);
			}

			String requestSchema = context.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Receiving request in schema " + requestSchema);
			}

			schema = Schema.getSchema(requestSchema);
			notification = new SignalProcessingNotificationType();
			AcquisitionPoint acquisitionPoint = null;
			event = schema.getRequestHandler().parseSCCRequest(nameSpaceChangedMessage);
			//Log into message log.
			AuditLogger.auditMessage(originalMessage, AuditLogHelper.populateAuditLogVO(context, event));
			isAuditMessageRequestLogged = true;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("How many AcquiredSignals " + event.getAcquiredSignal().size());
			}

			HashMap<String, Long> aquisitionTimes = new HashMap<String, Long>();
			HashMap<String, Map<QName, String>> aquisitionAttributes = new HashMap<String, Map<QName, String>>();
			for (SignalProcessingEventType.AcquiredSignal signal : event.getAcquiredSignal()) {
				notification.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());

				ResponseSignalType responseSignal = CppUtil.addNewResponseSignal(notification, signal);
				populateDescriptorInfoInContextAndNotification(context, signal, notification);
				SCTE35PointDescriptorType scte35Pt = null;
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("Decrypted Binary :\n"
							+ XmlUtils.getJAXBString(SignalProcessingEventType.class, new tv.blackarrow.cpp.signaling.ObjectFactory().createSignalProcessingEvent(event)));
				}
				Short segmentTypeId = 0;
				if ((notification.getResponseSignal() != null) && (notification.getResponseSignal().get(0) != null)
						&& (notification.getResponseSignal().get(0).getSCTE35PointDescriptor() != null)) {
					scte35Pt = notification.getResponseSignal().get(0).getSCTE35PointDescriptor();
					segmentTypeId = (scte35Pt.getSegmentationDescriptorInfo() != null) && (scte35Pt.getSegmentationDescriptorInfo().size() > 0)
							&& (scte35Pt.getSegmentationDescriptorInfo().get(0) != null) && (scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() != null)
									? scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()
									: 0;
					context.getMessage().setOutboundProperty(CppConstants.IS_GROUP_SIGNAL,
							scte35Pt.getSegmentationDescriptorInfo().size() > 1);				
				}
				acquisitionPoint = dataManager.getAcquisitionPoint(signal.getAcquisitionPointIdentity());
				boolean isI02BlackoutRequest = isI02BlackoutRequest(signal, scte35Pt, segmentTypeId);
				boolean isScte224EventRequest = isScte224EventRequest(segmentTypeId);
				boolean isScheduleLessFeedAltEventRequest = isScheduleLessAltEventRequest(acquisitionPoint, signal);
				
				//If signal is in past then response will be noop
				if (acquisitionPoint != null
						&& (SegmentType.isAdStartSignal(segmentTypeId) || SegmentType.isProgramOverlapStartSignal(segmentTypeId) || SegmentType.isProgramStartSignal(segmentTypeId))
						&& (signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis() + 30000) < System.currentTimeMillis()) {
					LOGGER.info("AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + " is in past for the start signal, noop will be returned ");
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC, TRUE_STR);
				}
				//CS2:474 Delete chapter start and chapter end from all Feed Implementations
				else if ((acquisitionPoint != null) && (scte35Pt.getSegmentationDescriptorInfo() != null) && (scte35Pt.getSegmentationDescriptorInfo().size() == 1)
						&& (SegmentType.isChapterStartSignal(segmentTypeId) || SegmentType.isChapterEndSignal(segmentTypeId))) {
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_DELETE_SCC, TRUE_STR);
				} else if ((acquisitionPoint != null) && acquisitionPoint.isFeedHasAlternateContentEnabled()
						&& ((AlternateContentVersion.ESNI_I02.equals(acquisitionPoint.getFeedsAlternateContentVersion()) && isI02BlackoutRequest)
								|| (AlternateContentVersion.ESNI_224.equals(acquisitionPoint.getFeedsAlternateContentVersion()) && isScte224EventRequest)
								|| (AlternateContentVersion.SCHEDULELESS.equals(acquisitionPoint.getFeedsAlternateContentVersion()) && isScheduleLessFeedAltEventRequest))) {
					switch (acquisitionPoint.getFeedsAlternateContentVersion()) {
					case ESNI_I02:
						if (isI02BlackoutRequest && (isDeliveryRestricted(scte35Pt) || SegmentType.isBlackoutAbortRequestWithoutSignalId(signal, scte35Pt))) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug(
										"AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + ", I02 Event flow will be executed.");
							}
							context.getMessage().setOutboundProperty(SEGMENT_TYPE_ID, segmentTypeId);
							context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_ALTCONTENT_REQUEST, TRUE_STR);
							BlackoutHandler.processAndOrderResponseSignals(context, notification, scte35Pt.getSegmentationDescriptorInfo(), signal);
							// For Blackout Request We will be Identifying Weather to Confirm By EventId or BY Blackout Time.
							if (acquisitionPoint != null) {
								context.getMessage().setOutboundProperty(CppConstants.ACQUISITION_POINT, acquisitionPoint);
								context.getMessage().setOutboundProperty(CppConstants.TRIGGER_BLACKOUT_EVENTS_BY_EVENT_ID, acquisitionPoint.isFeedTriggerEventsByEventID());
							}
							if ((signal.getSignalPointID() != null) && !signal.getSignalPointID().trim().isEmpty()) {
								responseSignal.setSignalPointID(signal.getSignalPointID().trim());
							}
						} else if (SegmentType.isValidBlackoutSignal(segmentTypeId) && !isDeliveryRestricted(scte35Pt)) {
							context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_ALTCONTENT_REQUEST, TRUE_STR);
							// We do not do anything for such case.
						}
						break;
					case ESNI_224:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + ", ESNI224 Event flow will be executed.");
						}
						//Add generic information for next phase
						context.getMessage().setOutboundProperty(CppConstants.ACQUISITION_POINT, acquisitionPoint);
						context.getMessage().setOutboundProperty(SEGMENT_TYPE_ID, segmentTypeId);
						context.getMessage().setOutboundProperty(CppConstants.SCTE224, TRUE_STR);
						context.getMessage().setOutboundProperty(CppConstants.SCTE224_MEDIA_BASED_ALTCONTENT, TRUE_STR);
						break;
					case SCHEDULELESS:
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(
									"AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + ", SCHEDULELESS Event flow will be executed.");
						}
						context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_SCHEDULELESS_ALTCONTENT_REQUEST, TRUE_STR);
						if (acquisitionPoint != null) {
							context.getMessage().setOutboundProperty(CppConstants.ACQUISITION_POINT, acquisitionPoint);
						}
						break;
					}
				} else if (acquisitionPoint != null && SegmentType.isValidPOSignal(segmentTypeId, scte35Pt, signal)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + ", Placement flow will be executed.");
					}
					deleteOrNoopAdSignalsIfAlternateEventIsInProgress(context, acquisitionPoint);//Dates Jan 3, We want Delete for Viacom scheduleless altcontent and noop for SCTE224 event case
					context.getMessage().setOutboundProperty(SEGMENT_TYPE_ID, segmentTypeId);
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_PO_REQUEST, TRUE_STR);
				} else if (acquisitionPoint != null && AlternateContentVersion.ESNI_224.equals(acquisitionPoint.getFeedsAlternateContentVersion())) {//Any one of the descriptor if is unrecognized on 224 feed, then should be Delete(CS2-306)
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint.getAcquisitionPointIdentity() + ", ESNI224 Unrecognized Signa flow will be executed.");
					}
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_DELETE_SCC, TRUE_STR);
					context.getMessage().setOutboundProperty(SEGMENT_TYPE_ID, segmentTypeId);
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_PO_REQUEST, TRUE_STR);
				} else {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("AcquisitionSignalID " + signal.getAcquisitionSignalID() + " on acquistionPoint " + acquisitionPoint + ", Generic Not Supported flow will be executed.");
					}
					context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC, TRUE_STR);
				}

				XMLGregorianCalendar acqtime = signal.getAcquisitionTime();
				if (acqtime != null) {
					long time = acqtime.toGregorianCalendar().getTimeInMillis();
					aquisitionTimes.put(signal.getAcquisitionSignalID(), time);
				}
				Map<QName, String> attributes = signal.getOtherAttributes();
				if ((attributes != null) && !attributes.isEmpty()) {
					aquisitionAttributes.put(signal.getAcquisitionSignalID(), attributes);
				}
			}

			// sort the ResponseSignalType list to make sure they are in UTCPoint order
			Collections.<ResponseSignalType> sort(notification.getResponseSignal(), new Comparator<ResponseSignalType>() {
				@Override
				public int compare(ResponseSignalType responseSignalType1, ResponseSignalType responseSignalType2) {
					long utctime1 = responseSignalType1.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
					long utctime2 = responseSignalType2.getUTCPoint().getUtcPoint().toGregorianCalendar().getTimeInMillis();
					return (utctime1 < utctime2) ? -1 : (utctime1 == utctime2 ? 0 : 1);
				}
			});
			//Some more generic information
			context.getMessage().setProperty(NOTIFICATION_EVENT, event, PropertyScope.OUTBOUND);
			context.getMessage().setProperty(ACQUISITION_POINT, acquisitionPoint, PropertyScope.OUTBOUND);
			context.getMessage().setOutboundProperty(ACQUISITION_TIMES, aquisitionTimes);
			context.getMessage().setOutboundProperty(ACQUISITION_ATTRIBUTES, aquisitionAttributes);
		} catch (JAXBException e) {
			String errorMessage = e.getLinkedException().getMessage();
			try {
				LOGGER.error(errorMessage, e);
			} catch (Exception ex) {
				//Very unfortunate but just in case log4j2 API bombed while logging the error.
				e.printStackTrace();
				ex.printStackTrace();
			}
			response = originalMessage;
			context.getMessage().setOutboundProperty(CppConstants.SYSTEM_ERROR, "SignalProcessingEvent message is invalid: " + errorMessage);
		} catch (Exception e) {
			try {
				LOGGER.error(e.getMessage(), e);
			} catch (Exception ex) {
				//Very unfortunate but just in case log4j2 API bombed while logging the error.
				e.printStackTrace();
				ex.printStackTrace();
			}
			response = originalMessage;
			context.getMessage().setOutboundProperty(CppConstants.SYSTEM_ERROR, "SignalProcessingEvent message is invalid: " + e.getMessage());
		} finally {
			// put notification response list in Mule context for later on process, even for errors.
			context.getMessage().setOutboundProperty(SIGNAL_RESPONSE, notification);
			if (!isAuditMessageRequestLogged) {
				//Log into message log.
				try {
					AuditLogger.auditMessage(originalMessage, AuditLogHelper.populateAuditLogVO(context, event));
				} catch (Exception ex) {
					//Very unfortunate but just in case log4j2 API bombed while logging the error.
					ex.printStackTrace();
				}
			}
		}

		return response;
	}

	/**
	 * Delete all Ad Signals if the the feed is configured to serve 
	 * Scheduleless Altcontent and a Scheduleless Altcontent is already in progress.
	 * 
	 * P821-486, P821-487
	 * 
	 * @param context
	 * @param acquisitionPoint
	 */
	private void deleteOrNoopAdSignalsIfAlternateEventIsInProgress(final MuleEventContext context, AcquisitionPoint acquisitionPoint) {
		if (acquisitionPoint != null) {
			AlternateContentVersion alternateContentVersion = acquisitionPoint.getFeedsAlternateContentVersion();
			if (alternateContentVersion != null) {
				switch (alternateContentVersion) {
				case ESNI_224:
					//Encoder Level Media Event, if its running and not ended then respond to PO signal as "NOOP"(As discussed in today's meeting with Shannon/Amit and Rosaiah)
					if (StringUtils.isNotBlank(acquisitionPoint.getZoneIdentity())) {
						SignalStateModel signalState = DataManagerFactory.getInstance().getLastConfirmedEvent(acquisitionPoint.getAcquisitionPointIdentity());
						if (signalState == null) {
							return;
						}
						SCTE224DataManager scte224DataManager = DataManagerFactory.getSCTE224DataManager();
						MediaLedger ledger = scte224DataManager.getAcquisitionPointMediaLedger(acquisitionPoint.getAcquisitionPointIdentity(), signalState.getSignalId());
						if (ledger != null && ledger.isMediaStarted() && !ledger.isMediaEnded()) {
							context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC, TRUE_STR);
						}
					}
					break;
				case SCHEDULELESS:
					//SCHEDULELESS ALT CONTENT VIACOM, if its running and not ended then respond to PO signal as "DELETE"
					SchedulelessAltEventLedger eventLedger = DataManagerFactory.getSchedulelessAltEventDataManager()
							.getAcquisitionPointSchedulelessAltEventLedger(acquisitionPoint.getAcquisitionPointIdentity());
					if (eventLedger != null && !eventLedger.isAltEventEnded()) {
						context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_DELETE_SCC, TRUE_STR);
					}
					break;
				default:
					break;
				}
			}
		}

	}

	private boolean isScheduleLessAltEventRequest(AcquisitionPoint aq, AcquiredSignal signal) {
		if (aq == null) {
			return false;
		}
		Set<Short> segmentIdList = new HashSet<Short>();
		fillAllRecievedSegmentTypeIdInRequest(signal, segmentIdList);
		return AlternateContentVersion.SCHEDULELESS.equals(aq.getFeedsAlternateContentVersion()) && CppConstants.ENABLED.equalsIgnoreCase(aq.getBlackoutConfirmationType())
				&& SegmentType.isValidSchedulelessAltEventSignal(segmentIdList);
	}

	private void fillAllRecievedSegmentTypeIdInRequest(AcquiredSignal signal, Set<Short> segmentIdList) {
		if ((signal != null) && (signal.getSCTE35PointDescriptor() != null) && (signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null)
				&& !signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().isEmpty()) {

			for (SegmentationDescriptorType descriptorInfo : signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo()) {
				Short segmentTypeId = descriptorInfo.getSegmentTypeId() != null ? descriptorInfo.getSegmentTypeId() : 0;
				segmentIdList.add(segmentTypeId);
			}
		}

	}

	private boolean populateDescriptorInfoInContextAndNotification(MuleEventContext context, final SignalProcessingEventType.AcquiredSignal signal,
			SignalProcessingNotificationType notification) {
		boolean isBlackout = false;
		SCTE35PointDescriptorType scte35Pt = null;
		HashMap<String, String> ptsTimes = new HashMap<String, String>();
		HashMap<String, String> ptsAdjustments = new HashMap<String, String>();
		if (signal.getBinaryData() != null) {
			// translate binary data to text format
			scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(signal.getBinaryData().getValue());
			final StringBuilder pts = new StringBuilder();
			final StringBuilder pts_adjustments = new StringBuilder();
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustments);
			ptsTimes.put(signal.getAcquisitionSignalID(), pts.toString());
			ptsAdjustments.put(signal.getAcquisitionSignalID(), pts_adjustments.toString());
			signal.setSCTE35PointDescriptor(scte35Pt);//Setting the XML info here only. The request has been already logged.
		} else if (signal.getSCTE35PointDescriptor() != null) {
			scte35Pt = signal.getSCTE35PointDescriptor();
		}

		notification.getResponseSignal().get(0).setSCTE35PointDescriptor(scte35Pt);
		context.getMessage().setOutboundProperty("acquisitionSignalId", signal.getAcquisitionSignalID());
		context.getMessage().setOutboundProperty("ptsTimes", ptsTimes);
		context.getMessage().setOutboundProperty("ptsAdjustments", ptsAdjustments);

		return isBlackout;
	}

	private boolean isI02BlackoutRequest(final SignalProcessingEventType.AcquiredSignal signal, final SCTE35PointDescriptorType scte35PointDescriptorType,
			final Short segmentTypeId) {
		return SegmentType.isValidBlackoutSignal(segmentTypeId) || SegmentType.isBlackoutAbortRequest(signal, scte35PointDescriptorType);
	}

	private boolean isScte224EventRequest(final Short segmentTypeId) {
		return SegmentType.isValidSCTE224SCCEventSignal(segmentTypeId);
	}

	private boolean isDeliveryRestricted(final SCTE35PointDescriptorType scte35PointDescriptorType) {
		boolean result = false;
		if ((scte35PointDescriptorType.getSegmentationDescriptorInfo() != null) && (scte35PointDescriptorType.getSegmentationDescriptorInfo().size() > 0)) {
			SegmentationDescriptorType segmentDescType = scte35PointDescriptorType.getSegmentationDescriptorInfo().get(0);
			String deliveryNotRestrictedFlag = segmentDescType.getOtherAttributes().get(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG));
			result = "false".equals(deliveryNotRestrictedFlag) || "0".equals(deliveryNotRestrictedFlag);
		}
		return result;
	}

}
