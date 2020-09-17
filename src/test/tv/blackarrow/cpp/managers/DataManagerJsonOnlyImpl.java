package test.tv.blackarrow.cpp.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.couchbase.client.java.document.StringDocument;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.components.signalstate.model.SignalStateModel;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BlackoutEvent;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.RuntimeEnvironmentState;
import tv.blackarrow.cpp.model.SegmentationDescriptor;
import tv.blackarrow.cpp.model.ServerConfig;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * This implementation is only meant for performance testing to measure time for
 * json serialization and deserilization without data store overhead.
 */
public class DataManagerJsonOnlyImpl implements DataManager {

    // gson is thread safe so we only need one instance to use everywhere
    private static final Gson gson = new Gson();

    private String lastAcquisitionPoint;
    private String lastLoaderCursor;
    private String lastSignalProcessorCursor;
    private String lastPlacementOpportunity;
    private String lastConfirmedPlacementOpportunity;
    private String lastRuntimeEnvironmentState;
    private String blackoutEvent;
    private long blackoutCompilationTime;

    

    public DataManagerJsonOnlyImpl() {
        warmUp();
    }

    private void warmUp() {
        // to reduce latency during requests, we will run every object through gson
        // one time during initialization
        gson.fromJson(gson.toJson(new AcquisitionPoint()), AcquisitionPoint.class);
        gson.fromJson(gson.toJson(new LoaderCursor()), LoaderCursor.class);
        gson.fromJson(gson.toJson(new SignalProcessorCursor()), SignalProcessorCursor.class);
        gson.fromJson(gson.toJson(new PlacementOpportunity()), PlacementOpportunity.class);
        gson.fromJson(gson.toJson(new ConfirmedPlacementOpportunity()), ConfirmedPlacementOpportunity.class);
        gson.fromJson(gson.toJson(new RuntimeEnvironmentState()), RuntimeEnvironmentState.class);
    }

    @Override
    public AcquisitionPoint getAcquisitionPoint(String acquisitionPointIdentity) {
        return gson.fromJson(lastAcquisitionPoint, AcquisitionPoint.class);
    }

    @Override
    public void putAcquisitionPoint(AcquisitionPoint acquisitionPoint) {
        lastAcquisitionPoint = gson.toJson(acquisitionPoint);
    }

    @Override
    public void deleteAcquisitionPoint(String acquisitionPointIdentity) {
    }

    @Override
    public LoaderCursor getLoaderCursor(String feedExternalRef) {
        return gson.fromJson(lastLoaderCursor, LoaderCursor.class);
    }

    @Override
    public void putLoaderCursor(LoaderCursor loaderCursor) {
        lastLoaderCursor = gson.toJson(loaderCursor);
    }

    @Override
    public boolean casLoaderCursor(LoaderCursor loaderCursor) {
        lastLoaderCursor = gson.toJson(loaderCursor);
        return true;
    }

    @Override
    public void deleteLoaderCursor(String feedExternalRef) {
    }

    @Override
    public SignalProcessorCursor getSignalProcessorCursor(String acquisitionPointIdentity) {
        return gson.fromJson(lastSignalProcessorCursor, SignalProcessorCursor.class);
    }

    @Override
    public void putSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
        lastSignalProcessorCursor = gson.toJson(signalProcessorCursor);
    }

    @Override
    public boolean casSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
        lastSignalProcessorCursor = gson.toJson(signalProcessorCursor);
        return true;
    }

    @Override
    public void mergeSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor) {
    }

    @Override
    public void deleteSignalProcessorCursor(String acquisitionPointIdentity) {
    }

    @Override
    public PlacementOpportunity getPlacementOpportunity(String POKey) {
        return gson.fromJson(lastPlacementOpportunity, PlacementOpportunity.class);
    }

    @Override
    public void putPlacementOpportunity(PlacementOpportunity placementOpportunity) {
        lastPlacementOpportunity = gson.toJson(placementOpportunity);
    }

    @Override
    public boolean casPlacementOpportunity(PlacementOpportunity placementOpportunity) {
        lastPlacementOpportunity = gson.toJson(placementOpportunity);
        return true;
    }

    @Override
    public void deletePlacementOpportunity(String POKey) {
    }

    @Override
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String signalId) {
        return gson.fromJson(lastConfirmedPlacementOpportunity, ConfirmedPlacementOpportunity.class);
    }

    @Override
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String acquisitionPointIdentity, long utcSignalTime) {
        return gson.fromJson(lastConfirmedPlacementOpportunity, ConfirmedPlacementOpportunity.class);
    }

    @Override
    public void putConfirmedPlacementOpportunity(ConfirmedPlacementOpportunity confirmedPlacementOpportunity) {
        lastConfirmedPlacementOpportunity = gson.toJson(confirmedPlacementOpportunity);
    }

    @Override
    public void deleteConfirmedPlacementOpportunity(String signalId) {
    }

    @Override
    public RuntimeEnvironmentState getRuntimeEnvironmentState() {
        return gson.fromJson(lastRuntimeEnvironmentState, RuntimeEnvironmentState.class);
    }

    @Override
    public void putRuntimeEnvironmentState(RuntimeEnvironmentState runtimeEnvironmentState) {
        lastRuntimeEnvironmentState = gson.toJson(runtimeEnvironmentState);
    }

    @Override
    public void setDefaultDataExpirationSeconds(int expirationSeconds) {
    }

    @Override
    public boolean lock(String lockName, int expirationSeconds) {
        return true;
    }

    @Override
    public void unlock(String lockName) {
    }

	@Override
	public void putBlackoutEvents(String feedId, String blackoutEvents,
			long compilationTime) {
		this.blackoutEvent = blackoutEvents;
		this.blackoutCompilationTime = compilationTime;
		
	}

	@Override
	public long getBlackoutEventCompilationTime(String feedId) {
		return this.blackoutCompilationTime;
	}

	@Override
	public ArrayList<BlackoutEvent> getAllBlackoutEventsOnFeed(String feedName) {
		String value = blackoutEvent;
		if (value != null && !value.isEmpty()) { 		
			try {
				Gson gson = new Gson();
				ArrayList<BlackoutEvent> events= gson.fromJson(value, new TypeToken<ArrayList<BlackoutEvent>>(){}.getType());
				return events;
			} catch (Exception ex) {
				throw new RuntimeException("Fail to convert blackout event", ex);	
			}
		} else {
			return new ArrayList<BlackoutEvent>(0);
		}
	}

	@Override
	public ConfirmedPlacementOpportunity getConfirmedBlackoutCommonAcrossAFeedAPs(String signalId) {
        return gson.fromJson(lastConfirmedPlacementOpportunity, ConfirmedPlacementOpportunity.class);
	}


	@Override
	public void putConfirmedBlackout(
			ConfirmedPlacementOpportunity confirmedPlacementOpportunity) {
		lastConfirmedPlacementOpportunity = gson.toJson(confirmedPlacementOpportunity);
	}

	@Override
	public void deleteConfirmedBlackout(String signalId) {
		
	}

	@Override
	public BlackoutEvent getSingleBlackoutEvent(String singalId) {
		// NO implementation
		return null;
	}

	@Override
	public void putFeedToAcquistionMap(String feedId,
			Set<String> AcquisitionPointSet) {
		// NO implementation
	}

	@Override
	public Set<String> getFeedToAcquistionMap(String feedId) {
		// NO implementation
		return null;
	}

	@Override
	public void putBlackoutEventByEventId(String eventId, BlackoutEvent event) {
		// TODO Auto-generated method stub
	}

	@Override
	public BlackoutEvent getBlackoutEventByEventId(String eventId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void putActiveFeedList(String feedlist) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getActiveFeedList() {
		// TODO Auto-generated method stub
		return null;
}

	@Override
	public void deleteBlackoutEvent(BlackoutEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putBlackoutEvent(BlackoutEvent event) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#putBlackoutEvents(java.lang.String, java.lang.String)
	 */
	@Override
	public void putBlackoutEvents(String feedName, String blackoutEvents) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#putRealtimeNotificationStatus(java.lang.String, java.lang.String, long, long, java.lang.String)
	 */
	@Override
	public void putRealtimeNotificationStatus(String eventId,
			String acquistionPointIdentity, long utcStartTime, long utcEndtime,
			String status) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#getReatimeNotificationStatus(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public String getReatimeNotificationStatus(String eventId,
			String acquistionPointIdentity, long utcStartTime, long utcEndtime) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#isLockTaken(java.lang.String)
	 */
	@Override
	public boolean isLockTaken(String lockName) {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public String getHostedNotificationStatus(String eventId, String eventState) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putHostedNotificationStatus(String eventId, String eventState, String status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putInPointsSignal(String inPointSignalId,
			String confirmedCpoSignalId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getInPointsSignal(String inPointSignalId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getBlackoutMccStartTime(String acquisitionPointTd,
			String signalId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void putBlackoutMccStartTime(String acquisitionPointTd,
			String signalId, long startTime) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void putSegmentationDescriptor(String signalId, SegmentationDescriptor segmentationDescriptor){
		// TODO Auto-generated method stub
		
	}
	@Override
    public SegmentationDescriptor getSegmentationDescriptor(String signalId) {
		// TODO Auto-generated method stub 
		return null;
	}

	@Override
	public StringDocument lock(String lockName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unlock(StringDocument lockedDocument) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * put active data center
     */
	@Override
    public void putActiveDataCenter(String dataCenter){
    	return;
    }
    
    /**
     * get active data center
     */
	@Override
    public String getActiveDataCenter(){
		return null;
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#getServerConfig()
	 */
	@Override
	public ServerConfig getServerConfig() {
		return null;
	}

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.managers.DataManager#isServerInActiveDataCenter()
	 */
	@Override
	public boolean isServerInActiveDataCenter() {
		return false;
	}

	@Override
	public String get(String couchbaseSearchKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(String key, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putSingleBlackoutEvent(String signalId, BlackoutEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putBlackoutEvent(String eventId, String signalId,
			BlackoutEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StringDocument getDocument(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConfirmedPlacementOpportunity getConfirmedBlackoutForGivenAP(
			String acquisitionPointIdentity, String blackoutSignalId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AcquisitionPoint> getAcquisitionPoints(Collection<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeHostedNotificationStatus(String eventId, String eventState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putConfirmedBlackout(ConfirmedPlacementOpportunity confirmedPlacementOpportunity,String signalId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putAPConfirmedSignal(String apId, String signalId, String signalResponses) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SignalStateModel getLastConfirmedEvent(String apId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void appendToQueue(String queueName, String queueElement, int expirationTime) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void appendToQueue(String queueName, String queueElement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String popFromQueue(String queueName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void forcefullyUnscheduleAllNotifications(String upstreamNotificationQueueName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getQueueSize(String queueName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void set(String key, String value, int expirationTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unscheduleNotification(String upstreamNotificationQueueName, String eventSignalId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Integer> getEventScheduledNotificationQueueTimes(String mediaSignalId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveEventScheduledNotificationQueueTimes(String mediaSignalId, Set<Integer> scheduledTimes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unscheduleNotificationByNotificationId(String queueName, String scheduledNotificationId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteEventScheduledNotificationQueueTimes(String eventSignalId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getEventsDeletedSinceLastRulesLoading(long lastRuleLoadingTime, String feedId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recordEventDeletionViaRealtimeUpdates(long lastRuleLoadingTime, String feedId, String eventSignalId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean caslock(String lockName, int locktime) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void putNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType,
			String status) {
		// TODO Auto-generated method stub
		
	}

}
