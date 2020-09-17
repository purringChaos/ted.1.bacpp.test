package test.tv.blackarrow.cpp.utils;

import java.math.BigInteger;
import java.util.Arrays;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;

import org.apache.commons.codec.binary.Base64;

import junit.framework.TestCase;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.SpliceInsertType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType.AcquiredSignal;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public class Scte35BinaryUtilTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	
	public void testToBinary() {
		String value = Scte35BinaryUtil.toBinary((byte)1);
		assertEquals("00000001", value);
		
		value = Scte35BinaryUtil.toBinary((byte)2);
		assertEquals("00000010", value);

		value = Scte35BinaryUtil.toBinary((byte)3);
		assertEquals("00000011", value);

	
		value = Scte35BinaryUtil.toBinary((byte)4);
		assertEquals("00000100", value);
	
		value = Scte35BinaryUtil.toBinary((byte)8);
		assertEquals("00001000", value);

		value = Scte35BinaryUtil.toBinary((byte)15);
		assertEquals("00001111", value);
	}
	

	public void testToBitString() {		
		String value;
		
		value = Scte35BinaryUtil.toBitString(1, 1);
		assertEquals("1", value);

		value = Scte35BinaryUtil.toBitString(1, 3);
		assertEquals("001", value);
		
		value = Scte35BinaryUtil.toBitString(5, 5);
		assertEquals("00101", value);

		value = Scte35BinaryUtil.toBitString(10, 7);
		assertEquals("0001010", value);
	}
	
	public void testToBitStringFromBigInteger() {		
		String value;
		BigInteger bigNum = new BigInteger("48834629195901497565931995283161002217");
		value = Scte35BinaryUtil.toBitString(bigNum, 128);
		assertEquals("00100100101111010011010100011100101100101010000111000101001001000101011001111010100111010100101011101010011110101011000011101001", value);

		bigNum = new BigInteger("48834629195901497565931995283161002216");
		value = Scte35BinaryUtil.toBitString(bigNum, 128);
		assertEquals("00100100101111010011010100011100101100101010000111000101001001000101011001111010100111010100101011101010011110101011000011101000", value);
	}
	

	public void testGetHexFromBitString() {
		String value;
		
		value =  Scte35BinaryUtil.getHexFromBitString("0100");
		assertEquals("4", value);

		value =  Scte35BinaryUtil.getHexFromBitString("0110");
		assertEquals("6", value);
		
		value =  Scte35BinaryUtil.getHexFromBitString("1110");
		assertEquals("E", value);
		
		value =  Scte35BinaryUtil.getHexFromBitString("10100100");
		assertEquals("A4", value);

		try {
			value =  Scte35BinaryUtil.getHexFromBitString("101000100");
			fail("should have exception before this");
		} catch(Exception e) {
		}

		value =  Scte35BinaryUtil.getHexFromBitString("10101110");
		assertEquals("AE", value);
		
		value =  Scte35BinaryUtil.getHexFromBitString("111110101110");
		assertEquals("FAE", value);
		
	}
	
	public void testScte35SpliceInfoSectionSpliceInsert() { 
		System.out.println("\n\n=======================\nSplice Insert Processing\n=======================");
		SignalProcessingEventType event = new SignalProcessingEventType();
		
		AcquiredSignal signal = new AcquiredSignal();
		event.getAcquiredSignal().add(signal);
		SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
		signal.setSCTE35PointDescriptor(scte35Pt);
		
		scte35Pt.setSpliceCommandType(CppConstants.SCTE35_SPLICE_INSERT_COMMAND_TYPE);
		SpliceInsertType insertType = new SpliceInsertType();
		scte35Pt.setSpliceInsert(insertType);
		
		insertType.setAvailNum((short)0);
		insertType.setAvailsExpected((short)0);
		insertType.setOutOfNetworkIndicator(true);
		insertType.setSpliceEventCancelIndicator(false);
		insertType.setSpliceEventId(566765754l);
		insertType.setUniqueProgramId(7586546);
		
		Duration duration;
		try {
			duration = JavaxUtil.getDatatypeFactory().newDuration(60*1000);
			insertType.setDuration(duration);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		SCTE35PointDescriptorType pdType = event.getAcquiredSignal().get(0).getSCTE35PointDescriptor(); 
		String result = Scte35BinaryUtil.scte35SpliceInfoSection(pdType, "", Scte35BinaryUtil.toBitString(0, 33));
		
		System.out.println(result);
		System.out.println("length: " + result.length());
		
		StringBuilder res = new StringBuilder();
		for(int i = 0; i < result.length()/4; i++ ) {
			int value = Integer.parseInt(result.substring(i, i + 4), 2);
			switch(value) {
			case 10: res.append('A'); break;
			case 11: res.append('B'); break;
			case 12: res.append('C'); break;
			case 13: res.append('D'); break;
			case 14: res.append('E'); break;
			case 15: res.append('F'); break;
			default: res.append(value); break;
			}
		}
		System.out.println("HEX string: \n" + res.toString());
		
		
		byte[] bts = Base64.encodeBase64(res.toString().getBytes());
		System.out.println("HEX string encoded: \n" + Arrays.toString(bts));
		
		System.out.println("********************");
		
	}
	
	/*
	public void testScte35SpliceInfoSectionTimeSignal() { 
		System.out.println("\n\n=======================\nTime Signal Processing\n=======================");
		
		SignalProcessingEventType event = new SignalProcessingEventType();
		
		AcquiredSignal signal = new AcquiredSignal();
		event.getAcquiredSignal().add(signal);
		SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
		signal.setSCTE35PointDescriptor(scte35Pt);
		
		scte35Pt.setSpliceCommandType(CppConstants.SCTE35_TIME_SIGNAL_COMMAND_TYPE);
		SegmentationDescriptorType sdType = new SegmentationDescriptorType();
		scte35Pt.getSegmentationDescriptorInfo().add(sdType);
		
		Duration duration;
		try {
			duration = JavaxUtil.getDatatypeFactory().newDuration(60*1000);
			sdType.setDuration(duration);
			sdType.setSegmentEventId(99790150L);
			sdType.setUpidType((short)9);
			sdType.setUpid("435834565465446546".getBytes());
			sdType.setSegmentTypeId((short)50);
			sdType.setSegmentNum((short)0);
			sdType.setSegmentsExpected((short)0);
			sdType.setSegmentationEventCancelIndicator(false);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		SCTE35PointDescriptorType pdType = event.getAcquiredSignal().get(0).getSCTE35PointDescriptor(); 
		String result = Scte35BinaryUtil.scte35SpliceInfoSection(pdType, "");
		
		System.out.println(result);
		assertEquals(312, result.length());
		System.out.println("HEX string:");
		
		String hexStr = Scte35BinaryUtil.getHexFromBitString(result);
		System.out.println(hexStr);
		
		System.out.println("HEX string encoded: \n" + Base64.encodeBase64(hexStr.getBytes()));
		
		System.out.println("********************");
	}
	*/
	
	public void testDecode() {
		String str = "F00F0500028CE87FCFFED58923BF556600000000";
		
		String str2 = "8A8FAAKM6H/P/tWJI79VZgAAAAA=";
		byte[] decoded = Base64.decodeBase64(str2.getBytes());
        System.out.println("decoded of string " + str2);		
        System.out.println(decoded);		
        System.out.println(Arrays.toString(decoded));		

		StringBuilder vals = new StringBuilder();
		for(byte bt : decoded) {
			vals.append(Scte35BinaryUtil.toBinary(bt));
		}
		System.out.println("value of decoded in bit: " + vals.toString());
		
		
//		assertEquals(str, value);
		
/*		
		int base = 2;
		//int decimal = Integer.parseInt(binaryString, base);
		//System.out.println("table_id: " + vals.substring(0, 8) + "  value: " + Integer.parseInt(vals.substring(0, 8), base));
		
		System.out.println("command length: " + vals.substring(4, 16) + "  value: " + Integer.parseInt(vals.substring(4, 16), base));
		
		int commandType = Integer.parseInt(vals.substring(16, 24), base);
		System.out.println("command type: " + commandType);
		
		if(commandType == 5) {  // insert
			// event ID
			long eventId = Long.parseLong(vals.substring(24, 56), base);
			System.out.println("eventId: " + eventId);
			
			int cancelFlag = Integer.parseInt(vals.substring(56, 57), base);
			System.out.println("cancel flag: " + cancelFlag);
		}
		else if(commandType == 6) {  // time signal
			
		}
		*/
	}
	
	public void testEncode() {
		
		System.out.println("\n\n==========test encoded=============");
		
		String str = "F00F0500028CE87FCFFED58923BF556600000000";
		
		String str2 = "8A8FAAKM6H/P/tWJI79VZgAAAAA=";

		StringBuilder vals = new StringBuilder();
		byte[] bts = Base64.encodeBase64(str.getBytes());//"1111000000001111000001010000000000000010100011001110100001111111110011111111111011010101100010010010001110111111010101010110011000000000000000000000000000000000".getBytes());
		System.out.println(bts.toString());
		System.out.println(Base64.encodeBase64("1111000000001111000001010000000000000010100011001110100001111111110011111111111011010101100010010010001110111111010101010110011000000000000000000000000000000000".getBytes()));
		//System.out.println(Scte35BinaryUtil.getHexFromBitString( bts.toString()));
		
	}	
	
	
	public void testDecodeBinaryData() {
		System.out.println("\n\n==========test testDecodeBinaryData=============");
		
		String binaryData = "/DAqAAAAAAAA/////wVAAAY/f8//JdfzckxIAAAACgAIQ1VFSQAAAACRGgRi";
		SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();		
		Scte35BinaryUtil.decodeScte35BinaryData(binaryData, scte35Pt, new StringBuilder(), new StringBuilder());

		assertNotNull(scte35Pt.getSpliceInsert());
		assertEquals(5, scte35Pt.getSpliceCommandType());
		assertEquals(0, scte35Pt.getSpliceInsert().getAvailNum().intValue());
		assertEquals(0, scte35Pt.getSpliceInsert().getAvailsExpected().intValue());
		assertEquals(1073743423, scte35Pt.getSpliceInsert().getSpliceEventId().longValue());
	} 
	

	public void testEncodeScte35DataToBinary() {
		SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();
		
		scte35Pt.setSpliceCommandType(5);
		SpliceInsertType insertType = new SpliceInsertType();
		scte35Pt.setSpliceInsert(insertType);
		
		insertType.setAvailNum((short)0);
		insertType.setAvailsExpected((short)0);
		insertType.setOutOfNetworkIndicator(true);
		insertType.setSpliceEventCancelIndicator(false);
		insertType.setSpliceEventId(566765754l);
		insertType.setUniqueProgramId(7586546);
		
		Duration duration;
		try {
			duration = JavaxUtil.getDatatypeFactory().newDuration(60*1000);
			insertType.setDuration(duration);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		try {
			String encodedStr = Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, "", Scte35BinaryUtil.toBitString(0, 33));
			System.out.println(encodedStr);
		} catch(Exception ex) {
			fail(ex.getMessage());
		}
	}
	
}
