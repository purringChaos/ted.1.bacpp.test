package tv.blackarrow.cpp.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mule.api.MuleEventContext;

import com.google.common.net.HttpHeaders;

public class MuleContextHelper {
	private static final String EMPTY_STRING = "";
	private static final Logger LOGGER = LogManager.getLogger(MuleContextHelper.class);

	private static final String HTTP_REMOTE_ADDRESS = "http.remote.address";

	public static String getClientIPAddressDetail(final MuleEventContext context) {

		if(context == null){
			return EMPTY_STRING;
		}

		//If Given that the standard allows more than one proxy address X-Forwarded-For: client, proxy1, proxy2. The left most one will contain actual client Address and right most will consider
		//the more recently used proxy.
		String xForwardedForValue = context.getMessage().getInboundProperty(HttpHeaders.X_FORWARDED_FOR) != null
				? context.getMessage().getInboundProperty(HttpHeaders.X_FORWARDED_FOR).toString().split(",")[0] : EMPTY_STRING;

		//E.g. MULE_REMOTE_CLIENT_ADDRESS=/10.1.21.169: 55959,
		String muleOriginatingEndpointValue = context.getMessage().getInboundProperty(HTTP_REMOTE_ADDRESS) != null
				? context.getMessage().getInboundProperty(HTTP_REMOTE_ADDRESS).toString().split(":")[0] : EMPTY_STRING;

		final String ipAddressOfClientForAuditLog = StringUtils.isNotBlank(xForwardedForValue) ? xForwardedForValue : (muleOriginatingEndpointValue.startsWith("/")?muleOriginatingEndpointValue.substring(1):muleOriginatingEndpointValue);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("The client IP Address is: " + ipAddressOfClientForAuditLog);
		}
		return ipAddressOfClientForAuditLog;
	}

}
