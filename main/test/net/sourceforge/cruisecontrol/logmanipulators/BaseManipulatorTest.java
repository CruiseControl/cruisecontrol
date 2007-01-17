/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.logmanipulators;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Calendar;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 *
 * @author hack
 */
public class BaseManipulatorTest extends TestCase {

    public BaseManipulatorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BaseManipulatorTest.class);

        return suite;
    }

    public void testSetUnit() throws CruiseControlException {
        BaseManipulator instance = new BaseManipulatorImpl();

        instance.setUnit("DAY");
        assertEquals(Calendar.DAY_OF_MONTH, instance.getUnit().intValue());

        instance.setUnit("week");
        assertEquals(Calendar.WEEK_OF_YEAR, instance.getUnit().intValue());

        instance.setUnit("MoNtH");
        assertEquals(Calendar.MONTH, instance.getUnit().intValue());

        instance.setUnit("yEAR");
        assertEquals(Calendar.YEAR, instance.getUnit().intValue());

        instance.setUnit("something else");
        assertNull(instance.getUnit());
    }

    private class BaseManipulatorImpl extends BaseManipulator {
        BaseManipulatorImpl() {
            super();
        }
        public void execute(String logDir) {
        }
    }
}
