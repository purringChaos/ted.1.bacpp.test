package test.tv.blackarrow.cpp.smoketest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import junit.framework.Assert;
import tv.blackarrow.cpp.model.CppConfigurationBean;


public class SCCMCCRequestTest extends SmokeTestBase {
	private static String POIS_LINEAR_SCC_URL = "http://localhost:6640/scc/signal";
	private static String POIS_LINEAR_MCC_URL = "http://localhost:6660/mcc/signal";
	private static String POIS_LINEAR_LOADER_URL = "http://localhost:6650/admin";
	
//	private static String HOSTNAME = "app11.tst.sn.blackarrow.tv"; // "app53.tst.sn.blackarrow.tv";
	private static String HOSTNAME = "localhost"; // "app53.tst.sn.blackarrow.tv";
	
	private static String ACQUISITION_POINT = "FID01_AP1";
	private static String ACQUISITION_POINT_2 = "FID02_AP1";
	
//	@BeforeClass
//	public void init() throws Exception {
////		testPOISLoader();
//	}
	
	private boolean confirmTimeSignal = false;
	
	{
		POIS_LINEAR_SCC_URL = POIS_LINEAR_SCC_URL.replace("localhost", HOSTNAME);
		POIS_LINEAR_MCC_URL = POIS_LINEAR_MCC_URL.replace("localhost", HOSTNAME);
		POIS_LINEAR_LOADER_URL = POIS_LINEAR_LOADER_URL.replace("localhost", HOSTNAME);
	}
	
	private String prepareSCCRequest(String sccTemplate, String timeStamp, String acquistionPoint) throws IOException {
		InputStream sccStream = this.getClass().getResourceAsStream(sccTemplate);
		String scc = IOUtils.toString(sccStream);
		
		scc = scc.replaceAll("CURRENT_TIME", timeStamp);
		scc = scc.replaceAll("ACQUISITION_POINT", acquistionPoint);
		
		return scc; 
	}
	
	private String sendSCCRequest(String sccTemplate) throws Exception {
		return sendSCCRequest(sccTemplate, 0);
	}
	
	private String sendSCCRequest(String sccTemplate, int offset) throws Exception {
		String timeStamp = getCurrentRequestTime(offset);
		return sendSCCRequest(sccTemplate, timeStamp);
	}

	private String sendSCCRequest(String sccTemplate, String timeStamp) throws Exception {
		return sendSCCRequest(sccTemplate, timeStamp, ACQUISITION_POINT);
	}

	private String sendSCCRequest(String sccTemplate, String timeStamp, String acquisitonPoint) throws Exception {
		String message = prepareSCCRequest(sccTemplate, timeStamp, acquisitonPoint);
		return sendSCCRequestMessage(message);
	}

	private String sendSCCRequestMessage(String message) throws Exception {
		String url = POIS_LINEAR_SCC_URL.replace("localhost", HOSTNAME);
		System.out.println("scc request : " + url + "\n" + message + "\n");
		String ret = post(url, message);
		return ret;
	}

	private String prepareMCCRequest(String mccTemplate) throws Exception {
		String timeStamp = getNextHour(60000);
		return prepareMCCRequest("scc-request.xml", mccTemplate, "replace", timeStamp, true);	
	}
	private String prepareMCCRequest(String sccTemplate, String mccTemplate, String action, String timeStamp, boolean binary) throws Exception {
		String scc = sendSCCRequest(sccTemplate, timeStamp);
		
		System.out.println(" scc response:\n" + scc);
		
		int actionStart = scc.indexOf("action=\"" + action);
		Assert.assertTrue("SCC " + action + " action not found in SCC response.", actionStart >=0);
		
		final String SCTE35_POINTDESCRIPTOR_START = "<sig:SCTE35PointDescriptor";
		final String SCTE35_POINTDESCRIPTOR_END = "</sig:SCTE35PointDescriptor>";
		final String SCTE35_BINARY_DATA_START = "<sig:BinaryData";
		final String SCTE35_BINARY_DATA_END = "</sig:BinaryData>";
		
		String startTag = binary ? SCTE35_BINARY_DATA_START : SCTE35_POINTDESCRIPTOR_START;
		String endTag = binary ? SCTE35_BINARY_DATA_END: SCTE35_POINTDESCRIPTOR_END;

		int start = scc.indexOf(startTag, actionStart);
		int end = scc.indexOf(endTag, actionStart);

		Assert.assertTrue("SCC point descriptor tag " + startTag + " not found in SCC response.", start >=0);
		
		String scte35PtDesc = scc.substring(start, end + endTag.length());
		
		InputStream mccStream = this.getClass().getResourceAsStream(mccTemplate);
		String mcc = IOUtils.toString(mccStream);

		String timeString = timeStamp;		
		
		mcc = mcc.replaceAll("CURRENT_TIME", timeString);
		mcc = mcc.replaceAll("<sig:BinaryData signalType=\"SCTE35\">BINDAR_DATA</sig:BinaryData>", scte35PtDesc);
		mcc = mcc.replaceAll("ACQUISITION_POINT", ACQUISITION_POINT);
		
		return mcc; 
	}
	
	private void assertContains(String source, String expect) {
		boolean found = source.indexOf(expect) >= 0;
		Assert.assertTrue("Response does not contain expected string: " + expect, found);
	}
	
	private void assertNotContains(String source, String expect) {
		boolean found = source.indexOf(expect) >= 0;
		Assert.assertTrue("Response should  not contain string: " + expect, !found);
	}

	@Test	
	public void testSCCDeleteRequest() throws Exception {
		final int BREAK_COUNT = 10;
		final int INTERVAL = 5*60*1000;		// request intervals in milliseconds
		
		String ret = "";
		boolean deleted = false;
		for (int i = 0; i < BREAK_COUNT; i++) {
			String timeStamp = getTimeStamp(i, INTERVAL, 0);
			ret = sendSCCRequest("scc-request.xml", timeStamp, ACQUISITION_POINT_2);
			if (ret.indexOf("action=\"delete\"") > 0) {
				System.out.print("result: " + ret);
				deleted = true;
				break;
			}
		}
		
		Assert.assertEquals("signal not deleted.", true, deleted);
	}	


	@Test
	public void testSCCRequest() throws Exception {
		String timeStamp = getCurrentRequestTime();
		String ret = sendSCCRequest("scc-request.xml", timeStamp);
		System.out.println("result: " + ret);
		assertContains(ret,  "action=\"replace\"");
		assertContains(ret,  "action=\"create\"");
	}

	@Test
	public void testSCCRequestMultiCue() throws Exception {
		testSCCRequest();

		String nextTimeStamp = getCurrentRequestTime(350);		// current request time plus 100 milliseconds 
		String ret = sendSCCRequest("scc-request.xml", nextTimeStamp);
		System.out.print("multi-cue result: " + ret);
		assertContains(ret,  "action=\"delete\"");
		

	}

	@Test	
	public void testSCCSpliceInsertRequest() throws Exception {

		
		String ret = sendSCCRequest("scc-splice-insert-request.xml");
		System.out.print("result: " + ret);
		assertContains(ret,  "action=\"replace\"");
		assertContains(ret,  "action=\"create\"");
		assertContains(ret, "<sig:StreamTimes>");
		assertContains(ret, "<sig:SCTE35PointDescriptor spliceCommandType=\"6\">");
	}	

	@Test	
	/** test scc splice insert with multi-cue requests 
	 * the signal should be returned as deleted.
	 * @throws Exception
	 */
	public void testSCCSpliceInsertRequestMultiCue() throws Exception {

		testSCCSpliceInsertRequest();
		
		String ret = sendSCCRequest("scc-splice-insert-request.xml", 100);
		System.out.print("multi-cue result: " + ret);
		assertContains(ret,  "action=\"delete\"");
	}	
	
	@Test	
	public void testSCCTimeSignalPOStartRequest() throws Exception {

		
		String ret = sendSCCRequest("scc-time-signal-request.xml");
		System.out.println(ret);
		if (confirmTimeSignal) {
			assertContains(ret,  "action=\"replace\"");
			assertContains(ret,  "action=\"create\"");
			assertContains(ret, "<sig:StreamTimes>");
			assertContains(ret, "<sig:SCTE35PointDescriptor spliceCommandType=\"6\">");
		} else {
			System.out.print("scc response: " + ret);
			assertContains(ret,  "action=\"noop\"");
		}
	
	}	

	@Test	
	public void testSCCTimeSignalPOEndRequest() throws Exception {

		if (confirmTimeSignal) {
			String ret = sendSCCRequest("scc-time-signal-request-po-end.xml");
			System.out.print("result: \n" + ret);
			assertContains(ret,  "action=\"delete\"");
		} else {
			String ret = sendSCCRequest("scc-time-signal-request-po-end.xml");
			System.out.print("result: \n" + ret);
			assertContains(ret,  "action=\"noop\"");
		}
		
	}	


	@Test	
	/** test scc time signal with multi-cue requests 
	 * the signal should be returned as deleted.
	 * @throws Exception
	 */
	public void testSCCTimeSignalRequestMultiCue() throws Exception {

		testSCCTimeSignalPOStartRequest();		
		
		String ret = sendSCCRequest("scc-time-signal-request.xml", 100);
		System.out.print("result: " + ret);
		
		if (confirmTimeSignal) {
			assertContains(ret,  "action=\"delete\"");
		} else {
			assertContains(ret,  "action=\"noop\"");
		}
	}	


	
	@Test	
	public void testSCCOtherCommandRequest() throws Exception {

		
		String ret = sendSCCRequest("scc-other-command.xml");
		System.out.print("result: " + ret);
		assertNoop(ret);
	}	

	@Test	
	public void testSCCContentIdentificationRequest() throws Exception {
		
		String ret = sendSCCRequest("scc-content-ident-event.xml");
		System.out.print("result: " + ret);
		assertNoop(ret);
	}	
	
	private void assertNoop(String resp) {
		assertContains(resp, "action=\"noop\"");
	}
	
	@Test	
	public void testSCCAbortEventRequest() throws Exception {
		
		String ret = sendSCCRequest("scc-abort-event.xml");		
		System.out.print("result: " + ret);
		assertNoop(ret);
	}	


	@Test	
	public void testSCCBlackoutEventRequest() throws Exception {
		String timeStamp = getNextHour(110);
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"replace\"");
		assertContains(ret, "action=\"create\"");
	}	

	@Test	
	public void testSCCBlackoutEventOnBufferTime() throws Exception {
		// test the signal on the exact buffer time
		String timeStamp = getNextHour(1800000);
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"replace\"");
		assertContains(ret, "action=\"create\"");
	}	

	@Test	
	public void testSCCBlackoutEventOutsideBuffer() throws Exception {
		// send out a signal outside the program start buffer, assume 10 minutes is sufficient. 
		String timeStamp = getNextHour(10*60*1000);
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"delete\"");
	}	
	
	@Test	
	public void testSCCBlackoutEventOnTime() throws Exception {
		// test the signal on the 30:00 of hour that is event start time
		String timeStamp = getNextHour(1800000);
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"replace\"");
		assertContains(ret, "action=\"create\"");
	}	


	@Test	
	public void testSCCBlackoutEventRequestMultiCue() throws Exception {
		testSCCBlackoutEventOnTime();
		
		String timeStamp = getNextHour(1800000 + 222);		// test on the 30:00 + 220ms 
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"delete\"");
	}	

	@Test	
	public void testSCCBlackoutBinaryEventRequest() throws Exception {
		String timeStamp = getNextHour(110);
		String ret = sendSCCRequest("scc-blackout-binary-event.xml", timeStamp);
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"replace\"");
		assertContains(ret, "action=\"create\"");
	}	
	
	@Test	
	public void testSCCBlackoutEventInvalidAquisitionPoint() throws Exception {
		// test the signal on the exact buffer time
		String timeStamp = getNextHour(1800000);
		String ret = sendSCCRequest("scc-blackout-event.xml", timeStamp, "FOO");
		System.out.print("result: " + ret);
		assertContains(ret, "action=\"noop\"");
	}
	
	@Test	
	public void testSCCSpliceCancelEventRequest() throws Exception {

		
		String ret = sendSCCRequest("scc-splice-cancel-event.xml");
		System.out.print("result: " + ret);
		assertNoop(ret);
	}	

	@Test	
	public void testMccHlsRequest() throws Exception {

		String mcc = prepareMCCRequest("mcc-request-hls.xml");
		System.out.println("mcc hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":FirstSegment>");
		assertContains(ret,  ":SpanSegment>");
		assertContains(ret,  ":LastSegment>");
	}	

	private String prepareBlackoutMCCRequest(String mccTemplate, String action) throws Exception {
		String timeStamp = getNextHour(1800000);
		String mcc = prepareMCCRequest("scc-blackout-event.xml", mccTemplate, action, timeStamp, false);
		return mcc;
	}
	
	@Test	
	public void testMccBlackoutProgramStartHlsRequest() throws Exception {
		String mcc = prepareBlackoutMCCRequest("mcc-request-hls.xml", "replace");
		System.out.println("mcc blackout hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":FirstSegment>");
		assertContains(ret,  ":SpanSegment>");
		assertContains(ret,  ":LastSegment>");
	}	
	
	@Test	
	public void testMccBlackoutContentIdentificationHlsRequest() throws Exception {

		String mcc = prepareBlackoutMCCRequest("mcc-request-hls.xml", "create");
		System.out.println("mcc blackout hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":FirstSegment>");
		assertNotContains(ret,  ":SpanSegment>");
		assertNotContains(ret,  ":LastSegment>");
	}	

	@Test	
	public void testMccHssRequest() throws Exception {

		String mcc = prepareMCCRequest("mcc-request-hss.xml");
		System.out.println("mcc hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":SparseTrack");
		assertContains(ret,  "trackName=");
	}	

	@Test	
	public void testMccBlackoutProgramStartHssRequest() throws Exception {

		String mcc = prepareBlackoutMCCRequest("mcc-request-hss.xml", "replace");
		System.out.println("mcc hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":SparseTrack");
		assertContains(ret,  "trackName=");
	}	

	@Test	
	public void testMccBlackoutContentIdentificationHssRequest() throws Exception {

		String mcc = prepareBlackoutMCCRequest("mcc-request-hss.xml", "create");
		System.out.println("mcc hls request:\n" + mcc);
		String ret = post(POIS_LINEAR_MCC_URL, mcc);
		System.out.print("result: " + ret);
		assertContains(ret,  ":SparseTrack");
		assertContains(ret,  "trackName=");
	}	

	// @Test
	public void testPrepareAndLoadPOISData() throws Exception {
		execute ("cp /opt/blackarrow/rulescore/linear/1/pois_repos/*.zip /opt/blackarrow/ess/pois_repos ");
		String ret = post(POIS_LINEAR_LOADER_URL, "load");
		if (!"OK\n".equals(ret)) 
			throw new Exception("POIS loading failed: " + ret);
		System.out.print("result: " + ret);
	}

	@Test
	public void testPOISLoader() throws Exception {
		String ret = post(POIS_LINEAR_LOADER_URL, "load");
		if (!"OK\n".equals(ret)) 
			throw new Exception("POIS loading failed: " + ret);
		System.out.print("result: " + ret);
	}

	@Test
	public void testAuditRollver() throws Exception {
		String ret = post(POIS_LINEAR_LOADER_URL, "rollover");
		if (!"OK\n".equals(ret)) 
			throw new Exception("POIS loading failed");
		System.out.print("result: " + ret);

	}
	
	
	private String extractSignalId(String notif) {
		final String SIGNAL_ID = "signalPointID=";
		
		int start = notif.indexOf(SIGNAL_ID);
		Assert.assertTrue("Signal Point ID not found", start > 0);
		start += SIGNAL_ID.length() + 1;
		
		int end = notif.indexOf('"', start);
		String signalId = notif.substring(start, end);
		return signalId;		
	}

	@Test
	public void testPlacementResponse() throws Exception {		
		final String TARGET_AD_ZONE = "093";
		
		String adsNotifURL = CppConfigurationBean.getInstance().getLinearPoisToAdsNotificationUrl();
		Assert.assertTrue("ADS Nodification URL not configured", !adsNotifURL.isEmpty());
		
		String adsURL = adsNotifURL.replace("6701/ponotification", "6702/ads");
			
		
		String sccresp = sendSCCRequest("scc-request.xml");
		System.out.print("scc notification:\n " + sccresp);
		
		
		HashMap<String, String> params = new HashMap<String, String>();
		
		String adZone = TARGET_AD_ZONE;
		String signalId = extractSignalId(sccresp);
		String signalIdEncoded = URLEncoder.encode(signalId, "UTF-8");
		
		params.put("AdZone", adZone);
		params.put("SignalId", signalIdEncoded);
		System.out.println("ADS Placement Request: " + adsURL + ", Ad Zone: " + adZone + ", Signal Id: " + signalId);
		String ret = post(adsURL, params);
		System.out.print("ads response: \n" + ret);
	}
		
}
