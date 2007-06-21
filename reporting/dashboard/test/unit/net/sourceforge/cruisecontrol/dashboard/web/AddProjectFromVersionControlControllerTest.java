/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.dashboard.web;

import java.io.File;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.exception.NonSupportedVersionControlException;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.service.VersionControlFactory;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.ConnectionResult;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Cvs;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Perforce;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.util.CruiseRuntime;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddProjectFromVersionControlControllerTest extends MockObjectTestCase {

    private AddProjectFromVersionControlController controller;

    private Configuration configuration;

    private Mock configurationMock;

    private File configurationFile;

    private MockHttpServletResponse response;

    private MockHttpServletRequest request;

    private Mock versionControlFactoryMock;

    protected void setUp() throws Exception {
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        configurationMock =
                mock(Configuration.class, new Class[] {ConfigXmlFileService.class},
                        new Object[] {null});
        configuration = (Configuration) configurationMock.proxy();
        versionControlFactoryMock =
                mock(VersionControlFactory.class, new Class[] {CruiseRuntime.class},
                        new Object[] {null});
        controller =
                new AddProjectFromVersionControlController(
                        (VersionControlFactory) versionControlFactoryMock.proxy(), configuration);
    }

    protected void onTearDown() throws Exception {
        configurationFile.delete();
    }

    public void testShouldPromptErrorMessageIfVersionControlTypeNotValid() throws Exception {
        prepareRequestParameter("valid", "project name", "invalid");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                throwException(new NonSupportedVersionControlException("")));

        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("You have to specify a valid version control system", result.get("response"));
    }

    private MockHttpServletResponse getResponse() {
        return response;
    }

    private MockHttpServletRequest getRequest() {
        return request;
    }

    public void testShouldPromptErrorMessageWhenProjectNameIsEmtpty() throws Exception {
        prepareRequestParameter("valid", "", "svn");
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("Project name can not be blank", result.get("response"));
    }

    public void testShouldPromptErrorMessageWhenProjectAlreadyExist() throws Exception {
        prepareRequestParameter("valid", "project1", "svn");
        configurationMock.expects(once()).method("hasProject").will(returnValue(true));

        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("project1 already exists, please choose another name.", result.get("response"));
    }

    public void testShouldReturnValidConnectionJsonViewIfSVNAddressIsValid() throws Exception {
        prepareRequestParameter("valid", "whatever", "svn");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        configurationMock.expects(once()).method("getSourceCodeRoot").with(eq("whatever"));
        configurationMock.expects(once()).method("addProject");
        configurationMock.expects(once()).method("getSourceCodeRoot").with(eq("whatever"));

        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new HappyVcs()));
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("success", result.get("ok"));
    }

    public void testCheckOutShouldBeInvoked() throws Exception {
        prepareRequestParameter("valid", "whatever", "svn");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        configurationMock.expects(once()).method("getSourceCodeRoot").with(eq("whatever"));
        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new CheckoutThrowExceptionVcs()));
        try {
            controller.handleRequest(getRequest(), getResponse());
            fail();
        } catch (Exception e) {
            // ok.
        }
    }

    public void testShouldReturnInvalidConnectionJsonViewIfSVNAddressIsInvalid() throws Exception {
        prepareRequestParameter("whatever", "whatever", "connectionFailed");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new ConnectionFailedVcs()));
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("failure", result.get("ok"));
    }

    public void testShouldContainsBuildXmlNotFoundWhenBuildDotXmlIsMissing() throws Exception {
        prepareRequestParameter("whatever", "whatever", "buildFileNotExist");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        configurationMock.expects(once()).method("getSourceCodeRoot").with(eq("whatever"));
        configurationMock.expects(once()).method("addProject");
        configurationMock.expects(once()).method("getSourceCodeRoot").with(eq("whatever"));

        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new BuildFileNotExistVcs()));
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertTrue(StringUtils.contains((String) result.get("response"), "build.xml"));
    }

    public void testShouldShowErrorMessageIfModuleNameMissingForCvsProject() throws Exception {
        prepareRequestParameter("valid", "project name", "cvs");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new Cvs("", "", null)));
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("You must provide a module name for cvs project", result.get("response"));
    }

    public void testShouldShowErrorMessageIfModuleNameMissingForPerforceProject() throws Exception {
        prepareRequestParameter("valid", "project name", "perforce");
        configurationMock.expects(once()).method("hasProject").will(returnValue(false));
        versionControlFactoryMock.expects(once()).method("getVCSInstance").will(
                returnValue(new Perforce(null, null, null, null)));
        ModelAndView mov = controller.handleRequest(getRequest(), getResponse());
        Map result = (Map) mov.getModelMap().get("result");
        assertEquals("You must provide the depot path for perforce project", result.get("response"));
    }

    private void prepareRequestParameter(String url, String projectName, String vcsType) {
        getRequest().setMethod("POST");
        getRequest().addParameter("url", url);
        getRequest().addParameter("projectName", projectName);
        getRequest().addParameter("vcsType", vcsType);
    }

    class VersionControlFactoryStub extends VersionControlFactory {

        public VersionControlFactoryStub(CruiseRuntime runtime) {
            super(runtime);
        }

        public VCS getVCSInstance(String projectName, String url, String module, String type)
                throws NonSupportedVersionControlException {
            if ("svn".equals(type)) {
                return new HappyVcs();
            }
            if ("connectionFailed".equals(type)) {
                return new ConnectionFailedVcs();
            }
            if ("buildFileNotExist".equals(type)) {
                return new BuildFileNotExistVcs();
            }
            if ("checkoutThrowException".equals(type)) {
                return new CheckoutThrowExceptionVcs();
            }
            return super.getVCSInstance(projectName, url, null, type);
        }

    }

    class HappyVcs implements VCS {

        public boolean checkBuildFile() {
            return true;
        }

        public ConnectionResult checkConnection() {
            return new ConnectionResult(true, "");
        }

        public void checkout(String path) {
        }

        public Element getRepositoryElement(Document doc) {
            return null;
        }

        public Element getBootStrapperElement(Document doc) {
            return null;
        }

        public String getBootStrapper() {
            return "svnbootstrapper";
        }

        public String getRepository() {
            return "svn";
        }
    }

    class ConnectionFailedVcs extends HappyVcs {

        public ConnectionResult checkConnection() {
            return new ConnectionResult(false, "error");
        }

        public String getTagName() {
            return "whatever";
        }
    }

    class BuildFileNotExistVcs extends HappyVcs {

        public boolean checkBuildFile() {
            return false;
        }

        public String getTagName() {
            return "whatever";
        }

    }

    class CheckoutThrowExceptionVcs extends HappyVcs {
        public void checkout(String path) {
            throw new RuntimeException("checkout has been invoked.");
        }
    }
}
