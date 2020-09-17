package test.tv.blackarrow.cpp.components;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import junit.framework.Assert;

public class SCCRequestComponentTest extends DynamicPortTestCase {
	private String baseDir = "../";

	public SCCRequestComponentTest() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tck.FunctionalTestCase#getConfigResources()
	 */
	protected String getConfigResources() {
		if (new File("resources/test/bacpp-test-config.xml").exists()) {
			baseDir = "./";
			return "resources/test/bacpp-test-config.xml";
		} else
			return "../resources/test/bacpp-test-config.xml";
	}

	private String getTimeSignalMissingAcqPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccTimeSignalMissingAcqPointRequest.xml"));
	}

	private String getTimeSignalMissingSignalPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccTimeSignalMissingSignalPointRequest.xml"));
	}

	private String getTimeSignalMissingUTCPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccTimeSignalMissingUTCPointRequest.xml"));
	}

	private String getSpliceInsertMissingAcqPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccSpliceInsertMissingAcqPointRequest.xml"));
	}

	private String getSpliceInsertMissingSignalPointPayload() throws Exception {
		return FileUtils
				.readFileToString(new File(
						baseDir
								+ "etc/test_files/sccSpliceInsertMissingSignalPointRequest.xml"));
	}

	private String getSpliceInsertMissingUTCPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccSpliceInsertMissingUTCPointRequest.xml"));
	}

	public String getName() {
		return "Mule Server Test";
	}

	private String getBinaryDataMissingAcqPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccBinaryDataMissingAcqPointRequest.xml"));
	}

	private String getBinaryDataMissingSignalPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccBinaryDataMissingSignalPointRequest.xml"));
	}

	private String getBinaryDataMissingUTCPointPayload() throws Exception {
		return FileUtils.readFileToString(new File(baseDir
				+ "etc/test_files/sccBinaryDataMissingUTCPointRequest.xml"));
	}

	@Test
	public void testTestCase() throws Exception {
		MuleClient client = new MuleClient(muleContext);
		String payload = "";
		Map<String, Object> properties = null;
		MuleMessage result = null;

		payload = getBinaryDataMissingAcqPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		Assert.assertNotNull(result.getPayloadAsString());
		// System.out.println(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") != -1);

		payload = getBinaryDataMissingSignalPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
		Assert.assertNotNull(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") != -1);

		payload = getBinaryDataMissingUTCPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
//		System.out.println(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

		payload = getTimeSignalMissingAcqPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

		payload = getTimeSignalMissingSignalPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
		//assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

		
		payload = getTimeSignalMissingUTCPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);
		//System.out.println(result.getPayloadAsString());

		payload = getSpliceInsertMissingAcqPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

		payload = getSpliceInsertMissingSignalPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
		//assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

		payload = getSpliceInsertMissingUTCPointPayload();
		result = client.send(((InboundEndpoint) client.getMuleContext()
				.getRegistry().lookupObject("sccInbound")).getAddress(),
				payload, properties);
		//System.out.println(result.getPayloadAsString());
//		assertTrue(result.getPayloadAsString().indexOf("classCode=\"1\"") !=-1);

	}

	@Override
	protected int getNumPortsToFind() {
		return 5;
	}
}
