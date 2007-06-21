/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.utils;

import java.text.SimpleDateFormat;

import net.sourceforge.cruisecontrol.util.DateUtil;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class CCDateFormatter {

    private static final int DATE_START = 3;

    private static final int DATE_END = 17;

    private static final int MILLONS_OF_DAY = 1000 * 3600 * 24;

    private static DateTimeFormatter yyyyMMddHHmmssPattern = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    private static SimpleDateFormat yyyyMMddHHmmssSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private CCDateFormatter() {
    }

    public static String duration(long timeSpan) {
        String when = timeSpan > 0 ? "ago" : "later";
        long days = timeSpan / CCDateFormatter.MILLONS_OF_DAY;
        String daysString = days == 0 ? "" : days + " days ";
        String remaining = DateUtil.formatTime(timeSpan - days * CCDateFormatter.MILLONS_OF_DAY);
        String space = remaining.endsWith(" ") ? "" : " ";
        return daysString + remaining + space + when;
    }

    public static String getDateStringInHumanBeingReadingStyle(DateTime date) {
        String dateString = getDateFormatterWithTimeZone().format(date.toDate());
        int colonPlace = dateString.length() - 2;
        return dateString.substring(0, colonPlace) + ":" + dateString.substring(colonPlace);
    }

    private static SimpleDateFormat getDateFormatterWithTimeZone() {
        return new SimpleDateFormat("d MMM yyyy HH:mm 'GMT' Z");
    }

    public static String yyyyMMddHHmmss(DateTime date) {
        return yyyyMMddHHmmssSimpleDateFormat.format(date.toDate());
    }

    public static DateTime format(String datetime, String dateFormat) {
        return DateTimeFormat.forPattern(dateFormat).parseDateTime(datetime);
    }

    public static String format(DateTime datetime, String format) {
        return new SimpleDateFormat(format).format(datetime.toDate());
    }

    public static DateTime formatLogName(String logFileName) {
        return yyyyMMddHHmmssPattern.parseDateTime(getBuildDateFromLogFileName(logFileName));
    }

    public static String getBuildDateFromLogFileName(String logFileName) {
        return logFileName.substring(DATE_START, DATE_END);
    }
}
