package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class BuildLoopControllerFake extends AbstractController {

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        IOUtils.copy(request.getInputStream(), response.getOutputStream());
        return null;
    }
}
