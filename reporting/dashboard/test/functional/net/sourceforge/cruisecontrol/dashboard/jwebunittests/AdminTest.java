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

import org.apache.commons.io.FileUtils;

import net.sourceforge.cruisecontrol.dashboard.service.SystemService;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.web.AdminController;

public class AdminTest extends BaseFunctionalTest {
	private String content;
	private File dashboardConfig;

	protected void onSetUp() throws Exception {
		dashboardConfig = DataUtils.getDashboardConfigXmlOfWebApp();
		content = FileUtils.readFileToString(dashboardConfig, "UTF-8");
	}

	protected void tearDown() throws Exception {
		if (!dashboardConfig.exists()) {
			dashboardConfig.createNewFile();
			FileUtils.writeStringToFile(dashboardConfig, content);
		}
	}

	public void testShouldBeAbleToShowEditConfigFormAndShowConfigFileContentsAndUpdateContents()
			throws Exception {
		tester.beginAt("/admin/config");
		shouldContainDashboardConfigurationXmlLocation();
		shouldContainDashboardConfigurationXmlContent();
		shouldContainDiagnosisInfomration();
	}

	public void testShouldNotDisplayConfigXmlContentWhenFileIsMissing()
			throws Exception {
		dashboardConfig.delete();
		tester.beginAt("/admin/config");
		shouldContainDashboardConfigurationXmlLocation();
		shouldNotContainDashboardConfigurationXmlContent();
	}

	private void shouldNotContainDashboardConfigurationXmlContent() {
		tester.assertTextPresent(AdminController.ERROR_MESSAGE_NOT_EXIST);
	}

	private void shouldContainDiagnosisInfomration() throws Exception {
		tester.assertTextPresent("N/A");
		SystemService systemService = new SystemService();
		tester.assertTextPresent(systemService.getJvmVersion());
		tester.assertTextPresent(systemService.getOsInfo());
		tester.assertTextPresent(DataUtils.getLogRootOfWebapp()
				.getAbsolutePath());
		tester.assertTextPresent(DataUtils.getArtifactRootOfWebapp()
				.getAbsolutePath());
	}

	private void shouldContainDashboardConfigurationXmlContent() {
		tester.assertTextPresent("<buildloop");
		tester.assertTextPresent("<features");
		tester.assertTextPresent("<trackingtool");
	}

	private void shouldContainDashboardConfigurationXmlLocation() {
		tester.assertTextPresent("dashboard-config.xml");
	}
}