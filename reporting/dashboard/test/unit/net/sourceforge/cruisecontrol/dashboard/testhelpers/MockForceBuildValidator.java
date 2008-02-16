package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class MockForceBuildValidator implements Validator {

    private final EnvironmentService environmentService;

    public MockForceBuildValidator(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    public boolean supports(Class clazz) {
        return ForceBuildCommand.class.equals(clazz);
    }

    public void validate(Object commandObj, Errors error) {
        if (!environmentService.isForceBuildEnabled()) {
            error.reject("project is inactive");
        }
    }
}
