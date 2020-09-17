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
package tv.blackarrow.cpp.components.mcc.other;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.BlackoutResponseDurationHandler;
import tv.blackarrow.cpp.components.mcc.BaseManifestlEventComponent;
import tv.blackarrow.cpp.components.mcc.MCCTypes;
import tv.blackarrow.cpp.components.mcc.ManifestInfo;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecorator;
import tv.blackarrow.cpp.components.mcc.ManifestResponseDecoratorFactory;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.manifest.ManifestResponseType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimesType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * 
 * handle ESAM MCC request
 * 
 */
public class ManifestlEventComponent extends BaseManifestlEventComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(ManifestlEventComponent.class);

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

			SCTE35PointDescriptorType scte35Pt= signal.getSCTE35PointDescriptor();
			String signalId = "";
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
					String upidHex = new HexBinaryAdapter().marshal(upidBinary);
					signalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);
				} else { //If the previous SCC request was an abort request than the SCC response will not contain the UPID so grab it from signal level.
					signalId = signal.getSignalPointID();
				}

				response.setSignalPointID(signalId);

				// Duration 
				// response.setDuration(value);
				setDurationInResponse(aqpt, signalId, segmentationDescriptorType);
				
				// in-point request received, use original signal-id at program start or opportunity start
				if (CppConstants.SCC_INPOINTS_COMCAST_P3.equals(CppConfigurationBean.getInstance().getSccInpointReponse())) {
					if (SegmentType.PLACEMENT_OPPORTUNITY_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()
							|| SegmentType.PROGRAM_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()
							|| SegmentType.PROVIDER_ADVERTISEMENT_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()
							|| SegmentType.DISTRIBUTOR_ADVERTISEMENT_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()) {
						String orignal_signalId = DataManagerFactory.getInstance().getInPointsSignal(signalId);
						if (orignal_signalId != null) {
							signalId = orignal_signalId;
						}
					}
				}
			}

			// Now process the body of the response
			ConfirmedPlacementOpportunity cpo = DataManagerFactory.getInstance().getConfirmedPlacementOpportunity(signalId);

			// Let's read the configuration to decide, how to respond.
			DataManager dataManager = DataManagerFactory.getInstance();
			aqpt = dataManager.getAcquisitionPoint(signal.getAcquisitionPointIdentity());
			if (cpo != null && aqpt != null) {

				// Stream Types is required if P5 is configured
				if (CppConstants.INTERFACE_COMCAST_P5.equals(aqpt.getBaHlsInterfaceTypeExternalRef()) && (streamTypes == null || streamTypes.isEmpty())) {
					// Stream Types is required
					statusCode.setClassCode("1");
					statusCode.setDetailCode("3");
					statusCode.getNote().add("HLS or HSS Stream Type is required in the request.");
					content.append(objectToXML(notification, schema));
					//Log into message log.
					AuditLogger.auditMessage(content.toString(), AuditLogHelper.populateAuditLogVO(context, event));
					return content.toString();
				}

				long mccStartTime = DataManagerFactory.getInstance().getBlackoutMccStartTime(aqpt.getAcquisitionPointIdentity(), cpo.getSignalId());
				if (mccStartTime == 0) {
					DataManagerFactory.getInstance().putBlackoutMccStartTime(aqpt.getAcquisitionPointIdentity(), cpo.getSignalId(),
							signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime().getTime());
				}

				String hlsInterfaceType = aqpt.getBaHlsInterfaceTypeExternalRef();
				String hssInterfaceType = aqpt.getBaHssInterfaceTypeExternalRef();
				//CS2-387: changes in UTC time for PO ABort
				long currentSystemTimeWithAddedDelta = System.currentTimeMillis() + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis();

				if (cpo != null && cpo.isAborted()) {
					signal.setUTCPoint(SCCResponseUtil.generateUTCPoint(currentSystemTimeWithAddedDelta));
					adjustSignalTimeOffset(signal.getStreamTimes(), aqpt);
				}
				schema = decorateAllManifestResponse(signal, cpo, response, streamTypes, hlsInterfaceType, hssInterfaceType, aqpt, schema, segmentTypeId);

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
				if (aqpt != null && cpo == null && isHLSTemplateAqpt && isOnlyHLSStreamType
						&& (SegmentType.PROGRAM_START.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId()
								|| SegmentType.PROGRAM_END.getSegmentTypeId() == segmentationDescriptorType.getSegmentTypeId())) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Unable to find cpo for signal id : " + signalId);
					}
					String hlsInterfaceType = aqpt.getBaHlsInterfaceTypeExternalRef();
					String hssInterfaceType = aqpt.getBaHssInterfaceTypeExternalRef();
					schema = decorateAllManifestResponse(signal, cpo, response, streamTypes, hlsInterfaceType, hssInterfaceType, aqpt, schema, segmentTypeId);
				} else {
					if (aqpt == null) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(()->"Unable to find acquisition point: " + signal.getAcquisitionPointIdentity());
						}
					}
					if (cpo == null) {
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
					schema = decorateOnlyDASHManifestResponse(signal, cpo, response, streamTypes, null, null, aqpt, schema, segmentTypeId);

				}
			}
			statusCode.setClassCode("0");
		} catch (javax.xml.bind.UnmarshalException e) {
			LOGGER.error(e.getLinkedException().getMessage(), e);
			statusCode.setClassCode("1");
			statusCode.setDetailCode("1");
			statusCode.getNote().add(e.getLinkedException().getMessage());
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

	private void setDurationInResponse(AcquisitionPoint aqpt, String signalId, SegmentationDescriptorType segmentationDescriptorType) {
		Media media = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(aqpt.getFeedExternalRef(), signalId);
//		scte35Pt.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()
		if (SegmentType.isProgramStartSignal(segmentationDescriptorType.getSegmentTypeId())) {
			Optional<MediaPoint> startMediaPoint = media.getMediaPoints().stream().filter(mp -> mp.getMatchSignal()
					.getSegmentationTypeIds().contains(SegmentType.PROGRAM_START.getSegmentTypeId())).findFirst();
			if (startMediaPoint.isPresent()) {
				MediaPoint mediaPoint = startMediaPoint.get();
				if (isMatchTimeAndSignalTolerancePresent(mediaPoint)) {
					
				} else {
					Optional<MediaPoint> endMediaPoint = media.getMediaPoints().stream().filter(mp -> mp.getMatchSignal()
							.getSegmentationTypeIds().contains(SegmentType.PROGRAM_END.getSegmentTypeId())).findFirst();
					
				}
			}
		} else if (SegmentType.isProgramEndSignal(segmentationDescriptorType.getSegmentTypeId())) {
			
		} else if (SegmentType.isContentIdentificationSignal(segmentationDescriptorType.getSegmentTypeId())) {
			
		} else {
			
		}
	}
	
	private boolean isMatchTimeAndSignalTolerancePresent(MediaPoint mediaPoint) {
		return mediaPoint.getMatchTimeInMS() != null && mediaPoint.getMatchTimeInMS() > 0
				&& mediaPoint.getMatchSignal() != null
				&& mediaPoint.getMatchSignal().getSignalToleranceDurationInMS() != null
				&& mediaPoint.getMatchSignal().getSignalToleranceDurationInMS() > 0;
	}
	
	private Schema decorateAllManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo,
			final ManifestResponseType response, List<String> streamTypes, String hlsInterfaceType, String hssInterfaceType, AcquisitionPoint acquisitionPoint, Schema schema,
			Short segmentTypeId) throws Exception {
		if (streamTypes == null || streamTypes.size() == 0) {// add HLS as
			// default
			streamTypes = new ArrayList<String>();
			streamTypes.add(CppConstants.SIGNAL_STREAM_TYPE_HLS);
		}

		String signalId = response.getSignalPointID();
		// Segment Modify for HLS
		String apid = "", utc = "", inSignalId = "";
		Date utcDate = signal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		utc = sdf.format(utcDate);

		ManifestInfo info = new ManifestInfo();
		info.setSignalId(signalId);
		info.setApid(apid);
		info.setFeed(acquisitionPoint.getFeedExternalRef());
		info.setNetwork(acquisitionPoint.getNetworkExternalRef());
		info.setUtc(utc);
		info.setInSignalId(inSignalId);
		BlackoutResponseDurationHandler.setMCCResponseDuration(signal, cpo, response, info, acquisitionPoint);
		info.setSchema(schema);
		for (String streamType : streamTypes) {
			ManifestResponseDecorator decorator = ManifestResponseDecoratorFactory.getManifestResponseDector(info, schema, signal, streamType, hlsInterfaceType, hssInterfaceType,
					signalId, MCCTypes.OTHER, segmentTypeId, acquisitionPoint);
			try {
				decorator.decorateManifestResponse(response, signal, cpo, info, acquisitionPoint);
			} catch (Exception e) {
				//if this is the only stream type queried for then we throw the exception for whole request, else continue with other decorators.
				if (streamTypes.size() == 1) {
					throw e;
				}
			}
		}

		return info.getSchema();
	}

	private Schema decorateOnlyDASHManifestResponse(final ManifestConfirmConditionEventType.AcquiredSignal signal, final ConfirmedPlacementOpportunity cpo,
			final ManifestResponseType response, List<String> streamTypes, String hlsInterfaceType, String hssInterfaceType, AcquisitionPoint acquisitionPoint, Schema schema,
			Short segmentTypeId) throws Exception {
		if (streamTypes == null || streamTypes.size() == 0) {// add HLS as
			return schema;
		}

		// Segment Modify for HLS
		String apid = "", utc = "", inSignalId = "";
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
		BlackoutResponseDurationHandler.setMCCResponseDuration(signal, cpo, response, info, acquisitionPoint);
		info.setSchema(schema);

		for (String streamType : streamTypes) {
			if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH)) {// DASH
				ManifestResponseDecorator decorator = ManifestResponseDecoratorFactory.getManifestResponseDector(info, schema, signal, streamType, hlsInterfaceType,
						hssInterfaceType, null, MCCTypes.OTHER, segmentTypeId, acquisitionPoint);
				decorator.decorateManifestResponse(response, signal, cpo, info, acquisitionPoint);
			}
		}
		return info.getSchema();
	}

	protected void adjustSignalTimeOffset(StreamTimesType streamTimesType, AcquisitionPoint aqpt) {
		long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : ConfirmedPlacementOpportunity.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);
		if (streamTimesType != null) {
			Scte35BinaryUtil.adjustPtsTimeInStream(streamTimesType, signalTimeOffset);
		}

	}

}
