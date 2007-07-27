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

import java.util.Arrays;

import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.service.CruiseControlJMXService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;
import net.sourceforge.cruisecontrol.dashboard.service.JMXFactory;
import net.sourceforge.cruisecontrol.dashboard.service.SystemService;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetCommitMessageControllerTest extends MockObjectTestCase {
    private MockHttpServletRequest request = new MockHttpServletRequest();

    private MockHttpServletResponse response = new MockHttpServletResponse();

    private Mock jmxServiceMock =
            mock(CruiseControlJMXService.class, new Class[] {JMXFactory.class, EnvironmentService.class},
                    new Object[] {null,
                            new EnvironmentService(new SystemService(), new DashboardConfigService[] {})});

    private GetCommitMessageController controller =
            new GetCommitMessageController((CruiseControlJMXService) jmxServiceMock.proxy());

    public void testJSONObjectTypeShouldBeArray() throws Exception {
        jmxServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new ModificationKey[] {})));
        request.setParameter("project", "project1");

        controller.handleRequest(request, response);

        String json = (String) response.getContentAsString();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }

    public void testShouldReturnEmptyArrayIfThereIsNoCommitMessages() throws Exception {
        jmxServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new ModificationKey[] {})));
        request.setParameter("project", "project1");

        controller.handleRequest(request, response);

        String json = (String) response.getContentAsString();
        assertEquals("[]", json);
    }

    public void testJSONObjectShouldHasUserAndMessageProperty() throws Exception {
        jmxServiceMock.expects(once()).method("getCommitMessages").with(eq("project1")).will(
                returnValue(Arrays.asList(new ModificationKey[] {
                        new ModificationKey("add new feature", "joe"),
                        new ModificationKey("update build", "joe")})));
        request.setParameter("project", "project1");

        controller.handleRequest(request, response);

        String json = (String) response.getContentAsString();
        assertTrue(StringUtils.contains(json, "{"));
        assertTrue(StringUtils.contains(json, "\"user\":\"joe\""));
        assertTrue(StringUtils.contains(json, "\"comment\":\"update build\""));
        assertTrue(StringUtils.contains(json, "\"comment\":\"add new feature\""));
        assertTrue(StringUtils.contains(json, "}"));
    }
}
