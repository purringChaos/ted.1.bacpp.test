package tv.blackarrow.cpp.comparators;

import java.util.Comparator;

import tv.blackarrow.cpp.po.PlacementOpportunity;

public class PlacementOpportunityCompareByPosition implements Comparator<PlacementOpportunity> {

	@Override
	public int compare(PlacementOpportunity spot1, PlacementOpportunity spot2) {
		return ((Integer)spot1.getPositionInBreak()).compareTo(((Integer)spot2.getPositionInBreak()));
	}

}
