/**
 * 
 */
package tv.blackarrow.cpp.loader.serverconfig;

import tv.blackarrow.cpp.loader.po.LinearDataFileFilter;
import tv.blackarrow.cpp.model.CppConfigurationBean;


/**
 * @author akumar
 *
 */
public class LinearServerConfigDataFileFilter extends LinearDataFileFilter {
	
    // if the format of the configuration file changes then this needs to be updated along with the compiler version to support runtime separation properly.
    final private static int SERVER_CONFIG_DATA_FILE_VERSION_NUMBER = 1;

    final static private String prefix = CppConfigurationBean.getInstance().getLinearRunTimeID() + "_" + SERVER_CONFIG_DATA_FILE_VERSION_NUMBER + "_";
    final static private String suffix = "server_config.zip";

	@Override
	protected String getFilePrefix() {
		return prefix;
	}

	@Override
	protected String getFileSuffix() {
		return suffix;
	}

}
