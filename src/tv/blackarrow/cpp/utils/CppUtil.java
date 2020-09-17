package tv.blackarrow.cpp.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;

import tv.blackarrow.cpp.exeptions.CppException;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.signal.signaling.ObjectFactory;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants.ESSNodeDeliveryType;

public final class CppUtil {

	private static final Logger LOGGER = LogManager.getLogger(CppUtil.class.getName());
	private static final CppUtil INSTANCE = new CppUtil();
	private static ESSNodeDeliveryType clusterType;
	private static ObjectFactory I01_SIGNALING_CHILD_OBJECT_FACTORY = new ObjectFactory();

	private CppUtil() {}

	public static CppUtil getInstance() {
		LOGGER.debug("Returning singleton isntance of BaCppUtil class");
		return INSTANCE;
	}

	public Object cloneObject(Object object) throws CppException {
		return convertStringToObject(convertObjectToString(object), object.getClass());
	}

	public Object convertStringToObject(String string, Class<? extends Object> objClass) throws CppException {
		Object string2Object = null;
		try {
			XStream xStream = new XStream();
			xStream.alias(objClass.getSimpleName(), objClass);
			string2Object = xStream.fromXML(string);
		} catch(Exception ex) {
			LOGGER.debug("ERROR occured in convertStringToObject(String string)", ex);
		}
		return string2Object;
	}

	public String convertObjectToString(Object object) throws CppException {
		String object2String = null;
		try {
			XStream xStream = new XStream();
			xStream.alias(object.getClass().getSimpleName(), object.getClass());
			object2String = xStream.toXML(object);
		} catch(Exception ex) {
			LOGGER.debug("ERROR occured in convertObjectToString(Object object)", ex);
		}
		return object2String;
	}

	/**
	 * define a key valued (feedid and event time combination)
	 * @param feedId
	 * @param eventTime
	 * @return
	 */
	public String generateFeedIdKey(String feedId, String eventTime) {
		return feedId + "@" + eventTime;
	}

	public static ResponseSignalType addNewResponseSignal(SignalProcessingNotificationType notification, SignalProcessingEventType.AcquiredSignal signal) {
		if (notification == null || signal == null) {
			return null;
		}
		ResponseSignalType responseSignal = getBasicPopulationInNewResponseSignal(notification, signal);
		responseSignal.setSCTE35PointDescriptor(getCopy(signal.getSCTE35PointDescriptor()));

		return responseSignal;
	}

	public static ResponseSignalType addNewResponseSignalWithoutDescriptorInfo(SignalProcessingNotificationType notification, SignalProcessingEventType.AcquiredSignal signal) {
		if (notification == null || signal == null) {
			return null;
		}
		ResponseSignalType responseSignal = getBasicPopulationInNewResponseSignal(notification, signal);
		responseSignal.setSCTE35PointDescriptor(getCopyWithoutDescriptorInfo(signal.getSCTE35PointDescriptor()));

		return responseSignal;
	}
	public static Boolean isInflightMedia(long mediaStartTime, long mediaEndTime, long currentSystemTime) {
		if(mediaStartTime < currentSystemTime && mediaEndTime > currentSystemTime) {
			return true;
		}
		return false;
	}
	private static ResponseSignalType getBasicPopulationInNewResponseSignal(SignalProcessingNotificationType notification, SignalProcessingEventType.AcquiredSignal signal) {
		ResponseSignalType responseSignal = new ResponseSignalType();
		notification.getResponseSignal().add(responseSignal);
		responseSignal.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());
		responseSignal.setAcquisitionSignalID(signal.getAcquisitionSignalID());
		responseSignal.setAcquisitionTime(signal.getAcquisitionTime());
		responseSignal.setSignalPointID(signal.getSignalPointID());
		responseSignal.setAction("replace");
		responseSignal.setUTCPoint(signal.getUTCPoint());
		responseSignal.setStreamTimes(signal.getStreamTimes());
		responseSignal.setBinaryData(signal.getBinaryData());
		return responseSignal;
	}

	public static SCTE35PointDescriptorType getCopy(SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		SCTE35PointDescriptorType scte = new SCTE35PointDescriptorType();
		scte.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		scte.setSpliceInsert(scte35PointDescriptor.getSpliceInsert());
		scte.getSegmentationDescriptorInfo().addAll(scte35PointDescriptor.getSegmentationDescriptorInfo());
		return scte;
	}

	public static SCTE35PointDescriptorType getCopyWithoutDescriptorInfo(SCTE35PointDescriptorType scte35PointDescriptor) {
		if(scte35PointDescriptor == null){return null;}
		SCTE35PointDescriptorType scte = new SCTE35PointDescriptorType();
		scte.setSpliceCommandType(scte35PointDescriptor.getSpliceCommandType());
		scte.setSpliceInsert(scte35PointDescriptor.getSpliceInsert());
		return scte;
	}

	public static ESSNodeDeliveryType getClusterType() {
		if (clusterType == null) {
			clusterType = ESSNodeDeliveryType.ALL;
			try {
				clusterType = CppConfigurationBean.getInstance().getDeliveryType() != null
						? ESSNodeDeliveryType.valueOf(CppConfigurationBean.getInstance().getDeliveryType().trim().toUpperCase())
						: clusterType;
			} catch (Exception e) {

			}
		}
		return clusterType;
	}

	public static SegmentationDescriptorType deepCopy(SegmentationDescriptorType seg) {
		if (seg == null) {
			return null;
		}
		SegmentationDescriptorType segmentationDescriptorType = I01_SIGNALING_CHILD_OBJECT_FACTORY.createSegmentationDescriptorType();
		segmentationDescriptorType.setDuration(seg.getDuration());
		segmentationDescriptorType.setSegmentationEventCancelIndicator(seg.isSegmentationEventCancelIndicator());
		segmentationDescriptorType.setSegmentEventId(seg.getSegmentEventId());
		segmentationDescriptorType.setSegmentNum(seg.getSegmentNum());
		segmentationDescriptorType.setSegmentsExpected(seg.getSegmentsExpected());
		segmentationDescriptorType.setSegmentTypeId(seg.getSegmentTypeId());
		segmentationDescriptorType.setUpid(seg.getUpid());
		segmentationDescriptorType.setUpidType(seg.getUpidType());
		segmentationDescriptorType.getOtherAttributes().putAll(seg.getOtherAttributes());
		return segmentationDescriptorType;
	}

}
