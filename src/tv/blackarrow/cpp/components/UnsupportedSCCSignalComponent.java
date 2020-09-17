
//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
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
package tv.blackarrow.cpp.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.utils.CppConstants;

/**
 * 
 */
public class UnsupportedSCCSignalComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(UnsupportedSCCSignalComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) {
		//Always return no_op in this case
		context.getMessage().setOutboundProperty(CppConstants.INTERNAL_FLAG_UNSUPPORTED_SCC_SIGNAL, "true");
		return "";
	}

}
