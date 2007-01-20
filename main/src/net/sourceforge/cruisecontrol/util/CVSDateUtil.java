package net.sourceforge.cruisecontrol.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class CVSDateUtil {

    /**
     * Date format required by commands passed to CVS.
     */
    // The timezone is hard coded to GMT to prevent problems with it being
    // formatted as GMT+00:00. However, we still need to set the time zone
    // of the formatter so that it knows it's in GMT.
    private static final String CVS_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss 'GMT'";

    // This cannot be exposed as TimeZones are mutable
    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");

    private CVSDateUtil() {

    }

    /**
     * Formats a given date to the format required by commands passed to CVS.
     * 
     * @param date
     *            the date
     */
    public static String formatCVSDate(Date date) {
        DateFormat format = new SimpleDateFormat(CVS_DATE_PATTERN);
        format.setTimeZone(UTC);
        return format.format(date);
    }

    /**
     * Parses a text in CVS date format as a date.
     * 
     * @param text
     *            the date to parse
     * @return a date in the default timezone
     */
    public static Date parseCVSDate(String text) throws ParseException {
        DateFormat format = new SimpleDateFormat(CVS_DATE_PATTERN);
        format.setTimeZone(UTC);
        return format.parse(text);
    }

}
