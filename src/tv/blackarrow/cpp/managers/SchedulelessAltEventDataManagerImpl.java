/**
 *
 */
package tv.blackarrow.cpp.managers;

import java.lang.reflect.Type;

import org.apache.commons.lang.StringUtils;

import tv.blackarrow.cpp.model.schedulelessaltevent.SchedulelessAltEventLedger;

/**
 *
 */
public final class SchedulelessAltEventDataManagerImpl implements SchedulelessAltEventDataManager {

	// gson is thread safe so we only need one instance to use everywhere
	private static final SchedulelessAltEventDataManagerImpl SCHEDULELESS_ALTEVENT_DATA_MANAGER = new SchedulelessAltEventDataManagerImpl();

	private SchedulelessAltEventDataManagerImpl() {
		super();
		warmup();
	}

	public static SchedulelessAltEventDataManager getInstance() {
		return SCHEDULELESS_ALTEVENT_DATA_MANAGER;
	}

	private void warmup() {
		// to reduce latency during requests, we will run every object through gson one time during initialization.
		GSON.fromJson(GSON.toJson(new SchedulelessAltEventLedger()), SchedulelessAltEventLedger.class);

	}

	@Override
	public SchedulelessAltEventLedger getAcquisitionPointSchedulelessAltEventLedger(String acquisitionPointIdentity) {
		SchedulelessAltEventLedger apMediaLedger = getObjectFromDataStore(
				DataManagerCouchbaseImpl.AP_SCHEDULELESS_ALTEVENT_LEDGER_NAME_SPACE + acquisitionPointIdentity, SchedulelessAltEventLedger.class);

		return apMediaLedger;
	}

	@Override
	public void saveAcquisitionPointSchedulelessAltEventLedger(SchedulelessAltEventLedger schedulelessAltEventLedger, String acquisitionPointId, String signalId) {
		saveObjectInDataStore(DataManagerCouchbaseImpl.AP_SCHEDULELESS_ALTEVENT_LEDGER_NAME_SPACE + acquisitionPointId, schedulelessAltEventLedger,
				SchedulelessAltEventLedger.class);
	}

	private <T> T getObjectFromDataStore(final String couchbaseSearchKey, final Class<T> couchbaseValueObjectClass) {
		String objectJsonStringFromDataStore = DATAMANAGER.get(couchbaseSearchKey);
		if (StringUtils.isNotBlank(objectJsonStringFromDataStore)) {
			return GSON.fromJson(objectJsonStringFromDataStore, couchbaseValueObjectClass);
		}
		return null;
	}

	private void saveObjectInDataStore(final String couchbaseSearchKey, final Object couchbaseValueObject, final Type typeOfObject) {
		DATAMANAGER.set(couchbaseSearchKey, typeOfObject != null ? GSON.toJson(couchbaseValueObject, typeOfObject) : GSON.toJson(couchbaseValueObject));
	}

}
