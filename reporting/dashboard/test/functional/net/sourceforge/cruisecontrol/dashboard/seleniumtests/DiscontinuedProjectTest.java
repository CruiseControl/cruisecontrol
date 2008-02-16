package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import org.apache.commons.lang.StringUtils;

public class DiscontinuedProjectTest extends SeleniumTestCase {
    protected void doSetUp() throws Exception {
        willChangeConfigFile();
    }

    public void testShouldLoadProjectAsDiscontinuedInDashboard() throws Exception {
        this.openDashboardPage();
        shouldBeDiscontinuedInTooltip();
        shouldBeDiscontinuedInBar();
        shouldHaveDiscontinuedInStatistics();
        shouldBeAbleToNavigateToBuildDetailFromTooltip();
        shouldBeAbleToNavigateToBuildDetailFromBar();
    }

    public void testShouldLoadProjectAsDiscontinuedInBuilds() throws Exception {
        this.openBuildsPage();
        shouldBeDiscontinuedInProfile();
        shouldNotHaveForceBuildInProfile();
        shouldHaveDiscontinuedInStatistics();
        shouldBeAbleToNavigateToAllPassedBuildsFromLink();
        shouldBeAbleToNavigateToAllBuildsFromLink();
        shouldBeAbleToNavigateToBuildDetailFromLink();
    }

    public void testShouldHavePreviousResultAndDiscontinuedAsCurrentStatusInBuildDetailPage()
            throws Exception {
        this.openBuildDetailPageDirectly("projectWithoutConfiguration");
        shouldHavePreviousResultAndDiscontinuedAsCurrentStatusBuildDetailHeader();
        shouldNotHaveForceBuildInProfile();
    }

    public void testAjaxShouldSwtichProjectStatusInDashboard() throws Exception {
        this.openDashboardPage();
        changeProjectToBeNormal();
        shouldNotBeDiscontinuedInTooltip();
        shouldNotBeDiscontinuedInBar();
        shouldNotHaveDiscontinuedInStatistics();

        changeProjectToBeDiscontinued();
        shouldBeDiscontinuedInTooltip();
        shouldBeDiscontinuedInBar();
        shouldHaveDiscontinuedInStatistics();
        shouldBeAbleToNavigateToBuildDetailFromTooltip();
        shouldBeAbleToNavigateToBuildDetailFromBar();
    }

    public void testAjaxShouldSwitchProjectStatusInBuilds() throws Exception {
        this.openBuildsPage();

        changeProjectToBeNormal();
        shouldNotBeDiscontinuedInProfile();
        shouldHaveForceBuildInProfile();
        shouldNotHaveDiscontinuedInStatistics();

        changeProjectToBeDiscontinued();
        shouldBeDiscontinuedInProfile();
        shouldNotHaveForceBuildInProfile();
        shouldHaveDiscontinuedInStatistics();
        shouldBeAbleToNavigateToAllPassedBuildsFromLink();
        shouldBeAbleToNavigateToAllBuildsFromLink();
        shouldBeAbleToNavigateToBuildDetailFromLink();
    }

    private void shouldBeAbleToNavigateToBuildDetailFromTooltip() throws Exception {
        assertLinkClickable("projectWithoutConfiguration_bar_link");
    }

    private void shouldBeAbleToNavigateToBuildDetailFromBar() throws Exception {
        assertLinkClickable("projectWithoutConfiguration_tooltip_link");
    }

    private void shouldBeAbleToNavigateToAllPassedBuildsFromLink() throws Exception {
        assertLinkClickable("projectWithoutConfiguration_all_successful_builds");
    }

    private void shouldBeAbleToNavigateToAllBuildsFromLink() throws Exception {
        assertLinkClickable("projectWithoutConfiguration_all_builds");
    }

    private void shouldBeAbleToNavigateToBuildDetailFromLink() throws Exception {
        assertLinkClickable("projectWithoutConfiguration_build_detail");
    }

    private void changeProjectToBeNormal() throws Exception {
        String before = user.getText("statistics_discontinued");
        addProjectToConfigFile("projectWithoutConfiguration");
        waitUntilStatisticsChange(before, "discontinued", 4 * AJAX_DURATION);
    }

    private void changeProjectToBeDiscontinued() throws Exception {
        String before = user.getText("statistics_discontinued");
        rollbackConfigFile();
        waitUntilStatisticsChange(before, "discontinued", 4 * AJAX_DURATION);
    }

    private void shouldNotBeDiscontinuedInProfile() throws Exception {
        this.hasClassName("projectWithoutConfiguration_profile", "failed");
        this.hasNoClassName("projectWithoutConfiguration_profile", "discontinued");
    }

    private void shouldNotHaveDiscontinuedInStatistics() {
        user.isTextPresent("1 project(s) discontinued");
    }

    private void shouldNotBeDiscontinuedInBar() throws Exception {
        this.hasClassName("projectWithoutConfiguration_bar", "failed");
        this.hasNoClassName("projectWithoutConfiguration_bar", "discontinued");
    }

    private void shouldNotBeDiscontinuedInTooltip() throws Exception {
        this.hasClassName("tooltip_projectWithoutConfiguration", "failed");
        this.hasNoClassName("tooltip_projectWithoutConfiguration", "discontinued");
    }

    private void shouldBeDiscontinuedInTooltip() throws Exception {
        this.hasClassName("tooltip_projectWithoutConfiguration", "discontinued");
        this.hasClassName("tooltip_projectWithoutConfiguration", "failed");
    }

    private void shouldBeDiscontinuedInBar() throws Exception {
        this.hasClassName("projectWithoutConfiguration_bar", "discontinued");
        this.hasClassName("projectWithoutConfiguration_bar", "failed");
    }

    private void shouldHaveDiscontinuedInStatistics() {
        assertTrue(user.isTextPresent("2 project(s) discontinued"));
    }

    private void shouldBeDiscontinuedInProfile() throws Exception {
        this.hasClassName("projectWithoutConfiguration_profile", "discontinued");
        this.hasClassName("projectWithoutConfiguration_profile", "failed");
    }

    private void shouldNotHaveForceBuildInProfile() throws Exception {
        this.hasClassName("projectWithoutConfiguration_forcebuild", "force_build_disabled");
    }

    private void shouldHaveForceBuildInProfile() throws Exception {
        this.hasClassName("projectWithoutConfiguration_forcebuild", "force_build_enabled");
    }

    private void shouldHavePreviousResultAndDiscontinuedAsCurrentStatusBuildDetailHeader() throws Exception {
        this.hasClassName("build_detail_summary_container", "discontinued");
    }

    private void assertLinkClickable(String id) throws Exception {
        String href = getAttribute(id, "href");
        href = StringUtils.defaultString(href);
        assertFalse(StringUtils.equals("javascript:void(0)", href) || StringUtils.isBlank(href));
    }
}
