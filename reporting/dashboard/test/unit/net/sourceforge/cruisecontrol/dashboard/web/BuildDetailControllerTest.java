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
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.WidgetPluginService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import net.sourceforge.cruisecontrol.dashboard.widgets.Widget;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.PropertiesMethodNameResolver;

public class BuildDetailControllerTest extends SpringBasedControllerTests {

    private BuildDetailController controller;

    private Configuration configuration;

    private PropertiesMethodNameResolver projectDetailResolver;

    protected void onControllerSetup() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_HOME, "test/data");
        System.setProperty(EnvironmentService.PROPS_CC_CONFIG_ARTIFACTS_DIR,
                DataUtils.getArtifactsDirAsFile().getAbsolutePath());
        super.onControllerSetup();
        configuration = (Configuration) this.applicationContext.getBean("configuration");
        configuration.setCruiseConfigLocation(DataUtils.getConfigXmlAsFile().getAbsolutePath());
    }
    
    protected void onTearDown() throws Exception {
        System.setProperty(EnvironmentService.PROPS_CC_HOME, "");
    }

    public void setBuildDetailController(BuildDetailController controller) {
        this.controller = controller;
    }

    private void prepareRequest(String name, String projectName) {
        getRequest().setMethod("GET");
        getRequest().setRequestURI("/detail/" + projectName + "/" + name);
    }

    private BuildDetail getBuildDetail(BuildCommand buildCommand) {
        return (BuildDetail) buildCommand.getBuild();
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
        assertEquals("net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest", testCase
                .getClassname());
        assertEquals("junit.framework.AssertionFailedError: Error during schema validation \n"
                + "\tat junit.framework.Assert.fail(Assert.java:47)", testCase.getMessageBody());
        assertEquals(1, suite.getErrorTestCases().size());
        testCase = (BuildTestCase) suite.getErrorTestCases().get(0);
        assertEquals("testFourConnected", testCase.getName());
        assertEquals("0.016", testCase.getDuration());
        assertEquals("net.sourceforge.cruisecontrol.sampleproject.connectfour.PlayingStandTest", testCase
                .getClassname());
        assertEquals("java.lang.NoClassDefFoundError: org/objectweb/asm/CodeVisitor\n"
                + "\tat net.sf.cglib.core.KeyFactory$Generator.generateClass(KeyFactory.java:165)", testCase
                .getMessageBody());
    }

    public void testShouldBeAbleToFindArtifactsForSuccessfulBuildOfConfiguredProject() throws Exception {
        prepareRequest(DataUtils.getPassingBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        assertEquals("page_build_detail", mav.getViewName());
        BuildCommand buildCommand = (BuildCommand) mav.getModel().get("build");
        BuildDetail build = getBuildDetail(buildCommand);
        String artifacts = build.getArtifactFiles().toString();
        assertEquals(3, build.getArtifactFiles().size());
        assertTrue(StringUtils.contains(artifacts, "artifact1.txt"));
        assertFalse(StringUtils.contains(artifacts, "artifact2.txt"));
        assertTrue(StringUtils.contains(artifacts, "subdir"));
        assertTrue(StringUtils.contains(artifacts, "coverage.xml"));
    }

    public void testShouldBeAbleToShowTheDurationBetweenSuccessfulBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        Map model = mav.getModel();
        String buildDuration = (String) model.get("durationToSuccessfulBuild");
        assertEquals("1 seconds ago", buildDuration);
    }

    public void testShouldBeAbleToShowFailedCheckStyleForFailedBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertTrue(StringUtils.contains((String) build.getPluginOutputs().get("Merged Check Style"),
                "Parser.java"));
    }

    public void setProjectDetailResolver(PropertiesMethodNameResolver resolver) {
        this.projectDetailResolver = resolver;
    }

    public void testShouldBeAbleToInvokePluginOutputServiceStub() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        BuildSummariesService service = new BuildSummariesService(configuration, new BuildSummaryService());
        BuildDetailController newController =
                new BuildDetailController(new BuildService(configuration), service, new WidgetPluginService(
                        configuration) {
                    public void mergePluginOutput(BuildDetail build, Map parameters) {
                        build.addPluginOutput("checkstyle", "some thing got wrong");
                    }
                }, new BuildSummaryUIService(service), null);
        newController.setMethodNameResolver(projectDetailResolver);
        ModelAndView mav = newController.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertEquals("some thing got wrong", build.getPluginOutputs().get("checkstyle"));
    }

    public void testShouldPassWebContextRootToWidgets() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        getRequest().setContextPath("any_new_context_path");
        BuildSummariesService service = new BuildSummariesService(configuration, new BuildSummaryService());
        BuildDetailController newController =
                new BuildDetailController(new BuildService(configuration), service, new WidgetPluginService(
                        configuration) {
                    public void mergePluginOutput(BuildDetail build, Map parameters) {
                        build.addPluginOutput("checkstyle", parameters.get(Widget.PARAM_WEB_CONTEXT_PATH));
                    }
                }, new BuildSummaryUIService(service), null);
        newController.setMethodNameResolver(projectDetailResolver);
        ModelAndView mav = newController.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertEquals("any_new_context_path", build.getPluginOutputs().get("checkstyle"));
    }

    public void testShouldBeAbleToGetCheckstyleInfoAndMergeItToBuild() throws Exception {
        prepareRequest("log20051209122103.xml", "project2");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("build"));
        assertTrue(StringUtils.contains((String) build.getPluginOutputs().get("Merged Check Style"),
                "Parser.java"));
    }
}
