/**
 * 
 */
package tv.blackarrow.cpp.mcctemplate;


/**
 * @author Amit Kumar Sharma
 *
 */
public enum HLSTemplateType {
    PLACEMENT_OPPORTUNITY("PlacementOpportunity"),
	SCHEDULELESS_INBAND_PLACEMENT_OPPORTUNITY("SchedulelessInbandPlacementOpportunity"),
    PROGRAM_START("ProgramStart"), 
    PROGRAM_RUNOVER_UNPLANNED("ProgramRunoverUnplanned"),
    PROGRAM_BLACKOUT_OVERRIDE("BlackoutOverride"),
    PROGRAM_END("ProgramEnd"), 
    CONTENT_IDENTIFICATION("ContentIdentification"),
    NO_BLACKOUT_PROGRAM_START("NoBlackoutProgramStart"), 
    NO_BLACKOUT_PROGRAM_END("NoBlackoutProgramEnd");
    
    private final String value;

    HLSTemplateType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HLSTemplateType fromValue(String v) {
        for (HLSTemplateType c: HLSTemplateType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
