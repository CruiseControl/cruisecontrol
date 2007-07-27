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

    public void testShouldDownloadArtifactsFromPassedBuild() throws Exception {
        tester.beginAt("/build/detail/project1/" + DataUtils.PASSING_BUILD_LBUILD_0_XML);
        tester.assertTextPresent("Merged Check Style");
        downloadAndAssert("artifact1.txt", "artifact content", "text/plain");
    }

    public void testShouldDownloadArtifactsFromFailedBuild() throws Exception {
        tester.beginAt("/build/detail/project1/" + DataUtils.FAILING_BUILD_XML);
        tester.assertTextPresent("Merged Check Style");
        downloadAndAssert("coverage2214.xml", "<report>", "application/xml");
        tester.beginAt("/build/detail/project1/" + DataUtils.FAILING_BUILD_XML);
        downloadAndAssert("artifact2214.txt", "artifact2214.txt content", "text/plain");
    }
    
    private void downloadAndAssert(String text, String contentToAssert, String contentType) throws Exception {
        tester.clickLinkWithText(text);
        String serveurResponse = tester.getServeurResponse();
        assertTrue(serveurResponse, StringUtils.contains(serveurResponse, contentType));
        tester.saveAs(downloadedFile);
        String content = DataUtils.readFileContent(downloadedFile);
        assertTrue(content, StringUtils.contains(content, contentToAssert));
    }
}
