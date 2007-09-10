/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.UtilLocator;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

public class AntScriptTest extends TestCase {
    private AntScript script;
    private AntBuilder unixBuilder;
    private AntBuilder windowsBuilder;
    private Hashtable properties;
    private static final boolean USE_LOGGER = true;
    private static final boolean USE_SCRIPT = true;
    private static final boolean IS_WINDOWS = true;
    private static final String UNIX_PATH = "/usr/java/jdk1.5.0/lib/tools.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/dist/cruisecontrol.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/log4j.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/jdom.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/xerces.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/xalan.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/jakarta-oro-2.0.8.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mail.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/junit.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/activation.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/commons-net-1.1.0.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/starteam-sdk.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-tools.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-remote.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/smack.jar:.";
    private static final String WINDOWS_PATH = "C:\\Progra~1\\IBM\\WSAD\\tools.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\dist\\cruisecontrol.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\log4j.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jdom.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xerces.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xalan.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jakarta-oro-2.0.8.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mail.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\junit.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\activation.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\commons-net-1.1.0.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\starteam-sdk.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-tools.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-remote.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\smack.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\comm.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\x10.jar;.";

    protected void setUp() throws Exception {
        script = new AntScript();


        properties = new Hashtable();
        properties.put("label", "200.1.23");

        // default setup of script
        script.setBuildProperties(properties);
        script.setArgs(new ArrayList());
        script.setProperties(new ArrayList());
        script.setLibs(new ArrayList());
        script.setListeners(new ArrayList());
        script.setBuildFile("buildfile");
        script.setTarget("target");

        unixBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return UNIX_PATH;
            }
        };
        unixBuilder.setTarget("target");
        unixBuilder.setBuildFile("buildfile");

        windowsBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return WINDOWS_PATH;
            }
        };
        windowsBuilder.setTarget("target");
        windowsBuilder.setBuildFile("buildfile");
    }

    public void testGetAntLauncherJarLocationForWindows() throws Exception {
        assertEquals("C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar",
                     script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS));
    }

    public void testGetAntLauncherJarLocationForUnix() throws Exception {
        assertEquals("/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS));
    }

    public void testGetCommandLineArgs() throws CruiseControlException {
        String[] resultInfo =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertArray(
                "Logger set to INFO",
                resultInfo,
            script.buildCommandline().getCommandline());


        String[] resultLogger =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-logfile",
                "log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        TestUtil.assertArray(
                "Using result Logger",
                resultLogger,
            script.buildCommandline().getCommandline());


    }

    public void testGetCommandLineArgs_EmptyLogger() throws CruiseControlException {
        String[] resultInfo =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        properties.put("label", "");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        TestUtil.assertArray(
                "resultInfo",
                resultInfo,
            script.buildCommandline().getCommandline());


        String[] resultLogger =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-logfile",
                "log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        script.setUseLogger(USE_LOGGER);
        script.setUseScript(!USE_SCRIPT);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultLogger",
                resultLogger,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_Debug() throws CruiseControlException {
        String[] resultDebug =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-logfile",
                "log.xml",
                "-debug",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };

        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setUseDebug(true);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultDebug",
                resultDebug,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_DebugWithListener() throws CruiseControlException {
             String[] resultDebug =
             {
                 "java",
                 "-classpath",
                 script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                 "org.apache.tools.ant.launch.Launcher",
                 "-lib",
                 UNIX_PATH,
                 "-listener",
                 "org.apache.tools.ant.XmlLogger",
                 "-DXmlLogger.file=log.xml",
                 "-debug",
                 "-Dlabel=200.1.23",
                 "-buildfile",
                 "buildfile",
                 "target" };
             script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
             script.setBuildProperties(properties);
             script.setUseLogger(!USE_LOGGER);
             script.setWindows(!IS_WINDOWS);
             script.setUseScript(!USE_SCRIPT);
             script.setSystemClassPath(UNIX_PATH);
             script.setUseDebug(true);
        TestUtil.assertArray(
                     "debug with listener",
                     resultDebug,
            script.buildCommandline().getCommandline());
         }

    public void testGetCommandLineArgs_Quiet() throws CruiseControlException {
        String[] resultQuiet =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-logfile",
                "log.xml",
                "-quiet",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(UNIX_PATH);
        script.setUseQuiet(true);
        TestUtil.assertArray(
                "resultQuiet",
                resultQuiet,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_KeepGoingDebug() throws CruiseControlException {
        String[] resultDebug =
            {
                "java",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-logfile",
                "log.xml",
                "-keep-going",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };

        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setKeepGoing(true);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultDebug",
                resultDebug,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_MaxMemory() throws CruiseControlException {
        String[] resultWithMaxMemory =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        List args = new ArrayList();
        args.add(arg);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultWithMaxMemory",
                resultWithMaxMemory,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_MaxMemoryAndProperty() throws CruiseControlException {
        String[] resultWithMaxMemoryAndProperty =
            {
                "java",
                "-Xmx256m",
                "-classpath",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                UNIX_PATH,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-Dfoo=bar",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        Property prop = unixBuilder.createProperty();
        prop.setName("foo");
        prop.setValue("bar");

        List args = new ArrayList();
        args.add(arg);
        List props = new ArrayList();
        props.add(prop);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setProperties(props);
        script.setSystemClassPath(UNIX_PATH);
        TestUtil.assertArray(
                "resultWithMaxMemoryAndProperty",
                resultWithMaxMemoryAndProperty,
            script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_BatchFile() throws CruiseControlException {
        String[] resultBatchFile =
            {
                "ant.bat",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setAntScript("ant.bat");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);
        TestUtil.assertArray(
                "resultBatchFile",
                resultBatchFile,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_ShellScript() throws CruiseControlException {
        String[] resultShellScript =
            {
                "ant.sh",
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setAntScript("ant.sh");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);
        TestUtil.assertArray(
                "resultShellScript",
                resultShellScript,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_AlternateLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-listener",
                "com.canoo.Logger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);


        TestUtil.assertArray(
                "args",
                args,
            script.buildCommandline().getCommandline());

    }

    static class MockProgress implements Progress {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public void testSetupResolvedLoggerClassname() throws Exception {
        // set to same default used by AntBuilder
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setProgress(null);
        script.setupResolvedLoggerClassname();
        assertEquals(AntBuilder.DEFAULT_LOGGER, script.getLoggerClassName());

        final Progress progress = new AntScriptTest.MockProgress();
        script.setProgress(progress);
        script.setupResolvedLoggerClassname();
        assertEquals(AntProgressLogger.class.getName(), script.getLoggerClassName());

        // set to same default used by AntBuilder
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setUseLogger(true);
        script.setProgress(null);
        script.setupResolvedLoggerClassname();
        assertEquals(AntBuilder.DEFAULT_LOGGER, script.getLoggerClassName());
        script.setProgress(progress);
        script.setupResolvedLoggerClassname();
        assertEquals(AntProgressXmlLogger.class.getName(), script.getLoggerClassName());

        final String dummyLogger = "dummyLogger";
        script.setUseLogger(false);
        script.setLoggerClassName(dummyLogger);
        script.setIsLoggerClassNameSet(true);
        script.setProgress(null);
        script.setupResolvedLoggerClassname();
        assertEquals(dummyLogger, script.getLoggerClassName());
        script.setProgress(progress);
        script.setupResolvedLoggerClassname();
        assertEquals(dummyLogger, script.getLoggerClassName());

        script.setUseLogger(true);
        script.setLoggerClassName(dummyLogger);
        script.setProgress(null);
        script.setupResolvedLoggerClassname();
        assertEquals(dummyLogger, script.getLoggerClassName());
        script.setProgress(progress);
        script.setupResolvedLoggerClassname();
        assertEquals(dummyLogger, script.getLoggerClassName());
    }

    /**
     * To avoid dependency and classpath issues, the custom AntLoggers/Listeners should NOT be
     * referenced by CC production classes directly. The test below ensures the common definitions
     * of constant strings in independent classes remain equal.
     */
    public void testIndependentContantsAreEqual() {
        assertEquals(AntScript.MSG_PREFIX_ANT_PROGRESS, AntProgressLogger.MSG_PREFIX_ANT_PROGRESS);

        assertEquals(AntScript.CLASSNAME_ANTPROGRESS_LOGGER, AntProgressLogger.class.getName());
        assertEquals(AntScript.CLASSNAME_ANTPROGRESS_XML_LISTENER, AntProgressXmlListener.class.getName());
        assertEquals(AntScript.CLASSNAME_ANTPROGRESS_XML_LOGGER, AntProgressXmlLogger.class.getName());
    }

    public void testDefaultProgressLoggerLib() throws Exception {
        try {
            AntScript.findDefaultProgressLoggerLib();
            fail("Shouldn't find ProgressLoggerLib in classes tree.");
        } catch (AntScript.ProgressLibLocatorException e) {
            assertTrue(e.getMessage().startsWith("The progressLoggerLib jar file does not exist where expected: "));
        }

        try {
            script.setupDefaultProgressLoggerLib();
            fail("Shouldn't find ProgressLoggerLib in classes tree.");
        } catch (AntScript.ProgressLibLocatorException e) {
            assertTrue(e.getMessage().startsWith("The progressLoggerLib jar file does not exist where expected: "));
        }
    }

    public void testGetCommandLineArgs_ProgressLoggerNotUseLogger() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-logger",
                AntProgressLogger.class.getName(),
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-listener",
                AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-lib",
                getLib(),
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntProgressLogger.class.getName());
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new MockProgress());

        final File fakeJar = createFakeProgressLoggerLib();
        try {
            TestUtil.assertArray(
                    "args",
                    args,
                script.buildCommandline().getCommandline());
        } finally {
            fakeJar.delete();
        }
    }

    public void testGetCommandLineArgs_ProgressLoggerUseLogger() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-logger",
                AntProgressXmlLogger.class.getName(),
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-listener",
                AntProgressXmlListener.class.getName(),
                "-DXmlLogger.file=log.xml",
                "-lib",
                getLib(),
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntProgressXmlLogger.class.getName());
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new MockProgress());

        final File fakeJar = createFakeProgressLoggerLib();
        try {
            TestUtil.assertArray(
                    "args",
                    args,
                script.buildCommandline().getCommandline());
        } finally {
            fakeJar.delete();
        }
    }

    public void testGetCommandLineArgs_ProgressLoggerLibNotUseLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-logger",
                AntProgressLogger.class.getName(),
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-listener",
                AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-lib",
                "c:\\DirWithAntProgressLoggerJar",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntProgressLogger.class.getName());
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new MockProgress());
        script.setProgressLoggerLib("c:\\DirWithAntProgressLoggerJar");

        TestUtil.assertArray(
                "args",
                args,
            script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ProgressLoggerLibUseLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-logger",
                AntProgressXmlLogger.class.getName(),
                "-listener",
                AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-listener",
                AntProgressXmlListener.class.getName(),
                "-DXmlLogger.file=log.xml",
                "-lib",
                "c:\\DirWithAntProgressLoggerJar",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntProgressXmlLogger.class.getName());
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new MockProgress());
        script.setProgressLoggerLib("c:\\DirWithAntProgressLoggerJar");

        TestUtil.assertArray(
                "args",
                args,
            script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_PropertyFile() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath",
                script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                WINDOWS_PATH,
                "-listener",
                AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-propertyfile",
                "testPropertyFile.properties",    
                "-buildfile",
                "buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setPropertyFile("testPropertyFile.properties");

        TestUtil.assertArray(
                "args",
                args,
            script.buildCommandline().getCommandline());

    }

    public void testGetCommandLineArgs_MultipleLibs() throws CruiseControlException {
        String[] args =
         {
             "java.exe",
             "-classpath",
             script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
             "org.apache.tools.ant.launch.Launcher",
             "-lib",
             WINDOWS_PATH,
             "-listener",
             "com.canoo.Logger",
             "-DXmlLogger.file=log.xml",
             "-lib",
             "c:\\somedir",
             "-Dlabel=200.1.23",
             "-buildfile",
             "buildfile",
             "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        AntBuilder.Lib lib = windowsBuilder.new Lib();
        lib.setSearchPath("c:\\somedir");
        List libs = new ArrayList();
        libs.add(lib);
        script.setLibs(libs);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);


        TestUtil.assertArray(
             "args",
             args,
             script.buildCommandline().getCommandline());    
     }

    public void testGetCommandLineArgs_MultipleListeners() throws CruiseControlException {
        String[] args =
         {
             "java.exe",
             "-classpath",
             script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS),
             "org.apache.tools.ant.launch.Launcher",
             "-lib",
             WINDOWS_PATH,
             "-listener",
             "com.canoo.Logger",
             "-DXmlLogger.file=log.xml",
             "-listener",
             "org.apache.tools.ant.listener.Log4jListener",
             "-Dlabel=200.1.23",
             "-buildfile",
             "buildfile",
             "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        AntBuilder.Listener listener = windowsBuilder.new Listener();
        listener.setClassName("org.apache.tools.ant.listener.Log4jListener");
        List listeners = new ArrayList();
        listeners.add(listener);
        script.setListeners(listeners);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);


        TestUtil.assertArray(
             "args",
             args,
             script.buildCommandline().getCommandline());
     }

    public void testConsumeLine() throws Exception {
        final Progress progress = new MockProgress();
        script.setProgress(progress);

        assertNull(progress.getValue());

        script.consumeLine("non-matching prefix");
        assertNull(progress.getValue());

        script.consumeLine("");
        assertNull(progress.getValue());

        script.consumeLine(null);
        assertNull(progress.getValue());

        script.consumeLine(AntScript.MSG_PREFIX_ANT_PROGRESS);
        assertEquals("", progress.getValue());

        script.consumeLine(AntScript.MSG_PREFIX_ANT_PROGRESS + "valid progress msg");
        assertEquals("valid progress msg", progress.getValue());
    }

    private static String getLib() {
        File ccMain = UtilLocator.getClassSource(AntScript.class);
        final File progressLoggerJar = new File(ccMain, AntScript.LIBNAME_PROGRESS_LOGGER);
        return progressLoggerJar.getAbsolutePath();
    }

    private static File createFakeProgressLoggerLib() throws IOException {
        final File fakeJar = new File(getLib());
        fakeJar.createNewFile();
        fakeJar.deleteOnExit();
        return fakeJar;
    }
}
