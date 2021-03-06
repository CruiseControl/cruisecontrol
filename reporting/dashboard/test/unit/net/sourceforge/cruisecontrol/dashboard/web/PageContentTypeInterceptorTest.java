package net.sourceforge.cruisecontrol.dashboard.web;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class PageContentTypeInterceptorTest extends TestCase {

    public void testShouldSetUpReponseContentTypeByDefault() throws Exception {
        PageContentTypeInterceptor interceptor = new PageContentTypeInterceptor();
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(new MockHttpServletRequest(), response, null);
        assertTrue(result);
        assertEquals("text/html", response.getContentType());
    }
}
