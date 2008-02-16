package net.sourceforge.cruisecontrol.dashboard.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.util.BuildInformationHelper;

import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class BuildLoopController extends AbstractController {

    private final BuildInformationHelper helper;

    private final BuildInformationRepository repository;

    public BuildLoopController(BuildInformationHelper helper, BuildInformationRepository repository) {
        this.helper = helper;
        this.repository = repository;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String content = IOUtils.toString(request.getInputStream());
        BuildLoopInformation info = helper.toObject(content);
        repository.saveOrUpdate(info);
        return null;
    }
}
