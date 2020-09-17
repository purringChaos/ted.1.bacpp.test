package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224;

import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;

import tv.blackarrow.cpp.i03.signaling.BinarySignalType;
import tv.blackarrow.cpp.i03.signaling.ResponseSignalType;
import tv.blackarrow.cpp.i03.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.i03.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.BaseNotificationMessageBuilderImpl;
import tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.scte224.ip.manifestalevel.OutOfBandNotificationInfo;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public abstract class SCTE224NotificationMessageBuilderImpl extends BaseNotificationMessageBuilderImpl {
	@Override
	public String getNotificationMessage(NotificationMessage notificationMessage) {
		String message = null;
		switch (notificationMessage.getSchema()) {
		case i03:
			message = getI03SchemaUpstreamNotificatonMessage(notificationMessage);
			break;
		default:
			break;

		}
		return message;
	}

	protected static void setBasicSegmentInfo(final byte[] upId, final Short upidType, SegmentationDescriptorType segment) {
		segment.setUpidType(upidType);
		segment.setUpid(upId);
		segment.setSegmentNum((short) 1);
		segment.setSegmentsExpected((short) 1);
	}
	
	protected static void setSegmentDescriptorAttributesInResponseSignal(final SegmentationDescriptorType segment,
			OutOfBandNotificationInfo outOfBandNotificationInfo) {
		segment.getOtherAttributes().put(new QName(CppConstants.DELIVERY_NOT_RESTRICTED_FLAG), "0");

		segment.getOtherAttributes().put(new QName(CppConstants.NO_REGIONAL_BLACKOUT_FLAG), String.valueOf(outOfBandNotificationInfo.getNoRegionalBlackout()));
		segment.getOtherAttributes().put(new QName(CppConstants.DEVICE_RESTRICTIONS), String.valueOf(outOfBandNotificationInfo.getDeviceRestrictions()));

		segment.getOtherAttributes().put(new QName(CppConstants.WEB_DELIVERY_ALLOW_FLAG), "1");
		segment.getOtherAttributes().put(new QName(CppConstants.ARCHIVE_ALLOWED_FLAG), "1");
	}
	
	protected static void setSegmentDescriptorAsBinaryInResponseSignal(final String ptsTimeInBinary, final String pts_adjustment, final ResponseSignalType respSignalType,
			final SCTE35PointDescriptorType scte35Pnt) {
		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(getDeepCopy(scte35Pnt), ptsTimeInBinary, pts_adjustment);
		BinarySignalType binarySignal = I03_SIGNALING_OBJECT_FACTORY.createBinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType("SCTE35");
		respSignalType.setBinaryData(binarySignal);
		respSignalType.setSCTE35PointDescriptor(null);
	}
	

	protected abstract String getI03SchemaUpstreamNotificatonMessage(NotificationMessage notificationMessage);;

}
