package test.tv.blackarrow.cpp.utils;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ Scte35BinaryUtilTest.class, DecoderTest.class, GsonTest.class})
public class AllUtilTests {
}
