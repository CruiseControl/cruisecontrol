package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

public class DownloadTest extends SeleniumTestCase {
    public void testShouldNavigateInTreeStructureAndDonwload() throws Exception {
        selenium.open("/dashboard/build/detail/project1/" + DataUtils.PASSING_BUILD_LBUILD_0_XML);
        assertFalse(selenium.isTextPresent("artifact2.txt"));
        assertFalse(selenium.isTextPresent("index.html"));
        clickLinkWithTextAndWait("subdir", 500);
        assertTrue(selenium.isTextPresent("artifact2.txt"));
        assertTrue(selenium.isTextPresent("index.html"));
        assertFalse(selenium.isTextPresent("artifact3.txt"));
        clickLinkWithTextAndWait("third_level", 500);
        assertTrue(selenium.isTextPresent("artifact3.txt"));
    }

    public void testShouldSupportHtmlLinks() throws Exception {
        selenium.open("/dashboard/build/detail/project1/" + DataUtils.PASSING_BUILD_LBUILD_0_XML);
        clickLinkWithTextAndWait("subdir", 500);
        clickLinkWithTextAndWait("index.html", 500);
        clickLinkWithTextAndWait("Click to open content", 500);
        assertTrue(selenium.isTextPresent("It is the content html"));
    }
}
