package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import org.apache.commons.lang.StringUtils;

public class LatestBuildTest extends SeleniumTestCase {
    private static final String PAGE_LOAD_WAIT_DURATION = "" + (8 * AJAX_DURATION * 1000);

    protected void doSetUp() throws Exception {
        willChangeConfigFile();
    }

    public void testShouldBeAbleToOpenDashboardPage() throws Exception {
        openDashboardPage();
        assertEquals("CruiseControl - Dashboard", this.user.getTitle());
        textShouldPresent(" project build(s) succeed");
    }

    public void testShouldBeAbleToBuildsPage() throws Exception {
        openBuildsPage();
        assertEquals("CruiseControl - Builds", this.user.getTitle());
        textShouldPresent(" project build(s) succeed");
    }

    
    public void testShouldDisplayDashboardAsPaused() throws Exception {
        openDashboardPage();
        this.hasClassName("tooltip_paused", "paused");
    }

    public void testShouldDisplayBuildsAsPaused() throws Exception {
        openBuildsPage();
        this.hasClassName("paused_profile", "paused");
    }

    public void testShouldNavigateToBuildDetailPageWhenClickTheBar() throws Exception {
        openDashboardPage();
        assertTrue(this.user.isElementPresent("project1_bar_link"));
        this.clickAndWait("project1_bar_link");
        assertTrue(StringUtils.contains(user.getTitle(), "Project project1 - Build"));
    }

    public void testShouldReloadPageWhenNewProjectAddedToConfigFile() throws Exception {
        openDashboardPage();
        elementShouldNotBePresent("missing_project_container");
        addProjectToConfigFile("missing");
        user.waitForPageToLoad(PAGE_LOAD_WAIT_DURATION);
        elementShouldPresent("missing_project_container");
    }

}
