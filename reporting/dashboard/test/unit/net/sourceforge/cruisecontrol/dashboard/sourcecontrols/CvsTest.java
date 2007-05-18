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
package net.sourceforge.cruisecontrol.dashboard.sourcecontrols;

import junit.framework.Assert;
import junitx.util.PrivateAccessor;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.MockRuntime;
import org.jmock.cglib.MockObjectTestCase;

public class CvsTest extends MockObjectTestCase {
    public void testShouldGetCorrectCommandlineForCheckingOut() throws Throwable {
        String url = ":pserver@some.cvs.rep:/rep/path";
        String path = "destination/path";
        String module = "module";
        Cvs cvs = new Cvs(url, module, null);
        String actual = PrivateAccessor
                .invoke(cvs, "getCheckoutCommandLine", new Class[]{String.class}, new Object[]{path}).toString();
        String expected = "cvs -z3 -d " + url + " co -P -d " + path + " " + module;
        assertEquals(expected, actual);
    }

    public void testShouldGetCorrectCommandlineforListingModuleFiles() throws Throwable {
        String url = ":pserver@some.cvs.rep:/rep/path";
        String module = "module";
        Cvs cvs = new Cvs(url, module, null);
        String actual = PrivateAccessor.invoke(cvs, "getListingModuleCommandLine", null, null).toString();
        String expected = "cvs -z3 -d " + url + " rls " + module;
        assertEquals(expected, actual);
    }

    public void testShouldReturnTrueIfBuildFileExists() throws Exception {
        String url = ":pserver@some.cvs.rep:/rep/path";
        String output = "build.xml\n";
        Cvs cvs = new Cvs(url, "module", new MockRuntime(output, false));
        assertTrue(cvs.checkBuildFile());
    }

    public void testShouldReturnFalseIfBuildFileNotExist() throws Exception {
        String url = ":pserver@some.cvs.rep:/rep/path";
        String output = "build not exist\n";
        Cvs cvs = new Cvs(url, "module", new MockRuntime(output, false));
        assertFalse(cvs.checkBuildFile());
    }

    public void testShouldGetCorrectCommandlineForCheckConnection() throws Throwable {
        String url = ":pserver@some.cvs.rep:/rep/path";
        Cvs cvs = new Cvs(url, "module", null);
        String actual = PrivateAccessor.invoke(cvs, "getTestConnectionCommandline", null, null).toString();
        String expected = "cvs -z3 -d " + url + " rlog";
        assertEquals(expected, actual);
    }

    public void testShouldReturnSuccessContextIfConnectionValid() throws Exception {
        String url = ":pserver@some.cvs.rep:/rep/path";
        Cvs cvs = new Cvs(url, "module", new MockRuntime("whatever", false));
        Assert.assertEquals(cvs.checkConnection().status(), ConnectionResult.STATUS_SUCCESS);
    }

    public void testShouldReturnFailedConextIfConnectionInvalid() throws Exception {
        String url = ":pserver@some.cvs.rep:/rep/path";
        Cvs cvs = new Cvs(url, "module", new MockRuntime("whatever", true));
        assertEquals(cvs.checkConnection().status(), ConnectionResult.STATUS_FAILURE);
    }
}
