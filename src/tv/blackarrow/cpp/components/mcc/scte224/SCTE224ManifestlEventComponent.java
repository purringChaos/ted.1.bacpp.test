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
package tv.blackarrow.cpp.components.mcc.scte224;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.mcc.BaseManifestlEventComponent;
import tv.blackarrow.cpp.components.mcc.MCCTypes;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecoratorFactory;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType.AcquiredSignal;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConstants;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.NotificationMessageBuilder;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * 
 * handle ESAM MCC request
 * 
 */
public class SCTE224ManifestlEventComponent extends BaseManifestlEventComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(SCTE224ManifestlEventComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {

		StringBuilder content = new StringBuilder();
		String requestSchema = context.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);
		Schema schema = Schema.getSchema(requestSchema);
		AcquisitionPoint aqpt = context.getMessage().getProperty(ACQUISITION_POINT, PropertyScope.OUTBOUND);
		ManifestConfirmConditionEventType event = context.getMessage().getProperty(NOTIFICATION_EVENT, PropertyScope.OUTBOUND);
		Short segmentTypeId = context.getMessage().getProperty(SEGMENT_TYPE_ID, PropertyScope.OUTBOUND);
		ManifestConfirmConditionNotificationType notification = new ManifestConfirmConditionNotificationType();
		StatusCodeType statusCode = new StatusCodeType();
		notification.setStatusCode(statusCode);

		try {			
			// there are many AcquiredSignals in the request.We will process the
			// first signal and ignore the others, as there's no need to
			// implement in forseeable future.
			ManifestConfirmConditionEventType.AcquiredSignal signal = event.getAcquiredSignal().get(0);

			// let's determine whether we will support the passed-in streamType
			List<String> streamTypes = new ArrayList<String>(); // Default,
																// could be
																// linear parity
																// use-case,
																// when this
																// list is
																// empty.

			ManifestResponseType response = new ManifestResponseType();
			notification.setManifestResponse(response);
			response.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
			response.setAcquisitionSignalID(signal.getAcquisitionSignalID());
			//PreCondition2: Invalid stream times return back
			if (signal.getStreamTimes() != null && signal.getStreamTimes().getStreamTime() != null && signal.getStreamTimes().getStreamTime().size() > 0) {
				boolean supportedStreamTypes = loadValidStreamTypes(signal.getStreamTimes().getStreamTime(), streamTypes);
				if (!supportedStreamTypes) {
					content = unsupportedStreamTimesErrorResponse(context, schema, event, notification, statusCode);
					return content.toString();
				}
			}

			long currentTime = System.currentTimeMillis();
			long timeInMillis = currentTime + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("The current time with added delta: (" + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis() + ") is :  " + timeInMillis);
			}
			response.getOtherAttributes().put(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME, String.valueOf(timeInMillis));
			
			SCTE35PointDescriptorType scte35Pt= signal.getSCTE35PointDescriptor();
			String signalId = "";
			String incomingUpid = null;
			SegmentationDescriptorType segmentationDescriptorType = null;
			if (scte35Pt != null) {// HLS, HSS, Linear parity use-case.
				// now we got the SCTE35PointDescriptorType object
				// we will retrieve the SegmentationDescriptorInfo section for
				// late use
				if (scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo().size() > 0) {
					segmentationDescriptorType = scte35Pt.getSegmentationDescriptorInfo().get(0);
				}
				//PreCondition4: Segmentation Descriptor Info Present
				if (segmentationDescriptorType == null) {
					content = segmentationDescriptorInfoNotPresentErrorResponse(context, schema, event, notification, statusCode);
					return content.toString();
				}

				byte[] upidBinary = segmentationDescriptorType.getUpid();
				
				if (upidBinary != null) {
					incomingUpid = new HexBinaryAdapter().marshal(upidBinary);
					signalId = ESAMHelper.getSignalIdFromUPIDHexString(incomingUpid);
				} else { //If the previous SCC request was an abort request than the SCC response will not contain the UPID so grab it from signal level.
					signalId = signal.getSignalPointID();
				}
				response.setSignalPointID(signalId);
			}

			// Now process the body of the response
			String signalIdForFindingMediaLedger = NotificationMessageBuilder.removeTerritoryUpdateCounterFromSignalIdIfPresent(signalId);
			
			MediaLedger mediaLedger = DataManagerFactory.getSCTE224DataManager().getAcquisitionPointMediaLedger(aqpt.getAcquisitionPointIdentity(), signalIdForFindingMediaLedger);
			MediaTransaction startMediaTransaction = mediaLedger != null ? mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC): null;
			if (mediaLedger != null && startMediaTransaction!=null) {
				String hlsInterfaceType = aqpt.getBaHlsInterfaceTypeExternalRef();
				String hssInterfaceType = aqpt.getBaHssInterfaceTypeExternalRef();
				schema = decorateAllManifestResponse(signal, mediaLedger, response, streamTypes, hlsInterfaceType, hssInterfaceType, aqpt, schema, segmentTypeId);
			} else {
				boolean isHLSTemplateAqpt = false;
				if (aqpt != null && aqpt.getBaHlsInterfaceTypeExternalRef() != null && CppConstants.INTERFACE_TEMPLATE.equals(aqpt.getBaHlsInterfaceTypeExternalRef())) {
					isHLSTemplateAqpt = true;
				}
				boolean isOnlyHLSStreamType = false;
				if (signal != null && signal.getStreamTimes() != null) {
					isOnlyHLSStreamType = ESAMHelper.isOnlyHLSStreamType(signal.getStreamTimes().getStreamTime());
				} else {
					isOnlyHLSStreamType = true;
				}
				if (aqpt != null && mediaLedger == null && isHLSTemplateAqpt && isOnlyHLSStreamType
						&& (SegmentType.PROGRAM_START.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()
								|| SegmentType.PROGRAM_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId())) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Unable to find cpo for signal id : " + signalId);
					}
					String hlsInterfaceType = aqpt.getBaHlsInterfaceTypeExternalRef();
					String hssInterfaceType = aqpt.getBaHssInterfaceTypeExternalRef();
					schema = decorateAllManifestResponse(signal, mediaLedger, response, streamTypes, hlsInterfaceType, hssInterfaceType, aqpt, schema, segmentTypeId);
				} else {					
					if (mediaLedger == null) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Unable to find cpo for signal id : " + signalId);
						}
					}

					//Do not add No-Op if the only streamType asked is DASH
					if (streamTypes != null && !streamTypes.isEmpty() && !(streamTypes.size() == 1 && streamTypes.get(0).equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH))) {// DASH
						statusCode.getNote().add("No-Op");
					}
					// In this case, we will remove the Signal Point Id from the
					// response.
					response.setSignalPointID(null);
					//Set Dummy Response For PO End Case for DASH
					schema = decorateOnlyDASHManifestResponse(signal, mediaLedger, response, streamTypes, null, null, aqpt, schema, segmentTypeId);

				}
			}
			
			/**
			 * Packager need the original signal point id for it to identify which signal to replace. Whereas the MDC need the new
			 * signal id with counter appended to it so that it knows that it need to invalidate the old cache. 
			 */
			response.setSignalPointID(signalIdForFindingMediaLedger);
			
			statusCode.setClassCode("0");
		} catch (StringIndexOutOfBoundsException e) {
			LOGGER.error(()->"Invalid MCC binary received: " + e.getMessage());
			statusCode.setClassCode("1");
			statusCode.setDetailCode("1");
			statusCode.getNote().add(e.getMessage());
		} catch (RuntimeException e) {
			LOGGER.error(()->"Invalid MCC Request received: " + e.getMessage());
			statusCode.setClassCode("1");
			statusCode.setDetailCode("1");
			statusCode.getNote().add(e.getMessage());
		} catch (Exception ex) {
			LOGGER.error(()->ex.getMessage(), ex);
			statusCode.setClassCode("1");
			statusCode.setDetailCode("1");
			statusCode.getNote().add(ex.getMessage());
		} finally {
			//Clear the cache maintained for this request if any.
			SCCMCCThreadLocalCache.clearMyCache();
		}
		
		content.append(objectToXML(notification, schema));
		//Log into message log.
		AuditLogger.auditMessage(content.toString(), AuditLogHelper.populateAuditLogVO(context, event));
		if (LOGGER.isInfoEnabled()) {
			LOGGER.debug("MCC response: \n " + content.toString());
		}

		return content.toString();
	}

	private Schema decorateAllManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final MediaLedger mediaLedger,
			final ManifestResponseType response, List<String> streamTypes, String hlsInterfaceType, String hssInterfaceType, AcquisitionPoint acquisitionPoint, Schema schema, Short segmentTypeId)
			throws Exception {
		if (streamTypes == null || streamTypes.isEmpty()) {// add HLS as
			// default
			streamTypes = new ArrayList<>();
			streamTypes.add(CppConstants.SIGNAL_STREAM_TYPE_HLS);
		}

		String signalId = response.getSignalPointID();
		// Segment Modify for HLS
		String apid = "";
		String inSignalId = "";
		String utc = "";

		long currentTimewithDelta = Long.parseLong(response.getOtherAttributes().get(MCCTemplateConstants.MCC_TEMPLATE_UTC_TIME));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		utc = sdf.format(currentTimewithDelta);

		ManifestInfo info = new ManifestInfo();
		info.setSignalId(signalId);
		info.setApid(apid);
		info.setFeed(acquisitionPoint.getFeedExternalRef());
		info.setNetwork(acquisitionPoint.getNetworkExternalRef());
		info.setUtc(utc);
		info.setInSignalId(inSignalId);
		setMCCResponseDuration(response, acquisitionPoint, NotificationMessageBuilder.removeTerritoryUpdateCounterFromSignalIdIfPresent(signalId), 
				mediaLedger, signal);
		info.setSchema(schema);
		for (String streamType : streamTypes) {
			ManifestResponseDecorator decorator = ManifestResponseDecoratorFactory.getManifestResponseDector(info, schema, signal, streamType, hlsInterfaceType, hssInterfaceType,
					signalId, MCCTypes.SCTE224, segmentTypeId, acquisitionPoint);
			try {
				decorator.decorateManifestResponse(response, signal, mediaLedger, info, acquisitionPoint, segmentTypeId);
			} catch (Exception e) {
				//if this is the only stream type queried for then we throw the exception for whole request, else continue with other decorators.
				if (streamTypes.size() == 1) {
					throw e;
				}
			}
		}

		return info.getSchema();
	}
	
	private void setMCCResponseDuration(final ManifestResponseType response, AcquisitionPoint aqpt, String signalId,
			final MediaLedger mediaLedger, AcquiredSignal signal)
			throws DatatypeConfigurationException {
		Media media = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(aqpt.getFeedExternalRef(),
				signalId);

		if (media != null) {
			MediaPoint endMediaPoint = null;
			MediaTransaction startTransaction = null;
			MediaTransaction endTransaction = null;
			long duration = 0l;

			for (MediaPoint mp : media.getMediaPoints()) {
				if (mp.getMatchSignal().getSegmentationTypeIds()
						.contains(SegmentType.PROGRAM_END.getSegmentTypeId())) {
					endMediaPoint = mp;
				}
			}

			if (mediaLedger != null) {
				startTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
				endTransaction = mediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);

				if (endTransaction != null && startTransaction != null) {
					duration = endTransaction.getSignalTimeInMS() - startTransaction.getSignalTimeInMS();
				} else if (startTransaction != null) {
					if (isMatchTimePresent(endMediaPoint)) {
						duration = (endMediaPoint.getMatchTimeInMS()
								+ (endMediaPoint.getMatchSignal() != null
										&& endMediaPoint.getMatchSignal().getSignalToleranceDurationInMS() != null
												? endMediaPoint.getMatchSignal().getSignalToleranceDurationInMS() : 0))
								- startTransaction.getSignalTimeInMS();
					} else {
						duration = endMediaPoint.getExpiresTimeInMS() - startTransaction.getSignalTimeInMS();
					}
				}
			}
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		}
	}
	
	private boolean isMatchTimePresent(MediaPoint mediaPoint) {
		if (Objects.nonNull(mediaPoint)) {
			return mediaPoint.getMatchTimeInMS() != null && mediaPoint.getMatchTimeInMS() > 0;
		}
		return false;
	}
	
	private Schema decorateOnlyDASHManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final MediaLedger mediaLedger,
			final ManifestResponseType response, List<String> streamTypes, String hlsInterfaceType, String hssInterfaceType, AcquisitionPoint acquisitionPoint, Schema schema, Short segmentTypeId)
			throws Exception {
		if (streamTypes == null || streamTypes.isEmpty()) {// add HLS as
			return schema;
		}

		// Segment Modify for HLS
		String apid = "";
		String inSignalId = "";
		String utc = "";
		Date utcDate = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		utc = sdf.format(utcDate);

		ManifestInfo info = new ManifestInfo();
		info.setApid(apid);
		info.setFeed(acquisitionPoint.getFeedExternalRef());
		info.setNetwork(acquisitionPoint.getNetworkExternalRef());
		info.setUtc(utc);
		info.setInSignalId(inSignalId);
		populateDuration(response, mediaLedger);

		info.setSchema(schema);

		for (String streamType : streamTypes) {
			if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH)) {// DASH
				ManifestResponseDecorator decorator = ManifestResponseDecoratorFactory.getManifestResponseDector(info, schema, signal, streamType, hlsInterfaceType,
						hssInterfaceType, null, MCCTypes.SCTE224, segmentTypeId, acquisitionPoint);
				decorator.decorateManifestResponse(response, signal, mediaLedger, info, acquisitionPoint,segmentTypeId);
			}
		}
		return info.getSchema();
	}
	
	private long populateDuration(final ManifestResponseType response, final MediaLedger mediaLedger) throws DatatypeConfigurationException {

		Long duration = null;
		if (mediaLedger.isMediaStarted() && !mediaLedger.isMediaEnded()) {
			MediaTransaction startMediaTransaction = mediaLedger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			duration = startMediaTransaction.getTotalDurationInMS();

		} else if (mediaLedger.isMediaEnded()) {
			MediaTransaction endMediaTransaction = mediaLedger.getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
			duration = endMediaTransaction.getTotalDurationInMS();

		}

		if (duration != null) {
			response.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		}
		return duration;
	}
}
