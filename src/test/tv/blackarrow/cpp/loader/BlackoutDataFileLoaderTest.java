package test.tv.blackarrow.cpp.loader;

import org.junit.Test;

import tv.blackarrow.cpp.loader.bo.LinearBlackoutDataFileLoader;

public class BlackoutDataFileLoaderTest {
	
	@Test
	public void testBlackoutDataFileLoader() {
		 LinearBlackoutDataFileLoader proc = new LinearBlackoutDataFileLoader();
		
		proc.load();
	}

}
