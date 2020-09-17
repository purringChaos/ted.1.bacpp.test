/**
 * 
 */
package test.tv.blackarrow.cpp.setting;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import tv.blackarrow.cpp.setting.AcquisitionConfigBean;

/**
 * @author hcao
 *
 */
public class AcquisitionFeedConfigBeanTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.setting.AcquisitionConfigBean#load(java.lang.String)}.
	 */
	@Test
	public void testLoad() {
//		String path = new File("").getAbsolutePath() + "/resources/test/acquisition_feed_config.xml";
//		AcquisitionConfigBean.load(path);
//		AcquisitionConfigBean bean = AcquisitionConfigBean.getInstance();
//		assertNotNull(AcquisitionConfigBean.getInstance());
	}

	@Test
	public void testGetFeedToTranscoderUrlMap() {
//		String path = new File("").getAbsolutePath() + "/resources/test/acquisition_feed_config.xml";
		AcquisitionConfigBean.load();
		AcquisitionConfigBean instance = AcquisitionConfigBean.getInstance();
		assertNotNull(AcquisitionConfigBean.getInstance());
		Map<String, List<String>> map = instance.getFeedToTranscoderUrlMap();
		System.out.println("==");
	}

}
