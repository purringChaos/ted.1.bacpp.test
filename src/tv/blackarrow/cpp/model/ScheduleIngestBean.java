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
package tv.blackarrow.cpp.model;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ScheduleIngestBean {
	private String inboxDir;
	private String backupTo;
	private String workingDir;
	private String cppHostIngestUrl;
	private String cppHostApEventUrl;
	private String cpaHostIngestUrl;
	private String cpaHostNotificationUrl;
	private String transcoderUrl;
	
    private static final ApplicationContext CTX;
    private static ScheduleIngestBean instance = null;
    
    static {
        CTX = new ClassPathXmlApplicationContext("conf/cpp_bean.xml");
        instance = (ScheduleIngestBean) CTX.getBean("ingestBean");
    }
	
    public static ScheduleIngestBean getInstance() {
    	return instance;
    }
	
	public String getInboxDir() {
		return inboxDir;
	}

	public void setInboxDir(final String inboxDir) {
		this.inboxDir = inboxDir;
	}

	public String getBackupTo() {
		return backupTo;
	}

	public void setBackupTo(final String backupTo) {
		if(!backupTo.endsWith("/")) {
			this.backupTo = backupTo + "/";
		} else {
			this.backupTo = backupTo;
		}
	}
	

	public String getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(String workingDir) {
		if(!workingDir.endsWith("/")) {
			this.workingDir = workingDir + "/";
		} else {
			this.workingDir = workingDir;
		}
	}

	public String getCppHostIngestUrl() {
		if(!cppHostIngestUrl.endsWith("/")) {
			return cppHostIngestUrl + "/";
		} else {
			return cppHostIngestUrl;
		}
	}

	public void setCppHostIngestUrl(String cppHostIngestUrl) {
		this.cppHostIngestUrl = cppHostIngestUrl;
	}

	public String getCpaHostIngestUrl() {
		if(!cpaHostIngestUrl.endsWith("/")) {
			return cpaHostIngestUrl + "/";
		} else {
			return cpaHostIngestUrl;
		}
	}

	public void setCpaHostIngestUrl(String cpaHostIngestUrl) {
		this.cpaHostIngestUrl = cpaHostIngestUrl;
	}

	public String getCpaHostNotificationUrl() {
		if(!cpaHostNotificationUrl.endsWith("/")) {
			return cpaHostNotificationUrl + "/";
		} else {
			return cpaHostNotificationUrl;
		}
	}

	public void setCpaHostNotificationUrl(String cpaHostNotificationUrl) {
		this.cpaHostNotificationUrl = cpaHostNotificationUrl;
	}

	public String getCppHostApEventUrl() {
		if(!cppHostApEventUrl.endsWith("/")) {
			return cppHostApEventUrl + "/";
		} else {
			return cppHostApEventUrl;
		}
	}

	public void setCppHostApEventUrl(String cppHostApEventUrl) {
		this.cppHostApEventUrl = cppHostApEventUrl;
	}

	public String getTranscoderUrl() {
		return transcoderUrl;
	}

	public void setTranscoderUrl(String transcoderUrl) {
		this.transcoderUrl = transcoderUrl;
	}

}
