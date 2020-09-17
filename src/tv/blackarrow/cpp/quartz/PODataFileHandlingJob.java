package tv.blackarrow.cpp.quartz;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.loader.po.LinearDataFileProcessor;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.utils.CppConstants;

public class PODataFileHandlingJob implements Job {

	private static final Logger LOGGER = LogManager.getLogger(PODataFileHandlingJob.class);

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		if(!CppConfigurationBean.getInstance().isServiceEnabled()) { return; } 
		
		boolean locked = false;
		try {
		    // note: this lock only ensures that this process does not run at the same
		    // time as the schedule loader on an individual node.   since this file 
		    // handling job only deals with extracting the files to the local file 
		    // system, it is fine if this runs at the same time on two different nodes.
			locked = ScheduleLoadingJob.scheduleLoadingLock.tryLock(CppConstants.MAX_LOADING_TIME, TimeUnit.SECONDS);
			if(locked) {
				LOGGER.info(() -> "Linear POIS Begin - checking for new PO data files.");
				LinearDataFileProcessor.process();
			} else {
				LOGGER.error(() -> "Linear POIS Waited for more than " + CppConstants.MAX_LOADING_TIME + "seconds to process new file, " +
					"please check file loading job.");
			}
		} catch (Exception ex) {
			LOGGER.error("ERROR occured in execute(JobExecutionContext ctx)", ex);
			throw new JobExecutionException("ERROR occured in execute(JobExecutionContext ctx)", ex);
		} finally {
			if(locked) {
			    ScheduleLoadingJob.scheduleLoadingLock.unlock();
			}
			LOGGER.info(() -> "Linear POIS Finished - checking for new PO data files.");
		}

	}

}
