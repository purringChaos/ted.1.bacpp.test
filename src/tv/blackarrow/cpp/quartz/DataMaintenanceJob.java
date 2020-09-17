package tv.blackarrow.cpp.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.po.maintenance.DataMaintenanceProcessor;

public class DataMaintenanceJob implements Job {

	private static final Logger LOGGER = LogManager.getLogger(DataMaintenanceJob.class);

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
		if(CppConfigurationBean.getInstance().isServiceEnabled()) { 
			LOGGER.info(()->"Begin data maintenance.");
			DataMaintenanceProcessor.maintainData();
			LOGGER.info(()->"Finished data maintenance.");
		}
    }
}
