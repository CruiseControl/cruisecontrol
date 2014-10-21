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
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitor;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.Configuration;
import net.sourceforge.cruisecontrol.launch.ConfigurationTest;
import net.sourceforge.cruisecontrol.launch.LaunchException;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.jdom.Element;

public class MainTest extends TestCase {
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private final FilesToDelete testFiles = new FilesToDelete(); 

//    public static void setSkipUsage() {
//        System.setProperty(Main.SYSPROP_CCMAIN_SKIP_USAGE, "true");
//    }

    public void tearDown() {
        testFiles.delete();
        // Remove all properties
        for (String p : System.getProperties().stringPropertyNames()) {
            if (p.startsWith("cc.")) {
                System.getProperties().remove(p);
            }
        }
    }

    public void testParsePassword() throws Exception {
        String[] correctArgs = new String[] {"-password", "password"};
        String[] missingValue = new String[] {"-password"};
        String[] missingParam = new String[] {};

        assertEquals("password", Main.parsePassword(new TestConfiguration(correctArgs)));
        assertEquals(null, Main.parsePassword(new TestConfiguration(missingValue)));
        assertEquals(null, Main.parsePassword(new TestConfiguration(missingParam)));
    }

    public void testParseUser() throws Exception {
        String[] correctArgs = new String[] {"-user", "user"};
        String[] missingValue = new String[] {"-user"};
        String[] missingParam = new String[] {};
        assertEquals("user", Main.parseUser(new TestConfiguration(correctArgs)));
        assertEquals(null, Main.parseUser(new TestConfiguration(missingValue)));
        assertEquals(null, Main.parseUser(new TestConfiguration(missingParam)));
    }

    public void testParseConfigurationFileName() throws Exception {
        StringBuilder fullPath = new StringBuilder();
        String[] correctArgs = new String[] {"-configfile", makeFile("myconfig.xml", fullPath)};
        String[] missingParam = new String[] {};
        String[] missingValue = new String[] {"-configfile"};

        // Write empty XML to the file, otherwise configuration will throw LauncherException since
        // the file cannot be read correctly
        Element launchXml = ConfigurationTest.makeLauchXML(Collections.<String, String> emptyMap());
        Element cruiseXml = ConfigurationTest.makeConfigXML(launchXml);
        File f = testFiles.add(new File(fullPath.toString()));
        ConfigurationTest.storeXML(cruiseXml, f);

        assertEquals(fullPath.toString(), Main.parseConfigFileName(new TestConfiguration(correctArgs), null));

        // DEPRECATED
        // The default config file does not exist neither is set, so the name passed to
        // Main.parseConfigFileName() is used as the config ...
        assertEquals("config.xml", Main.parseConfigFileName(new TestConfiguration(missingParam), "config.xml"));
        // And without the name passed
        try {
            Main.parseConfigFileName(new TestConfiguration(missingValue), null);
            fail("Expected CruiseControlException on missing configfile value");
        } catch (CruiseControlException e) {
            // expected
        }
        // ^^^^^

        // Default file - launcher configuration without cruisecontrol configuration element set
        // Uses default name, but the file must exist
        makeFile("cruisecontrol.xml", fullPath);
        f = testFiles.add(new File(fullPath.toString()));
        ConfigurationTest.storeXML(launchXml, f);

        assertEquals(fullPath.toString(), Main.parseConfigFileName(new TestConfiguration(missingParam), "config.xml"));
    }

    public void testParseJettyXml() throws Exception {
        StringBuilder fullPath = new StringBuilder();

        String[] missingValue = new String[] {"-jettyxml"};
        String[] missingParam = new String[] {};
        String[] correctArgs = new String[] {"-jettyxml", makeFile("myJetty.xml", fullPath)};
        assertEquals(fullPath.toString(), Main.parseJettyXml(new TestConfiguration(correctArgs), "ccHome"));

        // Default file does not exist
        try {
            Main.parseJettyXml(new TestConfiguration(missingParam), null);
            fail("Expected IllegalArgumentException on missing jettyxml file");
        } catch (IllegalArgumentException e) {
            // OK
        }

        // make ./ccHome/etc/jetty.xml and ./etc/jetty.xml
        // Both paths must exist, otherwise the configuration will throw IllegalArgumentException
        makeDir(new File(new File("ccHome"), "etc"), fullPath);
        makeFile(new File(fullPath.toString(), "jetty.xml"), fullPath);
        makeDir("etc/", fullPath);
        makeFile(new File(fullPath.toString(), "jetty.xml"), fullPath);

        assertEquals("ccHome/etc/jetty.xml", Main.parseJettyXml(new TestConfiguration(missingParam), "ccHome"));
        assertEquals("etc/jetty.xml", Main.parseJettyXml(new TestConfiguration(missingValue), ""));
    }

    public void testParseDashboardUrl() throws Exception {
        String[] customizedArgs = new String[] {"-dashboardurl", "http://myserver:1234/dashboard"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-dashboardurl"};

        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl(new TestConfiguration(customizedArgs)));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(new TestConfiguration(missingParam)));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(new TestConfiguration(defaultValue)));
    }

    public void testParseDashboardUrlWithWebport() throws Exception {
        String[] onlyWebport = {"-webport", "8585"};
        String[] webportAndUrl = {"-webport", "8585", "-dashboardurl", "http://myserver:1234/dashboard"};
        String[] webportAndDefaultUrl = {"-webport", "8585", "-dashboardurl"};

        assertEquals("http://localhost:8585/dashboard", Main.parseDashboardUrl(new TestConfiguration(onlyWebport)));
        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl(new TestConfiguration(webportAndUrl)));
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl(new TestConfiguration(webportAndDefaultUrl)));
    }

    public void testParseHttpPostingInterval() throws Exception {
        String[] customizedArgs = new String[] {"-postinterval", "1234"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-postinterval"};

        assertEquals(1234, Main.parseHttpPostingInterval(new TestConfiguration(customizedArgs)));
        assertEquals(5, Main.parseHttpPostingInterval(new TestConfiguration(missingParam)));
        assertEquals(5, Main.parseHttpPostingInterval(new TestConfiguration(defaultValue)));
    }

    public void testParsePostingEnabled() throws Exception {
        String[] customizedArgs = new String[] {"-postenabled", "false"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-postenabled"};

        assertFalse(Main.parseHttpPostingEnabled(new TestConfiguration(customizedArgs)));
        assertTrue(Main.parseHttpPostingEnabled(new TestConfiguration(missingParam)));
        assertTrue(Main.parseHttpPostingEnabled(new TestConfiguration(defaultValue)));
    }

    public void testParseHttpPort() throws Exception {
        String[] correctArgs = new String[] {"-jmxport", "123"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-jmxport"};
        String[] invalidArgs = new String[] {"-jmxport", "ABC"};
        String[] deprecatedArgs = new String[] {"-port", "123"};
        String[] deprecatedAndCorrectArgs = new String[] {"-port", "123", "-jmxport", "123"};

        assertEquals(123, Main.parseJMXHttpPort(new TestConfiguration(correctArgs)));
        assertEquals(8000, Main.parseJMXHttpPort(new TestConfiguration(missingParam)));
        assertEquals(8000, Main.parseJMXHttpPort(new TestConfiguration(defaultValue)));
        assertEquals(123, Main.parseJMXHttpPort(new TestConfiguration(deprecatedArgs)));

        try {
            Main.parseJMXHttpPort(new TestConfiguration(invalidArgs));
            fail("Expected IllegalArgumentException on non-int ABC");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Main.parseJMXHttpPort(new TestConfiguration(deprecatedAndCorrectArgs));
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testParseRmiPort() throws Exception {
        String[] correctArgs = new String[] {"-rmiport", "123"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-rmiport"};
        String[] invalidArgs = new String[] {"-rmiport", "ABC"};

        assertEquals(123, Main.parseRmiPort(new TestConfiguration(correctArgs)));
        assertEquals(1099, Main.parseRmiPort(new TestConfiguration(missingParam))); // default value
        assertEquals(1099, Main.parseRmiPort(new TestConfiguration(defaultValue)));

        try {
            Main.parseRmiPort(new TestConfiguration(invalidArgs));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseWebPort() throws Exception {
        String[] correctArgs = new String[] {"-webport", "123"};
        String[] missingParam = new String[] {};
        String[] defaultValue = new String[] {"-webport"};
        String[] invalidArgs = new String[] {"-webport", "ABC"};

        assertEquals(123, Main.parseWebPort(new TestConfiguration(correctArgs)));
        assertEquals(8080, Main.parseWebPort(new TestConfiguration(missingParam)));
        assertEquals(8080, Main.parseWebPort(new TestConfiguration(defaultValue)));

        try {
            Main.parseWebPort(new TestConfiguration(invalidArgs));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testShouldSetWebPortAsSystemProperty() throws Exception {
        final File temporaryConfigFile = File.createTempFile("temp-config", ".xml");
        temporaryConfigFile.deleteOnExit();

        Main.setUpSystemPropertiesForDashboard(temporaryConfigFile.getAbsolutePath(), 123, 456, 789);

        assertEquals("789", System.getProperty("cc.webport"));
    }

    public void testParseWebappPath() throws Exception {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        final File webappDir = new File(tempDirName, "testwebapp");

        String[] correctArgs = new String[] {"-webapppath", webappDir.getAbsolutePath()};
        String[] missingParam = new String[] {};
        String[] missingValue = new String[] {"-webapppath"};
        String[] invalidArgs = new String[] {"-webapppath", "does_not_exist"};

        makeDir(new File(webappDir, "WEB-INF"), new StringBuilder());

        Main theMainClass = new Main();
        assertEquals(webappDir.getAbsolutePath(), theMainClass.parseWebappPath(new TestConfiguration(correctArgs)));

        final String msg = "Option 'webapppath' = '/webapps/cruisecontrol' does not represent existing directory!";
        try {
            theMainClass.parseWebappPath(new TestConfiguration(missingValue));
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(msg, expected.getMessage());
        }
        try {
            theMainClass.parseWebappPath(new TestConfiguration(missingParam));
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(msg, expected.getMessage());
        }

        try {
            theMainClass.parseWebappPath(new TestConfiguration(invalidArgs));
            fail();
        } catch (IllegalArgumentException expected) {
            // OK
        }
    }

    public void testParseXslPath() throws Exception {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        String[] correctArgs = new String[] {"-xslpath", tempDirName};
        String[] missingParam = new String[] {};
        String[] missingValue = new String[] {"-xslpath"};
        final String invalidXsl = "does_Not_Exist";
        String[] invalidArgs = new String[] {"-xslpath", invalidXsl};

        assertEquals(tempDirName, Main.parseXslPath(new TestConfiguration(correctArgs)));
        assertEquals(new File(".").getAbsolutePath(), Main.parseXslPath(new TestConfiguration(missingParam))); // use default value
        assertEquals(new File(".").getAbsolutePath(), Main.parseXslPath(new TestConfiguration(missingValue)));

        try {
            Main.parseXslPath(new TestConfiguration(invalidArgs));
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Option 'xslpath' = '"+invalidXsl+"' does not represent existing directory!",
                    expected.getMessage());
        }
    }

    public void testParseEnableJMXAgentUtility() throws Exception {
        assertEquals("default, if no command line arg present. Not an error if load fails.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.LOAD_IF_AVAILABLE,
                Main.parseEnableJMXAgentUtility(new TestConfiguration(new String[] {})));

        assertEquals("-agentutil true. Considered an error if load fails.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD,
                Main.parseEnableJMXAgentUtility(new TestConfiguration(
                        new String[] {"-agentutil"})));
        assertEquals("-agentutil true. Considered an error if load fails.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD,
                Main.parseEnableJMXAgentUtility(new TestConfiguration(
                        new String[] {"-agentutil", "true"})));

        assertEquals("-agentutil false. Do not attempt to load.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_BYPASS,
                Main.parseEnableJMXAgentUtility(new TestConfiguration(
                        new String[] {"-agentutil", "false"})));
    }

    public void testUsage() throws Exception {
        String[] usage = {"-?"};
        String[] notusage = {"-port", "8000"};
        assertTrue(Main.shouldPrintUsage(new TestConfiguration(usage)));
        assertFalse(Main.shouldPrintUsage(new TestConfiguration(notusage)));
    }

    public void testshouldStartController() throws Exception {
        String[] bothArgs = new String[] {"-jmxport", "8085", "-rmiport", "8086"};
        String[] bothArgsWithDeprecated = new String[] {"-port", "8085", "-rmiport", "8086"};
        String[] rmiPort = new String[] {"-rmiport", "8086"};
        String[] httpPort = new String[] {"-jmxport", "8085"};
        String[] httpPortWithDefault = new String[] {"-jmxport"};
        String[] neitherArg = new String[] {};
        String[] deprecatedHttpPort = new String[] {"-port", "8085"};

        assertTrue(Main.shouldStartJmxAgent(new TestConfiguration(bothArgs)));
        assertTrue(Main.shouldStartJmxAgent(new TestConfiguration(bothArgsWithDeprecated)));
        assertTrue(Main.shouldStartJmxAgent(new TestConfiguration(rmiPort)));
        assertTrue(Main.shouldStartJmxAgent(new TestConfiguration(httpPort)));
        assertFalse(Main.shouldStartJmxAgent(new TestConfiguration(httpPortWithDefault))); // previous was 'true', but this variant seems strange
        assertTrue(Main.shouldStartJmxAgent(new TestConfiguration(deprecatedHttpPort)));
        assertFalse(Main.shouldStartJmxAgent(new TestConfiguration(neitherArg)));
    }

    public void testShouldStartEmbeddedServer() throws Exception {
        String[] bothArgs = new String[] {"-webport", "1234", "-webapppath", "/tmp/foo"};
        String[] webPort = new String[] {"-webport", "1234"};
        String[] webappPath = new String[] {"-webapppath", "/tmp/foo"};
        String[] neitherArg = EMPTY_STRING_ARRAY;

        assertTrue(Main.shouldStartEmbeddedServer(new TestConfiguration(bothArgs)));
        assertTrue(Main.shouldStartEmbeddedServer(new TestConfiguration(webPort)));
        assertTrue(Main.shouldStartEmbeddedServer(new TestConfiguration(webappPath)));
        assertFalse(Main.shouldStartEmbeddedServer(new TestConfiguration(neitherArg)));

    }

    public void testShouldStartBuildLoopMonitor() throws Exception {
        BuildLoopMonitor buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNull(buildLoopMonitor);
        new Main().startPostingToDashboard(new TestConfiguration(EMPTY_STRING_ARRAY));
        buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNotNull(buildLoopMonitor);

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testShouldNOTRestartBuildLoopMonitorIfItAlreadyExisting() throws Exception {
        assertTrue(Main.shouldPostDataToDashboard(new TestConfiguration(EMPTY_STRING_ARRAY)));

        new Main().startPostingToDashboard(new TestConfiguration(EMPTY_STRING_ARRAY));

        assertFalse(Main.shouldPostDataToDashboard(new TestConfiguration(EMPTY_STRING_ARRAY)));

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testDeprecatedArgs() throws Exception {
        String[] args = {"-port", "8000"};

        StringBufferAppender appender = new StringBufferAppender();
        Logger testLogger = Logger.getLogger(Main.class);
        testLogger.addAppender(appender);
        Main.checkDeprecatedArguments(new TestConfiguration(args), testLogger);

        assertTrue(appender.toString().indexOf(
                "WARNING: The port argument is deprecated. Use jmxport instead.") >= 0);
    }

    private String makeDir(final File path, final StringBuilder fullPath) throws IOException  {
        if (!path.exists()) {
            // Create the path and prepare its delete
            testFiles.add(path);
            if (!Util.doMkDirs(path)) {
                throw new IOException("Could not create test " + path.getAbsolutePath() + " dir");
            }
        }
        fullPath.setLength(0);
        fullPath.append(path.getAbsolutePath());

        return path.getName();
    }
    private String makeDir(final String path, final StringBuilder fullPath) throws IOException  {
        return makeDir(new File(path), fullPath);
    }

    private String makeFile(final File name, final StringBuilder fullPath)  {
        fullPath.setLength(0);
        try {
            if (!name.exists()) {
                // Create the file and prepare its delete
                testFiles.add(name);
                if (!name.createNewFile()) {
                    throw new IOException("Could not create test " + name.getAbsolutePath() + " file");
                }
            }

            // Fill full path and get base name
            fullPath.append(name.getAbsolutePath());
            return name.getName();

        } catch (IOException e) {
            fail("Unabel to create file: " + name.getAbsolutePath());
        }

        // should not happen due to fail()
        fullPath.append("path was not set"); // causes test failure anyway ...
        return name.getName();
    }

    private String makeFile(final String name, final StringBuilder fullPath)  {
        return makeFile(new File(name), fullPath);
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
            myBuffer.append(event.getMessage()).append("\n");
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

    /** Override of Configuration, it redefines the constructor to allow the creation of more
     *  instances. */
    public static class TestConfiguration extends Configuration {
        // Public constructor
        public TestConfiguration(String[] args) throws LaunchException, CruiseControlException {
                super(args);
        }
    }
}
