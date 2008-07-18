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
import net.sourceforge.cruisecontrol.util.Util;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Maven2BuilderTest extends TestCase {

    private static final String MOCK_SUCCESS = "successful build";
    private static final String MOCK_BUILD_FAILURE = "failed build";
    private static final String MOCK_BUILD_ERROR = "download failure";
    private static final String MOCK_FATAL_ERROR = "fatal error";
    private static final String MOCK_SCRIPT_TIMEOUT = "build timeout";

    private static final String MSG_PREFIX_HOME_SCRIPT_BOTH_SET = "'mvnhome' and 'mvnscript' cannot both be set.";


    private File createTestMvnScriptFile() throws IOException, CruiseControlException {
        File testScript = File.createTempFile("Maven2BuilderTest.testValidate", "_testmaven.bat");
        testScript.deleteOnExit();
        MavenBuilderTest.makeTestFile(testScript, "@echo This is a fake maven.bat\n", true, filesToDelete);
        return testScript;
    }

    private File createTestMvnProjectFile() throws IOException, CruiseControlException {
        File testProject = File.createTempFile("Maven2BuilderTest.testValidate", "_testproject.xml");
        testProject.deleteOnExit();
        MavenBuilderTest.makeTestFile(testProject,
            "<project><!-- This is a fake Maven project file --></project>\n", true, filesToDelete);
        return testProject;
    }

    private Maven2Builder createValidM2Builder() throws IOException, CruiseControlException {
        Maven2Builder mb = new Maven2Builder();
        // these files must also exist for Maven2Builder to be happy.
        final File testScript = createTestMvnScriptFile();
        filesToDelete.add(testScript);
        final File testProject = createTestMvnProjectFile();
        filesToDelete.add(testProject);

        mb.setMultiple(1);
        mb.setMvnHome(testScript.getParentFile().getAbsolutePath());
        mb.setPomFile(testProject.getAbsolutePath());
        mb.setGoal("mygoal");
        return mb;
    }

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    protected void tearDown() {
        filesToDelete.delete();
    }

    public void testFindMaven2Script() throws Exception {
        final Maven2Builder mb = new Maven2Builder();
        try {
            mb.findMaven2Script(false);
            fail("expected exception");
        } catch (CruiseControlException e) {
            assertEquals("mvnhome attribute not set.", e.getMessage());
        }
        try {
            mb.findMaven2Script(true);
            fail("expected exception");
        } catch (CruiseControlException e) {
            assertEquals("mvnhome attribute not set.", e.getMessage());
        }

        final String testMvnHome = "isWindowsTestScript";
        mb.setMvnHome(testMvnHome);
        assertEquals(testMvnHome + File.separator + Maven2Builder.MVN_BIN_DIR + "mvn.bat",
                mb.findMaven2Script(true));
        assertEquals(testMvnHome + File.separator + Maven2Builder.MVN_BIN_DIR + "mvn",
                mb.findMaven2Script(false));
    }

    public void testValidateMvnHomeAndMvnScriptSet() throws Exception {
        final Maven2Builder mb = createValidM2Builder();

        // make invalid by setting both Home and Script
        final File testScript = createTestMvnScriptFile();
        mb.setMvnHome(testScript.getParentFile().getAbsolutePath());
        mb.setMvnScript(testScript.getAbsolutePath());

        try {
            mb.validate();
            fail();
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith(MSG_PREFIX_HOME_SCRIPT_BOTH_SET));
        }

        mb.setMvnScript(null);
        mb.validate();
        // rerun validate to test for reuse issues
        try {
            mb.validate();
            fail("Second call to validate() should have failed.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith(MSG_PREFIX_HOME_SCRIPT_BOTH_SET));
        }
    }

    /**
     * Test validation with MvnHome set and resuse issues.
     * @throws Exception if anything breaks
     */
    public void testValidateMvnHomeReuse() throws Exception {
        final Maven2Builder mb = createValidM2Builder();

        mb.validate();
        // rerun validate to test for reuse issues
        try {
            mb.validate();
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith(MSG_PREFIX_HOME_SCRIPT_BOTH_SET));
        }            
    }

    public void testValidate() throws Exception {
        Maven2Builder mb = new Maven2Builder();
        try {
            mb.validate();
            fail("Maven2Builder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'mvnhome' or 'mvnscript' must be set.", e.getMessage());
        }

        // these files must also exist for Maven2Builder to be happy.
        final File testScript = createTestMvnScriptFile();
        final File testProject = createTestMvnProjectFile();

        mb.setMultiple(1);
        mb.setMvnScript(testScript.getAbsolutePath());

        try {
            mb.validate();
            fail("Maven2Builder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'pomfile' is required for Maven2Builder", e.getMessage());
        }

        mb.setPomFile(testProject.getAbsolutePath());

        try {
            mb.validate();
            fail("Maven2Builder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'goal' is required for Maven2Builder", e.getMessage());
        }

        mb.setGoal("");

        try {
            mb.validate();
            fail("Maven2Builder should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'goal' is required for Maven2Builder", e.getMessage());
        }

        mb.setGoal("mygoal");

        mb.validate();
        // do validate again, just to check reuse issues
        mb.validate();
    }
    
    public void testValidatePomFile() throws Exception {
        Maven2Builder mb = new Maven2Builder();
        final File testProject = createTestMvnProjectFile();
        mb.validatePomFile(testProject);
        try {
            mb.validatePomFile(testProject.getParentFile());
            fail("directories are not valid pom files");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith("the pom file can't be a directory"));
        }
        assertTrue(testProject.delete());
        assertFalse(testProject.exists());
        try {
            mb.validatePomFile(testProject);
            fail("pom files must exist");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().startsWith("the pom file could not be found"));
        }
    }

    public void testBuild_Success() throws IOException, CruiseControlException {
        Maven2Builder mb = new Maven2Builder();
        internalTestBuild(MOCK_SUCCESS, mb);
    }

    public void testBuild_BuildFailure() throws IOException, CruiseControlException {
        Maven2Builder mb = new Maven2Builder();
        internalTestBuild(MOCK_BUILD_FAILURE, mb);
    }

    public void testBuild_Error() throws IOException, CruiseControlException {
        Maven2Builder mb = new Maven2Builder();
        internalTestBuild(MOCK_BUILD_ERROR, mb);
    }

    public void testBuild_FatalError() throws IOException, CruiseControlException {
        Maven2Builder mb = new Maven2Builder();
        internalTestBuild(MOCK_FATAL_ERROR, mb);
    }

    /**
     * Element build(Map). mockFailure == (Mock a failure?).
     *
     * @param statusType The exit status to be tested
     * @param mb a Maven2Builder instance
     * @throws IOException if something breaks
     * @throws CruiseControlException if something breaks
     */
    private void internalTestBuild(final String statusType, final Maven2Builder mb)
            throws IOException, CruiseControlException {

        setupMockBuild(statusType, mb);

        // some fake goal is still needed to start working (no '|' here!)
        mb.setGoal("fakegoal");
        // this should "succeed"
        Element logElement = mb.build(getUnitTestBuildProperties(), null);

        assertNotNull(statusType, logElement);
        List goalTags = logElement.getChildren("mavengoal");
        assertNotNull(statusType, goalTags);
        assertEquals(statusType, 2, goalTags.size());
        Element we = (Element) goalTags.get(0);
        assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
        we = (Element) goalTags.get(1);
        assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        assertBuildState(statusType, logElement);

        // this time let's test multiple runs
        mb.setGoal("fakegoal|otherfakegoal");
        // this should "double succeed"
        logElement = mb.build(new Hashtable(), null);
        assertNotNull(statusType, logElement);
        goalTags = logElement.getChildren("mavengoal");
        assertNotNull(statusType, goalTags);
        we = (Element) goalTags.get(0);
        assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
        we = (Element) goalTags.get(1);
        assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        assertBuildState(statusType, logElement);
        if (!isExpectedSuccess(statusType)) {
            // if we mocked a failure, the second run should never happen
            assertEquals(statusType, 2, goalTags.size());
        } else {
            assertEquals(statusType, 4, goalTags.size());
            we = (Element) goalTags.get(2);
            assertEquals(statusType, "java:compile", we.getAttribute("name").getValue());
            we = (Element) goalTags.get(3);
            assertEquals(statusType, "test:test", we.getAttribute("name").getValue());
        }
    }

    private void setupMockBuild(String statusType, Maven2Builder mb) throws IOException, CruiseControlException {
        final String statusText = getStatusText(statusType);
        // Prepare mock files.
        final String tempFilePrefix = "Maven2BuilderTest.internalTestBuild";
        final File testScript;
        if (Util.isWindows()) {
            testScript = File.createTempFile(tempFilePrefix, "_testmaven.bat");
            testScript.deleteOnExit();
            MavenBuilderTest.makeTestFile(
                testScript,
                "@rem This is a fake maven.bat\n"
                    + "@echo [INFO] [java:compile]\n"
                    + "@echo [INFO] Bla-bla-compile\n"
                    + "@echo [INFO] [test:test]\n"
                    + "@echo "
                    + statusText
                    + "\n",
                true, filesToDelete);
        } else {
            testScript = File.createTempFile(tempFilePrefix, "_testmaven.sh");
            testScript.deleteOnExit();
            MavenBuilderTest.makeTestFile(
                testScript,
                "#!/bin/sh\n"
                    + "\n"
                    + "# This is a fake maven.sh\n"
                    + "echo [INFO] [java:compile]\n"
                    + "echo [INFO] Bla-bla-compile\n"
                    + "echo [INFO] [test:test]\n"
                    + "echo "
                    + statusText
                    + "\n",
                false, filesToDelete);
        }
        mb.setMvnScript(testScript.getAbsolutePath());

        // pom must exist before call to build()
        final File testPom = File.createTempFile(tempFilePrefix, "don-t-care-pom.xml", new File("."));
        filesToDelete.add(testPom);
        testPom.deleteOnExit();
        mb.setPomFile(testPom.getName());
    }

    private boolean isExpectedSuccess(final String statusType) {
        return statusType.equals(MOCK_SUCCESS);
    }

    private void assertBuildState(final String statusType, final Element logElement) {
        if (!isExpectedSuccess(statusType)) {
            assertNotNull("error attribute not found when " + statusType, logElement.getAttribute("error"));
            assertNull(statusType, logElement.getAttribute("success"));
        } else {
            assertNull(statusType, logElement.getAttribute("error"));
            assertNotNull(statusType, logElement.getAttribute("success"));
        }
        assertNotNull(statusType + " missing 'time' attribute", logElement.getAttribute("time"));
    }

    private static Map getUnitTestBuildProperties() {
        final Map buildProperties = new Hashtable();
        buildProperties.put("cclastbuildtimestamp", "20070614095818");
        buildProperties.put("cclastgoodbuildtimestamp", "20070607114812");
        buildProperties.put("label", "build.11");
        buildProperties.put("lastbuildsuccessful", "false");
        buildProperties.put("cvstimestamp", "2007-06-14 07:59:22 GMT"); // should have spaces
        buildProperties.put("projectname", "java_32.A01");
        buildProperties.put("cctimestamp", "20070614095922");
        return buildProperties;
    }

    /**
     * List getGoalSets()
     */
    public void testGetGoalSets() {
        Maven2Builder mb = new Maven2Builder();
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

    private static final int TIMEOUT_SLEEP = 2;

    public void testBuildTimeout() throws Exception {

        if (Util.isWindows()) {
            System.out.println("Skipping testBuildTimeout(), no DOS 'sleep' command");
            return;
        }

        Maven2Builder mb = new Maven2Builder();
        mb.setTimeout(TIMEOUT_SLEEP);
        //Element logElement = internalTestBuild(MOCK_SCRIPT_TIMEOUT, mb);
        setupMockBuild(MOCK_SCRIPT_TIMEOUT, mb);

        mb.setGoal("fakegoal");

        final long startTime = System.currentTimeMillis();

        // this should "succeed"
        final Element logElement = mb.build(getUnitTestBuildProperties(), null);

        final long endTime = System.currentTimeMillis();

        assertNotNull("Build should have timed out.", logElement.getAttributeValue("error"));
        assertTrue(logElement.getAttributeValue("error").indexOf("timeout") >= 0);
        assertTrue((endTime - startTime) < 9 * 1000L);
    }

    /**
     * Text for build status
     *
     * @param statusCode The exit status to be tested
     * @return the expected script output text for the given build status
     */
    private String getStatusText(String statusCode) {
        if (statusCode.equals(MOCK_SUCCESS)) {
            return "[INFO] BUILD SUCCESSFUL";
        } else if (statusCode.equals(MOCK_BUILD_FAILURE)) {
            return "[ERROR] BUILD FAILURE";
        } else if (statusCode.equals(MOCK_BUILD_ERROR)) {
            return  "[ERROR] BUILD ERROR";
        } else if (statusCode.equals(MOCK_FATAL_ERROR)) {
            return  "[ERROR] FATAL ERROR";
        } else if (statusCode.equals(MOCK_SCRIPT_TIMEOUT)) {
            int sleepTime = TIMEOUT_SLEEP * 2;
            return  "[INFO] BUILD SUCCESSFUL"
                    + "\n" + (Util.isWindows() ? "@" : "") + "echo sleeping " + sleepTime + "..."
                    + "\nsleep " + sleepTime;
        }
        throw new IllegalArgumentException("please use one of the constants");
    }
}