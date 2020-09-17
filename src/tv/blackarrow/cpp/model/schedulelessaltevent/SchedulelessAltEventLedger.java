/**
 *
 */
package tv.blackarrow.cpp.model.schedulelessaltevent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

import tv.blackarrow.cpp.components.util.ContextConstants.ESSRequestType;
import tv.blackarrow.cpp.managers.SCTE224DataManagerImplV1;
import tv.blackarrow.cpp.utils.SegmentType;

/**

 *
 */
public final class SchedulelessAltEventLedger {

	@Expose
	private String acquisitionPointIdentity = null;

	@Expose
	private boolean isAltEventStarted = false;
	@Expose
	private boolean isAltEventEnded = false;

	@Expose
	private String cadentSignalId;

	@Expose
	private List<SchedulelessAltEventTransaction> altEventTransactions = new LinkedList<SchedulelessAltEventTransaction>();

	public static SchedulelessAltEventLedger getEmptyLedger(String acquisitionPointIdentity) {
		return new SchedulelessAltEventLedger(acquisitionPointIdentity);
	}

	/**
	 *
	 */
	public SchedulelessAltEventLedger() {
		super();
	}

	/**
	 * @param acquisitionPointIdentity
	 */
	private SchedulelessAltEventLedger(String acquisitionPointIdentity) {
		this();
		this.acquisitionPointIdentity = acquisitionPointIdentity;
	}

	/**
	 * @return the acquisitionPointIdentity
	 */
	public String getAcquisitionPointIdentity() {
		return acquisitionPointIdentity;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("SchedulelessAltEventLedger [acquisitionPointIdentity=%s, isAltEventStarted=%s, isAltEventEnded=%s, cadentSignalId=%s ]", acquisitionPointIdentity, isAltEventStarted,
				isAltEventEnded, cadentSignalId);
	}

	public static void main(String[] args) {
		//Test JSON
		SchedulelessAltEventLedger altEventLedger = SchedulelessAltEventLedger.getEmptyLedger("Test AP");
		altEventLedger.setAltEventStarted(true);
		altEventLedger.setAltEventEnded(true);
		System.out.println(SCTE224DataManagerImplV1.GSON.toJson(altEventLedger));
		System.out.println(SCTE224DataManagerImplV1.GSON.fromJson(SCTE224DataManagerImplV1.GSON.toJson(altEventLedger), SchedulelessAltEventLedger.class));
	}

	public boolean isAltEventStarted() {
		return isAltEventStarted;
	}

	public void setAltEventStarted(boolean isAltEventStarted) {
		if (!this.isAltEventStarted) {
			this.isAltEventStarted = isAltEventStarted;
		}
	}

	public boolean isAltEventEnded() {
		return isAltEventEnded;
	}

	public void setAltEventEnded(boolean isAltEventEnded) {
		if (!this.isAltEventEnded) {
			this.isAltEventEnded = isAltEventEnded;
		}
	}

	/**
	 * Returns a read-only list of SignalTransaction that has been registered in to this ledger.
	 *
	 * @return the altEventTransactions
	 */
	public List<SchedulelessAltEventTransaction> getSignalTransactions() {
		return Collections.unmodifiableList(altEventTransactions);
	}

	/**
	 * @param altEventTransaction
	 */
	public SchedulelessAltEventLedger addSignalTransaction(SchedulelessAltEventTransaction altEventTransaction) {
		if (altEventTransaction != null) {
			this.altEventTransactions.add(altEventTransaction);
			Short signalSegmentTypeId = altEventTransaction.getSignalSegmentTypeId();
			if (signalSegmentTypeId != null) {
				this.setAltEventStarted(SegmentType.isProgramStartSignal(signalSegmentTypeId));
				this.setAltEventEnded(SegmentType.isProgramEndSignal(signalSegmentTypeId));
			}
		}
		return this;
	}

	public SchedulelessAltEventTransaction getProgramStartSignalTransaction(ESSRequestType searchInSavedEssRequestType) {
		if ((getSignalTransactions() != null) && !getSignalTransactions().isEmpty()) {
			for (SchedulelessAltEventTransaction transaction : getSignalTransactions()) {
				if ((SegmentType.PROGRAM_START.getSegmentTypeId() == transaction.getSignalSegmentTypeId()) && searchInSavedEssRequestType.equals(transaction.getEssRequestType())) {
					return transaction;
				}
			}
		}
		return null;
	}

	public String getCadentSignalId() {
		return cadentSignalId;
	}

	public void setCadentSignalId(String cadentSignalId) {
		this.cadentSignalId = cadentSignalId;
	}


	/**
	 * Returns if this Scheduless Alt event Ledger has an entry for SegmentType.PROGRAM_END  transaction.
	 *
	 * @param searchInSavedEssRequestType whether to search in SCC or MCC transactions.
	 * @return
	 */
	public SchedulelessAltEventTransaction getProgramEndSignalTransaction(ESSRequestType searchInSavedEssRequestType) {
		if (getSignalTransactions() != null && isAltEventEnded && !getSignalTransactions().isEmpty()) {
			for (SchedulelessAltEventTransaction transaction : getSignalTransactions()) {
				if ((SegmentType.PROGRAM_END.getSegmentTypeId() == transaction.getSignalSegmentTypeId()) && searchInSavedEssRequestType.equals(transaction.getEssRequestType())) {
					return transaction;
				}
			}
		}
		return null;
	}


}
