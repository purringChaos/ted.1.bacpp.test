package tv.blackarrow.cpp.normalize;

import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SkyScte35BinaryUtil;

/**
 * AbstractScte35NormalizationHandler implements the common method to generate 
 * the normalized binary format, and leaves updateResponseSignalActionAndBinary 
 * implementation to different handler to implement
 * 
 * 
 * @author jwang
 *
 */
public abstract class AbstractScte35NormalizationHandler implements Scte35NormalizationHandler {
	private static final Logger LOG = LogManager.getLogger(AbstractScte35NormalizationHandler.class);
	
	public final static String DELIVERY_NOT_RESTRICTED_FLAG = "deliveryNotRestrictedFlag";
	public final static String WEB_DELIVERY_ALLOW_FLAG = "webDeliveryAllowedFlag";
	public final static String NO_REGIONAL_BLACKOUT_FLAG = "no_regional_blackout_flag";
	
	public AbstractScte35NormalizationHandler() {};

	@Override
	public SignalProcessingNotificationType normalizeSignal(final SignalProcessingEventType event) {
		SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
		
		final StringBuilder pts = new StringBuilder(); 
		final StringBuilder ptsAdjustments = new StringBuilder(); 
		final StringBuilder providerAvailId = new StringBuilder(); 

		for (SignalProcessingEventType.AcquiredSignal signal :  event.getAcquiredSignal()) {
			pts.setLength(0);  // reuse existing buffer without create a new StringBuilder object
			ptsAdjustments.setLength(0);  
			providerAvailId.setLength(0); 
			
			notification.setAcquisitionPointIdentity(signal.getAcquisitionPointIdentity());

			ResponseSignalType responseSignal = CppUtil.addNewResponseSignal(notification, signal);
			
			// update binary data
			final byte[] encoded = Base64.encodeBase64(signal.getBinaryData().getValue());
			SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
			SkyScte35BinaryUtil.decodeScte35BinaryData(encoded, scte35Pt, pts, ptsAdjustments, providerAvailId);
			
			updateResponseSignalActionAndBinary(responseSignal, scte35Pt, pts, ptsAdjustments, providerAvailId);
			
			scte35Pt.setSpliceInsert(null);  // remove splice_insert if there is one
		}
		
		return notification;
	}
		
	/**
	 * the implementation should handle splcie_insert and time_signal properly, and it depends on the handler version
	 *  
	 * @param responseSignal
	 * @param scte35Pt
	 * @param pts
	 * @param ptsAdjustments
	 * @param providerAvailId
	 */
	public abstract void updateResponseSignalActionAndBinary(final ResponseSignalType responseSignal, final SCTE35PointDescriptorType scte35Pt,
			final StringBuilder pts, final StringBuilder ptsAdjustments, final StringBuilder providerAvailId);
	
	
	public BinarySignalType generateNormalizedBinary(final SCTE35PointDescriptorType scte35Pt, final StringBuilder pts,
			final StringBuilder ptsAdjustments, StringBuilder providerAvailId) {
		long spotId = Long.parseLong(providerAvailId.toString(), 2);
		LOG.debug(()->"==>>" + pts + " / " + scte35Pt.getSpliceInsert().getDuration() + " / "  + providerAvailId + " / " + spotId);
		
		scte35Pt.setSpliceCommandType(6); // time signal

		SegmentationDescriptorType segment ;
		if(scte35Pt.getSpliceInsert() != null) {
			segment = new SegmentationDescriptorType();
			scte35Pt.getSegmentationDescriptorInfo().add(segment);
			segment.setSegmentEventId(scte35Pt.getSpliceInsert().getSpliceEventId()); 
			segment.setDuration(scte35Pt.getSpliceInsert().getDuration());
		} else {
			segment = scte35Pt.getSegmentationDescriptorInfo().get(0);
		}
		segment.setUpidType((short)0x0C); // 0x0c

		String upidStr = segmentation_upid(spotId);
		final byte[] upid = getBytefromBits(upidStr);
		segment.setUpid(upid);
		segment.setSegmentTypeId((short)0x36);  // segment.setSegmentTypeId
		segment.setSegmentNum((short)0);
		segment.setSegmentsExpected((short)0);

		segment.getOtherAttributes().put(new QName(DELIVERY_NOT_RESTRICTED_FLAG), "1");
		segment.getOtherAttributes().put(new QName(WEB_DELIVERY_ALLOW_FLAG), "1");
		segment.getOtherAttributes().put(new QName(NO_REGIONAL_BLACKOUT_FLAG), "1");

		final String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, pts.toString(), ptsAdjustments.toString());
		final BinarySignalType binarySignal = new BinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType("SCTE35");

		return binarySignal;
	}
	
	/**
	 * generate segment UPID string using event_id
	 * check SCTE 35 spec
	 * @param eventId
	 * @return UPID 
	 */
	public String segmentation_upid(final long eventId) {
		return MPU(eventId);
	}
	
	/*
	 * the algorithm / syntax is defined in the format agreed by Cadent and LG VM
	 */
	protected String MPU(final long eventId) {
		final StringBuilder sb = new StringBuilder();
		// form identifier
		sb.append(Scte35BinaryUtil.toBinary((byte)('L')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('B')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('T')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('Y')));
		sb.append("00000000"); // 0x00 - version
		sb.append("00000000"); // 0x00 - basic opportunity (timing trigger)
		sb.append(Scte35BinaryUtil.toBitString(eventId, 128)); // event_id - spot id
//		sb.append("00000001"); // ba_ownership

		return sb.toString();
	}
	
	
	/**
	 * a function return byte arrays from a bit String
	 * @param bits
	 * @return byte arrays
	 */
	protected byte[] getBytefromBits(String bits) {
		bits = bits + '0';
		final byte[] vals = new byte[bits.length()/8];
		for(int idx = 0; idx < bits.length() - 1; idx += 8) {
			final String str = bits.substring(idx, idx + 8);
			vals[idx/8] = (byte)Integer.parseInt(str, 2);
		}
		
		return vals;
	}
	
	
}
