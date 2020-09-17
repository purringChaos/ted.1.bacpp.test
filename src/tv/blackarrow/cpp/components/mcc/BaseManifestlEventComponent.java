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

import java.util.List;

import javax.xml.namespace.QName;

import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionNotificationType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signal.signaling.StreamTimeType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;

/**
 * 
 * handle ESAM MCC request
 * 
 */
public abstract class BaseManifestlEventComponent {

	protected static ThreadLocal<Boolean> HACKED = new ThreadLocal<Boolean>();

	protected StringBuilder segmentationDescriptorInfoNotPresentErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"/ManifestConfirmConditionEvent/AcquiredSignal/SCTE35PointDescriptor/SegmentationDescriptorInfo is required.");
	}

	protected StringBuilder unsupportedSpliceCommandTypeErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"/ManifestConfirmConditionEvent/AcquiredSignal/SCTE35PointDescriptor@spliceCommandType must have spliceCommandType as 5 or 6.");
	}

	protected StringBuilder spliceInsertAtWrongPlaceErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"/ManifestConfirmConditionEvent/AcquiredSignal/SCTE35PointDescriptor/SpliceInsert must not be present for spliceCommandType 6.");
	}

	protected StringBuilder spliceSectionNotPresentErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"/ManifestConfirmConditionEvent/AcquiredSignal/SCTE35PointDescriptor/SpliceInsert must be present for spliceCommandType 5.");
	}

	protected StringBuilder unsupportedStreamTimesErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"Unsupported stream type: HDS found in the MCC request. Currently HLS, and HSS are only supported.");
	}

	protected StringBuilder noAcquiredSignalErrorResponse(final MuleEventContext context, Schema schema, ManifestConfirmConditionEventType event,
			ManifestConfirmConditionNotificationType notification, StatusCodeType statusCode) {
		return sendErrorResponse(context, schema, event, notification, statusCode, 
				"No AquiredSignal to confirm.");
	}
	
	protected StringBuilder sendErrorResponse(final MuleEventContext context, final Schema schema, final ManifestConfirmConditionEventType event, 
			final ManifestConfirmConditionNotificationType notification, final StatusCodeType statusCode, final String message) {
		StringBuilder content = new StringBuilder();
		statusCode.setClassCode("1");
		statusCode.setDetailCode("3");
		statusCode.getNote().add(message);
		content.append(objectToXML(notification, schema));
		//Log into message log.
		AuditLogger.auditMessage(content.toString(), AuditLogHelper.populateAuditLogVO(context, event));
		return content;
	}
	
	/**
	 * generate XML content
	 * 
	 * @param notification
	 * @return
	 */
	protected String objectToXML(final ManifestConfirmConditionNotificationType notification, Schema schema) {
		String response = schema.getResponseHandler().generateMCCResponse(notification);

		// NAMESPACE_HACK : ought to be removed once not needed
		Boolean hacked = HACKED.get();
		if (hacked != null && hacked.booleanValue()) {
			return response.replace(CppConstants.NEW_MCC_NAMESPACE, CppConstants.OLD_MCC_NAMESPACE);
		}

		return response;
	}
	
	protected boolean loadValidStreamTypes(List<StreamTimeType> streamTimes, List<String> streamTypes) {
		for (StreamTimeType streamTime : streamTimes) {
			String streamType = streamTime.getTimeType();
			if (streamType == null) { // Sometime JAXB unmarshals stream times
										// into the other attributes.
				streamType = streamTime.getOtherAttributes().get(QName.valueOf("TimeType"));
			}
			if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HLS) || streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HSS)
					|| streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_DASH)) {
				streamTypes.add(streamType);
			} else if (streamType.equalsIgnoreCase(CppConstants.SIGNAL_STREAM_TYPE_HDS)) {
				return false;
			}
		}
		return true;
	}
}
