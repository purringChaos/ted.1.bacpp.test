package tv.blackarrow.cpp.pretrigger.jobs;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.pretrigger.beans.BisMonitorBean;
import tv.blackarrow.cpp.pretrigger.beans.PretriggerSettingBean;
import tv.blackarrow.cpp.pretrigger.handler.HintScheduleHandler;
import tv.blackarrow.cpp.pretrigger.manager.PretriggerSchedulingManager;
import tv.blackarrow.cpp.pretrigger.model.Media;
import tv.blackarrow.cpp.pretrigger.model.MediaPoint;
import tv.blackarrow.cpp.pretrigger.model.PretriggerEvent;
import tv.blackarrow.cpp.setting.AcquisitionConfigBean;
import tv.blackarrow.cpp.utils.ThreadSafeSimpleDateFormat;

public class BisQueryTimeTask extends TimerTask {
	private static final String ESAM_VERSION = "ESAM Version";
	private static final String ENCODER_ENDPOINT = "Encoder Endpoint";
	private static final Logger LOG = LogManager.getLogger(BisQueryTimeTask.class);
	private static final String UTC_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
	private ThreadSafeSimpleDateFormat dateformat = new ThreadSafeSimpleDateFormat(UTC_DATE_PATTERN);
	private static boolean DEBUG_ENABLED = LOG.isDebugEnabled();

	public static final BisMonitorBean MONITOR_BEAN = new BisMonitorBean();
	/**
	 * The method will query remote BIS to obtain the latest hint schedules.
	 * Once the schedules were fetched, the job will be scheduled to kick off
	 * at the specified time to send the notification messages 
	 */
	@Override
	public void run() {
	  try{
		if(!PretriggerSettingBean.getInstance().isServiceEnabled()) { return; }
		if(PretriggerSettingBean.getInstance().getTriggerAdvanceTime() < PretriggerSettingBean.ADVANCE_TIME_MINIMUM) {
			LOG.error("triggerAdvanceTime is too small: " + PretriggerSettingBean.getInstance().getTriggerAdvanceTime() + 
					". The pre-trigger service will not be enabled. Please change triggerAdvanceTime value in the configuration.");
			return;
		}

		long timestampStart = System.currentTimeMillis();
		long timestampEnd = timestampStart + PretriggerSettingBean.getInstance().getBisBreakWindow() * 60000;
		LOG.debug("query BIS schedule ->" + PretriggerSettingBean.getInstance().getBisUrl());
		StringBuilder urlSb = new StringBuilder(PretriggerSettingBean.getInstance().getBisUrl());
		urlSb.append("?startTime=").append(timestampStart).append("&endTime=").append(timestampEnd);
		
		LOG.debug("query endpoint => " + urlSb.toString());
		String content = HintScheduleHandler.getInstance().obtainHintSchedule(urlSb.toString());
		if(DEBUG_ENABLED) {
			  LOG.debug("query BIS schedule ->" + PretriggerSettingBean.getInstance().getBisUrl());
			  LOG.debug("query endpoint => " + urlSb.toString());
			  LOG.debug("received content:");
			  LOG.debug(content);
		}
		
		Map<String, PretriggerEvent> eventMap = new HashMap<String, PretriggerEvent>();
		
		List<Media> mediaList = HintScheduleHandler.getInstance().parseHintSchedule(content);	
		if(mediaList != null){
		  MONITOR_BEAN.setNoOfRecordsFromBis(mediaList.size());
		for(Media media : mediaList) {
			 Map<String, Set<String>> feedAqMap = AcquisitionConfigBean.getInstance().getFeedToAcquisitionPointMap();
			 Set<String> aqSet = feedAqMap.get(media.getId());
			if(aqSet == null ||  aqSet.size() == 0) {
				LOG.warn("cannot find the acquistion point based on feed ID: " + media.getId());
			} else {
				Iterator itr = aqSet.iterator();
				while(itr.hasNext()) {
					String aq = (String)itr.next();  // acquisition point
					
					long duration = 0;
					long startTime = 0;
					long lastSpotStartTime = 0;
					PretriggerEvent event = null;
					for(MediaPoint mp : media.getMediaPoints()) {
						if(event != null && event.getBreakId().equals(mp.getBreakId())) { // merge
							try {
								Date date = dateformat.parse(mp.getMatchTime());
								duration = date.getTime() - event.getStartTime() + getDuration(mp);
								event.setDuration(duration);
							} catch (ParseException e) {
								LOG.error(e.getMessage() + "==>" + mp.getMatchTime());
							}
							// keep a record of lastSpotStartTime
							lastSpotStartTime = getStartTime(mp);
							if(PretriggerSettingBean.getInstance().getAdvanceTimeMinimum() > 0) {
								event.setLastSpotStartTime(lastSpotStartTime);
							} else {
								event.setLastSpotStartTime(lastSpotStartTime + getDuration(mp));
							}
						} else {
							event = new PretriggerEvent();
//							if(breakId.isEmpty()) {
//								event.setBreakId(mp.getBreakId());
//							} else {
//								event.setBreakId(breakId); // set old break ID
//							}
							event.setBreakId(mp.getBreakId()); // set old break ID
							startTime = getStartTime(mp);							
							duration = getDuration(mp);
							lastSpotStartTime = startTime;  // init
							event.setAcqusitionId(aq);
							event.setDuration(duration);
							event.setFeedId(media.getId());
							event.setStartTime(startTime);
							event.setBreakUuid(mp.getBreakUuid());
							event.setLastSpotStartTime(lastSpotStartTime);
							
							Map<String, String> aqMap = AcquisitionConfigBean.getInstance().getAcquisitionPointMap().get(aq);
							
							String transcoderUrl = aqMap.get(ENCODER_ENDPOINT);
							if(transcoderUrl != null) {
								event.setTranscoderUrl(transcoderUrl);
							} else {
								LOG.error("no Encoder Endpoint value for acquistion point: " + aq);
							}
							String signalOffSetStr = aqMap.get("Signal Time Offset");
							if(signalOffSetStr != null) {
								try {
									event.setSignalOffset(Long.parseLong(signalOffSetStr));
								} catch(Exception ex) {}
							} else {
								LOG.warn("no Signal Time Offset value for acquistion point: " + aq);
								event.setSignalOffset(0);
							}
							
							event.setEsamVersion(aqMap.get(ESAM_VERSION));
							
							String key = event.getBreakId() + "-" + event.getStartTime() + "-" + event.getAcqusitionId();
							eventMap.put(key, event); // overwritten
						}
					}
				}
			}
		}
		}else{
		  MONITOR_BEAN.setNoOfRecordsFromBis(0);
		}
		Set<String> keySet = eventMap.keySet();
		Iterator itr = keySet.iterator();
		MONITOR_BEAN.resetNoOfRecordsScheduled();
		while(itr.hasNext()) {
			PretriggerEvent event = eventMap.get(itr.next());
			PretriggerSchedulingManager.getInstance().scheduleOutOfBandNotification(event);
			MONITOR_BEAN.increaseNoOfRecordsScheduled();
		}
	  }catch(Throwable e){
	    LOG.error("Error during bis query: ",e);
		}
	}

	private long getDuration(MediaPoint mp) {
		long duration = 0;
		Duration durationXML;
		try {
			durationXML = DatatypeFactory.newInstance().newDuration(mp.getDuration());
			duration = durationXML.getTimeInMillis(Calendar.getInstance()); 
		} catch (DatatypeConfigurationException e) {
			LOG.error(e.getMessage() + "==>" + mp.getDuration());
		}
		return duration;
	}

	private long getStartTime(MediaPoint mp) {
		long startTime = 0;
		
		try {
			Date date = dateformat.parse(mp.getMatchTime());
			startTime = date.getTime();
			//duration = mp.getDuration();
		} catch (ParseException e) {
			LOG.error(e.getMessage() + "==>" + mp.getMatchTime());
		}
		
		return startTime;
	}

}
