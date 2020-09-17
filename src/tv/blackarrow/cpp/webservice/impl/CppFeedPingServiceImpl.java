
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

package tv.blackarrow.cpp.webservice.impl;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.transport.PropertyScope;

import tv.blackarrow.cpp.model.FeedQueryResponse;
import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.CppUtil;

/**
 * Converged Platform POIS feed acquisition Restful interface 
 */

@Path("/cpp")
public class CppFeedPingServiceImpl {
	private static final Logger LOG = LogManager.getLogger(CppFeedPingServiceImpl.class);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/feed/{acquisition_point}/{event_time}")
	public FeedQueryResponse feedPostRequest(@PathParam("acquisition_point")final String acquisitionPoint, 
			@PathParam("event_time")final String eventTime) {
		LOG.debug(()-> "Processing event from acquisition_point:" + acquisitionPoint + ", event_time:" + eventTime);
		
		final Map<String, String> mapping = AcquisitionConfigBean.getInstance().getAcquisitionFeedMap();
		String feedId = mapping.get(acquisitionPoint);
		if(feedId == null) {
			feedId = "NON_EXIST";
			if(acquisitionPoint != null) {
				feedId = acquisitionPoint;
			}
		}
		
		final MuleEventContext context = RequestContext.getEventContext();
		context.getMessage().setProperty(CppConstants.FEED_ID, feedId, PropertyScope.OUTBOUND);		
		context.getMessage().setProperty(CppConstants.EVENT_TIME, eventTime, PropertyScope.OUTBOUND);		
		context.getMessage().setProperty(CppConstants.FEED_ID_KEY, 
				CppUtil.getInstance().generateFeedIdKey(feedId, eventTime), PropertyScope.OUTBOUND);		
		
		return new FeedQueryResponse(false, "");
	}
	
}
