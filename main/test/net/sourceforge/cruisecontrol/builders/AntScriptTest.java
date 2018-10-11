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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProgressImplTest;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.UtilLocator;

public class AntScriptTest extends TestCase {
    private static final FilesToDelete files2del = new FilesToDelete();
    private static File fakeJar = null;
    private AntScript script;
    private AntBuilder unixBuilder;
    private AntBuilder windowsBuilder;
    private Map<String, String> properties;
    private static final boolean USE_LOGGER = true;
    private static final boolean USE_SCRIPT = true;
    private static final boolean IS_WINDOWS = true;
    private static final int NUMBER_OF_SAXON_JARS = 2;
    private static final int UNIX_PATH_LENGTH = 22;
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
      + "/home/joris/java/cruisecontrol-2.2/main/lib/saxon8.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/saxon8-dom.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-tools.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-remote.jar:"
      + "/home/joris/java/cruisecontrol-2.2/main/lib/smack.jar:.";
    private static String unixPathWithoutSaxonJars;
    private static final int WINDOWS_PATH_LENGTH = 24;
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
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\saxon8.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\saxon8-dom.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-tools.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\mx4j-remote.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\smack.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\comm.jar;"
      + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\x10.jar;.";
    private static String windowsPathWithoutSaxonJars;

    /** Expose package level constant for unit testing only. */
    public static final String MSG_PREFIX_ANT_PROGRESS = AntScript.MSG_PREFIX_ANT_PROGRESS;

    @Override
    protected void setUp() throws Exception {
        createFakeProgressLoggerLib();

        script = new AntScript();

        properties = new HashMap<String, String>();
        properties.put("label", "200.1.23");

        // default setup of script
        script.setBuildProperties(properties);
        script.setArgs(new ArrayList<AntBuilder.JVMArg>());
        script.setProperties(new ArrayList<Property>());
        script.setLibs(new ArrayList<AntBuilder.Lib>());
        script.setListeners(new ArrayList<AntBuilder.Listener>());
        script.setBuildFile("buildfile");
        script.setTarget("target");

        unixBuilder = new AntBuilder() {
            @Override
            protected String getSystemClassPath() {
                return UNIX_PATH;
            }
        };
        unixBuilder.setTarget("target");
        unixBuilder.setBuildFile("buildfile");

        windowsBuilder = new AntBuilder() {
            @Override
            protected String getSystemClassPath() {
                return WINDOWS_PATH;
            }
        };
        windowsBuilder.setTarget("target");
        windowsBuilder.setBuildFile("buildfile");

        unixPathWithoutSaxonJars = script.removeSaxonJars(UNIX_PATH, !IS_WINDOWS);
        windowsPathWithoutSaxonJars = script.removeSaxonJars(WINDOWS_PATH, IS_WINDOWS);
    }

    public void testGetClasspathItemsForWindows() throws Exception {
        final List<String> list = script.getClasspathItems(WINDOWS_PATH, IS_WINDOWS);
        assertEquals(WINDOWS_PATH_LENGTH, list.size());
        assertEquals("C:\\Progra~1\\IBM\\WSAD\\tools.jar", list.get(0));
    }

    public void testGetClasspathItemsForUnix() throws Exception {
        final List<String> list = script.getClasspathItems(UNIX_PATH, !IS_WINDOWS);
        assertEquals(UNIX_PATH_LENGTH, list.size());
        assertEquals("/usr/java/jdk1.5.0/lib/tools.jar", list.get(0));
    }

    public void testGetAntLauncherJarLocationForWindows() throws Exception {
        assertEquals("C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar",
                     script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS));
    }

    public void testGetAntLauncherJarLocationForUnix() throws Exception {
        assertEquals("/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar",
                script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS));
    }

    public void testRemoveSaxonJarsForWindows() throws Exception {
        // skip this test if not running under windows,
        // as File objects with "c:..." paths return full path for file.getName() used in script.removeSaxonJars()
        if (!Util.isWindows()) {
            System.out.println("skipping test: " + getName());
            return;
        }

        List<String> list = script.getClasspathItems(WINDOWS_PATH, IS_WINDOWS);
        final String path = script.removeSaxonJars(list, IS_WINDOWS);
        assertFalse(path.indexOf("saxon") >= 0);
        list = script.getClasspathItems(path, IS_WINDOWS);
        assertEquals(WINDOWS_PATH_LENGTH - NUMBER_OF_SAXON_JARS, list.size());
    }

    public void testRemoveSaxonJarsForUnix() throws Exception {
        List<String> list = script.getClasspathItems(UNIX_PATH, !IS_WINDOWS);
        final String path = script.removeSaxonJars(list, IS_WINDOWS);
        assertFalse(path.indexOf("saxon") >= 0);
        list = script.getClasspathItems(path, IS_WINDOWS);
        assertEquals(UNIX_PATH_LENGTH - NUMBER_OF_SAXON_JARS, list.size());
    }

    public void testGetCommandLineArgs() throws CruiseControlException {
        String[] resultInfo =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultInfo, script.buildCommandline().getCommandline());


        String[] resultLogger =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-logger org.apache.tools.ant.XmlLogger",
                "-logfile log.xml",
                "-Dcustom=label.200.1.23",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };

        properties.put("custom", "label.${label}"); // Add custom property referring another property

        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);

        TestUtil.assertCommandLine(resultLogger, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_EmptyLogger() throws CruiseControlException {
        String[] resultInfo =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-buildfile buildfile",
                "target" };
        properties.put("label", "");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);

        TestUtil.assertCommandLine(resultInfo, script.buildCommandline().getCommandline());


        String[] resultLogger =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-logger org.apache.tools.ant.XmlLogger",
                "-logfile log.xml",
                "-buildfile buildfile",
                "target" };
        script.setUseLogger(USE_LOGGER);
        script.setUseScript(!USE_SCRIPT);
        script.setWindows(!IS_WINDOWS);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultLogger, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_Debug() throws CruiseControlException {
        String[] resultDebug =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-logger org.apache.tools.ant.XmlLogger",
                "-logfile log.xml",
                "-debug",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };

        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setUseDebug(true);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultDebug, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_DebugWithListener() throws CruiseControlException {
             String[] resultDebug =
             {
                 "java",
                 "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                 "-lib " + unixPathWithoutSaxonJars,
                 "-listener org.apache.tools.ant.XmlLogger",
                 "-DXmlLogger.file=log.xml",
                 "-debug",
                 "-Dlabel=200.1.23",
                 "-buildfile buildfile",
                 "target" };
             script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
             script.setBuildProperties(properties);
             script.setUseLogger(!USE_LOGGER);
             script.setWindows(!IS_WINDOWS);
             script.setUseScript(!USE_SCRIPT);
             script.setSystemClassPath(UNIX_PATH);
             script.setUseDebug(true);

             TestUtil.assertCommandLine(resultDebug, script.buildCommandline().getCommandline());
         }

    public void testGetCommandLineArgs_Quiet() throws CruiseControlException {
        String[] resultQuiet =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-logger org.apache.tools.ant.XmlLogger",
                "-logfile log.xml",
                "-quiet",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(UNIX_PATH);
        script.setUseQuiet(true);

        TestUtil.assertCommandLine(resultQuiet, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_KeepGoingDebug() throws CruiseControlException {
        String[] resultDebug =
            {
                "java",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-logger org.apache.tools.ant.XmlLogger",
                "-logfile log.xml",
                "-keep-going",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };

        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setKeepGoing(true);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultDebug, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_MaxMemory() throws CruiseControlException {
        final String[] resultWithMaxMemory =
            {
                "java",
                "-Xmx256m",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        final AntBuilder.JVMArg arg = unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        final List<AntBuilder.JVMArg> args = new ArrayList<AntBuilder.JVMArg>();
        args.add(arg);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultWithMaxMemory, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_MaxMemoryAndProperty() throws CruiseControlException {
        final String[] resultWithMaxMemoryAndProperty =
            {
                "java",
                "-Xmx256m",
                "-classpath " + script.getAntLauncherJarLocation(UNIX_PATH, !IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + unixPathWithoutSaxonJars,
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-Dfoo=bar",
                "-buildfile buildfile",
                "target" };
        final AntBuilder.JVMArg arg = unixBuilder.createJVMArg();
        arg.setArg("-Xmx256m");
        final Property prop = unixBuilder.createProperty();
        prop.setName("foo");
        prop.setValue("bar");

        final List<AntBuilder.JVMArg> args = new ArrayList<AntBuilder.JVMArg>();
        args.add(arg);
        final List<Property> props = new ArrayList<Property>();
        props.add(prop);
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setArgs(args);
        script.setProperties(props);
        script.setSystemClassPath(UNIX_PATH);

        TestUtil.assertCommandLine(resultWithMaxMemoryAndProperty, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_BatchFile() throws CruiseControlException {
        String[] resultBatchFile =
            {
                "ant.bat",
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setAntScript("ant.bat");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);

        TestUtil.assertCommandLine(resultBatchFile, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShellScript() throws CruiseControlException {
        String[] resultShellScript =
            {
                "ant.sh",
                "-listener org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setAntScript("ant.sh");
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(!IS_WINDOWS);
        script.setUseScript(USE_SCRIPT);

        TestUtil.assertCommandLine(resultShellScript, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_AlternateLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener com.canoo.Logger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        script.setUseLogger(!USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setUseScript(!USE_SCRIPT);
        script.setSystemClassPath(WINDOWS_PATH);


        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }


    public void testSetupResolvedLoggerClassname() throws Exception {
        // set to same default used by AntBuilder
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setProgress(null);
        script.setupResolvedLoggerClassname();
        assertEquals(AntBuilder.DEFAULT_LOGGER, script.getLoggerClassName());

        final Progress progress = new ProgressImplTest.MockProgress();
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
            fakeJar.delete();

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
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntProgressLogger.class.getName(),
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-lib " + getLib(),
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntProgressLogger.class.getName());
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new ProgressImplTest.MockProgress());

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ProgressLoggerUseLogger() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntProgressXmlLogger.class.getName(),
                "-listener " + AntProgressXmlListener.class.getName(),
                "-DXmlLogger.file=log.xml",
                "-lib " + getLib(),
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntProgressXmlLogger.class.getName());
        script.setUseLogger(USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new ProgressImplTest.MockProgress());

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ProgressLoggerLibNotUseLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntProgressLogger.class.getName(),
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-lib c:\\PathToAntProgressLogger.jar",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntProgressLogger.class.getName());
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new ProgressImplTest.MockProgress());
        script.setProgressLoggerLib("c:\\PathToAntProgressLogger.jar");

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ProgressLoggerLibUseLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntProgressXmlLogger.class.getName(),
                "-listener " + AntProgressXmlListener.class.getName(),
                "-DXmlLogger.file=log.xml",
                "-lib c:\\PathToAntProgressLogger.jar",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntProgressXmlLogger.class.getName());
        script.setUseLogger(USE_LOGGER);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setProgress(new ProgressImplTest.MockProgress());
        script.setProgressLoggerLib("c:\\PathToAntProgressLogger.jar");

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShowAntOutputFalse() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setShowAntOutput(false);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShowAntOutputTrue() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setShowAntOutput(true);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShowAntOutputUseLogger() throws Exception {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntBuilder.DEFAULT_LOGGER,
                "-logfile log.xml",
                "-listener " + AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-lib " + getLib(),
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setShowAntOutput(true);
        script.setUseLogger(true);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShowAntOutputOverrideLib() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setShowAntOutput(true);
        script.setProgressLoggerLib("c:\\PathToAntProgressLogger.jar");

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_ShowAntOutputOverrideLibUseLogger() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-logger " + AntBuilder.DEFAULT_LOGGER,
                "-logfile log.xml",
                "-listener " + AntScript.CLASSNAME_DASHBOARD_LISTENER,
                "-lib c:\\PathToAntProgressLogger.jar",
                "-Dlabel=200.1.23",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setShowAntOutput(true);
        script.setProgressLoggerLib("c:\\PathToAntProgressLogger.jar");
        script.setUseLogger(true);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_PropertyFile() throws CruiseControlException {
        String[] args =
            {
                "java.exe",
                "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
                "-lib " + windowsPathWithoutSaxonJars,
                "-listener " + AntBuilder.DEFAULT_LOGGER,
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-propertyfile testPropertyFile.properties",
                "-buildfile buildfile",
                "target" };
        script.setLoggerClassName(AntBuilder.DEFAULT_LOGGER);
        script.setBuildProperties(properties);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);
        script.setPropertyFile("testPropertyFile.properties");

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
    }

    public void testGetCommandLineArgs_MultipleLibs() throws CruiseControlException {
        final String[] args =
         {
             "java.exe",
             "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
             "-lib " + windowsPathWithoutSaxonJars,
             "-listener com.canoo.Logger",
             "-DXmlLogger.file=log.xml",
             "-lib c:\\somedir",
             "-Dlabel=200.1.23",
             "-buildfile buildfile",
             "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        final AntBuilder.Lib lib = windowsBuilder.new Lib();
        lib.setSearchPath("c:\\somedir");
        final List<AntBuilder.Lib> libs = new ArrayList<AntBuilder.Lib>();
        libs.add(lib);
        script.setLibs(libs);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
     }

    public void testGetCommandLineArgs_MultipleListeners() throws CruiseControlException {
        final String[] args =
         {
             "java.exe",
             "-classpath " + script.getAntLauncherJarLocation(WINDOWS_PATH, IS_WINDOWS) + " org.apache.tools.ant.launch.Launcher",
             "-lib " + windowsPathWithoutSaxonJars,
             "-listener com.canoo.Logger",
             "-DXmlLogger.file=log.xml",
             "-listener org.apache.tools.ant.listener.Log4jListener",
             "-Dlabel=200.1.23",
             "-buildfile buildfile",
             "target" };
        script.setLoggerClassName("com.canoo.Logger");
        script.setBuildProperties(properties);
        final AntBuilder.Listener listener = windowsBuilder.new Listener();
        listener.setClassName("org.apache.tools.ant.listener.Log4jListener");
        final List<AntBuilder.Listener> listeners = new ArrayList<AntBuilder.Listener>();
        listeners.add(listener);
        script.setListeners(listeners);
        script.setWindows(IS_WINDOWS);
        script.setSystemClassPath(WINDOWS_PATH);

        TestUtil.assertCommandLine(args, script.buildCommandline().getCommandline());
     }

    public void testConsumeLine() throws Exception {
        final Progress progress = new ProgressImplTest.MockProgress();
        script.setProgress(progress);

        assertNull(progress.getText());

        script.consumeLine("non-matching prefix");
        assertNull(progress.getText());

        script.consumeLine("");
        assertNull(progress.getText());

        script.consumeLine(null);
        assertNull(progress.getText());

        script.consumeLine(AntScript.MSG_PREFIX_ANT_PROGRESS);
        assertEquals("", progress.getText());

        script.consumeLine(AntScript.MSG_PREFIX_ANT_PROGRESS + "valid progress msg");
        assertEquals("valid progress msg", progress.getText());
    }


    private static String getLib() {
        File ccMain = UtilLocator.getClassSource(AntScript.class);
        final File progressLoggerJar = new File(ccMain, AntScript.LIBNAME_PROGRESS_LOGGER);
        return progressLoggerJar.getAbsolutePath();
    }

    static File createFakeProgressLoggerLib() throws IOException {
        if (fakeJar != null && fakeJar.exists()) {
            return fakeJar;
        }

        fakeJar = files2del.add(new File(getLib()));
        fakeJar.createNewFile();
        return fakeJar;
    }
}
