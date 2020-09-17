package tv.blackarrow.cpp.managers;

import java.util.Date;
import java.util.List;

import tv.blackarrow.cpp.exeptions.CppException;
import tv.blackarrow.cpp.po.PlacementOpportunity;
import tv.blackarrow.cpp.po.PoSchedule;

public interface PersistanceManager {

	/**
	 * Set schedule for the feedId
	 * @param feedId feedId for the schedule 
	 * @param schedule PoSchedule
	 * @return String response
	 * @throws CppException
	 */
	public String setPoSchedule(String feedId, PoSchedule schedule) throws CppException;

	/**
	 * Get PoSchedule for the feedId and scheduleId
	 * @param feedId feedId for the schedule 
	 * @param scheduleId scheduleId for the schedule
	 * @return PoSchedule schedule
	 * @throws CppException
	 */
	public PoSchedule getPoSchedule(String feedId, String scheduleId) throws CppException;

	/**
	 * Confirm event and return signalId
	 * @param feedId feedId from the acquisition  point
	 * @param eventTime Time at which the event is triggered
	 * @return String signalId
	 * @throws CppException
	 */
	public String confirmPoBreakEvent(String feedId, Date eventTime) throws CppException;

	/**
	 * Get confirmed PlacementOpportunities (from PoBreak)
	 * @param feedId feedId for the schedule 
	 * @param signalId confirmation signalId
	 * @return List List of PlacmentOpportunities
	 * @throws CppException
	 */
	public List<PlacementOpportunity> getConfirmedPosBySignalId(String feedId, String signalId) throws CppException;

}
