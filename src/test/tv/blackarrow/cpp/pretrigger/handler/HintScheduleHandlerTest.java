package test.tv.blackarrow.cpp.pretrigger.handler;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tv.blackarrow.cpp.pretrigger.handler.HintScheduleHandler;

public class HintScheduleHandlerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetInstance() {
		HintScheduleHandler instance1 = HintScheduleHandler.getInstance();
		HintScheduleHandler instance2 = HintScheduleHandler.getInstance();
		assertTrue(instance1 == instance2);
	}

	@Test
	public void testParseHintSchedule() {
	}

	@Test
	public void testObtainHintSchedule() {
	}

	@Test
	public void testGetQueryTimer() {
	}

}
