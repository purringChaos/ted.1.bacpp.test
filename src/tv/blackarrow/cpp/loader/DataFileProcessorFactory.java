package tv.blackarrow.cpp.loader;

import tv.blackarrow.cpp.loader.bo.LinearBlackoutDataFileProcessor;
import tv.blackarrow.cpp.loader.po.LinearPODataFileProcessor;
import tv.blackarrow.cpp.loader.serverconfig.LinearServerConfigDataFileProcessor;

public class DataFileProcessorFactory {
	
	final static public int SERVER_CONFIG_FILE_PROCESSOR = -1;
	final static public int PO_DATA_FILE_PROCESSOR = 0;
	final static public int BLACKOUT_DATA_FILE_PROCESSOR = 1;

	final static private DataFileProcessorFactory factory = new DataFileProcessorFactory();
	
	private DataFileProcessorFactory() {
	}

	public static DataFileProcessorFactory getInstance() {
		return factory;
	}

	public DataFileProcessor getDataFileProcessor(int type) {
		switch(type) {
		case SERVER_CONFIG_FILE_PROCESSOR:
			return new LinearServerConfigDataFileProcessor();
		case PO_DATA_FILE_PROCESSOR: 
			return new LinearPODataFileProcessor();
		case BLACKOUT_DATA_FILE_PROCESSOR: 
			return new LinearBlackoutDataFileProcessor();
		default:
			throw new RuntimeException("Unsupported data file processor type " + type);			
		}		
	}

}
