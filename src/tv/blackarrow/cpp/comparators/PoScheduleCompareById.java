package tv.blackarrow.cpp.comparators;

import java.util.Comparator;

import tv.blackarrow.cpp.po.PoSchedule;

public class PoScheduleCompareById implements Comparator<PoSchedule> {

	@Override
	public int compare(final PoSchedule schedule1, final PoSchedule schedule2) {
		return schedule1.getScheduleId().compareTo(schedule2.getScheduleId());
	}

}
