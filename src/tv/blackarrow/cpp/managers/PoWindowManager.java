package tv.blackarrow.cpp.managers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PoWindowManager {

	private static final Logger LOGGER = LogManager.getLogger(PoWindowManager.class.getName());
	private static final PoWindowManager INSTANCE = new PoWindowManager();

	private PoWindowManager() {
		try {
		} catch (Exception ex) {
			LOGGER.error("ERROR occured in creating an instance of PoWindowManager", ex);
		}
	}

	public static PoWindowManager getInstance() {
		return INSTANCE;
	}
}
