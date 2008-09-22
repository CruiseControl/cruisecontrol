package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

public class NoDeadLinkTest extends SeleniumTestCase {
    public void testShouldNotDisplay404WhenClickTheLink() throws Exception {
        clickTabs();
        clickBuildProfile();
        clickBuildBar();
        clickAllBuilds();
        clickAllPassedBuilds();
    }

    private void clickBuildProfile() throws Exception {
        openBuildsPage();
        shouldNOTBeHttp4xx("project1_build_detail");
        openBuildsPage();
        shouldNOTBeHttp4xx("project1_all_successful_builds");
        openBuildsPage();
        shouldNOTBeHttp4xx("project1_all_builds");
    }

    private void clickAllBuilds() throws Exception {
        openBuildsPage();
        shouldNOTBeHttp4xx("project1_all_builds");
    }

    private void clickAllPassedBuilds() throws Exception {
        openBuildsPage();
        shouldNOTBeHttp4xx("project1_all_successful_builds");
    }

    private void clickBuildBar() throws Exception {
        openDashboardPage();
        shouldNOTBeHttp4xx("//a[@id='project2_bar_link']");
    }

    private void clickTabs() throws Exception {
        openBuildsPage();
        clickAndWait("//li[@id='administation']/a");
        clickAndWait("//li[@id='builds']/a");
        clickAndWait("//li[@id='administation']/a");
        clickAndWait("//li[@id='dashboard']/a");
    }

    private void shouldNOTBeHttp4xx(String link) throws Exception {
        clickAndWait(link);
        textShouldNOTPresent("ERROR: 4");
    }
}
