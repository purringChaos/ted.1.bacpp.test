package tv.blackarrow.cpp.components.scc.scte224;

import static tv.blackarrow.cpp.components.util.ContextConstants.ACQUISITION_POINT;
import static tv.blackarrow.cpp.components.util.ContextConstants.NOTIFICATION_EVENT;
import static tv.blackarrow.cpp.components.util.ContextConstants.PTS_ADJUSTMENTS;
import static tv.blackarrow.cpp.components.util.ContextConstants.PTS_TIMES;
import static tv.blackarrow.cpp.components.util.ContextConstants.SCHEMA;
import static tv.blackarrow.cpp.components.util.ContextConstants.SEGMENT_TYPE_ID;
import static tv.blackarrow.cpp.components.util.ContextConstants.SIGNAL_RESPONSE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.components.SCTE224EventDecisionBase;
import tv.blackarrow.cpp.components.scc.scte224.response.ExecutionTypeFactory;
import tv.blackarrow.cpp.components.scc.scte224.response.common.IBaseResponseProcessor;
import tv.blackarrow.cpp.components.util.ContextConstants;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.I03ResponseModelDelta;
import tv.blackarrow.cpp.model.scte224.FeedMediaCompactedDetail;
import tv.blackarrow.cpp.notifications.hosted.model.scte224.HostedAppEventStatusScte224NotifyModel;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.scte224.handler.MediaRuntimeNotificationsHandler;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.SegmentType;

public class MediaDecisionComponent extends SCTE224EventDecisionBase implements Callable {

	private static final Logger LOGGER = LogManager.getLogger(MediaDecisionComponent.class);

	/*
	 * Each acquired Signal has dummy response Signal Populated in them during SCCRequestComponent.
	 * We may need to add additional if need and set specific attribute.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		final HashMap<String, String> ptsTimes = (HashMap<String, String>) context.getMessage().getProperty(PTS_TIMES, PropertyScope.OUTBOUND);
		SignalProcessingNotificationType notificationResponse = context.getMessage().getProperty(SIGNAL_RESPONSE, PropertyScope.OUTBOUND);
		SignalProcessingEventType notificationEvent = context.getMessage().getProperty(NOTIFICATION_EVENT, PropertyScope.OUTBOUND);
		Short segmentTypeId = context.getMessage().getProperty(SEGMENT_TYPE_ID, PropertyScope.OUTBOUND);
		AcquisitionPoint aqpt = context.getMessage().getProperty(ACQUISITION_POINT, PropertyScope.OUTBOUND);
		final HashMap<String, String> ptsAdjustments = (HashMap<String, String>) context.getMessage().getOutboundProperty(PTS_ADJUSTMENTS);

		Map<String, I03ResponseModelDelta> responseToI03ResponseModelDeltaMap = new HashMap<>();

		String requestSchema = context.getMessage().getProperty(SCHEMA, PropertyScope.INVOCATION);
		Schema schema = Schema.getSchema(requestSchema);
		FeedMediaCompactedDetail medias = DataManagerFactory.getSCTE224DataManager().getFeedMediaCompactedDetailV1(aqpt.getFeedExternalRef());
		//For each acquired signal 1 response signal has been added by SCCRequest component, you may want to either return one or any additional signal

		IBaseExecutor executor = ExecutionTypeFactory.getClient(aqpt);
		SegmentType requestSegmentTypeIdEnum = SegmentType.valueOf(segmentTypeId);
		IBaseResponseProcessor response = executor.getResponseProcessor(requestSegmentTypeIdEnum, aqpt, notificationEvent.getAcquiredSignal().get(0));

		ResponseSignalType baseResponseSignal = notificationResponse.getResponseSignal().get(0);
		boolean binary = baseResponseSignal.getBinaryData() != null;
		if (response != null) {
			LOGGER.debug(() -> aqpt.getAcquisitionPointIdentity() + ", Processor-> " + response.getClass().getName());
			for (SignalProcessingEventType.AcquiredSignal acquiredSignal : notificationEvent.getAcquiredSignal()) {
				//Step1: Match the Acquired Signal
				//Adding a new parameter CurrentSystemTime
				//TODO CS2-387 Avnee
				Date CurrentSystemTimeWithAddedDelta = new Date(System.currentTimeMillis() + CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis());
				if(LOGGER.isInfoEnabled())
					LOGGER.info("The current System Time with added delta( "+CppConfigurationBean.getInstance().getEsamResponseUTCTimeDeltaInMillis() +") is :  " + CurrentSystemTimeWithAddedDelta.getTime());
				response.matchAcquiredSignal(context, acquiredSignal, medias, aqpt.getAcquisitionPointIdentity(), CurrentSystemTimeWithAddedDelta);

				List<HostedAppEventStatusScte224NotifyModel> hostedAppEventStatusNotifyModels = new ArrayList<>();

				//Step2: Make A Notification Response
				
				response.generateResponse(acquiredSignal, notificationResponse, aqpt, ptsTimes, ptsAdjustments, context,
						responseToI03ResponseModelDeltaMap, hostedAppEventStatusNotifyModels, baseResponseSignal, CurrentSystemTimeWithAddedDelta);
				
				LOGGER.debug("Hosted App Event Status Scte224 NotifyModel " + hostedAppEventStatusNotifyModels);
				
				//Step3: Notify to hosted all at once
				if (hostedAppEventStatusNotifyModels != null && !hostedAppEventStatusNotifyModels.isEmpty()) {
					hostedAppEventStatusNotifyModels.stream().forEach(he -> {
						MediaRuntimeNotificationsHandler.notify224EventStatusToHostedImmediately(he);
					});

				}

				//Step4: For Signal State Request: 
				//Save Confirmed Blackout's signal id in Mule Context. Only few classes(program start) classes have this implementation at this time since only they can confirm the blackout.
				//The idea is to put this method here is to tell that we are going to finally add this signal for SSR from BaseSCCReponseComponent. BaseSCCReponseComponent currently saves it from common place for scheduleless/SCTE224 implementation.
				response.saveConfirmedSignalIDInContextForSSR(acquiredSignal.getAcquisitionSignalID(), aqpt.getAcquisitionPointIdentity(), context);

			}

			//Step5: Reset any metadata that we may have collected during the process. Like, leaving xml data if original request was binary
			if (binary && notificationResponse != null && notificationResponse.getResponseSignal() != null) {
				notificationResponse.getResponseSignal().stream().forEach(itr -> itr.setSCTE35PointDescriptor(null));
			}
			//Step6: Audit log will happen from SCCResponse layer just before sending to transcoders. No need to do Audit logging from this class. 
		}
		context.getMessage().setProperty(ContextConstants.I03_MODEL_DELTA, responseToI03ResponseModelDeltaMap, PropertyScope.OUTBOUND);
		context.getMessage().setProperty(SIGNAL_RESPONSE, notificationResponse, PropertyScope.OUTBOUND);
		return "";
	}

	public String objectToXML(final SignalProcessingNotificationType signalNotification, Schema schema) {
		return schema.getResponseHandler().generateSCCResponse(signalNotification, null);
	}
}
