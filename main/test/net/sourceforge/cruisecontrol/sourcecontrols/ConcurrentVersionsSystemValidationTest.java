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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.OSEnvironment;

/**
 * @author <a href="mailto:j.c.yip@computer.org">Jason Che-han Yip</a>
 */
public class ConcurrentVersionsSystemValidationTest extends TestCase {

    private ConcurrentVersionsSystem cvs;

    protected void setUp() throws Exception {
        super.setUp();

        cvs = new ConcurrentVersionsSystem();
    }

    public void testPassesValidationIfCvsrootAndModuleSet() throws Exception {
        cvs.setCvsRoot("cvsroot");
        cvs.setModule("module");

        try {
            cvs.validate();
        } catch (CruiseControlException e) {
            fail("Should not have failed validation");
        }

    }

    public void testFailsValidationIfNeitherLocalWorkingCopyNorBothCvsrootAndModuleSet() throws Exception {
        final MockOSEnvironment env = new MockOSEnvironment();
        ConcurrentVersionsSystem mockEnvCvs = new ConcurrentVersionsSystem() {
            protected OSEnvironment getOSEnvironment() {
                return env;
            }
        };

        // set only module
        mockEnvCvs.setModule("module");

        assertFailsValidation(mockEnvCvs);

        // set only cvsroot as parameter
        mockEnvCvs.setModule(null);
        mockEnvCvs.setCvsRoot("cvsroot");

        assertFailsValidation(mockEnvCvs);

        // set only CVSROOT as environment variable
        mockEnvCvs.setCvsRoot(null);
        env.add("CVSROOT", "cvsroot");
        assertFailsValidation(mockEnvCvs);
    }

    public void testLocalWorkingCopyAndCvsrootModuleAreMutuallyExclusive() throws Exception {
        cvs.setLocalWorkingCopy("localWorkingCopy");
        cvs.setModule("module");
        cvs.setCvsRoot("cvsroot");

        assertFailsValidation(cvs);
    }

    public void testFailsValidationIfLocalWorkingCopyDoesNotExist() {
        cvs.setLocalWorkingCopy("localWorkingCopy");

        assertFailsValidation(cvs);
    }

    private void assertFailsValidation(ConcurrentVersionsSystem cvs) {
        try {
            cvs.validate();
            fail("Should fail validation");
        } catch (CruiseControlException e) {
            // expected
        }
    }

    private static class MockOSEnvironment extends OSEnvironment {
        private Map myVariables = new HashMap();

        public void add(String variable, String value) {
            myVariables.put(variable, value);
        }

        public String getVariable(String variable) {
            return (String) myVariables.get(variable);
        }
    }

}
