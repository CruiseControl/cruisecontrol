package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import org.apache.commons.lang.StringUtils;

public class SingleQuoteTest extends BaseFunctionalTest {

    public void testShouldEscapeSingleQuoteInProjectNameInDashboardPage() throws Exception {
        tester.beginAt("/tab/dashboard");
        String page = tester.getPageSource();
        assertTrue(StringUtils.contains(page, "new Tooltip('\\'_bar', 'tooltip_\\'')"));

        tester.beginAt("/forcebuild.ajax?projectName='");
        getJSONWithAjaxInvocation("getProjectBuildStatus.ajax");
        tester.beginAt("/tab/build/detail/'");
        page = tester.getPageSource();
        assertTrue(page, StringUtils.contains(page,"new BuildDetailObserver('\\'')"));
    }

    public void testShouldEscapeSingleQuoteInProjectNameInBuildsPage() throws Exception {
        tester.beginAt("/tab/builds");
        String page = tester.getPageSource();
        assertTrue(StringUtils.contains(page, "new Toolkit().hide('toolkit_\\'"));
    }
}
