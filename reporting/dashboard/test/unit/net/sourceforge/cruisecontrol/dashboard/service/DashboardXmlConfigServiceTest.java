package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class DashboardXmlConfigServiceTest extends MockObjectTestCase {
    public void testShouldReturnBuildLoop() throws Exception {
        File dashboardConfig = DataUtils.getDashboardConfig();
        Mock mockSystemService = mock(SystemService.class);
        mockSystemService.expects(once()).method("getProperty").with(eq("dashboard.config")).will(
                returnValue(dashboardConfig.getAbsolutePath()));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        DashboardXmlConfigService dashboardConfigService =
                new DashboardXmlConfigService((SystemService) mockSystemService.proxy());
        dashboardConfigService.afterPropertiesSet();
        assertEquals("config.xml", dashboardConfigService.getConfigXml());
        assertEquals("logs", dashboardConfigService.getLogsDir());
        assertEquals("artifacts", dashboardConfigService.getArtifactsDir());
        assertEquals("projects", dashboardConfigService.getProjectsDir());
        assertEquals("8000", dashboardConfigService.getJMXPort());
        assertEquals("1099", dashboardConfigService.getRMIPort());
        assertEquals("true", dashboardConfigService.isForceBuildEnabled());
        assertEquals("true", dashboardConfigService.isConfigFileEditable());
    }

    public void testShouldReturnInvalidIfCannotFindDashboardConfigFile() throws Exception {
        Mock mockSystemService = mock(SystemService.class);
        mockSystemService.expects(once()).method("getProperty").with(eq("dashboard.config")).will(
                returnValue("I/Dont/Exist.xml"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        DashboardXmlConfigService dashboardConfigService =
                new DashboardXmlConfigService((SystemService) mockSystemService.proxy());
        dashboardConfigService.afterPropertiesSet();
        assertEquals("", dashboardConfigService.getConfigXml());
        assertEquals("", dashboardConfigService.getLogsDir());
        assertEquals("", dashboardConfigService.getArtifactsDir());
        assertEquals("", dashboardConfigService.getProjectsDir());
        assertEquals("", dashboardConfigService.getJMXPort());
        assertEquals("", dashboardConfigService.getRMIPort());
        assertEquals("", dashboardConfigService.isForceBuildEnabled());
        assertEquals("", dashboardConfigService.isConfigFileEditable());
    }

    public void testShouldLoadDashboardConfigFileFromCCHome() throws Exception {
        Mock mockSystemService = mock(SystemService.class);
        mockSystemService.expects(once()).method("getProperty").with(eq("dashboard.config")).will(
                returnValue("I/Dont/Exist.xml"));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(false));
        mockSystemService.expects(once()).method("getProperty").with(
                eq(SystemPropertyConfigService.PROPS_CC_HOME)).will(returnValue("CC_HOME"));
        DashboardXmlConfigService dashboardConfigService =
                new DashboardXmlConfigService((SystemService) mockSystemService.proxy());
        dashboardConfigService.afterPropertiesSet();
    }
    

    public void testShouldReturnStoryTrackers() throws Exception {
        File dashboardConfig = DataUtils.getDashboardConfig();
        Mock mockSystemService = mock(SystemService.class);
        mockSystemService.expects(once()).method("getProperty").with(eq("dashboard.config")).will(
                returnValue(dashboardConfig.getAbsolutePath()));
        mockSystemService.expects(once()).method("isAbsolutePath").will(returnValue(true));
        DashboardXmlConfigService dashboardConfigService =
                new DashboardXmlConfigService((SystemService) mockSystemService.proxy());
        dashboardConfigService.afterPropertiesSet();
        Map storyTrackers = dashboardConfigService.getStoryTrackers();
        assertEquals(2, storyTrackers.size());

        assertTrue(storyTrackers.containsKey("project1"));
        assertTrue(storyTrackers.containsKey("project2"));

        StoryTracker cc = (StoryTracker) storyTrackers.get("project1");
        assertEquals("https://mingle05.thoughtworks.com/projects/project1/cards/", cc.getBaseUrl());
        assertEquals("#,build,story,card", cc.getKeywords());

    }
}