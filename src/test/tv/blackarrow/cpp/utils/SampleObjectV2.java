package test.tv.blackarrow.cpp.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//only using serializable to make junit testing easier, not required for actual classes
@SuppressWarnings("serial")
public class SampleObjectV2 implements Serializable{

    // these are the data types that are in use at this time, if new types are to be used
    // then they should be added to this and the other sample objects and tested.  for example,
    // the java.util.Date type would be a problem using the default serialization so if
    // you intend to use that then you will need to configure Gson explicitly to handle it.
    //
    // note: generics will cause issues since Gson will actually instantiate the concrete class
    // during deserialization.  during testing, it was noted that Map will be a LinkedTreeMap
    // instead of the more common HashMap,  Set will be a LinkedHashSet.  This may not cause
    // any functional issue but it could impact performance.  for now, we will change to use
    // concrete implementations in our pojo value objects.
    @SuppressWarnings("unused")
    private String stringValue;
    @SuppressWarnings("unused")
    private int intValue;
    @SuppressWarnings("unused")
    private long longValue;
    @SuppressWarnings("unused")
    private boolean booleanValue;
    @SuppressWarnings("unused")
    private Integer integerValue;
    private ArrayList<String> listStringValue;
    private HashSet<String> setStringValue;
    private HashMap<String, String> mapStringToStringValue;
    @SuppressWarnings("unused")
    private SampleNestedObjectV1 nestedObject;
    private ArrayList<SampleNestedObjectV1> listNestedObjects;

    // add new fields like what version 2 may need to add
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
    @SuppressWarnings("unused")
    private SampleNestedObjectV2 nestedObjectV2;
    private ArrayList<SampleNestedObjectV2> listNestedObjectsV2;

    public SampleObjectV2() {
    }

    public SampleObjectV2(int id, boolean populateV2Fields) {
        stringValue = "Value " + id;
        intValue = id;
        longValue = id + 1000000000000L;
        booleanValue = ((id % 2) == 0) ? true : false;
        integerValue = Integer.valueOf(id);
        listStringValue = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            listStringValue.add("Value " + (id + i));
        }
        setStringValue = new HashSet<String>();
        for (int i = 0; i < 3; i++) {
            setStringValue.add("Value " + (id + i));
        }
        mapStringToStringValue = new HashMap<String, String>();
        for (int i = 0; i < 3; i++) {
            mapStringToStringValue.put("Key " + (id + i), "Value " + (id + i));
        }
        nestedObject = new SampleNestedObjectV1(id);
        listNestedObjects = new ArrayList<SampleNestedObjectV1>();
        for (int i = 0; i < 3; i++) {
            listNestedObjects.add(new SampleNestedObjectV1(i));
        }
        if (populateV2Fields) {
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
            nestedObjectV2 = new SampleNestedObjectV2(id, populateV2Fields);
            listNestedObjectsV2 = new ArrayList<SampleNestedObjectV2>();
            for (int i = 0; i < 3; i++) {
                listNestedObjectsV2.add(new SampleNestedObjectV2(i, populateV2Fields));
            }
        }
    }

}
