package tv.blackarrow.cpp.utils;

import static tv.blackarrow.cpp.utils.ResponseSignalAction.REPLACE;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.signal.signaling.BinarySignalType;
import tv.blackarrow.cpp.signaling.ResponseSignalType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;

public class TerritoryUpdateNotificationTask implements Runnable {

	private static final Logger LOG = LogManager.getLogger(TerritoryUpdateNotificationTask.class);
	private AcquisitionPoint acquisitionPoint;
	private ConfirmedPlacementOpportunity cpo;
	private BlackoutEvent blackoutEvent;
	private long currentSystemTime;
	private DataManager dataManager;
	
	
	public TerritoryUpdateNotificationTask(AcquisitionPoint acquisitionPoint, ConfirmedPlacementOpportunity cpo,long currentSystemTime,BlackoutEvent blackoutEvent,DataManager dataManager) {
		this.acquisitionPoint = acquisitionPoint;
		this.cpo = cpo;
		this.currentSystemTime = currentSystemTime;
		this.blackoutEvent = blackoutEvent;
		this.dataManager = dataManager;
	}

	/*This task was broken during sometime, and there is requirement cleared around it. So we discontinuing supporting this. Not removing the old code, but not
	 * moving the ntoifictaion to new structure. Once requirements gets clear PRI-10759  
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		SignalProcessingNotificationType notification;
		try {
			//dataManager.putRealtimeNotificationStatus(blackoutEvent.getEventId(), acquisitionPoint.getAcquisitionPointIdentity(), blackoutEvent.getUtcStartTime(), 
			//		BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent), NotificationStatus.NEW.toString());
			notification = createTerritoryUpdateTransCoderNotification();
			List<AcquisitionPoint> acqPoints = new ArrayList<>();
			List<BlackoutEvent> passedBlackouts = new ArrayList<>();
			acqPoints.add(acquisitionPoint);
			passedBlackouts.add(blackoutEvent);
			//EventCRUDNotificationsHandler.handleNotification(acqPoints, EventAction.UPDATE, passedBlackouts, true);			
			//dataManager.putRealtimeNotificationStatus(blackoutEvent.getEventId(), acquisitionPoint.getAcquisitionPointIdentity(), blackoutEvent.getUtcStartTime(), 
			//		BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent), NotificationStatus.SENT.toString());
			LOG.info("Successfully sent territory update notification for event "+ blackoutEvent.getEventId() + " at acquistion point " + acquisitionPoint.getAcquisitionPointIdentity() + 
					" for feed "+acquisitionPoint.getFeedExternalRef());
		} catch (DatatypeConfigurationException e) {
			//dataManager.putRealtimeNotificationStatus(blackoutEvent.getEventId(), acquisitionPoint.getAcquisitionPointIdentity(), blackoutEvent.getUtcStartTime(), 
			//		BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent), NotificationStatus.FAIL.toString());
			LOG.warn("Some unexpected error occured, couldn't send the Territory replace notification to the transcoder.", e);
		}
	}
	
	private SignalProcessingNotificationType createTerritoryUpdateTransCoderNotification() throws DatatypeConfigurationException{
		
		long segmentEventId = currentSystemTime & 0x3fffffff;
		try {
			segmentEventId = Long.valueOf(blackoutEvent.getEventId());
		} catch(Exception ex) {
			LOG.info("Event id \"" + blackoutEvent.getEventId() + "\" is not a number and can not be parsed as long value "+" : Using the system generated segment event id.");
		}
		
			SignalProcessingNotificationType notification = new SignalProcessingNotificationType();
			notification.setAcquisitionPointIdentity(acquisitionPoint.getAcquisitionPointIdentity());
			

			DatatypeFactory df = DatatypeFactory.newInstance();
	    	GregorianCalendar tempGregorianCalendar = new GregorianCalendar();

	    	long blackoutEventStartTimeWithOffset = cpo.getActualUtcStartTime()+acquisitionPoint.getSignalTimeOffset();
	    	long blackoutEventStopTimeWithOffset  = BlackoutEvent.getActualBlackoutStopTime(cpo, blackoutEvent)+ acquisitionPoint.getSignalTimeOffset();
	        
	    	//Create blackout event start time element.
	    	tempGregorianCalendar.setTimeInMillis(blackoutEventStartTimeWithOffset);
	        XMLGregorianCalendar startUTCTimeWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);
	        
	        //Create blackout event end time element.
	        tempGregorianCalendar.setTimeInMillis(blackoutEventStopTimeWithOffset);
	        XMLGregorianCalendar stopUTCTimeWithOffset = df.newXMLGregorianCalendar(tempGregorianCalendar);
	        
	        //create the Acquisition Signal Id
	        String acquisitionSignalId = UUIDUtils.getBase64UrlEncodedUUID();
	        // Create program start component of the notification.
	        ResponseSignalType respSignalProgramStart = new ResponseSignalType();
	        respSignalProgramStart.setBinaryData(new BinarySignalType());
			String upidStr = ESAMHelper.generateUpidString(blackoutEvent.getSignalId());
			byte[] upid = new HexBinaryAdapter().unmarshal(upidStr);
			ESAMObjectCreationHelper.setProgramStartResponseSignal(respSignalProgramStart, acquisitionPoint, upid, startUTCTimeWithOffset, stopUTCTimeWithOffset, acquisitionSignalId, 
					REPLACE, segmentEventId, "", Scte35BinaryUtil.toBitString(0, 33), false, blackoutEventStopTimeWithOffset - blackoutEventStartTimeWithOffset, null, 
					null, false, null, false);
	        
			notification.getResponseSignal().add(respSignalProgramStart);
			
	        upidStr = ESAMHelper.generateUpidString(cpo.getTerritoryUpdateSignalId());
			upid = new HexBinaryAdapter().unmarshal(upidStr);

			
			ResponseSignalType respSignalContentIdentification = ESAMObjectCreationHelper.createContentIndentficationRespSignal( acquisitionPoint, upid, startUTCTimeWithOffset, 
					stopUTCTimeWithOffset, acquisitionSignalId, true, 
					segmentEventId, REPLACE, "", Scte35BinaryUtil.toBitString(0, 33), false, blackoutEventStopTimeWithOffset-blackoutEventStartTimeWithOffset, 
					null, null, (cpo != null && cpo.isAborted()), false);
			
			//Add this content identification in the notification.
			notification.getResponseSignal().add(respSignalContentIdentification);
			return notification;
			
	}
	
}
