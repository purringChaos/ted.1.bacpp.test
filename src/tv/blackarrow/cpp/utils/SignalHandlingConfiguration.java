/**
 * 
 */
package tv.blackarrow.cpp.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author Amit Kumar Sharma
 *
 * Enumeration for the possible action types that can be configured in the hosted app for an Acquisition point, to instruct runtime ESAM Service, 
 * to respond with corresponding action/processing whenever a splice insert/time signal type message is received. 
 */
public enum SignalHandlingConfiguration {
	DELETE("Delete"), NOOP("Noop"), PROCESS("Process"),CONVERT_TO_DISTRIBUTOR_AD("Convert to Distributor Ad"), PRESERVE("Preserve"), 
	PROCESS_IN_BAND("Process In-band");
	
	private final String configurationType;
	private static final Map<String, SignalHandlingConfiguration> lookup = getLookupMap();
	
	private SignalHandlingConfiguration(String configurationType){
		this.configurationType = configurationType;
	}

	private static Map<String, SignalHandlingConfiguration> getLookupMap() {
		Map<String, SignalHandlingConfiguration> lookup = new HashMap<String, SignalHandlingConfiguration>();
		lookup.put(DELETE.getConfigurationType().toUpperCase(), DELETE);
		lookup.put(NOOP.getConfigurationType().toUpperCase(), NOOP);
		lookup.put(PROCESS.getConfigurationType().toUpperCase(), PROCESS);
		lookup.put(CONVERT_TO_DISTRIBUTOR_AD.getConfigurationType().toUpperCase(), CONVERT_TO_DISTRIBUTOR_AD);
		lookup.put(PRESERVE.getConfigurationType().toUpperCase(), PRESERVE);
		lookup.put(PROCESS_IN_BAND.getConfigurationType().toUpperCase(), PROCESS_IN_BAND);
		return Collections.unmodifiableMap(lookup);
	}

	public String getConfigurationType() {
		return configurationType;
	}
	
	public static SignalHandlingConfiguration getConfigurationValue(String configuration){
		if(StringUtils.isBlank(configuration)){
			return SignalHandlingConfiguration.PROCESS;
		}
		return lookup.get(configuration.toUpperCase());
	}
}
