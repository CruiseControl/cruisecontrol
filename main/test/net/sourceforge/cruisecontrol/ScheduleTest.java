/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.MockBuilder;

import java.util.Calendar;

import org.jdom.Element;

public class ScheduleTest extends TestCase {

    private Schedule schedule;

    private Calendar cal;
    private Calendar cal2;
    private Calendar cal3;
    private Calendar cal4;
    private Calendar cal5;
    private Calendar timeBuildPreviousDay;
    private Calendar atMiddayTimeBuilderTime;
    private Calendar atMidnightTimeBuilderTime;

    private MockBuilder middayTimeBuilder;
    private MockBuilder midnightTimeBuilder;
    private MockBuilder multipleOfFive;
    private MockBuilder multipleOfOne;

    private static final long ONE_MINUTE = Schedule.ONE_MINUTE;

    public void setUp() {
        schedule = new Schedule();
        middayTimeBuilder = new MockBuilder();
        middayTimeBuilder.setTime("1200");
        middayTimeBuilder.setBuildLogXML(new Element("builder1"));
        midnightTimeBuilder = new MockBuilder();
        midnightTimeBuilder.setTime("0000");
        midnightTimeBuilder.setBuildLogXML(new Element("builder1"));
        multipleOfFive = new MockBuilder();
        multipleOfFive.setMultiple(5);
        multipleOfFive.setBuildLogXML(new Element("builder2"));
        multipleOfOne = new MockBuilder();
        multipleOfOne.setMultiple(1);
        multipleOfOne.setBuildLogXML(new Element("builder3"));
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(2300);
        pauseBuilder.setEndTime(2359);

        schedule.addBuilder(middayTimeBuilder);
        schedule.addBuilder(midnightTimeBuilder);
        schedule.addBuilder(multipleOfFive);
        schedule.addBuilder(multipleOfOne);
        schedule.addPauseBuilder(pauseBuilder);

        cal = Calendar.getInstance();
        // Nov 22, 2001 was a Thursday
        cal.set(2001, Calendar.NOVEMBER, 22, 10, 01, 01);
        cal2 = Calendar.getInstance();
        cal2.set(2001, Calendar.NOVEMBER, 22, 11, 01, 01);
        cal3 = Calendar.getInstance();
        cal3.set(2001, Calendar.NOVEMBER, 22, 12, 01, 01);
        cal4 = Calendar.getInstance();
        cal4.set(2001, Calendar.NOVEMBER, 22, 23, 01, 01);
        cal5 = Calendar.getInstance();
        // Nov 23, 2001 was a Friday
        cal5.set(2001, Calendar.NOVEMBER, 23, 00, 00, 01);
        timeBuildPreviousDay = Calendar.getInstance();
        timeBuildPreviousDay.set(2001, Calendar.NOVEMBER, 21, 12, 01, 01);
        atMiddayTimeBuilderTime = Calendar.getInstance();
        atMiddayTimeBuilderTime.set(2001, Calendar.NOVEMBER, 22, 12, 00, 00);
        atMidnightTimeBuilderTime = Calendar.getInstance();
        atMidnightTimeBuilderTime.set(2001, Calendar.NOVEMBER, 23, 00, 00, 00);
    }

    public void testSelectBuilder() throws CruiseControlException {
        Builder buildIsMultipleOfOne =
            schedule.selectBuilder(12, cal.getTime(), cal2.getTime());
        assertEquals(multipleOfOne, buildIsMultipleOfOne);
        Builder buildIsMultipleOfFive =
            schedule.selectBuilder(10, cal.getTime(), cal2.getTime());
        assertEquals(multipleOfFive, buildIsMultipleOfFive);
        Builder middayTimeBuild =
            schedule.selectBuilder(11, cal.getTime(), cal3.getTime());
        assertEquals(middayTimeBuilder, middayTimeBuild);
        Builder midnightTimeBuild =
            schedule.selectBuilder(11, cal.getTime(), cal5.getTime());
        assertEquals(midnightTimeBuilder, midnightTimeBuild);
        Builder timeBuildAcrossDays =
            schedule.selectBuilder(
                11,
                timeBuildPreviousDay.getTime(),
                cal3.getTime());
        assertEquals(middayTimeBuilder, timeBuildAcrossDays);
        Builder atMiddayTimeBuild =
            schedule.selectBuilder(
                11,
                timeBuildPreviousDay.getTime(),
                atMiddayTimeBuilderTime.getTime());
        assertEquals(middayTimeBuilder, atMiddayTimeBuild);
        Builder atMidnightTimeBuild =
            schedule.selectBuilder(
                11,
                timeBuildPreviousDay.getTime(),
                atMidnightTimeBuilderTime.getTime());
        assertEquals(midnightTimeBuilder, atMidnightTimeBuild);
    }

    public void testIsPaused() {
        assertEquals(schedule.isPaused(cal4.getTime()), true);
        assertEquals(schedule.isPaused(cal2.getTime()), false);
    }

    public void testGetTimeToNextBuild() {
        long fiveSeconds = 5 * 1000;
        long oneHour = 60 * ONE_MINUTE;
        long elevenHours = 11 * oneHour;
        long oneHourFiftyNineMinutes = 2 * oneHour - ONE_MINUTE;
        long twelveHours = 12 * oneHour;
        long twentyFourHours = 2 * twelveHours;

        // time till next time build > build interval
        assertEquals(
            fiveSeconds,
            schedule.getTimeToNextBuild(cal.getTime(), fiveSeconds));
        // time till next time build < build interval
        assertEquals(
            oneHourFiftyNineMinutes,
            schedule.getTimeToNextBuild(cal.getTime(), elevenHours));
        // next build would be in pause interval
        assertEquals(
            twelveHours - ONE_MINUTE,
            schedule.getTimeToNextBuild(cal3.getTime(), elevenHours));
        // time till next time build is tomorrow
        assertEquals(
            twentyFourHours - ONE_MINUTE,
            schedule.getTimeToNextBuild(cal3.getTime(), twentyFourHours * 2));
        // time till end of pause that we're currently in
        assertEquals(
            oneHour - ONE_MINUTE,
            schedule.getTimeToNextBuild(cal4.getTime(), fiveSeconds));

        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(0000);
        pauseBuilder.setEndTime(100);
        schedule.addPauseBuilder(pauseBuilder);

        // next build would be in a pause interval on the next day
        assertEquals(
            2 * oneHour,
            schedule.getTimeToNextBuild(cal4.getTime(), oneHour));

        pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(100);
        pauseBuilder.setEndTime(200);
        schedule.addPauseBuilder(pauseBuilder);

        // next build would be in a pause interval on the next day
        assertEquals(
            3 * oneHour,
            schedule.getTimeToNextBuild(cal4.getTime(), oneHour));

        schedule = new Schedule();

        pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(0000);
        pauseBuilder.setEndTime(1100);
        schedule.addPauseBuilder(pauseBuilder);

        // pause gives time > than 1 day
        assertEquals(
            twentyFourHours + oneHour,
            schedule.getTimeToNextBuild(cal.getTime(), twentyFourHours));

        pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(0000);
        pauseBuilder.setEndTime(2359);
        pauseBuilder.setDay("friday");
        schedule.addPauseBuilder(pauseBuilder);

        // chained pauses
        assertEquals(
            (2 * twentyFourHours) + oneHour,
            schedule.getTimeToNextBuild(cal.getTime(), twentyFourHours));
            
        schedule = new Schedule();
        
        pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(0000);
        pauseBuilder.setEndTime(2359);
        schedule.addPauseBuilder(pauseBuilder);
        
        assertEquals(
            Schedule.MAX_INTERVAL_MILLISECONDS,
            schedule.getTimeToNextBuild(cal.getTime(), twentyFourHours));
    }
    
    public void testInterval() {
        assertEquals("default interval", 300 * 1000, schedule.getInterval());
        schedule.setInterval(500);
        assertEquals(500 * 1000, schedule.getInterval());
    }
    
    public void testValidate() throws CruiseControlException {
        schedule = new Schedule();
        
        try {
            schedule.validate();
            fail("validate should throw exception if it is configured with no builders");
        } catch (CruiseControlException e) {
        }
        
        schedule.addBuilder(multipleOfOne);
        schedule.validate();
        
        long oneYearInSeconds = 60 * 60 * 24 * 365;
        schedule.setInterval(oneYearInSeconds);
        schedule.validate();
        
        schedule.setInterval(oneYearInSeconds + 1);
        try {
            schedule.validate();
            fail("validate should throw exception if interval is greater than " + oneYearInSeconds);
        } catch (CruiseControlException e) {
        }
    }

}
