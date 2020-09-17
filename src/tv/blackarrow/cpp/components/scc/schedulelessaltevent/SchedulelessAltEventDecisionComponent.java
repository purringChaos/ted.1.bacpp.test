package tv.blackarrow.cpp.components.scc.schedulelessaltevent;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_SIGNAL_ID;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.PTS_ADJUSTMENTS;
import static tv.blackarrow.cpp.components.util.ContextConstants.PTS_TIMES;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.SCTE224EventDecisionBase;
import tv.blackarrow.cpp.components.scc.schedulelessaltevent.response.IBaseResponseProcessor;
import tv.blackarrow.cpp.components.scc.schedulelessaltevent.response.ResponseFactory;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.ESAMHelper;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class SchedulelessAltEventDecisionComponent extends SCTE224EventDecisionBase implements Callable {

	private static final String EMPTY_STRING = "";
	private static final String _0 = "0";
	private static final String ACTION_NOOP = "noop";
	private static final String _1 = "1";
	private static final String TRUE = "true";
	private static final Logger LOGGER = LogManager.getLogger(SchedulelessAltEventDecisionComponent.class);
	private static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	/*
	 * Each acquired Signal has dummy response Signal Populated in them during SCCRequestComponent.
	 * We may need to add additional if need and set specific attribute.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		final HashMap<String, String> ptsTimes = (HashMap<String, String>) context.getMessage().getProperty(PTS_TIMES, PropertyScope.OUTBOUND);
		SignalProcessingEventType signalProcessingEvent = context.getMessage().getProperty(NOTIFICATION_EVENT, PropertyScope.OUTBOUND);
		AcquisitionPoint aqpt = context.getMessage().getProperty(ACQUISITION_POINT, PropertyScope.OUTBOUND);
		final HashMap<String, String> ptsAdjustments = (HashMap<String, String>) context.getMessage().getOutboundProperty(PTS_ADJUSTMENTS);
		String acquisitionSignalId = context.getMessage().getOutboundProperty(ACQUISITION_SIGNAL_ID, null);

		String requestSchema = context.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);
		Schema schema = Schema.getSchema(requestSchema);
		if (Schema.i01 == schema) {
			String msg = "Request with i01 Schema are not supported for a feed configured to execute Scheduleless Alt Events.";
			LOGGER.warn(()->msg);
			context.getMessage().setProperty(CppConstants.RESOURCE_NOT_FOUND, new Integer(3), PropertyScope.OUTBOUND);
			return EMPTY_STRING;
		}
		long programStartBuffer = aqpt.getProgramStartBuffer();
		long currentTimeInMS = System.currentTimeMillis();
		boolean isBinaryRequest = false;
		//Current basic functions have been implemented in i01, we will continue to use that however, for new fields we will create other models and right conversion layers.

		//Step 0: Create notification response
		//Step 1: Iterate through Acquired Signals Segmentation Descriptor Info list
		//If 16 ---> Process and create response Signals accordingly.
		//If 17 ---> Process and create response signals accordingly.
		//For all other segmentation descriptor Info add it back in the either 16/17's response signal's first section segmentation descriptor info.
		SignalProcessingNotificationType notificationResponse = new SignalProcessingNotificationType();
		notificationResponse.setAcquisitionPointIdentity(aqpt.getAcquisitionPointIdentity());

		List<SegmentationDescriptorType> listOfSegmentationDescriptorToBeAddedAsitIs = new ArrayList<SegmentationDescriptorType>();
		Map<String, I03ResponseModelDelta> AltContentIdentityResponseModelDeltaInContext = new HashMap<>();

		for (SignalProcessingEventType.AcquiredSignal acquiredSignal : signalProcessingEvent.getAcquiredSignal()) {
			SchedulelessAltEventLedger ledger = null;
			if ((acquiredSignal != null) && (acquiredSignal.getSCTE35PointDescriptor() != null)
					&& (acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null)
					&& !acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().isEmpty()) {
				TreeMap<Integer, SegmentationDescriptorType> rearrangedSegmentationDescriptor = new TreeMap<Integer, SegmentationDescriptorType>();
				reArrangeSegmentationDescForProcessing(rearrangedSegmentationDescriptor, acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo());
				ResponseSignalType programStartResponseSignal = null;
				ResponseSignalType programEndResponseSignal = null;
				isBinaryRequest = isBinaryRequest || acquiredSignal.getBinaryData() != null;
				Date requestTime = acquiredSignal.getUTCPoint().getUtcPoint().toGregorianCalendar().getTime();
				for (Integer order : rearrangedSegmentationDescriptor.keySet()) {
					SegmentationDescriptorType descriptorType = rearrangedSegmentationDescriptor.get(order);
					Short segmentTypeId = descriptorType.getSegmentTypeId() != null ? descriptorType.getSegmentTypeId() : 0;
					SegmentType requestSegmentTypeIdEnum = SegmentType.valueOf(segmentTypeId);
					IBaseResponseProcessor responseProcessor = ResponseFactory.getClient(requestSegmentTypeIdEnum);
					//Check flags in this descriptor info
					boolean isProgramStartBasedOnAttributes = isProgramStart(descriptorType);
					boolean isProgramEndBasedOnAttributes = isProgramEnd(descriptorType);
					if (DEBUG_ENABLED) {
						LOGGER.debug(()->"isProgramStartAttributesCorrect = " + isProgramStartBasedOnAttributes + ", isProgramEndAttributesCorrect = " + isProgramEndBasedOnAttributes);
					}
					boolean descriptorProcessed = false;

					switch (requestSegmentTypeIdEnum) {
					case PROGRAM_START:
						programStartResponseSignal = CppUtil.addNewResponseSignalWithoutDescriptorInfo(notificationResponse, acquiredSignal);
						populateDescriptorInfoInSCTE35DescriporOfNotification(context, acquiredSignal, programStartResponseSignal, descriptorType);
						if (isProgramStartBasedOnAttributes) {
							ledger = DataManagerFactory.getSchedulelessAltEventDataManager().getAcquisitionPointSchedulelessAltEventLedger(aqpt.getAcquisitionPointIdentity());
							//If Ledger is existing then return same until Signal time + program start buffer else create new
							if (ledger != null && ledger.getProgramStartSignalTransaction(ESSRequestType.SCC) != null) {
								//If previous event has ended or Current time is more than last started time +buffer, then recreate new ledger.
								long signalTimeInMs = ledger.getProgramStartSignalTransaction(ESSRequestType.SCC).getSignalTimeInMS();
								if (ledger.isAltEventEnded() || currentTimeInMS > (signalTimeInMs + programStartBuffer)) {
									//Add new Cadent Signal Id
									ledger = createEmptyLedger(context, aqpt, acquiredSignal, descriptorType, responseProcessor, requestTime);
								}
							} else {
								//Add new Cadent Signal Id
								ledger = createEmptyLedger(context, aqpt, acquiredSignal, descriptorType, responseProcessor, requestTime);
							}
							responseProcessor.generateResponse(notificationResponse, programStartResponseSignal, aqpt, SegmentType.PROGRAM_START, acquisitionSignalId, ptsTimes,
									ptsAdjustments, context, ledger, AltContentIdentityResponseModelDeltaInContext, requestTime);
						} else {
							generateNoOpForIt(programStartResponseSignal);
						}
						descriptorProcessed = true;//This was processed
						break;

					case PROGRAM_END:
						programEndResponseSignal = CppUtil.addNewResponseSignalWithoutDescriptorInfo(notificationResponse, acquiredSignal);
						populateDescriptorInfoInSCTE35DescriporOfNotification(context, acquiredSignal, programEndResponseSignal, descriptorType);
						if (isProgramEndBasedOnAttributes) {
							ledger = DataManagerFactory.getSchedulelessAltEventDataManager().getAcquisitionPointSchedulelessAltEventLedger(aqpt.getAcquisitionPointIdentity());
							if (ledger != null && !ledger.isAltEventEnded()
									&& ledger.getProgramStartSignalTransaction(ESSRequestType.SCC).getSignalTimeInMS() < requestTime.getTime()) {
								responseProcessor.processSignal(context, acquiredSignal, aqpt, descriptorType, ledger, requestTime);
								responseProcessor.generateResponse(notificationResponse, programEndResponseSignal, aqpt, SegmentType.PROGRAM_START, acquisitionSignalId, ptsTimes,
										ptsAdjustments, context, ledger, AltContentIdentityResponseModelDeltaInContext, requestTime);
							} else {
								generateNoOpForIt(programEndResponseSignal);
							}
						} else {
							generateNoOpForIt(programEndResponseSignal);
						}
						descriptorProcessed = true;//This was processed
						break;

					case CONTENT_IDENTIFICATION:
						if (acquiredSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().size() == 1) {
							ledger = DataManagerFactory.getSchedulelessAltEventDataManager().getAcquisitionPointSchedulelessAltEventLedger(aqpt.getAcquisitionPointIdentity());
							ResponseSignalType responseSignal1 = CppUtil.addNewResponseSignalWithoutDescriptorInfo(notificationResponse, acquiredSignal);
							populateDescriptorInfoInSCTE35DescriporOfNotification(context, acquiredSignal, responseSignal1, descriptorType);
							responseProcessor.processSignal(context, acquiredSignal, aqpt, descriptorType, ledger, requestTime);
							responseProcessor.generateResponse(notificationResponse, responseSignal1, aqpt, SegmentType.CONTENT_IDENTIFICATION, acquisitionSignalId, ptsTimes,
									ptsAdjustments, context, ledger, AltContentIdentityResponseModelDeltaInContext, requestTime);
							descriptorProcessed = true;//This was processed
						}
						break;
					default:
						break;
					}
					//Add unprocessed signal as it is in this list.
					if (!descriptorProcessed) {
						listOfSegmentationDescriptorToBeAddedAsitIs.add(descriptorType);
					}
				}

				// Add all unused segmentation descriptor info in program start's response, if the Program start descriptor did not get generate then add unused in first.
				if (listOfSegmentationDescriptorToBeAddedAsitIs != null && !listOfSegmentationDescriptorToBeAddedAsitIs.isEmpty()
						&& notificationResponse.getResponseSignal() != null && !notificationResponse.getResponseSignal().isEmpty()) {
					for (SegmentationDescriptorType item : listOfSegmentationDescriptorToBeAddedAsitIs) {
						if (programStartResponseSignal != null) {
							programStartResponseSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(item);
						} else if (programEndResponseSignal != null) {
							programEndResponseSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(item);
						} else {
							notificationResponse.getResponseSignal().get(0).getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(item);
						}
					}
				}

				//resetResponseSignalForBinary Mode if request was binary
				if (isBinaryRequest && notificationResponse.getResponseSignal() != null && !notificationResponse.getResponseSignal().isEmpty()) {
					resetResponseSignalForBinaryMode(notificationResponse.getResponseSignal(), ptsTimes, ptsAdjustments, aqpt);
				}

				//Save the last Program Start response processed on this Acquisition Point for SignalStateRequest
				if (ledger != null && programStartResponseSignal != null && !ACTION_NOOP.equalsIgnoreCase(programStartResponseSignal.getAction())) {
					context.getMessage().setProperty(ContextConstants.CONFIRMED_EVENT_SIGNAL_ID, ledger.getCadentSignalId(), PropertyScope.OUTBOUND);
				}
			}

		}
		context.getMessage().setProperty(ContextConstants.I03_MODEL_DELTA, AltContentIdentityResponseModelDeltaInContext, PropertyScope.OUTBOUND);
		context.getMessage().setProperty(ContextConstants.SIGNAL_RESPONSE, notificationResponse, PropertyScope.OUTBOUND);
		return EMPTY_STRING;
	}

	private SchedulelessAltEventLedger createEmptyLedger(MuleEventContext context, AcquisitionPoint aqpt, SignalProcessingEventType.AcquiredSignal acquiredSignal,
			SegmentationDescriptorType descriptorType, IBaseResponseProcessor response, Date requestTime) {
		SchedulelessAltEventLedger ledger;
		ledger = SchedulelessAltEventLedger.getEmptyLedger(aqpt.getAcquisitionPointIdentity());
		ledger.setCadentSignalId(UUIDUtils.getBase64UrlEncodedUUID());//generated
		response.processSignal(context, acquiredSignal, aqpt, descriptorType, ledger, requestTime);
		return ledger;
	}

	private void generateNoOpForIt(ResponseSignalType programStartResponseSignal) {
		if (programStartResponseSignal != null) {
			programStartResponseSignal.setAction(ACTION_NOOP);
		}

	}

	private void reArrangeSegmentationDescForProcessing(TreeMap<Integer, SegmentationDescriptorType> rearrangedSegmentationDescriptor,
			List<SegmentationDescriptorType> segmentationDescriptorInfo) {
		int FIRST = 1;
		int SECOND = 2;
		int THIRD = 3;
		if (segmentationDescriptorInfo != null && !segmentationDescriptorInfo.isEmpty()) {
			for (SegmentationDescriptorType descriptorType : segmentationDescriptorInfo) {
				Short segmentTypeId = descriptorType.getSegmentTypeId() != null ? descriptorType.getSegmentTypeId() : 0;
				SegmentType requestSegmentTypeIdEnum = SegmentType.valueOf(segmentTypeId);
				switch (requestSegmentTypeIdEnum) {
				case CONTENT_IDENTIFICATION:
					rearrangedSegmentationDescriptor.put(THIRD, descriptorType);
					break;
				case PROGRAM_END:
					rearrangedSegmentationDescriptor.put(FIRST, descriptorType);
					break;
				case PROGRAM_START:
					rearrangedSegmentationDescriptor.put(SECOND, descriptorType);
					break;
				default:
					break;
				}
			}
		}

	}

	private void populateDescriptorInfoInSCTE35DescriporOfNotification(MuleEventContext context, final SignalProcessingEventType.AcquiredSignal signal,
			ResponseSignalType responseSignal, SegmentationDescriptorType currentDescriptorType) {
		responseSignal.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().add(currentDescriptorType);
	}

	private boolean isProgramEnd(SegmentationDescriptorType descriptorType) {
		boolean isprogramEnd = false;
		if ((descriptorType != null) && (descriptorType.getOtherAttributes() != null) && !descriptorType.getOtherAttributes().isEmpty()) {
			String deliveryNotRestrictedFlagValue = descriptorType.getOtherAttributes().get(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG));
			boolean deliveryNotRestrictedFlag = TRUE.equals(deliveryNotRestrictedFlagValue) || _1.equals(deliveryNotRestrictedFlagValue);
			String webDeliveryAllowedFlagValue = descriptorType.getOtherAttributes().get(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG));
			boolean webDeliveryAllowedFlag = TRUE.equals(webDeliveryAllowedFlagValue) || _1.equals(webDeliveryAllowedFlagValue);
			//As per Khaleesi.4, viacom document
			if ((!deliveryNotRestrictedFlag && webDeliveryAllowedFlag) || deliveryNotRestrictedFlag) {
				isprogramEnd = true;
			}
		}
		return isprogramEnd;
	}

	private boolean isProgramStart(SegmentationDescriptorType descriptorType) {
		boolean isprogramStart = false;
		if ((descriptorType != null) && (descriptorType.getOtherAttributes() != null) && !descriptorType.getOtherAttributes().isEmpty()) {
			String deliveryNotRestrictedFlagValue = descriptorType.getOtherAttributes().get(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG));
			boolean deliveryNotRestrictedFlag = TRUE.equals(deliveryNotRestrictedFlagValue) || _1.equals(deliveryNotRestrictedFlagValue);
			String webDeliveryAllowedFlagValue = descriptorType.getOtherAttributes().get(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG));
			boolean webDeliveryAllowedFlag = TRUE.equals(webDeliveryAllowedFlagValue) || _1.equals(webDeliveryAllowedFlagValue);
			//As per Khaleesi.4, viacom document
			if (!deliveryNotRestrictedFlag && !webDeliveryAllowedFlag) {
				isprogramStart = true;
			}
		}
		return isprogramStart;
	}

	/*
	 * Remove the customized data if original signal was binary, Also if we appended extra segmentation descriptors in descriptor info then generate binary for it.
	 */
	private void resetResponseSignalForBinaryMode(List<ResponseSignalType> responseSignalTypes, HashMap<String, String> ptsTimes, HashMap<String, String> ptsAdjustments,
			AcquisitionPoint aqpt) {
		if ((responseSignalTypes != null) && (responseSignalTypes.size() > 0)) {
			int counter = 0;
			for (ResponseSignalType responseSignalType : responseSignalTypes) {

				String pts_adjustment = EMPTY_STRING;
				String ptsTimePlusOffsetInBinaryDefault = Scte35BinaryUtil.toBitString(0l, 33);//Default value;

				//Change each SCTE35PointDescriptorType in response signal to Binary at last.
				if (responseSignalType.getSCTE35PointDescriptor() != null && responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo() != null
						&& !responseSignalType.getSCTE35PointDescriptor().getSegmentationDescriptorInfo().isEmpty()) {
					++counter;
					long signalTimeOffset = (aqpt != null ? aqpt.getSignalTimeOffset() : CppConstants.SIGNAL_TIME_OFFSET_DEFAULT_VALUE);
					pts_adjustment = (ptsAdjustments == null) || ptsAdjustments.isEmpty() ? Scte35BinaryUtil.toBitString(0l, 33)
							: ptsAdjustments.get(responseSignalType.getAcquisitionSignalID());
					String ptsTimeInBinary = StringUtils.isNotBlank(ptsTimes.get(responseSignalType.getAcquisitionSignalID()))
							? ptsTimes.get(responseSignalType.getAcquisitionSignalID())
							: _0;
					try {
						ptsTimePlusOffsetInBinaryDefault = Scte35BinaryUtil.applySignalTimeOffset(ptsTimeInBinary, signalTimeOffset);//try to apply ACQusition point offset if any
					} catch (Exception stringIndexOutOfBoundsException) {

					}
					if (DEBUG_ENABLED) {
						LOGGER.debug(counter + ". Before encoding SCTE35PointDescriptor: \n" + logDescriptorString(responseSignalType.getSCTE35PointDescriptor()));
					}

					setSegmentDescriptorAsBinaryOrXMLInResponseSignal(ptsTimePlusOffsetInBinaryDefault, pts_adjustment, responseSignalType,
							responseSignalType.getSCTE35PointDescriptor());

				}
				responseSignalType.setSCTE35PointDescriptor(null);//Reset xml portion as binary may have already got converted
				if (DEBUG_ENABLED) {
					SCTE35PointDescriptorType logscte35Pt = new SCTE35PointDescriptorType();
					StringBuilder sbPtsTimePlusOffsetInBinaryDefault = new StringBuilder(ptsTimePlusOffsetInBinaryDefault);
					StringBuilder sbPts_adjustment = new StringBuilder(pts_adjustment);
					final byte[] encoded = Base64.encodeBase64(responseSignalType.getBinaryData().getValue());
					Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), logscte35Pt, sbPtsTimePlusOffsetInBinaryDefault, sbPts_adjustment);
					LOGGER.debug(counter + ". After decoding SCTE35PointDescriptor binary for verification: \n" + logDescriptorString(logscte35Pt));
				}

			}
		}
	}

	private void setSegmentDescriptorAsBinaryOrXMLInResponseSignal(final String ptsTimeInBinary, final String pts_adjustment, ResponseSignalType respSignalType,
			SCTE35PointDescriptorType scte35Pnt) {
		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pnt, ptsTimeInBinary, pts_adjustment);
		BinarySignalType binarySignal = new BinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType(CppConstants.SIGNAL_TYPE_SCTE35);
		respSignalType.setBinaryData(binarySignal);
	}

	private Object logDescriptorString(SCTE35PointDescriptorType logscte35Pt) {
		StringBuilder sb = new StringBuilder();
		if (logscte35Pt != null) {
			if (logscte35Pt.getSegmentationDescriptorInfo() != null) {
				for (SegmentationDescriptorType type : logscte35Pt.getSegmentationDescriptorInfo()) {

					String upidHex = new HexBinaryAdapter().marshal(type.getUpid());
					String signalId = ESAMHelper.getSignalIdFromUPIDHexString(upidHex);

					sb.append("duration=").append(type.getDuration()).append("\totherAttributes=").append(type.getOtherAttributes()).append("\tsegmenteventId=")
							.append(type.getSegmentEventId()).append("\tsegmentnum=").append(type.getSegmentNum()).append("\tsegmentexpected=").append(type.getSegmentsExpected())
							.append(type.getSegmentTypeId()).append("\tupid=").append(upidHex).append("\tsignalIdInUpid=").append(signalId).append("\tupidtype=")
							.append(type.getUpidType());
				}
			}
		}
		return sb;
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema) {
		return schema.getResponseHandler().generateSCCResponse(signalNotification, null);
	}
}
