package tv.blackarrow.cpp.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author snagpal
 */
public enum SpliceDescriptorType {
	
	//These values are taken from SCTE 35 2013
	AVAIL_DESCRIPTOR(0x00),DTMF_DESCRIPTOR(0x01),SEGMENTATION_DESCRIPTOR(0x02);
	
	private static final Logger LOGGER = LogManager.getLogger(SpliceDescriptorType.class);
	private static final Map<Long,SpliceDescriptorType> lookup = new HashMap<Long, SpliceDescriptorType>();

	static {
	      for(SpliceDescriptorType spliceDescriptorType : SpliceDescriptorType.values()){
	           lookup.put(spliceDescriptorType.getDescriptorType(), spliceDescriptorType);
	      }
	 }
	
	private long descriptorType;
	
	public long getDescriptorType() {
		return descriptorType;
}

	private SpliceDescriptorType(final long descriptorType){
		this.descriptorType = descriptorType;
	}
	
	public static SpliceDescriptorType valueOf(long descriptorType){
		SpliceDescriptorType spliceDescriptorType = lookup.get(descriptorType);
		if(spliceDescriptorType==null){
			//throw new IllegalArgumentException("This command type is not supported.");
			LOGGER.info("Unsupported splice descriptor  type: " + descriptorType);
		}
		return spliceDescriptorType;
	}
}
