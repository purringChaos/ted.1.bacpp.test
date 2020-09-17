/**
 *
 */
package tv.blackarrow.cpp.managers;

import com.google.gson.Gson;

import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;

/**
 * @author yrajoria
 *
 */
public interface SchedulelessAltEventDataManager {

	public static final Gson GSON = new Gson();
	public static final DataManager DATAMANAGER = DataManagerFactory.getInstance();
	public static final String NAME_SPACE_VALUE_SEPARATOR = ":";

	/**
	 * Returns the SchedulelessAltEventLedger available for the provided acquisition point and incoming upid hex.
	 */
	public SchedulelessAltEventLedger getAcquisitionPointSchedulelessAltEventLedger(String acquisitionPointIdentity);

	/**
	 * @param schedulelessAltEventLedger
	 * @param acquisitionPointId
	 * @param upidHex
	 */
	public void saveAcquisitionPointSchedulelessAltEventLedger(final SchedulelessAltEventLedger schedulelessAltEventLedger, final String acquisitionPointId, final String upidHex);

}
