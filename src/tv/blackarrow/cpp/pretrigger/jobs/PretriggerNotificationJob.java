package tv.blackarrow.cpp.pretrigger.jobs;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

import com.google.gson.Gson;

import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.pretrigger.beans.PretriggerSettingBean;
import tv.blackarrow.cpp.pretrigger.handler.PretriggerHandler;
import tv.blackarrow.cpp.pretrigger.manager.PretriggerSchedulingManager;
import tv.blackarrow.cpp.pretrigger.model.PretriggerEvent;
import tv.blackarrow.cpp.pretrigger.service.PretriggerNotificationService;
import tv.blackarrow.cpp.service.NotificationStatus;
import tv.blackarrow.cpp.setting.FeedConfigBean;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signaling.EventScheduleType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.JavaxUtil;
import tv.blackarrow.cpp.utils.SCCResponseUtil;

/**
 * send notification to the MUX / DCMs. The MUX should insert the pre-trigger information
 * into the video stream
 *
 * @author jwang
 */
public class PretriggerNotificationJob implements Job {
	private static final int LOCKTIME = 20;  // in sec. The cap is 30 seconds
	private static final Logger LOG = LogManager.getLogger(PretriggerNotificationJob.class);
	private static final  ApplicationContext CTX = new ClassPathXmlApplicationContext("conf/service_beans.xml");
	private static final String EVENT_ID_LOCK_PREFIX = "evt_lock_";
	//	private static boolean debugEnabled = LOG.isDebugEnabled();
	private Gson gson = new Gson();

	@Override
	public void execute(final JobExecutionContext ctx) throws JobExecutionException {
		try {
			PretriggerSchedulingManager.getInstance().getTriggerList().remove(ctx.getTrigger());
			LOG.info("start job name: {} on {}", ctx.getJobDetail().getKey().getName(), ctx.getTrigger().getStartTime());

			String triggerName = ctx.getTrigger().getKey().getName();
			PretriggerEvent event = gson.fromJson(triggerName, PretriggerEvent.class);

			// only one node can send the pre-trigger message. All nodes have to compete to gain the lock of the message sending permission.
			// Once the lock is obtained, then check the sending status. If the status is SENT / START, then skip this.
			// Otherwise, send the message and update the status.
			// each node has random delay to offset the node clock difference, so each node has chance to get the lock (node time should be in sync)
			try {Thread.sleep((long)(Math.random()*300));} catch (InterruptedException e1) {}  
			final String lockName = EVENT_ID_LOCK_PREFIX + event.getBreakUuid() + "_" +event.getAcqusitionId();
			boolean locked = acquireLock(event, lockName);

			if(locked) { // got the lock, then process it
				LOG.info("get the lock {}", lockName); 
				final String status = DataManagerFactory.getInstance().getNotificationStatus(event.getBreakUuid(), event.getAcqusitionId(), null); // check the status again
				if (!NotificationStatus.SENT.toString().equals(status) && !NotificationStatus.STARTED.toString().equals(status)) {
					// set status to START
					DataManagerFactory.getInstance().putNotificationStatus(event.getBreakUuid(), event.getAcqusitionId(), null, NotificationStatus.STARTED.toString());

					boolean success = sendPretriggerMEssage(event);
					// update the status
					final String result = success ? NotificationStatus.SENT.toString() : NotificationStatus.FAIL.toString();
					DataManagerFactory.getInstance().putNotificationStatus(event.getBreakUuid(), event.getAcqusitionId(), null, result);
					logPretriggerSentKeyInfo(event, result);
				}  else {
					LOG.info("skip the pre-trigger message {} - {} as its status is {}", event.getBreakUuid(), event.getAcqusitionId(), status);
				}
			}

			// remove the lock
			DataManagerFactory.getInstance().unlock(lockName);
			LOG.info("lock {} was released", lockName);
		} catch(Exception ex) {
			LOG.warn(ex.getMessage());
		}
	}

	private void logPretriggerSentKeyInfo(PretriggerEvent event, final String result) {
		if(result.equals(NotificationStatus.SENT.toString())) {
			String ipStr = "";
		    try {
		    	ipStr = InetAddress.getLocalHost().toString();
		    } catch (UnknownHostException e) {}						
			LOG.info("Pretrigger was sent: {}, {} from {}", event.getBreakUuid(), event.getAcqusitionId(), ipStr);
		}
	}

	/**
	 * get the lock, and continue try until 1) get the lock or 2) the waiting time is over the specific LOCK TIME 
	 * (for example: 20 seconds)
	 * 
	 * use lock method : it is possible there is a leader node that always gets the lock. When this node is down, 
	 * 		other node will find a lead node
	 * use caslock method: each node has the chance to get the lock. The method was removed recently, 
	 * 		the method might need too add back if necessary 
	 * @param event
	 * @param lockName
	 * @return
	 */
	public boolean acquireLock(final PretriggerEvent event, final String lockName) {
		boolean locked = DataManagerFactory.getInstance().lock(lockName, LOCKTIME); // lock 20 seconds
		LOG.debug(lockName + " - lock status: " + locked);
		long cnt = 1;
		long start = System.currentTimeMillis();  // new line
		while (!locked) {
			if (LOG.isDebugEnabled() && cnt%800 == 1) {	LOG.debug("Unable to get the lock {}, will check the status and try again.", lockName); }
			final String status = DataManagerFactory.getInstance().getNotificationStatus(event.getBreakUuid(), event.getAcqusitionId(),null);
			if (NotificationStatus.SENT.toString().equals(status)) { // already processed, then consider it as a fake lock
				if (LOG.isDebugEnabled()) { LOG.debug("The notification has been SENT before"); }
				locked = false;
				break;
			} else {
				if((System.currentTimeMillis() - start) > LOCKTIME * 1000) {
					LOG.info("force release the lock : {} after {} seconds", lockName, LOCKTIME);
					DataManagerFactory.getInstance().unlock(lockName);
					DataManagerFactory.getInstance().putNotificationStatus(event.getBreakUuid(), event.getAcqusitionId(), null, NotificationStatus.NEW.toString());
				}
				try { Thread.sleep(2); } catch (InterruptedException e) {}
				// try to lock it again
				locked = DataManagerFactory.getInstance().lock(lockName, LOCKTIME); 
			}
			cnt++;
		}
		return locked;
	}

	/**
	 *
	 * @param event
	 * @return  message sending results
	 * @throws JobExecutionException
	 */
	private boolean sendPretriggerMEssage(final PretriggerEvent event) throws JobExecutionException {
		SignalProcessingNotificationType notification = null;
		String outbandMsg = "";
		try {
			notification = createOutbandNotification(event);
			outbandMsg = marshalOutOfBandMessage(notification, Schema.getSchema(event.getEsamVersion()));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			throw new JobExecutionException(e);
		}

		String identity = event.getBreakUuid() + " - " + event.getAcqusitionId();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start(identity);
		
		PretriggerNotificationService notificationService = (PretriggerNotificationService) CTX.getBean("pretriggerNotificationService");
		// send message
		int maxTry = 1;
		boolean success = false;
		//AuditLogger.auditMessage(outbandMsg, AuditLogHelper.populateAuditLogVO(null, notification));
		for(int i = 0; i < maxTry && !success; i++) {
			success = notificationService.sendNotificationMesage(event.getTranscoderUrl(), outbandMsg);
		}
		stopWatch.stop();
		LOG.info("It takes {} ms when sending pretrigger message [{}] to {}. Result: {}", stopWatch.getTotalTimeMillis(), 
				identity, event.getTranscoderUrl(), success? "sucess" : "failed");

		return success;
	}

	public static SignalProcessingNotificationType createOutbandNotification(PretriggerEvent event) throws DatatypeConfigurationException  {
		SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
		notification.setAcquisitionPointIdentity(event.getAcqusitionId());
		FeedConfigBean feedConfigBean = FeedConfigBean.getInstance();
		long advTimeForFeed = 0;

		// PRI- 14380 : Configurable advance interval and repeat interval for each channel.
		if (feedConfigBean.getPretriggerAdvanceInterval(event.getFeedId()) != -1) {
			LOG.debug("Advance Interval for feedId "+event.getFeedId()+" is configured to "+feedConfigBean.getPretriggerAdvanceInterval(event.getFeedId()));
			advTimeForFeed = feedConfigBean.getPretriggerAdvanceInterval(event.getFeedId());
		}
		else {
			LOG.debug("Advance Interval for feedId "+event.getFeedId()+" is not defined, using default value "+PretriggerSettingBean.getInstance().getTriggerAdvanceTime());
			advTimeForFeed = PretriggerSettingBean.getInstance().getTriggerAdvanceTime();
		}

		long repeatTimeForFeed = (feedConfigBean.getPretriggerRetryInterval(event.getFeedId()) != -1)
				? feedConfigBean.getPretriggerRetryInterval(event.getFeedId()) : PretriggerSettingBean.getInstance().getPretriggerFrequency();

		// Create program start component of the notification.
        ResponseSignalType respSignal = new ResponseSignalType();
        respSignal.setBinaryData(new BinarySignalType());
        respSignal.setAction("create");
        respSignal.setAcquisitionPointIdentity(event.getAcqusitionId());
        final long start = event.getStartTime() - advTimeForFeed;
        respSignal.setUTCPoint(SCCResponseUtil.generateUTCPoint(event.getStartTime())); // PKH-1226
        
        String encodedStr = PretriggerHandler.getInstance().generateEncodedScte35String(
        		Long.parseLong(event.getBreakId()), event.getBreakUuid(), event.getDuration());
		BinarySignalType binarySignal = new BinarySignalType();
		binarySignal.setValue(Base64.decodeBase64(encodedStr.getBytes()));
		binarySignal.setSignalType("SCTE35");
		respSignal.setBinaryData(binarySignal);
        
		EventScheduleType eventSchedule = new EventScheduleType();
		eventSchedule.setStartUTC(SCCResponseUtil.generateUTCPoint(start));
		eventSchedule.setInterval(JavaxUtil.getDatatypeFactory().newDuration(
				repeatTimeForFeed*1000));
		eventSchedule.setStopUTC(SCCResponseUtil.generateUTCPoint(event.getLastSpotStartTime() - 
				PretriggerSettingBean.getInstance().getAdvanceTimeMinimum()));
		respSignal.setEventSchedule(eventSchedule);
        
		//Add this program start in the notification.
		notification.getResponseSignal().add(respSignal);
		
		return notification;
	}

	public static String marshalOutOfBandMessage(
			final SignalProcessingNotificationType notification, Schema schema)
			throws JAXBException, PropertyException {
		return schema.getResponseHandler().generateSCCResponse(notification, null);
	}
	
}
