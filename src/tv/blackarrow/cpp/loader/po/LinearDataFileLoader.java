package tv.blackarrow.cpp.loader.po;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.loader.bo.LinearBlackoutDataFileLoader;
import tv.blackarrow.cpp.loader.bo.LinearMediaDataFileLoader;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.scte224.ApplyorRemove;
import tv.blackarrow.cpp.model.scte224.MatchSignal;
import tv.blackarrow.cpp.model.scte224.MatchType;
import tv.blackarrow.cpp.model.scte224.Media;
import tv.blackarrow.cpp.model.scte224.MediaPoint;

/**
 * entry class for PO data loader, and blackout data loader. 
 * @author pzhang
 *
 */
public class LinearDataFileLoader {

	private static final Logger LOGGER = LogManager.getLogger(LinearDataFileLoader.class);

	public void load() {
		if (DataManagerFactory.getInstance().isServerInActiveDataCenter()) {
			LinearPODataFileLoader poloader = new LinearPODataFileLoader();
			poloader.load();
			LinearBlackoutDataFileLoader blackoutLoader = new LinearBlackoutDataFileLoader();
			blackoutLoader.load();

			//dummyMediaObject();//TO REMOVE LATER YAMINEE

			LinearMediaDataFileLoader.getInstance().load();
		} else {
			LOGGER.info(()->"This server is not in an active data center so skipping the placement rule loading process.");
		}
	}

	private void dummyMediaObject() {
		List<Media> mediasToBeSaved = new LinkedList<Media>();

		LinkedList<MediaPoint> list = new LinkedList<MediaPoint>();
		List<Short> segemntTypeIdList = new ArrayList<Short>();
		segemntTypeIdList.add(new Short("16"));
		segemntTypeIdList.add(new Short("23"));
		
		List<Short> segemntTypeIdListEnd = new ArrayList<Short>();
		segemntTypeIdListEnd.add(new Short("17"));
		segemntTypeIdListEnd.add(new Short("18"));

		LinkedList<ApplyorRemove> applyList = new LinkedList<ApplyorRemove>();
		ApplyorRemove newApp = new ApplyorRemove(1800000l);
		applyList.add(newApp);

		LinkedList<String> asserts = new LinkedList<String>();
		asserts.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[@segmentationUpidType = 8]");
		asserts.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[text()='708142419']");
		asserts.add("/SpliceInfoSection/SegmentationDescriptor[@segmentationTypeId = 16 or @segmentationTypeId = 23]");

		LinkedList<String> assertsEnd = new LinkedList<String>();
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[@segmentationUpidType = 8]");
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[text()='708142419']");
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor[@segmentationTypeId = 17 or @segmentationTypeId = 18]");

		MatchSignal signal = new MatchSignal(1987200000l, MatchType.ANY, asserts, 8, "708142419", segemntTypeIdList);
		MatchSignal signalEnd = new MatchSignal(1987200000l, MatchType.ANY, assertsEnd, 8, "708142419", segemntTypeIdListEnd);

		long currentTimeInMillis = System.currentTimeMillis();
		long mpStartEffective = currentTimeInMillis - (10 * 60 * 1000);//10 minutes before
		long mpStartMatchTime = currentTimeInMillis - (5 * 60 * 1000);//5 minutes before
		long mpEndMatchOffsetDurationTime = (20 * 60 * 60 * 1000);//20 hours Tolerance
		long mpStartExpired = currentTimeInMillis + (24 * 60 * 60 * 1000);//24 hours after
		
		List<String> zones = new ArrayList<>();
		zones.add("1");
		zones.add("5");
		zones.add("4");
		zones.add("6");
		
		
		MediaPoint mpStart = new MediaPoint("-EmWupMKR_eoBJqBeMeysQ", 1, mpStartEffective, mpStartExpired, mpStartMatchTime, mpEndMatchOffsetDurationTime, signal, applyList, applyList, zones, true, 0);
		MediaPoint mpEnd = new MediaPoint("6cwUe1vFTlaVTfY6QtZTNg", 2, mpStartEffective, mpStartExpired, mpStartMatchTime, mpEndMatchOffsetDurationTime, signalEnd, null, applyList, zones, false, 3);
		list.add(mpStart);
		list.add(mpEnd);

		long mEffective = mpStartEffective - (10 * 60 * 1000);//10 minutes before

		long mExpired = mpStartExpired + (24 * 60 * 60 * 1000);//24 hours after
		Media m = new Media("/media/YM1", "YFeed_SCTE224_MediaDuration", mEffective, mExpired, 1520379135000l, "xyz", "NsvVQHoSQhORvo7rR_WSsg","NsvVQHoSQhORvo7rR_WSsg", list, false, false);
		mediasToBeSaved.add(m);
		LinkedList<Media> mediasToBeS= new LinkedList<Media>();
		mediasToBeS.addAll(mediasToBeSaved);
		
		//DataManagerFactory.getSCTE224DataManager().saveMedias("YFeed_SCTE224_MediaDuration", mediasToBeS);
		DataManagerFactory.getSCTE224DataManager().saveMediasDuringRuleFileLoadV1("YFeed_SCTE224_MediaDuration", mediasToBeS, null, true, null);
		dummy2MediaObject();
	}
	
	private List<Media> dummy2MediaObject() {
		List<Media> mediasToBeSaved = new LinkedList<Media>();

		LinkedList<MediaPoint> list = new LinkedList<MediaPoint>();
		List<Short> segemntTypeIdList = new ArrayList<Short>();
		segemntTypeIdList.add(new Short("16"));
		segemntTypeIdList.add(new Short("23"));
		
		List<Short> segemntTypeIdListEnd = new ArrayList<Short>();
		segemntTypeIdListEnd.add(new Short("17"));
		segemntTypeIdListEnd.add(new Short("18"));

		LinkedList<ApplyorRemove> applyList = new LinkedList<ApplyorRemove>();
		ApplyorRemove newApp = new ApplyorRemove();
		applyList.add(newApp);

		LinkedList<String> asserts = new LinkedList<String>();
		asserts.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[@segmentationUpidType = 8]");
		asserts.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[text()='708142419']");
		asserts.add("/SpliceInfoSection/SegmentationDescriptor[@segmentationTypeId = 16 or @segmentationTypeId = 23]");

		LinkedList<String> assertsEnd = new LinkedList<String>();
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[@segmentationUpidType = 8]");
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor/SegmentationUpid[text()='708142419']");
		assertsEnd.add("/SpliceInfoSection/SegmentationDescriptor[@segmentationTypeId = 17 or @segmentationTypeId = 18]");

		MatchSignal signal = new MatchSignal(1987200000l, MatchType.ANY, asserts, 8, "708142419", segemntTypeIdList);
		MatchSignal signalEnd = new MatchSignal(1987200000l, MatchType.ANY, assertsEnd, 8, "708142419", segemntTypeIdListEnd);

		long currentTimeInMillis = System.currentTimeMillis();
		long mpStartEffective = currentTimeInMillis - (10 * 60 * 1000);//10 minutes before
		long mpStartMatchTime = currentTimeInMillis - (5 * 60 * 1000);//5 minutes before
		long mpEndMatchOffsetDurationTime = (20 * 60 * 60 * 1000);//20 hours Tolerance
		long mpStartExpired = currentTimeInMillis + (24 * 60 * 60 * 1000);//24 hours after
		
		List<String> zones = new ArrayList<>();
		zones.add("1");
		zones.add("5");
		zones.add("4");
		zones.add("6");
		MediaPoint mpStart = new MediaPoint("-E1WupMKR_eoBJqBeMeysQ", 1, mpStartEffective, mpStartExpired, mpStartMatchTime, mpEndMatchOffsetDurationTime, signal, applyList, applyList, zones, true, 0);
		MediaPoint mpEnd = new MediaPoint("1cwUe1vFTlaVTfY6QtZTNg", 2, mpStartEffective, mpStartExpired, mpStartMatchTime, mpEndMatchOffsetDurationTime, signalEnd, null, null, zones, true, 0);
		list.add(mpStart);
		list.add(mpEnd);

		long mEffective = mpStartEffective - (10 * 60 * 1000);//10 minutes before

		long mExpired = mpStartExpired + (24 * 60 * 60 * 1000);//24 hours after
		Media m = new Media("/media/YM2", "YFeed_SCTE224", mEffective, mExpired, 1520379135000l, "xyz", "N1vVQHoSQhORvo7rR_WSsg","NsvVQHoSQhORvo7rR_WSsg", list, false, false);
		mediasToBeSaved.add(m);
		LinkedList<Media> mediasToBeS= new LinkedList<Media>();
		mediasToBeS.addAll(mediasToBeSaved);
		
		//DataManagerFactory.getSCTE224DataManager().saveMedias("YFeed_SCTE224", mediasToBeS);
		DataManagerFactory.getSCTE224DataManager().saveMediasDuringRuleFileLoadV1("YFeed_SCTE224", mediasToBeS, null, true, null);
		return mediasToBeS;
	}

}
