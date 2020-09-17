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

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.annotations.param.Payload;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signal.signaling.StatusCodeType;
import tv.blackarrow.cpp.signaling.ObjectFactory;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SpliceCommandType;

/**
 * 
 * generate ESAM SCC response
 *
 */
public class SignalResponseComponent {
	private static final Logger LOGGER = LogManager.getLogger(SignalResponseComponent.class);
	private static final boolean INFO_ENABLED = LOGGER.isInfoEnabled();
	
	public String buildResponse(final @Payload String signalId) {
		LOGGER.debug(()->"Request properties. signalId=" + signalId);

		final MuleEventContext ctx = RequestContext.getEventContext();
		final SignalProcessingNotificationType signalNotification = (SignalProcessingNotificationType)ctx.getMessage().
									getProperty("signal_response", PropertyScope.OUTBOUND);
		
		final StatusCodeType statusCode = new StatusCodeType();
		statusCode.setClassCode("0");
		if(signalId.length() > 0) {  // confirmed signal ID
			signalNotification.getResponseSignal().get(0).setSignalPointID(signalId);
			//statusCode.getNote().add("replace signal");  // optional  (comment out so RGB's transcoder can work)

			final String upidStr = ESAMHelper.generateUpidString(signalId);
			final byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);

			final SCTE35PointDescriptorType scte35Des = signalNotification.getResponseSignal().get(0).getSCTE35PointDescriptor();
			if(scte35Des != null && scte35Des.getSpliceCommandType() == SpliceCommandType.SPLICE_INSERT.getCommandtype()) {  // add one segmentation into the response
				long duration = Long.valueOf(ctx.getMessage().getProperty(CppConstants.SEGMENT_DURATION, PropertyScope.OUTBOUND).toString()) * 1000;
				LOGGER.debug(()->"duration " + duration);
				
				final SegmentationDescriptorType segment = generateSegment(upid, duration);
				scte35Des.getSegmentationDescriptorInfo().add(segment);
			} else if(scte35Des != null && scte35Des.getSpliceCommandType() == SpliceCommandType.TIME_SIGNAL.getCommandtype()) {
				for(SegmentationDescriptorType seg: scte35Des.getSegmentationDescriptorInfo()) {
					seg.setUpid(upid);
				}
			}
			
			final BinarySignalType binarySignal = signalNotification.getResponseSignal().get(0).getBinaryData();
			if(binarySignal != null) {
				generateBinaryData(ctx, upid, binarySignal);
			}
		} else {
			signalNotification.getResponseSignal().get(0).setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_DELETE);
			//statusCode.getNote().add("signal: " + HostSettingBean.getInstance().getUnconfirmedAction());  // optional 
			signalNotification.getResponseSignal().get(0).setSCTE35PointDescriptor(null);
			signalNotification.getResponseSignal().get(0).setStreamTimes(null);
		}
		
		// statusCode.setDetailCode(""); // if any error
		signalNotification.setStatusCode(statusCode);
		
		final String resp =  objectToXML(signalNotification);
		
		//Log into message log.
		AuditLogger.auditMessage(resp, AuditLogHelper.populateAuditLogVO(ctx, signalNotification));
		
		if(INFO_ENABLED) {
			LOGGER.debug(()->"SCC Response: \n" + resp);
		}
		
		// clean all head properties
		RequestContext.getEventContext().getMessage().clearProperties();
		
		return resp;
	}

	private void generateBinaryData(final MuleEventContext ctx, final byte[] upid,
			final BinarySignalType binarySignal) {
		try {
			// set UPID based on the Signal ID			
			final SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			final byte[] encoded = Base64.encodeBase64(binarySignal.getValue());
			final StringBuilder pts = new StringBuilder(); 
			final StringBuilder pts_adjustment = new StringBuilder(); 
			Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustment);
			
			if(scte35Pt.getSegmentationDescriptorInfo() != null && scte35Pt.getSegmentationDescriptorInfo().size() > 0) {
				scte35Pt.getSegmentationDescriptorInfo().get(0).setUpid(upid);
			} else {
				long duration = Long.valueOf(ctx.getMessage().getProperty(CppConstants.SEGMENT_DURATION, PropertyScope.OUTBOUND).toString()) * 1000;
				SegmentationDescriptorType segment = generateSegment(upid, duration);
				scte35Pt.getSegmentationDescriptorInfo().add(segment);
			}
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, pts.toString(), pts_adjustment.toString());
			binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));  
		} catch(Exception ex) {
			LOGGER.warn(()->"binary data process issue", ex);
		}
	}

	private SegmentationDescriptorType generateSegment(final byte[] upid, final long duration) {
		final SegmentationDescriptorType segment = new SegmentationDescriptorType();

		try {
			segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		} catch (DatatypeConfigurationException e) {
			LOGGER.error(()->e.getMessage() + "duration " + duration, e);
		}
		segment.setSegmentationEventCancelIndicator(false);
		segment.setSegmentEventId(System.currentTimeMillis() & 0x3fffffff);  // make it unique
		segment.setSegmentNum((short)0);
		segment.setSegmentsExpected((short)0);
		segment.setSegmentTypeId((short)9);
		segment.setUpid(upid);
		segment.setUpidType((short)40);
		return segment;
	}

	private String objectToXML(final SignalProcessingNotificationType signalNotification) {
		JAXBContext jaxbCxt;
		JAXBElement<SignalProcessingNotificationType> jxbElement = null;
		StringWriter writer = new StringWriter();
		
		try {
			jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();//JAXBContext.newInstance(SignalProcessingNotificationType.class.getPackage().getName());
			Marshaller marshaller	= jaxbCxt.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			jxbElement = factory.createSignalProcessingNotification(signalNotification);
			marshaller.marshal(jxbElement, writer);					
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		
		return writer.toString();
	}
}
