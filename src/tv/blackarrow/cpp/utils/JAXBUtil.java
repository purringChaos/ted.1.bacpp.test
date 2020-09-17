package tv.blackarrow.cpp.utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JAXBUtil {

	private static final Logger LOG = LogManager.getLogger(JAXBUtil.class);
	public static final String LINEAR_POIS_SCC_PACKAGE_NAME = "tv.blackarrow.cpp.signaling";
	public static final String LINEAR_POIS_MCC_PACKAGE_NAME = "tv.blackarrow.cpp.manifest";
	public static final String LINEAR_POIS_MCC_EXT_PACKAGE_NAME = "tv.blackarrow.cpp.signaling.ext";
	public static final String LINEAR_POIS_BLACKOUT_PACKAGE_NAME = "tv.blackarrow.cpp.blackout";
	public static final String LINEAR_POIS_NOTIFICATION_PACKAGE_NAME = "tv.blackarrow.cpp.webservice.scte130_5_2010";
	public static final String LINEAR_POIS_SERVICE_CHECK_PACKAGE_NAME = "tv.blackarrow.cpp.webservice.scte130_5_2010";
	private static JAXBContext linearPOISSccJAXBContext = null;
	private static JAXBContext linearPOISMccJAXBContext = null;
	private static JAXBContext linearPOISBlackoutJAXBContext = null;
	private static JAXBContext linearPOISNotificationJAXBContext = null;
	private static JAXBContext linearPOISServiceCheckJAXBContext = null;

	
	// static initializer to init the cip JAXBContext instance eagerly
	static {
		try {
			linearPOISSccJAXBContext = JAXBContext
					.newInstance(LINEAR_POIS_SCC_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOG.error("Exception occured in getting JAXB context for context path: "
					+ LINEAR_POIS_SCC_PACKAGE_NAME,e);
		}

		try {
			linearPOISMccJAXBContext = JAXBContext
					.newInstance(LINEAR_POIS_MCC_PACKAGE_NAME+":"+LINEAR_POIS_MCC_EXT_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOG.error("Exception occured in getting JAXB context for context path: "
					+ LINEAR_POIS_MCC_PACKAGE_NAME, e);
		}

		try {
			linearPOISBlackoutJAXBContext = JAXBContext
					.newInstance(LINEAR_POIS_BLACKOUT_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOG.error("Exception occured in getting JAXB context for context path: "
					+ LINEAR_POIS_BLACKOUT_PACKAGE_NAME, e);
		}

		try {
			linearPOISNotificationJAXBContext = JAXBContext
					.newInstance(LINEAR_POIS_NOTIFICATION_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOG.error("Exception occured in getting JAXB context for context path: "
					+ LINEAR_POIS_NOTIFICATION_PACKAGE_NAME, e);
		}
		
		try {
			linearPOISServiceCheckJAXBContext = JAXBContext
					.newInstance(LINEAR_POIS_SERVICE_CHECK_PACKAGE_NAME);
		} catch (JAXBException e) {
			LOG.error("Exception occured in getting JAXB context for context path: "
					+ LINEAR_POIS_SERVICE_CHECK_PACKAGE_NAME,e);
		}
		
	}

	public static JAXBContext getLinearPOISSccJAXBContext() {
		return linearPOISSccJAXBContext;
	}

	public static JAXBContext getLinearPOISMccJAXBContext() {
		return linearPOISMccJAXBContext;
	}

	public static JAXBContext getLinearPOISBlackoutJAXBContext() {
		return linearPOISBlackoutJAXBContext;
	}

	public static JAXBContext getLinearPONotificationJAXBContext() {
		return linearPOISNotificationJAXBContext;
	}
	
	public static JAXBContext getLinearPOServiceCheckJAXBContext() {
		return linearPOISServiceCheckJAXBContext;
	}
}
