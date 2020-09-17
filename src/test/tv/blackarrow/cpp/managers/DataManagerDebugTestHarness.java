package test.tv.blackarrow.cpp.managers;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.PurgerCursor;
import tv.blackarrow.cpp.model.RuntimeEnvironmentState;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.PersistentHashMap;

public class DataManagerDebugTestHarness {

    private static File getUnitTestingPersistenceDirectory() {
        return new File("/opt/blackarrow/pois_linear/dat/debug/");
    }

    private static PersistentHashMap<String, Serializable> getMap() throws IOException, ClassNotFoundException {
        PersistentHashMap<String, Serializable> map = new PersistentHashMap<String, Serializable>(
                getUnitTestingPersistenceDirectory());
        return map;
    }

    private static void print(AcquisitionPoint ap) {
        System.out.println("acquisitionPointIdentity: " + ap.getAcquisitionPointIdentity());
        System.out.println("feedExternalRef: " + ap.getFeedExternalRef());
        System.out.println("networkExternalRef: " + ap.getNetworkExternalRef());
        System.out.println("providerExternalRef: " + ap.getProviderExternalRef());
    }

    private static void print(PlacementOpportunity po) {
        System.out.println("poKey: " + po.getPOKey());
        System.out.println("nextPOKey: " + po.getNextPOKey());
        System.out.println("utcWindowStartTime: " + po.getUtcWindowStartTime());
        System.out.println("windowDurationMilliseconds: " + po.getWindowDurationMilliseconds());
        System.out.println("placementsDurationMilliseconds: " + po.getPlacementsDurationMilliseconds());
    }

    private static void print(PurgerCursor pc) {
        System.out.println("feedExternalRef: " + pc.getFeedExternalRef());
        System.out.println("firstPOKeyByZone: " + pc.getFirstPOKeyByZone());
        System.out.println("poKeysToBePurged: " + pc.getPoKeysToBePurged());
    }

    private static void print(LoaderCursor lc) {
        System.out.println("feedExternalRef: " + lc.getFeedExternalRef());
        System.out.println("lastPOKeyByZone: " + lc.getLastPOKeyByZone());
    }

    private static void print(SignalProcessorCursor spc) {
        System.out.println("acquisitionPointIdentity: " + spc.getAcquisitionPointIdentity());
        System.out.println("nextPOKeyByZone: " + spc.getNextPOKeyByZone());
        System.out.println("lastProcessedPOKeyByZone: " + spc.getLastProcessedPOKeyByZone());
    }

    private static void print(RuntimeEnvironmentState res) {
        System.out.println("acquisitionPointIdentities: " + res.getAcquisitionPointIdentities());
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        PersistentHashMap<String, Serializable> map = getMap();
        List<String> sortedKeys = new ArrayList<String>(map.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            Serializable value = map.get(key);
            System.out.println("key: " + key);
            if (value instanceof AcquisitionPoint) {
                print((AcquisitionPoint) value);
            } else if (value instanceof PlacementOpportunity) {
                print((PlacementOpportunity) value);
            } else if (value instanceof PurgerCursor) {
                print((PurgerCursor) value);
            } else if (value instanceof LoaderCursor) {
                print((LoaderCursor) value);
            } else if (value instanceof SignalProcessorCursor) {
                print((SignalProcessorCursor) value);
            } else if (value instanceof RuntimeEnvironmentState) {
                print((RuntimeEnvironmentState) value);
            }
            System.out.println();
        }
    }
}
