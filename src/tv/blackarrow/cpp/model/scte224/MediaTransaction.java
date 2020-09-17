/**
 * 
 */
package tv.blackarrow.cpp.model.scte224;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;

/**
 * @author Amit Kumar Sharma
 *
 */
public final class MediaTransaction {

	@Expose
	private Long signalTimeInMS = null;
	@Expose
	private Short signalSegmentTypeId = null;
	@Expose
	private Long totalDurationInMS = null;//Leave this duration null when not setting. Do not set negative or 0 value here. The null option will be used while performing businessLogic
	@Expose
	private String originalUpid = null;
	@Expose
	private ESSRequestType essRequestType = null;
	
	@Expose
	private Short resultedSignalSegmentTypeId = null;//This will be used to tell, the content id resulted in program start/end

	/**
	 * Factory Method to create the Media Transaction. This objects is used to keep a history of all the transactions that happened on an 
	 * acquisition point related to a media.
	 * 
	 * @param signalTimeInMS the UTC time that came in the request.
	 * @param signalSegmentTypeId the segment id that was received in the request.
	 * @param totalDurationInMS total duration if any at the time of this signal.
	 * @param originalUpid the original UPID received within that signal if any.
	 * @param essRequestType the request type or transaction type, it can have one of the following two values: SCC, MCC
	 * @return the Media Transaction record ready to be added into the Media Ledger.
	 */
	public static MediaTransaction build(final Long signalTimeInMS, final Short signalSegmentTypeId,
			final Long totalDurationInMS, final String originalUpid, final ESSRequestType essRequestType) {
		return new MediaTransaction(signalTimeInMS, signalSegmentTypeId, totalDurationInMS, originalUpid, essRequestType);
	}
	
	public static MediaTransaction getEmptyTransaction() {
		return new MediaTransaction();
	}

	/**
	 * @param signalTimeInMS
	 * @param signalSegmentTypeId
	 * @param totalDurationInMS
	 * @param originalUpid
	 */
	private MediaTransaction(final Long signalTimeInMS, final Short signalSegmentTypeId, 
			final Long totalDurationInMS, final String originalUpid, final ESSRequestType essRequestType) {
		this();
		this.signalTimeInMS = signalTimeInMS;
		this.signalSegmentTypeId = signalSegmentTypeId;
		this.totalDurationInMS = totalDurationInMS;
		this.originalUpid = originalUpid;
		this.essRequestType = essRequestType;
	}

	private MediaTransaction() {
		super();
	}

	/**
	 * @return the signalTimeInMS
	 */
	public Long getSignalTimeInMS() {
		return signalTimeInMS;
	}

	/**
	 * @return the totalDurationInMS
	 */
	public Long getTotalDurationInMS() {
		return totalDurationInMS;
	}

	/**
	 * @return the signalSegmentTypeId
	 */
	public Short getSignalSegmentTypeId() {
		return signalSegmentTypeId;
	}

	/**
	 * 
	 * @return the originalUpid
	 */
	public String getOriginalUpid() {
		return originalUpid;
	}

	/**
	 * @return the essRequestType
	 */
	public ESSRequestType getEssRequestType() {
		return essRequestType;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("MediaTransaction [signalTimeInMS=%s, signalSegmentTypeId=%s,totalDurationInMS=%s, originalUpid=%s, essRequestType=%s, resultedSignalSegmentTypeId=%s]", signalTimeInMS, signalSegmentTypeId, 
				totalDurationInMS, originalUpid, essRequestType,resultedSignalSegmentTypeId);
	}

	public Short getResultedSignalSegmentTypeId() {
		return resultedSignalSegmentTypeId;
	}

	public void setResultedSignalSegmentTypeId(Short resultedSignalSegmentTypeId) {
		this.resultedSignalSegmentTypeId = resultedSignalSegmentTypeId;
	}
}
