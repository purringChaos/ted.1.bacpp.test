package tv.blackarrow.cpp.setting;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public class FeedNetworkMappingBean {

	private static ConfigurableApplicationContext ctx = null;
    private static FeedNetworkMappingBean instance = null;

	private Map<String, String> feedNetworkMap = null;

	private FeedNetworkMappingBean() {
	}
	
	public static void load() {
		if(ctx == null) {
			ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/feed_network_mapping.xml");
		} else {
			ctx.refresh();
		}
	    instance = (FeedNetworkMappingBean) ctx.getBean("feedMappingBean");
	}

    public static FeedNetworkMappingBean getInstance() {
        return (FeedNetworkMappingBean)instance;
    }
	
	public Map<String, String> getFeedNetworkMap() {
		return feedNetworkMap;
	}

	public void setFeedNetworkMap(Map<String, String> feedNetworkMap) {
		this.feedNetworkMap = feedNetworkMap;
	}
	
}
