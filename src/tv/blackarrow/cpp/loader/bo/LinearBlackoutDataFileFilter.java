package tv.blackarrow.cpp.loader.bo;
import tv.blackarrow.cpp.loader.po.LinearDataFileFilter;
/**
 * blackout data file filter on the format of <runtime env id>_<version>_<compile time in yyyyMMDDHHMM>_blackout.zip
 * @author pzhang 
 */
import tv.blackarrow.cpp.model.CppConfigurationBean;

public class LinearBlackoutDataFileFilter extends LinearDataFileFilter {
    // if the format of the data file changes then this needs to be updated
    // along with the compiler version to support runtime separation properly
    final private static int BLACKOUT_DATA_FILE_VERSION_NUMBER = 1;

    final static private String prefix = CppConfigurationBean.getInstance().getLinearRunTimeID() + "_" + BLACKOUT_DATA_FILE_VERSION_NUMBER + "_";
    final static private String suffix = "blackout.zip";
    
	@Override
	protected String getFilePrefix() {
		return prefix;
	}
	@Override
	protected String getFileSuffix() {
		return suffix;
	}

}
