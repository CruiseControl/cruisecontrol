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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.jdom.Element;

import junit.framework.TestCase;

/**
 * @author jfredrick
 */
public class BuilderTest extends TestCase {

    private Builder builder;
    private Calendar nov22nd2001;

    public BuilderTest(String name) {
        super(name);
        nov22nd2001 = Calendar.getInstance();
        nov22nd2001.set(2001, Calendar.NOVEMBER, 22, 10, 01, 01);

    }

    protected void setUp() throws Exception {
        builder = new TestBuilder();
    }

    public void testValidate() throws CruiseControlException {
        try {
            builder.validate();
            fail("time or multiple must be set");
        } catch (CruiseControlException e) {
            // should throw exception
        }

        builder.setTime("0000");
        builder.validate();

        builder.setMultiple(1);
        try {
            builder.validate();
            fail("can't set both time and multiple");
        } catch (CruiseControlException e) {
            // should throw exception
        }

        builder.setTime("-1");
        builder.validate();
    }

    public void testIsValidDay() {
        Date thursday = nov22nd2001.getTime();

        assertTrue(builder.isValidDay(thursday));

        builder.setDay("wednesday");
        assertTrue(!builder.isValidDay(thursday));

        builder.setDay("thursday");
        assertTrue(builder.isValidDay(thursday));
    }

    class TestBuilder extends Builder {
        public Element build(Map properties) throws CruiseControlException {
            return null;
        }
    }
}
