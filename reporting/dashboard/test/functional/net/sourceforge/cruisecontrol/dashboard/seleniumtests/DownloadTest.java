package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

public class DownloadTest extends SeleniumTestCase {
    public void testShouldNavigateInTreeStructureAndDonwload() throws Exception {
        openBuildDetailPage("project1", CCDateFormatter.getBuildDateFromLogFileName(
                DataUtils.PASSING_BUILD_LBUILD_0_XML));

        textShouldNOTPresent("artifact2.txt");
        textShouldNOTPresent("index.html");
        clickLinkWithTextAndWait("subdir");
        textShouldPresent("artifact2.txt");
        textShouldPresent("index.html");

        textShouldNOTPresent("artifact3.txt");
        clickLinkWithTextAndWait("third_level");
        textShouldPresent("artifact3.txt");
    }

    public void testShouldSupportHtmlLinks() throws Exception {
        openBuildDetailPage("project1", CCDateFormatter.getBuildDateFromLogFileName(
                DataUtils.PASSING_BUILD_LBUILD_0_XML));
        clickLinkWithTextAndWait("subdir");
        clickLinkWithTextAndWait("index.html");
        clickLinkWithTextAndWait("Click to open content");
        textShouldPresent("It is the content html");
    }
}
