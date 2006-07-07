/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.MockBuilder;

import org.jdom.Element;

public class ScheduleTest extends TestCase {
    private Schedule schedule;
    private TimeZone defaultTimeZone;

    private static final long ONE_MINUTE = Schedule.ONE_MINUTE;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long TWELVE_HOURS = 12 * ONE_HOUR;
    private static final long ONE_DAY = Schedule.ONE_DAY;
    private static final TimeZone LOS_ANGELES;

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
    
    private static final PauseBuilder PAUSE_11_13;
    private static final PauseBuilder PAUSE_11_13_FRIDAY;

    static {
        LOS_ANGELES = createLosAngelesTimeZone();
        
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

        PAUSE_11_13 = new PauseBuilder();
        PAUSE_11_13.setStartTime(1100);
        PAUSE_11_13.setEndTime(1300);

        PAUSE_11_13_FRIDAY = new PauseBuilder();
        PAUSE_11_13_FRIDAY.setStartTime(1100);
        PAUSE_11_13_FRIDAY.setEndTime(1300);
        PAUSE_11_13_FRIDAY.setDay("friday");
        
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

        schedule.add(NOON_BUILDER);
        schedule.add(MIDNIGHT_BUILDER);
        schedule.add(MULTIPLE_5);
        schedule.add(MULTIPLE_1);
        
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(LOS_ANGELES);
    }

    protected void tearDown() throws Exception {
        schedule = null;
        TimeZone.setDefault(defaultTimeZone);
    }
    
    private static TimeZone createLosAngelesTimeZone() {
        // taken from SimpleTimeZone javadoc:
        
        // Base GMT offset: -8:00
        // DST starts:      at 2:00am in standard time
        //                  on the first Sunday in April
        // DST ends:        at 2:00am in daylight time
        //                  on the last Sunday in October
        // Save:            1 hour
        return new SimpleTimeZone(-28800000,
                       "America/Los_Angeles",
                       Calendar.APRIL, 1, -Calendar.SUNDAY,
                       7200000,
                       Calendar.OCTOBER, -1, Calendar.SUNDAY,
                       7200000,
                       3600000);
    }

    /**
     * @param calendar Calendar with date set
     * @param hour
     * @param min
     * @return Date with date and time set
     */
    private static Date getDate(Calendar calendar, int hour, int min) {
        Calendar cal = (Calendar) calendar.clone();
        cal.setTimeZone(LOS_ANGELES);
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
        timeBasedSchedule.add(NOON_BUILDER);
        timeBasedSchedule.add(MIDNIGHT_BUILDER);

        Builder nextTimeBuilder = timeBasedSchedule.selectBuilder(3, THURSDAY_1001, THURSDAY_1101);
        assertEquals(NOON_BUILDER, nextTimeBuilder);

        try {
            Schedule badSchedule = new Schedule();
            badSchedule.selectBuilder(1, THURSDAY_1001, THURSDAY_1101);
            fail("should fail with no builders");
        } catch (CruiseControlException expected) {
        }
    }

    public void testSelectBuilder_MultipleBuildersWithDaySet() throws CruiseControlException {
        Builder thursdayBuilder = new MockBuilder();
        thursdayBuilder.setDay("thursday");
        Schedule scheduledByDay = new Schedule();
        scheduledByDay.add(thursdayBuilder);
        assertEquals(thursdayBuilder, scheduledByDay.selectBuilder(1, THURSDAY_1001, THURSDAY_1101));
        assertEquals(thursdayBuilder, scheduledByDay.selectBuilder(1, THURSDAY_1001, FRIDAY_0000));        

        Builder fridayBuilder = new MockBuilder();
        fridayBuilder.setDay("friday");
        scheduledByDay.add(fridayBuilder);
        assertEquals(thursdayBuilder, scheduledByDay.selectBuilder(1, THURSDAY_1001, THURSDAY_1101));
        assertEquals(fridayBuilder, scheduledByDay.selectBuilder(1, THURSDAY_1001, FRIDAY_0000));
    }

    public void testSelectBuilder_ForcedBuildAcrossTimeChangeBoundary() throws CruiseControlException {
        Builder thursdayBuilder = new MockBuilder();

        thursdayBuilder.setTime("1120");
        thursdayBuilder.setDay("thursday");
        Schedule scheduledByDay = new Schedule();
        scheduledByDay.add(thursdayBuilder);

        Calendar oct272005 = Calendar.getInstance();
        oct272005.set(2005, Calendar.OCTOBER, 27);

        // mimic a JMX trigger
        Date last = getDate(oct272005, 11, 20);
        Date now = getDate(oct272005, 11, 25);
        assertEquals(thursdayBuilder, scheduledByDay.selectBuilder(123, last, now));
    }
    
    public void testIsPaused() {
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(2300);
        pauseBuilder.setEndTime(2359);
        schedule.add(pauseBuilder);

        assertEquals(schedule.isPaused(THURSDAY_2301), true);
        assertEquals(schedule.isPaused(THURSDAY_1101), false);
    }

    public void testGetTimeToNextBuild() {
        long fiveSeconds = 5 * 1000;
        long elevenHours = 11 * ONE_HOUR;
        long oneHourFiftyNineMinutes = (2 * ONE_HOUR) - ONE_MINUTE;

        PauseBuilder pause = new PauseBuilder();
        pause.setStartTime(2300);
        pause.setEndTime(2359);
        schedule.add(pause);

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
            TWELVE_HOURS - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_1201, ONE_DAY * 2));

        assertEquals(
            "wait till after pause",
            TWELVE_HOURS - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_1201, elevenHours));

        assertEquals(
            "wait till after pause we're in",
            ONE_HOUR - ONE_MINUTE,
            schedule.getTimeToNextBuild(THURSDAY_2301, fiveSeconds));

        pause = new PauseBuilder();
        pause.setStartTime(0000);
        pause.setEndTime(100);
        schedule.add(pause);

        assertEquals(
            "wait till after pause on next day",
            2 * ONE_HOUR,
            schedule.getTimeToNextBuild(THURSDAY_2301, ONE_HOUR));

        assertEquals(
            "two back-to-back pauses",
            2 * ONE_HOUR,
            schedule.getTimeToNextBuild(THURSDAY_2301, fiveSeconds));

        pause = new PauseBuilder();
        pause.setStartTime(0000);
        pause.setEndTime(2359);
        pause.setDay("friday");
        schedule.add(pause);

        assertEquals(
            "chained pauses with day specific pause",
            ONE_DAY + (2 * ONE_HOUR),
            schedule.getTimeToNextBuild(THURSDAY_2301, ONE_HOUR));
    }
    
    public void testGetTimeToNextBuild_MultipleBuilderWithDaySet() {
        Schedule intervalThursdaysSchedule = new Schedule();
        Builder intervalThursdays = new MockBuilder();
        intervalThursdays.setDay("thursday");
        intervalThursdaysSchedule.add(intervalThursdays);
        assertEquals(ONE_MINUTE, intervalThursdaysSchedule.getTimeToNextBuild(THURSDAY_2301, ONE_MINUTE));
        assertEquals(6 * ONE_DAY + ONE_HOUR - ONE_MINUTE,
                intervalThursdaysSchedule.getTimeToNextBuild(THURSDAY_2301, ONE_HOUR));
        assertEquals(6 * ONE_DAY + ONE_HOUR - ONE_MINUTE,
                intervalThursdaysSchedule.getTimeToNextBuild(THURSDAY_2301, ONE_DAY));
    }
    
    public void testGetTimeToNextBuild_ShouldUseIntervalWhenThereAreNoBuilders() {
        Schedule badSchedule = new Schedule();

        assertEquals(
            ONE_DAY,
            badSchedule.getTimeToNextBuild(THURSDAY_1001, ONE_DAY));
    }
    
    public void testGetTimeToNextBuild_ShouldNotExceedMaximumInterval() {
        Schedule badSchedule = new Schedule();    
        PauseBuilder alwaysPaused = new PauseBuilder();
        alwaysPaused.setStartTime(0000);
        alwaysPaused.setEndTime(2359);
        badSchedule.add(alwaysPaused);

        assertEquals(
            Schedule.MAX_INTERVAL_MILLISECONDS,
            badSchedule.getTimeToNextBuild(THURSDAY_1001, ONE_DAY));
        
        badSchedule = new Schedule();
        badSchedule.add(NOON_BUILDER);
        badSchedule.add(PAUSE_11_13);
        
        assertEquals(
            Schedule.MAX_INTERVAL_MILLISECONDS,
            badSchedule.getTimeToNextBuild(THURSDAY_1101, ONE_MINUTE));
    }
    
    public void testGetTimeToNextBuild_DailyBuild() {
        Schedule dailyBuildSchedule = new Schedule();
        dailyBuildSchedule.add(NOON_BUILDER);

        assertEquals(
            "ignore interval when only time builds",
            ONE_HOUR - ONE_MINUTE,
            dailyBuildSchedule.getTimeToNextBuild(THURSDAY_1101, ONE_MINUTE));
            
        PauseBuilder pause = new PauseBuilder();
        pause.setStartTime(0000);
        pause.setEndTime(2359);
        pause.setDay("friday");
        dailyBuildSchedule.add(pause);

        assertEquals(
            "pause w/only time builder",
            (ONE_DAY * 2) - ONE_MINUTE,
            dailyBuildSchedule.getTimeToNextBuild(THURSDAY_1201, ONE_MINUTE));
    }

    public void testGetTimeToNextBuild_WeeklyBuild() {
        schedule = new Schedule();
        Builder weeklyBuilder = new MockBuilder();
        weeklyBuilder.setTime("0100");
        weeklyBuilder.setDay("Sunday");
        schedule.add(weeklyBuilder);

        assertEquals((ONE_DAY * 2) + ONE_HOUR,
                schedule.getTimeToNextBuild(FRIDAY_0000, ONE_MINUTE));
    }

    public void testGetTimeToNextBuild_WeeklyBuildAcrossDaylightSavingsTimeBoundary() {
        Builder thursdayBuilder = new MockBuilder();

        thursdayBuilder.setTime("1120");
        thursdayBuilder.setDay("thursday");
        Schedule scheduledByDay = new Schedule();
        scheduledByDay.add(thursdayBuilder);

        Calendar oct272005 = Calendar.getInstance();
        oct272005.set(2005, Calendar.OCTOBER, 27);

        Date now = getDate(oct272005, 11, 25);
        long dstEndingAdjustment = ONE_HOUR;
        assertEquals(((ONE_DAY * 6) + (ONE_HOUR * 23) + (ONE_MINUTE * 55) + dstEndingAdjustment),
                        scheduledByDay.getTimeToNextBuild(now, ONE_MINUTE));        
    }

    public void testGetTimeToNextBuild_MonthlyBuild() {
        schedule = new Schedule();
        Builder monthlyBuilder = new MockBuilder() {
            private long targetTime = FRIDAY_0000.getTime() + (30 * ONE_DAY);
            public boolean isValidDay(Date now) {
                return targetTime <= now.getTime() ? true : false;
            }
        };
        monthlyBuilder.setTime("0000");
        schedule.add(monthlyBuilder);

        assertEquals(ONE_DAY * 30,
                schedule.getTimeToNextBuild(FRIDAY_0000, ONE_MINUTE));

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

        schedule.add(MULTIPLE_1);
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
    
    public void testValidateShouldFailIfAllTimedBuildersInPauses() throws CruiseControlException {
        schedule = new Schedule();

        schedule.add(PAUSE_11_13_FRIDAY);
        schedule.add(NOON_BUILDER);
        schedule.validate();
        
        schedule.add(PAUSE_11_13);
        try {
            schedule.validate();
            fail();
        } catch (CruiseControlException expected) {
        }
       
        schedule.add(MIDNIGHT_BUILDER);
        schedule.validate();
    }
    
    public void testGetDayString() {
        assertEquals("sunday", schedule.getDayString(Calendar.SUNDAY).toLowerCase());
        assertEquals("friday", schedule.getDayString(Calendar.FRIDAY).toLowerCase());
        try {
            schedule.getDayString(0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("valid values of days are between 1 and 7, was 0", e.getMessage());
        }
    }

}
