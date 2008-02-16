package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

public class DashboardXmlConfigServiceTest extends MockObjectTestCase {

    private static String dashboardConfig =
            "<dashboard>\n"
                    + " \t<buildloop\n"
                    + " \t\thome=\"c:\\\"\n"
                    + "    \tconfigfile=\"config.xml\"\n"
                    + "    \tlogsdir=\"logs\"\n"
                    + "    \tartifactsdir=\"artifacts\"\n"
                    + "\t\tprojectsdir=\"projects\"\n"
                    + "    \tjmxport=\"8000\"\n"
                    + "    \trmiport=\"1099\"/>\n"
                    + "\t<features allowforcebuild=\"true\"/>\n"
                    + "\t<trackingtool projectname=\"project1\" baseurl=\"https://mingle05."
                    + "thoughtworks.com/projects/project1/cards/\" keywords=\"#,build,story,card\"/>\n"
                    + "\t<trackingtool projectname=\"project2\" baseurl=\"https://mingle05."
                    + "thoughtworks.com/projects/project2/cards/\" keywords=\"#,build,story,card\"/>\n"
                    + "<subtabs>\n"
                    + "  <subtab class=\"net.sourceforge.cruisecontrol.dashboard.widgets.MergedCheckStyleWidget\" />\n"
                    + "  <subtab class=\"net.sourceforge.cruisecontrol.dashboard.widgets.PanopticodeWidget\" />\n"
                    + "  <subtab class=\"net.sourceforge.cruisecontrol.dashboard.widgets.EmmaArtifactWidget\" />\n"
                    + "</subtabs>\n" + " </dashboard>";

    public void testShouldReturnBuildLoop() throws Exception {
        DashboardXmlConfigService c = buildConfigService();
        c.afterPropertiesSet();
        assertEquals("logs", c.getLogsDir());
        assertEquals("artifacts", c.getArtifactsDir());
        assertEquals("true", c.isForceBuildEnabled());
    }

    public void testShouldReturnInvalidIfCannotFindDashboardConfigFile() throws Exception {
        Mock mock = mock(DashboardConfigFileFactory.class);
        mock.expects(once()).method("asStream").will(throwException(new FileNotFoundException("not found")));
        DashboardXmlConfigService dashboardConfigService =
                new DashboardXmlConfigService((DashboardConfigFileFactory) mock.proxy());
        dashboardConfigService.afterPropertiesSet();
        assertEquals("", dashboardConfigService.getLogsDir());
        assertEquals("", dashboardConfigService.getArtifactsDir());
        assertEquals("", dashboardConfigService.isForceBuildEnabled());
    }

    public void testShouldReturnStoryTrackers() throws Exception {
        DashboardXmlConfigService configService = buildConfigService();
        configService.afterPropertiesSet();
        Map storyTrackers = configService.getStoryTrackers();
        assertEquals(2, storyTrackers.size());

        assertTrue(storyTrackers.containsKey("project1"));
        assertTrue(storyTrackers.containsKey("project2"));

        StoryTracker cc = (StoryTracker) storyTrackers.get("project1");
        assertEquals("https://mingle05.thoughtworks.com/projects/project1/cards/", cc.getBaseUrl());
        assertEquals("#,build,story,card", cc.getKeywords());
    }

    public void testShouldReturnDashboardConfigurationFile() throws Exception {
        File dashboardConfigFile = new File("dc.xml");
        Mock mock = mock(DashboardConfigFileFactory.class);
        mock.expects(once()).method("getDashboardConfigFileLocation").will(returnValue(dashboardConfigFile));
        DashboardXmlConfigService configService =
                new DashboardXmlConfigService((DashboardConfigFileFactory) mock.proxy());

        assertEquals(dashboardConfigFile, configService.getConfigurationFile());
    }

    public void testShouldGetAllSubtabClasses() throws Exception {
        DashboardXmlConfigService configService = buildConfigService();
        configService.afterPropertiesSet();
        List subTabClasses = configService.getSubTabClassNames();
        assertEquals(3, subTabClasses.size());
        assertTrue(
                subTabClasses
                        .contains("net.sourceforge.cruisecontrol.dashboard.widgets.MergedCheckStyleWidget"));
        assertTrue(
                subTabClasses
                        .contains("net.sourceforge.cruisecontrol.dashboard.widgets.EmmaArtifactWidget"));
        assertTrue(
                subTabClasses
                        .contains("net.sourceforge.cruisecontrol.dashboard.widgets.PanopticodeWidget"));
    }

    public void testShouldReturnEmptyListIfCOnfigurationIsInvalid() throws Exception {
        Mock mock = mock(DashboardConfigFileFactory.class);
        mock.expects(once()).method("asStream").will(returnValue(null));
        DashboardXmlConfigService configService =
                new DashboardXmlConfigService((DashboardConfigFileFactory) mock.proxy());

        configService.afterPropertiesSet();
        assertEquals(0, configService.getSubTabClassNames().size());
    }

    private DashboardXmlConfigService buildConfigService() {
        Mock mock = mock(DashboardConfigFileFactory.class);
        mock.expects(once()).method("asStream").will(
                returnValue(new ByteArrayInputStream(dashboardConfig.getBytes())));
        return new DashboardXmlConfigService((DashboardConfigFileFactory) mock.proxy());
    }
}
