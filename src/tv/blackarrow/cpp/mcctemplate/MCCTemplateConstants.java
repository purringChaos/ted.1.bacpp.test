package tv.blackarrow.cpp.mcctemplate;

import java.util.HashMap;

import javax.xml.namespace.QName;

/**
 * @author shwetanks
 *
 */
public class MCCTemplateConstants {

	
	
	// Macro constant for MCC Template
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SPLICE_COMMAND_TYPE = "[scte35.spliceCommandType]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID_FEED_ID = "[feed.providerId-feedId]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_EVENT_ID = "[scte35.segmentationEventId]";
	
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_TYPE_ID = "[scte35.segmentationTypeId]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_WEB_DELIVERY_ALLOWED_FLAG = "[scte35.webDeliveryAllowedFlag]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_NO_REGIONAL_BLACKOUT_FLAG = "[scte35.noRegionalBlackoutFlag]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_ARCHIVE_ALLOWED_FLAG = "[scte35.archiveAllowedFlag]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_DEVICE_RESTRICTIONS = "[scte35.deviceRestrictions]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_DURATION = "[scte35.segmentationDuration]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID = "[scte35.segmentationUpid]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_TYPE = "[scte35.segmentationUpidType]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_SIGNAL_ID = "[scte35.segmentationSignalId]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_FEED_FEED_ID = "[feed.feedId]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_FEED_PROVIDER_ID = "[feed.providerId]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_UTC_POINT = "[scte35.utcPoint]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_ACQUISITION_POINT_IDENTITY = "[scte35.acquisitionPointIdentity]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_NUM = "[scte35.segmentationNum]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_COUNT = "[scte35.segmentationCount]";
	public static final String MCC_TEMPLATE_MACRO_SCTE35_SEGMENTATION_UPID_PROGRAM_ID = "[scte35.segmentationUpidProgramId]";

	
	public static final String MCC_TEMPLATE_FIRST_SEGMENT = "first-segment";
	public static final String MCC_TEMPLATE_SPAN_SEGMENT = "span-segment";
	public static final String MCC_TEMPLATE_LAST_SEGMENT = "last-segment";
	
	public static final QName MCC_TEMPLATE_SEGMENTATION_TYPE_ID = new QName("", "MCC_TEMPLATE_SEGMENTATION_TYPE_ID");
	public static final QName MCC_TEMPLATE_UTC_TIME = new QName("", "MCC_TEMPLATE_UTC_TIME");
	
	//
	
	public static final String TIME_SIGNAL_ID = "6"; 
	public static final String TIME_SIGNAL_VALUE = "timeSignal";
	public static final String SPLICE_INSERT_ID = "5"; 
	public static final String SPLICE_INSERT_VALUE = "spliceInsert";
	
	public static final HashMap<String, String> SPLICE_TYPES = new HashMap<String, String>();
	static {
		SPLICE_TYPES.put(TIME_SIGNAL_ID,TIME_SIGNAL_VALUE);
		SPLICE_TYPES.put(SPLICE_INSERT_ID,SPLICE_INSERT_VALUE);
	}
	
}
