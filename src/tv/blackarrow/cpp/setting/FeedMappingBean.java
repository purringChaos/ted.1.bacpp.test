//
// Copyright © 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.setting;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;

/**
 * FeedMappingBean
 * 
 * saved Acquisition point to feed ID mapping
 * 
 */
public class FeedMappingBean {

	private static ConfigurableApplicationContext ctx = null;
    private static FeedMappingBean instance = null;

    private Map<String, String> acquisitionFeedMap;

    private FeedMappingBean() {
    };
 
	public static void load() {
	    if(ctx == null) {
	    	ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/acquisition_feed_mapping.xml");
	    } else {
	    	ctx.refresh();
	    }
	    instance = (FeedMappingBean) ctx.getBean("feedMappingBean");
	}

    public static FeedMappingBean getInstance() {
        return (FeedMappingBean)instance;
    }
    
    
    public Map<String, String> getAcquisitionFeedMap() {
        return acquisitionFeedMap;
    }

    public void setAcquisitionFeedMap(Map<String, String> acquitionFeedMap) {
        this.acquisitionFeedMap = acquitionFeedMap;
    }
    
}
