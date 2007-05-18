package net.sourceforge.cruisecontrol.dashboard.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class PageContentTypeInterceptor implements HandlerInterceptor {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object obj)
            throws Exception {
        // some app servers don't handle our web 2.0 style urls too well and default to
        // application/octet-stream for the pages. we set it here so that it can be
        // overwritten in the controllers.
        response.setContentType("text/html");
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object obj,
            ModelAndView arg3) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object arg2,
            Exception exception) throws Exception {
    }

}
