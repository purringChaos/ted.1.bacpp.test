package tv.blackarrow.cpp.utils;

import org.mule.api.MuleEventContext;

import tv.blackarrow.cpp.i03.signaling.ProcessStatusNotificationType;
import tv.blackarrow.cpp.log.model.ALEssAuditLogVO;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.manifest.ManifestConfirmConditionEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;

public class AuditLogHelper {

	private static final String EMPTY_STRING = "";

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, SignalProcessingNotificationType notification) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(notification));
		return vo;
	}

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, SignalProcessingNotificationType notification, String altEventId) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(notification));
		vo.setAltEventId(altEventId);
		return vo;
	}

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, SignalProcessingEventType event) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(event));
		return vo;
	}

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, ManifestConfirmConditionEventType event) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(event));
		return vo;
	}

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, ProcessStatusNotificationType notification) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(notification));
		return vo;
	}

	public static PoisAuditLogVO populateAuditLogVO(MuleEventContext context, tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType notification) {
		PoisAuditLogVO vo = new PoisAuditLogVO();
		vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));
		vo.setAcquisitionSignalID(getAcquisitionSignalID(notification));
		return vo;
	}

	/*
	 * In any Linear POIS response, all response signal contains original same acquisitionSignalID as received in request.
	 */
	public static String getAcquisitionSignalID(final SignalProcessingNotificationType notification) {
		if(notification!=null && notification.getResponseSignal()!=null && !notification.getResponseSignal().isEmpty()) {
			return notification.getResponseSignal().get(0).getAcquisitionSignalID();
		}
		return EMPTY_STRING;
	}

	public static String getAcquisitionSignalID(SignalProcessingEventType event) {
		if(event != null && event.getAcquiredSignal() != null && !event.getAcquiredSignal().isEmpty()){
			return event.getAcquiredSignal().iterator().next().getAcquisitionSignalID();
		}
		return EMPTY_STRING;
	}

	public static String getAcquisitionSignalID(ManifestConfirmConditionEventType event) {
		if(event != null && event.getAcquiredSignal() != null && !event.getAcquiredSignal().isEmpty()){
			return event.getAcquiredSignal().iterator().next().getAcquisitionSignalID();
		}
		return EMPTY_STRING;
	}

	private static String getAcquisitionSignalID(ProcessStatusNotificationType notification) {
		if(notification != null && notification.getAcquisitionSignalID() != null){
			return notification.getAcquisitionSignalID();
		}
		return EMPTY_STRING;
	}

	public static String getAcquisitionSignalID(final tv.blackarrow.cpp.i03.signaling.SignalProcessingNotificationType notification) {
		if(notification!=null && notification.getResponseSignal()!=null && !notification.getResponseSignal().isEmpty()) {
			return notification.getResponseSignal().get(0).getAcquisitionSignalID();
		}
		return EMPTY_STRING;
	}

	public static ALEssAuditLogVO populateALNormalizeAuditLogVO(MuleEventContext context) {	  ALEssAuditLogVO vo = populateALAuditLogVO();      vo.setIpAddressOfClient(MuleContextHelper.getClientIPAddressDetail(context));      return vo;  }		public static ALEssAuditLogVO populateALAuditLogVO() {      ALEssAuditLogVO vo = new ALEssAuditLogVO();      vo.setUniqueId(UUIDUtils.getUUID().toString());      return vo;  }

}
