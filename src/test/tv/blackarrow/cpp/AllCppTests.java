package test.tv.blackarrow.cpp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.tv.blackarrow.cpp.components.AllComponentTests;
import test.tv.blackarrow.cpp.managers.AllManagerTests;
import test.tv.blackarrow.cpp.model.AllModelTests;
import test.tv.blackarrow.cpp.purger.AllPurgerTests;
import test.tv.blackarrow.cpp.utils.AllUtilTests;

@RunWith(Suite.class)
@SuiteClasses({ AllComponentTests.class, AllManagerTests.class, AllModelTests.class, AllPurgerTests.class, AllUtilTests.class })
public class AllCppTests {
}
