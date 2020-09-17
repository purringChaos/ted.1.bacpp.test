package tv.blackarrow.cpp.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.log.AuditLogger;
import tv.blackarrow.cpp.quartz.PODataFileHandlingJob;
import tv.blackarrow.cpp.quartz.ScheduleLoadingJob;
import tv.blackarrow.cpp.quartz.SchedulePurgerJob;

/**
 * handle administration requests
 */
public class AdminComponent implements Callable {
    private static final Logger LOGGER = LogManager.getLogger(AdminComponent.class);

    private static final String ROLLOVER = "rollover";
    private static final String LOAD = "load";
    private static final String OK = "OK";
    private static final String UNKNOWN_ADMIN_COMMAND = "Unknown admin command";
	
	@Override
	public Object onCall(final MuleEventContext context) throws Exception {
		final String command =  context.getMessageAsString();
		if (ROLLOVER.equals(command) || (command != null && command.indexOf(ROLLOVER) != -1)) {
		    return rollover();
		}
        if (LOAD.equals(command)) {
            return load();
        }
        LOGGER.error(()->UNKNOWN_ADMIN_COMMAND + " \"" + command + "\"");
		return UNKNOWN_ADMIN_COMMAND + " \"" + command + "\"";
	}
	
	private String rollover() {
        LOGGER.debug(()->"Received rollover command.  Rolling over audit log files");
        AuditLogger.rollover();
        return OK;
	}

    private String load() {
        LOGGER.debug(()->"Received load command.  Forcing immediate full loading cycle.  This should only be done in testing environments.");
        try {
            new PODataFileHandlingJob().execute(null);
        } catch (JobExecutionException e) {
            LOGGER.error(()->"Error executing PODataFileHandlingJob");
        }
        try {
            new ScheduleLoadingJob().execute(null);
        } catch (JobExecutionException e) {
            LOGGER.error(()->"Error executing ScheduleLoadingJob");
        }
        try {
            new SchedulePurgerJob().execute(null);
        } catch (JobExecutionException e) {
            LOGGER.error(()->"Error executing SchedulePurgerJob");
        }
        return OK;
    }

}
