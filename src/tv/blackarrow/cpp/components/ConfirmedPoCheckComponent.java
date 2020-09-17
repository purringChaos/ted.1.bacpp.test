
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
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.utils.CppConstants;

/**
 * 
 * Search confirmed POs. If available, return the confirmed placement opportunity
 * 
 * @author mchakkarapani
 */
public class ConfirmedPoCheckComponent implements Callable {
	private static final Logger LOGGER = LogManager.getLogger(ConfirmedPoCheckComponent.class);

	@Override
	public Object onCall(final MuleEventContext context) throws Exception {
		final String acqPointId = context.getMessage().getProperty(CppConstants.ACQUISITION_POINT_IDENTITY, PropertyScope.OUTBOUND);		
		final Long eventTime = context.getMessage().getProperty(CppConstants.EVENT_TIME, PropertyScope.OUTBOUND);
		LOGGER.debug("Request properties. Acquision Point Identity=" + acqPointId + ", eventTime:" + eventTime);
		POProcessingComponent poComponent = new POProcessingComponent(DataManagerFactory.getInstance().getAcquisitionPoint(acqPointId), eventTime);
		ConfirmedPlacementOpportunity  cpo = poComponent.getConfirmedPlacementOpportunity();
		context.getMessage().setProperty(CppConstants.CONFIRMED_PLACEMENT_OPPORTUNITY, cpo, PropertyScope.OUTBOUND);	
		return cpo;
	}

}
