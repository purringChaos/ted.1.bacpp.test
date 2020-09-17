package tv.blackarrow.cpp.purger;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.blackarrow.cpp.model.CppConfigurationBean;

public class Purger {

    private static final Logger LOGGER = LogManager.getLogger(Purger.class.getName());

    /**
     * The purger has been changed to rely on the data store itself (Couchbase)
     * to purge data as it expires. The only purging that remains here is to
     * delete old data files from the file system. This should not cause any
     * conflicts with the file extractor or loader now so it should be safe to
     * run the purger at any time. To avoid errors, it is still necessary to
     * ensure that two purges do not run at the same time.
     */
    public static synchronized void purgeUnneededData() {
        purgeExpiredDataFile();
    }

    private static void purgeExpiredDataFile() {
        File dataDir = new File(CppConfigurationBean.getInstance().getPoInProcessDir());
        File[] feedDir = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        if (feedDir == null) {
            LOGGER.info(()->"No data file directory is found, skip purging.");
            return;
        }

        final String hrPrefix = getCurrentDayPrefix();
        FilenameFilter hrFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.compareTo(hrPrefix) < 0;
            }
        };

        int deleteCount = 0;
        for (File dir : feedDir) {
            String[] poFileArr = dir.list(hrFilter);
            for (String fileName : poFileArr) {
                if ((new File(dir.getAbsolutePath() + File.separator + fileName)).delete()) {
                    deleteCount++;
                }
            }
        }
        LOGGER.info("Deleted " + deleteCount + " old po files up to yesterday");
    }

    @SuppressWarnings("deprecation")
    private static String getCurrentDayPrefix() {
        Date date = new Date();
        date.setHours(0);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHH");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

}
