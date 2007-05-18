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
package net.sourceforge.cruisecontrol.dashboard.web;

import java.io.File;
import java.util.List;
import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.BuildTestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildTestSuite;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.PluginOutputService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemFixture;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.PropertiesMethodNameResolver;

public class BuildDetailControllerTest extends SpringBasedControllerTests {

    private BuildDetailController controller;
    private Configuration configuration;
    private File tmpFile;
    private PropertiesMethodNameResolver projectDetailResolver;

    protected void onControllerSetup() throws Exception {
        super.onControllerSetup();
        configuration = (Configuration) this.applicationContext.getBean("configuration");
        configuration.setCruiseConfigLocation(DataUtils.getConfigXmlAsFile().getAbsolutePath());
    }

    protected void onTearDown() throws Exception {
        if (tmpFile != null) {
            FileUtils.forceDelete(tmpFile);
        }
    }

    public void setBuildDetailController(BuildDetailController controller) {
        this.controller = controller;
    }

    private void prepareRequest(String name, String projectName) {
        getRequest().setMethod("GET");
        getRequest().setRequestURI("/detail/" + projectName + "/" + name);
    }

    public void testShouldBeAbleToShowFailedTestCasesForFailedBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildCommand buildCommand = (BuildCommand) mav.getModel().get("build");
        BuildDetail build = getBuildDetail(buildCommand);
        List suites = build.getTestSuites();
        BuildTestSuite suite = (BuildTestSuite) suites.get(0);
        assertEquals(DataUtils.TESTSUITE_IN_BUILD_LBUILD, suite.getName());
        assertEquals(10, suite.getNumberOfTests());
        assertEquals("Get wrong number of passed test cases.", 8, suite.getPassedTestCases().size());
        assertEquals("Get wrong number of failed test cases.", 1, suite.getFailingTestCases().size());
        BuildTestCase testCase = (BuildTestCase) suite.getFailingTestCases().get(0);
        assertEquals("testSomething", testCase.getName());
        assertEquals("3.807", testCase.getDuration());
        assertEquals("net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest",
                testCase.getClassname());
        assertEquals("junit.framework.AssertionFailedError: Error during schema validation \n"
                + "\tat junit.framework.Assert.fail(Assert.java:47)", testCase.getMessageBody());
        assertEquals(1, suite.getErrorTestCases().size());
        testCase = (BuildTestCase) suite.getErrorTestCases().get(0);
        assertEquals("testFourConnected", testCase.getName());
        assertEquals("0.016", testCase.getDuration());
        assertEquals("net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest",
                testCase.getClassname());
        assertEquals("java.lang.NoClassDefFoundError: org/objectweb/asm/CodeVisitor\n"
                + "\tat net.sf.cglib.core.KeyFactory$Generator.generateClass(KeyFactory.java:165)",
                testCase.getMessageBody());
    }

    private BuildDetail getBuildDetail(BuildCommand buildCommand) {
        return (BuildDetail) buildCommand.getBuild();
    }

    public void testShouldBeAbleToFindArtifactsForSuccessfulBuildOfConfiguredProject() throws Exception {
        prepareRequest(DataUtils.getPassingBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        assertEquals("buildDetail", mav.getViewName());
        BuildCommand buildCommand = (BuildCommand) mav.getModel().get("build");
        BuildDetail build = getBuildDetail(buildCommand);
        String artifacts = build.getArtifacts().toString();
        assertEquals(3, build.getArtifacts().size());
        assertTrue(StringUtils.contains(artifacts, "artifact1.txt"));
        assertTrue(StringUtils.contains(artifacts, "artifact2.txt"));
        assertTrue(StringUtils.contains(artifacts, "coverage.xml"));
    }

    public void testShouldBeAbleToShowTheDurationBetweenSuccessfulBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        Map model = mav.getModel();
        String buildDuration = (String) model.get("durationToSuccessfulBuild");
        assertEquals("N/A", buildDuration);
    }

    public void testShouldBeAbleToGetLastestSuccessfulBuild() throws Exception {
        File folder = DataUtils.getFailedBuildLbuildAsFile().getParentFile();
        tmpFile = FilesystemFixture.createFile("log19990704155710Lbuild.489.xml", folder);
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        Map model = mav.getModel();
        String buildDuration = (String) model.get("durationToSuccessfulBuild");
        assertTrue(StringUtils.contains(buildDuration, "days"));
        assertTrue(StringUtils.contains(buildDuration, "hours"));
        assertTrue(StringUtils.contains(buildDuration, "minutes"));
        assertTrue(StringUtils.contains(buildDuration, "seconds"));
        assertTrue(StringUtils.contains(buildDuration, "ago"));
    }

    public void testShouldBeAbleToGetLastestSuccessfulBuildWithoutDays() throws Exception {
        File folder = DataUtils.getFailedBuildLbuildAsFile().getParentFile();
        tmpFile = FilesystemFixture.createFile("log20051209112103Lbuild.489.xml", folder);
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        Map model = mav.getModel();
        String buildDuration = (String) model.get("durationToSuccessfulBuild");
        assertFalse(StringUtils.contains(buildDuration, "days"));
        assertTrue(StringUtils.contains(buildDuration, "hours"));
        assertFalse(StringUtils.contains(buildDuration, "minutes"));
        assertFalse(StringUtils.contains(buildDuration, "seconds"));
        assertTrue(StringUtils.contains(buildDuration, "ago"));
    }

    public void testShouldBeAbleToShowFailedCheckStyleForFailedBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertTrue(StringUtils.contains((String) build.getPluginOutputs().get("Merged Check Style"), "Parser.java"));
    }

    public void setProjectDetailResolver(PropertiesMethodNameResolver resolver) {
        this.projectDetailResolver = resolver;
    }

    public void testShouldBeAbleToInvokePluginOutputServiceStub() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        BuildSummariesService service = new BuildSummariesService(configuration, new BuildSummaryService());
        BuildDetailController newController = new BuildDetailController(new BuildService(configuration), service,
                new PluginOutputServiceStub(configuration), new BuildSummaryUIService(service));
        newController.setMethodNameResolver(projectDetailResolver);
        ModelAndView mav = newController.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertEquals("some thing got wrong", build.getPluginOutputs().get("checkstyle"));
    }

    private static class PluginOutputServiceStub extends PluginOutputService {
        public PluginOutputServiceStub(Configuration configuration) {
            super(configuration);
        }

        public void mergePluginOutput(BuildDetail build, Map parameters) {
            build.addPluginOutput("checkstyle", "some thing got wrong");
        }
    }

    public void testShouldBeAbleToGetCheckstyleInfoAndMergeItToBuild() throws Exception {
        prepareRequest("log20051209122103.xml", "project2");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertTrue(StringUtils.contains((String) build.getPluginOutputs().get("Merged Check Style"), "Parser.java"));
    }

}
