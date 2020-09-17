package tv.blackarrow.cpp.components.admin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.utils.CppConstants;

public class AdminUtil {

	private static final Logger LOGGER = LogManager.getLogger(AdminUtil.class);
	
	public static final String CLUSTER_USERNAME_PARAM = "user";
	public static final String CLUSTER_PASSWORD_PARAM = "password";
	

	protected static HttpClient m_client = null;
	static {
		MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
		mgr.getParams().setDefaultMaxConnectionsPerHost(100);
		mgr.getParams().setMaxTotalConnections(100);
		m_client = new HttpClient(mgr);
	}
	
	public static String makeHttpCall(String url, String action, String authUser, String authPass, Map<String,String> params) {
    	if (authUser != null && authUser.length() > 0) {
    		// basic authentication needed
            Credentials credentials = new UsernamePasswordCredentials(authUser, authPass);
            try {
				HttpURL httpUrl = new HttpURL(url);
				String host = httpUrl.getHost();
				int port = httpUrl.getPort();
	            AuthScope authScope =new AuthScope(host, port);
	            HttpState state = m_client.getState();
	            state.setCredentials(authScope, credentials);
			} catch (URIException e) {
				LOGGER.error(()->"Could not parse the Host and Port from the passed in url: " + url,e);
			}
    	}
    	
        m_client.getParams().setParameter("http.useragent", "BlackArrow CPP");
        
        HttpMethod method = null;
        
        if ("GET".equalsIgnoreCase(action)) {
        	// GET case, we assume all the parameters are embedded in the URL already
        	method = new GetMethod(url);
        }
        else if ("POST".equalsIgnoreCase(action)) {
        	// POST case, add all the parameters here
        	method = new PostMethod(url);
            if (params != null && params.size() > 0) {
            	for (Map.Entry<String,String> entry : params.entrySet()) {
                    ((PostMethod)method).addParameter(entry.getKey(), entry.getValue());   
                }
            }
        }
        
		StringBuilder sb = new StringBuilder();
		
		BufferedReader br = null;
		try{
			int returnCode = m_client.executeMethod(method);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				LOGGER.error(()->"The Http method" + action + " is not implemented by this URI");
				// still consume the response body
				sb.append(method.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine;
				while(((readLine = br.readLine()) != null)) {
					sb.append(readLine);
				}
			}
		  } catch (Exception e) {
			  LOGGER.error(()->"Exception in executing the url: " + url,e);
		  } finally {
		    method.releaseConnection();
		    if(br != null) try { br.close(); } catch (Exception fe) {}
		  }
		  
		  return sb.toString();
	}

	public static Map<String,String> getURLParameters(String url) {
		if (url == null || url.length() == 0 || url.indexOf('?') == -1) {
			return null; 
		}
		return getParamsFromQueryString(url.substring(url.indexOf('?') + 1));
	}
	
	public static Map<String, String> getParamsFromQueryString(String queryString) {
		if (queryString == null) return null;
		
        HashMap<String,String> params = new HashMap<String,String>();

        String[] vals = queryString.split("&");
        for( int i = 0; i < vals.length; i++){
        	String[] p = vals[i].split("=");
        	if( p.length == 2 ){
	        	try {
					params.put(p[0], URLDecoder.decode(p[1],"UTF-8") );
				} catch (UnsupportedEncodingException e) {
					LOGGER.error(()->"The parameter value encoding is not supported: " + p[1],e);
				}
        	}        	
        }
		return params;
	}  
	
	public static String getMapEntryAsString(Map<String,String> params) {
		StringBuilder sb = new StringBuilder();
		
        if (params != null && params.size() > 0) {
        	for (Map.Entry<String,String> entry : params.entrySet()) {
        		sb.append((sb.length() == 0 ? "" : "&") + entry.getKey() + "=" + entry.getValue());
            }
        }
        
        return sb.toString();
	}
	
	public static int getActiveNodeListSize(String clusterDetailJson) {
		// get all active notes
		StringBuilder sb = new StringBuilder();
		Gson gson = new Gson();
		HashMap cluster = gson.fromJson(clusterDetailJson, HashMap.class);
		List nodes = (List) cluster.get("nodes");
		return nodes.size();
	}
	
	public static String getActiveNodeList(String clusterDetailJson) {
		// get all active notes
		StringBuilder sb = new StringBuilder();
		Gson gson = new Gson();
		HashMap cluster = gson.fromJson(clusterDetailJson, HashMap.class);
		List nodes = (List) cluster.get("nodes");
		for (Object obj : nodes) {
			Map map = (Map) obj;
			String hostname = (String)map.get("hostname");
			if (hostname != null) {
				int index = hostname.indexOf(':');
				if (index > 0) {
					hostname = hostname.substring(0,index);
					sb.append((sb.length() == 0 ? "" : ",") + "ns_1@" + hostname);
				}
			}
		}
		return sb.toString();
	}

	public static boolean isRebalanceRunning(String rebalanceProgressJson) {
		// {"status":"none"}
		// or
		// {
		// 		"status":"running",
		// 		"ns_1@192.168.0.56":{"progress":0.2734375},
		// 		"ns_1@192.168.0.77":{"progress":0.09114583333333337}
		// }
		StringBuilder sb = new StringBuilder();
		Gson gson = new Gson();
		HashMap rebalanceProgressStatus = gson.fromJson(rebalanceProgressJson, HashMap.class);
		String status = (String) rebalanceProgressStatus.get("status");
		if (status != null && status.equalsIgnoreCase("running")) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean isAutoFailoverEnabled() {
		HashMap failoverSettings = getAutoFailoverSetting();
		boolean enabled = false;
		Boolean b = (Boolean)failoverSettings.get("enabled");
		if (b != null && b.booleanValue()) {
			enabled = true;
		}
		return enabled;
	}
	
	public static HashMap getAutoFailoverSetting() {
		// {"enabled":false,"timeout":30,"count":0}

		// retrieve and filter cluster note basic authentication credentials
		// not through the request but through cpp_config.xml file
		String clusterUser = CppConfigurationBean.getInstance().getCbClusterUsername();
		String clusterPassword = CppConfigurationBean.getInstance().getCbClusterPassword();
		
		String hostName = CppConstants.COUCHBASE_CLUSTER_IPS;
		
		// retrieve automatic failover setting request
		String autoFailoverSettings = AdminUtil.makeHttpCall("http://"+hostName+":8091/settings/autoFailover", "GET", clusterUser, clusterPassword, null);
		
		Gson gson = new Gson();
		return gson.fromJson(autoFailoverSettings, HashMap.class);
	}
	
	public static boolean hasNodeFailedOver() {
		// {"nodes" -> [{"clusterMembership" : "active"},{"clusterMembership" : "inactiveFailed"}]}

		// retrieve and filter cluster note basic authentication credentials
		// not through the request but through cpp_config.xml file
		String clusterUser = CppConfigurationBean.getInstance().getCbClusterUsername();
		String clusterPassword = CppConfigurationBean.getInstance().getCbClusterPassword();
		
		String hostName = CppConstants.COUCHBASE_CLUSTER_IPS;
		
		// retrieve automatic failover setting request
		String clusterDetails = AdminUtil.makeHttpCall("http://"+hostName+":8091/pools/default", "GET", clusterUser, clusterPassword, null);
		
		Gson gson = new Gson();
		HashMap hmClusterDetails = (HashMap)gson.fromJson(clusterDetails, HashMap.class);
		
		boolean b = false;
		
		List<LinkedTreeMap> nodes = (List<LinkedTreeMap>)hmClusterDetails.get("nodes");
		for (LinkedTreeMap node : nodes) {
			String membershipStatus = (String)node.get("clusterMembership");
			//log.debug("hostname: " + node.get("hostname") + " == memshipstatus: " + membershipStatus);
			if ("inactiveFailed".equals(membershipStatus)) {
				b = true;
				break;
			}
		}
		
		return b;
	}
	
	public static String resetDiskPath(String hostname, String path) {
		LOGGER.debug(()->"Reset Couchbase data path for: " + hostname + " to: " + path);
		HashMap params = new HashMap();
		params.put("path", path);
		String settingsResponse = AdminUtil.makeHttpCall("http://" + hostname + ":8091/nodes/self/controller/settings", "POST", "badeploy", "badeploy123", params);
		if (settingsResponse == null || settingsResponse.length() == 0) {
			return "Data path was successfully reset to: '" + path + "' for host: '" + hostname + "'"; 
		}
		else {
			return "Data path reset failed on: '" + path + "' for host: '" + hostname + "'"; 
		}
	}

}
