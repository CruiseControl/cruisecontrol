package net.sourceforge.cruisecontrol.dashboard.web.view;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;

public abstract class BaseFileView implements View, ServletContextAware {

    private ServletContext servletContext;

    protected String getUrl(HttpServletRequest httpServletRequest, String fileName) {
        // TODO is it URI?
        return httpServletRequest.getContextPath() + httpServletRequest.getServletPath()
                + httpServletRequest.getPathInfo() + '/' + fileName;
    }

    protected ServletContext getServletContext() {
        return this.servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
