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

import java.io.IOException;

import junit.framework.Assert;
import junitx.util.PrivateAccessor;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.MockRuntime;

import org.jmock.cglib.MockObjectTestCase;

public class SvnTest extends MockObjectTestCase {

    public void testShouldGetCorrectCommandLine() throws Throwable {
        String url = "http://thoughtworks.com/svn/";
        Svn svn = new Svn(url, null);
        String actual = PrivateAccessor.invoke(svn, "getCheckConnectionCommandLine", null, null).toString();
        String expected = "svn info --non-interactive " + url;
        assertEquals(expected, actual);
    }

    public void testShouldGetCorrectCommandLineForCheckout() throws Throwable {
        String url = "http://thoughtworks.com/svn/";
        String path = "path";
        Svn svn = new Svn(url, null);
        String actual = PrivateAccessor
                .invoke(svn, "getCheckoutCommandLine", new Class[]{String.class}, new Object[]{path}).toString();
        String expected = "svn co --non-interactive " + url + " " + path;
        assertEquals(expected, actual);
    }

    public void testShouldReturnPositiveWhenRepositoryAccessible() throws IOException, InterruptedException {
        String url = "https://cruisecontrol.svn.sourceforge.net/svnroot/cruisecontrol";
        Svn svn = new Svn(url, new MockRuntime("", true));
        Assert.assertTrue(svn.checkConnection().isValid());
    }

    public void testShouldReturnNegativeAndResponseWhenErrorOccurred() throws Exception {
        String errorMessage = "error emssage";
        Svn svn = new Svn("http://foo", new MockRuntime(errorMessage, true));
        ConnectionResult checkResult = svn.checkConnection();
        assertFalse(checkResult.isValid());
        assertEquals(errorMessage + '\n', checkResult.getMessage());
    }

    public void testShouldReturnTrueIfBuildFileExists() throws Exception {
        String url = "svn://somesvn";
        String output = "build.xml\n";
        Svn svn = new Svn(url, new MockRuntime(output, false));
        assertTrue(svn.checkBuildFile());
    }

    public void testShouldReturnTrueIfBuildFileNotExist() throws Exception {
        String url = "svn://somesvn";
        String output = "build not exist\n";
        Svn svn = new Svn(url, new MockRuntime(output, false));
        assertFalse(svn.checkBuildFile());
    }
}
