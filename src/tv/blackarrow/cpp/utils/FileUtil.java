//
// Copyright 2012 BlackArrow, Inc. All rights reserved.
//
// The information contained herein is confidential, proprietary to BlackArrow Inc., and
// considered a trade secret as defined in section 499C of the penal code of the State of
// California. Use of this information by anyone other than authorized employees of
// BlackArrow Inc. is granted only under a written non-disclosure agreement, expressly
// prescribing the scope and manner of such use.
//
// $Change$
// $Author$
// $Id$
// $DateTime$
//
package tv.blackarrow.cpp.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FileUtil
 * @author JWang
 */
public class FileUtil {
    private static final Logger LOGGER = LogManager.getLogger(FileUtil.class);

    private FileUtil() { }

    /**
     * This method copies the input file to output file. This method expects the calling code to
     * provide the folder path ending with slash.
     * 
     * @param inputFile
     * @param out
     * @throws Exception
     */
    public static void copyFile(final File inputFile, final String outputFile,
            final String folderPath) throws Exception {

        if (inputFile == null) {
            LOGGER.error(()->"FileUtil.copyFile() : File to copy is null : ");
            throw new Exception("FileUtil.copyFile() : File to copy is null ");
        }
        if (folderPath == null || "".equals(folderPath)) {
            LOGGER.error(()->"FileUtil.copyFile() : Destiantion folder not provided : ");
            throw new Exception("FileUtil.copyFile() : Destiantion folder not provided ");
        }
        try {
            // now check whether the folder exists
            final File destination = new File(folderPath);
            if (destination.exists()) {
                if (destination.isDirectory()) {
                    final File out = new File(folderPath + outputFile);
                    final FileInputStream fis = new FileInputStream(inputFile);
                    final FileOutputStream fos = new FileOutputStream(out);
                    final byte[] buf = new byte[1024];
                    int counter = 0;
                    while ((counter = fis.read(buf)) != -1) {
                        fos.write(buf, 0, counter);
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    LOGGER.debug(()->"FileUtil.copyFile() : File copied to " + folderPath);
                }
                else {
                    LOGGER.error(()->"FileUtil.copyFile() : Destiantion is not a folder");
                    throw new Exception("FileUtil.copyFile() : Destiantion is not a folder");
                }
            }
            else {
                LOGGER.error(()->"FileUtil.copyFile() : Invalid Destiantion folder");
                throw new Exception("FileUtil.copyFile() : Invalid Destiantion folder");
            }

        }
        catch (FileNotFoundException e) {
            LOGGER.error(()->"FileUtil.copyFile() : File not found : ", e);
            throw new Exception("FileUtil.copyFile() : File not found : ", e);
        }
        catch (IOException e) {
            LOGGER.error(()->"FileUtil.copyFile() : File not found : ", e);
            throw new Exception("FileUtil.copyFile() : IOException : ", e);
        }
    }

    /**
     * This method retrieves all the files from the given folder ignoring the folders.
     * 
     * @param folder
     * @return
     * @throws Exception
     */
    public static List<File> fetchFiles(final String folder) throws Exception {
        final List<File> files = new ArrayList<File>();
        
        try {
            final File folderObject = new File(folder);
            if (folderObject.isDirectory()) {
                final File[] allFiles = folderObject.listFiles();
                if (allFiles != null) {
                    for (int i = 0; i < allFiles.length; i++) {
                        if (allFiles[i].isFile()) {
                            files.add(allFiles[i]);
                        }

                    }
                }
            }
            else {
                // throw Exception
                LOGGER.error(()->"FileUtil.fetchFiles() Error directory not found : " + folder);
                throw new Exception("FileUtil.fetchFiles() Error Directory not found : " + folder);
            }
        }
        catch (Exception e) {
            LOGGER.error(()->"FileUtil.fetchFiles() Exception while fetching the files from : " + folder);
            throw new Exception("FileUtil.fetchFiles() Exception while fetching the files from  : "
                    + folder, e);
        }
        return files;
    }

    /**
     * @param strPath
     * @return
     * @throws Exception
     */
    public static File getFile(String strPath) throws Exception {
        File out = null;
        try {
            out = new File(strPath);
        }
        catch (Exception e) {
            LOGGER.error(()->"FileUtil.getFile() Exception while fetching the files from : " + strPath);
            throw new Exception("FileUtil.getFile() Exception while fetching the files from  : "
                    + strPath, e);
        }

        return out;
    }


    /**
     * delete multiple files
     * 
     * @param Vector filesToDelete
     */
    public static void deleteFileCollection(Vector<String> filesToDelete) throws Exception {

        if (filesToDelete != null && filesToDelete.size() > 0) {
            int noOfFiles = filesToDelete.size();
            for (int i = 0; i < noOfFiles; i++) {
                deleteFile((String) filesToDelete.elementAt(i));
            }

        }

    }

    /**
     * delete file from file system
     * 
     * @param strFileToDelete
     */
    public static boolean deleteFile(String strFileToDelete) throws Exception {
        // Make sure the file or directory exists and isn't write protected
        File fileToDelete = new File(strFileToDelete);
        if (!fileToDelete.exists())
            throw new IllegalArgumentException("Delete: no such file or directory: "
                    + strFileToDelete);

        if (!fileToDelete.canWrite())
            throw new IllegalArgumentException("Delete: write protected: " + strFileToDelete);

        // Attempt to delete it
        boolean success = fileToDelete.delete();

        return success;
    }

    /**
     * unzip file to a specified directory
     * 
     * @param zipfileName
     */
    public static void unzip(String zipfileName, String unzipToDirectory) {
        try {
            ZipFile zipFile = new ZipFile(zipfileName);
            Enumeration<?> enumeration = zipFile.entries();

            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();

                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));

                int size;
                byte[] buffer = new byte[2048];

                FileOutputStream fos = new FileOutputStream(unzipToDirectory + zipEntry.getName());
                BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);

                while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
                    bos.write(buffer, 0, size);
                }

                bos.flush();
                bos.close();
                fos.close();

                bis.close();
            }
        }
        catch (IOException e) {
            LOGGER.error(()->"failed to delete the file : " + zipfileName, e);
        }
    }

    /**
     * zip files into one zip file
     * @param filenames
     * @param outputZippedFileName
     */
    public static void zipFiles(String[] filenames, String outputZippedFileName) {
        // These are the files to include in the ZIP file

        // Create a buffer for reading the files
        byte[] buf = new byte[1024];

        try {
            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZippedFileName));

            // Compress the files
            for (int i=0; i<filenames.length; i++) {
                FileInputStream in = new FileInputStream(filenames[i]);

                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(filenames[i]));

                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();
                in.close();
            }

            // Complete the ZIP file
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * zip single file
     * @param filenames
     * @param outputZippedFileName
     */
    public static void zipFiles(String fullpath_filename, String filename, String outputZippedFileName) {
        // These are the files to include in the ZIP file

        // Create a buffer for reading the files
        byte[] buf = new byte[1024];

        try {
            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZippedFileName));

            // Compress the files
            FileInputStream in = new FileInputStream(fullpath_filename);

            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(filename));

            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            // Complete the entry
            out.closeEntry();
            in.close();

            // Complete the ZIP file
            out.close();
        } catch (IOException e) {
        }
    }

}
