/**
 * 
 */
package tv.blackarrow.cpp.components.filter;

import java.util.List;
import java.util.stream.Collectors;

import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.CppConfigurationBean;

/**
 * @author asharma
 *
 */
public class RestrictionFilter {
	
	private RestrictionFilter() {
		super();
	}

	public static void cleanupRestrictions(List<BlackoutEvent> events) {
		
		final String restrictionType = CppConfigurationBean.getInstance().getQamZoneIdentityType();
		if(events!=null && restrictionType != null) {
			events.forEach(event -> {
				if(event.getRestrictions() != null) {
					event.setRestrictions(event.getRestrictions()
							.parallelStream()
							.filter(restriction -> restriction.getRestrictionType().equalsIgnoreCase(restrictionType))
							.collect(Collectors.toList()));
				}
			});
		}
	}
}