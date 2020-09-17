/**
 * 
 */
package tv.blackarrow.cpp.components;

/**
 * definitions for Scte 35 constants 
 * @author pzhang
 *
 */
public interface Scte35Contants {
	
	// segmentation type id 
	static final int SEGMENTATION_TYPE_PROGRAM_START = 0x10;
	static final int SEGMENTATION_TYPE_PROGRAM_END = 0x11;
	static final int SEGMENTATION_TYPE_CONTENT_IDENT = 0x1;
	static final int SEGMENTATION_TYPE_PROGRAM_EARLY_TERMINATION = 0x12;
	static final int SEGMENTATION_TYPE_PROGRAM_RUNOVER_PLANNED = 0x15;
	static final int SEGMENTATION_TYPE_PROGRAM_RUNOVER_UNPLANNED = 0x16;
	static final int SEGMENTATION_TYPE_PROGRAM_BLACKOUT_OVERRIDE = 0x18;

	static final int SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_START= 0x34;
	static final int SEGMENTATION_TYPE_PLACEMENT_OPPORTUNITY_END= 0x35;
	static final int SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_START= 0x30;
	static final int SEGMENTATION_TYPE_PROVIDER_ADVERTISEMENT_END= 0x31;
	static final int SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_START= 0x32;
	static final int SEGMENTATION_TYPE_DISTRIBUTOR_ADVERTISEMENT_END= 0x33;
	
	
}
