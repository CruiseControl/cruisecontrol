package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sourceforge.cruisecontrol.dashboard.service.TemplateRenderService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class DirectoryViewTest extends MockObjectTestCase {
    private MockHttpServletResponse mockResponse;

    private MockHttpServletRequest mockRequest;

    private BaseFileView view;

    private Mock mockServletContext;

    protected void setUp() throws Exception {
        super.setUp();
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        mockServletContext = mock(ServletContext.class);
        TemplateRenderService service = new TemplateRenderService();
        service.loadTemplates();
        view = new DirectoryView(service);
        view.setServletContext((ServletContext) mockServletContext.proxy());
    }

    public void testShouldReturnDivForSubFolder() throws Exception {
        mockRequest.setContextPath("/dashboard");
        mockRequest.setServletPath("/build");
        mockRequest.setPathInfo("/download/artifacts/project1/log20051209122103Lbuild.489.xml/dir");
        File dir = FilesystemUtils.createDirectory("dir");
        FilesystemUtils.createDirectory("subdir", dir.getName());
        Map model = new HashMap();
        model.put("targetFile", dir);
        view.render(model, mockRequest, mockResponse);
        String contentAsResponse = mockResponse.getContentAsString();
        assertTrue(contentAsResponse, StringUtils.contains(contentAsResponse, "/dir/subdir"));
        String html =
                "div id=\"_dashboard_build_download_artifacts_project1_log20051209122103Lbuild.489.xml_dir_subdir\"";
        assertTrue(contentAsResponse, StringUtils.contains(contentAsResponse, html));
    }

    public void testShouldDisplayFoldeContent() throws Exception {
        mockRequest.setContextPath("/dashboard");
        mockRequest.setServletPath("/build");
        mockRequest.setPathInfo("/download/artifacts/project1/log20051209122103Lbuild.489.xml/dir");
        File dir = DataUtils.createTempDirectory("dir");
        FilesystemUtils.createFile("file1.txt", dir);
        Map model = new HashMap();
        model.put("targetFile", dir);
        view.render(model, mockRequest, mockResponse);
        String contentAsResponse = mockResponse.getContentAsString();
        assertTrue(contentAsResponse, StringUtils.contains(contentAsResponse, "/dir/file1.txt"));
    }

    public void testShouldNotDisplayHiddenFile() throws Exception {
        mockRequest.setContextPath("/dashboard");
        mockRequest.setServletPath("/build");
        mockRequest.setPathInfo("/download/artifacts/project1/log20051209122103Lbuild.489.xml/dir");

        Mock mockDir = mock(File.class, new Class[] {String.class}, new String[] {"dir"});
        Mock mockFile = mock(File.class, new Class[] {String.class}, new String[] {"file"});
        mockDir.expects(once()).method("listFiles").will(
                returnValue(new File[] {(File) mockFile.proxy()}));
        mockFile.expects(once()).method("isHidden").will(returnValue(true));
        Map model = new HashMap();
        model.put("targetFile", mockDir.proxy());
        view.render(model, mockRequest, mockResponse);
        assertEquals("<ul style=\"margin-left:1em\"></ul>", mockResponse.getContentAsString());
    }

    public void testDefaultContentTypeShouldBeTextPlain() throws Exception {
        assertEquals("text/html", view.getContentType());
    }
}
