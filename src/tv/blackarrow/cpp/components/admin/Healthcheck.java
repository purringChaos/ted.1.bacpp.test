package tv.blackarrow.cpp.components.admin;

import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.core.message.internal.EndpointHealth;
import com.couchbase.client.core.state.LifecycleState;

import tv.blackarrow.cpp.cb.CouchbaseUtil;
import tv.blackarrow.cpp.managers.DataManagerFactory;

public class Healthcheck {

	private static final String OK = "OK";

	public String process() {
		DiagnosticsReport report = CouchbaseUtil.CLUSTER.diagnostics();
		for (EndpointHealth endpoint : report.endpoints()) {
			if (endpoint.state() != LifecycleState.CONNECTED) {
				return "Error - The application couldn't communicate to Couchbase cluster. Please verify that all the Couchbase cluster nodes are in healthy states.";
			}
		}
		if (!DataManagerFactory.getInstance().isServerInActiveDataCenter()) {
			return "Error - This application does not belong to an active data center. Please direct your requests to an active data center.";
		}
		return OK;
	}

}
