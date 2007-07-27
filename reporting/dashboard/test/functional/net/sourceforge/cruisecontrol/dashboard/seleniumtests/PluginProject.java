package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

public class PluginProject extends SeleniumTestCase {
    public void testShouldBeAbleToAddNewProjectToConfigXml() throws Exception {
        selenium.open("/dashboard/dashboard");
        click("cclive_build_detail");
        assertTrue(selenium.isTextPresent("cclive Failed"));
        selenium.open("/dashboard/dashboard");
        click("cclive_all_successful_builds");
        assertTrue(selenium.isTextPresent("cclive"));
        selenium.open("/dashboard/dashboard");
        click("cclive_all_builds");
        assertTrue(selenium.isTextPresent("cclive"));
    }

    private void click(String link) throws Exception {
        selenium.click(link);
        Thread.sleep(500);
    }
}
