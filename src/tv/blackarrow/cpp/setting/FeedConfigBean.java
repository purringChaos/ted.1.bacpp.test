package tv.blackarrow.cpp.setting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.utils.AlternateContentVersion;
import tv.blackarrow.cpp.utils.CppConstants;

public class FeedConfigBean {
	private static ConfigurableApplicationContext ctx = null;
	private static FeedConfigBean instance = null;
	private static final String ALLOW_OPEN_ENDED_BLACKOUTS = "Allow Open Ended Blackouts";
	private static final String TRIIGER_EVENTS_BY_EVENT_ID = "Trigger Event By Event ID";
	private static final String ALTERNATE_CONTENT_VERSION = "Alternate Content Version";
	private static final String ALTERNATE_CONTENT_ENABLED = "Alternate Content Enabled";
	private static final String ALTERNATE_CONTENT_LOCATION =	"Alternate Content Location";
	private static final String AD_SLATE_LOCATION =	"Ad Slate Location";
	private static final String FEED_SCC_NOTIFICATION_BUFFER =	"SCC Notification Buffer";
	private static final String SCC_NOTIFICATION_BUFFER =	"SCC Notification Buffer";
	private static final String PRETRIGGER_ADVANCE_INTERVAL = "Pre-Trig Advance Interval";
	private static final String PRETRIGGER_RETRY_INTERVAL = "Pre-Trig Repeat Interval";

	private Map<String, Map<String, String>> feedConfigMap;
	private Map<String, String> feedNetworkMap;
	private Map<String, String> feedProviderMap;
	private Map<String, Boolean> feedSignalAbortMap;
	private Map<String, Long> feedLastUpdatedTimeMap;

	private FeedConfigBean() {
	};

	public static void load() {
		if (ctx == null) {
				ctx = new FileSystemXmlApplicationContext("file:" + CppConfigurationBean.getInstance().getPoInProcessDir() + "/acquisition_feed_mapping.xml");
			} else {
			ctx.refresh();
		}
		instance = (FeedConfigBean) ctx.getBean("feedConfigBean");
	}

	public static FeedConfigBean getInstance() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public Map<String, Map<String, String>> getFeedConfigMap() {
		return feedConfigMap;
	}

	public void setFeedConfigMap(Map<String, Map<String, String>> map) {
		feedConfigMap = map;
	}

	public Map<String, String> getFeedNetworkMap() {
		if (feedNetworkMap == null) {
			feedNetworkMap = new HashMap<String, String>();
			Iterator<String> it = feedConfigMap.keySet().iterator();
			while (it.hasNext()) {
				String feedId = it.next();
				Map<String, String> feedMap = feedConfigMap.get(feedId);
				String value = feedMap.get(SettingUtils.NETWORK_ID);
				if (value != null) {
					feedNetworkMap.put(feedId, value);
				}
			}
		}
		return feedNetworkMap;
	}

	public Map<String, String> getFeedProviderMap() {
		if (feedProviderMap == null) {
			feedProviderMap = new HashMap<String, String>();
			Iterator<String> it = feedConfigMap.keySet().iterator();
			while (it.hasNext()) {
				String feedId = it.next();
				Map<String, String> feedMap = feedConfigMap.get(feedId);
				String value = feedMap.get(SettingUtils.PROVIDER_ID);
				if (value != null) {
					feedProviderMap.put(feedId, value);
				}
			}
		}
		return feedProviderMap;
	}

	public Map<String, Boolean> getFeedSignalAbortEnabledMap() {
		if (feedSignalAbortMap == null) {
			feedSignalAbortMap = new HashMap<String, Boolean>();
			Iterator<String> it = feedConfigMap.keySet().iterator();
			while (it.hasNext()) {
				String feedId = it.next();
				Map<String, String> feedMap = feedConfigMap.get(feedId);
				String value = feedMap.get(SettingUtils.SIGNAL_ABORT_ENABLED);
				if (value != null) {
					feedSignalAbortMap.put(feedId, Boolean.valueOf(value));
				}
			}
		}
		return feedSignalAbortMap;
	}

	private Map<String, Long> getFeedLastUpdatedTimeMap() {
		if (feedLastUpdatedTimeMap == null) {
			feedLastUpdatedTimeMap = new HashMap<String, Long>();
			Iterator<String> it = feedConfigMap.keySet().iterator();
			while (it.hasNext()) {
				String feedId = it.next();
				Map<String, String> feedMap = feedConfigMap.get(feedId);
				String value = feedMap.get(SettingUtils.LAST_UPDATED_TIME);
				if (value != null) {
					feedLastUpdatedTimeMap.put(feedId, Long.valueOf(value));
				}
			}
		}
		return feedLastUpdatedTimeMap;
	}

	public boolean isOpenEndedBlackoutAllowed(final String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(ALLOW_OPEN_ENDED_BLACKOUTS)
				&& Boolean.valueOf(feedConfigMap.get(feedId).get(ALLOW_OPEN_ENDED_BLACKOUTS));
	}

	public boolean isTriggerEventsByEventID(final String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(TRIIGER_EVENTS_BY_EVENT_ID)
				&& Boolean.valueOf(feedConfigMap.get(feedId).get(TRIIGER_EVENTS_BY_EVENT_ID));
	}

	public AlternateContentVersion getAltContentVersion(final String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(ALTERNATE_CONTENT_VERSION)
				? AlternateContentVersion.fromValue(feedConfigMap.get(feedId).get(ALTERNATE_CONTENT_VERSION))
				: null;
	}

	public String getSchedulelessAltContentLocation(final String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(ALTERNATE_CONTENT_LOCATION)
				? feedConfigMap.get(feedId).get(ALTERNATE_CONTENT_LOCATION)
				: null;
	}

	public boolean getUseInBandPlacementOpportunitySignals(final String feedId) {

		boolean inBandPlacementOpSignal = false;
		String useInBandPlacementOpSignal = (feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(CppConstants.USE_INBAND_OPPORTUNITY_SIGNAL)) ?
				feedConfigMap.get(feedId).get(CppConstants.USE_INBAND_OPPORTUNITY_SIGNAL)  : null;
		if(useInBandPlacementOpSignal != null && useInBandPlacementOpSignal.toUpperCase().equals(CppConstants.USE_INBAND_OPPORTUNITY_SIGNAL_YES)) {				
			inBandPlacementOpSignal=true;				
		}
		return inBandPlacementOpSignal;
	}

	public boolean isAltContentEnabled(final String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(ALTERNATE_CONTENT_ENABLED)
				&& Boolean.valueOf(feedConfigMap.get(feedId).get(ALTERNATE_CONTENT_ENABLED));
	}

	public boolean isSCTE224Feed(final String feedId) {
		return AlternateContentVersion.ESNI_224.equals(getAltContentVersion(feedId));
	}

	public boolean isI02Feed(final String feedId) {
		return AlternateContentVersion.ESNI_I02.equals(getAltContentVersion(feedId));
	}

	public boolean isSchedulelessAltContentEnabled(final String feedId) {
		return AlternateContentVersion.SCHEDULELESS.equals(getAltContentVersion(feedId));
	}

	public String getAdSlateLocation(String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) ? feedConfigMap.get(feedId).get(AD_SLATE_LOCATION) : null;
	}

	public long getLastUpdatedTime(String feedId) {
		if(getFeedLastUpdatedTimeMap() == null) {
			return -1;
		}
		Long lastUpdatedTime = getFeedLastUpdatedTimeMap().get(feedId);
		return lastUpdatedTime == null ? -1 : lastUpdatedTime;
	}
	
	public String getSCCNotificationBuffer(final String feedId) {
		return  feedConfigMap != null && 
				feedConfigMap.containsKey(feedId) ? 
				feedConfigMap.get(feedId).get(FEED_SCC_NOTIFICATION_BUFFER) : null;
	}

	public long getPretriggerAdvanceInterval(String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(PRETRIGGER_ADVANCE_INTERVAL)
				? Long.valueOf(feedConfigMap.get(feedId).get(PRETRIGGER_ADVANCE_INTERVAL))*1000
				: -1;
	}

	public long getPretriggerRetryInterval(String feedId) {
		return feedConfigMap != null && feedConfigMap.containsKey(feedId) && feedConfigMap.get(feedId).containsKey(PRETRIGGER_RETRY_INTERVAL)
				? Long.valueOf(feedConfigMap.get(feedId).get(PRETRIGGER_RETRY_INTERVAL))
				: -1;
	}
}
