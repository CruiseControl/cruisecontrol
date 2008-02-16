package net.sourceforge.cruisecontrol.dashboard.web;

import net.sourceforge.cruisecontrol.BuildLoopInformation;
import net.sourceforge.cruisecontrol.dashboard.repository.BuildInformationRepository;
import net.sourceforge.cruisecontrol.util.BuildInformationHelper;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;

public class BuildLoopControllerTest extends MockObjectTestCase {
    public void testBuildLoopShouldPassBuildInfomationProviderRepositoryImpl() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setContent("<xml/>".getBytes());
        req.setMethod("POST");
        Mock repositoryMock = mock(BuildInformationRepository.class);
        Mock helperMock = mock(BuildInformationHelper.class);
        BuildLoopController buildLoopController =
                new BuildLoopController((BuildInformationHelper) helperMock.proxy(),
                        (BuildInformationRepository) repositoryMock.proxy());
        BuildLoopInformation buildLoopInformation = new BuildLoopInformation(null, null, null, null);
        helperMock.expects(once()).method("toObject").with(eq("<xml/>")).will(
                returnValue(buildLoopInformation));
        repositoryMock.expects(once()).method("saveOrUpdate").with(eq(buildLoopInformation));
        buildLoopController.handleRequest(req, null);
    }
}
