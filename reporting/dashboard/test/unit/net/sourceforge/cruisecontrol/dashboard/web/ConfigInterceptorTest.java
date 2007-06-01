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

import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.service.ConfigXmlFileService;
import net.sourceforge.cruisecontrol.dashboard.service.EnvironmentService;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ConfigInterceptorTest extends MockObjectTestCase {
    private MockHttpServletRequest request;

    private MockHttpServletResponse reponse;

    private Mock mockConfiguration;

    private Configuration configurationMock;

    private ConfigInterceptor interceptor;

    protected void setUp() throws Exception {
        request = new MockHttpServletRequest();
        reponse = new MockHttpServletResponse();
        mockConfiguration = mock(Configuration.class, new Class[]{ConfigXmlFileService.class},
                new Object[]{new ConfigXmlFileService(new EnvironmentService())});
        configurationMock = (Configuration) mockConfiguration.proxy();
        interceptor = new ConfigInterceptor(configurationMock);
    }

    public void testShouldReturnFalseWhenTheCruiseLogIsEmtpty() throws Exception {
        request.setRequestURI("not");
        mockConfiguration.expects(once()).method("getCruiseConfigLocation").will(returnValue(null));
        boolean result = interceptor.preHandle(request, reponse, null);
        assertFalse(result);
        assertEquals("/admin/config", reponse.getRedirectedUrl());
    }

    public void testShouldReturnTrueWhenURLIsUpdateConfigXmlLocation() throws Exception {
        mockConfiguration.expects(once()).method("getCruiseConfigLocation").will(returnValue(null));
        mockConfiguration.expects(once()).method("getCruiseConfigLocation").will(returnValue(null));
        request.setRequestURI("admin/config/setup");
        boolean result = interceptor.preHandle(request, reponse, null);
        assertTrue(result);
        request.setRequestURI("admin/config");
        result = interceptor.preHandle(request, reponse, null);
        assertTrue(result);
    }

    public void testShouldReturnTrueWhenTheCruiseLogIsNotEmtpty() throws Exception {
        mockConfiguration.expects(once()).method("getCruiseConfigLocation").will(returnValue("not empty"));
        boolean result = interceptor.preHandle(request, reponse, null);
        assertTrue(result);
        assertNull(reponse.getRedirectedUrl());
    }

}
