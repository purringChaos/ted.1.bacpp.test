package tv.blackarrow.cpp.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PurgerCursor implements Serializable {

    private static final long serialVersionUID = 6971523357380011678L;
    private String feedExternalRef;
    private Map<String, String> firstPOKeyByZone;
    private Set<String> poKeysToBePurged;

    public PurgerCursor() {
    }

    /**
     * Copy constructor to get a deep copy of this object. Be sure to update
     * this constructor if you add fields to this object.
     * 
     * @param source
     *            Source object to use for the deep copy.
     */
    public PurgerCursor(PurgerCursor source) {
        feedExternalRef = source.feedExternalRef;

        // All of the maps keys and entries are immutable strings so we don't need 
        // to make copies of those, only the map itself.
        if (source.firstPOKeyByZone != null) {
            firstPOKeyByZone = new HashMap<String, String>(source.firstPOKeyByZone);
        }
    }

    public String getFeedExternalRef() {
        return feedExternalRef;
    }

    public void setFeedExternalRef(String feedExternalRef) {
        this.feedExternalRef = feedExternalRef;
    }

    public Map<String, String> getFirstPOKeyByZone() {
        return firstPOKeyByZone;
    }

    public void setFirstPOKeyByZone(Map<String, String> firstPOKeyByZone) {
        this.firstPOKeyByZone = firstPOKeyByZone;
    }

    public Set<String> getPoKeysToBePurged() {
        return poKeysToBePurged;
    }

    public void setPoKeysToBePurged(Set<String> poKeysToBePurged) {
        this.poKeysToBePurged = poKeysToBePurged;
    }

}
