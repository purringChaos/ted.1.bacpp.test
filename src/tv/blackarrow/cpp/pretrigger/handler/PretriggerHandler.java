package tv.blackarrow.cpp.pretrigger.handler;

import java.math.BigInteger;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SegmentationDescriptorType;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;
import tv.blackarrow.cpp.utils.SpliceCommandType;

/**
 * the utility will be response to handle pre-trigger required
 * SCTE-35 binary data generation and XML message generation
 * based on i03 ESAM spec
 * 
 * @author jwang
 *
 */
public class PretriggerHandler {
	private static final Logger LOG = LogManager.getLogger(HintScheduleHandler.class);
	private static final PretriggerHandler INSTANCE = new PretriggerHandler();
	
	private PretriggerHandler() {	}
	
	public static final PretriggerHandler getInstance() {
		return INSTANCE;
	}

	/**
	 * Given a break ID and duration, generate SCTE-35 binary data
	 * 
	 * it can be verified by post the generated binary data to
	 * http://ess-hostname:6666/scc/bc
	 *  
	 * @param breakId
	 * @param uuid corresponding to a uuid 
	 * @param duration
	 * @return SCTE35 binary data
	 */
	public String generateEncodedScte35String(long breakId, String uuid, long duration) {
		//SignalProcessingEventType.AcquiredSignal signal = new SignalProcessingEventType.AcquiredSignal();
		SCTE35PointDescriptorType scte35Pnt = new SCTE35PointDescriptorType();
		scte35Pnt.setSpliceCommandType(SpliceCommandType.TIME_SIGNAL.getCommandtype()); 

		SegmentationDescriptorType segment = new SegmentationDescriptorType();
		scte35Pnt.getSegmentationDescriptorInfo().add(segment);
		segment.setSegmentEventId(breakId);
		
		// duration of this content identification segmentation
		try {
			segment.setDuration(JavaxUtil.getDatatypeFactory().newDuration(duration));
		} catch (DatatypeConfigurationException e) {
			LOG.error(()->e.getMessage());
		}
		segment.setUpidType((short)0x0C);  // managed private UPID  (MPU)
		
		String upidStr = segmentation_upid(uuid);
		final byte[] upid = Scte35BinaryUtil.getBytefromBits(upidStr);// Scte35Util.getHexFromBitString(upidStr);
		segment.setUpid(upid);
		segment.setSegmentTypeId((short)54);  // 0x36 Ad Avail
		segment.setSegmentNum((short)0);
		segment.setSegmentsExpected((short)0);
		segment.setSegmentationEventCancelIndicator(false);
//		segment.getOtherAttributes().put(new QName(Scte35BinaryUtil.DELIVERY_NOT_RESTRICTED_FLAG), "1");
		
		String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pnt, "", Scte35BinaryUtil.toBitString(0, 33));
		
		return encodedStr; 
	}
	
	/**
	 * The algorithm to generate segmentation_upid()  
	 * @param uuid - defined in hint schedule ( corresponding to break ID)
	 * @return upid
	 */
	public String segmentation_upid(String uuidStr) {
		return MPU(uuidStr);
	}
	
	private String MPU(String uuidStrAsBigInteger) {
		StringBuilder sb = new StringBuilder();
		// form identifier
		sb.append(Scte35BinaryUtil.toBinary((byte)('L')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('B')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('T')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('Y')));

		sb.append("00000000"); // 0x00 - version
		sb.append("00010000"); // 0x10 - basic opportunity (pre-trigger) command
		BigInteger bigNum = new BigInteger(uuidStrAsBigInteger);
		sb.append(Scte35BinaryUtil.toBitString(bigNum, 128)); // event_id
//		sb.append("00000001"); // ba_ownership
		
		return sb.toString();
	}

	/*
	private String MPU2(String uuidStr) {
		StringBuilder sb = new StringBuilder();
		// form identifier
		sb.append(Scte35BinaryUtil.toBinary((byte)('L')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('B')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('T')));
		sb.append(Scte35BinaryUtil.toBinary((byte)('Y')));

		sb.append("00010000"); // 0x10 - basic opportunity (pre-trigger) command
		
		//sb.append(Scte35BinaryUtil.toBitString(breakId, 128)); // event_id
		UUID uuid = UUID.fromString(uuidStr);
		byte[] bytes = UUIDUtils.asByteArray(uuid);
		for(byte val : bytes) {
			sb.append(Scte35BinaryUtil.toBinary(val));
		}
		
		sb.append("00000001"); // ba_ownership
		
		return sb.toString();
	} */
	
}
