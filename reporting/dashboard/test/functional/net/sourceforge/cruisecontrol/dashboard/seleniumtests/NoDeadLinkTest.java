package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;

public class NoDeadLinkTest extends SeleniumTestCase {
    public void testShouldNotDisplay404WhenClickTheLink() throws Exception {
        clickTabs();
        clickAddProject();
        clickRssFeed();
        clickCCTray();
        clickControlPanel();
        clickBuildProfile();
        clickBuildBar();
        clickAllBuilds();
        clickAllPassedBuilds();
    }

    private void clickBuildProfile() throws Exception {
        selenium.open("/dashboard/dashboard?s=1");
        clickAssert("project1_build_detail");
        clickAssert("project1_all_successful_builds");
        selenium.open("/dashboard/build/detail/project1");
        clickAssert("project1_all_builds");
    }

    private void clickAllBuilds() throws Exception {
        selenium.open("/dashboard/dashboard?s=1");
        clickAssert("project1_all_builds");
    }

    private void clickAllPassedBuilds() throws Exception {
        selenium.open("/dashboard/dashboard?s=1");
        clickAssert("project1_all_successful_builds");
    }

    private void clickBuildBar() throws Exception {
        selenium.open("/dashboard/dashboard");
        clickAssert("//div[@id='project1_bar']/div/a");
    }

    private void clickAddProject() throws Exception {
        selenium.open("/dashboard/dashboard");
        clickAssert("//div[@id='add_project']/a");
        clickAssert("//form[@id='addProject']/p/a");
    }

    private void clickRssFeed() throws Exception {
        //TODO Selenium throw exception when open RSS feed
        //selenium.open("/dashboard/dashboard");
        //clickLinkWithIdAndWait("//div[@id='rss_feed_for_all']/a");

    }

    private void clickCCTray() throws Exception {
        //TODO how to assert?
        //selenium.open("/dashboard/dashboard");
        //clickLinkWithIdAndWait("//div[@id='cctray']/a");
        // Failed to assert automatically, coz it is xml
    }

    private void clickControlPanel() throws Exception {
        selenium.open("/dashboard/dashboard");
        clickLinkWithIdAndWait("//div[@id='configure_panel']/a");
        String hostName = InetAddress.getLocalHost().getCanonicalHostName();
        String exptectSrc = "http://" + hostName + ":8000/";
        String source = selenium.getHtmlSource();
        assertTrue(source, StringUtils.contains(source, exptectSrc));
    }

    private void clickTabs() throws Exception {
        clickLinkWithIdAndWait("//div[@id='location_message']/a");
        clickLinkWithIdAndWait("//li[@id='administation']/a");
        clickLinkWithIdAndWait("//li[@id='builds']/a");
        clickLinkWithIdAndWait("//li[@id='administation']/a");
        clickLinkWithIdAndWait("//li[@id='dashboard']/a");
    }

    private void clickAssert(String link) throws Exception {
        clickLinkWithIdAndWait(link);
        assertFalse(selenium.isTextPresent("ERROR: 404"));
    }
}
