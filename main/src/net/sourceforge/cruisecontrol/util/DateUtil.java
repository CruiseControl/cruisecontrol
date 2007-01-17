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

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;

public final class DateUtil {

    private static final Logger LOG = Logger.getLogger(DateUtil.class);

    public static final transient long ONE_SECOND = 1000;

    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    static final long ONE_HOUR = 60 * ONE_MINUTE;

    public static final String SIMPLE_DATE_FORMAT = "yyyyMMddHHmmss";

    /**
     * This is the date format required by commands passed to CVS.
     */
    // The timezone is hard coded to GMT to prevent problems with it being
    // formatted as GMT+00:00. However, we still need to set the time zone
    // of the formatter so that it knows it's in GMT.
    private static final String CVS_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss 'GMT'";

    // This cannot be exposed as TimeZones are mutable
    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");

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
     * @deprecated Use correctly spelled <code>milliTimeDifference</code> instead.
     */
    public static long milliTimeDiffernce(int earlier, int later) {
        return milliTimeDifference(earlier, later);
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
    public static long milliTimeDifference(int earlier, int later) {
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
        return hours * ONE_HOUR + minutes * ONE_MINUTE;
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
        return new SimpleDateFormat(SIMPLE_DATE_FORMAT).format(date);
    }

    public static Date parseFormattedTime(String timeString, String description) throws CruiseControlException {

        Date date;
        if (timeString == null) {
            throw new IllegalArgumentException("Null date string for " + description);
        }
        try {
            date = new SimpleDateFormat(SIMPLE_DATE_FORMAT).parse(timeString);
        } catch (ParseException e) {
            LOG.error("Error parsing timestamp for [" + description + "]", e);
            throw new CruiseControlException("Cannot parse string for " + description + ":" + timeString);
        }

        return date;
    }

    /**
     * Formats a given date as a the format required by commands passed to CVS.
     * @param date the date
     */
    public static String formatCVSDate(Date date) {
        DateFormat format = new SimpleDateFormat(CVS_DATE_PATTERN);
        format.setTimeZone(UTC);
        return format.format(date);
    }

    /**
     * Parses a text in CVS date format as a date.
     * @param text the date to parse
     * @return a date in the default timezone
     */
    public static Date parseCVSDate(String text) throws ParseException {
        DateFormat format = new SimpleDateFormat(CVS_DATE_PATTERN);
        format.setTimeZone(UTC);
        return format.parse(text);
    }

    /**
     * Return a String representation of a duration specified in milliseconds.
     */
    public static String getDurationAsString(final long buildLength) {
        long timeSeconds = buildLength / 1000;
        long minutes = (timeSeconds / 60);
        long seconds = timeSeconds - (minutes * 60);
        return minutes + " minute(s) " + seconds + " second(s)";
    }
}
