package test.tv.blackarrow.cpp.components;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import tv.blackarrow.cpp.components.POProcessingComponent;
import tv.blackarrow.cpp.log.model.PoisAuditLogVO;
import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.CppConstants;

public class POProcessingComponentTest {
	private static DataManager dataManager;
    private static final String IPADDRESS_OF_REMOTE_CLIENT = "<IPADDRESS_OF_REMOTE_CLIENT>";
    private static final String ACQUISITION_SIGNAL_ID = "<ACQUISITION_SIGNAL_ID>";
	private static final String TEST_AP_IDENTITY = "Test_AP_Identity";
	private static AcquisitionPoint TEST_AP = null;
	private static final String[] breakIds = new String[] {
		"98f8d7e9-20e4-4bbc-a74c-11e2d44daa1d", "98f8d7e9-20e4-4bbc-a74c-11e2d44daa1e", "98f8d7e9-20e4-4bbc-a74c-11e2d44daa1f"};
	
	private static final HashMap<String, Boolean> lastProcessedPoKeyZone = new HashMap<String, Boolean>();
	
	@Before
	public void setup() {
		dataManager = DataManagerFactory.getInstance();
		
		// set the expiration time to something short so this test data is purged quickly
        // note: if you run this test two times more quickly than this expiration time it will fail
		dataManager.setDefaultDataExpirationSeconds(3);
		setupTestSignalProcessorCursor();
		TEST_AP = dataManager.getAcquisitionPoint(TEST_AP_IDENTITY);
	}
	
	@Test
	public void testConfirmedPOSignal() throws Exception {
		POProcessingComponent component = new POProcessingComponent(TEST_AP, 1352231550000L);
		Assert.assertNull(component.getConfirmedPlacementOpportunity());
		
		component = new POProcessingComponent(TEST_AP, 1352231560000L);
		ConfirmedPlacementOpportunity cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNotNull(cpo);
		Assert.assertEquals(cpo.getSignalId(), "98f8d7e9-20e4-4bbc-a74c-11econfirmed");
	}
	
	@Test
	public void testGetNewPOSignal() throws Exception {
		POProcessingComponent component = new POProcessingComponent(TEST_AP, 1352231470000L);
		ConfirmedPlacementOpportunity cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		Assert.assertNull(cpo);
		
		component = new POProcessingComponent(TEST_AP, 1352231570000L+5000);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		Assert.assertNotNull(cpo);
		Assert.assertNotNull(cpo.getSignalId());
		//System.out.println(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 1);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		
		component = new POProcessingComponent(TEST_AP, 1352232170001L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		Assert.assertNotNull(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 2);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		//resend the signal again
		component = new POProcessingComponent(TEST_AP, 1352232170001L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNotNull(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 2);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352232770002L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 2);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352233371503L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 1);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352233971504L);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 1);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352233971504L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		//System.out.println(cpo.getSignalId());
		Assert.assertNull(cpo);
		
		//let's simulate loader updating more windows.
		simulateUpdatePOByLoader();
	
		component = new POProcessingComponent(TEST_AP, 1352234600000L+5000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 3);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352235200002L);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		dataManager.putConfirmedPlacementOpportunity(cpo);
		//System.out.println(cpo.getSignalId());
		Assert.assertNotNull(cpo.getSignalId());
		Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 3);
		Assert.assertNotNull(cpo.getPoKeyByZone());
		
		component = new POProcessingComponent(TEST_AP, 1352235200002L+66000);
		cpo = component.getConfirmedPlacementOpportunity();
		Assert.assertNull(cpo);
		cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
		//System.out.println(cpo.getSignalId());
		Assert.assertNull(cpo);
		
		//test lookup by signalId
		POProcessingComponent newComponent = new POProcessingComponent();
		cpo = newComponent.getConfirmedPlacementOpportunity("98f8d7e9-20e4-4bbc-a74c-11e2d44daa1d");
		Assert.assertNotNull(cpo);
		
		cpo = component.getConfirmedPlacementOpportunity("98f8d7e9-20e4-4bbc-a74c-11e2d44dab12");
		Assert.assertNotNull(cpo);
		
		cpo = newComponent.getConfirmedPlacementOpportunity("98f8d7e9-4bbc-a74c-11e2d44daa1d");
		Assert.assertNull(cpo);
		
	}
	
	private static void setupTestSignalProcessorCursor() {
		populateLastProcessedPoKeyZoneMap();
        SignalProcessorCursor spc = new SignalProcessorCursor();
        spc.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        spc.setLastProcessedPOKeyByZone(lastProcessedPoKeyZone);
        HashMap<String, String> nextPOKeyByZone = new HashMap<String, String>();
        for (int i = 0; i < breakIds.length; i++) {
            nextPOKeyByZone.put(Integer.toString(i), breakIds[i]);
        }
        spc.setNextPOKeyByZone(nextPOKeyByZone);
        
       
        //populate
        populateTestAcquisitionPoint();
        populatePlacementOpportunity();
        populateConfirmedPlacementOpportunity();
        dataManager.putSignalProcessorCursor(spc);
    }
	
	private static void validateData() {
		//let's test it's all correct first
        DataManager dataManager = DataManagerFactory.getInstance();
		SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(TEST_AP_IDENTITY);
    	
		if (cursor != null) {
    		Map<String, String>  nextKeyByZone = cursor.getNextPOKeyByZone();
    		Map<String, Boolean>  processedLastKeyByZone = cursor.getLastProcessedPOKeyByZone();
    		
    		String poKey = nextKeyByZone.get("0");
    		PlacementOpportunity po = dataManager.getPlacementOpportunity(poKey);
    		System.out.println("Zone: 0 "+" : "+processedLastKeyByZone.get("0"));
    		while (po != null) {
    			System.out.println(po.getPOKey()+" : "+po.getUtcWindowStartTime()+" : "+po.getNextPOKey());
    			po = dataManager.getPlacementOpportunity(po.getNextPOKey());
    		}
    		
    		System.out.println("Zone: 1 "+" : "+processedLastKeyByZone.get("1"));
    		poKey = nextKeyByZone.get("1");
    		po = dataManager.getPlacementOpportunity(poKey);
    		while (po != null) {
    			System.out.println(po.getPOKey()+" : "+po.getUtcWindowStartTime()+" : "+po.getNextPOKey());
    			po = dataManager.getPlacementOpportunity(po.getNextPOKey());
    		}
    		
    		System.out.println("Zone: 2 "+" : "+processedLastKeyByZone.get("2"));
    		poKey = nextKeyByZone.get("2");
    		po = dataManager.getPlacementOpportunity(poKey);
    		while (po != null) {
    			System.out.println(po.getPOKey()+" : "+po.getUtcWindowStartTime()+" : "+po.getNextPOKey());
    			po = dataManager.getPlacementOpportunity(po.getNextPOKey());
    		}
		}
	}
	
	private static void populateLastProcessedPoKeyZoneMap() {
		for (int i = 0; i < breakIds.length; i++) {
			lastProcessedPoKeyZone.put(Integer.toString(i), new Boolean(false));
		}
	}
	
	private static void simulateUpdatePOByLoader() {
		//zone 1 chain of pos
		PlacementOpportunity poZone12 = new PlacementOpportunity();
        poZone12.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab12");
        poZone12.setUtcWindowStartTime(1352234000000L+600000*2+2);
        poZone12.setWindowDurationMilliseconds(600000);
        poZone12.setPlacementsDurationMilliseconds(3000);
        poZone12.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone12);
	        
        PlacementOpportunity poZone11 = new PlacementOpportunity();
        poZone11.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab11");
        poZone11.setUtcWindowStartTime(1352234000000L+600000*1+1);
        poZone11.setWindowDurationMilliseconds(600000);
        poZone11.setPlacementsDurationMilliseconds(3000);
        poZone11.setNextPOKey(poZone12.getPOKey());
        dataManager.putPlacementOpportunity(poZone11);
        
        PlacementOpportunity poZone1 = new PlacementOpportunity();
        poZone1.setPOKey(breakIds[0]);
        poZone1.setUtcWindowStartTime(1352231572500L);
        poZone1.setWindowDurationMilliseconds(600000);
        poZone1.setPlacementsDurationMilliseconds(90000);
        poZone1.setNextPOKey(poZone11.getPOKey());
        dataManager.putPlacementOpportunity(poZone1);
        
        //zone 2 chain of pos
		PlacementOpportunity poZone24 = new PlacementOpportunity();
		poZone24.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab24");
		poZone24.setUtcWindowStartTime(1352234000000L+600000*2+2);
		poZone24.setWindowDurationMilliseconds(600000);
		poZone24.setPlacementsDurationMilliseconds(5000);
		poZone24.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone24);
	        
        PlacementOpportunity poZone23 = new PlacementOpportunity();
        poZone23.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab23");
        poZone23.setUtcWindowStartTime(1352234000000L+600000*1+1);
        poZone23.setWindowDurationMilliseconds(600000);
        poZone23.setPlacementsDurationMilliseconds(5000);
        poZone23.setNextPOKey(poZone24.getPOKey());
        dataManager.putPlacementOpportunity(poZone23);
        
        PlacementOpportunity poZone22 = new PlacementOpportunity();
        poZone22.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab22");
        poZone22.setUtcWindowStartTime(1352231570000L+600000*2+2);
        poZone22.setWindowDurationMilliseconds(600000);
        poZone22.setPlacementsDurationMilliseconds(90000);
        poZone22.setNextPOKey(poZone23.getPOKey());
        dataManager.putPlacementOpportunity(poZone22);
        
        
        //zone 3 chain of pos
		PlacementOpportunity poZone36 = new PlacementOpportunity();
		poZone36.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab36");
		poZone36.setUtcWindowStartTime(1352234000000L+600000*2+2);
		poZone36.setWindowDurationMilliseconds(600000);
		poZone36.setPlacementsDurationMilliseconds(7000);
		poZone36.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone36);
	        
        PlacementOpportunity poZone35 = new PlacementOpportunity();
        poZone35.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab35");
        poZone35.setUtcWindowStartTime(1352234000000L+600000*1+1);
        poZone35.setWindowDurationMilliseconds(600000);
        poZone35.setPlacementsDurationMilliseconds(7000);
        poZone35.setNextPOKey(poZone36.getPOKey());
        dataManager.putPlacementOpportunity(poZone35);
        
        PlacementOpportunity poZone34 = new PlacementOpportunity();
        poZone34.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab34");
        poZone34.setUtcWindowStartTime(1352231571500L+600000*4+4);
        poZone34.setWindowDurationMilliseconds(600000);
        poZone34.setPlacementsDurationMilliseconds(5000);
        poZone34.setNextPOKey(poZone35.getPOKey());
        dataManager.putPlacementOpportunity(poZone34);
	}
	
	private static void populatePlacementOpportunity() {
        PlacementOpportunity poZone1 = new PlacementOpportunity();
        poZone1.setPOKey(breakIds[0]);
        poZone1.setUtcWindowStartTime(1352231572500L);
        poZone1.setWindowDurationMilliseconds(600000);
        poZone1.setPlacementsDurationMilliseconds(90000);
        poZone1.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone1);
        
        //zone 2 chain of pos
        PlacementOpportunity poZone22 = new PlacementOpportunity();
        poZone22.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab22");
        poZone22.setUtcWindowStartTime(1352231570000L+600000*2+2);
        poZone22.setWindowDurationMilliseconds(600000);
        poZone22.setPlacementsDurationMilliseconds(90000);
        poZone22.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone22);
        
        PlacementOpportunity poZone21 = new PlacementOpportunity();
        poZone21.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab21");
        poZone21.setUtcWindowStartTime(1352231570000L+600000*1+1);
        poZone21.setWindowDurationMilliseconds(600000);
        poZone21.setPlacementsDurationMilliseconds(90000);
        poZone21.setNextPOKey(poZone22.getPOKey());
        dataManager.putPlacementOpportunity(poZone21);
        
        PlacementOpportunity poZone2 = new PlacementOpportunity();
        poZone2.setPOKey(breakIds[1]);
        poZone2.setUtcWindowStartTime(1352231570000L);
        poZone2.setWindowDurationMilliseconds(600000);
        poZone2.setPlacementsDurationMilliseconds(90000);
        poZone2.setNextPOKey(poZone21.getPOKey());
        dataManager.putPlacementOpportunity(poZone2);
        
        //zone 3 chain of pos
        PlacementOpportunity poZone34 = new PlacementOpportunity();
        poZone34.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab34");
        poZone34.setUtcWindowStartTime(1352231571500L+600000*4+4);
        poZone34.setWindowDurationMilliseconds(600000);
        poZone34.setPlacementsDurationMilliseconds(5000);
        poZone34.setNextPOKey(null);
        dataManager.putPlacementOpportunity(poZone34);
        
        PlacementOpportunity poZone33 = new PlacementOpportunity();
        poZone33.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab33");
        poZone33.setUtcWindowStartTime(1352231571500L+600000*3+3);
        poZone33.setWindowDurationMilliseconds(600000);
        poZone33.setPlacementsDurationMilliseconds(5000);
        poZone33.setNextPOKey(poZone34.getPOKey());
        dataManager.putPlacementOpportunity(poZone33);
        
        PlacementOpportunity poZone32 = new PlacementOpportunity();
        poZone32.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab32");
        poZone32.setUtcWindowStartTime(1352231571500L+600000*2+2);
        poZone32.setWindowDurationMilliseconds(600000);
        poZone32.setPlacementsDurationMilliseconds(5000);
        poZone32.setNextPOKey(poZone33.getPOKey());
        dataManager.putPlacementOpportunity(poZone32);
        
        PlacementOpportunity poZone31 = new PlacementOpportunity();
        poZone31.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab31");
        poZone31.setUtcWindowStartTime(1352231571500L+600000*1+1);
        poZone31.setWindowDurationMilliseconds(600000);
        poZone31.setPlacementsDurationMilliseconds(500);
        poZone31.setNextPOKey(poZone32.getPOKey());
        dataManager.putPlacementOpportunity(poZone31);
        
        PlacementOpportunity poZone3 = new PlacementOpportunity();
        poZone3.setPOKey(breakIds[2]);
        poZone3.setUtcWindowStartTime(1352231571500L);
        poZone3.setWindowDurationMilliseconds(600000);
        poZone3.setPlacementsDurationMilliseconds(90000);
        poZone3.setNextPOKey(poZone31.getPOKey());
        dataManager.putPlacementOpportunity(poZone3);
    }
	
	private static void populateConfirmedPlacementOpportunity() {
        ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        cpo.setSignalId("98f8d7e9-20e4-4bbc-a74c-11econfirmed");
        cpo.setUtcSignalTime(1352231560000L);
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
        cpo.setPoKeyByZone(poKeyByZone);
        dataManager.putConfirmedPlacementOpportunity(cpo);
    }
	
	private static void populateTestAcquisitionPoint() {
        AcquisitionPoint ap = new AcquisitionPoint();
        ap.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        ap.setProviderExternalRef("test1.provider.com");
        ap.setSccDeleteEmptyBreak(true);
        ap.setBaIntefactTypeExternalRef(CppConstants.INTERFACE_LINEAR_PARITY);
        dataManager.putAcquisitionPoint(ap);
    }
	
	 private PoisAuditLogVO getDummyPoisAuditVo() {
	    	PoisAuditLogVO vo = new PoisAuditLogVO();
	    	vo.setIpAddressOfClient(IPADDRESS_OF_REMOTE_CLIENT);
	    	vo.setAcquisitionSignalID(ACQUISITION_SIGNAL_ID);
			return vo;
		}
	
}
