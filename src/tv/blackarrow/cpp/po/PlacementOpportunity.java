package tv.blackarrow.cpp.po;

public class PlacementOpportunity {
	private String poId;
	private String assetRef;
	private String provider;
	private int positionInBreak;
	private int duration;

	/**
	 * @return the poId
	 */
	public String getPoId() {
		return poId;
	}

	/**
	 * @param poId
	 *            the poId to set
	 */
	public void setPoId(String poId) {
		this.poId = poId;
	}

	public String getAssetRef() {
		return assetRef;
	}

	public void setAssetRef(String assetRef) {
		this.assetRef = assetRef;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	/**
	 * @return the positionInBreak
	 */
	public int getPositionInBreak() {
		return positionInBreak;
	}

	/**
	 * @param positionInBreak
	 *            the positionInBreak to set
	 */
	public void setPositionInBreak(int positionInBreak) {
		this.positionInBreak = positionInBreak;
	}

	/**
	 * @return the duration
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * @param duration
	 *            the duration to set
	 */
	public void setDuration(int duration) {
		this.duration = duration;
	}

}
