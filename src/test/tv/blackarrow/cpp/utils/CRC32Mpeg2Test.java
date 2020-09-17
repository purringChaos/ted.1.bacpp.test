package test.tv.blackarrow.cpp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;

import com.envoisolutions.sxc.util.Base64;

import junit.framework.Assert;
import tv.blackarrow.cpp.utils.CRC32Mpeg2;

public class CRC32Mpeg2Test {

    private static String readResource(String filename) throws Exception {
        //URL url = CRC32Mpeg2Test.class.getResource(filename);
    	URL url = getTestFileAsURL(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();
        char[] buffer = new char[1024];
        while (true) {
            int length = reader.read(buffer);
            if (length == -1) {
                break;
            }
            stringBuffer.append(buffer, 0, length);
        }
        return stringBuffer.toString();
    }

    private void testCommon(String binaryData) {
        byte[] bytes = Base64.decode(binaryData);
        byte[] dataBytes = Arrays.copyOf(bytes, bytes.length - 4);
        long expected = 0;
        for (int i = bytes.length - 4; i < bytes.length; i++) {
            expected = (expected << 8) ^ (bytes[i] & 0xff);
        }
        expected = expected & 0xffffffffL;
        long actual = CRC32Mpeg2.getValue(dataBytes);
        // System.out.println(Long.toHexString(expected) + "," + Long.toHexString(actual));
        Assert.assertEquals("Failed validating " + binaryData, expected, actual);
    }

    @Test
    public void testRGBSamples() throws Exception {
        String samples = readResource("rgb_binary.txt");
        String[] binarySamples = samples.split("\n");
        for (String binaryData : binarySamples) {
            testCommon(binaryData);
        }
    }
    
    private static URL getTestFileAsURL(String filename) throws MalformedURLException {
    	if (new File("../etc/test_files/"+filename).exists()) {
    		return new File("../etc/test_files/"+filename).toURL();
    	} 
    	return new File("./etc/test_files/"+filename).toURL();
    }

    @Test
    public void testRGBTypical1() {
        testCommon("/DAqAAAAAAAA/////wVAAAY/f8//JdfzckxIAAAACgAIQ1VFSQAAAACRGgRi");
    }

    @Test
    public void testRGBTypical2() {
        testCommon("/DAvAAAAAAAA///wFAVAAArzf+//xbpe3/4AKT1sAAEAAAAKAAhDVUVJAAAAAOo6Lgc=");
    }

    @Test
    public void testRGBTypical3() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+OO6xSP4AUmXAAAEBAQAAriskeQ==");
    }

    @Test
    public void testRGBTypical4() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+lkX0yP4AUmXAAAEBAQAAYbaTxg==");
    }

    @Test
    public void testRGBTypical5() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+mX3uSP4AUmXAAAEBAQAAxUKe9Q==");
    }

    @Test
    public void testRGBTypical6() {
        testCommon("/DAzAAAAAAAAAP///wVQAAABf+/+vQHrln4AUmXAA+gAAAAOAQxDVUVJAN8xMjMqMTGT9qKm");
    }

    @Test
    public void testRGBTypical7() {
        testCommon("/DAvAAAAAAAA///wFAUAAABPf+/+66IFhn4AKT1sACkAAAAKAAhDVUVJAAAAANLno/c=");
    }

    @Test
    public void testEnvivioTypical1() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+Aa8HbP4AUmXAAAEBAQAAscaDhw==");
    }

    @Test
    public void testEnvivioTypical2() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+AQokuP4AUmXAAAEBAQAAYNwR+Q==");
    }

    @Test
    public void testEnvivioTypical3() {
        testCommon("/DAlAAAAAr8gAP/wFAUAAAABf+/+Aa8HbP4AUmXAAAEBAQAAscaDhw==");
    }

    @Test
    public void testIndependentlyVerifiedSamples() {
        // independently verified at http://www.zorc.breitbandkatze.de/crc.html
        // be sure to turn off both "reverse" options and set final xor value to 0
        // breaking these down into 4 values to accommodate the maximum input length on that online calculator
        long[] expected = new long[] { Long.parseLong("bcbd08f5", 16), Long.parseLong("f07509ca", 16),
                Long.parseLong("252d0a8b", 16), Long.parseLong("69e50bb4", 16) };
        byte[][] bytes = new byte[4][64];
        byte b = 0;
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < bytes[0].length; j++) {
                bytes[i][j] = b;
                b++;
            }
        }
        // print out strings that can be used as input on the above mentioned online calculator 
        //        for (int i = 0; i < bytes.length; i++) {
        //            for (int j = 0; j < bytes[0].length; j++) {
        //                int value = bytes[i][j] & 0xff;
        //                System.out.print("%" + ((value < 16) ? "0" : "") + Integer.toHexString(value));
        //            }
        //            System.out.println();
        //        }
        for (int i = 0; i < bytes.length; i++) {
            long actual = CRC32Mpeg2.getValue(bytes[i]);
            // System.out.println(Long.toHexString(expected[i]) + "," + Long.toHexString(actual));
            Assert.assertEquals(expected[i], actual);
        }
    }

}
