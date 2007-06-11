package net.sourceforge.cruisecontrol.dashboard.web.binder;

import org.springframework.mock.web.MockHttpServletRequest;

import net.sourceforge.cruisecontrol.dashboard.web.command.DownLoadArtifactsCommand;
import junit.framework.TestCase;

public class DownloadArtifactsBinderTest extends TestCase {
    public void testShouldParseURLAndInitFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/project/download/artifacts/project1/"
                + "log20051209122103Lbuild.489.xml/artifacts.txt");
        DownLoadArtifactsCommand command = new DownLoadArtifactsCommand(null);
        DownloadArtifactsBinder binder = new DownloadArtifactsBinder(command);
        binder.bind(request);
        assertEquals("project1", command.getProjectName());
        assertEquals("log20051209122103Lbuild.489.xml", command.getBuild());
        assertEquals("artifacts.txt", command.getFileToBeDownloaded());
    }

    public void testShouldParseURLAndResolveSubDir() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/project/download/artifacts/project1/"
                + "log20051209122103Lbuild.489.xml/subdest/artifacts.txt");
        DownLoadArtifactsCommand command = new DownLoadArtifactsCommand(null);
        DownloadArtifactsBinder binder = new DownloadArtifactsBinder(command);
        binder.bind(request);
        assertEquals("subdest/artifacts.txt", command.getFileToBeDownloaded());
    }
}
