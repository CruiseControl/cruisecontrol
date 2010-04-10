/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuilderTest;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class AntBuilderTest extends TestCase {
    private final FilesToDelete filesToDelete = new FilesToDelete();
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

        // builder.build() will create an antBuilderOutput.log file when showAntOutput=true, so clean it up.
        filesToDelete.add(new File(AntOutputLogger.DEFAULT_OUTFILE_NAME));
    }

    public void tearDown() {
        filesToDelete.delete();
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

    public void testShouldAddDashboardLoggerJarToCommandLine() throws Exception {
        assertFalse(AntBuilder.shouldAddDashboardLoggerJarToCommandLine(false, false));
        assertFalse(AntBuilder.shouldAddDashboardLoggerJarToCommandLine(false, true));
        assertFalse(AntBuilder.shouldAddDashboardLoggerJarToCommandLine(true, false));

        assertTrue(AntBuilder.shouldAddDashboardLoggerJarToCommandLine(true, true));
    }

    public void testValidateShowAntOutput() throws Exception {
        builder = new AntBuilder();

        assertTrue("Wrong default value for showAntOutput", builder.getShowAntOutput());
        assertNull("Wrong default value for progressLoggerLib", builder.getProgressLoggerLib());

        final File fakeProgressLoggerLibJar = AntScriptTest.createFakeProgressLoggerLib();
        filesToDelete.add(fakeProgressLoggerLibJar);

        builder.setShowAntOutput(false);
        builder.validate();

        builder.setShowAntOutput(true);
        builder.validate(); // should pass since fakeProgressLoggerLibJar exists

        final String dummyLoggerLib = "dummyLoggerLib";
        builder.setProgressLoggerLib(dummyLoggerLib);
        // should pass since default useLogger is false (see AntBuilder.isDashboardLoggerRequired())
        builder.validate();

        builder.setUseLogger(true);
        try {
            builder.validate();
            fail("Non-existant overriden progressLoggerLib should have failed.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith("File specified ["));
        }

        // Now run tests without fakeProgressLoggerLibJar
        builder.setUseLogger(false);
        assertTrue("failed to delete fakeProgressLoggerLibJar: " + fakeProgressLoggerLibJar.getAbsolutePath(),
                fakeProgressLoggerLibJar.delete());
        // reset to default
        builder.setProgressLoggerLib(null);

        builder.setShowAntOutput(false);
        builder.validate();

        builder.setShowAntOutput(true);
        // should pass since default useLogger is false (see AntBuilder.isDashboardLoggerRequired())
        builder.validate();

        builder.setUseLogger(true);
        try {
            builder.validate();
            fail("Missing default progressLoggerLib should have failed.");
        } catch (AntScript.ProgressLibLocatorException e) {
            assertTrue(e.getMessage().startsWith("The progressLoggerLib jar file does not exist where expected: "));
        }

        builder.setProgressLoggerLib(dummyLoggerLib);
        try {
            builder.validate();
            fail("Non-existant overriden progressLoggerLib should have failed.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith("File specified ["));
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

    public void testGetAntLogAsElement() throws CruiseControlException {
        Element buildLogElement = new Element("build");
        File logFile = new File(TestUtil.getTargetDir(), "_tempAntLog.xml");
        filesToDelete.add(logFile);
        IO.write(logFile,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<?xml-stylesheet "
                + "type=\"text/xsl\" href=\"log.xsl\"?>\n<build></build>");

        assertEquals(
                buildLogElement.toString(),
                builder.getAntLogAsElement(logFile).toString());
    }

    public void testGetAntLogAsElement_NoLogFile() {
        File doesNotExist = new File("blah blah blah does not exist");
        try {
            builder.getAntLogAsElement(doesNotExist);
            fail();
        } catch (CruiseControlException expected) {
            assertEquals("ant logfile " + doesNotExist.getAbsolutePath() + " does not exist.", expected.getMessage());
        }
    }

    public void testBuild() throws Exception {
        final File buildFile = File.createTempFile("testbuild", ".xml", TestUtil.getTargetDir());
        writeBuildFile(buildFile, 0);
        filesToDelete.add(buildFile);

        builder.setBuildFile(buildFile.getAbsolutePath());
        builder.setTempFile("notLog.xml");
        builder.setTarget("init");
        builder.validate();
        final Map<String, String> buildProperties = BuilderTest.createPropsWithProjectName("testproject");
        Element buildElement = builder.build(buildProperties, null);
        int initCount = getInitCount(buildElement);
        assertEquals(1, initCount);

        builder.setTarget("init init");
        buildElement = builder.build(buildProperties, null);
        initCount = getInitCount(buildElement);
        assertEquals(2, initCount);
    }

    private static void writeBuildFile(final File buildFile, final int sleepSeconds) throws CruiseControlException {
        final StringBuilder contents = new StringBuilder();
        contents.append("<project name='testbuild' default='init'>");
        contents.append("<target name='init'><echo message='called testbulid.xml init target'/></target>");
        contents.append("<target name='time.out'><sleep seconds='").append(sleepSeconds).append("'/></target>");
        contents.append("</project>");
        IO.write(buildFile, contents.toString());
    }

    private int getInitCount(final Element buildElement) {
        int initFoundCount = 0;
        final Iterator targetIterator = buildElement.getChildren("target").iterator();
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
        final File buildFile = File.createTempFile("testbuild", ".xml", TestUtil.getTargetDir());
        final int buildSleepSecs = 2;
        writeBuildFile(buildFile, buildSleepSecs);
        filesToDelete.add(buildFile);

        builder.setBuildFile(buildFile.getAbsolutePath());
        builder.setTarget("time.out");
        final int testTimeoutSecs = buildSleepSecs / 2;
        builder.setTimeout(testTimeoutSecs);
        builder.setUseDebug(true);
        builder.setUseLogger(true);
        builder.setShowAntOutput(false); // required to bypass Dashboard logger
        builder.validate();

        final Map<String, String> buildProperties = new HashMap<String, String>();
        final long startTime = System.currentTimeMillis();
        filesToDelete.add(new File(TestUtil.getTargetDir(), "log.xml"));
        Element buildElement = builder.build(buildProperties, null);
        final long elapsedMillis = System.currentTimeMillis() - startTime;
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
        assertTrue("Too much time has elapsed (" + elapsedMillis + " millis) for AntBuilder timeout of "
                + testTimeoutSecs + " secs.",
                elapsedMillis < ((testTimeoutSecs + 3) * 1000L));

        // test we don't fail when there is no ant log file
        builder.setTimeout(1);
        builder.setUseDebug(false);
        builder.setUseLogger(false);
        builder.setTempFile("shouldNot.xml");
        buildElement = builder.build(buildProperties, null);
        assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);
    }

    public void testSaveAntLog() throws IOException {
        final String originalDirName = "target";
        filesToDelete.add(new File(TestUtil.getTargetDir(), originalDirName));
        final String logName = "log.xml";
        final String saveDirName = "target/reports/ant";

        builder.setSaveLogDir(saveDirName);
        builder.setTempFile(logName);

        final File originalDir = new File(originalDirName);
        final File originalLog = new File(originalDir, logName);
        Util.doMkDirs(originalDir);
        originalLog.createNewFile();

        final File saveDir = new File(saveDirName);
        final File savedLog = new File(saveDir, logName);
        Util.doMkDirs(saveDir);
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
        final File antworkdir = new File("antworkdir");
        antworkdir.mkdir();
        antworkdir.deleteOnExit();

        final File file = File.createTempFile("build", ".xml", antworkdir);
        file.deleteOnExit();

        filesToDelete.add(file);
        filesToDelete.add(antworkdir);

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

    public void testValidateBuildFileNonAbsFileWithDifferentAntWorkDir()
            throws IOException, CruiseControlException {

        // this use case occurs for paths like "/tools/build.xml" on winz, where "/" will be on the current drive.
        // in such cases where config must build on multiple OS's, and we don't want to configure paths twice for each.
        // for example: antworkdir = /project
        //              buildfile = /tools/build.xml - not absolute on Winz, but will be found by Ant
        // assuming the following file exists: c:\tools\build.xml

        final File antworkdir = new File("antworkdir");
        antworkdir.mkdir();
        antworkdir.deleteOnExit();

        final File buildfileDir = new File("tools");
        buildfileDir.mkdir();
        buildfileDir.deleteOnExit();

        final File file = File.createTempFile("build", ".xml", buildfileDir);
        file.deleteOnExit();

        filesToDelete.add(file);
        filesToDelete.add(buildfileDir);
        filesToDelete.add(antworkdir);

        builder.setAntWorkingDir(antworkdir.getAbsolutePath());
        builder.setBuildFile(buildfileDir.getName() + "/" + file.getName());

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

    public void testRealBuild() throws IOException, CruiseControlException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempSubdir = new File(tempDir, "cruisecontroltest" + System.currentTimeMillis());
        tempSubdir.mkdir();
        filesToDelete.add(tempSubdir);

        File buildFile = new File(tempSubdir, "build.xml");

        createFakeProjectInTempDir(tempSubdir, buildFile);

        builder.setBuildFile(buildFile.getAbsolutePath());
        builder.setTarget("all");
        builder.setTempFile("notLog.xml");
        builder.validate();
        final Map<String, String> buildProperties = BuilderTest.createPropsWithProjectName("test1");
        builder.build(buildProperties, null);

        //Now verify a JUnit report was created
        File htmlJUnitReportFile = new File(tempSubdir, "reports/html/index.html");
        assertTrue("JUnit HTML Report was not created", htmlJUnitReportFile.exists());
    }

    private void createFakeProjectInTempDir(File dir, File buildFile) throws CruiseControlException {
        File srcDir = new File(dir, "src");
        File srcPackageDir = new File(srcDir, "apackage");
        writeSimpleClassFile(srcPackageDir);

        File testDir = new File(dir, "test");
        File testPackageDir = new File(testDir, "apackage");
        writeSimpleTestFile(testPackageDir);

        writeSimpleBuildFile(buildFile);
    }

    private void writeSimpleClassFile(final File srcPackageDir) throws CruiseControlException {
        srcPackageDir.mkdirs();
        final File simpleClassFile = new File(srcPackageDir, "Simple.java");

        final StringBuilder contents = new StringBuilder();
        contents.append("package apackage;");
        contents.append("public class Simple {");
        contents.append("    public void call() {}");
        contents.append("}");
        IO.write(simpleClassFile, contents.toString());
    }

    private void writeSimpleTestFile(final File testPackageDir) throws CruiseControlException {
        testPackageDir.mkdirs();
        final File simpleTestFile = new File(testPackageDir, "SimpleTest.java");

        final StringBuilder contents = new StringBuilder();
        contents.append("package apackage;");
        contents.append("import junit.framework.TestCase;");
        contents.append("public class SimpleTest extends TestCase {");
        contents.append("    public void testSimple() {");
        contents.append("        Simple simple = new Simple();");
        contents.append("        simple.call();");
        contents.append("    }");
        contents.append("}");
        IO.write(simpleTestFile, contents.toString());
    }

    private void writeSimpleBuildFile(final File buildFile) throws CruiseControlException {
        /*
         * This compiles the Simple and SimpleTest classes, runs JUnit, and generates a junitreport
         */

        final StringBuilder contents = new StringBuilder();
        contents.append("<project name='Simple' basedir='.' default='all'>\n");
        contents.append("    <target name='all'>\n");
        contents.append("         <mkdir dir='build' />\n");
        contents.append("         <path id='test.classpath'>\n");
        contents.append("             <pathelement location='build' />\n");
        contents.append("         </path>\n");

        // For whatever reason, rpm versions of Ant on Fedora fail with: Error starting modern compiler
        // unless we add fork="yes" to the javac line.
        // Also, Fedora requires the 'ant-trax' package to build the unit test report, otherwise you get the error:
        // Errors while applying transformations:
        //                      java.lang.ClassNotFoundException: org.apache.tools.ant.taskdefs.optional.TraXLiaison
        //  Here's my list of rpm ant
        // packages needed the CC unit tests to work:
        // ant.i386                                 1.7.0-1jpp.4.fc9       installed
        // ant-junit.i386                           1.7.0-1jpp.4.fc9       installed
        // ant-nodeps.i386                          1.7.0-1jpp.4.fc9       installed
        // ant-scripts.i386                         1.7.0-1jpp.4.fc9       installed
        // ant-trax.i386                            1.7.0-1jpp.4.fc9       installed
//        contents.append("         <javac srcdir='src' destdir='build' />\n");
//        contents.append("         <javac srcdir='test' destdir='build' classpathref='test.classpath' />\n");
        contents.append("         <javac srcdir='src' destdir='build' fork='yes' />\n");
        contents.append("         <javac srcdir='test' destdir='build' classpathref='test.classpath' fork='yes' />\n");

        contents.append("         <mkdir dir='reports/xml' />");
        contents.append("         <junit fork='yes'>\n");
        contents.append("             <classpath>\n");
        contents.append("                 <path refid='test.classpath' />\n");
        contents.append("             </classpath>\n");
        contents.append("             <formatter type='xml' />\n");
        contents.append("             <batchtest fork='yes' todir='reports/xml'>\n");
        contents.append("                 <fileset dir='build'>\n");
        contents.append("                     <include name='**/*Test*.class' />\n");
        contents.append("                 </fileset>\n");
        contents.append("             </batchtest>\n");
        contents.append("         </junit>\n");
        contents.append("         <junitreport>\n");
        contents.append("             <fileset dir='reports/xml'>\n");
        contents.append("                 <include name='TEST-*.xml'/>\n");
        contents.append("             </fileset>\n");
        contents.append("             <report format='frames' todir='reports/html' />\n");
        contents.append("         </junitreport>\n");
        contents.append("    </target>\n");
        contents.append("\n");
        contents.append("</project>\n");
        IO.write(buildFile, contents.toString());
    }

}
