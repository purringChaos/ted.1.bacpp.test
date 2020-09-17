package test.tv.blackarrow.cpp.managers;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DataManagerTest.class, DataManagerCouchbaseImplTest.class })
public class AllManagerTests {
}
