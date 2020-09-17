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
package tv.blackarrow.cpp.components.mcc;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;
import static tv.blackarrow.cpp.components.util.ContextConstants.TRUE_STR;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCCMCCThreadLocalCache;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;


/**
 *
 * handle ESAM MCC request
 *
 */
public class GeneralManifestEventComponent extends BaseManifestlEventComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(GeneralManifestEventComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {

		String originalMessage = context.getMessageAsString();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.debug(()->"MCC request:\n" + originalMessage);
		}
		StringBuilder content = new StringBuilder();

		String requestSchema = context.getMessage().getProperty("schema", PropertyScope.INVOCATION);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.debug(()->"Receiving request in schema " + requestSchema);
		}

		Schema schema = Schema.getSchema(requestSchema);

		// NAMESPACE_HACK : ought to be removed once not needed
		String nameSpaceChangedMessage = originalMessage;
		if (originalMessage.contains(CppConstants.OLD_MCC_NAMESPACE)) {
			nameSpaceChangedMessage = originalMessage.replace(CppConstants.OLD_MCC_NAMESPACE, CppConstants.NEW_MCC_NAMESPACE);
			HACKED.set(Boolean.TRUE);
		}

		AcquisitionPoint aqpt = null;
		ManifestConfirmConditionNotificationType notification = new ManifestConfirmConditionNotificationType();
		StatusCodeType statusCode = new StatusCodeType();
		notification.setStatusCode(statusCode);
		ManifestConfirmConditionEventType event = null;
		try {

			//Cache is used to return the documents for a single key from the cache for this request, rather than every time getting it from Couchbase.
			SCCMCCThreadLocalCache.cachingRequired();
			event = schema.getRequestHandler().parseMCCRequest(nameSpaceChangedMessage);

			context.getMessage().setProperty(NOTIFICATION_EVENT, event, PropertyScope.OUTBOUND);
			int asSize = event.getAcquiredSignal().size();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.debug(()->"How many AcquiredSignals " + asSize);
			}
			if (asSize == 0) {
				// no AcquiredSignal in the request
				statusCode.setClassCode("1");
				statusCode.setDetailCode("3");
				statusCode.getNote().add("No AquiredSignal to confirm.");
				content.append(objectToXML(notification, schema));
				//Log into message log.
				AuditLogger.auditMessage(content.toString(), AuditLogHelper.populateAuditLogVO(context, event));
				return content.toString();
			}

			// there are many AcquiredSignals in the request.We will process the
			// first signal and ignore the others, as there's no need to
			// implement in forseeable future.
			ManifestConfirmConditionEventType.AcquiredSignal signal = event.getAcquiredSignal().get(0);
			aqpt = DataManagerFactory.getInstance().getAcquisitionPoint(signal.getAcquisitionPointIdentity());

			// figure out the SegmentationDescriptorInfo object for this MCC
			// request
			// which should be presented because we send this in our SCC
			// notification response
			SCTE35PointDescriptorType scte35Pt = signal.getSCTE35PointDescriptor();
			if (signal.getSCTE35PointDescriptor() == null) {
				// binary format
				// we need to get the SCTE35PointDescriptorType by decoding the
				// binary data
				if (signal.getBinaryData() != null) {
					if (signal.getBinaryData() != null && signal.getBinaryData().getValue() != null) {
						scte35Pt = new SCTE35PointDescriptorType();
					}
					final byte[] encoded = Base64.encodeBase64(signal.getBinaryData().getValue());
					final StringBuilder pts = new StringBuilder();
					Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, new StringBuilder());
				}
				// set the scte 35 point descriptor from binary data
				signal.setSCTE35PointDescriptor(scte35Pt);
			}

			if(isScheduleLessBlackout(aqpt, scte35Pt)) {
				LOGGER.debug("AP " + aqpt.getAcquisitionPointIdentity() + " has been configured to serve scheduleless blackout"
						+ ". For scheduless blackout it is assumed that MCC should not come to ESAM Runtime. If we receive an"
						+ " MCC request on such APs ESS shall always return a NOOP response.");
				context.getMessage().setProperty(ContextConstants.SCHEMA, schema, PropertyScope.OUTBOUND);
				context.getMessage().setProperty(NOTIFICATION_EVENT, event, PropertyScope.OUTBOUND);
				context.getMessage().setProperty(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP, ContextConstants.TRUE_STR, PropertyScope.OUTBOUND);
				return "";
			}

			long spliceCommandType = scte35Pt.getSpliceCommandType();

			//PreCondition3: Verify if SpliceCommandType and structure correct
			if (spliceCommandType == 5 || spliceCommandType == 6) {
				boolean spliceInsertSection = (scte35Pt.getSpliceInsert() != null);
				if (spliceCommandType == 5 && !(spliceInsertSection)) {
					content = spliceSectionNotPresentErrorResponse(context, schema, event, notification, statusCode);
					return content.toString();
				} else if (spliceCommandType == 6 && spliceInsertSection) {
					content = spliceInsertAtWrongPlaceErrorResponse(context, schema, event, notification, statusCode);
					return content.toString();
				}
			} else {
				content = unsupportedSpliceCommandTypeErrorResponse(context, schema, event, notification, statusCode);
				return content.toString();
			}


			Short segmentTypeID = signal.getSCTE35PointDescriptor() != null && signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null
					&& !signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().isEmpty()
					&& signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId() != null
							? signal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().get(0).getSegmentTypeId()
							: 0;

			if (aqpt.isFeedHasAlternateContentEnabled()) {
				switch (aqpt.getFeedsAlternateContentVersion()) {
				case ESNI_224:
					if (isScte224MCCEventRequest(segmentTypeID)) {
						context.getMessage().setOutboundProperty(CppConstants.SCTE224, TRUE_STR);
					}
					break;
				default:
					break;
				}

			}
			//Some more generic information
			context.getMessage().setOutboundProperty(SEGMENT_TYPE_ID, segmentTypeID);
			context.getMessage().setProperty(ContextConstants.SCHEMA, schema, PropertyScope.OUTBOUND);
			context.getMessage().setProperty(ACQUISITION_POINT, aqpt, PropertyScope.OUTBOUND);
			statusCode.setClassCode("0");
		} catch (javax.xml.bind.UnmarshalException e) {
			LOGGER.error(()->e.getLinkedException().getMessage(), e);
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
			// put notification response list in Mule context for later on process, even for errors.
			context.getMessage().setOutboundProperty(SIGNAL_RESPONSE, notification);
			AuditLogger.auditMessage(originalMessage, AuditLogHelper.populateAuditLogVO(context, event));
		}

		return content.toString();
	}

	private boolean isScheduleLessBlackout(AcquisitionPoint aqpt, SCTE35PointDescriptorType scte35Pt) {
		boolean hasPOSignals = false;
		boolean hasProgramSignalsExceptContentId = false;
		boolean hasProgramSignalContentId = false;
		if(aqpt != null &&
				AlternateContentVersion.SCHEDULELESS.equals(aqpt.getFeedsAlternateContentVersion()) &&
				scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty()
			) {
			//Schedule less blackouts functionality was implemented for Viacom Feeds. It can have following three kind of request
			// 1. Program End, Program Start & Content Id Segment Descriptor infos in that same descriptor.
			// 2. PO Start, Content Id Segment Descriptor infos in that same descriptor.
			// But Program(except content id) and PO signals are never mixed.
				for(SegmentationDescriptorType segmentationDescriptorType :  scte35Pt.getSegmentationDescriptorInfo()) {
					if(!SegmentType.isContentIdentificationSignal(segmentationDescriptorType.getSegmentTypeId()) &&
							SegmentType.isValidBlackoutSignal(segmentationDescriptorType.getSegmentTypeId())) {
						hasProgramSignalsExceptContentId = true;
					}
					if(SegmentType.isValidPOSignal(segmentationDescriptorType.getSegmentTypeId())) {
						hasPOSignals = true;
					}
					if(SegmentType.isContentIdentificationSignal(segmentationDescriptorType.getSegmentTypeId())) {
						hasProgramSignalContentId = true;
					}
				}
				//So
				//1. If it does not have a PO signal it should be considered as a Program/Blackout request.
				//2. If it has just a content id signal.
				//3. If this has a descriptor with segment type other than PO and content Id then it is a program/blackout request.
				return !hasPOSignals || (!hasPOSignals && !hasProgramSignalsExceptContentId && hasProgramSignalContentId) ||
						hasProgramSignalsExceptContentId;
			}
		return false;
	}

	private boolean isScte224MCCEventRequest(final Short segmentTypeId) {
		return SegmentType.isValidSCTE224SCCEventSignal(segmentTypeId);
	}

}
