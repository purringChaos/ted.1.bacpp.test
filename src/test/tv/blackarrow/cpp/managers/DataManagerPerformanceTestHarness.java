package test.tv.blackarrow.cpp.managers;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl;
import tv.blackarrow.cpp.model.AcquisitionPoint;
import tv.blackarrow.cpp.model.ConfirmedPlacementOpportunity;
import tv.blackarrow.cpp.model.LoaderCursor;
import tv.blackarrow.cpp.model.PlacementOpportunity;
import tv.blackarrow.cpp.model.SignalProcessorCursor;
import tv.blackarrow.cpp.utils.UUIDUtils;

public class DataManagerPerformanceTestHarness {

    private static long lastGcTime = 0;
    private static String uniquePrefix;

    private static AcquisitionPoint getTestAcquisitionPoint(int id) {
        AcquisitionPoint ap = new AcquisitionPoint();
        ap.setAcquisitionPointIdentity(uniquePrefix + id);
        ap.setProviderExternalRef("test1.provider.com");
        ap.setSccDeleteEmptyBreak(true);
        return ap;
    }

    private static LoaderCursor getTestLoaderCursor(int id, int zones) {
        LoaderCursor lc = new LoaderCursor();
        lc.setFeedExternalRef(uniquePrefix + id);
        HashMap<String, String> lastPOKeyByZone = new HashMap<String, String>();
        for (int i = 0; i < zones; i++) {
            lastPOKeyByZone.put(Integer.toString(i), UUIDUtils.getBase64UrlEncodedUUID());
        }
        lc.setLastPOKeyByZone(lastPOKeyByZone);
        return lc;
    }

    private static SignalProcessorCursor getTestSignalProcessorCursor(int id, int zones) {
        SignalProcessorCursor spc = new SignalProcessorCursor();
        spc.setAcquisitionPointIdentity(uniquePrefix + id);
        HashMap<String, String> nextPOKeyByZone = new HashMap<String, String>();
        for (int i = 0; i < zones; i++) {
            nextPOKeyByZone.put(Integer.toString(i), UUIDUtils.getBase64UrlEncodedUUID());
        }
        spc.setNextPOKeyByZone(nextPOKeyByZone);
        return spc;
    }

    private static PlacementOpportunity getTestPlacementOpportunity(int id) {
        PlacementOpportunity po = new PlacementOpportunity();
        po.setPOKey(uniquePrefix + id);
        po.setUtcWindowStartTime(1352231572500L);
        po.setWindowDurationMilliseconds(600000);
        po.setPlacementsDurationMilliseconds(90000);
        po.setNextPOKey(UUIDUtils.getBase64UrlEncodedUUID());
        return po;
    }

    private static ConfirmedPlacementOpportunity getTestConfirmedPlacementOpportunity(int id, int zones) {
        ConfirmedPlacementOpportunity cpo = new ConfirmedPlacementOpportunity();
        cpo.setAcquisitionPointIdentity(uniquePrefix + id);

        // confirmed pos get stored by two keys, need to be sure the signal id is unique as well
        cpo.setSignalId(uniquePrefix + id);
        cpo.setUtcSignalTime(1352231850750L);
        HashMap<String, String> poKeyByZone = new HashMap<String, String>();
        for (int i = 0; i < zones; i++) {
            poKeyByZone.put(Integer.toString(i), UUIDUtils.getBase64UrlEncodedUUID());
        }
        cpo.setPoKeyByZone(poKeyByZone);
        return cpo;
    }

    private static long getGcDelta() {
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += gc.getCollectionTime();
        }
        long delta = gcTime - lastGcTime;
        lastGcTime = gcTime;
        return delta;
    }

    private static void reportResponseTimes(List<Long> responseTimesNano) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long total = 0;
        long count = responseTimesNano.size();
        long over1ms = 0;
        long over3ms = 0;
        long over5ms = 0;
        long over10ms = 0;
        long over50ms = 0;
        for (Long responseTime : responseTimesNano) {
            total += responseTime;
            if (responseTime < min) {
                min = responseTime;
            }
            if (responseTime > max) {
                max = responseTime;
            }
            if (responseTime > 1000000) {
                over1ms++;
                if (responseTime > 3000000) {
                    over3ms++;
                    if (responseTime > 5000000) {
                        over5ms++;
                        if (responseTime > 10000000) {
                            over10ms++;
                            if (responseTime > 50000000) {
                                over50ms++;
                            }
                        }
                    }
                }
            }
        }
        long avg = total / count;
        System.out.println("Response times (microseconds), min: " + (min / 1000) + " max: " + (max / 1000) + " avg: "
                + (avg / 1000));
        System.out.println("Count total: " + count + ", over 1ms: " + over1ms + " 3ms: " + over3ms + " 5ms: " + over5ms + " 10ms: "
                + over10ms + " 50ms: " + over50ms);
    }

    private static void testPutPerformance(DataManager dm, int feeds, int zones, int breaks, boolean forceGc) {
        System.out.println("Executing put performance test...");
        List<Long> durationsNano = new LinkedList<Long>();
        long start = System.currentTimeMillis();
        long startNano;
        long durationNano;
        long gcDuration;
        long gcTotalNano = 0;
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < feeds; i++) {
        //            LoaderCursor loaderCursor = getTestLoaderCursor(i, zones);
        //            startNano = System.nanoTime();
        //            dm.putLoaderCursor(loaderCursor);
        //            durations.add(System.nanoTime() - startNano);
        //            for (int j = 0; j < 2; j++) {
        //                AcquisitionPoint ap = getTestAcquisitionPoint(2 * i + j);
        //                startNano = System.nanoTime();
        //                dm.putAcquisitionPoint(ap);
        //                durations.add(System.nanoTime() - startNano);
        //                SignalProcessorCursor signalCursor = getTestSignalProcessorCursor(2 * i + j, zones);
        //                startNano = System.nanoTime();
        //                dm.putSignalProcessorCursor(signalCursor);
        //                durations.add(System.nanoTime() - startNano);
        //            }
        //        }
        // test for one hour load of breaks
        for (int i = 0; i < (feeds * zones * breaks); i++) {
            PlacementOpportunity po = getTestPlacementOpportunity(i);
            getGcDelta();
            startNano = System.nanoTime();
            dm.putPlacementOpportunity(po);
            durationNano = System.nanoTime() - startNano;
            gcDuration = getGcDelta();
            if (durationNano > 10000000) {
                System.out.println("Slow put for PO \"" + po.getPOKey() + "\" " + (durationNano / 1000000)
                        + "ms (garbage collector time: " + gcDuration + "ms)");
            }
            durationsNano.add(durationNano);
            if (forceGc) {
                startNano = System.nanoTime();
                if ((i % 5000) == 0) {
                    System.gc();
                }
                gcTotalNano += System.nanoTime() - startNano;
            }
        }
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < (feeds * breaks); i++) {
        //            ConfirmedPlacementOpportunity cpo = getTestConfirmedPlacementOpportunity(i, zones);
        //            startNano = System.nanoTime();
        //            dm.putConfirmedPlacementOpportunity(cpo);
        //            durations.add(System.nanoTime() - startNano);
        //        }
        long duration = System.currentTimeMillis() - start;
        reportResponseTimes(durationsNano);
        if (forceGc) {
            System.out.println("Total elapsed time: " + duration + "ms, excluding forced gc: "
                    + (duration - (gcTotalNano / 1000000)) + "ms");
        } else {
            System.out.println("Total elapsed time: " + duration + "ms");
        }
    }

    private static void testGetPerformance(DataManager dm, int feeds, int zones, int breaks, boolean forceGc) {
        System.out.println("Executing get performance test...");
        List<Long> durationsNano = new LinkedList<Long>();
        long start = System.currentTimeMillis();
        long startNano;
        long durationNano;
        long gcDuration;
        long gcTotalNano = 0;
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < feeds; i++) {
        //            LoaderCursor loaderCursor = getTestLoaderCursor(i, zones);
        //            startNano = System.nanoTime();
        //            if (dm.getLoaderCursor(loaderCursor.getFeedExternalRef()) == null) {
        //                System.err.println("Failed to get loader cursor " + i);
        //            }
        //            durations.add(System.nanoTime() - startNano);
        //            PurgerCursor purgerCursor = getTestPurgerCursor(i, zones);
        //            startNano = System.nanoTime();
        //            if (dm.getPurgerCursor(purgerCursor.getFeedExternalRef()) == null) {
        //                System.err.println("Failed to get purger cursor " + i);
        //            }
        //            durations.add(System.nanoTime() - startNano);
        //            for (int j = 0; j < 2; j++) {
        //                AcquisitionPoint ap = getTestAcquisitionPoint(2 * i + j);
        //                startNano = System.nanoTime();
        //                if (dm.getAcquisitionPoint(ap.getAcquisitionPointIdentity()) == null) {
        //                    System.err.println("Failed to get acquisition point " + (2 * i + j));
        //                }
        //                durations.add(System.nanoTime() - startNano);
        //                startNano = System.nanoTime();
        //                if (dm.getSignalProcessorCursor(ap.getAcquisitionPointIdentity()) == null) {
        //                    System.err.println("Failed to get signal processor cursor " + (2 * i + j));
        //                }
        //                durations.add(System.nanoTime() - startNano);
        //            }
        //        }
        // test for one hour of breaks
        for (int i = 0; i < (feeds * zones * breaks); i++) {
            String poKey = uniquePrefix + i;
            getGcDelta();
            startNano = System.nanoTime();
            if (dm.getPlacementOpportunity(poKey) == null) {
                System.err.println("Failed to get po " + i);
            }
            durationNano = System.nanoTime() - startNano;
            gcDuration = getGcDelta();
            if (durationNano > 10000000) {
                System.out.println("Slow get for PO \"" + poKey + "\" " + (durationNano / 1000000) + "ms (garbage collector time: "
                        + gcDuration + "ms)");
            }
            durationsNano.add(durationNano);
            if (forceGc) {
                startNano = System.nanoTime();
                if ((i % 5000) == 0) {
                    System.gc();
                }
                gcTotalNano += System.nanoTime() - startNano;
            }
        }
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < (feeds * breaks); i++) {
        //            ConfirmedPlacementOpportunity cpo = getTestConfirmedPlacementOpportunity(i, zones);
        //            startNano = System.nanoTime();
        //            if (dm.getConfirmedPlacementOpportunity(cpo.getSignalId()) == null) {
        //                System.err.println("Failed to get confirmed po " + i);
        //            }
        //            durations.add(System.nanoTime() - startNano);
        //        }
        long duration = System.currentTimeMillis() - start;
        reportResponseTimes(durationsNano);
        if (forceGc) {
            System.out.println("Total elapsed time: " + duration + "ms, excluding forced gc: "
                    + (duration - (gcTotalNano / 1000000)) + "ms");
        } else {
            System.out.println("Total elapsed time: " + duration + "ms");
        }
    }

    private static void testCasPerformance(DataManager dm, int feeds, int zones, int breaks, boolean forceGc) {
        System.out.println("Executing cas performance test...");
        List<Long> durationsNano = new LinkedList<Long>();
        long start = System.currentTimeMillis();
        long startNano;
        long durationNano;
        long gcDuration;
        long gcTotalNano = 0;
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < feeds; i++) {
        //            LoaderCursor loaderCursor = getTestLoaderCursor(i, zones);
        //            startNano = System.nanoTime();
        //            dm.putLoaderCursor(loaderCursor);
        //            durations.add(System.nanoTime() - startNano);
        //            for (int j = 0; j < 2; j++) {
        //                AcquisitionPoint ap = getTestAcquisitionPoint(2 * i + j);
        //                startNano = System.nanoTime();
        //                dm.putAcquisitionPoint(ap);
        //                durations.add(System.nanoTime() - startNano);
        //                SignalProcessorCursor signalCursor = getTestSignalProcessorCursor(2 * i + j, zones);
        //                startNano = System.nanoTime();
        //                dm.putSignalProcessorCursor(signalCursor);
        //                durations.add(System.nanoTime() - startNano);
        //            }
        //        }
        // test for one hour load of breaks
        for (int i = 0; i < (feeds * zones * breaks); i++) {
            String poKey = uniquePrefix + i;
            PlacementOpportunity po = dm.getPlacementOpportunity(poKey);
            if (po == null) {
                System.err.println("Failed to get po " + i);
            }
            getGcDelta();
            startNano = System.nanoTime();
            if (!dm.casPlacementOpportunity(po)) {
                System.err.println("Failed to cas po " + i);
            }
            durationNano = System.nanoTime() - startNano;
            gcDuration = getGcDelta();
            if (durationNano > 10000000) {
                System.out.println("Slow cas for PO \"" + po.getPOKey() + "\" " + (durationNano / 1000000)
                        + "ms (garbage collector time: " + gcDuration + "ms)");
            }
            durationsNano.add(durationNano);
            if (forceGc) {
                startNano = System.nanoTime();
                if ((i % 5000) == 0) {
                    System.gc();
                }
                gcTotalNano += System.nanoTime() - startNano;
            }
        }
        // FIXME restore the tests for these other types
        //        for (int i = 0; i < (feeds * breaks); i++) {
        //            ConfirmedPlacementOpportunity cpo = getTestConfirmedPlacementOpportunity(i, zones);
        //            startNano = System.nanoTime();
        //            dm.putConfirmedPlacementOpportunity(cpo);
        //            durations.add(System.nanoTime() - startNano);
        //        }
        long duration = System.currentTimeMillis() - start;
        reportResponseTimes(durationsNano);
        if (forceGc) {
            System.out.println("Total elapsed time: " + duration + "ms, excluding forced gc: "
                    + (duration - (gcTotalNano / 1000000)) + "ms");
        } else {
            System.out.println("Total elapsed time: " + duration + "ms");
        }
    }

    private static void testPerformance(DataManager dm, boolean forceGc) {
        // set a unique prefix for each test so previous test data does not interfere 
        uniquePrefix = "TEST_" + UUIDUtils.getBase64UrlEncodedUUID() + "_";
        System.out.println("============================================================");
        if (forceGc) {
            System.out.println("Starting performance test (with periodic forced gc)...");
        } else {
            System.out.println("Starting performance test (without periodic forced gc)...");
        }
        System.out.println("Forcing full garbage collection...");

        // set the expiration period to something short so data from this test will
        // be purged quickly.  we're using unique ids for everything in this
        // test so you can run it more frequently than this without problems.
        dm.setDefaultDataExpirationSeconds(5 * 60);

        // force a full garbage collection to cleanup after any previous tests
        System.gc();

        // test model settings based on one hour max requirements
        final int FEEDS = 80;
        final int ZONES = 600;
        final int BREAKS = 6;
        testPutPerformance(dm, FEEDS, ZONES, BREAKS, forceGc);
        testGetPerformance(dm, FEEDS, ZONES, BREAKS, forceGc);
        testCasPerformance(dm, FEEDS, ZONES, BREAKS, forceGc);
    }

    private static void testJsonOnly() {
        System.out.println("============================================================");
        System.out.println("Test with json only...");
        testPerformance(new DataManagerJsonOnlyImpl(), true);
        testPerformance(new DataManagerJsonOnlyImpl(), false);
    }

    private static void testCouchbase() {
        System.out.println("============================================================");
        System.out.println("Test with couchbase...");
        testPerformance(new DataManagerCouchbaseImpl(), true);
        testPerformance(new DataManagerCouchbaseImpl(), false);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // testJsonOnly();
        testCouchbase();
    }

}
