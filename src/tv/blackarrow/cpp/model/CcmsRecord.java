package tv.blackarrow.cpp.model;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import tv.blackarrow.cpp.utils.ThreadSafeSimpleDateFormat;

public class CcmsRecord {
	private String record;
	private int year;
	public static final ThreadSafeSimpleDateFormat DATETIME_FORMAT = new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * a sample record
	 * LOI 1227 002900 0014 0030 001 001 000030 000000 00000000 000 00000039452 0000 CANNON & DUNPHY                  CDIM 30 1191 1TV     Fill
	 * @param record
	 */
	public CcmsRecord(String record, int year) {
		this.record = record;
		this.year = year;
		
		if(record.length() < 135) {
			throw new RuntimeException("wrong record : " + record);
		}
	}

	public String getProvider() {
		return record.substring(78, 110).trim();
	}
	
	public String getAssetRef() {
		return record.substring(111, 131).trim();
	}

	public String getWindowStartTimeStr() {
		return record.substring(16, 21);
	}
	
	public Date getScheduleDateTime() throws ParseException {
		return DATETIME_FORMAT.parse(year + record.substring(4,15).replace(" ", ""));
	}
	
	public Date getWindowStartTime() throws ParseException {
		String datetime = new StringBuilder().append(year).append(record.substring(4,8)).
						append(record.substring(16,20)).append("00").toString();
		return DATETIME_FORMAT.parse(datetime);
	}

	/**
	 * get Window end time
	 * @return
	 * @throws ParseException
	 */
	public Date getWindowEndTime() throws ParseException {
		int hours = Integer.valueOf(record.substring(21, 23));
		int minutes = Integer.valueOf(record.substring(23, 25));
		int totalMinutes = hours * 60 + minutes;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(getWindowStartTime());
		cal.add(Calendar.MINUTE, totalMinutes);
		
		return cal.getTime();
	}
	
	public int getBreakNumber() {
		return Integer.valueOf(record.substring(26, 29));
	}

	public int getPositionNumber() {
		return Integer.valueOf(record.substring(30, 33));
	}
	
	public int getSpotLength() {
		return Integer.valueOf(record.substring(34, 40));
	}
	
}
