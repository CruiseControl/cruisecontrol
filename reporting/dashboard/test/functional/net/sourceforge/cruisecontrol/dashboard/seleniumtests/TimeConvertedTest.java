package net.sourceforge.cruisecontrol.dashboard.seleniumtests;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class TimeConvertedTest extends SeleniumTestCase {

    private static final String _4_DAYS_AGO = "4 days ago";
    private File file;

    public void doSetUp() throws Exception {
        if (file != null && file.exists()) {
            file.delete();
        }
        File projectCCLive = new File(DataUtils.getLogRootOfWebapp(), "cclive");
        DateTime fourDaysAgo = new DateTime().minusDays(4);
        String fourDaysAgoText = CCDateFormatter.yyyyMMddHHmmss(fourDaysAgo);
        file = new File(projectCCLive, "log" + fourDaysAgoText + "Lbuild.510.xml");
        file.createNewFile();

        initContentForSampleFile(fourDaysAgo, fourDaysAgoText);
    }

    private void initContentForSampleFile(DateTime fourDaysAgo, String fourDaysAgoText) throws Exception, IOException {
        File project1 = new File(DataUtils.getLogRootOfWebapp(), "project1");
        String content = FileUtils.readFileToString(new File(project1, "log20051209122103Lbuild.489.xml"), "UTF-8");
        String replacedContent = StringUtils.replace(content, "project1", "cclive");
        replacedContent = StringUtils.replace(replacedContent, "20051209122103", fourDaysAgoText);
        replacedContent = StringUtils.replace(replacedContent, "12/09/2005 12:21:03", new SimpleDateFormat(
                "d/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(fourDaysAgo.toDate()));
        FileUtils.writeStringToFile(file, replacedContent);
    }

    public void doTearDown() {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void testShouldDisplayProperConvertedTime() throws Exception {
        openDashboardPage();
        shouldGiveProperTimeInDashboardToolTip();
        shouldGiveProperTimeInBuilds();
        shouldGiveProperTimeInBuildDetail();
        shouldGiveProperTimeInHistoryBuildsInBuildDetail();
        shouldGiveProperTimeInAllBuilds();
        shouldGiveProperTimeInAllSuccessfulBuilds();
    }

    private void shouldGiveProperTimeInDashboardToolTip() throws Exception {
        assertEquals(_4_DAYS_AGO, user.getText("tooltip_cclive_date"));
    }

    private void shouldGiveProperTimeInBuilds() {
        openBuildsPage();
        assertEquals(_4_DAYS_AGO, user.getText("cclive_build_date"));
    }

    private void shouldGiveProperTimeInBuildDetail() throws Exception {
        openBuildDetailPageDirectly("cclive");
        assertTrue(user.isTextPresent("cclive passed (" + _4_DAYS_AGO + ")"));
    }

    private void shouldGiveProperTimeInHistoryBuildsInBuildDetail() {
        assertTrue(user.isTextPresent(_4_DAYS_AGO + " build.510"));
    }

    private void shouldGiveProperTimeInAllBuilds() {
        openAllBuilsPage("cclive");
        assertTrue(user.isTextPresent(_4_DAYS_AGO));
    }

    private void shouldGiveProperTimeInAllSuccessfulBuilds() {
        openAllBuilsPage("cclive");
        assertTrue(user.isTextPresent(_4_DAYS_AGO));
    }
}
