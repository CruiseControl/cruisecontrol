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

import junit.framework.TestCase;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author jfredrick
 */
public class DateUtilTest extends TestCase {

    private Calendar cal;

    private Calendar cal2;

    public void setUp() {
        // create a couple calendars/dates
        cal = Calendar.getInstance();
        cal.set(2001, Calendar.NOVEMBER, 22, 10, 01, 01);
        cal2 = Calendar.getInstance();
        cal2.set(2001, Calendar.NOVEMBER, 22, 11, 01, 01);
    }

    public void testGetThreadLocal8601Format() throws Exception {
        final Callable<SimpleDateFormat> c1 = new Callable<SimpleDateFormat>() {
            public SimpleDateFormat call() throws Exception {
                return DateUtil.getThreadLocal8601Format();
            }
        };

        final Callable<SimpleDateFormat> c2 = new Callable<SimpleDateFormat>() {
            public SimpleDateFormat call() throws Exception {
                return DateUtil.getThreadLocal8601Format();
            }
        };

        final ExecutorService pool = Executors.newFixedThreadPool(2);

        final Future<SimpleDateFormat> future1 = pool.submit(c1);
        final Future<SimpleDateFormat> future2 = pool.submit(c2);

        final DateFormat df1 = future1.get();
        assertSame(df1, future1.get());

        final DateFormat df2 = future2.get();
        assertNotSame(df2, df1);

        /* The ThreadLocal SoftReference might be reclaimed before this runs, so the check below is not reliable.
        // It does sometimes pass, therefore does "occaisionaly" prove that the ThreadLocal df is shared though...
        final Future<SimpleDateFormat> future1Again = pool.submit(c1);
        assertSame(df1, future1Again.get());
        //*/
    }

    public void testGetTimeFromDate() {
        assertEquals(DateUtil.getTimeFromDate(cal.getTime()), 1001);
        assertEquals(DateUtil.getTimeFromDate(cal2.getTime()), 1101);
    }

    public void testMilliTimeDifference() {
        int noon = 1200;
        int elevenThirty = 1130;
        int oneFifteen = 1315;

        long thirtyMinutes = 30 * 60 * 1000;
        long hourFifteenMinutes = (60 + 15) * 60 * 1000;
        long hourFortyFiveMinutes = (60 + 45) * 60 * 1000;

        assertEquals(thirtyMinutes, DateUtil.milliTimeDifference(elevenThirty, noon));
        assertEquals(hourFifteenMinutes, DateUtil.milliTimeDifference(noon, oneFifteen));
        assertEquals(hourFortyFiveMinutes, DateUtil.milliTimeDifference(elevenThirty, oneFifteen));
    }

    public void testConvertToMillis() {
        int noon = 1200;
        int oneAM = 100;
        int elevenFifteenPM = 2315;

        long noonMillis = 12 * 60 * 60 * 1000;
        long oneAMmillis = 1 * 60 * 60 * 1000;
        long elevenFifteenPMmillis = (23 * 60 + 15) * 60 * 1000;

        assertEquals(noonMillis, DateUtil.convertToMillis(noon));
        assertEquals(oneAMmillis, DateUtil.convertToMillis(oneAM));
        assertEquals(elevenFifteenPMmillis, DateUtil.convertToMillis(elevenFifteenPM));
    }

    public void testFormatTime() {
        long fiveSeconds = 5 * 1000;
        long oneHour = 60 * DateUtil.ONE_MINUTE;
        long oneHourFiftyNineMinutes = 2 * oneHour - DateUtil.ONE_MINUTE;

        String seconds = "5 seconds";
        String hoursMinutesSeconds = "1 hours 59 minutes 5 seconds";
        String negativeTime = "-1 hours -59 minutes -5 seconds";

        assertEquals(seconds, DateUtil.formatTime(fiveSeconds));
        assertEquals(hoursMinutesSeconds, DateUtil.formatTime(oneHourFiftyNineMinutes + fiveSeconds));
        assertEquals(negativeTime, DateUtil.formatTime(-1 * (oneHourFiftyNineMinutes + fiveSeconds)));
    }

    public void testGetBuildTimeAsString() {
        int minutes = 50;
        int seconds = 30;
        int millis = 123;
        long timeInMillis = millis + seconds * 1000 + minutes * 60 * 1000;
        assertEquals("50 minute(s) 30 second(s)",
            DateUtil.getDurationAsString(timeInMillis));
    }
}
