/**
 * 
 */
package tv.blackarrow.cpp.model;


/**
 * @author akumar
 *
 */
public class ServerConfig{
	private String activeDataCenter = "";
	private long lastUpdated = -1;
	
	public ServerConfig() {
		super();
	}

	public ServerConfig(String activeDataCenter, long lastUpdated) {
		this();
		this.activeDataCenter = activeDataCenter;
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the activeDataCenter
	 */
	public String getActiveDataCenter() {
		return activeDataCenter;
	}

	/**
	 * @param activeDataCenter the activeDataCenter to set
	 */
	public void setActiveDataCenter(String activeDataCenter) {
		this.activeDataCenter = activeDataCenter;
	}

	/**
	 * @return the lastUpdated
	 */
	public long getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

}
