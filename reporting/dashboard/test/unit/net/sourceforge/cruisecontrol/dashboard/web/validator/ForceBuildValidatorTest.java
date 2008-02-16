package net.sourceforge.cruisecontrol.dashboard.web.validator;

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigurationService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardXmlConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.web.command.ForceBuildCommand;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.validation.BindException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ForceBuildValidatorTest extends MockObjectTestCase {
    private ForceBuildValidator validator;

    private ForceBuildCommand forceBuildCommand;

    private BindException errors;

    private Mock mockConfiguration;

    protected void setUp() throws Exception {
        mockConfiguration =
                mock(ConfigurationService.class, new Class[] {EnvironmentService.class,
                        DashboardXmlConfigService.class, BuildLoopQueryService.class}, new Object[] {null,
                        null, null});
        validator = new ForceBuildValidator((ConfigurationService) mockConfiguration.proxy());
        forceBuildCommand = new ForceBuildCommand();
        forceBuildCommand.setProjectName("project1");
        errors = new BindException(forceBuildCommand, "target");
    }

    public void testShouldSupportForceBuildCommand() throws Exception {
        assertTrue(validator.supports(ForceBuildCommand.class));
    }

    public void testShouldFailedWhenForceBuildIsDisabled() throws Exception {
        List active = new ArrayList();
        active.add(new File("project2"));
        mockConfiguration.expects(once()).method("getDiscontinuedProjects").will(returnValue(active));
        mockConfiguration.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(new File("project1")));
        mockConfiguration.expects(once()).method("isForceBuildEnabled").will(returnValue(false));
        validator.validate(forceBuildCommand, errors);
        assertEquals(1, errors.getErrorCount());
    }

    public void testShouldFailWhenTheProjectIsInactive() throws Exception {
        List inactive = new ArrayList();
        inactive.add(new File("project1"));
        mockConfiguration.expects(once()).method("getDiscontinuedProjects").will(returnValue(inactive));
        mockConfiguration.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(new File("project1")));
        mockConfiguration.expects(once()).method("isForceBuildEnabled").will(returnValue(true));
        validator.validate(forceBuildCommand, errors);
        assertEquals(1, errors.getErrorCount());
    }

    public void testShouldFailWhenTheProjectIsBuilding() throws Exception {
        List active = new ArrayList();
        active.add(new File("project2"));
        mockConfiguration.expects(once()).method("getDiscontinuedProjects").will(returnValue(active));
        mockConfiguration.expects(once()).method("getLogRoot").with(eq("project1")).will(
                returnValue(new File("project1")));
        mockConfiguration.expects(once()).method("isForceBuildEnabled").will(returnValue(true));
        validator.validate(forceBuildCommand, errors);
        assertEquals(0, errors.getErrorCount());
    }
}
