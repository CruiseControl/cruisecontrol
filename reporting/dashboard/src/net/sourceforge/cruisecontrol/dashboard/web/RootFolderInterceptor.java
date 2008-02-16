package net.sourceforge.cruisecontrol.dashboard.web;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;

import java.util.Map;
import java.io.File;

public class RootFolderInterceptor implements HandlerInterceptor {
    private ConfigurationService service;

    public RootFolderInterceptor(ConfigurationService service) {
        this.service = service;
    }
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o)
            throws Exception {
        return true;
    }

    public void postHandle(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o,
            ModelAndView modelAndView) throws Exception {
        Map data = modelAndView.getModel();
        data.put("logRoot", service.getLogsRoot().getAbsolutePath() + File.separatorChar);
    }

    public void afterCompletion(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e)
            throws Exception {

    }
}
