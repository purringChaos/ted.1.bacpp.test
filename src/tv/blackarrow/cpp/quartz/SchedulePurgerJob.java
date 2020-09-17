package tv.blackarrow.cpp.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.purger.Purger;

public class SchedulePurgerJob implements Job {

    private static final Logger LOGGER = LogManager.getLogger(SchedulePurgerJob.class);

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
		if(!CppConfigurationBean.getInstance().isServiceEnabled()) { return; } 
    	
        // the purger only deletes old files now so it does not need to be 
        // synchronized with the file extractor or loader anymore. 
		LOGGER.info(()->"Begin purging.");
        Purger.purgeUnneededData();
        LOGGER.info(()->"Finished purging.");
    }
}
