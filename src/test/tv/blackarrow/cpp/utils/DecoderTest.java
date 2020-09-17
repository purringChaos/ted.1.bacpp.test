package test.tv.blackarrow.cpp.utils;


import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import junit.framework.TestCase;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

public class DecoderTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testDecoder() throws IOException {
		String str2 = "/D//AH/////////wDwUAAozof8/+1Ykjv1VmAAAAAP//////////";
		System.out.println("\n\nto be decoded");
		System.out.println(str2);
		
		byte[] decoded = Base64.decodeBase64(str2.getBytes());
		 
        System.out.println(decoded);		
        System.out.println(Arrays.toString(decoded));	
        
        StringBuilder str = new StringBuilder();
        for(byte bt : decoded) {
        	str.append(Scte35BinaryUtil.toBinary(bt));
        }
        System.out.println("\n" + str.toString());	
        
        
        StringBuilder res = new StringBuilder();
		for(int i = 0; i < str.length(); i += 4 ) {
			int value = Integer.parseInt(str.substring(i, i + 4), 2);
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
		System.out.println("HEX string length: " + res.toString().length());
	}

}
