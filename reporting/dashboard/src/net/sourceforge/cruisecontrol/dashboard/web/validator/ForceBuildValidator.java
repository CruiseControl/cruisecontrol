package net.sourceforge.cruisecontrol.dashboard.web.validator;

import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.io.File;
import java.util.Collection;

public class ForceBuildValidator implements Validator {

    private final ConfigurationService configuration;

    public ForceBuildValidator(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    public boolean supports(Class clazz) {
        return ForceBuildCommand.class.equals(clazz);
    }

    public void validate(Object commandObj, Errors error) {
        ForceBuildCommand command = (ForceBuildCommand) commandObj;
        File project = configuration.getLogRoot(command.getProjectName());
        Collection discontinuedProjects = configuration.getDiscontinuedProjects();
        if (!configuration.isForceBuildEnabled() || discontinuedProjects.contains(project)) {
            error.reject("project is inactive");
        }
    }
}
