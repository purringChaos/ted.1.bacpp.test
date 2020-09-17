package tv.blackarrow.cpp.quartz;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.loader.po.LinearDataFileLoader;
import tv.blackarrow.cpp.model.CppConfigurationBean;


public class ScheduleLoadingJob implements Job {

	private static final Logger LOGGER = LogManager.getLogger(ScheduleLoadingJob.class);

    public static Lock scheduleLoadingLock = new ReentrantLock(); 

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		if(!CppConfigurationBean.getInstance().isServiceEnabled()) { return; } 

		try {
			LOGGER.info(()->"start - loading schedule job");
            // note: this lock only ensures that these processes do not run at the same
            // time on an individual node.  the loader logic itself deals with ensuring that
            // loaders running on two different nodes do not interfere with each other.
			scheduleLoadingLock.lock();
			LOGGER.info(()->"Begin - loading schedule");
			LinearDataFileLoader loader = new LinearDataFileLoader();
			loader.load();
		} catch (Exception ex) {
			LOGGER.error(()->"Exception occured in execute(JobExecutionContext ctx)\n" + ex.getMessage());
		} finally {
			scheduleLoadingLock.unlock();
			LOGGER.info(()->"Finished - loading schedule");
		}
	}
}
