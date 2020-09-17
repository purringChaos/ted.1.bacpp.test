/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.utils.SegmentType;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class MediaLedger {
	private static final Logger LOGGER = LogManager.getLogger(MediaLedger.class);

	@Expose
	private String acquisitionPointIdentity = null;
	@Expose
	//This field should be set as soon as we notify either as OOB upstream or respond to Inband-SPE
	private boolean isMediaStartNotificationSent = false;
	@Expose
	//This field should be set as soon as we notify either as OOB upstream or respond to Inband-SPE
	private boolean isMediaEndNotificationSent = false;
	@Expose
	private List<MediaTransaction> mediaTransactions = new LinkedList<MediaTransaction>();
	@Expose
	private HashMap<SegmentType, String> acquisitionSignalIds = new LinkedHashMap<>();
	@Expose
	private boolean isExecutedAtEncoderLevel = false;
	@Expose
	private String signalId = null;	
	@Expose
	private Integer territoryUpdateCounter = 0;
	/**
	 * Factory method to build the new MediaLedger instance.
	 * 
	 * @param acquisitionPointIdentity
	 * @return
	 */
	public static MediaLedger build(final String acquisitionPointIdentity, final boolean isExecutedAtEncoderLevel) {
		return new MediaLedger(acquisitionPointIdentity, isExecutedAtEncoderLevel);
	}

	public static MediaLedger getEmptyLedger() {
		return new MediaLedger();
	}

	/**
	 * 
	 */
	private MediaLedger() {
		super();
	}

	/**
	 * @param acquisitionPointIdentity
	 */
	private MediaLedger(final String acquisitionPointIdentity, final boolean isExecutedAtEncoderLevel) {
		this();
		this.acquisitionPointIdentity = acquisitionPointIdentity;
		this.isExecutedAtEncoderLevel = isExecutedAtEncoderLevel;
	}

	/**
	 * @return the acquisitionPointIdentity
	 */
	public String getAcquisitionPointIdentity() {
		return acquisitionPointIdentity;
	}

	/**
	 * 	//This field will be set as soon as we notify either as OOB upstream or respond to Inband-SPE
	 * @param isMediaStartNotificationSent the isMediaStartNotificationSent to set
	 */
	public void setMediaStartNotificationSent(boolean isMediaStartNotificationSent) {
		this.isMediaStartNotificationSent = isMediaStartNotificationSent;
	}

	/**
	 * 	//This field should be set as soon as we notify either as OOB upstream or respond to Inband-SPE
	 * @param isMediaEndNotificationSent the isMediaEndNotificationSent to set
	 */
	public void setMediaEndNotificationSent(boolean isMediaEndNotificationSent) {
		this.isMediaEndNotificationSent = isMediaEndNotificationSent;
	}

	/**
	 * Returns a read-only list of Media Transactions that has been registered in to this ledger.
	 * 
	 * @return the mediaTransactions
	 */
	public List<MediaTransaction> getMediaTransactions() {
		return Collections.unmodifiableList(mediaTransactions);
	}

	/**
	 * This gets saved either from Inband/OOB SCC flow
	 * @param mediaTransaction
	 * @param matchedMediaPoint 
	 */
	public MediaLedger addMediaTransaction(MediaTransaction mediaTransaction, SegmentType contentIDResultedSegmentResult) {

		if (mediaTransaction != null) {
			mediaTransaction.setResultedSignalSegmentTypeId(contentIDResultedSegmentResult != null ? contentIDResultedSegmentResult.getSegmentTypeId() : null);
			this.mediaTransactions.add(mediaTransaction);
			if (this.mediaTransactions != null && this.mediaTransactions.size() > 2) {
				LOGGER.info(() -> acquisitionPointIdentity + ":" + signalId + "(received more than two transaction)->" + mediaTransaction.getSignalTimeInMS() + "(SegmentTypeId="
						+ mediaTransaction.getSignalSegmentTypeId() + ")");
			}
		}
		return this;
	}

	public SegmentType getResultedReponseSignalForContentID(MediaPoint matchedMediaPoint, Short signalSegmentTypeId) {
		SegmentType resultedSegmentType = null;
		if (matchedMediaPoint != null && SegmentType.CONTENT_IDENTIFICATION.getSegmentTypeId() == signalSegmentTypeId) {
			List<Short> segmentationTypeIdInMatchSignal = matchedMediaPoint.getMatchSignal().getSegmentationTypeIds();
			if (segmentationTypeIdInMatchSignal.contains(SegmentType.PROGRAM_START.getSegmentTypeId()) || segmentationTypeIdInMatchSignal.contains(SegmentType.PROGRAM_OVERLAP_START.getSegmentTypeId()) || matchedMediaPoint.getOrder() == 1) {
				resultedSegmentType = SegmentType.PROGRAM_START;
			} else if (segmentationTypeIdInMatchSignal.contains(SegmentType.PROGRAM_END.getSegmentTypeId()) || segmentationTypeIdInMatchSignal.contains(SegmentType.PROGRAM_EARLY_TERMINATION.getSegmentTypeId()) || matchedMediaPoint.getOrder() == 2) {
				resultedSegmentType = SegmentType.PROGRAM_END;
			}
		}
		return resultedSegmentType;
	}

	/**
	 * 
	 * @return whether this Media has already received a program Overlap/Start Signal.
	 */
	public boolean isMediaStarted() {
		if (this.isMediaStartNotificationSent) {
			//Only in case of XMP this condition makes sense, where the Program Start notification would have had gone but media not Started.
			//However, below condition could be used generically.
			MediaTransaction startMediaTransaction = getProgramStartOrOverLapMediaTransaction(ESSRequestType.SCC);
			return System.currentTimeMillis() > startMediaTransaction.getSignalTimeInMS();//Make Sure Current Time is after we sent/responded SPN.			
		}
		return false;
	}

	/**
	 * 
	 * @return whether this Media has already received a program End/Early Termination Signal.
	 */
	public boolean isMediaEnded() {
		if (this.isMediaEndNotificationSent) {
			//Only in case of XMP this condition makes sense, where the Program End notification would have had gone but media not ended.
			//However, below condition could be used generically.
			MediaTransaction endMediaTransaction = getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType.SCC);
			if (endMediaTransaction != null) {
				return System.currentTimeMillis() > endMediaTransaction.getSignalTimeInMS();
			}
		}
		return false;
	}

	/**
	 * @param segmentType
	 * @param acquisitionSignalId
	 */
	public void addAcquisitionSignalId(final SegmentType segmentType, final String acquisitionSignalId) {
		if (acquisitionSignalId == null) {
			throw new IllegalArgumentException("\"acquisitionSignalId\" can not be null. Please provide a valid argument value.");
		}
		if (!acquisitionSignalIds.containsKey(segmentType)) {
			acquisitionSignalIds.put(segmentType, acquisitionSignalId);
		}
	}


	/**
	 * @return the acquisitionSignalIds
	 */
	public Map<SegmentType, String> getAcquisitionSignalIds() {
		return Collections.unmodifiableMap(acquisitionSignalIds);
	}

	
	/**
	 * Returns if this media Ledger has an entry for SegmentType.PROGRAM_START or SegmentType.PROGRAM_OVERLAP_START transaction.
	 * 
	 * @param searchInSavedEssRequestType whether to search in SCC or MCC transactions.
	 * @return
	 */
	public MediaTransaction getProgramStartOrOverLapMediaTransaction(ESSRequestType searchInSavedEssRequestType) {
		if (getMediaTransactions() != null && !getMediaTransactions().isEmpty()) {
			for (MediaTransaction transaction : getMediaTransactions()) {
				if ((SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId() || SegmentType.PROGRAM_OVERLAP_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId()
						|| (transaction.getResultedSignalSegmentTypeId() != null
								&& (SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId() || SegmentType.PROGRAM_OVERLAP_START.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId())))
						&& searchInSavedEssRequestType.equals(transaction.getEssRequestType())) {
					//Return the first program start or Overlap Media transaction.
					return transaction;
				}
			}
		}
		return null;
	}

	/**
	 * Returns if this media Ledger has an entry for SegmentType.PROGRAM_END or SegmentType.PROGRAM_EARLY_TERMINATION transaction.
	 * 
	 * @param searchInSavedEssRequestType whether to search in SCC or MCC transactions.
	 * @return
	 */
	public MediaTransaction getProgramEndOrEarlyTerminationMediaTransaction(ESSRequestType searchInSavedEssRequestType) {
		MediaTransaction programEndOrEarlyTerminationMediaTransaction = null;
		if (getMediaTransactions() != null && !getMediaTransactions().isEmpty()) {
			for (MediaTransaction transaction : getMediaTransactions()) {
				if ((SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getSignalSegmentTypeId() || SegmentType.PROGRAM_EARLY_TERMINATION.getSegmentTypeId() == transaction.getSignalSegmentTypeId()
						|| (transaction.getResultedSignalSegmentTypeId() != null
								&& (SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId() || SegmentType.PROGRAM_EARLY_TERMINATION.getSegmentTypeId() == transaction.getResultedSignalSegmentTypeId())))
						&& searchInSavedEssRequestType.equals(transaction.getEssRequestType())) {
					programEndOrEarlyTerminationMediaTransaction = transaction;
					break;
				}
			}
		}
		//We always return the last Program End Signal Sent on this Media. For encoder level this could be one at the time of 
		//Program Start and one later at the time of end time update or delete Media operation.
		return programEndOrEarlyTerminationMediaTransaction;
	}

	/**
	 * @return the isExecutedAtEncoderLevel
	 */
	public boolean isExecutedAtEncoderLevel() {
		return isExecutedAtEncoderLevel;
	}

	public String getSignalId() {
		return signalId;
	}

	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	public boolean isMediaStartNotificationSent() {
		return isMediaStartNotificationSent;
	}

	public boolean isMediaEndNotificationSent() {
		return isMediaEndNotificationSent;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MediaLedger [acquisitionPointIdentity=" + acquisitionPointIdentity + ", isMediaStartNotificationSent="
				+ isMediaStartNotificationSent + ", isMediaEndNotificationSent=" + isMediaEndNotificationSent
				+ ", mediaTransactions=" + mediaTransactions + ", acquisitionSignalIds=" + acquisitionSignalIds
				+ ", isExecutedAtEncoderLevel=" + isExecutedAtEncoderLevel + ", signalId=" + signalId
				+ ", territoryUpdateCounter=" + territoryUpdateCounter + "]";
	}
	
	/**
	 * @return the territoryUpdateCounter
	 */
	public Integer getTerritoryUpdateCounter() {
		return territoryUpdateCounter;
	}


	/**
	 * @param territoryUpdateCounter the audienceIncrement to increment by 1
	 */
	public void incrementTerritoryUpdateCounter() {
		if (territoryUpdateCounter == null) {
			territoryUpdateCounter = 1;
		} else {
			territoryUpdateCounter += 1;
		}
	}

	public String  getSignalIdWithTerritoryCounter(String id) {
		if(this.getTerritoryUpdateCounter() > 0)
		{
			if (signalId == null) {
				return id + "_"+ String.format("%03d", this.getTerritoryUpdateCounter());
			} else {
				return signalId + "_"+ String.format("%03d", this.getTerritoryUpdateCounter());
			}		
		}
		
		return id;
	}
	
}
