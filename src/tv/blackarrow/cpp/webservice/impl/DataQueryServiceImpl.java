
//
// Copyright (c) 2013 BlackArrow, Inc. All rights reserved.
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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;

import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.BlackoutEvent;


@Path("/")
public class DataQueryServiceImpl {

	@GET
	@Path("/blackoutevent/feed/{feed}")
	public Response getBlackoutEvent(@PathParam(value="feed")final String feed) {
		
		Gson gson = new Gson();
		StringBuffer result = new StringBuffer();
		List<BlackoutEvent> eventList = DataManagerFactory.getInstance().getAllBlackoutEventsOnFeed(feed);
		for(BlackoutEvent event : eventList) {
			result.append(gson.toJson(event)).append("\n");
		}
		
		final Response response = Response.status(Status.OK).
				entity(result.toString()).build();
		
		return response;
	}
	
	
}
