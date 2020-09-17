package tv.blackarrow.cpp.setting;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public class AcquisitionDeleteEmptyBreakBean {

	private static ConfigurableApplicationContext ctx = null;
    private static AcquisitionDeleteEmptyBreakBean instance = null;

    private Map<String, Boolean> acquisitionDeleteEmptyBreakMap;

    private AcquisitionDeleteEmptyBreakBean() {
    };
 
	public static void load() {
	    if(ctx == null) {
	    	ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/acquisition_delete_empty_break_mapping.xml");
	    } else {
	    	ctx.refresh();
	    }
	    instance = (AcquisitionDeleteEmptyBreakBean) ctx.getBean("feedMappingBean");
	}

    public static AcquisitionDeleteEmptyBreakBean getInstance() {
        return (AcquisitionDeleteEmptyBreakBean)instance;
    }
    
    
    public Map<String, Boolean> getAcquisitionDeleteEmptyBreakMap() {
        return acquisitionDeleteEmptyBreakMap;
    }

    public void setAcquisitionDeleteEmptyBreakMap(Map<String, Boolean> acquisitionDeleteEmptyBreakMap) {
        this.acquisitionDeleteEmptyBreakMap = acquisitionDeleteEmptyBreakMap;
    }

}
