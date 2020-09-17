package tv.blackarrow.cpp.notifications.upstream.messages.runtime.builders.esam;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.notifications.upstream.messages.queue.NotificationMessage;
import tv.blackarrow.cpp.utils.JavaxUtil;

public abstract class BaseEsamNotificationMessageBuilder implements EsamNotificationMessageBuilder {
	private static final Logger LOG = LogManager.getLogger(BaseEsamNotificationMessageBuilder.class);
	//public static tv.blackarrow.cpp.signaling.ObjectFactory I01_SIGNALING_OBJECT_FACTORY = new tv.blackarrow.cpp.signaling.ObjectFactory();

	public String getPTSTime() {
		// TODO:Amit, Following line was introduced to test the patch and should be
		// cleaned up in the later releases.
		final String pts_time = Boolean.valueOf(System.getProperty("cadent.test.pts", Boolean.FALSE.toString()))
				? "000010100110011001111111010100101"
				: "";
		if (Boolean.valueOf(System.getProperty("cadent.test.pts", Boolean.FALSE.toString()))) {
			LOG.error(()->
					"\"cadent.test.pts\" system property should not be set in a production environment, this should only be set for testing in Dev and QA environments. "
							+ "Actual out of band notification should send empty string \"\" i.e. 0 in the pts_time.");
		}
		return pts_time;

	}
	protected Long getSegmentEventId(NotificationMessage notificationMessage)
	{
	long segmentEventId = notificationMessage.getNotificationScheduledTime() & 0x3fffffff;//getNotificationScheduledTime is currentTime for us
	try {
		segmentEventId = Long.valueOf(notificationMessage.getEventId());
	} catch(Exception ex) {
		LOG.debug(()->"Event id \"" + notificationMessage.getEventId() + "\" is not a number and can not be parsed as long value "+" : Using the system generated segment event id.");
	}
	return segmentEventId;
}
	public javax.xml.datatype.Duration getDuration(long contentDuretion) throws DatatypeConfigurationException{
		
		return JavaxUtil.getDatatypeFactory().newDuration(contentDuretion);
	}
	
	public long getTimeInMillisWithAQOffset(long eventStartOrStopTime, long aqSignalOffsetInMillis) {
		return eventStartOrStopTime + aqSignalOffsetInMillis;
	}
}
