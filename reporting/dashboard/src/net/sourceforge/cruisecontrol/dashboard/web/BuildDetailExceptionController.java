package net.sourceforge.cruisecontrol.dashboard.web;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class BuildDetailExceptionController extends BaseMultiActionController {

    public ModelAndView project(HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse) throws Exception {
        String projectName = (String) httpServletRequest.getAttribute("projectName");
        String errorMessage = "The requested project " + projectName + " does not exist or does not have any logs.";
        Map model = new HashMap();
        model.put("errorMessage", errorMessage);
        return new ModelAndView("page_exceptions", model);
    }

    public ModelAndView noproject(HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) throws Exception {
        Map model = new HashMap();
        model.put("errorMessage", "No project specified.");
        return new ModelAndView("page_exceptions", model);
    }

    public ModelAndView projectlog(HttpServletRequest httpServletRequest,
                                   HttpServletResponse httpServletResponse) throws Exception {
        String projectName = (String) httpServletRequest.getAttribute("projectName");
        String log = (String) httpServletRequest.getAttribute("log");
        String errorMessage = "The requested build log " + log + " does not exist in project " + projectName + ".";
        Map model = new HashMap();
        model.put("errorMessage", errorMessage);
        return new ModelAndView("page_exceptions", model);
    }
}
