package net.sourceforge.cruisecontrol.dashboard;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class ViewableStatusHelperTest extends TestCase {
    private ViewableStatusHelper helper;
    private static final String PASSING_LOGFILE = DataUtils.PASSING_BUILD_LBUILD_0_XML;

    protected void setUp() throws Exception {
        helper = new ViewableStatusHelper();
    }

    public void testShouldReturnInactiveWhenPreviousStatusIsUnknownAndCurrentStatusIsNotBuilding()
            throws Exception {
        BuildSummary summary = new BuildSummary("project1", PreviousResult.UNKNOWN, PASSING_LOGFILE);
        summary.updateStatus(CurrentStatus.WAITING.getCruiseStatus());
        assertEquals("inactive", helper.getVmStatus(summary));
    }

    public void testShouldReturnBuildingWhenPreviousStatusIsUnknownAndCurrentStatusIsNotBuilding()
            throws Exception {
        BuildSummary summary = new BuildSummary("project1", PreviousResult.UNKNOWN, PASSING_LOGFILE);
        summary.updateStatus(CurrentStatus.BUILDING.getCruiseStatus());
        assertEquals("building", helper.getVmStatus(summary));
    }

    public void testShouldReturnDiscontinuedWhenCurrentStatusIsDiscontinued() throws Exception {
        BuildSummary summary = new BuildSummary("project1", PreviousResult.FAILED, PASSING_LOGFILE);
        summary.updateStatus(CurrentStatus.DISCONTINUED.getCruiseStatus());
        assertEquals("discontinued", helper.getVmStatus(summary));
    }
}
