package tv.blackarrow.cpp.setting;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public class FeedProviderMappingBean {

	private static ConfigurableApplicationContext ctx = null;
    private static FeedProviderMappingBean instance = null;

	private Map<String, String> feedProviderMap;

	private FeedProviderMappingBean() {
	}
	
	public static void load() {
		if(ctx == null) {
			ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/config.xml");
		} else {
			ctx.refresh();
		}
	    instance = (FeedProviderMappingBean) ctx.getBean("feedMappingBean");
	}

    public static FeedProviderMappingBean getInstance() {
        return instance;
    }

	public Map<String, String> getFeedProviderMap() {
		return feedProviderMap;
	}

	public void setFeedProviderMap(Map<String, String> feedProviderMap) {
		this.feedProviderMap = feedProviderMap;
	}
	
}
