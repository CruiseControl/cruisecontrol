package net.sourceforge.cruisecontrol.dashboard.seleniumtests;


public class TooltipTest extends SeleniumTestCase {

    public void testShouldDisplayCurrentStatusAndLastStatusOnTooltip() throws Exception {
        openDashboardPage();
        shouldDisplayCurrentStatusAndPreviusStatusForProject("cclive", "Failed", "Waiting");
        shouldDisplayCurrentStatusAndPreviusStatusForProject("paused", "Failed", "Paused");
        shouldDisplayCurrentStatusAndPreviusStatusForProject("queuedPassed", "Passed", "Queued");
        openBuildsPage();
        waitUntilStartBuilding();
        openDashboardPage();
        shouldDisplayCurrentStatusAndPreviusStatusForProject("cclive", "Failed", "Building");
    }

    private void waitUntilStartBuilding() throws Exception {
        String before = user.getText("statistics_building");
        forceBuildByClick("cclive");
        waitUntilStatisticsChange(before, "building", 4 * AJAX_DURATION);
    }

    public void testTooltipBehaviour() throws Exception {
        openDashboardPage();
        shouldAlwaysShowTooptipWhenUserDoesNotMouseOut();
        shouldAlwaysShowTooltipWhenUserMouseOverTooltip();
        shouldImmidiatelyCloseOriginalTooltipWhenUserMouseOvernotherProject();
    }

    private void shouldAlwaysShowTooptipWhenUserDoesNotMouseOut() throws Exception {
        this.user.mouseOver("cclive_bar");
        assertTrue(this.user.isVisible("tooltip_cclive"));
        Thread.sleep(3000);
        assertTrue(this.user.isVisible("tooltip_cclive"));
    }

    private void shouldAlwaysShowTooltipWhenUserMouseOverTooltip() throws Exception {
        this.user.mouseOver("tooltip_cclive");
        assertTrue(this.user.isVisible("tooltip_cclive"));
        Thread.sleep(3000);
        assertTrue(this.user.isVisible("tooltip_cclive"));
    }

    private void shouldImmidiatelyCloseOriginalTooltipWhenUserMouseOvernotherProject() {
        this.user.mouseOver("paused_bar");
        assertTrue(this.user.isVisible("tooltip_paused"));
        assertFalse(this.user.isVisible("tooltip_cclive"));
    }

    private void shouldDisplayCurrentStatusAndPreviusStatusForProject(String projectName, String previous,
            String current) {
        this.user.mouseOver(projectName + "_bar");
        assertTrue(this.user.isVisible("tooltip_" + projectName));
        assertNotNull(this.user.getText("tooltip_" + projectName + "_server_name"));
        assertEquals(previous, this.user.getText("tooltip_" + projectName + "_previous_result"));
        assertEquals(current, this.user.getText("tooltip_" + projectName + "_current_status"));
    }
}
