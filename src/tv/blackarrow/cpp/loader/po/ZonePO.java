package tv.blackarrow.cpp.loader.po;

import tv.blackarrow.cpp.model.PlacementOpportunity;

public class ZonePO {

    private String zoneExtRef;
    
    private PlacementOpportunity po;

    public String getZoneExtRef() {
		return zoneExtRef;
	}
	public void setZoneExtRef(String zoneExtRef) {
		this.zoneExtRef = zoneExtRef;
	}
	public String getBreakID() {
		String breakId = null;
		if (po != null) {
			breakId = po.getPOKey();
		}
		return breakId;
	}
	public PlacementOpportunity getPo() {
		return po;
	}
	public void setPo(PlacementOpportunity po) {
		this.po = po;
	}
	public PlacementOpportunity getPlacementOpportunity() {
		return po;
	}
}
