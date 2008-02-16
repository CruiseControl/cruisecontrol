package net.sourceforge.cruisecontrol.dashboard.web;

import junit.framework.TestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class BuildDetailExceptionControllerTest extends TestCase {
    private BuildDetailExceptionController controller;
    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;

    protected void setUp() throws Exception {
        controller = new BuildDetailExceptionController();
        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletResponse = new MockHttpServletResponse();
    }

    public void testShouldReturnErrorMessageForNoProjectCase() throws Exception {
        ModelAndView modelAndView = controller.noproject(mockHttpServletRequest, mockHttpServletResponse);
        assertEquals("No project specified.", modelAndView.getModel().get("errorMessage"));
    }

    public void testShouldReturnErrorMessageForProjectWithLog() throws Exception {
        mockHttpServletRequest.setAttribute("projectName", "projectX");
        mockHttpServletRequest.setAttribute("log", "20050809114091");
        ModelAndView modelAndView = controller.projectlog(mockHttpServletRequest, mockHttpServletResponse);
        assertEquals("The requested build log 20050809114091 does not exist in project projectX.",
                modelAndView.getModel().get("errorMessage"));
    }

    public void testShouldReturnErrorMessageForLogMissing() throws Exception {
        mockHttpServletRequest.setAttribute("projectName", "projectX");
        ModelAndView modelAndView = controller.project(mockHttpServletRequest, mockHttpServletResponse);
        assertEquals("The requested project projectX does not exist or does not have any logs.",
                modelAndView.getModel().get("errorMessage"));
    }
}
