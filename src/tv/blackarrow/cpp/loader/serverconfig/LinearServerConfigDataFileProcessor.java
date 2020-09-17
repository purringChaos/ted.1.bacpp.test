/**
 * 
 */
package tv.blackarrow.cpp.loader.serverconfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.loader.DataFileProcessor;
import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.model.CppConfigurationBean;

/**
 * @author akumar
 *
 */
public class LinearServerConfigDataFileProcessor implements DataFileProcessor {
	
	private final static Logger LOGGER = LogManager.getLogger(LinearServerConfigDataFileProcessor.class);

	/* (non-Javadoc)
	 * @see tv.blackarrow.cpp.po.loader.DataFileProcessor#process()
	 */
	@Override
	public void process() {
		File newFile = getNewDataFile();
        if (newFile == null) {
            // no new data file.                 
            return;
        } 
        try {
        	LOGGER.info(()->"Processing Server Config data file " + newFile.getAbsolutePath());
            processNewDataFile(newFile);
            LoaderUtil.moveFile(newFile, LoaderUtil.getProcessedDirectory(), newFile.getName());
            LOGGER.info(()->"Processing Server Config data file " + newFile.getAbsolutePath() + " finished.");
        } catch (IOException e) {
        	LOGGER.error(()->"Fail to unpack data file " + newFile.getAbsolutePath(), e);
            LoaderUtil.moveFile(newFile, LoaderUtil.getErrorDirectory(), newFile.getName());
        } catch (Exception e) {
        	LOGGER.error(()->"Fail to process data file " + newFile.getAbsolutePath(), e);
            LoaderUtil.moveFile(newFile, LoaderUtil.getErrorDirectory(), newFile.getName());
        }
	}
	
    private void processNewDataFile(File newFile) throws IOException{
        moveFileToInprocessDirectory(newFile);
        new LinearServerConfigDataFileLoader().load();
    }

	private void moveFileToInprocessDirectory(File newFile)	throws FileNotFoundException, IOException {
		FileInputStream fis = null;
        ZipInputStream zis = null;
        String inProcessDirName = LoaderUtil.getInProcessDirectory();
        boolean isEmpty = true;
        BufferedOutputStream out = null;
        try {
            fis = new FileInputStream(newFile);
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
            	LOGGER.warn(()->"Server Config data file is empty. " + newFile.getAbsolutePath());
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

	private File getNewDataFile() {
        File parentDir = new File(CppConfigurationBean.getInstance().getPoRepoPath());
        File[] allZipFiles = parentDir.listFiles(new LinearServerConfigDataFileFilter());
        if (allZipFiles == null || allZipFiles.length == 0) {
            return null;
        } else if (allZipFiles.length > 1) {
            Arrays.sort(allZipFiles);
            for (int i = 0; i < allZipFiles.length - 1; i++) {
            	LoaderUtil.moveFile(allZipFiles[i], LoaderUtil.getProcessedDirectory(), allZipFiles[i].getName() + ".old");
            }
        }
        return allZipFiles[allZipFiles.length - 1];
    }
}
