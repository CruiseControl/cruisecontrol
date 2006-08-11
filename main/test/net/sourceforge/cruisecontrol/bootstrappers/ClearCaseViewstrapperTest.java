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
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class ClearCaseViewstrapperTest extends TestCase {
    public ClearCaseViewstrapperTest(String name) {
        super(name);
    }

    public void testValidate() {
        ClearCaseViewstrapper cvs = new ClearCaseViewstrapper();

        try {
            cvs.validate();
            fail("ClearCaseViewstrapper should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'viewpath' is required for ClearCaseViewstrapper", e.getMessage());
        }
        cvs.setViewpath("M:\\someview\\somevob\\somepath");
        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("ClearCaseViewstrapper should not throw an exception when the required attributes are set.");
        }
    }

    public void testIsWindows() {
        MockClearCaseViewstrapper cvs = new MockClearCaseViewstrapper("Windows NT");

        assertTrue("ClearCaseViewstrapper does not detect 'Windows NT' correctly", cvs.isWindows());

        cvs.setOsName("Some other operating system");
        assertTrue("ClearCaseViewstrapper does not detect os different from Windows", !cvs.isWindows());
    }

    public void testBuildStartViewCommand() {
        MockClearCaseViewstrapper cvs = new MockClearCaseViewstrapper("Linux");
        cvs.setViewpath("/view/someview/somevob/somepath");
        assertEquals("startview command was not created correctly.",
                "cleartool startview someview", cvs.buildStartViewCommand().toString());

        cvs.setViewpath("M:\\someview\\somevob\\somepath");
        cvs.setOsName("Windows NT");
        assertEquals("startview command was not created correctly.",
                "cleartool startview someview", cvs.buildStartViewCommand().toString());
    }
    
    private class MockClearCaseViewstrapper extends ClearCaseViewstrapper {

        private String osName;

        public MockClearCaseViewstrapper(String osName) {
            this.osName = osName;
        }

        public boolean isWindows() {
            return getOsName().indexOf("Windows") != -1;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }
    }
}
