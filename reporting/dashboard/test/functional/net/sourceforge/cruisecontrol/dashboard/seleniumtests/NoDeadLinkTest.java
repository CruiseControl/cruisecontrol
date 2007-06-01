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
        Thread.sleep(500);
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
        clickAssert("//div[@id='project1_bar']/a");
    }

    private void clickAddProject() throws Exception {
        selenium.open("/dashboard/dashboard");
        clickAssert("//div[@id='add_project']/a");
        clickAssert("//form[@id='addProject']//a");
    }

    private void clickRssFeed() throws Exception {
        selenium.open("/dashboard/dashboard");
        selenium.click("//div[@id='rss_feed_for_all']/a");
        Thread.sleep(500);
        // Failed to assert automatically, coz it is xml
    }

    private void clickCCTray() throws Exception {
        selenium.open("/dashboard/dashboard");
        selenium.click("//div[@id='cctray']/a");
        Thread.sleep(500);
        // Failed to assert automatically, coz it is xml
    }

    private void clickControlPanel() throws Exception {
        selenium.open("/dashboard/dashboard");
        clickAssert("//div[@id='configure_panel']/a");
        Thread.sleep(2000);
        String hostName = InetAddress.getLocalHost().getHostName();
        String exptectSrc = "http://" + hostName + ":8000/";
        String source = selenium.getHtmlSource();
        assertTrue(source, StringUtils.contains(source, exptectSrc));
    }

    private void clickTabs() throws Exception {
        clickAssert("//div[@id='location_message']/a");
        Thread.sleep(500);
        clickAssert("//li[@id='administation']/a");
        clickAssert("//li[@id='builds']/a");
        selenium.click("//li[@id='administation']/a");
        Thread.sleep(500);
        clickAssert("//li[@id='dashboard']/a");
    }

    private void clickAssert(String link) throws Exception {
        selenium.click(link);
        Thread.sleep(500);
        assertFalse(selenium.isTextPresent("ERROR: 404"));
    }
}
