package tv.blackarrow.cpp.transformers;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.InboundHeaders;
import org.mule.api.annotations.param.Payload;

import tv.blackarrow.cpp.model.FeedQueryRequest;

@ContainsTransformerMethods
public class FeedRequestTransformer {

	@Transformer
	public FeedQueryRequest stringToRequest(@Payload String s, 
			@InboundHeaders("feed_id") String feedId, 	@InboundHeaders("event_time") String eventTime) {
		
		System.out.println(feedId + " ============" + eventTime);
		
		return new FeedQueryRequest(feedId, eventTime);
	}
}
