package tv.blackarrow.cpp.loader;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public class LoaderUtil {
    private static final Logger LOGGER = LogManager.getLogger(LoaderUtil.class);


    static public File ensureDirectory(String dir) {
        File dirFile = new File(dir);
        if (!dirFile.exists() &&  !dirFile.mkdirs()) {
            LOGGER.fatal(()->"Fail to creatae directory: " + dirFile.getAbsolutePath());
        }
    	return dirFile;
    }
    
    static public String getProcessedDirectory() {
    	String dir = CppConfigurationBean.getInstance().getPoProcessedDir();
    	return dir;
    }
    
    static public String getInProcessDirectory() {
    	return CppConfigurationBean.getInstance().getPoInProcessDir();
    }
    
    static public String getErrorDirectory() {
    	return CppConfigurationBean.getInstance().getPoErrorDir();
    }

    static public void moveFile(File fromfile, String todir, String toname) {
    	ensureDirectory(todir);
    	File tofile = new File(todir + File.separator + toname);
    	if (!fromfile.renameTo(tofile))
            LOGGER.error(()->"Failed to move data file " + fromfile.getAbsolutePath() + " to " + tofile.getAbsolutePath());
    	else 
            LOGGER.debug(()->"Move data file " + fromfile.getAbsolutePath() + " to " + tofile.getAbsolutePath());    		    		
    }

    static public void moveFile(String from, String todir, String toname) {
    	File fromFile = new File(from);
    	moveFile(fromFile, todir, toname);
    }

    static public void deleteFile(String dir, String filename) {
    	String path = dir + File.separator + filename;
    	File file = new File(path);
    	if (!file.delete())
    		LOGGER.error(()->"Failed to delete data file: " + file.getAbsolutePath());
    		
    }

    static public String getFeedDataDirectory(String feedName) {
        return CppConfigurationBean.getInstance().getPoInProcessDir() + File.separator + feedName;
    }
    

}
