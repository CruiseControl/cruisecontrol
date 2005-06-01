package net.sourceforge.cruisecontrol.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;

public final class DateUtil {

    private static final Logger LOG = Logger.getLogger(DateUtil.class);

    public static final transient long ONE_SECOND = 1000;

    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    static final long ONE_HOUR = 60 * ONE_MINUTE;

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * This is the date format required by commands passed to CVS.
     */
    public static final SimpleDateFormat CVS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");

    private DateUtil() {
    }

    /**
     * Create an integer time from a <code>Date</code> object.
     * 
     * @param date
     *            The date to get the timestamp from.
     * @return The time as an integer formatted as "HHmm".
     */
    public static int getTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY) * 100;
        int minute = calendar.get(Calendar.MINUTE);
        return hour + minute;
    }

    /**
     * finds the difference in milliseconds between two integer time values of the format "HHmm".
     * 
     * @param earlier
     *            integer time value of format "HHmm"
     * @param later
     *            integer time value of format "HHmm"
     * @return long millisecond time difference
     */
    public static long milliTimeDiffernce(int earlier, int later) {
        long earlierMillis = convertToMillis(earlier);
        long laterMillis = convertToMillis(later);
        return laterMillis - earlierMillis;
    }

    /**
     * Convert a time represented by the format "HHmm" into milliseconds.
     * 
     * @param hhmm
     *            where hh are hours and mm are minutes
     * @return hhmm in milliseconds
     */
    public static long convertToMillis(int hhmm) {
        int minutes = hhmm % 100;
        int hours = (hhmm - minutes) / 100;
        long milliseconds = hours * ONE_HOUR + minutes * ONE_MINUTE;
        return milliseconds;
    }

    /**
     * @param time
     *            time in milliseconds
     * @return Time formatted as X hours Y minutes Z seconds
     */
    public static String formatTime(long time) {
        long seconds = time / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        StringBuffer sb = new StringBuffer();
        if (hours != 0) {
            sb.append(hours + " hours ");
        }
        if (minutes != 0) {
            sb.append(minutes + " minutes ");
        }
        if (seconds != 0) {
            sb.append(seconds + " seconds");
        }

        return sb.toString();
    }

    /**
     * @return midnight on today's date
     */
    public static Date getMidnight() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight.getTime();
    }

    public static String getFormattedTime(Date date) {
        if (date == null) {
            return null;
        }
        return SIMPLE_DATE_FORMAT.format(date);
    }

    public static Date parseFormattedTime(String timeString, String description) throws CruiseControlException {

        Date date;
        if (timeString == null) {
            throw new IllegalArgumentException("Null date string for " + description);
        }
        try {
            date = SIMPLE_DATE_FORMAT.parse(timeString);
        } catch (ParseException e) {
            LOG.error("Error parsing timestamp for [" + description + "]", e);
            throw new CruiseControlException("Cannot parse string for " + description + ":" + timeString);
        }

        return date;
    }

    public static String formatCVSDate(Date date) {
        return CVS_DATE_FORMAT.format(date);
    }

}
