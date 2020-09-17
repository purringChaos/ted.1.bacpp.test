package tv.blackarrow.cpp.pretrigger.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import tv.blackarrow.cpp.pretrigger.beans.PretriggerSettingBean;
import tv.blackarrow.cpp.pretrigger.model.Media;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import static tv.blackarrow.cpp.pretrigger.jobs.BisQueryTimeTask.MONITOR_BEAN;
/**
 * 
 * the class will parse json formatted schedule
 * and generated a list that will be used to cache and scheduling 
 *
 */
public class HintScheduleHandler {
	private static final Logger LOG = LogManager.getLogger(HintScheduleHandler.class);
	private static final HintScheduleHandler INSTANCE = new HintScheduleHandler();
	private Timer queryTimer = new Timer(false);
	
	private HintScheduleHandler() {}
	
	public static HintScheduleHandler getInstance() {
		return INSTANCE;
	}
	
	/**
	 * expected the hint schedule in json format
	 * @param jsonSchedule
	 * @return a list of hint schedules
	 */
	public List<Media> parseHintSchedule(String jsonSchedule) {
		List<Media> mediaList = new ArrayList<Media>();
		try {
			Type listType = new TypeToken<ArrayList<Media>>(){}.getType();
			mediaList = new Gson().fromJson(jsonSchedule, listType);
			if(mediaList != null){
			  LOG.info("media list size = " + mediaList.size());
			}
		} catch(Exception ex) {
			LOG.warn(ex.getMessage() + "-->" + jsonSchedule);
		}
		
		return mediaList;
	}
	
	
	/**
	 * get the hint schedules from the remote BIS server
	 * @return schedule in json format
	 */
	public String obtainHintSchedule(String url) {
		String data = "";

		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);

		// Provide custom retry handler is necessary
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
				new DefaultHttpMethodRetryHandler(PretriggerSettingBean.getInstance().getMaxRetry(), false));
		method.getParams().setParameter("http.socket.timeout", 
				PretriggerSettingBean.getInstance().getBisQueryTimeout());
		
		int retry = PretriggerSettingBean.getInstance().getMaxRetry();
        try {
          for (int i = 0; i < retry; i++) {
            try {
              MONITOR_BEAN.setLastAttemptCallToBis(System.currentTimeMillis());
              // Execute the method.
              int statusCode = client.executeMethod(method);
    
              if (statusCode != HttpStatus.SC_OK) {
                LOG.error("Method failed: " + method.getStatusLine());
              }
    
              // Read the response body.
              byte[] responseBody = method.getResponseBody();
              //PRI-6108 /once we get successful status, break the for loop..
              if (statusCode == HttpStatus.SC_OK) {
					LOG.info("BIS call successful in "+ (i+1) + " attempts ");
					//PRODISSUE-1499: Moved the monitoring set only after the response is successful
					//Success attempt shouldn't be updated if the HTTP response is 500.
					MONITOR_BEAN.setLastSuccessCallToBis(System.currentTimeMillis());
					data = new String(responseBody);

 					break;
			  }
            } catch (Exception e) {
              LOG.error("Fatal protocol violation: " + e.getMessage() + " ==> " + url, e);
              waitAndTry(i,  retry);
            }
          }
        }catch (Exception e) {
          LOG.error("Fatal protocol violation: " + e.getMessage() + " ==> " + url, e);
        } finally {
          // Release the connection.
          method.releaseConnection();
        }
		return data;
	}
	
  private void waitAndTry(int i, int retry) {
    if (i == retry - 1) {
      LOG.error("Bis Query retried for "
          + retry + " times");
    }else{
      try {
        Thread.sleep(10);
      } catch (Exception e1) {
        LOG.error("Error in waitAndTry: ",e1);
      }
    }
  }

	public Timer getQueryTimer() {
		return queryTimer;
	}

	/*		
	public static void main(String[] args) {
		String content = HintScheduleHandler.getInstance().obtainHintSchedule("http://localhost/schedule.json");
		List<Media> mediaList = HintScheduleHandler.getInstance().parseHintSchedule(content);	
		System.out.println(mediaList.size());
		for(Media media : mediaList) {
			System.out.println(media);
		}
	} */
	
}
