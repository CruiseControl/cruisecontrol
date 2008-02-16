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

public class ForceBuildEnabledInterceptorTest extends MockObjectTestCase {

    public void testShouldInvokeEnvironmentServiceAndPutInIntoModel() throws Exception {
        Mock configurationMock =
            mock(ConfigurationService.class, new Class[] {EnvironmentService.class,
                DashboardXmlConfigService.class, BuildLoopQueryService.class}, new Object[] {null,
                null, null});
        ForceBuildEnabledInterceptor interceptor =
                new ForceBuildEnabledInterceptor((ConfigurationService) configurationMock.proxy());
        ModelAndView mov = new ModelAndView();
        configurationMock.expects(once()).method("isForceBuildEnabled").will(returnValue(true));
        interceptor.postHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null, mov);
        assertEquals(Boolean.TRUE, mov.getModel().get("forceBuildEnabled"));
    }
}
