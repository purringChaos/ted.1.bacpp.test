package test.tv.blackarrow.cpp.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.BreakInfo;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.RuntimeEnvironmentState;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.CppConstants;
import tv.blackarrow.cpp.utils.UUIDUtils;

@RunWith(Parameterized.class)
public class DataManagerTest {

    private static final String TEST_FEED_EXTERNAL_REF = "Test_Feed";
    private static final String TEST_AP_IDENTITY = "Test_AP_Identity";
    private DataManager dataManager;

    public DataManagerTest(DataManager dataManagerImplementationUnderTest) {
        this.dataManager = dataManagerImplementationUnderTest;
    }

    private AcquisitionPoint getTestAcquisitionPoint() {
        AcquisitionPoint ap = new AcquisitionPoint();
        ap.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        ap.setProviderExternalRef("test1.provider.com");
        ap.setSccDeleteEmptyBreak(true);
        ap.setBaIntefactTypeExternalRef(CppConstants.INTERFACE_LINEAR_PARITY);
        return ap;
    }

    private LoaderCursor getTestLoaderCursor() {
        LoaderCursor lc = new LoaderCursor();
        lc.setFeedExternalRef(TEST_FEED_EXTERNAL_REF);
        HashMap<String, String> lastPOKeyByZone = new HashMap<String, String>();
        lastPOKeyByZone.put("001", "ee47263d-058a-4d44-9030-da10838bec3a");
        lastPOKeyByZone.put("002", "56e41d5e-1426-44ff-9071-72fc1bc64e46");
        lastPOKeyByZone.put("999", "777d6ea6-3514-4662-86c9-8f0f97f697db");
        lc.setLastPOKeyByZone(lastPOKeyByZone);
        return lc;
    }

    private SignalProcessorCursor getTestSignalProcessorCursor() {
        SignalProcessorCursor spc = new SignalProcessorCursor();
        spc.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        HashMap<String, String> nextPOKeyByZone = new HashMap<String, String>();
        nextPOKeyByZone.put("001", "98f8d7e9-20e4-4bbc-a74c-11e2d44daa1d");
        nextPOKeyByZone.put("002", "6e41d5e-14256-44ff-9071-72fc1bc64e46");
        nextPOKeyByZone.put("999", "85c45c0c-c34e-4ec5-bcaf-5e2c62224ddb");
        spc.setNextPOKeyByZone(nextPOKeyByZone);
        return spc;
    }

    private PlacementOpportunity getTestPlacementOpportunitySingle() {
        PlacementOpportunity po = new PlacementOpportunity();
        po.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44daa1d");
        po.setUtcWindowStartTime(1352231572500L);
        po.setWindowDurationMilliseconds(600000);
        po.setPlacementsDurationMilliseconds(90000);
        po.setNextPOKey("7416316f-bf29-4a07-b92b-09f0ddc48be9");
        po.setOutSignalId("98f8d7e9-20e4-4bbc-a74c-11e2d44daabb");
        po.setInSignalId("98f8d7e9-99e4-4bbc-a74c-11e2d44daabb");
        po.setBreakOrder(1);
        return po;
    }

    private PlacementOpportunity getTestPlacementOpportunity() {
        PlacementOpportunity po = new PlacementOpportunity();
        po.setPOKey("98f8d7e9-20e4-4bbc-a74c-11e2d44daa1d");
        po.setUtcWindowStartTime(1352231572500L);
        po.setWindowDurationMilliseconds(600000);
        po.setPlacementsDurationMilliseconds(90000);
        po.setNextPOKey("7416316f-bf29-4a07-b92b-09f0ddc48be9");
        return po;
    }

    private ConfirmedPlacementOpportunity getTestConfirmedPlacementOpportunitySingle() {
        ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        cpo.setSignalId("925b5ca9-3c81-4ac8-b228-9ffa79bd1042");
        cpo.setUtcSignalTime(1352231850750L);
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
        poKeyByZone.put("001", "925b5ca9-3c81-4ac8-b228-9ffa79bd1042");
        poKeyByZone.put("002", "b86560ba-b1fe-42dc-80ea-d27fc1e4c68c");
        poKeyByZone.put("999", "e1fc736a-27a2-4ba0-8495-1ebcae4ed82d");
        cpo.setPoKeyByZone(poKeyByZone);

        ArrayList<BreakInfo> breakInfos = new ArrayList<BreakInfo>();
        breakInfos.add(new BreakInfo("signal1", "break1", 90));
        breakInfos.add(new BreakInfo("signal2", "break2", 30));
        breakInfos.add(new BreakInfo("signal3", "break3", 60));
        cpo.setBreakInfos(breakInfos);

        return cpo;
    }

    private ConfirmedPlacementOpportunity getTestConfirmedPlacementOpportunity() {
        ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(TEST_AP_IDENTITY);
        cpo.setSignalId("925b5ca9-3c81-4ac8-b228-9ffa79bd1042");
        cpo.setUtcSignalTime(1352231850750L);
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
        poKeyByZone.put("001", "925b5ca9-3c81-4ac8-b228-9ffa79bd1042");
        poKeyByZone.put("002", "b86560ba-b1fe-42dc-80ea-d27fc1e4c68c");
        poKeyByZone.put("999", "e1fc736a-27a2-4ba0-8495-1ebcae4ed82d");
        cpo.setPoKeyByZone(poKeyByZone);
        ArrayList<BreakInfo> breakInfos = new ArrayList<BreakInfo>();
        breakInfos.add(new BreakInfo("signal1", "break1", 30000));
        breakInfos.add(new BreakInfo("signal2", "break2", 60000));
        cpo.setBreakInfos(breakInfos);
        return cpo;
    }

    private RuntimeEnvironmentState getTestRuntimeEnvironmentState() {
        RuntimeEnvironmentState res = new RuntimeEnvironmentState();
        HashSet<String> acquisitionPointIdentities = new HashSet<String>();
        acquisitionPointIdentities.add("AP1");
        acquisitionPointIdentities.add("AP2");
        acquisitionPointIdentities.add("AP3");
        res.setAcquisitionPointIdentities(acquisitionPointIdentities);
        return res;
    }

    private void assertEqualsNotSame(AcquisitionPoint expected, AcquisitionPoint actual) {
        // the cas id is expected to change
        // expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSame(LoaderCursor expected, LoaderCursor actual) {
        // the cas id is expected to change
        expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSame(SignalProcessorCursor expected, SignalProcessorCursor actual) {
        // the cas id is expected to change
        expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSame(PlacementOpportunity expected, PlacementOpportunity actual) {
        // the cas id is expected to change
        expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSame(ConfirmedPlacementOpportunity expected, ConfirmedPlacementOpportunity actual) {
        // the cas id is expected to change
        // expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSame(RuntimeEnvironmentState expected, RuntimeEnvironmentState actual) {
        // the cas id is expected to change
        // expected.setCasId(actual.getCasId());
        assertEqualsNotSameObject(expected, actual);
    }

    private void assertEqualsNotSameObject(Object expected, Object actual) {
        Assert.assertNotNull("Expected object should not be null", expected);
        Assert.assertNotNull("Actual object should not be null", actual);

        // verify it is a copy of our object and not the actual same reference
        Assert.assertNotSame("Objects should not be references to the exact same object", expected, actual);

        // verify that all the values are the same (use serialization to make it easier)
        String expectedSerialized = new Gson().toJson(expected);
        String actualSerialized = new Gson().toJson(actual);
        Assert.assertEquals("Object field values should be the same", expectedSerialized, actualSerialized);
    }

    @Test
    public void testGetAcquisitionPointSimple() {
        AcquisitionPoint expected = getTestAcquisitionPoint();

        // make sure our test object is not present before we start
        AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        Assert.assertNull("Object should not exist at this point", actual);

        // now add the object
        dataManager.putAcquisitionPoint(expected);

        // verify it is present and correct now
        actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetAcquisitionPointMultiple() {
        List<AcquisitionPoint> expectedList = new ArrayList<AcquisitionPoint>();
        for (int i = 0; i < 1000; i++) {
            AcquisitionPoint expected = getTestAcquisitionPoint();
            expected.setAcquisitionPointIdentity(expected.getAcquisitionPointIdentity() + i);
            expected.setProviderExternalRef(expected.getProviderExternalRef() + i);
            expectedList.add(expected);
        }

        // make sure our test objects are not present before we start
        for (AcquisitionPoint expected : expectedList) {
            AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
            Assert.assertNull("Object should not exist at this point", actual);
        }

        // now add the objects
        for (AcquisitionPoint expected : expectedList) {
            dataManager.putAcquisitionPoint(expected);
        }

        // verify it is present and correct now
        for (AcquisitionPoint expected : expectedList) {
            AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
            assertEqualsNotSame(expected, actual);
        }
    }

    @Test
    public void testGetAcquisitionPointIsolatedUpdates() {
        AcquisitionPoint expected = getTestAcquisitionPoint();
        dataManager.putAcquisitionPoint(expected);

        // verify it is present and correct now
        AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // verify that changes to the object returned do not affect the data store
        actual.setProviderExternalRef("updated provider");
        actual.setSccDeleteEmptyBreak(false);

        // get the value from the data store again after we have updated our copy
        AcquisitionPoint actualAfterUpdate = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate);
    }

    @Test
    public void testPutAcquisitionPointIsolatedUpdates() {
        AcquisitionPoint original = getTestAcquisitionPoint();
        AcquisitionPoint expected = getTestAcquisitionPoint();
        dataManager.putAcquisitionPoint(original);

        // verify that changes to the original object do not affect the data store
        original.setProviderExternalRef("updated provider");
        original.setSccDeleteEmptyBreak(false);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetAcquisitionPointUpdate() {
        AcquisitionPoint expected = getTestAcquisitionPoint();
        dataManager.putAcquisitionPoint(expected);

        // verify it is present and correct now
        AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setProviderExternalRef("updated provider");
        actual.setSccDeleteEmptyBreak(false);
        dataManager.putAcquisitionPoint(actual);

        // get the value from the data store again after we have updated our copy
        AcquisitionPoint actualAfterUpdate = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate);
    }

    @Test
    public void testDeleteAcquisitionPoint() {
        AcquisitionPoint expected = getTestAcquisitionPoint();
        dataManager.putAcquisitionPoint(expected);

        // verify it is present and correct now
        AcquisitionPoint actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // delete the object
        dataManager.deleteAcquisitionPoint(expected.getAcquisitionPointIdentity());

        // verify it is not present now
        actual = dataManager.getAcquisitionPoint(expected.getAcquisitionPointIdentity());
        Assert.assertNull("Object should not exist at this point", actual);
    }

    @Test
    public void testGetLoaderCursorSimple() {
        LoaderCursor expected = getTestLoaderCursor();

        // make sure our test object is not present before we start
        LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        Assert.assertNull("Object should not exist at this point", actual);

        // now add the object
        dataManager.putLoaderCursor(expected);

        // verify it is present and correct now
        actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetLoaderCursorMultiple() {
        List<LoaderCursor> expectedList = new ArrayList<LoaderCursor>();
        for (int i = 0; i < 1000; i++) {
            LoaderCursor expected = getTestLoaderCursor();
            expected.setFeedExternalRef(expected.getFeedExternalRef() + i);
            for (String key : expected.getLastPOKeyByZone().keySet()) {
                expected.getLastPOKeyByZone().put(key, expected.getLastPOKeyByZone().get(key) + i);
            }
            expectedList.add(expected);
        }

        // make sure our test objects are not present before we start
        for (LoaderCursor expected : expectedList) {
            LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
            Assert.assertNull("Object should not exist at this point", actual);
        }

        // now add the objects
        for (LoaderCursor expected : expectedList) {
            dataManager.putLoaderCursor(expected);
        }

        // verify it is present and correct now
        for (LoaderCursor expected : expectedList) {
            LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
            assertEqualsNotSame(expected, actual);
        }
    }

    @Test
    public void testGetLoaderCursorIsolatedUpdates() {
        LoaderCursor expected = getTestLoaderCursor();
        dataManager.putLoaderCursor(expected);

        // verify it is present and correct now
        LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        assertEqualsNotSame(expected, actual);

        // verify that changes to the object returned do not affect the data store
        actual.setLastPOKeyByZone(null);

        // get the value from the data store again after we have updated our copy
        LoaderCursor actualAfterUpdate = dataManager.getLoaderCursor(expected.getFeedExternalRef());

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate);
    }

    @Test
    public void testPutLoaderCursorIsolatedUpdates() {
        LoaderCursor original = getTestLoaderCursor();
        LoaderCursor expected = getTestLoaderCursor();
        dataManager.putLoaderCursor(original);

        // verify that changes to the original object do not affect the data store
        original.setLastPOKeyByZone(null);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetLoaderCursorUpdate() {
        LoaderCursor expected = getTestLoaderCursor();
        dataManager.putLoaderCursor(expected);

        // verify it is present and correct now
        LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setLastPOKeyByZone(null);
        dataManager.putLoaderCursor(actual);

        // get the value from the data store again after we have updated our copy
        LoaderCursor actualAfterUpdate = dataManager.getLoaderCursor(expected.getFeedExternalRef());

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate);
    }

    @Test
    public void testDeleteLoaderCursor() {
        LoaderCursor expected = getTestLoaderCursor();
        dataManager.putLoaderCursor(expected);

        // verify it is present and correct now
        LoaderCursor actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        assertEqualsNotSame(expected, actual);

        // delete the object
        dataManager.deleteLoaderCursor(expected.getFeedExternalRef());

        // verify it is not present now
        actual = dataManager.getLoaderCursor(expected.getFeedExternalRef());
        Assert.assertNull("Object should not exist at this point", actual);
    }

    @Test
    public void testGetSignalProcessorCursorSimple() {
        SignalProcessorCursor expected = getTestSignalProcessorCursor();

        // make sure our test object is not present before we start
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        Assert.assertNull("Object should not exist at this point", actual);

        // now add the object
        dataManager.putSignalProcessorCursor(expected);

        // verify it is present and correct now
        actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetSignalProcessorCursorMultiple() {
        List<SignalProcessorCursor> expectedList = new ArrayList<SignalProcessorCursor>();
        for (int i = 0; i < 1000; i++) {
            SignalProcessorCursor expected = getTestSignalProcessorCursor();
            expected.setAcquisitionPointIdentity(expected.getAcquisitionPointIdentity() + i);
            for (String key : expected.getNextPOKeyByZone().keySet()) {
                expected.getNextPOKeyByZone().put(key, expected.getNextPOKeyByZone().get(key) + i);
            }
            expectedList.add(expected);
        }

        // make sure our test objects are not present before we start
        for (SignalProcessorCursor expected : expectedList) {
            SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
            Assert.assertNull("Object should not exist at this point", actual);
        }

        // now add the objects
        for (SignalProcessorCursor expected : expectedList) {
            dataManager.putSignalProcessorCursor(expected);
        }

        // verify it is present and correct now
        for (SignalProcessorCursor expected : expectedList) {
            SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
            assertEqualsNotSame(expected, actual);
        }
    }

    @Test
    public void testGetSignalProcessorCursorIsolatedUpdates() {
        SignalProcessorCursor expected = getTestSignalProcessorCursor();
        dataManager.putSignalProcessorCursor(expected);

        // verify it is present and correct now
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // verify that changes to the object returned do not affect the data store
        actual.setNextPOKeyByZone(null);

        // get the value from the data store again after we have updated our copy
        SignalProcessorCursor actualAfterUpdate = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate);
    }

    @Test
    public void testPutSignalProcessorCursorIsolatedUpdates() {
        SignalProcessorCursor original = getTestSignalProcessorCursor();
        SignalProcessorCursor expected = getTestSignalProcessorCursor();
        dataManager.putSignalProcessorCursor(original);

        // verify that changes to the original object do not affect the data store
        original.setNextPOKeyByZone(null);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetSignalProcessorCursorUpdate() {
        SignalProcessorCursor expected = getTestSignalProcessorCursor();
        dataManager.putSignalProcessorCursor(expected);

        // verify it is present and correct now
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setNextPOKeyByZone(null);
        dataManager.putSignalProcessorCursor(actual);

        // get the value from the data store again after we have updated our copy
        SignalProcessorCursor actualAfterUpdate = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate);
    }

    @Test
    public void testMergeSignalProcessorCursor() {
        SignalProcessorCursor expected = getTestSignalProcessorCursor();
        dataManager.putSignalProcessorCursor(expected);

        // verify it is present and correct now
        SignalProcessorCursor updated = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, updated);

        // adding new values should be updated
        updated.getNextPOKeyByZone().put("777", "T9ECO3cwR/y+BDvgLFOm7Q");
        updated.getNextPOKeyByZone().put("888", "wCBrtTq9REm8DG3CyECcRA");
        expected.getNextPOKeyByZone().put("777", "T9ECO3cwR/y+BDvgLFOm7Q");
        expected.getNextPOKeyByZone().put("888", "wCBrtTq9REm8DG3CyECcRA");

        // changing existing values should not be updated (i.e. not in expected)
        updated.getNextPOKeyByZone().put("999", "fiI03m8NSz+nf9UMAF/Bgw");

        // merge the object
        dataManager.mergeSignalProcessorCursor(updated);

        // verify it is present and correct now
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testDeleteSignalProcessorCursor() {
        SignalProcessorCursor expected = getTestSignalProcessorCursor();
        dataManager.putSignalProcessorCursor(expected);

        // verify it is present and correct now
        SignalProcessorCursor actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        assertEqualsNotSame(expected, actual);

        // delete the object
        dataManager.deleteSignalProcessorCursor(expected.getAcquisitionPointIdentity());

        // verify it is not present now
        actual = dataManager.getSignalProcessorCursor(expected.getAcquisitionPointIdentity());
        Assert.assertNull("Object should not exist at this point", actual);
    }

    @Test
    public void testGetPlacementOpportunitySimple() {
        PlacementOpportunity expected = getTestPlacementOpportunitySingle();

        // make sure our test object is not present before we start
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        Assert.assertNull("Object should not exist at this point", actual);

        // now add the object
        dataManager.putPlacementOpportunity(expected);

        // verify it is present and correct now
        actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetPlacementOpportunityMultiple() {
        List<PlacementOpportunity> expectedList = new ArrayList<PlacementOpportunity>();
        for (int i = 0; i < 1000; i++) {
            PlacementOpportunity expected = getTestPlacementOpportunity();
            expected.setPOKey(expected.getPOKey() + i);
            expected.setNextPOKey(expected.getNextPOKey() + i);
            expected.setPlacementsDurationMilliseconds(i);
            expected.setUtcWindowStartTime(i);
            expected.setWindowDurationMilliseconds(i);
            expectedList.add(expected);
        }

        // make sure our test objects are not present before we start
        for (PlacementOpportunity expected : expectedList) {
            PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
            Assert.assertNull("Object should not exist at this point", actual);
        }

        // now add the objects
        for (PlacementOpportunity expected : expectedList) {
            dataManager.putPlacementOpportunity(expected);
        }

        // verify it is present and correct now
        for (PlacementOpportunity expected : expectedList) {
            PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
            assertEqualsNotSame(expected, actual);
        }
    }

    @Test
    public void testGetPlacementOpportunityIsolatedUpdates() {
        PlacementOpportunity expected = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(expected);

        // verify it is present and correct now
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);

        // verify that changes to the object returned do not affect the data store
        actual.setNextPOKey(null);
        actual.setPlacementsDurationMilliseconds(20);
        actual.setUtcWindowStartTime(500L);
        actual.setWindowDurationMilliseconds(2000);

        // get the value from the data store again after we have updated our copy
        PlacementOpportunity actualAfterUpdate = dataManager.getPlacementOpportunity(expected.getPOKey());

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate);
    }

    @Test
    public void testPutPlacementOpportunityIsolatedUpdates() {
        PlacementOpportunity original = getTestPlacementOpportunity();
        PlacementOpportunity expected = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(original);

        // verify that changes to the original object do not affect the data store
        original.setNextPOKey(null);
        original.setPlacementsDurationMilliseconds(20);
        original.setUtcWindowStartTime(500L);
        original.setWindowDurationMilliseconds(2000);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetPlacementOpportunityUpdate() {
        PlacementOpportunity expected = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(expected);

        // verify it is present and correct now
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setNextPOKey(null);
        actual.setPlacementsDurationMilliseconds(20);
        actual.setUtcWindowStartTime(500L);
        actual.setWindowDurationMilliseconds(2000);
        dataManager.putPlacementOpportunity(actual);

        // get the value from the data store again after we have updated our copy
        PlacementOpportunity actualAfterUpdate = dataManager.getPlacementOpportunity(expected.getPOKey());

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate);
    }

    @Test
    public void testCasPlacementOpportunitySucceeds() {
        PlacementOpportunity original = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(original);

        // get the po again so that we have the correct casId
        PlacementOpportunity expected = dataManager.getPlacementOpportunity(original.getPOKey());

        // make some changes and save them
        expected.setNextPOKey(null);
        expected.setPlacementsDurationMilliseconds(20);
        expected.setUtcWindowStartTime(500L);
        expected.setWindowDurationMilliseconds(2000);
        if (!dataManager.casPlacementOpportunity(expected)) {
            Assert.fail("CAS update did not succeed as expected");
        }

        // the object in the data store should look like the updated item
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testCasPlacementOpportunityNoCasIdFails() {
        PlacementOpportunity original = getTestPlacementOpportunity();
        PlacementOpportunity updated = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(updated);

        // we should not be able to just make more changes and save them
        // without retrieving the current version again (to get the new cas id)
        updated.setNextPOKey(null);
        updated.setPlacementsDurationMilliseconds(20);
        updated.setUtcWindowStartTime(500L);
        updated.setWindowDurationMilliseconds(2000);
        if (dataManager.casPlacementOpportunity(updated)) {
            Assert.fail("CAS update should have failed");
        }

        // the object in the data store should look like the original still
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(original.getPOKey());
        assertEqualsNotSame(original, actual);
    }

    @Test
    public void testCasPlacementOpportunityConcurrentUpdateFails() {
        PlacementOpportunity original = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(original);

        // get the po again so that we have the correct casId
        PlacementOpportunity updated1 = dataManager.getPlacementOpportunity(original.getPOKey());
        PlacementOpportunity updated2 = dataManager.getPlacementOpportunity(original.getPOKey());

        // make and save updates 
        updated1.setNextPOKey(null);
        updated1.setPlacementsDurationMilliseconds(20);
        updated1.setUtcWindowStartTime(500L);
        updated1.setWindowDurationMilliseconds(2000);
        if (!dataManager.casPlacementOpportunity(updated1)) {
            Assert.fail("CAS update should have succeeded");
        }

        // we should not be able to make changes and save them now that
        // another update has already been made to that version.
        updated2.setNextPOKey(null);
        updated2.setPlacementsDurationMilliseconds(30);
        updated2.setUtcWindowStartTime(300L);
        updated2.setWindowDurationMilliseconds(1000);
        if (dataManager.casPlacementOpportunity(updated2)) {
            Assert.fail("CAS update should have failed");
        }

        // the object in the data store should look like the first update still
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(updated1.getPOKey());
        assertEqualsNotSame(updated1, actual);
    }

    @Test
    public void testDeletePlacementOpportunity() {
        PlacementOpportunity expected = getTestPlacementOpportunity();
        dataManager.putPlacementOpportunity(expected);

        // verify it is present and correct now
        PlacementOpportunity actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        assertEqualsNotSame(expected, actual);

        // delete the object
        dataManager.deletePlacementOpportunity(expected.getPOKey());

        // verify it is not present now
        actual = dataManager.getPlacementOpportunity(expected.getPOKey());
        Assert.assertNull("Object should not exist at this point", actual);
    }

    @Test
    public void testGetConfirmedPlacementOpportunitySimple() {
        ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunitySingle();

        // make sure our test object is not present before we start
        ConfirmedPlacementOpportunity actual1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        Assert.assertNull("Object should not exist at this point", actual1);
        ConfirmedPlacementOpportunity actual2 = dataManager.getConfirmedPlacementOpportunity(
                expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        Assert.assertNull("Object should not exist at this point", actual2);

        // now add the object
        dataManager.putConfirmedPlacementOpportunity(expected);

        // verify it is present and correct now
        actual1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        assertEqualsNotSame(expected, actual1);
        actual2 = dataManager.getConfirmedPlacementOpportunity(expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        assertEqualsNotSame(expected, actual2);
    }

    @Test
    public void testGetConfirmedPlacementOpportunityMultiple() {
        List<ConfirmedPlacementOpportunity> expectedList = new ArrayList<ConfirmedPlacementOpportunity>();
        for (int i = 0; i < 1000; i++) {
            ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunity();
            expected.setAcquisitionPointIdentity(expected.getAcquisitionPointIdentity() + i);
            expected.setSignalId(expected.getSignalId() + i);
            expected.setUtcSignalTime(i);
            for (String key : expected.getPoKeyByZone().keySet()) {
                expected.getPoKeyByZone().put(key, expected.getPoKeyByZone().get(key) + i);
            }
            expectedList.add(expected);
        }

        // make sure our test objects are not present before we start
        for (ConfirmedPlacementOpportunity expected : expectedList) {
            ConfirmedPlacementOpportunity actual = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
            Assert.assertNull("Object should not exist at this point", actual);
        }

        // now add the objects
        for (ConfirmedPlacementOpportunity expected : expectedList) {
            dataManager.putConfirmedPlacementOpportunity(expected);
        }

        // verify it is present and correct now
        for (ConfirmedPlacementOpportunity expected : expectedList) {
            ConfirmedPlacementOpportunity actual1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
            assertEqualsNotSame(expected, actual1);
            ConfirmedPlacementOpportunity actual2 = dataManager.getConfirmedPlacementOpportunity(
                    expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
            assertEqualsNotSame(expected, actual2);
        }
    }

    @Test
    public void testGetConfirmedPlacementOpportunityIsolatedUpdates() {
        ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunity();
        dataManager.putConfirmedPlacementOpportunity(expected);

        // verify it is present and correct now
        ConfirmedPlacementOpportunity actual1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        assertEqualsNotSame(expected, actual1);
        ConfirmedPlacementOpportunity actual2 = dataManager.getConfirmedPlacementOpportunity(
                expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        assertEqualsNotSame(expected, actual2);

        // verify that changes to the object returned do not affect the data store
        actual1.setPoKeyByZone(null);
        actual2.setPoKeyByZone(null);

        // get the value from the data store again after we have updated our copy
        ConfirmedPlacementOpportunity actualAfterUpdate1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        ConfirmedPlacementOpportunity actualAfterUpdate2 = dataManager.getConfirmedPlacementOpportunity(
                expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate1);
        assertEqualsNotSame(expected, actualAfterUpdate2);
    }

    @Test
    public void testPutConfirmedPlacementOpportunityIsolatedUpdates() {
        ConfirmedPlacementOpportunity original = getTestConfirmedPlacementOpportunity();
        ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunity();
        dataManager.putConfirmedPlacementOpportunity(original);

        // verify that changes to the original object do not affect the data store
        original.setPoKeyByZone(null);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        ConfirmedPlacementOpportunity actual1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        assertEqualsNotSame(expected, actual1);
        ConfirmedPlacementOpportunity actual2 = dataManager.getConfirmedPlacementOpportunity(
                expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        assertEqualsNotSame(expected, actual2);
    }

    @Test
    public void testGetConfirmedPlacementOpportunityUpdate() {
        ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunity();
        dataManager.putConfirmedPlacementOpportunity(expected);

        // verify it is present and correct now
        ConfirmedPlacementOpportunity actual = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setPoKeyByZone(null);
        dataManager.putConfirmedPlacementOpportunity(actual);

        // get the value from the data store again after we have updated our copy
        ConfirmedPlacementOpportunity actualAfterUpdate1 = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        ConfirmedPlacementOpportunity actualAfterUpdate2 = dataManager.getConfirmedPlacementOpportunity(
                expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate1);
        assertEqualsNotSame(actual, actualAfterUpdate2);
    }

    @Test
    public void testDeleteConfirmedPlacementOpportunity() {
        ConfirmedPlacementOpportunity expected = getTestConfirmedPlacementOpportunity();
        dataManager.putConfirmedPlacementOpportunity(expected);

        // verify it is present and correct now
        ConfirmedPlacementOpportunity actual = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        assertEqualsNotSame(expected, actual);

        // verify it is present under the other key as well
        actual = dataManager.getConfirmedPlacementOpportunity(expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        assertEqualsNotSame(expected, actual);

        // delete the object
        dataManager.deleteConfirmedPlacementOpportunity(expected.getSignalId());

        // verify it is not present now
        actual = dataManager.getConfirmedPlacementOpportunity(expected.getSignalId());
        Assert.assertNull("Object should not exist at this point", actual);

        // verify it is not under the other key value either
        actual = dataManager.getConfirmedPlacementOpportunity(expected.getAcquisitionPointIdentity(), expected.getUtcSignalTime());
        Assert.assertNull("Object should not exist at this point", actual);
    }

    @Test
    public void testGetRuntimeEnvironmentStateSimple() {
        RuntimeEnvironmentState expected = getTestRuntimeEnvironmentState();

        // make sure our test object is not present before we start
        RuntimeEnvironmentState actual = dataManager.getRuntimeEnvironmentState();
        Assert.assertNull("Object should not exist at this point", actual);

        // now add the object
        dataManager.putRuntimeEnvironmentState(expected);

        // verify it is present and correct now
        actual = dataManager.getRuntimeEnvironmentState();
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetRuntimeEnvironmentStateIsolatedUpdates() {
        RuntimeEnvironmentState expected = getTestRuntimeEnvironmentState();
        dataManager.putRuntimeEnvironmentState(expected);

        // verify it is present and correct now
        RuntimeEnvironmentState actual = dataManager.getRuntimeEnvironmentState();
        assertEqualsNotSame(expected, actual);

        // verify that changes to the object returned do not affect the data store
        actual.setAcquisitionPointIdentities(null);

        // get the value from the data store again after we have updated our copy
        RuntimeEnvironmentState actualAfterUpdate = dataManager.getRuntimeEnvironmentState();

        // the object in the data store should look like the original without any
        // updates that we made to our local copy
        assertEqualsNotSame(expected, actualAfterUpdate);
    }

    @Test
    public void testPutRuntimeEnvironmentStateIsolatedUpdates() {
        RuntimeEnvironmentState original = getTestRuntimeEnvironmentState();
        RuntimeEnvironmentState expected = getTestRuntimeEnvironmentState();
        dataManager.putRuntimeEnvironmentState(original);

        // verify that changes to the original object do not affect the data store
        original.setAcquisitionPointIdentities(null);

        // the object in the data store should look like the original without any
        // updates that we made to the original version we stored
        RuntimeEnvironmentState actual = dataManager.getRuntimeEnvironmentState();
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testGetRuntimeEnvironmentStateUpdate() {
        RuntimeEnvironmentState expected = getTestRuntimeEnvironmentState();
        dataManager.putRuntimeEnvironmentState(expected);

        // verify it is present and correct now
        RuntimeEnvironmentState actual = dataManager.getRuntimeEnvironmentState();
        assertEqualsNotSame(expected, actual);

        // make some changes and save them
        actual.setAcquisitionPointIdentities(null);
        dataManager.putRuntimeEnvironmentState(actual);

        // get the value from the data store again after we have updated our copy
        RuntimeEnvironmentState actualAfterUpdate = dataManager.getRuntimeEnvironmentState();

        // the object in the data store should look like the updated version
        // since we saved the updates now
        assertEqualsNotSame(actual, actualAfterUpdate);
    }

    @Test
    public void testIdenticalKeys() {
        // use the same key for all of the objects to ensure that they do
        // not get mixed up in the data store.
        final String KEY = "KEY";

        // add some objects to the data store
        AcquisitionPoint ap = getTestAcquisitionPoint();
        ap.setAcquisitionPointIdentity(KEY);
        dataManager.putAcquisitionPoint(ap);
        LoaderCursor lc = getTestLoaderCursor();
        lc.setFeedExternalRef(KEY);
        dataManager.putLoaderCursor(lc);
        SignalProcessorCursor spc = getTestSignalProcessorCursor();
        spc.setAcquisitionPointIdentity(KEY);
        dataManager.putSignalProcessorCursor(spc);
        PlacementOpportunity po = getTestPlacementOpportunity();
        po.setPOKey(KEY);
        dataManager.putPlacementOpportunity(po);
        ConfirmedPlacementOpportunity cpo = getTestConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(KEY);
        cpo.setSignalId(KEY);
        dataManager.putConfirmedPlacementOpportunity(cpo);

        // verify all of our objects are present now
        AcquisitionPoint apActual = dataManager.getAcquisitionPoint(ap.getAcquisitionPointIdentity());
        assertEqualsNotSame(ap, apActual);
        LoaderCursor lcActual = dataManager.getLoaderCursor(lc.getFeedExternalRef());
        assertEqualsNotSame(lc, lcActual);
        SignalProcessorCursor spcActual = dataManager.getSignalProcessorCursor(spc.getAcquisitionPointIdentity());
        assertEqualsNotSame(spc, spcActual);
        PlacementOpportunity poActual = dataManager.getPlacementOpportunity(po.getPOKey());
        assertEqualsNotSame(po, poActual);
        ConfirmedPlacementOpportunity cpoActual1 = dataManager.getConfirmedPlacementOpportunity(cpo.getSignalId());
        assertEqualsNotSame(cpo, cpoActual1);
        ConfirmedPlacementOpportunity cpoActual2 = dataManager.getConfirmedPlacementOpportunity(cpo.getAcquisitionPointIdentity(),
                cpo.getUtcSignalTime());
        assertEqualsNotSame(cpo, cpoActual2);
    }

    @Test
    public void testLockSucceeds() {
        String lockName = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID();
        if (!dataManager.lock(lockName, 2)) {
            Assert.fail("Lock should have been successful");
        }
        dataManager.unlock(lockName);

        // now that it is unlocked we should be able to lock again
        if (!dataManager.lock(lockName, 2)) {
            Assert.fail("Lock should have been successful");
        }
        dataManager.unlock(lockName);
    }

    @Test
    public void testLockFails() {
        String lockName1 = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID();
        String lockName2 = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID();
        if (!dataManager.lock(lockName1, 2)) {
            Assert.fail("Lock should have been successful");
        }

        // we shouldn't be able to get the lock again with the same name
        if (dataManager.lock(lockName1, 2)) {
            Assert.fail("Lock should not have been successful");
        }

        // we should be able to get a lock with a different name however
        if (!dataManager.lock(lockName2, 2)) {
            Assert.fail("Lock should have been successful");
        }
        dataManager.unlock(lockName1);
        dataManager.unlock(lockName2);
    }

    @Test
    public void testLockExpires() {
        String lockName = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID();
        if (!dataManager.lock(lockName, 1)) {
            Assert.fail("Lock should have been successful");
        }

        // it should be locked at this point still so we can't lock again
        if (dataManager.lock(lockName, 1)) {
            Assert.fail("Lock should not have been successful");
        }

        // wait for the lock to expire
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        // now we should be able to lock again
        if (!dataManager.lock(lockName, 1)) {
            Assert.fail("Lock should have been successful");
        }
        dataManager.unlock(lockName);
    }

    @Test
    public void testDoubleUnlock() {
        String lockName = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID();
        if (!dataManager.lock(lockName, 2)) {
            Assert.fail("Lock should have been successful");
        }
        dataManager.unlock(lockName);

        // unlocking again should not cause any exceptions
        dataManager.unlock(lockName);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() throws IOException, ClassNotFoundException {
        Collection<Object[]> parameterSets = new ArrayList<Object[]>();

        // 1. test the couchbase data manager
        // this is more of an integration test since this involves the data manager 
        // implementation and couchbase
        DataManager dm = new DataManagerCouchbaseImpl();

        // set the expiration to something short so this test data will get purged quickly
        // note: if you run this test two times more quickly than this expiration time it will fail
        dm.setDefaultDataExpirationSeconds(10);
        parameterSets.add(new Object[] { dm });
        return parameterSets;
    }

}
