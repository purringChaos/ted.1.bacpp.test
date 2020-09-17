package test.tv.blackarrow.cpp.smoketest;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;

import tv.blackarrow.ds.common.util.XMLUtil;

public class SmokeTestBase {
	
	static final int TIME_15_MINUTE = 15*60*1000;		// 15 minutes
	
	public String post(String url, HashMap<String, String> params) {
		return XMLUtil.processXML(url, params);
	}
	
	public String post(String url, String message) throws Exception {
		return XMLUtil.postXML(url, message);
	}
	
	/** return current request time in yyyy-MM-dd'T'HH:mm:ss.SSS'Z'*/
	public String getCurrentRequestTime() {
		
		return getTimeStamp(0, TIME_15_MINUTE, 0);		
	}

	public String getNextHour(int offset) {
		return getTimeStamp(0, 60*60*1000, offset);		
	}
	/** return current request time plus offset in yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
	 * @param offset: offset from current time.
	 * */
	public String getCurrentRequestTime(int offset) {
		
		return getTimeStamp(0, TIME_15_MINUTE, offset);		
	}

	
	/** returns time stamp at the next n-th interval, */
	public String getTimeStamp(int n, int interval, int offset) {
		long currentTime = System.currentTimeMillis();
		
		long next = (currentTime + interval - 1)/interval * interval + n * interval + offset;		// set time to next 15:00 
		
		Date date = new Date(next);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String dateStr = format.format(date);
		
		return dateStr;		
	}
	
	/** execute the given system command 
	 * @throws IOException */
	public void execute(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
		
        p.waitFor();
        
        int exit = p.exitValue();
        if (exit != 0)  {
        	InputStream err = p.getErrorStream();
        	String message = IOUtils.toString(err);
        	throw new Exception("failed to execute " + cmd + "\n" + message);
        }
	}
	
	public void copyFile(String from, String to) throws Exception {
		execute("cp " + from + " " + to);
	}
}
