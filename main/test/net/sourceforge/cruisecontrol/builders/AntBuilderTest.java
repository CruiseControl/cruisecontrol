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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.IO;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AntBuilderTest extends TestCase {
    private final List filesToClear = new ArrayList();
    private AntBuilder builder;
    private static final String UNIX_PATH = "/usr/java/jdk1.5.0/lib/tools.jar:"
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
    private static final String WINDOWS_PATH = "C:\\Progra~1\\IBM\\WSAD\\tools.jar;"
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

    protected void setUp() throws Exception {
        builder = new AntBuilder();
        builder.setTarget("target");
        builder.setBuildFile("buildfile");
        
        AntBuilder unixBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return UNIX_PATH;
            }
        };
        unixBuilder.setTarget("target");
        unixBuilder.setBuildFile("buildfile");

        AntBuilder windowsBuilder = new AntBuilder() {
            protected String getSystemClassPath() {
                return WINDOWS_PATH;
            }
        };
        windowsBuilder.setTarget("target");
        windowsBuilder.setBuildFile("buildfile");
    }

    public void tearDown() {
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            if (file.exists()) {
                file.delete();
            }
        }
        
        builder = null;
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
    }
     
    public void testValidateShouldThrowExceptionWhenSaveLogDirDoesntExist() {
        builder.setSaveLogDir("I/hope/this/dir/does/not/exist/");
        try {
            builder.validate();
            fail();
        } catch (CruiseControlException expected) {          
        }
    }
    
    public void testValidateShouldThrowExceptionWhenMultipleAndTimeAreBothSet() {        
        builder.setTime("0100");
        builder.setMultiple(2);

        try {
            builder.validate();
            fail();
        } catch (CruiseControlException expected) {
        }
    }

    public void testValidateAntHomeNotExist() {
        builder = new AntBuilder();
        builder.setAntHome("/this/directory/doesnt/exist");
        try {
            builder.validate();
            fail("validate should throw exceptions when the specified anthome doesn't exist");
        } catch (CruiseControlException e) {
            assertTrue("wrong exception caught [" + e.getMessage() + "]", e
                    .getMessage().indexOf(
                            "'antHome' must exist and be a directory") >= 0);
        }
    }

    public void testValidateAntHomeExistButNoAntScript() {
        builder = new AntBuilder();
        builder.setAntHome("/");
        try {
            builder.validate();
            fail("validate should throw exceptions when the specified anthome doesn't contain the antscript");
        } catch (CruiseControlException e) {
            assertTrue(
                    "wrong exception caught [" + e.getMessage() + "]",
                    e.getMessage().indexOf(
                            "'antHome' must contain an ant execution script") >= 0);
        }
    }

    public void testValidateAntHomeAndAntscriptSet() {
        builder = new AntBuilder();
        builder.setAntHome("/");
        builder.setAntScript("foo.bat");
        try {
            builder.validate();
            fail("validate should throw exceptions when anthome and antscript are both set");
        } catch (CruiseControlException e) {
            assertTrue(
                    "wrong exception caught [" + e.getMessage() + "]",
                    e.getMessage().indexOf(
                            "'antHome' and 'antscript' cannot both be set") >= 0);
        }
    }

    public void testGetCommandLineArgs_DebugAndQuiet() {
        builder.setUseDebug(true);
        builder.setUseQuiet(true);
        try {
            builder.validate();
            fail("validate() should throw CruiseControlException when both useDebug and useQuiet are true");
        } catch (CruiseControlException expected) {
        }
    }

    public void testGetAntLogAsElement() throws IOException, CruiseControlException {
        Element buildLogElement = new Element("build");
        File logFile = new File("_tempAntLog.xml");
        filesToClear.add(logFile);
        IO.write(logFile,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<?xml-stylesheet "
                + "type=\"text/xsl\" href=\"log.xsl\"?>\n<build></build>");

        assertEquals(
                buildLogElement.toString(),
                AntBuilder.getAntLogAsElement(logFile).toString());
    }

    public void testGetAntLogAsElement_NoLogFile() {
        File doesNotExist = new File("blah blah blah does not exist");
        try {
            AntBuilder.getAntLogAsElement(doesNotExist);
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("ant logfile " + doesNotExist.getAbsolutePath() + " does not exist.", expected.getMessage());
        }
    }

    public void testBuild() throws Exception {
        assertTrue("testbuild.xml expected in working dir", new File("testbuild.xml").exists());
        builder.setBuildFile("testbuild.xml");
        builder.setTempFile("notLog.xml");
        builder.setTarget("init");
        builder.validate();
        HashMap buildProperties = new HashMap();
        Element buildElement = builder.build(buildProperties);
        int initCount = getInitCount(buildElement);
        assertEquals(1, initCount);

        builder.setTarget("init init");
        buildElement = builder.build(buildProperties);
        initCount = getInitCount(buildElement);
        assertEquals(2, initCount);
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
        assertTrue("testbuild.xml expected in working dir", new File("testbuild.xml").exists());
        builder.setBuildFile("testbuild.xml");
        builder.setTarget("timeout-test-target");
        builder.setTimeout(5);
        builder.setUseDebug(true);
        builder.setUseLogger(true);
        builder.validate();

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

    public void testSaveAntLog() throws IOException {
        String originalDirName = "target";
        String logName = "log.xml";
        String saveDirName = "target/reports/ant";

        builder.setSaveLogDir(saveDirName);
        builder.setTempFile(logName);

        File originalDir = new File(originalDirName);
        File originalLog = new File(originalDir, logName);
        originalDir.mkdirs();
        originalLog.createNewFile();
        
        File saveDir = new File(saveDirName);
        File savedLog = new File(saveDir, logName);
        saveDir.mkdirs();
        savedLog.delete();

        builder.saveAntLog(originalLog);
        assertTrue(savedLog.exists());

        savedLog.delete();

        builder.setSaveLogDir("");
        builder.saveAntLog(originalLog);
        assertFalse(savedLog.exists());

        builder.setSaveLogDir(null);
        builder.saveAntLog(originalLog);
        assertFalse(savedLog.exists());
    }    

    public void testFindAntScriptNonWindows() throws CruiseControlException {
        builder.setAntHome("/foo/bar");
        assertEquals("/foo/bar/bin/ant", builder.findAntScript(false));
    }

    public void testFindAntScriptWindows() throws CruiseControlException {
        builder.setAntHome("c:\\foo\\bar");
        assertEquals("c:\\foo\\bar\\bin\\ant.bat", builder.findAntScript(true));
    }

    public void testFindAntScriptWindowsNoAntHome() {
        try {
            builder.findAntScript(false);
            fail("expected exception");
        } catch (CruiseControlException expected) {
            //expected...
        }

        try {
            builder.findAntScript(true);
            fail("expected exception");
        } catch (CruiseControlException expected) {
            //expected...
        }
    }
    
    public void testValidateBuildFileWorksForNonDefaultDirectory() throws IOException, CruiseControlException {
        File antworkdir = new File("antworkdir");
        antworkdir.mkdir();
        File file = File.createTempFile("build", ".xml", antworkdir);
        builder.setAntWorkingDir(antworkdir.getAbsolutePath());
        builder.setBuildFile(file.getName());
        
        builder.validateBuildFileExists();
        
        builder.setBuildFile(file.getAbsolutePath());
        builder.validateBuildFileExists();

        file.delete();
        try {
            builder.validateBuildFileExists();
            fail();
        } catch (CruiseControlException expected) {
        }

        builder.setBuildFile(file.getName());
        try {
            builder.validateBuildFileExists();
            fail();
        } catch (CruiseControlException expected) {
        }
    }
}
