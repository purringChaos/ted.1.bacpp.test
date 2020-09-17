//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//

package tv.blackarrow.cpp.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public final class CppConstants {

	public static final int HOURS_PER_DELIVERY = 48;
	public static int MAX_LOADING_TIME = 300; // number of seconds
	public static int MAX_LOADING_THREADS = 1;
	public static int GENERIC_MAX_LOCKING_TIME_FOR_LOCK = 60000; // 1 minute
	public static final int PO_LIST_ALL = -1;
	public static final int PO_LIST_UNCONFIRMED = 0;
	public static final int PO_LIST_CONFIRMED = 1;

	public static final int WINDOW_LIST_ALL = -1;
	public static final int WINDOW_LIST_ACTIVE = 0;
	public static final int WINDOW_LIST_REVOKED = 1;
	public static final long SIGNAL_TIME_OFFSET_DEFAULT_VALUE = 0;
	public static final String SIGNAL_TYPE_SCTE35 = "SCTE35";

	public static final String FEED_ID = "feed_id";
	public static final String EVENT_TIME = "event_time";
	public static final String FEED_ID_KEY = "feed_id_key";
	public static final String SIGNAL_ID = "signal_id";
	public static final String SEGMENT_DURATION = "seg_duration";
	public static final String ACQUISITION_POINT_IDENTITY = "acq_point_id";
	public static final String ACQUISITION_POINT = "acquisition_point";
	public static final String CONFIRMED_PLACEMENT_OPPORTUNITY = "conf_placement_opp";
	public static final String RESOURCE_NOT_FOUND = "res_not_found";
	public static final String PLACEMENT_OPPORTUNITY_NOT_FOUND = "po_not_found";
	public static final String SYSTEM_ERROR = "system_error";
	public static final String IS_GROUP_SIGNAL = "is_group_signals";

	public static final String BLACKOUT_EVENT_NOT_FOUND = "blackout_not_found";
	public static final String NO_REGIONAL_BLACKOUT_FLAG = "no_regional_blackout_flag";
	public static final String ARCHIVE_ALLOWED_FLAG = "archive_allowed_flag";
	public static final String DEVICE_RESTRICTIONS = "device_restrictions";


	public static final String PO_INGEST_RESPONSE_SUCCESS = "Schedule ingest successful";
	public static final String PO_INGEST_RESPONSE_FAIL = "Schedule ingest failed";

	public static final short SCTE35_SPLICE_INSERT_COMMAND_TYPE = 5;
	public static final short SCTE35_TIME_SIGNAL_COMMAND_TYPE = 6;

	public static final String SCTE35_RESPONSE_SIGNAL_ACTION_DELETE = "delete";
	public static final String SCTE35_RESPONSE_SIGNAL_ACTION_NOOP = "noop";
	public static final String SCTE35_RESPONSE_SIGNAL_ACTION_REPLACE = "replace";
	public static final String SCTE35_RESPONSE_SIGNAL_ACTION_CREATE = "create";
	public static final String IS_ACTION_SET = "isActionSet";

	public static final String INTERNAL_FLAG_SCHEDULELESS_ALTCONTENT_REQUEST = "scheduleless_blackout";
	public static final String INTERNAL_FLAG_ALTCONTENT_REQUEST = "blackout";
	public static final String INTERNAL_FLAG_PO_REQUEST = "placementOpportunity";
	public static final String INTERNAL_FLAG_UNSUPPORTED_SCC_SIGNAL = "unsupportedSCCSignalFlag";
	public static final String INTERNAL_FLAG_UNSUPPORTED_SCC = "unsupported";
	public static final String INTERNAL_FLAG_DELETE_SCC = "delete";
	
	public static final String DELIVERY_NOT_RESTRICTED_FLAG = "deliveryNotRestrictedFlag";
	public static final String WEB_DELIVERY_ALLOW_FLAG = "webDeliveryAllowedFlag";
	public static final String SEGMENTATION_DURATION_FLAG = "segmentation_duration_flag";
	public static final String SCTE224 = "SCTE224";
	public static final String SCTE224_MEDIA_BASED_ALTCONTENT = "mediaBased";
	public static final String CADENT_OOH_ZONE = "OOH";

	public static final String INTERFACE_LINEAR_PARITY = "linear";
	public static final String INTERFACE_COMCAST_CDVR = "includeInPoint";
	public static final String INTERFACE_COMCAST_P5 = "COMCAST_P5";
	public static final String SCC_INPOINTS_COMCAST_P3 = "COMCAST_P3";
	public static final String INTERFACE_CUSTOM = "CUSTOM";
	public static final String INTERFACE_TEMPLATE = "TEMPLATE";
	public static final String INTERFACE_STANDARD = "STANDARD";
	public static final String INTERFACE_TWC_LINEAR = "TWC_LINEAR";
	public static final String SIGNAL_STREAM_TYPE_HSS = "HSS";
	public static final String SIGNAL_STREAM_TYPE_HLS = "HLS";
	public static final String SIGNAL_STREAM_TYPE_HDS = "HDS";
	public static final String SIGNAL_STREAM_TYPE_DASH = "DASH";
	public static final String NO_SCHEDULES = "N";

	public static final String SIGNAL_NOQ_EVENT_TYPE = "NOQ";
	public static final String SIGNAL_NPO_EVENT_TYPE = "NPO";
	public static final String SIGNAL_INS_EVENT_TYPE = "INS";
	public static final String BLACKOUT_NO_EVENT_TYPE = "BNE";
	public static final String BLACKOUT_CONFIRMED_EVENT_TYPE = "CONFIRMED";
	public static final String BLACKOUT_COMPLETE_EVENT_TYPE = "COMPLETE";

	public static final String SERVINCE_ZONE_IDENTITY_ATTRIBUTE = "zoneIdentity";

	public static final String POID_DELIMITER = "/";
	public static final int HTTP_SUCCESS_CODE = 200;
	public static final int HTTP_FAILED_VALIDATION_CODE = 400;
	public static final int HTTP_NO_END_POINT_CODE = 404;
	public static final String POIS_SYSTEM_ID = "BAPOISServer";
	public static final String POIS_P2_VERSION_ID = "1.1";
	public static final String POIS_ADS_SIMULATOR_SYSTEM_ID = "BAADSNotificationSimulationServer";

	// Couchbase cluster bucket info
	// if prefered, all these information can be moved to a configuration file
	public static final String COUCHBASE_CLUSTER_IPS = getHostNames();
	public static final String COUCHBASE_CLUSTER_LINEAR_BUCKET_NAME = getBucketName();
	public static final int COUCHBASE_CLUSTER_CONNECTION_TIMEOUT= getConnectionTimeOut();
	private static final String COUCHBASE_CLUSTER_LINEAR_BUCKET_PASSWORD = "";
	public static final String COUCHBASE_CLUSTER_BUCKET_USER= getBucketUser();
	public static final String COUCHBASE_CLUSTER_BUCKET_PASSWORD= getBucketPassword();

	// blackout_confirmation value (in acquisition_feed_mapping.xml)
	public static final String IN_BAND = "In-Band";
	public static final String OUT_OF_BAND = "Out-of-Band";
	public static final String IP = "IP";
	public static final String ENABLED = "enabled";
	public static final String DISABLED = "disabled";

	public static final String INVALID_SCC_REQUEST = "invalid_scc_request";
	public static final String IS_COMBINED_SIGNAL = "isCombinedSignal";

	// constants for NAMESPACE_HACK -- ought to be removed once not needed
	public static final String NAMESPACE_HACK = "NAMESPACE_HACK";
	public static final String OLD_SCC_NAMESPACE = "http://www.cablelabs.com/namespaces/metadata/xsd/conditioning/2";
	public static final String NEW_SCC_NAMESPACE = "urn:cablelabs:iptvservices:esam:xsd:signal:1";
	public static final String OLD_MCC_NAMESPACE = "http://www.cablelabs.com/namespaces/metadata/xsd/confirmation/2";
	public static final String NEW_MCC_NAMESPACE = "urn:cablelabs:iptvservices:esam:xsd:manifest:1";
	public static final String IO3_NAMESPACE_SIGNALING = "urn:cablelabs:md:xsd:signaling:3.0";
	public static final String TRIGGER_BLACKOUT_EVENTS_BY_EVENT_ID ="trigger_blackout_events_by_event_id";

	public static final String USE_INBAND_OPPORTUNITY_SIGNAL="Use In Band Placement Opportunity Signals";//USE FOR VIACOM REQUIREMENT.
	public static final String USE_INBAND_OPPORTUNITY_SIGNAL_YES="YES";//USE FOR VIACOM REQUIREMENT.
	public static final String IS_VALID_PO_START_SIGNAL = "isValidPOStartSignal";
	public static final String CADENT_PO_END_SIGNAL = "cadentPoEndSignal";
	
	public static final String PTS_TIME_PLUS_OFFSET_IN_BINARY = "PTS_TIME_PLUS_OFFSET_IN_BINARY";
	public static final String PTS_TIME_PLUS_OFFSET_IN_MILLIS = "PTS_TIME_PLUS_OFFSET_IN_MILLIS";


	public static  Map<Short, Short> SEGMENTATION_TYPE_MAP = Collections.unmodifiableMap(new HashMap<Short, Short>() {
		{
			put(SegmentType.PLACEMENT_OPPORTUNITY_START.getSegmentTypeId(), SegmentType.PLACEMENT_OPPORTUNITY_END.getSegmentTypeId());
			put(SegmentType.PROVIDER_ADVERTISEMENT_START.getSegmentTypeId(), SegmentType.PROVIDER_ADVERTISEMENT_END.getSegmentTypeId());
			put(SegmentType.DISTRIBUTOR_ADVERTISEMENT_START.getSegmentTypeId(), SegmentType.DISTRIBUTOR_ADVERTISEMENT_END.getSegmentTypeId());
		}
	});

	public enum ESSNodeDeliveryType {
		IP, QAM, ALL;
	}

	private CppConstants() {}

	final private static String getHostNames(){
		String cbHostNames = CppConfigurationBean.getInstance().getCbHostNames();
		return cbHostNames == null || cbHostNames.trim().isEmpty() ? "127.0.0.1" : cbHostNames.trim();
	}

	final private static String getBucketName(){
		String cbBucketName = CppConfigurationBean.getInstance().getCbBucketName();
		return cbBucketName == null || cbBucketName.trim().isEmpty() ? "pois_linear" : cbBucketName.trim();
	}
	
	final private static String getBucketUser(){
		String cbBucketUser = CppConfigurationBean.getInstance().getCbBucketAccessUserName();
		return cbBucketUser == null || cbBucketUser.trim().isEmpty() ? "pois_linear" : cbBucketUser.trim();
	}
	
	final private static String getBucketPassword(){
		String cbBucketPassword = CppConfigurationBean.getInstance().getCbBucketAccessUsersPassword();
		return cbBucketPassword == null || cbBucketPassword.trim().isEmpty() ? COUCHBASE_CLUSTER_LINEAR_BUCKET_PASSWORD : cbBucketPassword.trim();
	}

	final private static int getConnectionTimeOut(){
		int connectionTimeout= CppConfigurationBean.getInstance().getConnectionTimeout();
		return connectionTimeout == 0 ? 10000 : connectionTimeout;
	}
}
