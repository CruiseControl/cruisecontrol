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
import java.util.Hashtable;
import java.text.SimpleDateFormat;

import org.jdom.Element;

public class ScheduleTest extends TestCase {

    private Schedule _schedule;
    private Calendar _cal;
    private Calendar _cal2;
    private Calendar _cal3;
    private Calendar _cal4;

    public ScheduleTest(String name) {
        super(name);
    }

    public void setUp() {
        _schedule = new Schedule();
        MockBuilder builder1 = new MockBuilder();
        builder1.setTime("1200");
        builder1.setBuildLogXML(new Element("builder1"));
        MockBuilder builder2 = new MockBuilder();
        builder2.setMultiple(5);
        builder2.setBuildLogXML(new Element("builder2"));
        MockBuilder builder3 = new MockBuilder();
        builder3.setMultiple(1);
        builder3.setBuildLogXML(new Element("builder3"));
        PauseBuilder pauseBuilder = new PauseBuilder();
        pauseBuilder.setStartTime(2300);
        pauseBuilder.setEndTime(2359);

        _schedule.addBuilder(builder1);
        _schedule.addBuilder(builder2);
        _schedule.addBuilder(builder3);
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
    }

    public void testBuild() {
        try {
            Element testResults = _schedule.build(12, _cal.getTime(), _cal2.getTime(), new Hashtable());
            Element expected = new Element("builder3");
            assertEquals(testResults.toString(), expected.toString());
            Element testResults2 = _schedule.build(10, _cal.getTime(), _cal2.getTime(), new Hashtable());
            Element expected2 = new Element("builder2");
            assertEquals(testResults2.toString(), expected2.toString());
            Element testResults3 = _schedule.build(11, _cal.getTime(), _cal3.getTime(), new Hashtable());
            Element expected3 = new Element("builder1");
            assertEquals(testResults3.toString(), expected3.toString());
        } catch (CruiseControlException e) {
            e.printStackTrace();
        }
    }

    public void testIsPaused() {
        assertEquals(_schedule.isPaused(_cal4.getTime()), true);
        assertEquals(_schedule.isPaused(_cal2.getTime()), false);
    }

    public void testGetTimeFromDate() {
        assertEquals(_schedule.getTimeFromDate(_cal.getTime()), 1001);
        assertEquals(_schedule.getTimeFromDate(_cal2.getTime()), 1101);
    }
}
