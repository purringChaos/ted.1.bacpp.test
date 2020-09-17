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

package test.tv.blackarrow.cpp.model;

import junit.framework.TestCase;
import tv.blackarrow.cpp.model.ScheduleIngestBean;

public class ScheduleIngestBeanTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * test bean initialize from a setting
	 */
	public void testGetInstance() {
		try {
			ScheduleIngestBean.getInstance();
		} catch(Exception ex) {
			fail(ex.getMessage());
		}
	}
	
	public void testGetInboxDir() {
		String inboxDir =  ScheduleIngestBean.getInstance().getInboxDir();
		assertNotNull(inboxDir);
		assertFalse(inboxDir.isEmpty());
	}

	public void testGetBackupTo() {
		String backupDir =  ScheduleIngestBean.getInstance().getBackupTo();
		assertNotNull(backupDir);
		assertFalse(backupDir.isEmpty());
	}

	public void testGetCpaHost() {
		String cpaHost =  ScheduleIngestBean.getInstance().getCpaHostIngestUrl();
		assertNotNull(cpaHost);
		assertFalse(cpaHost.isEmpty());
	}

	public void testGetCppHost() {
		String cppHost =  ScheduleIngestBean.getInstance().getCppHostIngestUrl();
		assertNotNull(cppHost);
		assertFalse(cppHost.isEmpty());
	}
	
}
