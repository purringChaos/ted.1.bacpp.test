/**
 * 
 */
package tv.blackarrow.cpp.loader.serverconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;
import tv.blackarrow.cpp.model.ServerConfig;

/**
 * @author akumar
 *
 */
public class LinearServerConfigDataFileLoader {
	
	private static final Logger LOGGER = LogManager.getLogger(LinearServerConfigDataFileLoader.class);
    private static final Gson gson = new Gson();
    
    static {
    	gson.fromJson(gson.toJson(new ServerConfig()), ServerConfig.class);//Just warmup.
    }
    
	public void load(){
		LOGGER.info(()->"Loading server configuration file if exists.");
		
		final File dataDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir());
        if (!dataDir.exists()) {
        	LOGGER.error(()->"ESS data directory 'inprocess' does not exist, skip loading.");
            return;
        }
        loadInPersistenceStore(dataDir);
        LOGGER.info(()->"Loading server configuration file finished.");
	}
	
	private void loadInPersistenceStore(final File dataDir) {
		File servConfigFile = null;
		InputStream inputStream = null;
		try {
			servConfigFile = getDataFileToLoad(dataDir);
			if(servConfigFile != null && servConfigFile.exists()){
				inputStream = new FileInputStream(servConfigFile);
				String serverConfigJson = IOUtils.toString(inputStream);
				if(StringUtils.isNotBlank(serverConfigJson)){
					ServerConfig latestServerConfigReceived = gson.fromJson(serverConfigJson, ServerConfig.class);
					ServerConfig persistedServerConfig = DataManagerFactory.getInstance().getServerConfig();
					if(persistedServerConfig == null || (latestServerConfigReceived != null && latestServerConfigReceived.getLastUpdated() > persistedServerConfig.getLastUpdated())){
						DataManagerFactory.getInstance().putActiveDataCenter(serverConfigJson);
					}
				}
				//Delete the file from the inprocess directory.
				FileUtils.deleteQuietly(servConfigFile);
			}
		} catch (Exception ex){
			LOGGER.error(()->"Unexpected error occured while laoding the server configuration file.", ex);
		} finally {
			if(inputStream!=null){
				try {
					inputStream.close();
				} catch (IOException e) {
					LOGGER.error(()->"Unexpected error occured while laoding the server configuration file.", e);
				}
			}
		}
	}
	
    private File getDataFileToLoad(final File dataDir) {
        File[] datafiles = dataDir.listFiles(ServerConfigJsonFileFilter.instance);
        if (datafiles != null && datafiles.length > 0) {        
        	Arrays.sort(datafiles);
            for (int i = 0; i < datafiles.length - 1; i++) {
            	LoaderUtil.moveFile(datafiles[i], LoaderUtil.getProcessedDirectory(), datafiles[i].getName() + ".old");
            }
            return datafiles[datafiles.length-1];
        }
        return null;
    }
	
}

class ServerConfigJsonFileFilter implements FilenameFilter {
	
	public static final FilenameFilter instance = new ServerConfigJsonFileFilter();
	
	private ServerConfigJsonFileFilter(){}

	public static final String SERVER_CONFIG_DATA_FILE_SUFFIX = "_server_config.json";
	
	@Override
	public boolean accept(File dir, String name) {
		return name.endsWith(SERVER_CONFIG_DATA_FILE_SUFFIX); 
	}
	
}
