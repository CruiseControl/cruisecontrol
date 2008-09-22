package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.CurrentStatus;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LatestBuildSummariesServiceTest extends MockObjectTestCase {
    private Mock mockHistoricalBuildSummaries;
    private Mock mockBuildLoopQueryService;
    private LatestBuildSummariesService latestBuildSummariesService;

    public void setUp() {
        mockHistoricalBuildSummaries = mock(
                HistoricalBuildSummariesService.class, new Class[]{ConfigurationService.class,
                BuildSummaryService.class}, new Object[]{null, null});
        mockBuildLoopQueryService = mock(
                BuildLoopQueryService.class, new Class[]{EnvironmentService.class,
                BuildInformationRepository.class}, new Object[]{null, null});
        latestBuildSummariesService = new LatestBuildSummariesService(
                (HistoricalBuildSummariesService)
                        mockHistoricalBuildSummaries.proxy(),
                (BuildLoopQueryService) mockBuildLoopQueryService.proxy());
    }


    public void testShouldUpdateTheStatusOfActivatedProject() throws Exception {
        BuildSummary bs = new BuildSummary("project2");
        bs.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        Mock mockProjectInfo = mock(
                BuildLoopInformation.ProjectInfo.class, new Class[]{String.class, String.class, String.class},
                new Object[]{"", "", ""});
        mockProjectInfo.expects(once()).method("getStatus").will(returnValue(CurrentStatus.PAUSED.getCruiseStatus()));
        mockHistoricalBuildSummaries.expects(once()).method("getLatest").with(eq("project2")).will(returnValue(bs));
        mockBuildLoopQueryService.expects(once()).method("getProjectInfo").will(returnValue(mockProjectInfo.proxy()));
        mockBuildLoopQueryService.expects(once()).method("getServerName").will(
                returnValue("localhost"));
        assertFalse(CurrentStatus.PAUSED.equals(bs.getCurrentStatus()));
        latestBuildSummariesService.getLatestProject("project2");
        assertEquals(CurrentStatus.PAUSED, bs.getCurrentStatus());
    }

    public void testShouldBeAbleToReturnAllLastestBuildOfProjects() {
        BuildSummary inactivebs = new BuildSummary("projec1");
        inactivebs.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        List inactives = new ArrayList();
        inactives.add(inactivebs);

        BuildSummary activebs = new BuildSummary("projec2");
        activebs.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        List actives = new ArrayList();
        actives.add(activebs);


        BuildSummary discontinuedbs = new BuildSummary("projec3");
        discontinuedbs.updateStatus(CurrentStatus.DISCONTINUED.getCruiseStatus());
        List discontinueds = new ArrayList();
        discontinueds.add(discontinuedbs);


        mockHistoricalBuildSummaries.expects(once()).method("createInactiveProjects").will(returnValue(inactives));
        mockHistoricalBuildSummaries.expects(once()).method("createActiveProjects").will(returnValue(actives));
        mockHistoricalBuildSummaries.expects(once()).method("createDiscontinuedProjects")
                .will(returnValue(discontinueds));

        mockBuildLoopQueryService.expects(atLeastOnce()).method("getAllProjectsStatus").will(
                returnValue(
                        new HashMap() {
                            public boolean containsKey(Object object) {
                                return true;
                            }

                            public Object get(Object key) {
                                return CurrentStatus.BUILDING.getCruiseStatus();
                            }
                        }));
        Mock mockProjectInfo = mock(BuildLoopInformation.ProjectInfo.class,
                new Class[]{String.class, String.class, String.class}, new Object[]{null, null, null});
        mockProjectInfo.expects(atLeastOnce()).method("getBuildStartTime").will(returnValue("2007-09-08T12:13:11"));
        mockBuildLoopQueryService.expects(atLeastOnce()).method("getProjectInfo").will(
                returnValue(mockProjectInfo.proxy()));
        mockBuildLoopQueryService.expects(atLeastOnce()).method("getServerName").will(
                returnValue("localhost"));

        List allLatestOfProjects = latestBuildSummariesService.getLatestOfProjects();
        assertEquals(3, allLatestOfProjects.size());
        assertEquals("localhost", ((BuildSummary) allLatestOfProjects.get(0)).getServerName());
        //should be sorted by name
        assertEquals(inactivebs, allLatestOfProjects.get(0));
        assertEquals(CurrentStatus.BUILDING, inactivebs.getCurrentStatus());
        DateTime dateTime = CCDateFormatter.iso8601("2007-09-08T12:13:11");
        assertEquals(dateTime, inactivebs.getBuildingSince());
        assertEquals(activebs, allLatestOfProjects.get(1));
        assertEquals(discontinuedbs, allLatestOfProjects.get(2));
    }

    public void testShouldBeAbleToReturnLatestBuildOfProjects() {
        BuildSummary discontinuedbs = new BuildSummary("projec3");
        discontinuedbs.updateStatus(CurrentStatus.DISCONTINUED.getCruiseStatus());

        mockHistoricalBuildSummaries.expects(once()).method("getLatest").with(
                eq("project3")).will(returnValue(discontinuedbs));
        Mock mockProjectInfo = mock(BuildLoopInformation.ProjectInfo.class,
                new Class[]{String.class, String.class, String.class}, new Object[]{null, null, null});
        mockProjectInfo.expects(once()).method("getBuildStartTime").will(returnValue("2007-09-08T12:13:11"));
        mockProjectInfo.expects(once()).method("getStatus").will(
                returnValue(CurrentStatus.BUILDING.getCruiseStatus()));
        mockBuildLoopQueryService.expects(once()).method("getProjectInfo").will(
                returnValue(mockProjectInfo.proxy()));
        mockBuildLoopQueryService.expects(once()).method("getServerName").will(
                returnValue("localhost"));

        BuildSummary buildSummary = latestBuildSummariesService.getLatestProject("project3");
        DateTime dateTime = CCDateFormatter.iso8601("2007-09-08T12:13:11");
        assertEquals(CurrentStatus.BUILDING, buildSummary.getCurrentStatus());
        assertEquals(dateTime, buildSummary.getBuildingSince());
    }

    public void testShouldNotChangeDefaultStatusWhenThereIsNoSuchProjectInLiveStatus() throws Exception {
        BuildSummary inactivebs = new BuildSummary("projec1");
        inactivebs.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        List inactives = new ArrayList();
        inactives.add(inactivebs);

        mockHistoricalBuildSummaries.expects(once()).method("createInactiveProjects").will(returnValue(inactives));
        mockHistoricalBuildSummaries.expects(once()).method("createActiveProjects").will(returnValue(new ArrayList()));
        mockHistoricalBuildSummaries.expects(once()).method("createDiscontinuedProjects")
                .will(returnValue(new ArrayList()));

        mockBuildLoopQueryService.expects(atLeastOnce()).method("getAllProjectsStatus").will(
                returnValue(
                        new HashMap() {
                            public boolean containsKey(Object object) {
                                return false;
                            }

                            public Object get(Object key) {
                                return CurrentStatus.BUILDING.getCruiseStatus();
                            }
                        }));
        latestBuildSummariesService.getLatestOfProjects();
        assertEquals(CurrentStatus.WAITING, inactivebs.getCurrentStatus());
    }
}
