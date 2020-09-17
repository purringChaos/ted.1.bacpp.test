package tv.blackarrow.cpp.setting;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class HostSettingBean {
	private final Properties prop = new Properties();
	
	private static final String CONFIG_FILE = "/opt/blackarrow/converged/portal/conf/setting.properties";

	private static final HostSettingBean instance =  new HostSettingBean();
	
	private HostSettingBean() {
   			try {
				prop.load(new FileInputStream(CONFIG_FILE));
			} catch (FileNotFoundException e) {
				System.out.println("can not load " + CONFIG_FILE);
			} catch (IOException e) {
				System.out.println("can not load " + CONFIG_FILE);
			}
	}
	
	public static HostSettingBean getInstance() {
		return instance;
	}
	
	public String getPoisHost() {
		return prop.getProperty("pois_host");
	}

	public String getTranscoderHost() {
		return prop.getProperty("transcoder_host");
	}

	public String getAdsHost() {
		return prop.getProperty("ads_host");
	}

}
