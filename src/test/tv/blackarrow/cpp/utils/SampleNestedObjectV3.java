package test.tv.blackarrow.cpp.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//only using serializable to make junit testing easier, not required for actual classes
@SuppressWarnings("serial")
public class SampleNestedObjectV3 implements Serializable{

    // these are the data types that are in use at this time, if new types are to be used
    // then they should be added to this and the other sample objects and tested.  for example,
    // the java.util.Date type would be a problem using the default serialization so if
    // you intend to use that then you will need to configure Gson explicitly to handle it.

    // all of the version 1 fields have now been removed and version 2 fields remain
    @SuppressWarnings("unused")
    private String stringValueV2;
    @SuppressWarnings("unused")
    private int intValueV2;
    @SuppressWarnings("unused")
    private long longValueV2;
    @SuppressWarnings("unused")
    private boolean booleanValueV2;
    @SuppressWarnings("unused")
    private Integer integerValueV2;
    private ArrayList<String> listStringValueV2;
    private HashSet<String> setStringValueV2;
    private HashMap<String, String> mapStringToStringValueV2;

    // add new fields like what version 3 may need to add
    @SuppressWarnings("unused")
    private String stringValueV3;
    @SuppressWarnings("unused")
    private int intValueV3;
    @SuppressWarnings("unused")
    private long longValueV3;
    @SuppressWarnings("unused")
    private boolean booleanValueV3;
    @SuppressWarnings("unused")
    private Integer integerValueV3;
    private ArrayList<String> listStringValueV3;
    private HashSet<String> setStringValueV3;
    private HashMap<String, String> mapStringToStringValueV3;

    public SampleNestedObjectV3() {
    }

    public SampleNestedObjectV3(int id, boolean populateV3Fields) {
        stringValueV2 = "Value V2 " + id;
        intValueV2 = id + 1000000;
        longValueV2 = id + 2000000000000L;
        booleanValueV2 = ((id % 2) == 0) ? false : true;
        integerValueV2 = Integer.valueOf(id + 1000000);
        listStringValueV2 = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            listStringValueV2.add("Value V2 " + (id + i));
        }
        setStringValueV2 = new HashSet<String>();
        for (int i = 0; i < 3; i++) {
            setStringValueV2.add("Value V2 " + (id + i));
        }
        mapStringToStringValueV2 = new HashMap<String, String>();
        for (int i = 0; i < 3; i++) {
            mapStringToStringValueV2.put("Key V2 " + (id + i), "Value V2 " + (id + i));
        }
        if (populateV3Fields) {
            stringValueV3 = "Value V3 " + id;
            intValueV3 = id + 1000000;
            longValueV3 = id + 2000000000000L;
            booleanValueV3 = ((id % 2) == 0) ? false : true;
            integerValueV3 = Integer.valueOf(id + 1000000);
            listStringValueV3 = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                listStringValueV3.add("Value V3 " + (id + i));
            }
            setStringValueV3 = new HashSet<String>();
            for (int i = 0; i < 3; i++) {
                setStringValueV3.add("Value V3 " + (id + i));
            }
            mapStringToStringValueV3 = new HashMap<String, String>();
            for (int i = 0; i < 3; i++) {
                mapStringToStringValueV3.put("Key V3 " + (id + i), "Value V3 " + (id + i));
            }
        }
    }

}
