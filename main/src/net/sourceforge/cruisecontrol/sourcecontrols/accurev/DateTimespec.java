/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
/*
 * Created on 29-Jun-2005 by norru
 *
 * Copyright (C) Sony Computer Entertainment Europe
 *               Studio Liverpool Server Group
 *
 * Authors:
 *     Nicola Orru' <Nicola_Orru@scee.net>
 */
package net.sourceforge.cruisecontrol.sourcecontrols.accurev;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Full date timespec, format is either yyyy/MM/dd HH:mm:ss or yyyy/MM/dd HH:mm:ss.count
 *
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 */
public class DateTimespec extends Timespec {
    private static final Logger LOG = Logger.getLogger(DateTimespec.class);
    private static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    /**
     * Convenience constant containing the KewordTimespec "now"
     */
    public static final KeywordTimespec NOW = new KeywordTimespec("now");
    private Date date;

    /**
     * Creates a new DateTimespec without count (yyyy/MM/dd HH:mm:ss form)
     *
     * @param date the timespec date
     */
    public DateTimespec(Date date) {
        this.date = date;
    }

    /**
     * Creates a new DateTimespec without count (yyyy/MM/dd HH:mm:ss form) containing the current time
     * "shifted" by the given amount of seconds
     *
     * @param secondsFromNow distance in seconds from "now" (e.g. 3600 means "one hour from now", -60 means "one
     *                       minute ago").
     */
    public DateTimespec(int secondsFromNow) {
        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.SECOND, secondsFromNow);
        this.date = calendar.getTime();
    }

    /**
     * Returns the formatted date
     *
     * @return the formatted date if date is not null or a blank string ig the date is null
     */
    public String format() {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    /**
     * Parses a date from Accurev in the format <code>YYYY/MM/DD hh:mm:ss</code>
     *
     * @param date
     *          String containing the date to parse
     * @return a new Date whose value reflects the date string
     */
    public static Date parse(String date) {
        try {
            return new SimpleDateFormat(DATETIME_FORMAT).parse(date);
        } catch (ParseException e) {
            LOG.error("Error parsing date " + date + " using format" + DATETIME_FORMAT);
            return null;
        }
    }
}
