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

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.Options;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitor;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Util;

public class MainTest extends TestCase {
    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private final FilesToDelete testFiles = new FilesToDelete();

    private final Object configOwner = new Object();

    @Override
    public void tearDown() {
        CruiseControlOptions.delInstance(configOwner);
        testFiles.delete();
        // Remove all properties
        for (String p : System.getProperties().stringPropertyNames()) {
            if (p.startsWith("cc.")) {
                System.getProperties().remove(p);
            }
        }
    }

    public void testParsePassword() throws Exception {
        fillOptions(null);
        assertEquals(null, Main.parsePassword()); // use default value

        fillOptions(new String[] {"password", "password"});
        assertEquals("password", Main.parsePassword());
    }

    public void testParseUser() throws Exception {
        fillOptions(null);
        assertEquals(null, Main.parseUser());

        fillOptions(new String[] {"user", "user"});
        assertEquals("user", Main.parseUser());
    }

    public void testParseJettyXml() throws Exception {
        StringBuilder fullPath = new StringBuilder();

        fillOptions(new String[] {"jettyxml", makeFile("myJetty.xml", fullPath)});
        assertEquals(fullPath.toString(), Main.parseJettyXml(new File("ccHome")));

        // Default file does not exist
        try {
            fillOptions(new String[] {});
            Main.parseJettyXml(null);
            fail("Expected IllegalArgumentException on missing jettyxml file");
        } catch (CruiseControlException e) {
            // OK
        }

        // make ./ccHome/etc/jetty.xml and ./etc/jetty.xml
        // Both paths must exist, otherwise the configuration will throw IllegalArgumentException
        makeDir(new File(new File("ccHome"), "etc"), fullPath);
        makeFile(new File(fullPath.toString(), "jetty.xml"), fullPath);

        fillOptions(new String[] {});
        assertEquals(new File("ccHome/etc/jetty.xml").getAbsolutePath(), Main.parseJettyXml(new File("ccHome")));

        makeDir("etc/", fullPath);
        makeFile(new File(fullPath.toString(), "jetty.xml"), fullPath);
    }

    public void testParseDashboardUrl() throws Exception {
        fillOptions(new String[] {"dashboardurl", "http://myserver:1234/dashboard"});
        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl());

        fillOptions(null);
        assertEquals("http://localhost:8080/dashboard", Main.parseDashboardUrl());
    }

    public void testParseDashboardUrlWithWebport() throws Exception {
        fillOptions(new String[] {"webport", "8585"});
        assertEquals("http://localhost:8585/dashboard", Main.parseDashboardUrl());

        fillOptions(new String[] {"webport", "8585", "dashboardurl", "http://myserver:1234/dashboard"});
        assertEquals("http://myserver:1234/dashboard", Main.parseDashboardUrl());
    }

    public void testParseHttpPostingInterval() throws Exception {
        fillOptions(new String[] {"postinterval", "1234"});
        assertEquals(1234, Main.parseHttpPostingInterval());

        fillOptions(new String[] {});
        assertEquals(5, Main.parseHttpPostingInterval());
    }

    public void testParsePostingEnabled() throws Exception {
        fillOptions(new String[] {"postenabled", "false"});
        assertFalse(Main.parseHttpPostingEnabled());

        fillOptions(new String[] {});
        assertTrue(Main.parseHttpPostingEnabled());
    }

    public void testParseHttpPort() throws Exception {
        fillOptions(new String[] {"jmxport", "123"});
        assertEquals(123, Main.parseJMXHttpPort());

        fillOptions(new String[] {});
        assertEquals(8000, Main.parseJMXHttpPort());

        fillOptions(new String[] {"port", "123"});
        assertEquals(123, Main.parseJMXHttpPort());

        try {
            fillOptions(new String[] {"jmxport", "ABC"});
            Main.parseJMXHttpPort();
            fail("Expected IllegalArgumentException on non-int ABC");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            fillOptions(new String[] {"-port", "123", "-jmxport", "123"});
            Main.parseJMXHttpPort();
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testParseRmiPort() throws Exception {
        fillOptions(new String[] {"rmiport", "123"});
        assertEquals(123, Main.parseRmiPort());

        fillOptions(null);
        assertEquals(1099, Main.parseRmiPort()); // default value

        try {
            fillOptions(new String[] {"rmiport", "ABC"});
            Main.parseRmiPort();
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testParseWebPort() throws Exception {
        fillOptions(new String[] {"webport", "123"});
        assertEquals(123, Main.parseWebPort());

        fillOptions(null);
        assertEquals(8080, Main.parseWebPort());

        try {
            fillOptions(new String[] {"webport", "ABC"});
            Main.parseWebPort();
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testShouldSetWebPortAsSystemProperty() throws Exception {
        final File temporaryOptionsFile = testFiles.add("temp-config", ".xml");

        Main.setUpSystemPropertiesForDashboard(temporaryOptionsFile.getAbsolutePath(), 123, 456, 789);
        assertEquals("789", System.getProperty("cc.webport"));
    }

    public void testParseWebappPath() throws Exception {
        final String tempDirName = testFiles.tmpdir().getAbsolutePath();
        final File webappDir = testFiles.add(new File(tempDirName, "testwebapp"));
        final Main theMainClass = new Main();

        makeDir(new File(webappDir, "WEB-INF"), new StringBuilder());

        fillOptions(new String[] {"webapppath", webappDir.getAbsolutePath()});
        assertEquals(webappDir.getAbsolutePath(), theMainClass.parseWebappPath());

        // invalid path
        fillOptions(new String[] {});

        final File path = new File(CruiseControlOptions.getInstance().getOptionRaw(CruiseControlOptions.KEY_WEBAPP_PATH));
        final String msg = "Option 'webapppath' = '" + path.getAbsolutePath() + "' does not represent existing directory!";

        assertFalse(path.exists());

        try {
            theMainClass.parseWebappPath();
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(msg, expected.getMessage());
        } catch (CruiseControlException expected) {
            assertEquals(msg, expected.getMessage());
        }

        try {
            fillOptions(new String[] {"webapppath", "does_not_exist"});
            theMainClass.parseWebappPath();
            fail();
        } catch (IllegalArgumentException expected) {
            // OK
        }
    }

    public void testParseXslPath() throws Exception {
        final File tempDirName = testFiles.tmpdir().getAbsoluteFile();
        final String invalidXsl = "does_Not_Exist";

        fillOptions(new String[] {"xslpath", tempDirName.getPath()});
        assertEquals(tempDirName.getAbsolutePath(), Main.parseXslPath());

        fillOptions(null);
        assertEquals(new File(".").getAbsolutePath(), Main.parseXslPath()); // use default value

        try {
            fillOptions(new String[] {"xslpath", invalidXsl});
            Main.parseXslPath();
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("xslpath=" + invalidXsl + ": file does not exist",
                    expected.getMessage());
        }
    }

    public void testParseEnableJMXAgentUtility() throws Exception {
        fillOptions(null);
        assertEquals("default, if no command line arg present. Not an error if load fails.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.LOAD_IF_AVAILABLE,
                Main.parseEnableJMXAgentUtility());

        fillOptions(new String[] {"agentutil", "true"});
        assertEquals("-agentutil true. Considered an error if load fails.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD,
                Main.parseEnableJMXAgentUtility());

        fillOptions(new String[] {"agentutil", "false"});
        assertEquals("-agentutil false. Do not attempt to load.",
                CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_BYPASS,
                Main.parseEnableJMXAgentUtility());
    }

    public void testUsage() throws Exception {
        fillOptions(new String[] {"?", "true"}); // must pass two args, default is false
        assertTrue(Main.shouldPrintUsage());

        fillOptions(new String[] {"port", "8000"});
        assertFalse(Main.shouldPrintUsage());

        fillOptions(null);
        assertFalse(Main.shouldPrintUsage());
    }

    public void testshouldStartController() throws Exception {
        fillOptions(new String[] {"jmxport", "8055", "rmiport", "8056"});
        assertTrue(Main.shouldStartJmxAgent());

        fillOptions(new String[] {"port", "8085", "rmiport", "8086"});
        assertTrue(Main.shouldStartJmxAgent());

        fillOptions(new String[] {"rmiport", "8086"});
        assertTrue(Main.shouldStartJmxAgent());

        fillOptions(new String[] {"jmxport", "8085"});
        assertTrue(Main.shouldStartJmxAgent());

        fillOptions(new String[] {"port", "8085"});
        assertTrue(Main.shouldStartJmxAgent());

        fillOptions(null);
        assertFalse(Main.shouldStartJmxAgent());
    }

    public void testShouldStartEmbeddedServer() throws Exception {
        makeFile("/tmp/foo", new StringBuilder());

        fillOptions(new String[] {"webport", "1234", "webapppath", "/tmp/foo"});
        assertTrue(Main.shouldStartEmbeddedServer());

        fillOptions(new String[] {"webport", "1234"});
        assertTrue(Main.shouldStartEmbeddedServer());

        fillOptions(new String[] {"webapppath", "/tmp/foo"});
        assertTrue(Main.shouldStartEmbeddedServer());

        fillOptions(null);
        assertFalse(Main.shouldStartEmbeddedServer());
    }

    public void testShouldStartBuildLoopMonitor() throws Exception {
        fillOptions(null);

        BuildLoopMonitor buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNull(buildLoopMonitor);
        new Main().startPostingToDashboard();
        buildLoopMonitor = BuildLoopMonitorRepository.getBuildLoopMonitor();
        assertNotNull(buildLoopMonitor);

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testShouldNOTRestartBuildLoopMonitorIfItAlreadyExisting() throws Exception {
        fillOptions(null);

        assertTrue(Main.shouldPostDataToDashboard());
        new Main().startPostingToDashboard();
        assertFalse(Main.shouldPostDataToDashboard());

        BuildLoopMonitorRepository.cancelPosting();
    }

    public void testDeprecatedArgs() throws Exception {
        StringBufferAppender appender = new StringBufferAppender();
        Logger testLogger = Logger.getLogger(Main.class);
        testLogger.addAppender(appender);

        fillOptions(new String[] {"port", "8000"});
        Main.checkDeprecatedArguments();

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

    /**
     * Fills the config witht the given arguments
     * @param args the even list of key-value strings to be filled into config
     * @throws CruiseControlException
     */
    private void fillOptions(String[] args) throws CruiseControlException {
        CruiseControlOptions.delInstance(configOwner);
        Options cfg = CruiseControlOptions.getInstance(configOwner);

        // Leave with clear config when no arguments were set
        if (args == null) {
            return;
        }

        // Fill them
        assertEquals("Number of arguments must be even: " + args, 0,  args.length % 2);
        for (int i = 0; i < args.length; i+= 2) {
            cfg.setOption(args[i], args[i+1], configOwner);
        }
    }

    public static class StringBufferAppender implements Appender {
        private final StringBuffer myBuffer = new StringBuffer();

        @Override
        public void addFilter(Filter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Filter getFilter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearFilters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void doAppend(LoggingEvent event) {
            myBuffer.append(event.getMessage()).append("\n");
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setErrorHandler(ErrorHandler errorHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ErrorHandler getErrorHandler() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLayout(Layout layout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Layout getLayout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setName(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean requiresLayout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return myBuffer.toString();
        }
    }
}
