package tv.blackarrow.cpp.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.couchbase.client.java.document.StringDocument;

import tv.blackarrow.cpp.components.signalstate.model.SignalStateModel;
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

public interface DataManager {

    public static final String EVENT_SCHEDULED_NOTIFICATION_QUEUE_TIMES = "MSNQT:";
    /**
     * Retrieves an AcquisitionPoint object from the data store given its
     * acquisition point identity.
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity of the object to retrieve.
     * @return AcquisitionPoint object with the specified identity or null if
     *         there is no element in the data store with the identity
     *         specified. A deep copy of the object is returned so no changes
     *         made to the returned object will affect the data store. To update
     *         the data store, the caller must call the appropriate put method
     *         explicitly.
     */
    public AcquisitionPoint getAcquisitionPoint(String acquisitionPointIdentity);

    /**
     * Inserts the AcquisitionPoint object provided in the data store. If an
     * object already exists with the same key values as the new one, it is
     * replaced with the new object.
     * 
     * @param acquisitionPoint
     *            AcquisitionPoint object to store.
     */
    public void putAcquisitionPoint(AcquisitionPoint acquisitionPoint);

    /**
     * Deletes an AcquisitionPoint object from the data store given its
     * acquisition point identity.
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity of the object to remove.
     */
    public void deleteAcquisitionPoint(String acquisitionPointIdentity);

    /**
     * Retrieves a LoaderCursor object from the data store given its feed
     * external reference.
     * 
     * @param feedExternalRef
     *            Feed external reference of the object to retrieve.
     * @return LoaderCursor object with the specified feed external reference or
     *         null if there is no element in the data store with the feed
     *         external reference specified. A deep copy of the object is
     *         returned so no changes made to the returned object will affect
     *         the data store. To update the data store, the caller must call
     *         the appropriate put method explicitly.
     */
    public LoaderCursor getLoaderCursor(String feedExternalRef);

    /**
     * Inserts the LoaderCursor object provided in the data store. If an object
     * already exists with the same key values as the new one, it is replaced
     * with the new object.
     * 
     * @param loaderCursor
     *            LoadCursor object to store.
     */
    public void putLoaderCursor(LoaderCursor loaderCursor);

    /**
     * Updates the LoaderCursor object provided in the data store if and only if
     * the object has not been modified since it was originally retrieved. The
     * objects casId field is used to verify that the object has not been
     * modified. It is the responsibility of the caller to handle what to do if
     * the update fails because the object has been modified. The operation
     * cannot simply be retried. The updated object must be retrieved, changes
     * applied and the compare and set operation tried again.
     * 
     * @param loaderCursor
     *            LoadCursor object to store.
     * @return true if the update succeeds, false if it does not
     */
    public boolean casLoaderCursor(LoaderCursor loaderCursor);

    /**
     * Deletes a LoaderCursor object from the data store given its feed external
     * reference.
     * 
     * @param feedExternalRef
     *            Feed external reference of the object to remove.
     */
    public void deleteLoaderCursor(String feedExternalRef);

    /**
     * Retrieves a SignalProcessorCursor object from the data store given its
     * acquisition point identity.
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity of the object to retrieve.
     * @return SignalProcessorCursor object with the specified acquisition point
     *         identity or null if there is no element in the data store with
     *         the acquisition point identity specified. A deep copy of the
     *         object is returned so no changes made to the returned object will
     *         affect the data store. To update the data store, the caller must
     *         call the appropriate put method explicitly.
     */
    public SignalProcessorCursor getSignalProcessorCursor(String acquisitionPointIdentity);

    /**
     * Inserts the SignalProcessorCursor object provided in the data store. If
     * an object already exists with the same key values as the new one, it is
     * replaced with the new object.
     * 
     * @param signalProcessorCursor
     *            SignalProcessorCursor object to store.
     */
    public void putSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor);

    /**
     * Updates the SignalProcessorCursor object provided in the data store if
     * and only if the object has not been modified since it was originally
     * retrieved. The objects casId field is used to verify that the object has
     * not been modified. It is the responsibility of the caller to handle what
     * to do if the update fails because the object has been modified. The
     * operation cannot simply be retried. The updated object must be retrieved,
     * changes applied and the compare and set operation tried again.
     * 
     * @param signalProcessorCursor
     *            SignalProcessorCursor object to update.
     * @return true if the update succeeds, false if it does not
     */
    public boolean casSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor);

    /**
     * Inserts the SignalProcessorCursor object provided in the data store if it
     * is not there yet. If a SignalProcessorCursor exists already then it adds
     * new PO keys for new ad zones only, leaving existing ones intact. This
     * method only updates the data store if changes are actually made to the
     * cursor. This method ensures that updates from other processes are not
     * overwritten by using compare-and-set operations internally.
     * 
     * @param signalProcessorCursor
     */
    public void mergeSignalProcessorCursor(SignalProcessorCursor signalProcessorCursor);

    /**
     * Deletes a SignalProcessorCursor object from the data store given its
     * acquisition point identity.
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity of the object to remove.
     */
    public void deleteSignalProcessorCursor(String acquisitionPointIdentity);

    /**
     * Retrieves a PlacementOpportunity object from the data store given its key
     * (this is currently the GUID break_id from the database).
     * 
     * @param POKey
     *            Placement opportunity key of the object to retrieve.
     * @return PlacementOpportunity object with the specified PO key or null if
     *         there is no element in the data store with the Po key specified.
     *         A deep copy of the object is returned so no changes made to the
     *         returned object will affect the data store. To update the data
     *         store, the caller must call the appropriate put method
     *         explicitly.
     */
    public PlacementOpportunity getPlacementOpportunity(String POKey);

    /**
     * Inserts the PlacementOpportunity object provided in the data store. If an
     * object already exists with the same key values as the new one, it is
     * replaced with the new object.
     * 
     * @param placementOpportunity
     *            PlacementOpportunity object to store.
     */
    public void putPlacementOpportunity(PlacementOpportunity placementOpportunity);

    /**
     * Updates the PlacementOpportunity object provided in the data store if and
     * only if the object has not been modified since it was originally
     * retrieved. The objects casId field is used to verify that the object has
     * not been modified. It is the responsibility of the caller to handle what
     * to do if the update fails because the object has been modified. The
     * operation cannot simply be retried. The updated object must be retrieved,
     * changes applied and the compare and set operation tried again.
     * 
     * @param placementOpportunity
     *            PlacementOpportunity object to update.
     * @return true if the update succeeds, false if it does not
     */
    public boolean casPlacementOpportunity(PlacementOpportunity placementOpportunity);

    /**
     * Deletes a PlacementOpportunity object from the data store given its key
     * (this is currently the GUID break_id from the database).
     * 
     * @param POKey
     *            Placement opportunity key of the object to remove.
     */
    public void deletePlacementOpportunity(String POKey);

    /**
     * Retrieves a ConfirmedPlacementOpportunity object from the data store
     * given the signal identifier.
     * 
     * @param signalId
     *            Signal ID of the confirmed placement opportunity to retrieve.
     * @return ConfirmedPlacementOpportunity associated with the signal ID
     *         provided. A deep copy of the object is returned so no changes
     *         made to the returned object will affect the data store. To update
     *         the data store, the caller must call the appropriate put method
     *         explicitly.
     */
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String signalId);

    /**
     * Retrieves a ConfirmedPlacementOpportunity object from the data store
     * given the acquisition point identity and UTC signal time (as a java
     * epoch).
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity that the confirmed placement
     *            opportunity is associated with.
     * @param utcSignalTime
     *            UTC signal time associated with the confirmed placement
     *            opportunity (in java epoch format).
     * @return ConfirmedPlacementOpportunity associated with the acquisition
     *         point identity and UTC signal time provided. A deep copy of the
     *         object is returned so no changes made to the returned object will
     *         affect the data store. To update the data store, the caller must
     *         call the appropriate put method explicitly.
     */
    public ConfirmedPlacementOpportunity getConfirmedPlacementOpportunity(String acquisitionPointIdentity, long utcSignalTime);

    /**
     * Inserts the ConfirmedPlacementOpportunity object provided in the data
     * store. If an object already exists with the same key values as the new
     * one, it is replaced with the new object.
     * 
     * @param confirmedPlacementOpportunity
     *            ConfirmedPlacementOpportunity object to store.
     */
    public void putConfirmedPlacementOpportunity(ConfirmedPlacementOpportunity confirmedPlacementOpportunity);

    /**
     * Deletes a ConfirmedPlacementOpportunity object from the data store given
     * the signal identifier.
     * 
     * @param signalId
     *            Signal ID of the confirmed placement opportunity to remove.
     */
    public void deleteConfirmedPlacementOpportunity(String signalId);

    /**
     * Retrieves a blackout ConfirmedPlacementOpportunity object from the data store
     * given the signal identifier.
     * 
     * @param signalId
     *            Signal ID of the confirmed placement opportunity to retrieve.
     * @return ConfirmedPlacementOpportunity associated with the signal ID
     *         provided. A deep copy of the object is returned so no changes
     *         made to the returned object will affect the data store. To update
     *         the data store, the caller must call the appropriate put method
     *         explicitly.
     */
    public ConfirmedPlacementOpportunity getConfirmedBlackoutCommonAcrossAFeedAPs(String signalId);

    
    /**
     * Retrieves a blackout ConfirmedPlacementOpportunity object from the data store
     * given the acquisition point identity and UTC signal time (as a java
     * epoch).
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity that the confirmed blackout event is associated with.
     * @param blackoutSignalId
     *            signal id associated with the confirmed blackout event (in java epoch format).
     * @return ConfirmedPlacementOpportunity associated with the acquisition
     *         point identity and the signal id provided. A deep copy of the
     *         object is returned so no changes made to the returned object will
     *         affect the data store. To update the data store, the caller must
     *         call the appropriate put method explicitly.
     */
    public ConfirmedPlacementOpportunity getConfirmedBlackoutForGivenAP(String acquisitionPointIdentity, String blackoutSignalId);

    /**
     * Inserts the blackout ConfirmedPlacementOpportunity object provided in the data
     * store. If an object already exists with the same key values as the new
     * one, it is replaced with the new object. Blackout ConfirmedPlacementOpportunity and
     * other ConfirmedPlacementOpportunity are stored in separate namespace.
     * 
     * @param confirmedPlacementOpportunity
     *            ConfirmedPlacementOpportunity object to store.
     */
    public void putConfirmedBlackout(ConfirmedPlacementOpportunity confirmedPlacementOpportunity);

    /**
     * Deletes a blackout ConfirmedPlacementOpportunity object from the data store given
     * the signal identifier.
     * 
     * @param signalId
     *            Signal ID of the confirmed blackout event to remove.
     */
    public void deleteConfirmedBlackout(String signalId);

    
    /**
     * Retrieves the RuntimeEnvironmentState object from the data store. There
     * is only one object of this type in the data store so no input is
     * required.
     * 
     * @return RuntimeEnvironmentState object or null if it is not in the data
     *         store. A deep copy of the object is returned so no changes made
     *         to the returned object will affect the data store. To update the
     *         data store, the caller must call the appropriate put method
     *         explicitly.
     */
    public RuntimeEnvironmentState getRuntimeEnvironmentState();

    /**
     * Inserts the RuntimeEnvironmentState object provided in the data store. If
     * an object already exists, it is replaced with the new object.
     * 
     * @param runtimeEnvironmentState
     *            RuntimeEnvironmentState object to insert.
     */
    public void putRuntimeEnvironmentState(RuntimeEnvironmentState runtimeEnvironmentState);

    /**
     * Sets the default expiration value for all data added or updated after
     * this operation.
     * 
     * @param expirationSeconds
     *            expiration time in seconds
     */
    public void setDefaultDataExpirationSeconds(int expirationSeconds);

    /**
     * Attempts to get a cluster wide lock based on the unique lock name
     * provided.
     * 
     * @param lockName
     *            String name of the lock. Only one process in the cluster can
     *            get a lock with this name at a time.
     * @param expirationSeconds
     *            Number of seconds that the lock will last before automatically
     *            expiring. Normally the lock should be released using the
     *            unlock method but in the event of a severe failure, this
     *            ensures that the lock will automatically be released after a
     *            fixed amount of time.
     * @return true is the lock was obtained, false otherwise.
     */
    public boolean lock(String lockName, int expirationSeconds);
    

    /**
     * Releases a lock previously obtained with the lock method.
     * 
     * @param acquisitionPointIdentity
     *            Acquisition point identity of the object to retrieve.
     * @return AcquisitionPoint object with the specified identity or null if
     *         there is no element in the data store with the identity
     *         specified. A deep copy of the object is returned so no changes
     *         made to the returned object will affect the data store. To update
     *         the data store, the caller must call the appropriate put method
     *         explicitly.
     */
    public void unlock(String lockName);
    
    /**
     * Peeks into couchbase to see whether somebody has taken this lock.
     * 
     * @param lockName
     * @return
     */
    boolean isLockTaken(String lockName);
    
    /** 
     * persist blackout event json data for feed id, and the compilation time
     * @param feedId
     * @param blackoutEvents
     * @param compilationTime
     */
    public void putBlackoutEvents(String feedId, String blackoutEvents, long compilationTime);
    
    /** return the compilation time for the blackout event data
     * 
     * @param feedId
     * @return
     */
    public long getBlackoutEventCompilationTime (String feedId);

    /** get blackout event data for the feed id.
     *  
     * @param feedId
     * @return
     */
    public ArrayList<BlackoutEvent> getAllBlackoutEventsOnFeed(String feedId);
    
    /**
     * persist single blackout event json format data
     * signalID would be part of the key
     * @param signalID
     * @param event <code>BlackoutEvent<code>
     */
    void putSingleBlackoutEvent(final String signalId, final BlackoutEvent event);
    
    /**
     * get single blackout event json format data
     * 
     * @return
     */
    BlackoutEvent getSingleBlackoutEvent(final String signalId);

    /**
     * persist blackout event json format data
     * 
     * @param eventId
     * @param event <code>BlackoutEvent<code>
     */
    void putBlackoutEventByEventId(final String eventId,  final BlackoutEvent event);
    
    /**
     * get blackout event json format data
     * 
     * @return
     */
    BlackoutEvent getBlackoutEventByEventId(final String eventId);

    /**
     * set/update feed and its included acquisition point set
     * it will include feed name mapping to multiple acquisition points
     * for example:  feed1 - [acq1, acq2, acq3]   (acq - acquisition point)
     *               feed2 - [acq4, acq5, acq6]
     * @param feedid
     * @param AcquisitionPointSet
     */
    void putFeedToAcquistionMap(final String feedId, final Set<String> AcquisitionPointSet);
    
    /**
     * get feed ID related acquisition point set 
     * @param feedId
     * @return feedID covered acquisition points
     */
    Set<String> getFeedToAcquistionMap(final String feedId);

	void putActiveFeedList(String feedlist);

	String getActiveFeedList();

	/**
	 * Delete a Blackout Event from db
	 * @param event event to delete
	 */
	public void deleteBlackoutEvent(BlackoutEvent event);

	
	/**
	 * save a Blackout Event to db
	 * @param event event to delete
	 */
	void putBlackoutEvent(BlackoutEvent event);

	/**
	 * Used by real-time runtime updates from hosted to runtime.
	 * 
	 * @param feedName
	 * @param blackoutEvents
	 */
	void putBlackoutEvents(String feedName, String blackoutEvents);

	/**
	 * Saves status for realtime notifications sent.
	 * 
	 * @param eventId
	 * @param acquistionPointIdentity
	 * @param utcStartTime
	 * @param utcEndtime
	 * @param status
	 */
	void putRealtimeNotificationStatus(String eventId, String acquistionPointIdentity, long utcStartTime, long utcEndtime, String status);

	/**
	 * Retrieves status for realtime notifications sent.
	 * 
	 * @param eventId
	 * @param acquistionPointIdentity
	 * @param utcStartTime
	 * @param utcEndtime
	 * @return
	 */
	String getReatimeNotificationStatus(String eventId,	String acquistionPointIdentity, long utcStartTime, long utcEndtime);

	/**
	 * Put blackout event in couchbase.
	 * 
	 * @param eventId
	 * @param signalId
	 * @param event
	 */
	void putBlackoutEvent(String eventId, String signalId, BlackoutEvent event);

	public String getHostedNotificationStatus(String eventSignalId, String eventState);

	public void putHostedNotificationStatus(String eventSignalId, String eventState, String status);

	public void putInPointsSignal(String inPointSignalId, String confirmedCpoSignalId);

	String getInPointsSignal(String inPointSignalId);

	long getBlackoutMccStartTime(String acquisitionPointTd, String signalId);

	void putBlackoutMccStartTime(String acquisitionPointTd, String signalId,long startTime);
	
	void putConfirmedBlackout(ConfirmedPlacementOpportunity confirmedPlacementOpportunity,String signalId);
	
	/**
	 * Persist SegmentationDescriptor from SCC Request in JSON format to be further used in MCC Request.
	 * 
	 * @param signalId
	 * @param segmentationDescriptor
	 */
	public void putSegmentationDescriptor(String signalId, SegmentationDescriptor segmentationDescriptor);
	
	/**
	 * Retrieves SegmentationDescriptor on the basis of signalId
	 * @param signalId
	 * @return
	 */
	public SegmentationDescriptor getSegmentationDescriptor(String signalId);
	
    /**
     * Attempts to get a cluster wide lock based on the unique lock name
     * provided.
     * 
     * @param lockName
     *            String name of the lock. Only one process in the cluster can
     *            get a lock with this name at a time.
     * expirationSeconds: Uses Couchbase's default expiration time which is 30 seconds. Every lock must be unlocked and should not be left for automatic expiration.
     * @return <code>StringDocument</code> is the lock was obtained, false otherwise.
     */
    public StringDocument lock(String lockName);
    
    /**
     * Unlock the given Locked Document.
     * 
     * @param lockedDocument
     */
    public void unlock(final StringDocument lockedDocument);
    
    /**
     * put active data center
     */
    public void putActiveDataCenter(String dataCenter);
    
    /**
     * get active data center
     */
    public String getActiveDataCenter();
    
    /**
     * Returns the current server configuration.
     * 
     * @return
     */
	public ServerConfig getServerConfig();
	
	/**
	 * Tells whether the current server belongs to a active data center or not.
	 * 
	 * @return
	 */
	public boolean isServerInActiveDataCenter();

	public String get(String couchbaseSearchKey);

	public void set(String key, String value);

	public void delete(String key);

	public StringDocument getDocument(String key);
	/**
	 * Get the AcquisitionPoint List.
	 * @param ids
	 * @return
	 */
	
	public List<AcquisitionPoint> getAcquisitionPoints(final Collection<String> ids);

	void removeHostedNotificationStatus(String eventSignalId, String eventState);
	
	void putAPConfirmedSignal(String apId, String signalId, String signalResponses);

	SignalStateModel getLastConfirmedEvent(String apId);
	
	public void appendToQueue(String queueName, String queueElement, int expirationTime);
	
	public void appendToQueue(String queueName, String queueElement);
	
	public String popFromQueue(String queueName);

	public void unscheduleNotification(String upstreamNotificationQueueName, String eventSignalId);
	public void unscheduleNotificationByNotificationId(String queueName, String scheduledNotificationId) ;
	
	/**
	 * Note that this method should only be used if you are 100% sure of deletion/removal of the entire queue. Otherwise it is recommended to use the 
	 * <code>tv.blackarrow.cpp.managers.DataManager.unscheduleNotifications(String queueName, Set<String> notificationMessageRefs)</code> method.
	 * @param upstreamNotificationQueueName
	 */
	public void forcefullyUnscheduleAllNotifications(String upstreamNotificationQueueName);
	
	public long getQueueSize(final String queueName);

	void set(String key, String value, int expirationTime);
	
	/**
	 * Returns all the Queue times at which the notifications for this Media are scheduled.
	 * 
	 * @param mediaSignalId
	 * @return scheduledTimes
	 */
	public Set<Integer> getEventScheduledNotificationQueueTimes(String mediaSignalId);
	
	/**
	 * Saves all the Queue times at which the notifications for this Media are scheduled.
	 * 
	 * @param mediaSignalId
	 * @param scheduledTimes
	 */
	public void saveEventScheduledNotificationQueueTimes(String mediaSignalId, Set<Integer> scheduledTimes);

	void deleteEventScheduledNotificationQueueTimes(String eventSignalId);
	
	Set<String> getEventsDeletedSinceLastRulesLoading(long lastRuleLoadingTime, String feedId);

	void recordEventDeletionViaRealtimeUpdates(long lastRuleLoadingTime, String feedId, String eventSignalId);

	public String getNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType);

	public boolean caslock(String lockName, int locktime);

	public void putNotificationStatus(String eventId, String acquistionPointIdentity, SegmentType notificationType,
			String status);
	
	

}
