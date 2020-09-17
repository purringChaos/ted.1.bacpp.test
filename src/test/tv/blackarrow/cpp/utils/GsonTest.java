package test.tv.blackarrow.cpp.utils;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class GsonTest {

    private void assertNotEquals(Serializable expected, Serializable actual) {
        // verify that the values are different (use serialization to make it easier)
        byte[] expectedSerialized = SerializationUtils.serialize(expected);
        byte[] actualSerialized = SerializationUtils.serialize(actual);
        Assert.assertFalse("Object field values should be different", Arrays.equals(expectedSerialized, actualSerialized));
    }

    private void assertEqualsNotSame(Serializable expected, Serializable actual) {
        Assert.assertNotNull("Expected object should not be null", expected);
        Assert.assertNotNull("Actual object should not be null", actual);

        // verify it is a copy of our object and not the actual same reference
        Assert.assertNotSame("Objects should not be references to the exact same object", expected, actual);

        // verify that all the values are the same (use serialization to make it easier)
        byte[] expectedSerialized = SerializationUtils.serialize(expected);
        byte[] actualSerialized = SerializationUtils.serialize(actual);
        Assert.assertArrayEquals("Object field values should be the same", expectedSerialized, actualSerialized);
    }

    @Test
    public void testEqualsV1() {
        SampleObjectV1 expected1 = new SampleObjectV1(1);
        SampleObjectV1 expected2 = new SampleObjectV1(2);
        SampleObjectV1 actual1 = new SampleObjectV1(1);
        SampleObjectV1 actual2 = new SampleObjectV1(2);
        assertEqualsNotSame(expected1, actual1);
        assertEqualsNotSame(expected2, actual2);
        assertNotEquals(expected1, actual2);
        assertNotEquals(expected2, actual1);
    }

    @Test
    public void testEqualsV2() {
        SampleObjectV2 expected1 = new SampleObjectV2(1, true);
        SampleObjectV2 expected2 = new SampleObjectV2(2, true);
        SampleObjectV2 actual1 = new SampleObjectV2(1, true);
        SampleObjectV2 actual2 = new SampleObjectV2(2, true);
        assertEqualsNotSame(expected1, actual1);
        assertEqualsNotSame(expected2, actual2);
        assertNotEquals(expected1, actual2);
        assertNotEquals(expected2, actual1);
    }

    @Test
    public void testEqualsV3() {
        SampleObjectV3 expected1 = new SampleObjectV3(1, true);
        SampleObjectV3 expected2 = new SampleObjectV3(2, true);
        SampleObjectV3 actual1 = new SampleObjectV3(1, true);
        SampleObjectV3 actual2 = new SampleObjectV3(2, true);
        assertEqualsNotSame(expected1, actual1);
        assertEqualsNotSame(expected2, actual2);
        assertNotEquals(expected1, actual2);
        assertNotEquals(expected2, actual1);
    }

    @Test
    public void testToFromJson() {
        Gson gson = new Gson();
        SampleObjectV1 expected = new SampleObjectV1(1);
        String json = gson.toJson(expected);
        SampleObjectV1 actual = gson.fromJson(json, SampleObjectV1.class);
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testForwardCompatibility() {
        Gson gson = new Gson();

        // in the forward version, the version 2 fields will be populated but
        // this should not cause any problems for deserializing into version 1
        SampleObjectV2 forwardVersion = new SampleObjectV2(1, true);
        SampleObjectV1 expected = new SampleObjectV1(1);
        String json = gson.toJson(forwardVersion);

        // so now we have a version 2 json that we will deserialize into a version 1 object
        SampleObjectV1 actual = gson.fromJson(json, SampleObjectV1.class);
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testDoubleForwardCompatibility() {
        Gson gson = new Gson();

        // in the double forward version, the version 3 fields will be populated and 
        // the version 1 fields will be missing completely but
        // this should still not cause an error for deserializing into version 1
        SampleObjectV3 forwardVersion = new SampleObjectV3(1, true);

        // since the version 1 fields are now removed, the expected result should
        // be an empty version 1 object with no fields populated.  this may not be
        // a working scenario for the application still but it should not cause gson
        // to throw any errors regardless.
        SampleObjectV1 expected = new SampleObjectV1();
        String json = gson.toJson(forwardVersion);

        // so now we have a version 3 json that we will deserialize into a version 1 object
        SampleObjectV1 actual = gson.fromJson(json, SampleObjectV1.class);
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testBackwardCompatibility() {
        Gson gson = new Gson();
        SampleObjectV1 backwardVersion = new SampleObjectV1(1);

        // although it should be able to deserialize the json from version 1
        // into the object, the version 2 fields should not be populated
        SampleObjectV2 expected = new SampleObjectV2(1, false);
        String json = gson.toJson(backwardVersion);

        // so now we have a version 1 json that we will deserialize into a version 2 object
        SampleObjectV2 actual = gson.fromJson(json, SampleObjectV2.class);
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testDoubleBackwardCompatibility() {
        Gson gson = new Gson();
        SampleObjectV1 backwardVersion = new SampleObjectV1(1);

        // although it should be able to deserialize the json from version 1
        // into the object, all the fields are removed so it should be like a new 
        // instance with all fields set to their java default values
        SampleObjectV3 expected = new SampleObjectV3();
        String json = gson.toJson(backwardVersion);

        // so now we have a version 1 json that we will deserialize into a version 3 object
        SampleObjectV3 actual = gson.fromJson(json, SampleObjectV3.class);
        assertEqualsNotSame(expected, actual);
    }

    @Test
    public void testNull() {
        Gson gson = new Gson();
        String json = null;
        SampleObjectV1 actual = gson.fromJson(json, SampleObjectV1.class);
        Assert.assertNull(actual);
    }

}
