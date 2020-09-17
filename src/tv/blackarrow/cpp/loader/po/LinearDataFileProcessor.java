package tv.blackarrow.cpp.loader.po;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.loader.DataFileProcessor;
import tv.blackarrow.cpp.loader.DataFileProcessorFactory;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.quartz.ScheduleLoadingJob;


/**
 * process linear po, and blackout data files
 * @author pzhang
 * 
 */
public class LinearDataFileProcessor {
	
	private final static Logger LOGGER = LogManager.getLogger(LinearDataFileProcessor.class);
	
	private static volatile boolean wasNodeInActiveDataCenter = DataManagerFactory.getInstance().isServerInActiveDataCenter();

    public static synchronized void process() {
    	
    	DataFileProcessor serverConfigDataProc = DataFileProcessorFactory.getInstance().getDataFileProcessor(DataFileProcessorFactory.SERVER_CONFIG_FILE_PROCESSOR);
    	serverConfigDataProc.process();    	
    	    	
    	DataFileProcessor poDataProc = DataFileProcessorFactory.getInstance().getDataFileProcessor(DataFileProcessorFactory.PO_DATA_FILE_PROCESSOR);
    	poDataProc.process();
    	
    	DataFileProcessor blackoutDataProc = DataFileProcessorFactory.getInstance().getDataFileProcessor(DataFileProcessorFactory.BLACKOUT_DATA_FILE_PROCESSOR);    	
    	blackoutDataProc.process();
    	
    	boolean isNodeInActiveDatacenter = DataManagerFactory.getInstance().isServerInActiveDataCenter();
    	
    	//If this data center was a standby data center and has now become an active data center.
    	if(!wasNodeInActiveDataCenter && isNodeInActiveDatacenter){
    		LOGGER.info(()->"This server belongs to an active data center now, So going to laod all the rules if present and are not loaded by some other nodes in this data center.");
    		//Force load the files (and that should reschedule all the blackout out of band notifications too.)
            try {
                new ScheduleLoadingJob().execute(null);
            } catch (JobExecutionException e) {
            	LOGGER.error(()->"Error executing ScheduleLoadingJob");
            }
    	}
    	wasNodeInActiveDataCenter = isNodeInActiveDatacenter;
    }

}
