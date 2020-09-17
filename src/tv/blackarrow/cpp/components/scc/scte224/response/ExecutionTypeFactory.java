package tv.blackarrow.cpp.components.scc.scte224.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.components.scc.scte224.IBaseExecutor;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.utils.StreamExecutionType;

public class ExecutionTypeFactory {
	private static final Logger LOGGER = LogManager.getLogger(ExecutionTypeFactory.class);

	private ExecutionTypeFactory() {
	}

	/**
	 * Clients can be returned based on AcquisitionPoint.
	 * @param aqpt 
	 * @return
	 */
	public static IBaseExecutor getClient(AcquisitionPoint aqpt) {
		IBaseExecutor response = null;
		StreamExecutionType executionType = aqpt.getExecutionType();
		switch (executionType) {
		case ENCODER_LEVEL:
			response = new EncoderLevelExecutor();
			break;
		default:
			response = new ManifestLevelExecutor();
			break;
		}
		if (response != null) {
			String name = response.getClass().getName();
			LOGGER.debug(() -> aqpt.getAcquisitionPointIdentity() + " Acquisition Point will be executed at " + name);
		}
		return response;
	}
}
