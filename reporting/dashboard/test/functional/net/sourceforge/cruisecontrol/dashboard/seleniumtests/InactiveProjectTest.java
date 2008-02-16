package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class InactiveProjectTest extends SeleniumTestCase {
    private File logFile;

    private File ccLive2Folder;

    protected void doSetUp() throws Exception {
        ccLive2Folder = new File(DataUtils.getLogRootOfWebapp(), "cc-live-2");
        logFile =
                new File(ccLive2Folder, "log" + CCDateFormatter.yyyyMMddHHmmss(new DateTime())
                        + "Lbuild.510.xml");
    }

    protected void doTearDown() throws Exception {
        if (ccLive2Folder != null && ccLive2Folder.exists()) {
            FileUtils.deleteDirectory(ccLive2Folder);
        }
    }

    public void testShouldBeInactiveInDashboard() throws Exception {
        this.openDashboardPage();
        shouldBeInactiveInTooltip();
        shouldBeInactiveInBar();
        shouldNotBeAbleToNavigateToBuildDetailFromTooltip();
        shouldNotBeAbleToNavigateToBuildDetailFromBar();
    }

    public void testShouldBeInactiveInBuilds() throws Exception {
        this.openBuildsPage();
        shouldBeInactiveInProfile();
        forceBuildShouldBeActive();
        configPanelShouldBeActive();
        shouldNotBeAbleToNavigateToAllPassedBuildsFromLink();
        shouldNotBeAbleToNavigateToAllBuildsFromLink();
        shouldNotBeAbleToNavigateToBuildDetailFromLink();
    }
    public void testShouldChangeToActiveWhenLogAvailableInDashboard() throws Exception {
        this.openDashboardPage();
        createLogFileAndWaitUntilDashboardPickUp();
        shouldBePassedInTooltip();
        shouldBePassedInBar();
        shouldBeAbleToNavigateToBuildDetailFromTooltip();
        shouldBeAbleToNavigateToBuildDetailFromBar();

        removeLogFileAndWaitUntilDashboardPickUp();
        shouldBeInactiveInTooltip();
        shouldBeInactiveInBar();
        shouldNotBeAbleToNavigateToBuildDetailFromTooltip();
        shouldNotBeAbleToNavigateToBuildDetailFromBar();
    }

    public void testShouldChangeToActiveWhenLogAvailableInNBuilds() throws Exception {
        this.openBuildsPage();
        createLogFileAndWaitUntilDashboardPickUp();
        shouldBePassedInProfile();
        shouldBeAbleToNavigateToAllPassedBuildsFromLink();
        shouldBeAbleToNavigateToAllBuildsFromLink();
        shouldBeAbleToNavigateToBuildDetailFromLink();

        removeLogFileAndWaitUntilDashboardPickUp();
        shouldBeInactiveInProfile();
        shouldNotBeAbleToNavigateToAllPassedBuildsFromLink();
        shouldNotBeAbleToNavigateToAllBuildsFromLink();
        shouldNotBeAbleToNavigateToBuildDetailFromLink();
    }

    public void testShouldNotHaveLongerThan() throws Exception {
        this.openBuildsPage();
        String before = user.getText("statistics_building");
        forceBuildByClick("cc-live-2");
        textShouldAppearInCertainTime("Elapsed", 10 * AJAX_DURATION);
        shouldNotDisplayLongerThan();
        shouldNotDisplayUnknown();
        textShouldDisappearInCertainTime("Elapsed", 10 * AJAX_DURATION);
    }

    private void shouldNotDisplayUnknown() {
        assertFalse(this.user.isTextPresent("Unknown waiting"));
        assertFalse(this.user.isTextPresent("unknown waiting"));
    }

    private void shouldNotDisplayLongerThan() {
        assertFalse(this.user.isTextPresent("Remaining"));
        assertFalse(this.user.isTextPresent("Longer by"));
    }

    protected void createLogFileAndWaitUntilDashboardPickUp() throws Exception {
        String before = user.getText("statistics_inactive");
        ccLive2Folder.mkdir();
        logFile.createNewFile();
        waitUntilStatisticsChange(before, "inactive", 4 * AJAX_DURATION);
    }

    protected void removeLogFileAndWaitUntilDashboardPickUp() throws Exception {
        String before = user.getText("statistics_inactive");
        FileUtils.deleteDirectory(ccLive2Folder);
        waitUntilStatisticsChange(before, "inactive", 4 * AJAX_DURATION);
    }

    private void forceBuildShouldBeActive() throws Exception {
        hasClassName("cc-live-2_forcebuild", "force_build_enabled");
        assertTrue(isElementHasHandler("cc-live-2_forcebuild"));
    }

    private void configPanelShouldBeActive() throws Exception {
        hasClassName("cc-live-2_config_panel", "config_panel_enabled");
        this.user.click("cc-live-2_config_panel");
        elementShouldAppearInCertainTime("toolkit_cc-live-2", 6);
    }

    private void shouldNotBeAbleToNavigateToAllPassedBuildsFromLink() throws Exception {
        assertLinkUnclickable("cc-live-2_all_successful_builds");
    }

    private void shouldBeAbleToNavigateToAllPassedBuildsFromLink() throws Exception {
        assertLinkClickable("cc-live-2_all_successful_builds");
    }

    private void shouldNotBeAbleToNavigateToAllBuildsFromLink() throws Exception {
        assertLinkUnclickable("cc-live-2_all_builds");
    }

    private void shouldBeAbleToNavigateToAllBuildsFromLink() throws Exception {
        assertLinkClickable("cc-live-2_all_builds");
    }

    private void shouldNotBeAbleToNavigateToBuildDetailFromLink() throws Exception {
        assertLinkUnclickable("cc-live-2_build_detail");
    }

    private void shouldBeAbleToNavigateToBuildDetailFromLink() throws Exception {
        assertLinkClickable("cc-live-2_build_detail");
    }

    private void shouldNotBeAbleToNavigateToBuildDetailFromTooltip() throws Exception {
        assertLinkUnclickable("cc-live-2_tooltip_link");
    }

    private void shouldBeAbleToNavigateToBuildDetailFromTooltip() throws Exception {
        assertLinkClickable("cc-live-2_tooltip_link");
    }

    private void shouldNotBeAbleToNavigateToBuildDetailFromBar() throws Exception {
        assertLinkUnclickable("cc-live-2_bar_link");
    }

    private void shouldBeAbleToNavigateToBuildDetailFromBar() throws Exception {
        assertLinkClickable("cc-live-2_bar_link");
    }

    private void shouldBeInactiveInProfile() throws Exception {
        this.hasClassName("cc-live-2_profile", "inactive");
    }

    private void shouldBePassedInProfile() throws Exception {
        this.hasClassName("cc-live-2_profile", "passed");
        this.hasNoClassName("cc-live-2_profile", "inactive");
    }

    private void shouldBeInactiveInBar() throws Exception {
        this.hasClassName("cc-live-2_bar", "inactive");
    }

    private void shouldBePassedInBar() throws Exception {
        this.hasClassName("cc-live-2_bar", "passed");
        this.hasNoClassName("cc-live-2_bar", "inactive");
    }

    private void shouldBeInactiveInTooltip() throws Exception {
        this.hasClassName("tooltip_cc-live-2", "inactive");
    }

    private void shouldBePassedInTooltip() throws Exception {
        this.hasClassName("tooltip_cc-live-2", "passed");
        this.hasNoClassName("tooltip_cc-live-2", "inactive");
    }

    private void assertLinkClickable(String id) throws Exception {
        assertFalse(isLinkDisabled(id));
    }

    private void assertLinkUnclickable(String id) throws Exception {
        assertTrue(isLinkDisabled(id));
    }

    private boolean isLinkDisabled(String id) throws Exception {
        String href = getAttribute(id, "href");
        href = StringUtils.defaultString(href);
        boolean isDisabled = StringUtils.equals("javascript:void(0)", href) || StringUtils.isBlank(href);
        return isDisabled;
    }

    public boolean isElementHasHandler(String id) throws Exception {
        String handler = getAttribute(id, "onclick");
        handler = StringUtils.defaultString(handler);
        return StringUtils.equals("javascript:void(0)", handler) || StringUtils.isBlank(handler);
    }
}
