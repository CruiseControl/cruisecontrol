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
import java.util.Date;

import org.jdom.Element;

public class ScheduleTest extends TestCase {

    private Schedule schedule;

    private static final long ONE_MINUTE = Schedule.ONE_MINUTE;

    private static final Calendar FRIDAY;
    private static final Calendar THURSDAY;

    private static final Date THURSDAY_1001;
    private static final Date THURSDAY_1101;
    private static final Date THURSDAY_1201;
    private static final Date THURSDAY_2301;
    private static final Date FRIDAY_0000;

    private static final MockBuilder NOON_BUILDER;
    private static final MockBuilder MIDNIGHT_BUILDER;
    private static final MockBuilder MULTIPLE_5;
    private static final MockBuilder MULTIPLE_1;

    static {
        NOON_BUILDER = new MockBuilder();
        NOON_BUILDER.setTime("1200");
        NOON_BUILDER.setBuildLogXML(new Element("builder1"));

        MIDNIGHT_BUILDER = new MockBuilder();
        MIDNIGHT_BUILDER.setTime("0000");
        MIDNIGHT_BUILDER.setBuildLogXML(new Element("builder1"));

        MULTIPLE_5 = new MockBuilder();
        MULTIPLE_5.setMultiple(5);
        MULTIPLE_5.setBuildLogXML(new Element("builder2"));

        MULTIPLE_1 = new MockBuilder();
        MULTIPLE_1.setMultiple(1);
        MULTIPLE_1.setBuildLogXML(new Element("builder3"));

        THURSDAY = Calendar.getInstance();
        THURSDAY.set(2001, Calendar.NOVEMBER, 22);
        FRIDAY = Calendar.getInstance();
        FRIDAY.set(2001, Calendar.NOVEMBER, 23);

        THURSDAY_1001 = getDate(THURSDAY, 10, 01);
        THURSDAY_1101 = getDate(THURSDAY, 11, 01);
        THURSDAY_1201 = getDate(THURSDAY, 12, 01);
        THURSDAY_2301 = getDate(THURSDAY, 23, 01);
        FRIDAY_0000 = getDate(FRIDAY, 0, 0);
    }

    protected void setUp() {
        schedule = new Schedule();

        schedule.addBuilder(NOON_BUILDER);
        schedule.addBuilder(MIDNIGHT_BUILDER);
        schedule.addBuilder(MULTIPLE_5);
        schedule.addBuilder(MULTIPLE_1);
    }

    protected void tearDown() throws Exception {
        schedule = null;
    }

    /**
     * @param calendar Calendar with date set
     * @param hour
     * @param min
     * @return Date with date and time set
     */
    private static Date getDate(Calendar calendar, int hour, int min) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        return cal.getTime();
    }

    public void testSelectBuilder() throws CruiseControlException {
        Builder buildIsMultipleOfOne = schedule.selectBuilder(13, THURSDAY_1001, THURSDAY_1101);
        assertEquals(MULTIPLE_1, buildIsMultipleOfOne);

        Builder buildIsMultipleOfFive = schedule.selectBuilder(10, THURSDAY_1001, THURSDAY_1101);
        assertEquals(MULTIPLE_5, buildIsMultipleOfFive);

        Builder middayTimeBuild = schedule.selectBuilder(13, THURSDAY_1001, THURSDAY_1201);
        assertEquals(NOON_BUILDER, middayTimeBuild);

        Builder midnightTimeBuild = schedule.selectBuilder(13, THURSDAY_1001, FRIDAY_0000);
        assertEquals(MIDNIGHT_BUILDER, midnightTimeBuild);

        Calendar wednesday = Calendar.getInstance();
        wednesday.set(2001, Calendar.NOVEMBER, 21);
        Date noonWednesday = getDate(wednesday, 12, 00);

        Builder timeBuildAcrossDays = schedule.selectBuilder(11, noonWednesday, FRIDAY_0000);
        assertEquals(MIDNIGHT_BUILDER, timeBuildAcrossDays);

        Schedule timeBasedSchedule = new Schedule();
        timeBasedSchedule.addBuilder(NOON_BUILDER);
        timeBasedSchedule.addBuilder(MIDNIGHT_BUILDER);

        Builder nextTimeBuilder = timeBasedSchedule.selectBuilder(3, THURSDAY_1001, THURSDAY_1101);
        assertEquals(NOON_BUILDER, nextTimeBuilder);
        
        try {
            Schedule badSchedule = new Schedule();
            badSchedule.selectBuilder(1, THURSDAY_1001, THURSDAY_1101);
            fail("should fail with no builders");
        } catch (CruiseControlException expected) {
        }
    }

    public void testIsPaused() {
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(2300);
        pauseBuilder.setEndTime(2359);
        schedule.addPauseBuilder(pauseBuilder);

        assertEquals(schedule.isPaused(THURSDAY_2301), true);
        assertEquals(schedule.isPaused(THURSDAY_1101), false);
    }

    public void testGetTimeToNextBuild() {
        long fiveSeconds = 5 * 1000;
        long oneHour = 60 * ONE_MINUTE;
        long elevenHours = 11 * oneHour;
        long oneHourFiftyNineMinutes = 2 * oneHour - ONE_MINUTE;
        long twelveHours = 12 * oneHour;
        long twentyFourHours = 2 * twelveHours;

        PauseBuilder pause = new PauseBuilder();
        pause.setStartTime(2300);
        pause.setEndTime(2359);
        schedule.addPauseBuilder(pause);

        assertEquals(
            "next time build > build interval",
            fiveSeconds,
            schedule.getTimeToNextBuild(THURSDAY_1001, fiveSeconds));

        assertEquals(
            "next time build < build interval",
            oneHourFiftyNineMinutes,
            schedule.getTimeToNextBuild(THURSDAY_1001, elevenHours));

        assertEquals(
            "next time build is tomorrow",
            twelveHours - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_1201, twentyFourHours * 2));

        assertEquals(
            "wait till after pause",
            twelveHours - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_1201, elevenHours));

        assertEquals(
            "wait till after pause we're in",
            oneHour - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_2301, fiveSeconds));

        pause = new PauseBuilder();
        pause.setStartTime(0000);
        pause.setEndTime(100);
        schedule.addPauseBuilder(pause);

        assertEquals(
            "wait till after pause on next day",
            2 * oneHour,
            schedule.getTimeToNextBuild(THURSDAY_2301, oneHour));

        assertEquals(
            "two back-to-back pauses",
            2 * oneHour,
            schedule.getTimeToNextBuild(THURSDAY_2301, fiveSeconds));

        pause = new PauseBuilder();
        pause.setStartTime(0000);
        pause.setEndTime(2359);
        pause.setDay("friday");
        schedule.addPauseBuilder(pause);

        assertEquals(
            "chained pauses with day specific pause",
            twentyFourHours + (2 * oneHour),
            schedule.getTimeToNextBuild(THURSDAY_2301, oneHour));

        Schedule badSchedule = new Schedule();

        assertEquals(
            "use interval when no builders found",
            twentyFourHours,
            badSchedule.getTimeToNextBuild(THURSDAY_1001, twentyFourHours));

        PauseBuilder alwaysPaused = new PauseBuilder();
        alwaysPaused.setStartTime(0000);
        alwaysPaused.setEndTime(2359);
        badSchedule.addPauseBuilder(alwaysPaused);

        assertEquals(
            "pause doesn't exceed maximum interval",
            Schedule.MAX_INTERVAL_MILLISECONDS,
            badSchedule.getTimeToNextBuild(THURSDAY_1001, twentyFourHours));

        Schedule dailyBuildSchedule = new Schedule();
        dailyBuildSchedule.addBuilder(NOON_BUILDER);

        assertEquals(
            "ignore interval when only time builds",
            oneHour - ONE_MINUTE,
            dailyBuildSchedule.getTimeToNextBuild(THURSDAY_1101, fiveSeconds));

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

        schedule.addBuilder(MULTIPLE_1);
        schedule.validate();

        long oneYearInSeconds = 60 * 60 * 24 * 365;
        schedule.setInterval(oneYearInSeconds);
        schedule.validate();

        schedule.setInterval(oneYearInSeconds + 1);
        try {
            schedule.validate();
            fail("maximum allowed interval should be " + oneYearInSeconds);
        } catch (CruiseControlException e) {
        }
    }

}
