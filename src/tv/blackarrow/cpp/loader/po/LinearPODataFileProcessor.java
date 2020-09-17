package tv.blackarrow.cpp.loader.po;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.loader.DataFileProcessor;
import tv.blackarrow.cpp.loader.LoaderUtil;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.model.CppConfigurationBean;

import tv.blackarrow.cpp.utils.CppConstants;

/**
 * Perform initial validation of the file, make sure all expected files exist,
 * then unpack and move file to processed or error directory
 * 
 * @author hcao
 *
 */
public class LinearPODataFileProcessor implements DataFileProcessor {
	
	private static final Logger LOGGER = LogManager.getLogger(LinearPODataFileProcessor.class);
    private static final String DATEFORMAT = "yyyyMMddHHmmss";
	
    // if the format of the data file changes then this needs to be updated
    // along with the compiler version to support runtime separation properly
    private static int DATA_FILE_VERSION_NUMBER = 1;

    @Override
	public void process(){
    	try {
    		File newFile = getNewPODataFile();
    		if(newFile == null) {
    			LOGGER.info(()->"No new PO Data file is found, skip processing");
    			return;
    		} else {
    			LOGGER.info(()->"Found new PO Data file " + newFile.getAbsolutePath());
    		}
    		
    		if(DataManagerFactory.getInstance().isServerInActiveDataCenter()){
	    		boolean valid = validatePODataFile(newFile);
	    		
	    		String destDir = null;
	    		File destDirFile = null;
	    		if(valid) {
	    			processNewPODataFile(newFile);
	    			destDir = CppConfigurationBean.getInstance().getPoProcessedDir();
	    		} else {
	    			LOGGER.error(()->"PO Data file validation failed, abort processing");
	    			destDir = CppConfigurationBean.getInstance().getPoErrorDir();
	    		}
				destDirFile = new File(destDir);
				if(!destDirFile.exists()) {
					if(destDirFile.mkdirs()) {
						LOGGER.info("No existing directory found, successfully created -" + destDir );
					} else {
						LOGGER.error(()->"No existing inprocess directory found, failed to creat new one. Abort processing");
						return;
					}
				}			
	            File destFile = new File(destDir + File.separator + newFile.getName());
				if(newFile.renameTo(destFile)) {
					LOGGER.info("Successfully moved data file to " + destDir);
				} else {
					LOGGER.error("Failed to move data file to " + destDir);
				}
	            // added to insure acquisition point configuration is loaded promptly,
	            // currently every 1 minute
	            loadMappingFiles();
    		} else {
    			LOGGER.info("Server does not belong to an active data center, skipping the unpacking and loading, and moving the file to the processed directory.");
            	LoaderUtil.moveFile(newFile, LoaderUtil.getProcessedDirectory(), newFile.getName());
            }
            
    	} catch (Exception e) {
    		LOGGER.error(e.getMessage(),e);
    	}
		
    }
    
    private void loadMappingFiles() {
        try {
            LinearPODataFileLoader loader = new LinearPODataFileLoader();
            loader.loadMappingFiles();
            LOGGER.info("Finished loading configuration file");
        } catch (Exception ex) {
        	LOGGER.error("ERROR occured in execute(JobExecutionContext ctx)", ex);
        }
    }

	public File getNewPODataFile(){
		File parentDir = new File(CppConfigurationBean.getInstance().getPoRepoPath());
		File[] allZipFiles = parentDir.listFiles(new FilenameFilter() {
            private String prefix = CppConfigurationBean.getInstance().getLinearRunTimeID() + "_" + DATA_FILE_VERSION_NUMBER + "_";
				private String suffix = "placement_opportunity.zip";

				@Override
				public boolean accept(File dir, String name) {
					String dateString ="";
					if(name.length() > (prefix.length()+suffix.length())){
					 try {
						dateString =name.substring(prefix.length(), name.indexOf(suffix)-1);
					 } catch (Exception e) {
						// just leave dateString as blank.
					 }
					}
					boolean isDateValid=isDateValid(dateString);
                return name.startsWith(prefix) && name.endsWith(suffix) && !dateString.equals("")
                        && dateString.length() == DATEFORMAT.length() && isDateValid;
				}

				private boolean isDateValid(String dateString) {
					SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
				// this time string is in UTC
				// local timezone will fail to load one hour each year in DST spring forward case 
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
					//sdf.setTimeZone(TimeZone.getTimeZone("UTC"));//Bug fix. Passed-in date and time will always be UTC.// sdhiman - commented this.
					sdf.setLenient(false);
					try {
						sdf.parse(dateString);
						return true;
					} catch (ParseException e) {
						return false;
					}
				}
			});
        if (allZipFiles == null || allZipFiles.length == 0) {
			return null;
		} else if(allZipFiles.length > 1) {
			Arrays.sort(allZipFiles);
			for(int i=0; i<allZipFiles.length-1; i++) {
                allZipFiles[i].renameTo(new File(CppConfigurationBean.getInstance().getPoProcessedDir() + File.separator
                        + allZipFiles[i].getName() + ".old"));
			}
		}
		return allZipFiles[allZipFiles.length-1];
	}
	
	public void processNewPODataFile(File file) {
		FileInputStream fis = null;
		ZipInputStream zis = null;
		File[] feedDir = null;
		String inProcessDirName = CppConfigurationBean.getInstance().getPoInProcessDir();
		Collection<String> feedNamesToDelete = new ArrayList<String>();
		BufferedOutputStream out=null;
		try {
			File inProcessDir = new File(inProcessDirName);
			if(!inProcessDir.exists()) {
				if(inProcessDir.mkdirs()) {
					LOGGER.info("No existing inprocess directory found, successfully created - " + inProcessDir.getAbsolutePath());
				} else {
					LOGGER.error("No existing inprocess directory found, failed to creat new one. Abort processing");
					return;
				}
			}else{
				//Read the Data from there and store it in HashMap.
				feedDir = inProcessDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return file.isDirectory();
					}
				});
				for(File feed: feedDir){
					feedNamesToDelete.add(feed.getName());
				}
			}
			
			fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
			 
			LOGGER.info("Exploding zip file - " + file.getAbsolutePath() + " to " + inProcessDirName);
			ZipEntry ze = null;
			
			while( (ze = zis.getNextEntry()) != null ) {
				if(ze.isDirectory()) {
					File newDirFromZip = new File(inProcessDirName + File.separator + ze.getName());
					feedNamesToDelete.remove(newDirFromZip.getName());
					if(!newDirFromZip.exists() && !newDirFromZip.mkdirs()) {
						LOGGER.error("Error creating directory " + newDirFromZip.getAbsolutePath());
					} else {
						LOGGER.debug("Found directory " + newDirFromZip.getAbsolutePath());
					}
					continue;
				}
				
		        int count = 0;
		        int bufferSize = 1024;
		        String filename = inProcessDirName+File.separator+ze.getName();
		        new File(filename).getParentFile().mkdirs();
		        feedNamesToDelete.remove(new File(filename).getParentFile().getName());
		        
                out = new BufferedOutputStream(new FileOutputStream(filename), bufferSize);
		        byte data[] = new byte[bufferSize];
		        while((count = zis.read(data, 0, bufferSize)) != -1) {
		        	out.write(data, 0, count);
		        }
		        zis.closeEntry();
		        out.close();
		    }
		} catch (IOException e){
			LOGGER.error("Error unzipping data file " + file.getAbsolutePath() + "\n" + e.getMessage(), e);
		} finally {
			try {
				zis.close();
				zis=null;
				if(out!=null){
					out.close();
					out=null;
				}
				LOGGER.info("Successfully unzipped PO Data file " + file.getAbsolutePath());
			} catch (IOException e) {
				LOGGER.error("Cannot close zip file.", e);
			}
		}
		
		if(feedNamesToDelete !=null && !feedNamesToDelete.isEmpty()){
			for(String fileName: feedNamesToDelete){
				File fileToRemove = new File(inProcessDirName+File.separator+fileName);
				if(fileToRemove.exists()){
					try {
						FileUtils.deleteDirectory(fileToRemove);
						LOGGER.debug("Folder name "+fileToRemove+" is deleted successfuly");
					} catch (IOException e) {
						LOGGER.error("Folder name "+fileToRemove+" is not deleted");
					}
				}
			}
		}
	}
	
	public boolean validatePODataFile(File file) {
		FileInputStream fis = null;
		ZipInputStream zis = null;
		try {
			fis = new FileInputStream(file);
			zis = new ZipInputStream(fis);
			 
			Map<String, Integer> filenameMap = new HashMap<String, Integer>();
			
			ZipEntry ze = null;
			while( (ze = zis.getNextEntry()) != null ) {
				if(ze.isDirectory()) {
					continue;
				}
				String filename = ze.getName();
				if(filename.endsWith("xml")) {
					addXMLFileCount(filename, filenameMap);
				} else {
					addCSVFileCount(filename, filenameMap);
				}
			}
			PODataFileValidationResult result = validateFileCount(filenameMap);
			if(result.getFatalErrors() > 0) {
				result.logResult(LOGGER);
				return false;
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("File " + file.getAbsolutePath() + " does not exist", e);
			return false;
		} catch (IOException e) {
			LOGGER.error("Error reading zip file.", e);
			return false;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return false;
		} finally {
			if(zis != null) {
				try {
					zis.close();
				} catch (IOException e) {
					LOGGER.error("Error closing zip file.", e);
				}
			}
		}
		return true;
	}
	
	private void addXMLFileCount(String filename, Map<String, Integer> filenameMap) {
		addFileCount(filename,filenameMap);
	}
	
	private void addCSVFileCount(String filename, Map<String, Integer> filenameMap) {
		String feed = filename.substring(0, filename.indexOf("/")); // assuming file name like 2012101711_COMCENTEAST.csv
		addFileCount(feed, filenameMap);
	}
	
	private void addFileCount(String key, Map<String, Integer> filenameMap) {
		Integer value = filenameMap.get(key);
		if(value == null) {
			filenameMap.put(key, new Integer(1));
		} else {
			filenameMap.put(key, new Integer(value.intValue()+1));
		}
	}
	
	private PODataFileValidationResult validateFileCount(Map<String, Integer> filenameMap) throws Exception {
        PODataFileValidationResult result = verifyFileExists("acquisition_feed_mapping.xml", filenameMap);
		
		Iterator<Entry<String,Integer>> entryIterator = filenameMap.entrySet().iterator();
		while(entryIterator.hasNext()) {
			Entry<String,Integer> entry = entryIterator.next();
			if(entry.getKey().endsWith("xml")) {
				continue;
			}
			int fileCount = entry.getValue().intValue();
			if( fileCount < CppConstants.HOURS_PER_DELIVERY) {
                result.addError(fileCount + " files found for feed " + entry.getKey() + ", at least "
                        + CppConstants.HOURS_PER_DELIVERY + " expected ");
			}
		}
		return result;
	} 
	
	private PODataFileValidationResult verifyFileExists(String name, Map<String, Integer> filenameMap) throws Exception {
		PODataFileValidationResult result = new PODataFileValidationResult();
		StringBuilder msgBuilder = new StringBuilder("Missing required file "); 
		if (filenameMap.get(name) == null) {
			result.addError(msgBuilder.append(name + ".").toString());
		}
		return result;
	}
	
	class PODataFileValidationResult {
		private List<String> errorMsgList = new ArrayList<String>();
		private List<String> warningMsgList = new ArrayList<String>();
		
		public int getFatalErrors() {
			return errorMsgList.size();
		}
		
		public int getWarnings() {
			return warningMsgList.size();
		}

		public PODataFileValidationResult merge(PODataFileValidationResult result) {
			errorMsgList.addAll(result.errorMsgList);
			warningMsgList.addAll(result.warningMsgList);
			return this;
		}
		
		public void logResult(Logger log) {
			for(String msg : errorMsgList) {
				log.error(msg);
			}
		}
		
		private void addError(String msg) {
			errorMsgList.add(msg);
		}
				
	}
	
}
