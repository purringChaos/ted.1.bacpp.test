package tv.blackarrow.cpp.loader.bo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.loader.DataFileProcessor;
import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;



/**
 * unpack blackout data files to inprocess directory, and move the file to processed directory if successful, or to error directory if failed
 * in case of multiple data files, only the latest file is processed, and the rest is moved to processed directory with .old extension
 * @author pzhang
 * 
 */
public class LinearBlackoutDataFileProcessor implements DataFileProcessor {

    private static final Logger LOGGER = LogManager.getLogger(LinearBlackoutDataFileProcessor.class);
	private static final String SUPPORTED_VERSION = "1";
	private static final String ZIP_FILE_EXTENSION = ".zip";
	private static final String FILE_NAME_COMPONENT_SEPARATOR = "_";

    @Override
	public void process() {
        File newFile = getNewDataFile();
        if (newFile == null) {
            // no new data file.                 
            return;
        } 
        try {

            LOGGER.info(()->"Processing Blackout data file " + newFile.getAbsolutePath());
            if(DataManagerFactory.getInstance().isServerInActiveDataCenter()){
            	processNewDataFile(newFile);
            } else {
            	LOGGER.info(()->"Server does not belong to an active data center, skipping the unpacking and loading, and moving the file to the processed directory.");
            }
            
            LoaderUtil.moveFile(newFile, LoaderUtil.getProcessedDirectory(), newFile.getName());

        } catch (IOException e) {
            LOGGER.error(()->"Fail to unpack data file " + newFile.getAbsolutePath(), e);
            LoaderUtil.moveFile(newFile, LoaderUtil.getErrorDirectory(), newFile.getName());
        } catch (Exception e) {
            LOGGER.error(()->"Fail to process data file " + newFile.getAbsolutePath(), e);
            LoaderUtil.moveFile(newFile, LoaderUtil.getErrorDirectory(), newFile.getName());
        }
    }

    
    private File getNewDataFile() {
        File parentDir = new File(CppConfigurationBean.getInstance().getPoRepoPath());
        File[] allZipFiles = parentDir.listFiles(new LinearBlackoutDataFileFilter());
        if (allZipFiles == null || allZipFiles.length == 0) {
            return null;
        } else if (allZipFiles.length > 1) {
            Arrays.sort(allZipFiles);
            for (int i = 0; i < allZipFiles.length - 1; i++) {
            	if(isSupportedVersion(allZipFiles[i].getName())) {
            		LoaderUtil.moveFile(allZipFiles[i], LoaderUtil.getProcessedDirectory(), allZipFiles[i].getName() + ".old");
            	} else {
            		LoaderUtil.moveFile(allZipFiles[i], LoaderUtil.getErrorDirectory(), allZipFiles[i].getName() + ".unsupported");
            	}
            }
        }
        File fileToProcess = allZipFiles[allZipFiles.length - 1];
        if(isSupportedVersion(fileToProcess.getName())) {
        	return fileToProcess;
        } else {
        	LoaderUtil.moveFile(fileToProcess, LoaderUtil.getErrorDirectory(), fileToProcess.getName() + ".unsupported");
        	return null;
        }
    }

    private boolean isSupportedVersion(String zipFileName) {
		if(StringUtils.isNotBlank(zipFileName) && zipFileName.toLowerCase().endsWith(ZIP_FILE_EXTENSION)) {
			String[] filleNameComponents = zipFileName.split(FILE_NAME_COMPONENT_SEPARATOR);
			if(filleNameComponents.length > 1 ) {
				return SUPPORTED_VERSION.equals(filleNameComponents[1]);
			}
		}
		return false;
	}

    private void processNewDataFile(File file) throws IOException {
        FileInputStream fis = null;
        ZipInputStream zis = null;
        String inProcessDirName = LoaderUtil.getInProcessDirectory();
        boolean isEmpty = true;
        
        BufferedOutputStream out = null;
        try {
            fis = new FileInputStream(file);
            zis = new ZipInputStream(fis);

            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                	LoaderUtil.ensureDirectory(inProcessDirName + File.separator + ze.getName());
                    continue;
                }

                isEmpty = false;
                int count = 0;
                int bufferSize = 1024;
                String filename = inProcessDirName + File.separator + ze.getName();
                new File(filename).getParentFile().mkdirs();

                out = new BufferedOutputStream(new FileOutputStream(filename), bufferSize);
                byte data[] = new byte[bufferSize];
                while ((count = zis.read(data, 0, bufferSize)) != -1) {
                    out.write(data, 0, count);
                }
                zis.closeEntry();
                out.close();
            }
            if (isEmpty)
            	LOGGER.warn(()->"Blackout data file is empty. " + file.getAbsolutePath());
        } finally {
            try {
                if (zis != null) zis.close();
                if (fis != null) fis.close();
                if (out != null) out.close();
            } catch (IOException e) {
                LOGGER.error(()->"Cannot close zip file.", e);
            }
        }
    }

}
