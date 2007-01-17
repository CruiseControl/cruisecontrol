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

package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.util.Calendar;

public class PauseBuilderTest extends TestCase {

    private Calendar cal, cal2;

    public PauseBuilderTest(String name) {
        super(name);
    }

    public void setUp() {
        cal = Calendar.getInstance();
        cal.clear();
        cal.set(2001, Calendar.NOVEMBER, 22); //Thursday, November 22, 2001
        cal2 = Calendar.getInstance();
        cal2.clear();
        cal2.set(2001, Calendar.NOVEMBER, 23); //Friday, November 23, 2001
    }

    public void testValidate() {
        PauseBuilder pb = new PauseBuilder();

        try {
            pb.validate();
            fail("PauseBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        pb.setStartTime(1400);
        try {
            pb.validate();
            fail("PauseBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        pb.setEndTime(1500);

        try {
            pb.validate();
        } catch (CruiseControlException e) {
            fail("PauseBuilder should not throw exceptions when required fields are set.");
        }

        try {
            pb.setDay("sUnDaY");
            pb.validate();
            pb.setDay("monday");
            pb.validate();
            pb.setDay("TuesdaY");
            pb.validate();
            pb.setDay("wedNESday");
            pb.validate();
            pb.setDay("Thursday");
            pb.validate();
            pb.setDay("friday");
            pb.validate();
            pb.setDay("SATURDAY");
            pb.validate();
        } catch (CruiseControlException e) {
            fail("PauseBuilder shouldn't throw exception with english names for day of week (case insensitive)");
        }
        try {
            pb.setDay("1");
            pb.validate();
            fail("PauseBuilder requires english names for day of week (case insensitive)");
        } catch (CruiseControlException e) {
        }
    }

    public void testIsPaused() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2002, Calendar.DECEMBER, 23, 18, 00, 00);

        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(2002, Calendar.DECEMBER, 23, 20, 00, 00);

        Calendar calendar3 = Calendar.getInstance();
        calendar3.set(2002, Calendar.DECEMBER, 23, 22, 00, 00);

        Calendar calendar4 = Calendar.getInstance();
        calendar4.set(2002, Calendar.DECEMBER, 24, 3, 00, 00);

        Calendar calendar5 = Calendar.getInstance();
        calendar5.set(2002, Calendar.DECEMBER, 24, 7, 00, 00);

        PauseBuilder pb = new PauseBuilder();
        pb.setStartTime(1900);
        pb.setEndTime(2100);

        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(true, pb.isPaused(calendar2.getTime()));
        assertEquals(false, pb.isPaused(calendar3.getTime()));

        pb.setDay("monday");
        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(true, pb.isPaused(calendar2.getTime()));
        assertEquals(false, pb.isPaused(calendar3.getTime()));

        pb.setDay("tuesday");
        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(false, pb.isPaused(calendar2.getTime()));
        assertEquals(false, pb.isPaused(calendar3.getTime()));

        pb = new PauseBuilder();
        pb.setStartTime(2100);
        pb.setEndTime(500);

        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(true, pb.isPaused(calendar3.getTime()));
        assertEquals(true, pb.isPaused(calendar4.getTime()));
        assertEquals(false, pb.isPaused(calendar5.getTime()));

        pb.setDay("monday");

        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(true, pb.isPaused(calendar3.getTime()));
        assertEquals(true, pb.isPaused(calendar4.getTime()));
        assertEquals(false, pb.isPaused(calendar5.getTime()));

        pb.setDay("tuesday");

        assertEquals(false, pb.isPaused(calendar.getTime()));
        assertEquals(false, pb.isPaused(calendar3.getTime()));
        assertEquals(false, pb.isPaused(calendar4.getTime()));
        assertEquals(false, pb.isPaused(calendar5.getTime()));
    }

}
