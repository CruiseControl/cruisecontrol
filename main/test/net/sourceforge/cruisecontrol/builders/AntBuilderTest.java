/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jdom.Element;

public class AntBuilderTest extends TestCase {
    private final List filesToClear = new ArrayList();
    private AntBuilder builder;
    private String classpath;
    private String antLauncherPath;
    private Hashtable properties;
    private String javaCmd = "java";

    protected void setUp() throws Exception {
        builder = new AntBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        classpath = System.getProperty("java.class.path");
        antLauncherPath = builder.getAntLauncherJarLocation(classpath);
        if (builder.isWindows()) {
            javaCmd = "java.exe";
        }
        
        properties = new Hashtable();
        properties.put("label", "200.1.23");

        
        BasicConfigurator.configure(
            new ConsoleAppender(new PatternLayout("%m%n")));
    }

    public void tearDown() {
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
        
        builder = null;
        classpath = null;
        properties = null;
    }

    public void testValidate() {
        builder = new AntBuilder();

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("antbuilder has no required attributes");
        }

        builder.setTime("0100");
        builder.setBuildFile("buildfile");
        builder.setTarget("target");

        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("validate should not throw exceptions when options are set.");
        }
        
        builder.setMultiple(2);

        try {
            builder.validate();
            fail("validate should throw exceptions when multiple and time are both set.");
        } catch (CruiseControlException e) {
        }
    }

    public void testGetCommandLineArgs() throws CruiseControlException {
        String[] resultInfo =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        assertTrue(
            Arrays.equals(
                resultInfo,
                builder.getCommandLineArgs(properties, false, false, false)));

        String[] resultLogger =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        assertTrue(
            Arrays.equals(
                resultLogger,
                builder.getCommandLineArgs(properties, true, false, false)));
    }

    public void testGetCommandLineArgs_EmptyLogger() throws CruiseControlException {
        String[] resultInfo =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        properties.put("label", "");
        assertTrue(
            Arrays.equals(
                resultInfo,
                builder.getCommandLineArgs(properties, false, false, false)));

        String[] resultLogger =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-buildfile",
                "buildfile",
                "target" };
        assertTrue(
            Arrays.equals(
                resultLogger,
                builder.getCommandLineArgs(properties, true, false, false)));
    }

    public void testGetCommandLineArgs_Debug() throws CruiseControlException {
        String[] resultDebug =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-debug",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        builder.setUseDebug(true);
        assertTrue(
            Arrays.equals(
                resultDebug,
                builder.getCommandLineArgs(properties, true, false, false)));
    }

    public void testGetCommandLineArgs_Quiet() throws CruiseControlException {
        String[] resultQuiet =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-logger",
                "org.apache.tools.ant.XmlLogger",
                "-logfile",
                "log.xml",
                "-quiet",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        builder.setUseQuiet(true);
        assertTrue(
            Arrays.equals(
                resultQuiet,
                builder.getCommandLineArgs(properties, true, false, false)));
    }

    public void testGetCommandLineArgs_DebugAndQuiet() throws CruiseControlException {
        builder.setUseDebug(true);
        builder.setUseQuiet(true);
        try {
            builder.validate();
            fail("validate() should throw CruiseControlException when both useDebug and useQuiet are true");
        } catch (CruiseControlException expected) {
        }
    }

    public void testGetCommandLineArgs_MaxMemory() throws CruiseControlException {
        String[] resultWithMaxMemory =
            {
                javaCmd,
                "-Xmx256m",
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) builder.createJVMArg();
        arg.setArg("-Xmx256m");
        assertTrue(
            Arrays.equals(
                    resultWithMaxMemory,
                builder.getCommandLineArgs(properties, false, false, false)));
    }
    
    public void testGetCommandLineArgs_MaxMemoryAndProperty() throws CruiseControlException {
        String[] resultWithMaxMemoryAndProperty =
            {
                javaCmd,
                "-Xmx256m",
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-listener",
                "org.apache.tools.ant.XmlLogger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-Dfoo=bar",
                "-buildfile",
                "buildfile",
                "target" };
        AntBuilder.JVMArg arg = (AntBuilder.JVMArg) builder.createJVMArg();
        arg.setArg("-Xmx256m");
        AntBuilder.Property prop = builder.createProperty();
        prop.setName("foo");
        prop.setValue("bar");
        assertTrue(
            Arrays.equals(
                resultWithMaxMemoryAndProperty,
                builder.getCommandLineArgs(properties, false, false, false)));
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
        builder.setAntScript("ant.bat");
        assertTrue(
            Arrays.equals(
                resultBatchFile,
                builder.getCommandLineArgs(properties, false, true, true)));
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
        builder.setAntScript("ant.sh");
        assertTrue(
            Arrays.equals(
                resultShellScript,
                builder.getCommandLineArgs(properties, false, true, false)));
    }
    
    public void testGetCommandLineArgs_AlternateLogger() throws CruiseControlException {
        String[] args =
            {
                javaCmd,
                "-classpath",
                antLauncherPath,
                "org.apache.tools.ant.launch.Launcher",
                "-lib",
                classpath,
                "-listener",
                "com.canoo.Logger",
                "-DXmlLogger.file=log.xml",
                "-Dlabel=200.1.23",
                "-buildfile",
                "buildfile",
                "target" };
        builder.setLoggerClassName("com.canoo.Logger");
        assertTrue(
            Arrays.equals(
                args,
                builder.getCommandLineArgs(properties, false, false, false)));
    }

    public void testGetAntLogAsElement() throws IOException, CruiseControlException {
        Element buildLogElement = new Element("build");
        File logFile = new File("_tempAntLog.xml");
        filesToClear.add(logFile);
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(logFile));
        bw2.write(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<?xml-stylesheet "
                + "type=\"text/xsl\" href=\"log.xsl\"?>\n<build></build>");
        bw2.flush();
        bw2.close();

        assertEquals(
                buildLogElement.toString(),
                AntBuilder.getAntLogAsElement(logFile).toString());
    }

    public void testGetAntLogAsElement_NoLogFile() throws IOException {
        File doesNotExist = new File("blah blah blah does not exist");
        try {
            AntBuilder.getAntLogAsElement(doesNotExist);
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("ant logfile " + doesNotExist.getAbsolutePath() + " does not exist.", expected.getMessage());
        }
    }

    public void testBuild() throws Exception {
        builder.setBuildFile("testbuild.xml");
        builder.setTempFile("notLog.xml");
        builder.setTarget("init");
        HashMap buildProperties = new HashMap();
        Element buildElement = builder.build(buildProperties);
        int initCount = getInitCount(buildElement);
        assertEquals(1, initCount);

        builder.setTarget("init init");
        buildElement = builder.build(buildProperties);
        initCount = getInitCount(buildElement);
        assertEquals(2, initCount);
    }
    
    public void testIsWindows() {
        builder = new AntBuilder() {
            protected String getOsName() {
                return "Windows 2000";
            }
        };
        
        assertTrue(builder.isWindows());
    }

    public int getInitCount(Element buildElement) {
        int initFoundCount = 0;
        Iterator targetIterator = buildElement.getChildren("target").iterator();
        String name;
        while (targetIterator.hasNext()) {
            name = ((Element) targetIterator.next()).getAttributeValue("name");
            if (name.equals("init")) {
                initFoundCount++;
            }
        }
        return initFoundCount;
    }

    public void testBuildTimeout() throws Exception {
        builder.setBuildFile("testbuild.xml");
        builder.setTarget("timeout-test-target");
        builder.setTimeout(5);
        builder.setUseDebug(true);
        builder.setUseLogger(true);

        HashMap buildProperties = new HashMap();
        long startTime = System.currentTimeMillis();
        Element buildElement = builder.build(buildProperties);
        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);

        // test we don't fail when there is no ant log file
        builder.setTimeout(1);
        builder.setUseDebug(false);
        builder.setUseLogger(false);
        builder.setTempFile("shouldNot.xml");
        buildElement = builder.build(buildProperties);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
    }
    
    public void testGetAntLauncherJarLocationForWindows() throws Exception {
        AntBuilder windowsBuilder = new AntBuilder() {
            protected boolean isWindows() {
                return true;
            }
        };
        String windowsPath = "C:\\Progra~1\\IBM\\WSAD\\v5.1.2\\runtimes\\base_v51\\Java\\lib\\tools.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\dist\\cruisecontrol.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\log4j.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jdom.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xerces.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\xalan.jar;"
            + "C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\jakarta-oro-2.0.3.jar;"
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
        assertEquals("C:\\Java\\cruisecontrol-2.2\\main\\bin\\\\..\\lib\\ant\\ant-launcher.jar",
                     windowsBuilder.getAntLauncherJarLocation(windowsPath));
    }

    public void testGetAntLauncherJarLocationForUnix() throws Exception {
        AntBuilder unixBuilder = new AntBuilder() {
            protected boolean isWindows() {
                return false;
            }
        };
        String unixPath = "/usr/java/jdk1.5.0/lib/tools.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/dist/cruisecontrol.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/log4j.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/jdom.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/ant:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/xerces.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/xalan.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/jakarta-oro-2.0.3.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/mail.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/junit.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/activation.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/commons-net-1.1.0.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/starteam-sdk.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-tools.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/mx4j-remote.jar:"
            + "/home/joris/java/cruisecontrol-2.2/main/lib/smack.jar:.";
        assertEquals("/home/joris/java/cruisecontrol-2.2/main/lib/ant/ant-launcher.jar",
                     unixBuilder.getAntLauncherJarLocation(unixPath));
    }
}
