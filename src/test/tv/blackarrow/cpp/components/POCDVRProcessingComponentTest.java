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

public class POCDVRProcessingComponentTest {
    private static final String IPADDRESS_OF_REMOTE_CLIENT = "<IPADDRESS_OF_REMOTE_CLIENT>";
    private static final String ACQUISITION_SIGNAL_ID = "<ACQUISITION_SIGNAL_ID>";
    private static DataManager dataManager;
    private static AcquisitionPoint TEST_AP = null;
    private static final String[] breakIds = new String[] { "7nvY7KNyQw63rycnde+Zow==", "JDdxklCvTceQ+4Qu5vP+OA==",
            " Wtt+cgUGS3meMKCduHI6tA==" };

    private static final HashMap<String, Boolean> lastProcessedPoKeyZone = new HashMap<String, Boolean>();

    @Before
    public void setup() {
        dataManager = DataManagerFactory.getInstance();

        // set the expiration time to something short so this test data is purged quickly
        // note: if you run this test two times more quickly than this expiration time it will fail
        dataManager.setDefaultDataExpirationSeconds(3);
        setupTestSignalProcessorCursor();
        TEST_AP = dataManager.getAcquisitionPoint("Test_CDVR_AP_Identity");
    }

    @Test
    public void testConfirmedPOSignal() throws Exception {
        POProcessingComponent component = new POProcessingComponent(TEST_AP, 1352231550000L);
        Assert.assertNull(component.getConfirmedPlacementOpportunity());

        component = new POProcessingComponent(TEST_AP, 1352231560000L);
        ConfirmedPlacementOpportunity cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNotNull(cpo);
        Assert.assertEquals(cpo.getSignalId(), "wvTmUUzpT56zRR07TwBTrw==");
    }

    @Test
    public void testNPOSignal() throws Exception {
        POProcessingComponent component = new POProcessingComponent(TEST_AP, 1352231572500L + 600000 * 3 + 3 + 5000);
        ConfirmedPlacementOpportunity cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNotNull(cpo);
        Assert.assertNotNull(cpo.getSignalId());
        //System.out.println(cpo.getSignalId());

        component = new POProcessingComponent(TEST_AP, 1452231570000L);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNull(cpo);
    }

    @Test
    public void testGetNewPOSignal() throws Exception {
    	final long SIGNAL_TIME_BASE = 1352231572500L;
        POProcessingComponent component = new POProcessingComponent(TEST_AP, 1352231470000L);
        ConfirmedPlacementOpportunity cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNull(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 5000);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNotNull(cpo);
        Assert.assertNotNull(cpo.getSignalId());
        //System.out.println(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 3);
        Assert.assertNotNull(cpo.getPoKeyByZone());

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 1 + 1 + 5000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNotNull(cpo);
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 2);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        //resend the signal again
        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 1 + 1 + 5000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNotNull(cpo);
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 2);
        Assert.assertNotNull(cpo.getPoKeyByZone());

        // signal time within break durations, should be considered confirmed.
        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 1 + 1 + 6000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNotNull("multi-cue confirmation failed ", cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 1 + 1 + 5000 + 60001);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull("confirmed po passed the break duration failed ", cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 2);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 2 + 2 + 5000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 1);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 3 + 3 + 5000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getPlacementsDurationsInMilliseconds().size(), 1);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 3 + 3 + 6000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNotNull("multi-cue confirmation failed ", cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 3 + 3 + 6000 + 60001);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        Assert.assertNull(cpo);

        //let's simulate loader updating more windows.
        simulateUpdatePOByLoader();

        //validateData();
        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 4 + 4 + 6000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 3);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 5 + 5 + 6000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertNotNull(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 3);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 6 + 6 + 6000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);
        cpo = component.getPlacementOpportunity(getDummyPoisAuditVo());
        //System.out.println(cpo.getSignalId());
        Assert.assertEquals(cpo.getBreakInfos().size(), 1);
        Assert.assertNotNull(cpo.getPoKeyByZone());
        dataManager.putConfirmedPlacementOpportunity(cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 6 + 6 + 7000);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNotNull("multi-cue confirmation failed ", cpo);

        component = new POProcessingComponent(TEST_AP, SIGNAL_TIME_BASE + 600000 * 6 + 6 + 6000 + 90001);
        cpo = component.getConfirmedPlacementOpportunity();
        Assert.assertNull(cpo);

        //test lookup by signalId
        POProcessingComponent newComponent = new POProcessingComponent();
        cpo = newComponent.getConfirmedPlacementOpportunity("HHKvf3cnRtGVhezDdYtPRA==");
        Assert.assertNotNull(cpo);

        cpo = component.getConfirmedPlacementOpportunity("prnh8Jw+T3i/RNutL5fWew==");
        Assert.assertNotNull(cpo);

        cpo = newComponent.getConfirmedPlacementOpportunity("98f8d7e9-4bbc-a74c-11e2d44daa1d");
        Assert.assertNull(cpo);

    }

    private static void setupTestSignalProcessorCursor() {
        populateLastProcessedPoKeyZoneMap();
        SignalProcessorCursor spc = new SignalProcessorCursor();
        spc.setAcquisitionPointIdentity(TEST_AP.getAcquisitionPointIdentity());
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
        SignalProcessorCursor cursor = dataManager.getSignalProcessorCursor(TEST_AP.getAcquisitionPointIdentity());

        if (cursor != null) {
            Map<String, String> nextKeyByZone = cursor.getNextPOKeyByZone();
            Map<String, Boolean> processedLastKeyByZone = cursor.getLastProcessedPOKeyByZone();

            String poKey = nextKeyByZone.get("0");
            PlacementOpportunity po = dataManager.getPlacementOpportunity(poKey);
            System.out.println("Zone: 0 " + " : " + processedLastKeyByZone.get("0"));
            while (po != null) {
                System.out.println(po.getPOKey() + " : " + po.getUtcWindowStartTime() + " : " + po.getNextPOKey() + " : "
                        + po.getOutSignalId());
                po = dataManager.getPlacementOpportunity(po.getNextPOKey());
            }

            System.out.println("Zone: 1 " + " : " + processedLastKeyByZone.get("1"));
            poKey = nextKeyByZone.get("1");
            po = dataManager.getPlacementOpportunity(poKey);
            while (po != null) {
                System.out.println(po.getPOKey() + " : " + po.getUtcWindowStartTime() + " : " + po.getNextPOKey() + " : "
                        + po.getOutSignalId());
                po = dataManager.getPlacementOpportunity(po.getNextPOKey());
            }

            System.out.println("Zone: 2 " + " : " + processedLastKeyByZone.get("2"));
            poKey = nextKeyByZone.get("2");
            po = dataManager.getPlacementOpportunity(poKey);
            while (po != null) {
                System.out.println(po.getPOKey() + " : " + po.getUtcWindowStartTime() + " : " + po.getNextPOKey() + " : "
                        + po.getOutSignalId());
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
        PlacementOpportunity poZone13 = new PlacementOpportunity();
        poZone13.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab13");
        poZone13.setInSignalId(poZone13.getPOKey());
        poZone13.setUtcWindowStartTime(1352231572500L + 600000 * 6 + 6);
        poZone13.setWindowDurationMilliseconds(600000);
        poZone13.setPlacementsDurationMilliseconds(90000);
        poZone13.setNextPOKey(null);
        poZone13.setOutSignalId("prnh8Jw+T3i/RNutL5fWew==");
        poZone13.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone13);

        PlacementOpportunity poZone12 = new PlacementOpportunity();
        poZone12.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab12");
        poZone12.setInSignalId(poZone12.getPOKey());
        poZone12.setUtcWindowStartTime(1352231572500L + 600000 * 5 + 5);
        poZone12.setWindowDurationMilliseconds(600000);
        poZone12.setPlacementsDurationMilliseconds(90000);
        poZone12.setNextPOKey(poZone13.getPOKey());
        poZone12.setOutSignalId("ZtQzE/YQR/SiN1PEBCVBfQ==");
        poZone12.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone12);

        PlacementOpportunity poZone11 = new PlacementOpportunity();
        poZone11.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab11");
        poZone11.setInSignalId(poZone11.getPOKey());
        poZone11.setUtcWindowStartTime(1352231572500L + 600000 * 4 + 4);
        poZone11.setWindowDurationMilliseconds(600000);
        poZone11.setPlacementsDurationMilliseconds(90000);
        poZone11.setNextPOKey(poZone12.getPOKey());
        poZone11.setOutSignalId("tIk4UGLpQ32af7SoFrylbA==");
        poZone11.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone11);

        PlacementOpportunity poZone1 = new PlacementOpportunity();
        poZone1.setPOKey(breakIds[1]);
        poZone1.setInSignalId(poZone1.getPOKey());
        poZone1.setUtcWindowStartTime(1352231572500L + 600000 * 3 + 3);
        poZone1.setWindowDurationMilliseconds(600000);
        poZone1.setPlacementsDurationMilliseconds(90000);
        poZone1.setNextPOKey(poZone11.getPOKey());
        poZone1.setOutSignalId("HHKvf3cnRtGVhezDdYtPRA==");
        poZone1.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone1);

        //zone 2 chain of pos
        PlacementOpportunity poZone24 = new PlacementOpportunity();
        poZone24.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab24");
        poZone24.setInSignalId(poZone24.getPOKey());
        poZone24.setUtcWindowStartTime(1352231572500L + 600000 * 5 + 5);
        poZone24.setWindowDurationMilliseconds(600000);
        poZone24.setPlacementsDurationMilliseconds(60000);
        poZone24.setNextPOKey(null);
        poZone24.setBreakOrder(1);
        poZone24.setOutSignalId("ZtQzE/YQR/SiN1PEBCVBfQ==");
        dataManager.putPlacementOpportunity(poZone24);

        PlacementOpportunity poZone23 = new PlacementOpportunity();
        poZone23.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab23");
        poZone23.setInSignalId(poZone23.getPOKey());
        poZone23.setUtcWindowStartTime(1352231572500L + 600000 * 4 + 4);
        poZone23.setWindowDurationMilliseconds(600000);
        poZone23.setPlacementsDurationMilliseconds(60000);
        poZone23.setNextPOKey(poZone24.getPOKey());
        poZone23.setBreakOrder(1);
        poZone23.setOutSignalId("tIk4UGLpQ32af7SoFrylbA==");
        dataManager.putPlacementOpportunity(poZone23);

        PlacementOpportunity poZone22 = new PlacementOpportunity();
        poZone22.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab22");
        poZone22.setInSignalId(poZone22.getPOKey());
        poZone22.setUtcWindowStartTime(1352231572500L + 600000 * 3 + 3);
        poZone22.setWindowDurationMilliseconds(600000);
        poZone22.setPlacementsDurationMilliseconds(60000);
        poZone22.setNextPOKey(poZone23.getPOKey());
        poZone22.setBreakOrder(1);
        poZone22.setOutSignalId("HHKvf3cnRtGVhezDdYtPRA==");
        dataManager.putPlacementOpportunity(poZone22);

        //zone 3 chain of pos
        PlacementOpportunity poZone06 = new PlacementOpportunity();
        poZone06.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab36");
        poZone06.setUtcWindowStartTime(1352231572500L + 600000 * 5 + 5);
        poZone06.setWindowDurationMilliseconds(600000);
        poZone06.setPlacementsDurationMilliseconds(30000);
        poZone06.setNextPOKey(null);
        poZone06.setOutSignalId("ZtQzE/YQR/SiN1PEBCVBfQ==");
        poZone06.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone06);

        PlacementOpportunity poZone05 = new PlacementOpportunity();
        poZone05.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab35");
        poZone05.setUtcWindowStartTime(1352231572500L + 600000 * 4 + 4);
        poZone05.setWindowDurationMilliseconds(600000);
        poZone05.setPlacementsDurationMilliseconds(30000);
        poZone05.setNextPOKey(poZone06.getPOKey());
        poZone05.setOutSignalId("tIk4UGLpQ32af7SoFrylbA==");
        poZone05.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone05);

        PlacementOpportunity poZone04 = new PlacementOpportunity();
        poZone04.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab34");
        poZone04.setInSignalId(poZone04.getPOKey());
        poZone04.setUtcWindowStartTime(1352231572500L + 600000 * 3 + 3);
        poZone04.setWindowDurationMilliseconds(600000);
        poZone04.setPlacementsDurationMilliseconds(30000);
        poZone04.setNextPOKey(poZone05.getPOKey());
        poZone04.setBreakOrder(1);
        poZone04.setOutSignalId("HHKvf3cnRtGVhezDdYtPRA==");
        dataManager.putPlacementOpportunity(poZone04);
    }

    private static void populatePlacementOpportunity() {
        PlacementOpportunity poZone1 = new PlacementOpportunity();
        poZone1.setPOKey(breakIds[1]);
        poZone1.setInSignalId(poZone1.getPOKey());
        poZone1.setUtcWindowStartTime(1352231572500L);
        poZone1.setWindowDurationMilliseconds(600000);
        poZone1.setPlacementsDurationMilliseconds(90000);
        poZone1.setNextPOKey(null);
        poZone1.setOutSignalId("AF3UIXNESe+rykdnx2ANUg==");
        poZone1.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone1);

        //zone 2 chain of pos
        PlacementOpportunity poZone22 = new PlacementOpportunity();
        poZone22.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab22");
        poZone22.setInSignalId(poZone22.getPOKey());
        poZone22.setUtcWindowStartTime(1352231572500L + 600000 * 1 + 1);
        poZone22.setWindowDurationMilliseconds(600000);
        poZone22.setPlacementsDurationMilliseconds(60000);
        poZone22.setNextPOKey(null);
        poZone22.setBreakOrder(2);
        poZone22.setOutSignalId("Yhi/7olGTKa4mVJ+IxgS3A==");
        dataManager.putPlacementOpportunity(poZone22);

        PlacementOpportunity poZone21 = new PlacementOpportunity();
        poZone21.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab21");
        poZone21.setInSignalId(poZone21.getPOKey());
        poZone21.setUtcWindowStartTime(1352231572500L + 600000 * 1 + 1);
        poZone21.setWindowDurationMilliseconds(600000);
        poZone21.setPlacementsDurationMilliseconds(60000);
        poZone21.setNextPOKey(poZone22.getPOKey());
        poZone21.setBreakOrder(1);
        poZone21.setOutSignalId("gzG5zKi7T4aNOu2YZeM59w==");
        dataManager.putPlacementOpportunity(poZone21);

        PlacementOpportunity poZone2 = new PlacementOpportunity();
        poZone2.setPOKey(breakIds[2]);
        poZone2.setInSignalId(poZone2.getPOKey());
        poZone2.setUtcWindowStartTime(1352231572500L);
        poZone2.setWindowDurationMilliseconds(600000);
        poZone2.setPlacementsDurationMilliseconds(60000);
        poZone2.setNextPOKey(poZone21.getPOKey());
        poZone2.setBreakOrder(1);
        poZone2.setOutSignalId("AF3UIXNESe+rykdnx2ANUg==");
        dataManager.putPlacementOpportunity(poZone2);

        //zone 0 chain of pos
        PlacementOpportunity poZone04 = new PlacementOpportunity();
        poZone04.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab34");
        poZone04.setInSignalId(poZone04.getPOKey());
        poZone04.setUtcWindowStartTime(1352231572500L + 600000 * 3 + 3);
        poZone04.setWindowDurationMilliseconds(600000);
        poZone04.setPlacementsDurationMilliseconds(30000);
        poZone04.setNextPOKey(null);
        poZone04.setBreakOrder(1);
        poZone04.setOutSignalId("HHKvf3cnRtGVhezDdYtPRA==");
        dataManager.putPlacementOpportunity(poZone04);

        PlacementOpportunity poZone03 = new PlacementOpportunity();
        poZone03.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab33");
        poZone03.setInSignalId(poZone03.getPOKey());
        poZone03.setUtcWindowStartTime(1352231572500L + 600000 * 2 + 2);
        poZone03.setWindowDurationMilliseconds(600000);
        poZone03.setPlacementsDurationMilliseconds(30000);
        poZone03.setNextPOKey(poZone04.getPOKey());
        poZone03.setBreakOrder(1);
        poZone03.setOutSignalId("iQSYVJqMSfGNMl/0KO5Oew==");
        dataManager.putPlacementOpportunity(poZone03);

        PlacementOpportunity poZone02 = new PlacementOpportunity();
        poZone02.setInSignalId(poZone02.getPOKey());
        poZone02.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab32");
        poZone02.setUtcWindowStartTime(1352231572500L + 600000 * 1 + 1);
        poZone02.setWindowDurationMilliseconds(600000);
        poZone02.setPlacementsDurationMilliseconds(30000);
        poZone02.setNextPOKey(poZone03.getPOKey());
        poZone02.setBreakOrder(2);
        poZone02.setOutSignalId("Yhi/7olGTKa4mVJ+IxgS3A==");
        dataManager.putPlacementOpportunity(poZone02);

        PlacementOpportunity poZone01 = new PlacementOpportunity();
        poZone01.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44dab31");
        poZone01.setInSignalId(poZone01.getPOKey());
        poZone01.setUtcWindowStartTime(1352231572500L + 600000 * 1 + 1);
        poZone01.setWindowDurationMilliseconds(600000);
        poZone01.setPlacementsDurationMilliseconds(30000);
        poZone01.setNextPOKey(poZone02.getPOKey());
        poZone01.setOutSignalId("gzG5zKi7T4aNOu2YZeM59w==");
        poZone01.setBreakOrder(1);
        dataManager.putPlacementOpportunity(poZone01);

        PlacementOpportunity poZone0 = new PlacementOpportunity();
        poZone0.setPOKey(breakIds[0]);
        poZone0.setInSignalId(poZone0.getPOKey());
        poZone0.setUtcWindowStartTime(1352231572500L);
        poZone0.setWindowDurationMilliseconds(600000);
        poZone0.setPlacementsDurationMilliseconds(30000);
        poZone0.setNextPOKey(poZone01.getPOKey());
        poZone0.setBreakOrder(1);
        poZone0.setOutSignalId("AF3UIXNESe+rykdnx2ANUg==");
        dataManager.putPlacementOpportunity(poZone0);
    }

    private static void populateConfirmedPlacementOpportunity() {
        ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(TEST_AP.getAcquisitionPointIdentity());
        cpo.setSignalId("wvTmUUzpT56zRR07TwBTrw==");
        cpo.setUtcSignalTime(1352231560000L);
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
        cpo.setPoKeyByZone(poKeyByZone);
        dataManager.putConfirmedPlacementOpportunity(cpo);
    }

    private static void populateTestAcquisitionPoint() {
        AcquisitionPoint ap = new AcquisitionPoint();
        ap.setAcquisitionPointIdentity("Test_CDVR_AP_Identity");
        ap.setProviderExternalRef("test1.provider.com");
        ap.setSccDeleteEmptyBreak(true);
        ap.setBaIntefactTypeExternalRef(CppConstants.INTERFACE_COMCAST_CDVR);
        dataManager.putAcquisitionPoint(ap);
    }

    private PoisAuditLogVO getDummyPoisAuditVo() {
    	PoisAuditLogVO vo = new PoisAuditLogVO();
    	vo.setIpAddressOfClient(IPADDRESS_OF_REMOTE_CLIENT);
    	vo.setAcquisitionSignalID(ACQUISITION_SIGNAL_ID);
		return vo;
	}

}
