/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildTestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildTestCaseResult;
import net.sourceforge.cruisecontrol.dashboard.BuildTestSuite;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.LogFile;
import net.sourceforge.cruisecontrol.dashboard.ModificationAction;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;

public class BuildServiceTest extends MockObjectTestCase {
    private BuildService buildFactory;

    private Mock mockConfiguration;
    private Mock mockQueryService;

    protected void setUp() throws Exception {
        mockConfiguration = mock(ConfigurationService.class,
                new Class[]{EnvironmentService.class, DashboardXmlConfigService.class, BuildLoopQueryService.class},
                new Object[]{null, null, null});
        mockConfiguration.expects(once())
                .method("getArtifactRoot")
                .will(returnValue(DataUtils.getProject1ArtifactDirAsFile()));
        mockQueryService = mock(BuildLoopQueryService.class);
        buildFactory = new BuildService(
                (ConfigurationService) mockConfiguration.proxy(),
                (BuildLoopQueryService) mockQueryService.proxy());
    }

    public void testShouldReadSpecficBuild() throws Exception {
        BuildDetail expectedBuild = buildFactory.createBuildFromFile(DataUtils.getFailedBuildLbuildAsFile());
        assertEquals("project1", expectedBuild.getProjectName());
        assertFalse(expectedBuild.hasPassed());
    }

    public void testCanReadErrorDetailFromTest() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getFailedBuildLbuildAsFile());

        List suites = build.getTestSuites();
        BuildTestSuite firstSuite = (BuildTestSuite) suites.get(0);

        List erroringTestCases = firstSuite.getErrorTestCases();
        assertEquals(1, erroringTestCases.size());

        BuildTestCase erroredTest = (BuildTestCase) erroringTestCases.get(0);
        assertEquals(
                "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest", erroredTest
                .getClassname());
        assertEquals("0.016", erroredTest.getDuration());
        assertEquals("testFourConnected", erroredTest.getName());
        assertTrue(erroredTest.didError());
        assertEquals("org/objectweb/asm/CodeVisitor", erroredTest.getMessage());
        assertEquals(
                "java.lang.NoClassDefFoundError: org/objectweb/asm/CodeVisitor\n"
                        + "\tat net.sf.cglib.core.KeyFactory$Generator.generateClass(KeyFactory.java:165)",
                erroredTest.getMessageBody());
    }

    public void testCanReadFailureFromTest() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getFailedBuildLbuildAsFile());

        List suites = build.getTestSuites();
        BuildTestSuite firstSuite = (BuildTestSuite) suites.get(0);

        List failingCases = firstSuite.getFailingTestCases();
        assertEquals(3, firstSuite.getNumberOfFailures());
        assertEquals(3, failingCases.size());

        BuildTestCase failingTest = (BuildTestCase) failingCases.get(0);
        assertEquals(
                "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest", failingTest
                .getClassname());
        assertEquals("3.807", failingTest.getDuration());
        assertEquals("testSomething", failingTest.getName());
        assertEquals(BuildTestCaseResult.FAILED, failingTest.getResult());
        assertEquals("Not the expected result", failingTest.getMessage());
        assertEquals(
                "junit.framework.AssertionFailedError: Error during schema validation \n"
                        + "\tat junit.framework.Assert.fail(Assert.java:47)", failingTest.getMessageBody());
    }

    public void testCanReadModificationsFromLogFile() throws Exception {
        final BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        final Collection<Modification> modifications = build.getModifications();
        assertEquals(2, modifications.size());
        final Modification modification = modifications.iterator().next();

        assertEquals("cvs", modification.type);
        assertEquals("story123 project name changed to cache", modification.comment);
        assertEquals("readcb", modification.userName);

        final List<Modification.ModifiedFile> files = modification.getModifiedFiles();
        assertEquals(2, files.size());

        final Modification.ModifiedFile firstFile = files.get(0);
        assertEquals(ModificationAction.MODIFIED, ModificationAction.fromDisplayName(firstFile.action));
        assertEquals("build.xml", firstFile.fileName);
        assertEquals("1.2", firstFile.revision);

        final Modification.ModifiedFile secondFile = files.get(1);
        assertEquals(ModificationAction.DELETED, ModificationAction.fromDisplayName(secondFile.action));
        assertEquals("blah.txt", secondFile.fileName);
        assertEquals("1.2", secondFile.revision);
    }

    public void testShouldGetDateOfBuild() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        DateTime expectedDateTime = new DateTime(2005, 12, 9, 12, 21, 3, 0);
        assertEquals(expectedDateTime, build.getBuildDate());
    }

    public void testShouldReadTestSuitesFromFailedBuild() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getFailedBuildLbuildAsFile());
        List suites = build.getTestSuites();

        assertEquals(1, suites.size());

        BuildTestSuite firstTestSuite = (BuildTestSuite) suites.get(0);
        assertEquals(1, firstTestSuite.getNumberOfErrors());
        assertEquals(3, firstTestSuite.getNumberOfFailures());
        assertEquals(
                "net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest",
                firstTestSuite.getName());
        assertEquals(12, firstTestSuite.getNumberOfTests());
        assertEquals(1000.109, firstTestSuite.getDurationInSeconds(), 0.001);
    }

    public void testShouldTellIfBuildPassed() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        assertTrue("Build should have passed", build.hasPassed());
    }

    public void testShouldTellIfBuildFailed() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getFailedBuildLbuildAsFile());
        assertFalse("Build should have passed", build.hasPassed());
    }

    public void testShouldGetProjectNameFromBuildFile() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        assertEquals("project1", build.getProjectName());
    }

    public void testShouldGetLabelFromBuildFile() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        assertEquals("build.0", build.getLabel());
    }

    public void testShouldGetBuildDurationFromBuildFile() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        assertEquals("3 minutes 10 seconds", build.getDuration());
    }

    public void testShouldStoreLogFileName() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getPassingBuildLbuildAsFile());
        assertEquals(DataUtils.PASSING_BUILD_LBUILD_0_XML, build.getLogFileName());
    }

    public void testShouldBeAbleToParseBigLogFile() throws Exception {
        BuildDetail build = buildFactory.createBuildFromFile(DataUtils.getBigLogFile());
        assertNotNull(build);
    }

    public void testShouldReturnBuildDetailOfDiscontinuedStatus() throws Exception {
        LogFile logFile = DataUtils.getPassingBuildLbuildAsFile();
        mockConfiguration.expects(once()).method("getLogRoot").will(returnValue(logFile.getParentFile()));
        mockQueryService.expects(once()).method("isDiscontinued").with(eq("project1")).will(returnValue(true));
        BuildDetail build = buildFactory.getBuild("project1",
                CCDateFormatter.getBuildDateFromLogFileName(logFile.getName()));
        assertEquals(CurrentStatus.DISCONTINUED, build.getCurrentStatus());
    }

    public void testShouldReturnBuildDetailOfPausedStatus() throws Exception {
        String paused = CurrentStatus.PAUSED.getCruiseStatus();
        LogFile logFile = DataUtils.getPassingBuildLbuildAsFile();
        mockConfiguration.expects(once()).method("getLogRoot").will(returnValue(logFile.getParentFile()));
        mockQueryService.expects(once()).method("isDiscontinued").with(eq("project1")).will(returnValue(false));
        mockQueryService.expects(once()).method("getProjectStatus").with(eq("project1")).will(returnValue(paused));
        BuildDetail build = buildFactory.getBuild("project1",
                CCDateFormatter.getBuildDateFromLogFileName(logFile.getName()));
        assertEquals(CurrentStatus.PAUSED, build.getCurrentStatus());
    }
}
