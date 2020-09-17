package tv.blackarrow.cpp.loader.po;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public abstract class LinearDataFileFilter implements FilenameFilter {
    // if the format of the data file changes then this needs to be updated
    // along with the compiler version to support runtime separation properly
    private static final String DATEFORMAT = "yyyyMMddHHmmss";


    protected abstract String getFilePrefix();
    protected abstract String getFileSuffix();

    @Override
    public boolean accept(File dir, String name) {
        String dateString = "";
        String prefix = getFilePrefix();
        String suffix = getFileSuffix();
        
        if (name.length() > (prefix.length() + suffix.length())) {
            try {
                dateString = name.substring(prefix.length(), name.indexOf(suffix) - 1);
            } catch (Exception e) {
                // just leave dateString as blank.
            }
        }
        boolean isDateValid = isDateValid(dateString);
        return name.startsWith(prefix) && name.endsWith(suffix) && !dateString.equals("")
                && dateString.length() == DATEFORMAT.length() && isDateValid;
    }

    protected boolean isDateValid(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
		// this time string is in UTC
		// local timezone will fail to load one hour each year in DST spring forward case 
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.setLenient(false);
        try {
            sdf.parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
