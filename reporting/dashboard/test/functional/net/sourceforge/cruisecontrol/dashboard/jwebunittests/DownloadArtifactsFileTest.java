package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import java.io.File;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.apache.commons.lang.StringUtils;

public class DownloadArtifactsFileTest extends BaseFunctionalTest {
    private File downloadedFile;

    protected void onSetUp() throws Exception {
        setConfigFileAndSubmitForm(DataUtils.getConfigXmlOfWebApp().getPath());
        downloadedFile = DataUtils.createTempFile("downloadedArtifact", "txt");
    }

    public void testShouldShowDownloadableArtifacts() throws Exception {
        tester.beginAt("/build/detail/project1/" + DataUtils.PASSING_BUILD_LBUILD_0_XML);
        tester.assertTextPresent("Artifacts");
        tester.assertLinkPresentWithText("artifact1.txt");
        tester.assertLinkPresentWithText("coverage.xml");
        tester.assertLinkPresentWithText("subdir");
        tester.assertTextNotPresent("Targets");
        tester.assertTextPresent("Test Suites");
    }

    public void testShouldDownloadArtifactsFrom() throws Exception {
        tester.beginAt("/build/detail/project1/" + DataUtils.PASSING_BUILD_LBUILD_0_XML);
        tester.assertTextPresent("Merged Check Style");
        downloadAndAssert("artifact1.txt", "artifact content");
    }

    private void downloadAndAssert(String text, String contentToAssert) throws Exception {
        tester.clickLinkWithText(text);
        String serveurResponse = tester.getServeurResponse();
        assertTrue(StringUtils.contains(serveurResponse, "text/plain"));
        tester.saveAs(downloadedFile);
        String content = DataUtils.readFileContent(downloadedFile);
        assertTrue(StringUtils.contains(content, contentToAssert));
    }
}
