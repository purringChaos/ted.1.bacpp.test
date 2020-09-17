package tv.blackarrow.cpp.components.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Help {
	
	private static final Logger log = LogManager.getLogger(Help.class);

	public String process(Map<String, String> payload) {

		TreeMap allServices = new TreeMap();
		
		HashMap<String,String> params = null;
		
		// help
		TreeMap service = new TreeMap();
		service.put("serviceName", "Help");
		service.put("servicePath", "/admin/help");
		params = new HashMap<String,String>();
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/help");
		allServices.put("Help", service);
		
		// Healthcheck
    	service = new TreeMap();
		service.put("serviceName", "Healthcheck");
		service.put("servicePath", "admin/healthcheck");
		params = new HashMap<String,String>();
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/healthcheck");
		allServices.put("Healthcheck", service);
		
		// addNode
    	service = new TreeMap();
		service.put("serviceName", "AddNode");
		service.put("servicePath", "admin/cluster/addNode");
		params = new HashMap<String,String>();
		params.put("hostname", "The hostname/ip for the node that is joining the cluster.");
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/addNode?hostname=10.250.6.208");
		allServices.put("AddNode", service);
		
		// failOver
		service = new TreeMap();
		service.put("serviceName", "FailOver");
		service.put("servicePath", "admin/cluster/failOver");
		params = new HashMap<String,String>();
		params.put("otpNode", "The node hostname/ip that needs failover. Format: ns_1@<hostname>|<ip>");
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/failOver?otpNode=ns_1@10.250.6.208");
		allServices.put("FailOver", service);
		
		// removeNode
		service = new TreeMap();
		service.put("serviceName", "RemoveNode");
		service.put("servicePath", "admin/cluster/removeNode");
		params = new HashMap<String,String>();
		params.put("ejectedNodes", "The list of nodes that need to be removed from the cluster. Format: comma delimitted ns_1@<hostname>|<ip> list");
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/removeNode?ejectedNodes=ns_1@10.250.6.208");
		allServices.put("RemoveNode", service);
		
		// rebalance
		service = new TreeMap();
		service.put("serviceName", "Rebalance");
		service.put("servicePath", "admin/cluster/rebalance");
		params = new HashMap<String,String>();
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/rebalance");
		allServices.put("Rebalance", service);
		
		// viewDetail
		service = new TreeMap();
		service.put("serviceName", "ViewDetail");
		service.put("servicePath", "admin/cluster/viewDetail");
		params = new HashMap<String,String>();
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/viewDetail");
		allServices.put("ViewDetail", service);
		
		// get
		service = new TreeMap();
		service.put("serviceName", "Get");
		service.put("servicePath", "admin/cluster/get");
		params = new HashMap<String,String>();
		params.put("key", "The key to retrieve value for in the cluster.");
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/get?key=pokey");
		allServices.put("Get", service);
		
		// put
		service = new TreeMap();
		service.put("serviceName", "Put");
		service.put("servicePath", "admin/cluster/put");
		params = new HashMap<String,String>();
		params.put("key", "The key to put value for in the cluster.");
		params.put("value", "The value to put in the cluster for the key.");
		service.put("serviceParams", params);
		service.put("sampleURL", "http://app104.tst.sn.blackarrow.tv:6650/admin/cluster/put?key=pokey&value=povalue");
		allServices.put("Put", service);
		
		Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		String response = gson.toJson(allServices);
		
		return response;
	}
	
}
