package tv.blackarrow.cpp.pretrigger.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;

import tv.blackarrow.cpp.pretrigger.beans.PretriggerSettingBean;
import tv.blackarrow.cpp.pretrigger.jobs.PretriggerNotificationJob;
import tv.blackarrow.cpp.pretrigger.model.PretriggerEvent;
import tv.blackarrow.cpp.setting.FeedConfigBean;

/**
 * 
 * PretriggerSchedulingManager is used to manage Quartz triggers
 * 
 * function list:
 * 1. schedule pre-trigger notification jobs
 * 2. Manager scheduler
 * 3. Maintain trigger list
 * 
 */
public class PretriggerSchedulingManager {
	private static final Logger LOG = LogManager.getLogger(PretriggerSchedulingManager.class);
    private static PretriggerSchedulingManager INSTANCE;
    private static ApplicationContext ctx;
    public static final String TRIGGER_GROUP_PRETRIGGER = "pre-trigger-out-band";
    
    private Scheduler scheduler;
    private ConcurrentLinkedQueue<Trigger> triggerList = null;
    private Gson gson = new Gson();
    
    static {
        ctx = new ClassPathXmlApplicationContext("conf/schedule_manager.xml");
        INSTANCE = (PretriggerSchedulingManager) ctx.getBean("pretriggerSchedulingManager");
        if(INSTANCE.getScheduler() != null) {
        	try {
				INSTANCE.getScheduler().start();
				LOG.debug("start the scheduler");
			} catch (SchedulerException e) {
				LOG.error("Cannot start the scheduler");
			}
        }
    }

    private PretriggerSchedulingManager() {}

    public static PretriggerSchedulingManager getInstance() {
        return INSTANCE;
    }

    /**
     * init operation
     * 
     */
	public void init() {
		if(triggerList == null) {
			triggerList = new ConcurrentLinkedQueue<Trigger>();
		}
	}
	
	/**
	 * 
	 * @return a list of triggers
	 */
	public ConcurrentLinkedQueue<Trigger> getTriggerList() {
		return triggerList;
	}
	
	/**
	 * check if the schedule is running
	 * @return if the schedule is running
	 */
    public boolean isRunning() {
        boolean running = false;
        try {
            running = scheduler.isInStandbyMode() ^ true;  // fast;
        } catch (SchedulerException e) {
            LOG.warn("Check if scheduler is running", e);
        }

        return running;    
    }
    
    /**
     * shut down scheduler
     */
    public void closeManager() {
        LOG.debug("Shut down scheduler");
        try {
            // waiting for all jobs done and then shutdown
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOG.warn("Shut down scheduler failed", e);
        }
    }   
    
    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    private void cancelSchedule(String triggerName, String triggerGroup){
        if(triggerName == null){return;}
        try {
            Trigger triggerToBeRemoved = null;
            for(Trigger scheduledTrigger : getTriggerList()){
            	if(scheduledTrigger!=null && triggerName.equals(scheduledTrigger.getKey().getName())){
            		getScheduler().unscheduleJob(scheduledTrigger.getKey());
            		triggerToBeRemoved = scheduledTrigger;
            		break;
            	}
            }
            if(triggerToBeRemoved!=null){
            	getTriggerList().remove(triggerToBeRemoved);
            }
     	   if(LOG.isDebugEnabled()){
    		   LOG.debug("Successfully canceled the scheduing for "+ triggerName);
    	   }
        } catch (SchedulerException e) {
            LOG.error("Failed to cancel the scheduling "+ triggerName, e);
        }
    }
    
    public void unscheduleAllJobs(){
    	try {
    		getTriggerList().clear();
			List<String> jobGroupNames = scheduler.getJobGroupNames();
			if(jobGroupNames!=null){
				for(String jobGroupName: jobGroupNames){
					Set<JobKey> jobKeySet = scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroupName));
					if(jobKeySet!=null){
							LOG.debug("Unscheduling -> Job Group: " + jobGroupName + ", Job: " + jobKeySet);
							List<JobKey> list = new ArrayList<JobKey>(jobKeySet);
							scheduler.deleteJobs(list);
						}
				}
			}
		} catch (SchedulerException e) {
			LOG.error("Error happened while unscheduling a scheduled notification.", e);
		}
    }
    
    /**
     * schedule a bunch of pre-trigger out-of-band notification messages
     * 
     * If an old trigger is there, it will unsubscribe it, and then schedule a new one
     * else subscribe a new one   
     *  
     * @param event event for sending message
     * @param acq acquisition point for this event
     */
    public void scheduleOutOfBandNotification(final PretriggerEvent event) {
		FeedConfigBean feedConfigBean = FeedConfigBean.getInstance();
		if(event == null){
    		LOG.error("Event is null while scheduling the SCC message delivery.");
    		return;
    	}
		//PRI-14381 , advancetime per channel/feedid basis.
		long advanceTimeForFeed = (feedConfigBean.getPretriggerAdvanceInterval(event.getFeedId()) != -1)
				? feedConfigBean.getPretriggerAdvanceInterval(event.getFeedId()) : PretriggerSettingBean.getInstance().getTriggerAdvanceTime();

		Scheduler sched = PretriggerSchedulingManager.getInstance().getScheduler();
		try {
			long scheduleTime = event.getStartTime() - advanceTimeForFeed - event.getSignalOffset();
			
			String encoderNotificationTriggerName = gson.toJson(event);
			LOG.debug("Trying to cancel the exsiting scheduling for break "+ event.getBreakId() + 
					" on acquisition point " + event.getAcqusitionId());
			
			//find and unsubscribe the old schedule for event.
			cancelScheduleOutOfBandEncoderNotification(encoderNotificationTriggerName);
			
			if(scheduleTime < System.currentTimeMillis()) {  // if past the start time, don't send
				LOG.debug("event start time is " + new Date(scheduleTime)  + 
						" : stale. So skipping the rescheduling." + event.toString());
				return;
			}
			
			JobDetail job = JobBuilder.newJob(PretriggerNotificationJob.class).withIdentity(encoderNotificationTriggerName + "_job", TRIGGER_GROUP_PRETRIGGER).build();
			SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity(encoderNotificationTriggerName, TRIGGER_GROUP_PRETRIGGER)
                .startAt(new Date(scheduleTime)).forJob(job).build();
			getTriggerList().add(trigger);
			sched.scheduleJob(job, trigger);
			LOG.debug("schedule a new job for trigger " + trigger.getKey().getName() + ": expected trigger time is " + trigger.getStartTime());
			// define the job
			
			
		} catch (SchedulerException e) {
			LOG.error(e.getMessage(), e);
		} 
    }
    
    public void cancelScheduleOutOfBandEncoderNotification(String encoderNotificationTriggerName){
    	cancelSchedule(encoderNotificationTriggerName, TRIGGER_GROUP_PRETRIGGER);
    	/*
    	if(encoderNotificationTriggerName!=null){
	    	String [] eventIdAndAcquisitionPointId = encoderNotificationTriggerName.split(SEPARATOR);
	    	if(eventIdAndAcquisitionPointId != null && eventIdAndAcquisitionPointId.length ==2){
	    		// dataManager.removeOutOfBandEncoderNotificationStatus(eventIdAndAcquisitionPointId[0], eventIdAndAcquisitionPointId[1]);
	    	}
    	} */
    }
    

}
