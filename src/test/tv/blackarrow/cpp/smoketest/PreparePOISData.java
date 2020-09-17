/**
 * 
 */
package test.tv.blackarrow.cpp.smoketest;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.model.BlackoutEvent;

/**
 * @author pzhang
 *
 */
public class PreparePOISData extends SmokeTestBase {
	
	private String toJSON(ArrayList<BlackoutEvent> events) {

			Gson gson = new Gson();
			
			// convert the object to json string
			String json = gson.toJson(events, new TypeToken<ArrayList<BlackoutEvent>>(){}.getType());
			return json;
		}
	
	private ArrayList<BlackoutEvent> prepareBlackoutEvents(String feed) {
		final int BLACKOUT_HOURS = 48;
		final int INTERVAL = 30*60*1000;
		
		long ctime = System.currentTimeMillis();
		ctime = (ctime / INTERVAL) * INTERVAL;  
		
		ArrayList<BlackoutEvent> list = new ArrayList<BlackoutEvent>();
		for (int i = 0; i < BLACKOUT_HOURS * 60 * 60* 1000 / INTERVAL; i ++) {
			long startTime = ctime + i * INTERVAL;
			String signalId = feed + startTime;
			BlackoutEvent event = new BlackoutEvent();
			event.setSignalId(signalId);
			event.setUtcStartTime(startTime);		// 
			event.setUtcStopTime(startTime+  INTERVAL);
			list.add(event);
		}
		
		return list;
	}
	
	private void writeBlackoutData(String feed) throws Exception {
		ArrayList<BlackoutEvent> events = prepareBlackoutEvents(feed);
		String json = toJSON(events);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String fileName = format.format(new Date())  + "_" + feed + "_blackout.json";
		File file = new File("/opt/blackarrow/ess/pois_repos/inprocess/" + feed + "/" + fileName);
		FileOutputStream ostream = new FileOutputStream(file);
		ostream.write(json.getBytes());
		ostream.close();
		
	}

	@Test
	public void testCopyPOISData() throws Exception {
		execute ("cp /opt/blackarrow/rulescore/linear/1/pois_repos/*.zip /opt/blackarrow/ess/pois_repos ");
	}
	
	@Test
	public void testPrepreBlackoutData() throws Exception {
		writeBlackoutData("NW08");
	}
	
	static public void main(String[] args) {
		
		PreparePOISData preparer = new PreparePOISData();
	
		try {
			preparer.writeBlackoutData("NW08");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
