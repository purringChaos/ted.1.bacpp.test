package tv.blackarrow.cpp.model;

import java.util.HashMap;
import java.util.Map;

public class LoaderCursor {

    private String feedExternalRef;

    // intentionally using a concrete type here so that deserialization works cleanly
    private HashMap<String, String> lastPOKeyByZone;
    private String lastLoadedDataFile;
    private Long casId;

    public String getFeedExternalRef() {
        return feedExternalRef;
    }

    public void setFeedExternalRef(String feedExternalRef) {
        this.feedExternalRef = feedExternalRef;
    }

    public Map<String, String> getLastPOKeyByZone() {
        return lastPOKeyByZone;
    }

    public void setLastPOKeyByZone(HashMap<String, String> lastPOKeyByZone) {
        this.lastPOKeyByZone = lastPOKeyByZone;
    }

    public String getLastLoadedDataFile() {
        return lastLoadedDataFile;
    }

    public void setLastLoadedDataFile(String lastLoadedDataFile) {
        this.lastLoadedDataFile = lastLoadedDataFile;
    }

    public Long getCasId() {
        return casId;
    }

    public void setCasId(Long casId) {
        this.casId = casId;
    }

}
