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
package net.sourceforge.cruisecontrol.dashboard.smoketests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.cruisecontrol.BuildLoopInformationBuilder;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.dashboard.jwebunittests.BaseFunctionalTest;
import net.sourceforge.cruisecontrol.report.BuildLoopStatusReportTask;

import org.apache.commons.httpclient.HttpClient;

public class BuildLoopSmokeTest extends BaseFunctionalTest {

    public void testDashboardShouldReceiveSameBuildInformationBuildLoopSent() throws Exception {
        BuildLoopInformationBuilder builder =
                new BuildLoopInformationBuilder(new CruiseControlControllerStub("project1",
                        "now building since 20031212152235"));
        BuildLoopInformationBuilder builder2 =
                new BuildLoopInformationBuilder(new CruiseControlControllerStub("project1", "waiting"));
        String url = "http://localhost:9090/dashboard/buildloop/listener";
        BuildLoopStatusReportTask task = new BuildLoopStatusReportTask(builder, url, new HttpClient(), 20000);
        BuildLoopStatusReportTask task2 =
                new BuildLoopStatusReportTask(builder2, url, new HttpClient(), 20000);
        task.run();
        assertEquals(task.getSent(), task.getReponse());
        task2.run();
        assertEquals(task2.getSent(), task2.getReponse());
    }

    static class CruiseControlControllerStub extends CruiseControlController {
        private final String projectName;

        private final String status;

        public CruiseControlControllerStub(String projectName, String status) {
            this.projectName = projectName;
            this.status = status;
        }

        public List getProjects() {
            ProjectConfig p1 = new ProjectConfig() {
                public String getBuildStartTime() {
                    return "20031212152235";
                }

                public List getModifications() {
                    ArrayList list = new ArrayList();
                    Modification m1 = new Modification();
                    m1.comment = "support security check";
                    m1.userName = "JK";
                    m1.revision = "2023";
                    list.add(m1);
                    return list;
                }

                public String getName() {
                    return projectName;
                }

                public String getStatus() {
                    return status;
                }
            };
            return Arrays.asList(new ProjectConfig[] {p1});
        }
    }
}
