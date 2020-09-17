package tv.blackarrow.cpp.loader.po;

/** po data file filter with the format of <runtime env id>_<version>_<compile time in yyyyMMDDHHMM>_placement_opportunity.zip
 * @author pzhang 
 */
import tv.blackarrow.cpp.model.CppConfigurationBean;

public class LinearPODataFileFilter  extends LinearDataFileFilter {
    // if the format of the data file changes then this needs to be updated
    // along with the compiler version to support runtime separation properly
    private static int PO_DATA_FILE_VERSION_NUMBER = 1;

	
    private String prefix = CppConfigurationBean.getInstance().getLinearRunTimeID() + "_" + PO_DATA_FILE_VERSION_NUMBER + "_";
    private String suffix = "placement_opportunity.zip";
    
	@Override
	protected String getFilePrefix() {
		return prefix;
	}
	@Override
	protected String getFileSuffix() {
		return suffix;
	}

}
