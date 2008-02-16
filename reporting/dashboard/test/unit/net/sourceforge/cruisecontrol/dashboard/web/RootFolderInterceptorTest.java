package net.sourceforge.cruisecontrol.dashboard.web;

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;

public class RootFolderInterceptorTest extends MockObjectTestCase {
    private RootFolderInterceptor interceptor;

    private MockHttpServletResponse mockHttpServletResponse;
    private MockHttpServletRequest mockHttpServletRequest;
    private Mock mockConfiguration;

    protected void setUp() throws Exception {

        mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpServletRequest = new MockHttpServletRequest();
        mockConfiguration =
                mock(ConfigurationService.class, new Class[] {EnvironmentService.class,
                        DashboardXmlConfigService.class, BuildLoopQueryService.class}, new Object[] {null,
                        null, null});
        interceptor = new RootFolderInterceptor((ConfigurationService) mockConfiguration.proxy());
    }

    public void testShouldReturnTrueForPreHandling() throws Exception {
        assertTrue(interceptor.preHandle(null, null, null));
    }

    public void testReturnLogRootFolderPath() throws Exception {
        File file = new File("bbb");
        mockConfiguration.expects(once()).method("getLogsRoot").will(returnValue(file));
        ModelAndView modelAndView = new ModelAndView();
        interceptor.postHandle(mockHttpServletRequest, mockHttpServletResponse, null, modelAndView);
        assertEquals(file.getAbsolutePath() + File.separatorChar, modelAndView.getModel().get("logRoot"));
    }
}
