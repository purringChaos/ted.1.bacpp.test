package tv.blackarrow.cpp.pretrigger.service;

import org.springframework.stereotype.Service;

@Service
public interface PretriggerNotificationService {

	/**
	 * send out-bound message to the transcoder(s)
	 * The requested format is XMIL
	 * @param url
	 * @param requestStr
	 * @return notification result is successful or not
	 */
	public boolean sendNotificationMesage(final String url, final String requestStr);
	
}
