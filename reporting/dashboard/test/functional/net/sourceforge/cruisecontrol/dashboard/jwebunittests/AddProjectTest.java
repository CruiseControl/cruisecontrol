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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import java.io.File;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

public class AddProjectTest extends BaseFunctionalTest {
    private static final String PROJECT_FORM_NAME = "addProject";

    private static final String PROJECT_NAME_FIELD = "projectName";

    private static final String ADD_NEW_PROJECT_TITLE = "Add New Project";

    public static final String UNRESOLVABLE_HOSTNAME_ERROR = "svn: PROPFIND request failed on '/'\nsvn: PROPFIND "
        + "of '/': Could not resolve hostname `foo': Host not found (http://foo)\n";

    private File configurationFile;

    protected void onSetUp() throws Exception {
        configurationFile = DataUtils.getConfigXmlAsFile();
        setConfigFileAndSubmitForm(configurationFile.toString());
        tester.beginAt("/admin/project/add");
    }

    public void testShouldBeAbleToNagivateToAddProjectPageWithInputBox() {
        tester.assertTitleEquals(ADD_NEW_PROJECT_TITLE);
        tester.assertFormPresent(PROJECT_FORM_NAME);
        tester.assertFormElementPresent(PROJECT_NAME_FIELD);
    }

    public void testShouldBeAbleToAddProjectToCruiseControlAndGotoProjectListPage() throws Exception {
        String exptectedProject = "new-project-" + (new DateTime()).getMillis();
        String url = "addProjectFromVersionControl.ajax?vcsType=svn&url=valid&projectName=" + exptectedProject;
        String json = getJSONWithAjaxInvocation(url);
        assertTrue(StringUtils.contains(json, "\"ok\" : \"success\""));
        tester.beginAt(url);
        assertProjectedHasBeenAdded(exptectedProject);
    }

    public void testShouldShowBuildFileNotFoundInFlashMessage() throws Exception {
        String exptectedProject = "new-project-" + (new DateTime()).getMillis();
        String url = "addProjectFromVersionControl.ajax?vcsType=svn&url=valid&projectName=" + exptectedProject;
        String json = getJSONWithAjaxInvocation(url);
        assertTrue(StringUtils.contains(json, "\"ok\" : \"success\""));
        assertTrue(StringUtils.contains(json, "build.xml"));
        assertProjectedHasBeenAdded(exptectedProject);
    }

    public void testShouldNotShowBuildFileNotFoundInFlashMessageIfBuildFileFound() throws Exception {
        String exptectedProject = "new-project-" + (new DateTime()).getMillis();
        String url = "addProjectFromVersionControl.ajax?vcsType=svn&url=valid.build&projectName=" + exptectedProject;
        String json = getJSONWithAjaxInvocation(url);
        assertTrue(StringUtils.contains(json, "\"ok\" : \"success\""));
        assertFalse(StringUtils.contains(json, "build.xml"));
        assertProjectedHasBeenAdded(exptectedProject);
    }

    private void assertProjectedHasBeenAdded(String exptectedProject) {
        tester.beginAt("/admin/config");
        tester.assertTextInElement("configFileContent", exptectedProject);
    }

    public void testShouldShowVcsTypeNotSupportedInFlashMessage() throws Exception {
        String exptectedProject = "new-project-" + (new DateTime()).getMillis();
        String url = "addProjectFromVersionControl.ajax?vcsType=notSupported&url=valid.build&projectName="
                + exptectedProject;
        String json = getJSONWithAjaxInvocation(url);
        assertTrue(StringUtils.contains(json, "\"ok\" : \"failure\""));
        assertTrue(StringUtils.contains(json, "You have to specify a valid version control system"));
        assertProjectedNotAdded(exptectedProject);
    }

    public void testShouldShowReturnFailureMessageWhenUsingCvsWithoutProvidingModule() throws Exception {
        String exptectedProject = "new-project-" + (new DateTime()).getMillis();
        String url = "addProjectFromVersionControl.ajax?vcsType=cvs&url=valid.build&projectName=" + exptectedProject;
        String json = getJSONWithAjaxInvocation(url);
        assertTrue(StringUtils.contains(json, "\"ok\" : \"failure\""));
        assertTrue(StringUtils.contains(json, "You must provide a module name for cvs project"));
        tester.beginAt("/admin/config");
        assertProjectedNotAdded(exptectedProject);
    }

    private void assertProjectedNotAdded(String exptectedProject) {
        tester.beginAt("/admin/config");
        tester.assertTextNotInElement("configFileContent", exptectedProject);
    }
}
