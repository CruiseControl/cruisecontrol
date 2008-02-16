package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

public class ZippedLogTest extends BaseFunctionalTest {
    public void testShouldNotDisplayTheProjectNameAsLinkInListAllPassedPage() throws Exception {
        tester.beginAt("/project/list/passed/project3");
        tester.assertLinkPresentWithExactText(convertedTime("20061209122103") + " build.489");
    }

    public void testShouldNotDisplayTheProjectNameAsLinkInListAllPage() throws Exception {
        tester.beginAt("/project/list/all/project3");
        tester.assertLinkPresentWithExactText(convertedTime("20061209122103") + " build.489");
        tester.assertLinkPresentWithExactText(convertedTime("20061109122103"));
        tester.assertLinkPresentWithExactText(convertedTime("20051209122103"));
    }
}
