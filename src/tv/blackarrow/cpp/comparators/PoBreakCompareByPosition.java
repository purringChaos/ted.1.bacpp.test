package tv.blackarrow.cpp.comparators;

import java.util.Comparator;

import tv.blackarrow.cpp.po.PoBreak;

public class PoBreakCompareByPosition implements Comparator<PoBreak> {

	@Override
	public int compare(PoBreak break1, PoBreak break2) {
		return ((Integer)break1.getPositionInWindow()).compareTo(((Integer)break2.getPositionInWindow()));
	}

}
