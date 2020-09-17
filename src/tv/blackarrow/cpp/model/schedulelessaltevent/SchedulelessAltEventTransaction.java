package tv.blackarrow.cpp.model.schedulelessaltevent;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;

public class SchedulelessAltEventTransaction {

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

	/**
	 * Factory Method to create the SchedulelessAltEventTransaction Transaction. This objects is used to keep a history of all the transactions that happened on an
	 * acquisition point related to a SchedulelessAltEventTransaction.
	 *
	 * @param signalTimeInMS the UTC time that came in the request.
	 * @param signalSegmentTypeId the segment id that was received in the request.
	 * @param totalDurationInMS total duration if any at the time of this signal.
	 * @param originalUpid the original UPID received within that signal if any.
	 * @param essRequestType the request type or transaction type, it can have one of the following two values: SCC, MCC
	 * @return the SchedulelessAltEventTransaction Transaction record ready to be added into the SchedulelessAltEventTransaction Ledger.
	 */
	public static SchedulelessAltEventTransaction build(final Long signalTimeInMS, final Short signalSegmentTypeId,
			final Long totalDurationInMS, final String originalUpid, final ESSRequestType essRequestType) {
		return new SchedulelessAltEventTransaction(signalTimeInMS, signalSegmentTypeId, totalDurationInMS, originalUpid, essRequestType);
	}

	public static SchedulelessAltEventTransaction getEmptyTransaction() {
		return new SchedulelessAltEventTransaction();
	}

	/**
	 * @param signalTimeInMS
	 * @param signalSegmentTypeId
	 * @param totalDurationInMS
	 * @param originalUpid
	 */
	private SchedulelessAltEventTransaction(Long signalTimeInMS, Short signalSegmentTypeId, Long totalDurationInMS,
			String originalUpid, ESSRequestType essRequestType) {
		this();
		this.signalTimeInMS = signalTimeInMS;
		this.signalSegmentTypeId = signalSegmentTypeId;
		this.totalDurationInMS = totalDurationInMS;
		this.originalUpid = originalUpid;
		this.essRequestType = essRequestType;
	}

	private SchedulelessAltEventTransaction() {
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
		return String.format("SchedulelessAltEventTransaction [signalTimeInMS=%s, signalSegmentTypeId=%s, totalDurationInMS=%s, originalUpid=%s, essRequestType=%s]", signalTimeInMS, signalSegmentTypeId,
				totalDurationInMS, originalUpid, essRequestType);
	}
}
