package tv.blackarrow.cpp.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import tv.blackarrow.cpp.model.FeedQueryResponse;

public class FailedStatusComponent implements Callable {
	private static final Logger LOG = LogManager.getLogger(FailedStatusComponent.class);

	@Override
	public Object onCall(MuleEventContext context) throws Exception {
		return new FeedQueryResponse(false, "");
	}

}
