package test.tv.blackarrow.cpp.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//only using serializable to make junit testing easier, not required for actual classes
@SuppressWarnings("serial")
public class SampleNestedObjectV1 implements Serializable {
    
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

    public SampleNestedObjectV1() {
    }

    public SampleNestedObjectV1(int id) {
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
    }

}
