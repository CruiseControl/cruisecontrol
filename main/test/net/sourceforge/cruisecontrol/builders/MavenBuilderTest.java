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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;

import java.io.File;
import java.util.List;
import java.util.HashMap;

public class MavenBuilderTest extends TestCase {

    private static final String MOCK_SUCCESS = "successful build";
    private static final String MOCK_BUILD_FAILURE = "failed build";
    private static final String MOCK_DOWNLOAD_FAILURE = "download failure";

    public void testValidate() throws Exception {
        MavenBuilder mb = new MavenBuilder();
        try {
            mb.validate();
            fail("MavenBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        // these files must also exist for MavenBuilder to be happy.
        File testScript = File.createTempFile("MavenBuilderTest.testValidate", "_testmaven.bat");
        testScript.deleteOnExit();
        makeTestFile(testScript, "@echo This is a fake maven.bat\n", true, filesToDelete);

        File testProject = File.createTempFile("MavenBuilderTest.testValidate", "_testproject.xml");
        testProject.deleteOnExit();
        makeTestFile(testProject,
            "<project><!-- This is a fake Maven project file --></project>\n", true, filesToDelete);
        mb.setMultiple(1);
        mb.setMavenScript(testScript.getAbsolutePath());

        try {
            mb.validate();
            fail("MavenBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mb.setProjectFile(testProject.getAbsolutePath());

        try {
            mb.validate();
            fail("MavenBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mb.setGoal("");

        try {
            mb.validate();
            fail("MavenBuilder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mb.setGoal("mygoal");

        try {
            mb.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("MavenBuilder should not throw exceptions when required fields are set. Exception ["
                    + e.getMessage() + "].");
        }
    }

    public void testBuild_Success() throws Exception {
      MavenBuilder mb = new MavenBuilder();
      internalTestBuild(MOCK_SUCCESS, mb);
    }

    public void testBuild_BuildFailure() throws Exception {
        MavenBuilder mb = new MavenBuilder();
        internalTestBuild(MOCK_BUILD_FAILURE, mb);
    }

    public void testBuild_DownloadFailure() throws Exception {
        MavenBuilder mb = new MavenBuilder();
        internalTestBuild(MOCK_DOWNLOAD_FAILURE, mb);
    }

    /**
     * Element build(Map). mockFailure == (Mock a failure?).
     *
     * @param mb a maven build instance
     * @param statusType The exit status to be tested
     * @throws Exception if something breaks
     */
    private void internalTestBuild(String statusType, MavenBuilder mb)
            throws Exception {

        final File testScript;
        final boolean buildSuccessful = statusType.equals(MOCK_SUCCESS);
        final String statusText = getStatusText(statusType);
        // Prepare mock files.
        final String prefix = "MavenBuilderTest.internalTestBuild";
        if (Util.isWindows()) {
            testScript = File.createTempFile(prefix, "_testmaven.bat");
            testScript.deleteOnExit();
            makeTestFile(
                testScript,
                "@rem This is a fake maven.bat\n"
                    + "@echo java:compile:\n"
                    + "@echo Bla-bla-compile\n"
                    + "@echo test:test:\n"
                    + "@echo "
                    + statusText
                    + "\n",
                true, filesToDelete);
        } else {
            testScript = File.createTempFile(prefix, "_testmaven.sh");
            testScript.deleteOnExit();
            makeTestFile(
                testScript,
                "#!/bin/sh\n"
                    + "\n"
                    + "# This is a fake maven.sh\n"
                    + "echo java:compile:\n"
                    + "echo Bla-bla-compile\n"
                    + "echo test:test:\n"
                    + "echo "
                    + statusText
                    + "\n",
                false, filesToDelete);
        }
        mb.setMavenScript(testScript.getAbsolutePath());

        // pom must exist before call to build()
        final File testPom = File.createTempFile(prefix, "don-t-care-pom.xml", new File("."));
        filesToDelete.add(testPom);
        testPom.deleteOnExit();
        mb.setProjectFile(testPom.getName());

        Element we;
        List goalTags;

        // some fake goal is still needed to start working (no '|' here!)
        mb.setGoal("fakegoal");
        // this should "succeed"
        Element logElement = mb.build(new HashMap<String, String>(), null);
        assertNotNull(statusType, logElement);
        goalTags = logElement.getChildren("mavengoal");
        assertNotNull(statusType, goalTags);
        assertEquals(statusType, 2, goalTags.size());
        we = (Element) goalTags.get(0);
        assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
        we = (Element) goalTags.get(1);
        assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        if (!buildSuccessful) {
            assertNotNull("error attribute not found when " + statusType, logElement.getAttribute("error"));
        } else {
            assertNull(statusType, logElement.getAttribute("error"));
        }

        // this time let's test multiple runs
        mb.setGoal("fakegoal|otherfakegoal");
        // this should "double succeed"
        logElement = mb.build(new HashMap<String, String>(), null);
        assertNotNull(statusType, logElement);
        goalTags = logElement.getChildren("mavengoal");
        assertNotNull(statusType, goalTags);
        we = (Element) goalTags.get(0);
        assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
        we = (Element) goalTags.get(1);
        assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        if (!buildSuccessful) {
            assertNotNull(statusType, logElement.getAttribute("error"));
            // if we mocked a failure, the second run should never happen
            assertEquals(statusType, 2, goalTags.size());
        } else {
            assertNull(statusType, logElement.getAttribute("error"));
            assertEquals(statusType, 4, goalTags.size());
            we = (Element) goalTags.get(2);
            assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
            we = (Element) goalTags.get(3);
            assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        }
    }

    /**
     * List getGoalSets()
     */
    public void testGetGoalSets() {
        MavenBuilder mb = new MavenBuilder();
        List gsList;
        mb.setGoal(null);
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("No goal produces non-empty list", 0, gsList.size());

        mb.setGoal("clean "); // I want the space there..
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("One goal should produce one item", 1, gsList.size());
        // but notice, no space below
        assertEquals("One goal produces bad list content", "clean", (String) gsList.get(0));

        mb.setGoal(" clean|update "); // Notice the spaces here
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("Two goals should produce two items", 2, gsList.size());
        // but not here
        assertEquals("First run produces bad goal", "clean", (String) gsList.get(0));
        assertEquals("Second run produces bad goal", "update", (String) gsList.get(1));

        // full-featured test
        mb.setGoal("clean update|\ttest||"); // Notice the spaces here
        gsList = mb.getGoalSets();
        assertNotNull(gsList);
        assertEquals("Complex goal should produce two goalsets", 2, gsList.size());
        // but not here
        assertEquals("First cplx run produces bad goal", "clean update", (String) gsList.get(0));
        assertEquals("Second cplx run produces bad goal", "test", (String) gsList.get(1));
    }

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    protected void tearDown() {
        filesToDelete.delete();
    }

    /**
     * Make a test file with specified content. Assumes the file does not exist.
     * @param testFile the file to create
     * @param content the data to put into the created file
     * @param onWindows true if running on Windows OS
     * @param filesToDelete list of files to be deleted in tearDown
     * @throws net.sourceforge.cruisecontrol.CruiseControlException if anything breaks
     */
    static void makeTestFile(final File testFile, final String content, final boolean onWindows,
                              final TestUtil.FilesToDelete filesToDelete)
            throws CruiseControlException {

        IO.write(testFile.getAbsoluteFile(), content);

        if (filesToDelete != null) {
            filesToDelete.add(testFile);
        }

        if (!onWindows) {
            Commandline cmdline = new Commandline();
            cmdline.setExecutable("chmod");
            cmdline.createArgument("755");
            cmdline.createArgument(testFile.getAbsolutePath());
            try {
                Process p = cmdline.execute();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
                fail("exception changing permissions on test file " + testFile.getAbsolutePath());
            }
        }
    }


    public void testBuildTimeout() throws Exception {

        MavenBuilder builder = new MavenBuilder();
        builder.setTimeout(5);
        long startTime = System.currentTimeMillis();

        internalTestBuild(MOCK_BUILD_FAILURE, builder);

        assertTrue((System.currentTimeMillis() - startTime) < 9 * 1000L);
       // assertTrue(buildElement.getAttributeValue("error").indexOf("timeout") >= 0);


    }

    /**
     * Text for build status
     *
     * @param statusCode The exit status to be tested
     * @return the expected script output text for the given build status
     */
    private String getStatusText(String statusCode) {
        if (statusCode.equals(MOCK_SUCCESS)) {
            return "BUILD SUCCESSFUL";
        } else if (statusCode.equals(MOCK_BUILD_FAILURE)) {
            return "BUILD FAILED";
        } else if (statusCode.equals(MOCK_DOWNLOAD_FAILURE)) {
            return "The build cannot continue because of the following unsatisfied dependency";
        }
        throw new IllegalArgumentException("please use one of the constants");
    }
}