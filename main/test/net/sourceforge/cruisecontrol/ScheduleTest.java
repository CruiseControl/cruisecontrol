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

    private Schedule _schedule;
    
    private Calendar _cal;
    private Calendar _cal2;
    private Calendar _cal3;
    private Calendar _cal4;
    private Calendar timeBuildPreviousDay;
    private Calendar atTimeBuilderTime;
    
    private MockBuilder timeBuilder;
    private MockBuilder multipleOfFive;
	private MockBuilder multipleOfOne;
	
    private static final long ONE_MINUTE = Schedule.ONE_MINUTE;

    public ScheduleTest(String name) {
        super(name);
    }

    public void setUp() {
        _schedule = new Schedule();
        timeBuilder = new MockBuilder();
        timeBuilder.setTime("1200");
        timeBuilder.setBuildLogXML(new Element("builder1"));
        multipleOfFive = new MockBuilder();
        multipleOfFive.setMultiple(5);
        multipleOfFive.setBuildLogXML(new Element("builder2"));
        multipleOfOne = new MockBuilder();
        multipleOfOne.setMultiple(1);
        multipleOfOne.setBuildLogXML(new Element("builder3"));
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(2300);
        pauseBuilder.setEndTime(2359);

        _schedule.addBuilder(timeBuilder);
        _schedule.addBuilder(multipleOfFive);
        _schedule.addBuilder(multipleOfOne);
        _schedule.addPauseBuilder(pauseBuilder);

        //create a couple calendars/dates
        _cal = Calendar.getInstance();
        _cal.set(2001, Calendar.NOVEMBER, 22, 10, 01, 01);
        _cal2 = Calendar.getInstance();
        _cal2.set(2001, Calendar.NOVEMBER, 22, 11, 01, 01);
        _cal3 = Calendar.getInstance();
        _cal3.set(2001, Calendar.NOVEMBER, 22, 12, 01, 01);
        _cal4 = Calendar.getInstance();
        _cal4.set(2001, Calendar.NOVEMBER, 22, 23, 01, 01);
        timeBuildPreviousDay = Calendar.getInstance();
		timeBuildPreviousDay.set(2001, Calendar.NOVEMBER, 21, 12, 01, 01);
		atTimeBuilderTime = Calendar.getInstance();
		atTimeBuilderTime.set(2001, Calendar.NOVEMBER, 22, 12, 00, 00);
    }

    public void testSelectBuilder() throws CruiseControlException {
		Builder buildIsMultipleOfOne = _schedule.selectBuilder(12, _cal.getTime(), _cal2.getTime());
		assertEquals(multipleOfOne, buildIsMultipleOfOne);
		Builder buildIsMultipleOfFive = _schedule.selectBuilder(10, _cal.getTime(), _cal2.getTime());
		assertEquals(multipleOfFive, buildIsMultipleOfFive);
		Builder timeBuild = _schedule.selectBuilder(11, _cal.getTime(), _cal3.getTime());
		assertEquals(timeBuilder, timeBuild);
		Builder timeBuildAcrossDays = _schedule.selectBuilder(11, timeBuildPreviousDay.getTime(), _cal3.getTime());
		assertEquals(timeBuilder, timeBuildAcrossDays);
		Builder atTimeBuild = _schedule.selectBuilder(11, timeBuildPreviousDay.getTime(), atTimeBuilderTime.getTime());
		assertEquals(timeBuilder, atTimeBuild);
    }

    public void testIsPaused() {
        assertEquals(_schedule.isPaused(_cal4.getTime()), true);
        assertEquals(_schedule.isPaused(_cal2.getTime()), false);
    }

    public void testGetTimeToNextBuild() {
    	long fiveSeconds = 5 * 1000;
        long oneHour = 60 * ONE_MINUTE;
    	long elevenHours = 11 * oneHour;
    	long oneHourFiftyNineMinutes = 2 * oneHour - ONE_MINUTE;
		long twelveHours = 12 * oneHour;
		long twentyFourHours = 2 * twelveHours;
    	
    	// time till next time build > build interval
    	assertEquals(fiveSeconds, _schedule.getTimeToNextBuild(_cal.getTime(),fiveSeconds));
		// time till next time build < build interval
    	assertEquals(oneHourFiftyNineMinutes, _schedule.getTimeToNextBuild(_cal.getTime(),elevenHours));
		// next build would be in pause interval
		assertEquals(twelveHours-ONE_MINUTE, _schedule.getTimeToNextBuild(_cal3.getTime(),elevenHours));
		// time till next time build is tomorrow
		assertEquals(twentyFourHours-ONE_MINUTE, _schedule.getTimeToNextBuild(_cal3.getTime(),twentyFourHours*2));
        // time till end of pause that we're currently in
        assertEquals(oneHour-ONE_MINUTE, _schedule.getTimeToNextBuild(_cal4.getTime(),fiveSeconds));
        
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(0000);
        pauseBuilder.setEndTime(100);
        _schedule.addPauseBuilder(pauseBuilder);
        
        // next build would be in a pause interval on the next day
        assertEquals(2*oneHour, _schedule.getTimeToNextBuild(_cal4.getTime(),oneHour));
	}
    
    public void testFormatTime() {
        long fiveSeconds = 5 * 1000;
        long oneHour = 60 * ONE_MINUTE;
        long oneHourFiftyNineMinutes = 2 * oneHour - ONE_MINUTE;
        
        String seconds = "5 seconds";
        String hoursMinutesSeconds = "1 hours 59 minutes 5 seconds";
        String negativeTime = "-1 hours -59 minutes -5 seconds";
        
        assertEquals(seconds, Schedule.formatTime(fiveSeconds));
        assertEquals(hoursMinutesSeconds, Schedule.formatTime(oneHourFiftyNineMinutes+fiveSeconds));
        assertEquals(negativeTime, Schedule.formatTime(-1*(oneHourFiftyNineMinutes+fiveSeconds)));
    }
}
