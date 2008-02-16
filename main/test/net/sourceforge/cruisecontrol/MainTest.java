/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitor;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.util.MainArgs;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class MainTest extends TestCase {

    public void testParsePassword() {
        String[] correctArgs = new String[] {"-password", "password"};
        String[] missingValue = new String[] {"-password"};
        String[] missingParam = new String[] {""};
        assertEquals("password", Main.parsePassword(correctArgs));
        assertEquals(null, Main.parsePassword(missingValue));
        assertEquals(null, Main.parsePassword(missingParam));
    }

    public void testParseUser() {
        String[] correctArgs = new String[] {"-user", "user"};
        String[] missingValue = new String[] {"-user"};
        String[] missingParam = new String[] {""};
        assertEquals("user", Main.parseUser(correctArgs));
        assertEquals(null, Main.parseUser(missingValue));
        assertEquals(null, Main.parseUser(missingParam));
    }

    public void testParseConfigurationFileName() throws Exception {
        String[] correctArgs = new String[] {"-configfile", "myconfig.xml"};
        String[] missingParam = new String[] {""};
        String[] missingValue = new String[] {"-configfile"};

        assertEquals("myconfig.xml", Main.parseConfigFileName(correctArgs, null));
        assertEquals("config.xml", Main.parseConfigFileName(missingParam, "config.xml"));

        try {
            Main.parseConfigFileName(missingValue, null);
            fail("Expected CruiseControlException on missing configfile value");
        } catch (CruiseControlException e) {
            // expected
        }

    }

    public void testParseDashboardUrl() throws Exception {
        String[] customizedArgs = new String[] {"-dashboardurl", "http://myserver:1234/dashboard"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-dashboardurl"};

        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl(customizedArgs));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(missingParam));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(defaultValue));
    }

    public void testParseDashboardUrlWithWebport() throws Exception {
        String[] onlyWebport = {"-webport", "8585"};
        String[] webportAndUrl = {"-webport", "8585", "-dashboardurl", "http://myserver:1234/dashboard"};
        String[] webportAndDefaultUrl = {"-webport", "8585", "-dashboardurl"};

        assertEquals("http://localhost:8585/dashboard", Main.parseDashboardUrl(onlyWebport));
        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl(webportAndUrl));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(webportAndDefaultUrl));
    }

    public void testParseHttpPostingInterval() throws Exception {
        String[] customizedArgs = new String[] {"-postinterval", "1234"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-postinterval"};

        assertEquals(1234, Main.parseHttpPostingInterval(customizedArgs));
        assertEquals(5, Main.parseHttpPostingInterval(missingParam));
        assertEquals(5, Main.parseHttpPostingInterval(defaultValue));
    }

    public void testParsePostingEnabled() throws Exception {
        String[] customizedArgs = new String[] {"-postenabled", "false"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-postenabled"};

        assertFalse(Main.parseHttpPostingEnabled(customizedArgs));
        assertTrue(Main.parseHttpPostingEnabled(missingParam));
        assertTrue(Main.parseHttpPostingEnabled(defaultValue));
    }

    public void testParseHttpPort() throws Exception {
        String[] correctArgs = new String[] {"-jmxport", "123"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-jmxport"};
        String[] invalidArgs = new String[] {"-jmxport", "ABC"};
        String[] deprecatedArgs = new String[] {"-port", "123"};
        String[] deprecatedAndCorrectArgs = new String[] {"-port", "123", "-jmxport", "123"};

        assertEquals(123, Main.parseJMXHttpPort(correctArgs));
        assertEquals(MainArgs.NOT_FOUND, Main.parseJMXHttpPort(missingParam));
        assertEquals(8000, Main.parseJMXHttpPort(defaultValue));
        assertEquals(123, Main.parseJMXHttpPort(deprecatedArgs));

        try {
            Main.parseJMXHttpPort(invalidArgs);
            fail("Expected IllegalArgumentException on non-int ABC");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Main.parseJMXHttpPort(deprecatedAndCorrectArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testParseRmiPort() throws Exception {
        String[] correctArgs = new String[] {"-rmiport", "123"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-rmiport"};
        String[] invalidArgs = new String[] {"-rmiport", "ABC"};

        assertEquals(123, Main.parseRmiPort(correctArgs));
        assertEquals(MainArgs.NOT_FOUND, Main.parseRmiPort(missingParam));
        assertEquals(1099, Main.parseRmiPort(defaultValue));

        try {
            Main.parseRmiPort(invalidArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseWebPort() throws Exception {
        String[] correctArgs = new String[] {"-webport", "123"};
        String[] missingParam = new String[] {""};
        String[] defaultValue = new String[] {"-webport"};
        String[] invalidArgs = new String[] {"-webport", "ABC"};

        assertEquals(123, Main.parseWebPort(correctArgs));
        assertEquals(MainArgs.NOT_FOUND, Main.parseWebPort(missingParam));
        assertEquals(8080, Main.parseWebPort(defaultValue));

        try {
            Main.parseWebPort(invalidArgs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseWebappPath() throws Exception {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        final File webappDir = new File(tempDirName, "testwebapp");
        final File webinfDir = new File(webappDir, "WEB-INF");

        try {
            String[] correctArgs = new String[] {"-webapppath", webappDir.getAbsolutePath()};
            String[] missingParam = new String[] {""};
            String[] missingValue = new String[] {"-webapppath"};
            String[] invalidArgs = new String[] {"-webapppath", "does_not_exist"};

            if (!Util.doMkDirs(webinfDir)) {
                throw new Exception("Could not create test webapp dir");
            }
            webappDir.deleteOnExit();
            webinfDir.deleteOnExit();

            Main theMainClass = new Main();
            assertEquals(webappDir.getAbsolutePath(), theMainClass.parseWebappPath(correctArgs));

            final String msg =
                    "'webapppath' argument must specify an "
                            + "existing directory but was ./webapps/cruisecontrol";
            try {
                theMainClass.parseWebappPath(missingValue);
                fail();
            } catch (IllegalArgumentException expected) {
                assertEquals(msg, expected.getMessage());
            }
            try {
                theMainClass.parseWebappPath(missingParam);
                fail();
            } catch (IllegalArgumentException expected) {
                assertEquals(msg, expected.getMessage());
            }

            try {
                theMainClass.parseWebappPath(invalidArgs);
                fail();
            } catch (IllegalArgumentException expected) {
            }

        } finally {
            webinfDir.delete();
            webappDir.delete();
        }

    }

    public void testParseXslPath() {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        String[] correctArgs = new String[] {"-xslpath", tempDirName};
        String[] missingParam = new String[] {""};
        String[] missingValue = new String[] {"-xslpath"};
        final String invalidXsl = "does_Not_Exist";
        String[] invalidArgs = new String[] {"-xslpath", invalidXsl};

        assertEquals(tempDirName, Main.parseXslPath(correctArgs));
        assertNull(Main.parseXslPath(missingParam));
        assertNull(Main.parseXslPath(missingValue));

        try {
            Main.parseXslPath(invalidArgs);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("'xslpath' argument must specify an existing directory but was " + invalidXsl,
                    expected.getMessage());
        }
    }

    public void testUsage() {
        String[] usage = {"-?"};
        String[] notusage = {"-port", "8000"};
        assertTrue(Main.shouldPrintUsage(usage));
        assertFalse(Main.shouldPrintUsage(notusage));
    }

    public void testshouldStartController() throws Exception {
        String[] bothArgs = new String[] {"-jmxport", "8085", "-rmiport", "8086"};
        String[] bothArgsWithDeprecated = new String[] {"-port", "8085", "-rmiport", "8086"};
        String[] rmiPort = new String[] {"-rmiport", "8086"};
        String[] httpPort = new String[] {"-jmxport", "8085"};
        String[] httpPortWithDefault = new String[] {"-jmxport"};
        String[] neitherArg = new String[] {"-foo", "blah"};
        String[] deprecatedHttpPort = new String[] {"-port", "8085"};

        assertTrue(Main.shouldStartJmxAgent(bothArgs));
        assertTrue(Main.shouldStartJmxAgent(bothArgsWithDeprecated));
        assertTrue(Main.shouldStartJmxAgent(rmiPort));
        assertTrue(Main.shouldStartJmxAgent(httpPort));
        assertTrue(Main.shouldStartJmxAgent(httpPortWithDefault));
        assertTrue(Main.shouldStartJmxAgent(deprecatedHttpPort));
        assertFalse(Main.shouldStartJmxAgent(neitherArg));
    }

    public void testShouldStartEmbeddedServer() throws Exception {
        String[] bothArgs = new String[] {"-webport", "1234", "-webapppath", "/tmp/foo"};
        String[] webPort = new String[] {"-webport", "1234"};
        String[] webappPath = new String[] {"-webapppath", "/tmp/foo"};
        String[] neitherArg = new String[] {};

        assertTrue(Main.shouldStartEmbeddedServer(bothArgs));
        assertTrue(Main.shouldStartEmbeddedServer(webPort));
        assertTrue(Main.shouldStartEmbeddedServer(webappPath));
        assertFalse(Main.shouldStartEmbeddedServer(neitherArg));

    }

    public void testShouldStartBuildLoopMonitor() throws Exception {
        BuildLoopMonitor buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNull(buildLoopMonitor);
        new Main().startPostingToDashboard(new String[0]);
        buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNotNull(buildLoopMonitor);

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testShouldNOTRestartBuildLoopMonitorIfItAlreadyExisting() throws Exception {
        assertTrue(Main.shouldPostDataToDashboard(new String[0]));

        new Main().startPostingToDashboard(new String[0]);

        assertFalse(Main.shouldPostDataToDashboard(new String[0]));

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testDeprecatedArgs() {
        String[] args = {"-port", "8000"};

        StringBufferAppender appender = new StringBufferAppender();
        Logger testLogger = Logger.getLogger(Main.class);
        testLogger.addAppender(appender);
        Main.checkDeprecatedArguments(args, testLogger);

        assertTrue(appender.toString().indexOf(
                "WARNING: The port argument is deprecated. Use jmxport instead.") >= 0);
    }

    public static class StringBufferAppender implements Appender {
        private final StringBuffer myBuffer = new StringBuffer();

        public void addFilter(Filter filter) {
            throw new UnsupportedOperationException();
        }

        public Filter getFilter() {
            throw new UnsupportedOperationException();
        }

        public void clearFilters() {
            throw new UnsupportedOperationException();
        }

        public void close() {
            throw new UnsupportedOperationException();
        }

        public void doAppend(LoggingEvent event) {
            myBuffer.append(event.getMessage() + "\n");
        }

        public String getName() {
            throw new UnsupportedOperationException();
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
            throw new UnsupportedOperationException();
        }

        public ErrorHandler getErrorHandler() {
            throw new UnsupportedOperationException();
        }

        public void setLayout(Layout layout) {
            throw new UnsupportedOperationException();
        }

        public Layout getLayout() {
            throw new UnsupportedOperationException();
        }

        public void setName(String s) {
            throw new UnsupportedOperationException();
        }

        public boolean requiresLayout() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return myBuffer.toString();
        }
    }

}
