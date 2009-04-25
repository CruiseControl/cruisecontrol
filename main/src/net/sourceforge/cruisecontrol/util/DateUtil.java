/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.lang.ref.SoftReference;

import net.sourceforge.cruisecontrol.CruiseControlException;

// @todo Find alternative "logging" approach for classes where log4j may not be available
//import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;

public final class DateUtil {

    // Can't use log4j since DateUtil is now used in reporting/jsp
    // @todo Find alternative "logging" approach for classes where log4j may not be available
    //private static final Logger LOG = Logger.getLogger(DateUtil.class);

    public static final transient long ONE_SECOND = 1000;

    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    static final long ONE_HOUR = 60 * ONE_MINUTE;

    public static final String SIMPLE_DATE_FORMAT = "yyyyMMddHHmmss";
    private static final String GMT = "GMT -0:00";

    private DateUtil() {
    }


    private static final ThreadLocal<SoftReference<SimpleDateFormat>> TL
            = new ThreadLocal<SoftReference<SimpleDateFormat>>();

    /**
     * Store a ThreadLocal instance of an ISO8601 dateFormat that can be safely re-used by a single thread.
     * Avoids creating new dateFormat objects for calls from the same thread.
     * SoftReference is used to keep a thread from holding onto dateFormat instance forever.
     * Only package visible to allow for unit testing.
     * @return an instance of an ISO8601 dateFormat that can be safely re-used by a single thread.
     */
    static SimpleDateFormat getThreadLocal8601Format() {
        final SoftReference<SimpleDateFormat> ref = TL.get();
        if (ref != null) {
            final SimpleDateFormat result = ref.get();
            if (result != null) {
                return result;
            }
        }
        final SimpleDateFormat result = new SimpleDateFormat(DateUtils.ISO8601_DATETIME_PATTERN);
        final SoftReference<SimpleDateFormat> newRef = new SoftReference<SimpleDateFormat>(result);
        TL.set(newRef);
        return result;
    }

    /**
     * SimpleDateFormat is not thread-safe, see http://jira.public.thoughtworks.org/browse/CC-906.
     * @return a new date format (for use by one thread - not thread safe)
     */
    private static DateFormat createIso8601Format() {
        final SimpleDateFormat format = getThreadLocal8601Format();
        format.setTimeZone(TimeZone.getTimeZone(GMT));
        return format;
    }

    private static DateFormat createSimpleFormat() {
        return new SimpleDateFormat(SIMPLE_DATE_FORMAT);
    }

    /**
     * Create an integer time from a <code>Date</code> object.
     *
     * @param date
     *            The date to get the timestamp from.
     * @return The time as an integer formatted as "HHmm".
     */
    public static int getTimeFromDate(final Date date) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        final int hour = calendar.get(Calendar.HOUR_OF_DAY) * 100;
        final int minute = calendar.get(Calendar.MINUTE);
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
    public static long milliTimeDifference(final int earlier, final int later) {
        final long earlierMillis = convertToMillis(earlier);
        final long laterMillis = convertToMillis(later);
        return laterMillis - earlierMillis;
    }

    /**
     * Convert a time represented by the format "HHmm" into milliseconds.
     *
     * @param hhmm
     *            where hh are hours and mm are minutes
     * @return hhmm in milliseconds
     */
    public static long convertToMillis(final int hhmm) {
        final int minutes = hhmm % 100;
        final int hours = (hhmm - minutes) / 100;
        return hours * ONE_HOUR + minutes * ONE_MINUTE;
    }

    /**
     * @param time
     *            time in milliseconds
     * @return Time formatted as X hours Y minutes Z seconds
     */
    public static String formatTime(final long time) {
        long seconds = time / 1000;
        final long hours = seconds / 3600;
        final long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        final StringBuilder sb = new StringBuilder();
        if (hours != 0) {
            sb.append(hours).append(" hours ");
        }
        if (minutes != 0) {
            sb.append(minutes).append(" minutes ");
        }
        if (seconds != 0) {
            sb.append(seconds).append(" seconds");
        }

        return sb.toString();
    }

    /**
     * @return midnight on today's date
     */
    public static Date getMidnight() {
        final Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        return midnight.getTime();
    }

    public static String getFormattedTime(final Date date) {
        if (date == null) {
            return null;
        }
        return createSimpleFormat().format(date);
    }

    public static Date parseFormattedTime(final String timeString, final String description)
            throws CruiseControlException {

        if (timeString == null) {
            throw new IllegalArgumentException("Null date string for " + description);
        }
        final Date date;
        try {
            date = createSimpleFormat().parse(timeString);
        } catch (ParseException e) {
            // @todo Find alternative "logging" approach where log4j may not be available
            //LOG.error("Error parsing timestamp for [" + description + "]", e);
            throw new CruiseControlException("Cannot parse string for " + description + ":" + timeString);
        }

        return date;
    }

    /**
     * @param buildLength the length of a build in millis.
     * @return a String representation of a duration specified in milliseconds.
     */
    public static String getDurationAsString(final long buildLength) {
        long timeSeconds = buildLength / 1000;
        long minutes = (timeSeconds / 60);
        long seconds = timeSeconds - (minutes * 60);
        return minutes + " minute(s) " + seconds + " second(s)";
    }

    public static Date parseIso8601(final String timestamp) throws ParseException {
        return createIso8601Format().parse(timestamp);
    }

    public static String formatIso8601(final Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.format(date, DateUtils.ISO8601_DATETIME_PATTERN);
    }
}
