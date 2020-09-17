package tv.blackarrow.cpp.normalize;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Scte35NormalizationHandlerFactory caches Scte35NormalizationHandler instance, and decide 
 * which Scte35NormalizationHandler should be used
 * 
 * @author jwang
 *
 */	
public final class Scte35NormalizationHandlerFactory {
		private Scte35NormalizationHandlerFactory() {};
	
		/**
		 * maintain a hash map
		 */
	   public static Map<NormalizationVersion, Scte35NormalizationHandler> handlerMap = 
			   Collections.synchronizedMap(new HashMap<NormalizationVersion, Scte35NormalizationHandler>());
	    
	    public static Scte35NormalizationHandler getNormalizationHandler(NormalizationVersion version) {
	    	if(handlerMap.size() == 0) {
	    		handlerMap.put(NormalizationVersion.V1, Scte35NormalizationHandlerV1.getInstance());
	    		handlerMap.put(NormalizationVersion.V2, Scte35NormalizationHandlerV2.getInstance());
	    	}
	    	
	    	return handlerMap.get(version);
	 }
}
