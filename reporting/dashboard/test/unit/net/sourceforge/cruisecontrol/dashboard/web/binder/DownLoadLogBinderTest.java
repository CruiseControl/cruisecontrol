package net.sourceforge.cruisecontrol.dashboard.web.binder;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.web.command.DownloadLogCommand;

import org.springframework.mock.web.MockHttpServletRequest;

public class DownLoadLogBinderTest extends TestCase {
    public void testShouldParseURLAndInitFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/project/download/log/project1/log.txt");
        DownloadLogCommand command = new DownloadLogCommand(null);
        DownLoadLogBinder binder = new DownLoadLogBinder(command);
        binder.bind(request);
        assertEquals("project1", command.getProjectName());
        assertEquals("log.txt", command.getLogFile());
    }
}
