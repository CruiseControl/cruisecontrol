/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public final class Util {

    static final long ONE_MINUTE = 60 * 1000;
    static final long ONE_HOUR = 60 * ONE_MINUTE;

    private Util() {
        
    }

    /**
     *  Create an integer time from a <code>Date</code> object.
     *
     *  @param date The date to get the timestamp from.
     *  @return The time as an integer formatted as "HHmm".
     */
    public static int getTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY) * 100;
        int minute = calendar.get(Calendar.MINUTE);
        return hour + minute;
    }

    /**
     * finds the difference in milliseconds between two integer time
     * values of the format "HHmm".
     * @param earlier integer time value of format "HHmm"
     * @param later integer time value of format "HHmm"
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
     * @param hhmm where hh are hours and mm are minutes
     * @return hhmm in milliseconds
     */
    public static long convertToMillis(int hhmm) {
        int minutes = hhmm % 100;
        int hours = (hhmm - minutes) / 100;
        long milliseconds = hours * ONE_HOUR + minutes * ONE_MINUTE;
        return milliseconds;
    }

    public static Element loadConfigFile(File configFile)
        throws CruiseControlException {
        Element cruisecontrolElement = null;
        try {
            SAXBuilder builder =
                new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            cruisecontrolElement = builder.build(configFile).getRootElement();
        } catch (Exception e) {
            throw new CruiseControlException(
                "failed to load config file [" + configFile != null
                    ? configFile.getName()
                    : "" + "]",
                e);
        }
        return cruisecontrolElement;
    }

}
