//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow, and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Author: $
// $Date: $
// $Revision: $
//

package tv.blackarrow.cpp.setting;

import java.io.File;
import java.io.StringReader;
import java.util.TimerTask;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.quartz.JobExecutionException;

import tv.blackarrow.cpp.cb.CouchbaseUtil;
import tv.blackarrow.cpp.components.SCCResponseComponent;
import tv.blackarrow.cpp.handler.Schema;
import tv.blackarrow.cpp.mcctemplate.MCCTemplateConfiguration;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.notifications.hosted.executor.HostedNotificationExecutorsService;
import tv.blackarrow.cpp.notifications.upstream.executor.ScheduledNotificationExecutor;
import tv.blackarrow.cpp.pretrigger.beans.PretriggerSettingBean;
import tv.blackarrow.cpp.pretrigger.handler.HintScheduleHandler;
import tv.blackarrow.cpp.pretrigger.jobs.BisQueryTimeTask;
import tv.blackarrow.cpp.quartz.DataMaintenanceJob;
import tv.blackarrow.cpp.quartz.PODataFileHandlingJob;
import tv.blackarrow.cpp.quartz.ScheduleLoadingJob;
import tv.blackarrow.cpp.quartz.SchedulePurgerJob;
import tv.blackarrow.cpp.signal.signaling.SCTE35PointDescriptorType;
import tv.blackarrow.cpp.signal.signaling.UTCPointDescriptorType;
import tv.blackarrow.cpp.signaling.SignalProcessingEventType;
import tv.blackarrow.cpp.signaling.SignalProcessingNotificationType;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.JAXBUtil;
import tv.blackarrow.cpp.utils.SCCResponseUtil;
import tv.blackarrow.cpp.utils.Scte35BinaryUtil;

/**
 * it should be utilized for some configurations initialization check cip.xml
 * where it was defined as a Spring bean
 */
public class BootStrap {
    private static final Logger LOGGER = LogManager.getLogger(BootStrap.class.getName());

    public BootStrap() {
    	LOGGER.info(()-> "BootStraping started for ESS...");
        if (CppConfigurationBean.getInstance().isServiceEnabled()) {
            LOGGER.info("Linear POIS is enabled");
             LOGGER.info("1. Initializing couchbase...");
            initCouchbase();
            //LOGGER.info(()-> "2. Initializing HTTP Connection Pool for scheduling...");
			//HTTPConnectionPoolManager.initializeHttpConnectionPoolForScheduling();
			LOGGER.info(()-> "2. Loading Rulesfile...");
            startLoader();
            LOGGER.info(()-> "3. Init Bis Query Timer...");
            initBisQueryTimer();
            PretriggerSettingBean.getInstance().monitorFileChange();
        } else {
            LOGGER.info(()->"Linear POIS is disabled (please check cpp_bean.xml)");
        }

        warmup();
        
        ScheduledNotificationExecutor.initializeExecutors();
        HostedNotificationExecutorsService.initializeExecutors();
        LOGGER.info(()-> "BootStraping ended for ESS...");
    }

    private void initCouchbase() {
        // initialize the Couchbase Client to cluster note socket connection for "pois_linear" bucket
        CouchbaseUtil.getInstance().getCouchbaseClient(CppConstants.COUCHBASE_CLUSTER_LINEAR_BUCKET_NAME);
       // CouchbaseUtil.getInstance().monitorClusterAutoFailover();
        LOGGER.debug(()->"Configured wait time that the threads may wait for is: " + SettingUtils.getThreadLockAcquireWaitTime() + " ms.");
    }
    private void startLoader() {
        LOGGER.debug(()->"Server restarted. Forcing Runtime Loader's full loading cycle.");
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
        //Load MCC Response Template configuration file.
        MCCTemplateConfiguration.getInstance();
    }

    /**
     * do everything we can here to make sure the first requests are processed
     * quickly
     */
    private void warmup() {
        // the biggest issue is that JAXB initialization takes quite a bit of time (>2s), do it now.
        try {
            warmupJaxb();
        } catch (JAXBException e) {
            LOGGER.error(()->"Error initializing JAXB", e);
        }

        // the hotspot jvm is able to optimize routines (mainly by compiling instead of interpreting) for 
        // us after they have been called a significant number of times so warm them up now.  if this is 
        // not done then the first requests are slow to be processed.
        String binaryData = "/DAvAAAAAAAA///wFAVAAArzf+//xbpe3/4AKT1sAAEAAAAKAAhDVUVJAAAAAOo6Lgc=";
        for (int i = 0; i < 5000; i++) {
            // processing of xml calendars and durations can be optimized
            UTCPointDescriptorType utcPoint = SCCResponseUtil.generateUTCPoint(System.currentTimeMillis());
            SCCResponseUtil.adjustUTCPoint(utcPoint.getUtcPoint(), 123456790);
            
            // the binary encoder/decoder can be optimized
            SCTE35PointDescriptorType scte35Pt = new SCTE35PointDescriptorType();       
            Scte35BinaryUtil.decodeScte35BinaryData(binaryData, scte35Pt, new StringBuilder(), new StringBuilder());
            Scte35BinaryUtil.encodeScte35DataToBinary(scte35Pt, Scte35BinaryUtil.toBitString(1234567890, 33), Scte35BinaryUtil.toBitString(1234567890, 33));
        }
        
        // running the data maintenance job on startup primes couchbase and gets rid of stale data
        // which improves the response times for first requests.
        try {
            new DataMaintenanceJob().execute(null);
        } catch (Exception e) {
            LOGGER.warn(()->"Error executing DataMaintenanceJob");
        }
    }

    private void warmupJaxb() throws JAXBException {
        // it helps significantly more to have xml with some real elements in it
        String request = "<SignalProcessingEvent xmlns=\"urn:cablelabs:iptvservices:esam:xsd:signal:1\" xmlns:md=\"http://www.cablelabs.com/namespaces/metadata/xsd/signaling/2\">"
                + "<AcquiredSignal acquisitionPointIdentity=\"FID001_AP1\" acquisitionSignalID=\"3e9286a6-9612-4228-88fc-662cb0b211c6\" acquisitionTime=\"2013-11-22T10:49:00Z\">"
                + "<md:UTCPoint utcPoint=\"2014-05-21T04:05:00.000Z\" />"
                + "<md:BinaryData signalType=\"SCTE35\">/DAvAAAAAAAA///wFAVAAArzf+//xbpe3/4AKT1sAAEAAAAKAAhDVUVJAAAAAOo6Lgc=</md:BinaryData>"
                + "</AcquiredSignal>" + "</SignalProcessingEvent>";
        String response = "<SignalProcessingNotification acquisitionPointIdentity=\"FID001_AP1\" xmlns=\"urn:cablelabs:iptvservices:esam:xsd:signal:1\" xmlns:sig=\"http://www.cablelabs.com/namespaces/metadata/xsd/signaling/2\">"
                + "<ResponseSignal action=\"replace\" signalPointID=\"+n1W6k7g4xGgXPbuNd/bHA==\" acquisitionSignalID=\"3e9286a6-9612-4228-88fc-662cb0b211c6\" acquisitionPointIdentity=\"FID004_AP1\">"
                + "<sig:UTCPoint utcPoint=\"2014-05-21T03:45:00.000Z\"/>"
                + "<sig:BinaryData signalType=\"SCTE35\">/DBLAAAAAAAA///wBQb+AAAAAAA1AjNDVUVJQAAK83//AAApMuAJH1NJR05BTDorbjFXNms3ZzR4R2dYUGJ1TmQvYkhBPT00AAD1MM/J</sig:BinaryData>"
                + "</ResponseSignal>"
                + "<ConditioningInfo acquisitionSignalIDRef=\"2p9ZyhVcTiCf8cQ-QWsGXg\" duration=\"PT0.000S\"/>"
                + "<StatusCode classCode=\"0\"/>" + "</SignalProcessingNotification>";

        // the hotspot jvm can optimize these routines further if we prime it with a large number of calls.
        // without this, first requests to the server will be slow. 
        for (int i = 0; i < 5000; i++) {
            // getting the context does a large part of the initialization but actually doing
            // a marshal and unmarshall does a little bit more work that improves first response time.
            JAXBContext jaxbCxt = JAXBUtil.getLinearPOISSccJAXBContext();
            Unmarshaller unmarshaller = jaxbCxt.createUnmarshaller();
            unmarshaller.unmarshal(new StreamSource(new StringReader(request)), SignalProcessingEventType.class);
            JAXBElement<SignalProcessingNotificationType> element = unmarshaller.unmarshal(new StreamSource(new StringReader(response)), SignalProcessingNotificationType.class);
            SignalProcessingNotificationType responseType = element.getValue();
            new SCCResponseComponent().objectToXML(responseType, Schema.i01);
            //System.out.println(new SCCResponseComponent().objectToXML(responseType));
        }
        
    }

    private void initBisQueryTimer() {
    	TimerTask timerTask = new BisQueryTimeTask();
    	HintScheduleHandler.getInstance().getQueryTimer().scheduleAtFixedRate(timerTask, 30000, 
    			PretriggerSettingBean.getInstance().getBisQueryInterval()*60000); // delay 30 seconds
     }
}
