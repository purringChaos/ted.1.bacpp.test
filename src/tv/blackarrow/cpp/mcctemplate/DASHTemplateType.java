/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate;


/**
 * @author Amit Kumar Sharma
 *
 */
public enum DASHTemplateType {

    PROGRAM_START("ProgramStart"), 
    PROGRAM_END("ProgramEnd"), 
    PROGRAM_EXT("ProgramExt"),
    PROGRAM_RUNOVER_UNPLANNED("ProgramRunoverUnplanned"),
    PROGRAM_BLACKOUT_OVERRIDE("BlackoutOverride"),
    NO_BLACKOUT_PROGRAM_START("NoBlackoutProgramStart"), 
    NO_BLACKOUT_PROGRAM_END("NoBlackoutProgramEnd"), 
    CONTENT_IDENTIFICATION("ContentIdentification"),
	PLACEMENT_OPPORTUNITY_START("PlacementOpportunityStart"), 
	PLACEMENT_OPPORTUNITY_END("PlacementOpportunityEnd"),
	SCHEDULELESS_INBAND_PLACEMENT_OPPORTUNITY_START("SchedulelessInbandPlacementOpportunityStart"),
    DEFAULT("Default");

    private final String value;

    DASHTemplateType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DASHTemplateType fromValue(String v) {
        for (DASHTemplateType c: DASHTemplateType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
