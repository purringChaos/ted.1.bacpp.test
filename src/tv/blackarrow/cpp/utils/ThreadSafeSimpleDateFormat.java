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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ThreadSafeSimpleDateFormat
 * avoid thread issue
 * @author jwang
 */
public class ThreadSafeSimpleDateFormat {
    private DateFormat df;

    public ThreadSafeSimpleDateFormat(String format) {
        this.df = new SimpleDateFormat(format);
    }

    public synchronized String format(Date date) {
        return df.format(date);
    }

    public synchronized Date parse(String string) throws ParseException {
        return df.parse(string);
    }
}
