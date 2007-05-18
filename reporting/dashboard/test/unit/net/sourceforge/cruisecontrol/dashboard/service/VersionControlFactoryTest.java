/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.service;

import junit.framework.TestCase;
import junitx.util.PrivateAccessor;
import net.sourceforge.cruisecontrol.dashboard.exception.NonSupportedVersionControlException;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Cvs;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Perforce;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Svn;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.MockRuntime;

public class VersionControlFactoryTest extends TestCase {
    private VersionControlFactory factory;
    private MockRuntime runtime;

    protected void setUp() throws Exception {
        runtime = new MockRuntime();
        factory = new VersionControlFactory(runtime);
    }

    public void testShouldThrowExceptionIfTypeInValid() throws Exception {
        try {
            factory.getVCSInstance("irrelevant", "url", null, "invalid");
            fail("exception expected");
        } catch (NonSupportedVersionControlException e) {
            // pass
        }
    }

    public void testShouldBeAbleToCreateSvn() throws Exception {
        String url = "url";
        VCS vcs = factory.getVCSInstance("irrelevant", url, null, "svn");

        assertTrue(vcs instanceof Svn);
        assertEquals(url, PrivateAccessor.getField(vcs, "url"));
        assertSame(runtime, PrivateAccessor.getField(vcs, "runtime"));
    }

    public void testShouldBeAbleToCreateCvs() throws Exception {
        String url = "url";
        String module = "module";
        VCS vcs = factory.getVCSInstance("irrelevant", url, module, "cvs");

        assertTrue(vcs instanceof Cvs);
        assertEquals(url, PrivateAccessor.getField(vcs, "url"));
        assertEquals(module, PrivateAccessor.getField(vcs, "module"));
        assertSame(runtime, PrivateAccessor.getField(vcs, "runtime"));
    }

    public void testShouldBeAbleToCreatePerforce() throws Exception {
        String port = "somewhere:1666";
        String depotPath = "//depot/path";
        String projectName = "project name";
        VCS vcs = factory.getVCSInstance(projectName, port, depotPath, "perforce");

        assertTrue(vcs instanceof Perforce);
        assertEquals(port, PrivateAccessor.getField(vcs, "port"));
        assertEquals(depotPath, PrivateAccessor.getField(vcs, "depotPath"));
        assertEquals(projectName, PrivateAccessor.getField(vcs, "clientName"));
        assertSame(runtime, PrivateAccessor.getField(vcs, "runtime"));
    }


}
