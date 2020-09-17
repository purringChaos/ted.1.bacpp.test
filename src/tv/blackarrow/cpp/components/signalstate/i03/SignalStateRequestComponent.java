package tv.blackarrow.cpp.components.signalstate.i03;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.components.signalstate.model.SignalStateModel;
import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.handler.I03RequestHandler;
import tv.blackarrow.cpp.i03.signaling.ConditioningInfoType;
import tv.blackarrow.cpp.i03.signaling.ObjectFactory;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.i03.signaling.SignalStateRequest;
import tv.blackarrow.cpp.i03.signaling.StatusCodeType;
import tv.blackarrow.cpp.i03.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.managers.SCTE224DataManager;
import tv.blackarrow.cpp.managers.SchedulelessAltEventDataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaLedger;
import tv.blackarrow.cpp.model.scte224.MediaPoint;
import tv.blackarrow.cpp.model.scte224.MediaTransaction;
import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.AuditLogHelper;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.NamespacePrefixMapperImpl;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SegmentType;
import tv.blackarrow.cpp.utils.SpliceCommandType;
import tv.blackarrow.cpp.utils.UPIDType;

/**
 *
 * take ESAM SSR request
 *
 */
public class SignalStateRequestComponent implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(SignalStateRequestComponent.class);
	public static final String ERROR_MESSAGE = "ESAM I03 error. Please ensure messages sent to this endpoint comply with ESAM I03 schema.";

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {

		SignalStateModel signalState = null;
		String response = "";
		String message = context.getMessageAsString();
		LOGGER.debug(()->"ESAM Signal State Request:\n" + message);
		SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
		JAXBContext linearPOISSsrJAXBContext = I03RequestHandler.linearPOISSccJAXBContext;
		SignalStateRequest signalStateRequest = null;
		boolean isOldNameSpace = false;

		try {
			signalStateRequest = parseSignalStateRequest(message, linearPOISSsrJAXBContext);

			if (signalStateRequest != null && signalStateRequest.getAcquisitionPointIdentity() != null && signalStateRequest.getAcquisitionPointIdentity().trim().length() > 0) {
				DataManager dataManager = DataManagerFactory.getInstance();
				AcquisitionPoint acqPoint = dataManager.getAcquisitionPoint(signalStateRequest.getAcquisitionPointIdentity());
				if (acqPoint != null) {

					signalState = dataManager.getLastConfirmedEvent(signalStateRequest.getAcquisitionPointIdentity());
					if (signalState != null && signalState.getActualSignalStr() != null) {
						response = signalState.getActualSignalStr();
						// NAMESPACE_HACK : ought to be removed once not needed
						if (response.contains(CppConstants.OLD_SCC_NAMESPACE)) {
							response = response.replace(CppConstants.OLD_SCC_NAMESPACE, CppConstants.NEW_SCC_NAMESPACE);
							isOldNameSpace = true;
						}

						notification = XMLToObject(response);
						// for Media
						long now = System.currentTimeMillis();
						if (AlternateContentVersion.ESNI_224 == acqPoint.getFeedsAlternateContentVersion()) {

							SCTE224DataManager scte224DataManager = DataManagerFactory.getSCTE224DataManager();
							MediaLedger ledger = scte224DataManager.getAcquisitionPointMediaLedger(signalStateRequest.getAcquisitionPointIdentity(), signalState.getSignalId());
							if (ledger != null) {
								if (ledger.isMediaStarted() && ledger.isMediaEnded()) {
									notification = prepareNoopsSignalNotification(notification);
								}else if(ledger.isMediaStarted() && !ledger.isMediaEnded()) {
									MediaTransaction startTransaction = ledger.getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
									boolean isApplyDurationPassed = startTransaction != null && startTransaction.getSignalTimeInMS() != null
											&& startTransaction.getTotalDurationInMS() != null
											&& ((startTransaction.getSignalTimeInMS() + startTransaction.getTotalDurationInMS()) < System.currentTimeMillis());
									//PKH-1126 Consider the natural cause of Policy lapse time also as Media Ending
									//For Encoder level AQ
									if (StringUtils.isNotBlank(acqPoint.getZoneIdentity())) {
										Media media = DataManagerFactory.getSCTE224DataManager().getMediaBySignalIdV1(acqPoint.getFeedExternalRef(), signalState.getSignalId());
										MediaPoint programEndMediaPoint = media.getMediaPoints().get(1);
										long finalEndMatchTimeConsideringSignalTolerance = programEndMediaPoint != null ? programEndMediaPoint.getMatchTimeInMS() : 0l;
										// consider final End MediaPoint match time+SignalTolerance
										if (finalEndMatchTimeConsideringSignalTolerance > 0 && now >= finalEndMatchTimeConsideringSignalTolerance) {
											notification = prepareNoopsSignalNotification(notification);
										}
									} //For Manifest level Event AQ 
									else if (isApplyDurationPassed) {//manifest level blackouts, no change
										notification = prepareNoopsSignalNotification(notification);
									}
								}
							} else {
								LOGGER.debug("No Media found for this acquisition point id : " + signalStateRequest.getAcquisitionPointIdentity());
								notification = prepareNoopsSignalNotification(notification);
								notification.setStatusCode(generateErrorStatusCode("No Media found for this acquisition point id : "
										+ signalStateRequest.getAcquisitionPointIdentity()));

							}
						}
						else if (AlternateContentVersion.SCHEDULELESS == acqPoint.getFeedsAlternateContentVersion()) {
							SchedulelessAltEventDataManager slAltEventDataManager = DataManagerFactory.getSchedulelessAltEventDataManager();
							SchedulelessAltEventLedger ledger = slAltEventDataManager.getAcquisitionPointSchedulelessAltEventLedger(signalStateRequest.getAcquisitionPointIdentity());
							if (ledger != null) {
								if (ledger.isAltEventStarted() && ledger.isAltEventEnded()) {
									notification = prepareNoopsSignalNotificationForScheduless(notification, ledger.getCadentSignalId());//TODO Khaleesi.4 Remove All ResponseSignals except those whose SignalPointId == ledger's cadent signal id hex, and
									//then remove content id part from there.
								}else if(ledger.isAltEventStarted() && !ledger.isAltEventEnded()) {
									notification = cleamNotificationWithUnwantedResponseSignals(notification, ledger.getCadentSignalId());//TODO Khaleesi.4 Remove All ResponseSignals except those whose SignalPointId == ledger's cadent signal id hex, and
								}
							} else {
								LOGGER.debug("No Scheduleless alt event found for this acquisition point id : " + signalStateRequest.getAcquisitionPointIdentity());
								notification = prepareNoopsSignalNotification(notification);
								notification.setStatusCode(generateErrorStatusCode("No Scheduleless alt event found for this acquisition point id : "
										+ signalStateRequest.getAcquisitionPointIdentity()));
							}
						}
						// for blackout
						else if (AlternateContentVersion.ESNI_I02 == acqPoint.getFeedsAlternateContentVersion()) {
							ConfirmedPlacementOpportunity confirmedBlackout = dataManager.getConfirmedBlackoutForGivenAP(acqPoint.getAcquisitionPointIdentity(),
									signalState.getSignalId());
							if (confirmedBlackout != null) {
								if (confirmedBlackout.isProgramEnded()) {
									notification = prepareNoopsSignalNotification(notification);
								}
								//For Open ended we are not considering the blackout Stop time.
								else if(!acqPoint.isFeedAllowsOpenEndedBlackouts()){

									//handle if program end is not rec. and blackout stop time is in past.
									BlackoutEvent blackoutEvent = dataManager.getSingleBlackoutEvent(confirmedBlackout.getSignalId());
									long blackoutEndTime = BlackoutEvent.getActualBlackoutStopTime(confirmedBlackout, blackoutEvent);

									//If its Inband then wait till stop time + buffer time
									if(acqPoint.isInBand() && System.currentTimeMillis() > (blackoutEndTime + acqPoint.getProgramStartBuffer())){
										notification = prepareNoopsSignalNotification(notification);
									} else if(acqPoint.isOutBand() && System.currentTimeMillis() > blackoutEndTime){
										notification = prepareNoopsSignalNotification(notification);
									}
								}
							} else {
								LOGGER.debug("No blackout found for this acquisition point id : " + signalStateRequest.getAcquisitionPointIdentity());
								notification = prepareNoopsSignalNotification(notification);
								notification.setStatusCode(generateErrorStatusCode("No blackout found for this acquisition point id : "
										+ signalStateRequest.getAcquisitionPointIdentity()));
							}
						}
					} else {
						// Dummy Signals
						LOGGER.debug("No response  found for this acquisition point id :  " + signalStateRequest.getAcquisitionPointIdentity());
						notification = prepareDummyNotificationSignalResponse(signalStateRequest.getAcquisitionPointIdentity());
						notification.setStatusCode(generateErrorStatusCode("No response  found for this acquisition point id :  "
								+ signalStateRequest.getAcquisitionPointIdentity()));
					}

				} else {
					notification = prepareDummyNotificationSignalResponse(signalStateRequest.getAcquisitionPointIdentity());
					notification.setStatusCode(generateErrorStatusCode("No Acquisition point found :  " + signalStateRequest.getAcquisitionPointIdentity()));
					LOGGER.debug("No Acquisition point found :  " + signalStateRequest.getAcquisitionPointIdentity());
				}
			} else {
				LOGGER.debug(()->"Invalid request");
				notification = prepareDummyNotificationSignalResponse(null);
				notification.setStatusCode(generateErrorStatusCode("Invalid request"));
			}
				response = ObjectToXML(notification);
			// NAMESPACE_HACK : ought to be removed once not needed
			if (response != null && isOldNameSpace && response.contains(CppConstants.NEW_SCC_NAMESPACE)) {
				response = response.replace(CppConstants.NEW_SCC_NAMESPACE, CppConstants.OLD_SCC_NAMESPACE);
			}

		} catch (JAXBException jaxb) {
			LOGGER.error(()->jaxb.getMessage(), jaxb);
			notification.setStatusCode(generateErrorStatusCode(jaxb.getCause().getMessage()));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			LOGGER.error(()->"Unexpected error ", e);
			String errMsg = e.getMessage() == null ? e.toString() : e.getMessage();
			notification.setStatusCode(generateErrorStatusCode("Unexpected error: " + errMsg));
		} finally {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.debug("ESAM Signal State Response:\n" + response);
			}
		}
		// Log into message log.
		AuditLogger.auditMessage(response, AuditLogHelper.populateAuditLogVO(context, notification));
		return response;
	}

	private SignalProcessingNotificationType cleamNotificationWithUnwantedResponseSignals(SignalProcessingNotificationType notification, String cadentSignalId) {
		if (notification != null && notification.getResponseSignal() != null && !notification.getResponseSignal().isEmpty()) {
			List<ResponseSignalType> programStartSignals = new ArrayList<ResponseSignalType>();
			for (ResponseSignalType responseSignalType : notification.getResponseSignal()) {

				//Pick only confirmed signals based on ledger cadent Signal Id
				if (responseSignalType.getSignalPointID() != null &&  cadentSignalId != null &&  responseSignalType.getSignalPointID().equalsIgnoreCase(cadentSignalId)) {
					programStartSignals.add(responseSignalType);
				}
			}

			// clean responseSignal list
			notification.getResponseSignal().clear();

			// adding program start signal
			notification.getResponseSignal().addAll(programStartSignals);

			if(programStartSignals.isEmpty()){
				LOGGER.debug(()->"No confirmed signal found for Scheduleless alt event for this acquisition point id : " + notification.getAcquisitionPointIdentity());
				notification.setStatusCode(generateErrorStatusCode("No confirmed signal found for Scheduleless alt event for this acquisition point id : " + notification.getAcquisitionPointIdentity()));
			} else {
				// Set Class Code in response
				notification.setStatusCode(new StatusCodeType());
				notification.getStatusCode().setClassCode(new BigInteger("0"));
			}

			//Clean conditioning info that has duration(as only Program End response's conditioning info knows duration)
			if (notification != null && notification.getConditioningInfo() != null) {
				Iterator<ConditioningInfoType> itr = notification.getConditioningInfo().iterator();
				while (itr.hasNext()) {
					ConditioningInfoType ct = itr.next();
					if (ct.getDuration() != null) {
						itr.remove();
					}
				}
			}

		}

		return notification;
	}

	private SignalProcessingNotificationType prepareNoopsSignalNotificationForScheduless(SignalProcessingNotificationType notification, String cadentSignalId) {

		if (notification != null && notification.getResponseSignal() != null && !notification.getResponseSignal().isEmpty()) {

			boolean isStartSignalFound = false;
			ResponseSignalType programStartSignal = null;

			for (ResponseSignalType responseSignalType : notification.getResponseSignal()) {
				if (responseSignalType.getSignalPointID() != null && cadentSignalId != null && responseSignalType.getSignalPointID().equalsIgnoreCase(cadentSignalId)) {

					SCTE35PointDescriptorType scte35 = responseSignalType.getSCTE35PointDescriptor();
					Short segmentTypeId = scte35 != null && scte35.getSegmentationDescriptorInfo() != null && !scte35.getSegmentationDescriptorInfo().isEmpty() ? scte35
							.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() : null;

					if (segmentTypeId == null || (responseSignalType.getBinaryData() != null && responseSignalType.getBinaryData().getValue() != null)) {
						// Get Segment Type ID from Binary Data.
						try {
							tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType scte35Pt = null;
							// Handle Binary data
							if (responseSignalType.getBinaryData() != null) {
								scte35Pt = new tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType();
								final byte[] encoded = Base64.encodeBase64(responseSignalType.getBinaryData().getValue());
								final StringBuilder pts = new StringBuilder();
								final StringBuilder pts_adjustments = new StringBuilder();
								Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustments);
								segmentTypeId = scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty() ? scte35Pt
										.getSegmentationDescriptorInfo().get(0).getSegmentTypeId()
										: null;
							}
						} catch (Exception e) {
							LOGGER.error(()->"Error while getting segment type id from Binary data : " + e.getMessage());
						}
					}
					if (segmentTypeId != null) {
						if (SegmentType.valueOf(segmentTypeId) == SegmentType.PROGRAM_START) {
							responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
							isStartSignalFound = true;
							programStartSignal = responseSignalType;
							break;
						}
					}
				}
			}

			if (!isStartSignalFound) {
				// Handle if Start signal not found in response.
				return prepareDummyNotificationSignalResponse(notification.getAcquisitionPointIdentity());
			}
			// remove conditioning info
			notification.getConditioningInfo().clear();

			// clean responseSignal list
			notification.getResponseSignal().clear();

			// adding program start signal
			notification.getResponseSignal().add(programStartSignal);

			// Set Class Code in response
			notification.setStatusCode(new StatusCodeType());
			notification.getStatusCode().setClassCode(new BigInteger("0"));
		}

		return notification;
	}

	private SignalProcessingNotificationType prepareDummyNotificationSignalResponse(String acquisitionPointIndentity) {
		SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
		notification.setStatusCode(new StatusCodeType());
		notification.getStatusCode().setClassCode(new BigInteger("0"));
		notification.setAcquisitionPointIdentity(acquisitionPointIndentity);
		ResponseSignalType rst = new ResponseSignalType();
		rst.setAcquisitionPointIdentity(acquisitionPointIndentity);
		rst.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
		rst.setUTCPoint(new UTCPointDescriptorType());
		rst.getUTCPoint().setUtcPoint(SCCResponseUtil.generateUTC(System.currentTimeMillis()));

		tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType desc = new tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType();
		tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType segmentType =  new tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType();
		segmentType.setSegmentTypeId(SegmentType.PROGRAM_START.getSegmentTypeId());
		segmentType.setSegmentEventId(0l);
		segmentType.setSegmentsExpected((short)0);
		segmentType.setSegmentNum((short)0);
		segmentType.setUpidType(UPIDType.CABLELAB_ADI.getUPIDTypeId());
		segmentType.setSegmentationEventCancelIndicator(false);

		desc.getSegmentationDescriptorInfo().add(segmentType);
		desc.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype());

		//Convert SCTE35PointDescriptorType into binary
		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(desc, "", Scte35BinaryUtil.toBitString(0l, 33));
		tv.blackarrow.cpp.i03.signaling.BinarySignalType bst = new tv.blackarrow.cpp.i03.signaling.BinarySignalType();
		bst.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		rst.setBinaryData(bst);
		bst.setSignalType("SCTE35");
		rst.setSCTE35PointDescriptor(null);

		notification.getResponseSignal().add(rst);
		return notification;
	}

	private SignalStateRequest parseSignalStateRequest(String requestXml, JAXBContext linearPOISPsnJAXBContext) throws Exception {
		final StringReader reader = new StringReader(requestXml);
		final Unmarshaller unmarshaller = linearPOISPsnJAXBContext.createUnmarshaller();
		JAXBElement<SignalStateRequest> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalStateRequest.class);
		return jxbElement.getValue();
	}

	public SignalProcessingNotificationType XMLToObject(String responseXml) {
		try {
			final StringReader reader = new StringReader(responseXml);
			final JAXBContext jaxbCxt = I03RequestHandler.linearPOISSccJAXBContext;
			final Unmarshaller unmarshaller = jaxbCxt.createUnmarshaller();
			JAXBElement<SignalProcessingNotificationType> jxbElement = unmarshaller.unmarshal(new StreamSource(reader), SignalProcessingNotificationType.class);
			return jxbElement.getValue();
		} catch (Exception e) {
			LOGGER.error(()->e.getMessage());
			return null;
		}
	}

	public static StatusCodeType generateErrorStatusCode(String errorMessage) {
		StatusCodeType statusCode = new StatusCodeType();
		statusCode.setClassCode(new BigInteger("1"));
		statusCode.setDetailCode(new BigInteger("1"));
		statusCode.getNote().add(errorMessage);
		return statusCode;
	}

	public String ObjectToXML(SignalProcessingNotificationType notification) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller marshaller = I03RequestHandler.linearPOISSccJAXBContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapperImpl());
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			ObjectFactory factory = new ObjectFactory();
			JAXBElement<SignalProcessingNotificationType> jxbElement = factory.createSignalProcessingNotification(notification);
			marshaller.marshal(jxbElement, writer);
		} catch (JAXBException e) {
			LOGGER.error(()->e.getMessage());
		}
		return writer.toString();
	}

	private SignalProcessingNotificationType prepareNoopsSignalNotification(SignalProcessingNotificationType notification) {
		if (notification != null && notification.getResponseSignal() != null && !notification.getResponseSignal().isEmpty()) {
			boolean isStartSignalFound=false;
			ResponseSignalType programStartSignal = null;
			for (ResponseSignalType responseSignalType : notification.getResponseSignal()) {
				SCTE35PointDescriptorType scte35 = responseSignalType.getSCTE35PointDescriptor();
				Short segmentTypeId = scte35 != null && scte35.getSegmentationDescriptorInfo() != null && !scte35.getSegmentationDescriptorInfo().isEmpty() ? scte35
						.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() : null;

				// Handle Binary data
				if (segmentTypeId == null || (responseSignalType.getBinaryData() != null && responseSignalType.getBinaryData().getValue() != null)) {
					// Get Segment Type ID from Binary Data.
					try {
						tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType scte35Pt = null;
						if (responseSignalType.getBinaryData() != null) {
							scte35Pt = new tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType();
							final byte[] encoded = Base64.encodeBase64(responseSignalType.getBinaryData().getValue());
							final StringBuilder pts = new StringBuilder();
							final StringBuilder pts_adjustments = new StringBuilder();
							Scte35BinaryUtil.decodeScte35BinaryData(new String(encoded), scte35Pt, pts, pts_adjustments);
							segmentTypeId = scte35Pt != null && scte35Pt.getSegmentationDescriptorInfo() != null && !scte35Pt.getSegmentationDescriptorInfo().isEmpty() ? scte35Pt
									.getSegmentationDescriptorInfo().get(0).getSegmentTypeId() : null;
						}
					} catch (Exception e) {
						LOGGER.error(()->"Error while getting segment type id from Binary data : " + e.getMessage());
					}
				}

				if (segmentTypeId != null) {
					if (SegmentType.valueOf(segmentTypeId) == SegmentType.PROGRAM_START) {
						responseSignalType.setAction(CppConstants.SCTE35_RESPONSE_SIGNAL_ACTION_NOOP);
						isStartSignalFound = true;
						programStartSignal = responseSignalType;
						break;
					}
				} else {
					// Handle if segment type id is not found from binary data.
					return prepareDummyNotificationSignalResponse(notification.getAcquisitionPointIdentity());
				}
			}

			if(!isStartSignalFound){
				// Handle if Start signal not found in response.
				return prepareDummyNotificationSignalResponse(notification.getAcquisitionPointIdentity());
			}
			//remove conditioning info
			notification.getConditioningInfo().clear();

			//clean responseSignal list
			notification.getResponseSignal().clear();

			//adding program start signal
			notification.getResponseSignal().add(programStartSignal);

			//Set Class Code in response
			notification.setStatusCode(new StatusCodeType());
			notification.getStatusCode().setClassCode(new BigInteger("0"));
		}
		return notification;
	}
}
