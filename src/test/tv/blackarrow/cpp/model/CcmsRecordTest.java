package test.tv.blackarrow.cpp.model;

import java.text.ParseException;
import java.util.Date;

import junit.framework.TestCase;
import tv.blackarrow.cpp.model.CcmsRecord;

public class CcmsRecordTest extends TestCase {
	private String record = "LOI 1227 002900 0014 0030 001 001 000030 000000 00000000 000 00000039452 0000 spotlight.com                    AD0100BP1            Fill";
	private CcmsRecord ccmsRecord;
	
	protected void setUp() throws Exception {
		super.setUp();
		ccmsRecord = new CcmsRecord(record, 2011);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetProvider() {
		assertEquals("spotlight.com", ccmsRecord.getProvider());
	}

	public void testGetAssetRef() {
		assertEquals("AD0100BP1", ccmsRecord.getAssetRef());
	}
	
	public void testGetScheduleDate() {
		try {
			Date date = ccmsRecord.getScheduleDateTime();
			String dateStr = CcmsRecord.DATETIME_FORMAT.format(date);
			assertEquals("20111227002900", dateStr);
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	public void testGetWindowStartTime() {
		try {
			Date date = ccmsRecord.getWindowStartTime();
			String dateStr = CcmsRecord.DATETIME_FORMAT.format(date);
			assertEquals("20111227001400", dateStr);
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	public void testGetWindowEndTime() {
		try {
			Date date = ccmsRecord.getWindowEndTime();
			String dateStr = CcmsRecord.DATETIME_FORMAT.format(date);
			assertEquals("20111227004400", dateStr);
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	public void testGetBreakNumber() {
		assertEquals(1, ccmsRecord.getBreakNumber());
	}
	
	public void testGetPositionNumber() {
		assertEquals(1, ccmsRecord.getPositionNumber());
	}
}
