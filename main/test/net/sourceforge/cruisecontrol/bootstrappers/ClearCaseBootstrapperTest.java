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
package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class ClearCaseBootstrapperTest extends TestCase {
    public ClearCaseBootstrapperTest(String name) {
        super(name);
    }

    public void testValidate() {
        ClearCaseBootstrapper cbs = new ClearCaseBootstrapper();

        try {
            cbs.validate();
            fail("ClearCaseBootstrapper should throw an exception when the required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("exception message when required attributes not set",
                    "'file' is required for ClearCaseBootstrapper", e.getMessage());
        }
        cbs.setFile("somefile");
        try {
            cbs.validate();
        } catch (CruiseControlException e) {
            fail("ClearCaseBootstrapper should not throw an exception when the required attributes are set.");
        }
    }

    public void testIsWindows() {
        MockClearCaseBootstrapper cbs = new MockClearCaseBootstrapper("Windows NT");

        assertTrue("ClearCaseBootstrapper does not detect 'Windows NT' correctly", cbs.isWindows());

        cbs.setOsName("Some other operating system");
        assertTrue("ClearCaseBootstrapper does not detect os different from Windows", !cbs.isWindows());
    }

    public void testBuildUpdateCommand() {
        MockClearCaseBootstrapper cbs = new MockClearCaseBootstrapper("Linux");
        cbs.setFile("somefile");
        assertEquals("Update command was not created correctly.",
                "cleartool update -force -log /dev/null somefile", cbs.buildUpdateCommand().toString());

        cbs.setOsName("Windows NT");
        assertEquals("Update command was not created correctly.",
                "cleartool update -force -log NUL somefile", cbs.buildUpdateCommand().toString());

        cbs.setViewpath("someviewpath");
        assertEquals("Update command was not created correctly.",
                "cleartool update -force -log NUL someviewpath/somefile", cbs.buildUpdateCommand().toString());
    }

    private class MockClearCaseBootstrapper extends ClearCaseBootstrapper {

        private String osName;

        public MockClearCaseBootstrapper(String osName) {
            this.osName = osName;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }
    }
}
