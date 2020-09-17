package test.tv.blackarrow.cpp.utils;

import junit.framework.TestCase;
import tv.blackarrow.cpp.utils.ESAMHelper;

public class ESAMHelperTest extends TestCase {

	public void testGenerateUpidString() {
		String signalId = "0888d75c-530e-11e3-a219-29cf21f8bdfb";
		String upid = ESAMHelper.generateUpidString(signalId);
		assertEquals("5349474e414c3a30383838643735632d353330652d313165332d613231392d323963663231663862646662", upid);
	}

	public void testParseUpidString() {
		String signalId = ESAMHelper.getSignalIdFromUPIDHexString("5349474e414c3a30383838643735632d353330652d313165332d613231392d323963663231663862646662");
		assertEquals("0888d75c-530e-11e3-a219-29cf21f8bdfb", signalId);
	}

	/*
	public void testCreateContentIndentficationRespSignal() {
	}

	public void testSetProgramStartResponseSignal() {
	}

	public void testConstructBlackoutNotification() {
	}
	 */
}
