package net.sourceforge.cruisecontrol.dashboard.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ForceBuildEnabledInterceptor implements HandlerInterceptor {
    private final ConfigurationService configuration;

    public ForceBuildEnabledInterceptor(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object command,
            Exception mov) throws Exception {
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object command,
            ModelAndView mov) throws Exception {
        mov.getModel().put("forceBuildEnabled", Boolean.valueOf(configuration.isForceBuildEnabled()));
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object command)
            throws Exception {
        return true;
    }
}
