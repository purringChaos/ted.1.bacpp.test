package tv.blackarrow.cpp.utils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class JavaxUtil {
	static private DatatypeFactory datatypeFactory = null;
	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	static public DatatypeFactory getDatatypeFactory()  throws DatatypeConfigurationException{
		return datatypeFactory;
	}
}
