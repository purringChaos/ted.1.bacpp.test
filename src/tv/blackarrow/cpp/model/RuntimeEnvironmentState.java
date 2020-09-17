package tv.blackarrow.cpp.model;

import java.util.HashSet;

public class RuntimeEnvironmentState {

    // intentionally using a concrete type here so that deserialization works cleanly
    private HashSet<String> acquisitionPointIdentities;

    public HashSet<String> getAcquisitionPointIdentities() {
        return acquisitionPointIdentities;
    }

    public void setAcquisitionPointIdentities(HashSet<String> acquisitionPointIdentities) {
        this.acquisitionPointIdentities = acquisitionPointIdentities;
    }

}
