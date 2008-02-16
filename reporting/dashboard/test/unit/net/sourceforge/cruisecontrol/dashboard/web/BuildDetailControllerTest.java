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
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.service.BuildService;
import net.sourceforge.cruisecontrol.dashboard.service.HistoricalBuildSummariesService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildSummaryUIService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.SystemPropertyConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.WidgetPluginService;
import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.BuildLoopQueryServiceStub;
import net.sourceforge.cruisecontrol.dashboard.web.command.BuildCommand;
import net.sourceforge.cruisecontrol.dashboard.widgets.Widget;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.PropertiesMethodNameResolver;

public class BuildDetailControllerTest extends SpringBasedControllerTests {

    private BuildDetailController controller;

    private PropertiesMethodNameResolver projectDetailResolver;

    protected void onControllerSetup() throws Exception {
        System.setProperty(DashboardConfigFileFactory.PROPS_CC_DASHBOARD_CONFIG,
                "test/data/dashboard-config.xml");
        System.setProperty(BuildLoopQueryServiceStub.PROPS_CC_CONFIG_FILE, DataUtils.getConfigXmlAsFile()
                .getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_ARTIFACTS_DIR, DataUtils
                .getArtifactsDirAsFile().getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_LOG_DIR, DataUtils.getLogDirAsFile()
                .getAbsolutePath());
        super.onControllerSetup();

        springConfigurationService().setDashboardConfigLocation(DataUtils.getConfigXmlAsFile().getAbsolutePath());
        springDashboardXmlConfigService().afterPropertiesSet();
    }

    protected void onTearDown() throws Exception {
        System.setProperty(DashboardConfigFileFactory.PROPS_CC_DASHBOARD_CONFIG, "");
    }

    public void setBuildDetailController(BuildDetailController controller) {
        this.controller = controller;
    }

    private void prepareRequest(String name, String projectName) {
        getRequest().setMethod("GET");
        getRequest().setRequestURI("/build/detail/" + projectName + "/"
                + CCDateFormatter.getBuildDateFromLogFileName(name));
    }

    private BuildDetail getBuildDetail(BuildCommand buildCommand) {
        return (BuildDetail) buildCommand.getBuild();
    }

    public void testShouldBeAbleToShowFailedTestCasesForFailedBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildCommand buildCommand = (BuildCommand) mav.getModel().get("buildCmd");
        BuildDetail build = getBuildDetail(buildCommand);
        List suites = build.getTestSuites();
        BuildTestSuite suite = (BuildTestSuite) suites.get(0);
        assertEquals(DataUtils.TESTSUITE_IN_BUILD_LBUILD, suite.getName());
        assertEquals(12, suite.getNumberOfTests());
        assertEquals("Get wrong number of passed test cases.", 8, suite.getPassedTestCases().size());
        assertEquals("Get wrong number of failed test cases.", 3, suite.getFailingTestCases().size());
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
        BuildCommand buildCommand = (BuildCommand) mav.getModel().get("buildCmd");
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
        assertEquals("less than a minute ago", buildDuration);
    }

    public void testShouldBeAbleToShowFailedCheckStyleForFailedBuild() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("buildCmd"));
        String pluginOutput = (String) build.getPluginOutputs().get("Merged Check Style");
        assertTrue(StringUtils.contains(pluginOutput, "Parser.java"));
    }

    public void setProjectDetailResolver(PropertiesMethodNameResolver resolver) {
        this.projectDetailResolver = resolver;
    }

    public void testShouldBeAbleToInvokePluginOutputServiceStub() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        BuildDetailController newController = createNewController(new WidgetPluginService(null) {
            public void mergePluginOutput(BuildDetail build, Map parameters) {
                build.addPluginOutput("checkstyle", "some thing got wrong");
            }
        });
        newController.setMethodNameResolver(projectDetailResolver);
        ModelAndView mav = newController.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("buildCmd"));
        assertEquals("some thing got wrong", build.getPluginOutputs().get("checkstyle"));
    }

    public void testShouldPassWebContextRootToWidgets() throws Exception {
        prepareRequest(DataUtils.getFailedBuildLbuildAsFile().getName(), "project1");
        getRequest().setContextPath("any_new_context_path");
        BuildDetailController newController = createNewController(new WidgetPluginService(null) {
            public void mergePluginOutput(BuildDetail build, Map parameters) {
                build.addPluginOutput("checkstyle", parameters.get(Widget.PARAM_WEB_CONTEXT_PATH));
            }
        });
        newController.setMethodNameResolver(projectDetailResolver);
        ModelAndView mav = newController.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("buildCmd"));
        assertEquals("any_new_context_path", build.getPluginOutputs().get("checkstyle"));
    }

    private BuildDetailController createNewController(WidgetPluginService widgetPluginService) {
        return new BuildDetailController(
                springBuildService(),
                springHistoricalBuildSummaryService(),
                widgetPluginService,
                springBuildSummaryUIService(),
                springBuildLoopQueryService());
    }

    public void testShouldBeAbleToGetCheckstyleInfoAndMergeItToBuild() throws Exception {
        prepareRequest("log20051209122103.xml", "project2");
        ModelAndView mav = this.controller.handleRequest(getRequest(), getResponse());
        BuildDetail build = getBuildDetail((BuildCommand) mav.getModel().get("buildCmd"));
        assertTrue(StringUtils.contains((String) build.getPluginOutputs().get("Merged Check Style"), "Parser.java"));
    }

    private BuildSummaryUIService springBuildSummaryUIService() {
        return (BuildSummaryUIService) this.applicationContext.getBean("buildSummaryUIService");
    }

    private BuildLoopQueryService springBuildLoopQueryService() {
        return (BuildLoopQueryService) this.applicationContext.getBean("jmxServiceStub");
    }

    private DashboardXmlConfigService springDashboardXmlConfigService() {
        return (DashboardXmlConfigService) this.applicationContext.getBean("dashboardXmlConfigService");
    }

    private BuildService springBuildService() {
        return (BuildService) this.applicationContext.getBean("buildService");
    }

    private HistoricalBuildSummariesService springHistoricalBuildSummaryService() {
        return (HistoricalBuildSummariesService) this.applicationContext.getBean("historicalBuildSummariesService");
    }

    private ConfigurationService springConfigurationService() {
        return (ConfigurationService) this.applicationContext.getBean("configuration");
    }
}
