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
package net.sourceforge.cruisecontrol.servlet;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.StatusHelper;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletContext;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;
import net.sourceforge.cruisecontrol.mock.MockServletResponse;
import junit.framework.TestCase;

public class XmlServletTest extends TestCase {

    private static final String LOGDIR = "LOGDIR";
    private static final String SINGLE_PROJECT = "SINGLEPROJECT";
    private static final String STATUS_FILE = "STATUSFILE";
    
    private XmlServlet xmlServlet;

    protected void setUp() throws Exception {
        super.setUp();
        this.xmlServlet = new XmlServlet();
    }

    protected void tearDown() throws Exception {
        this.xmlServlet = null;
        super.tearDown();
    }

    public void testInitServletConfig() throws ServletException {
        MockServletConfig mockConfig = new MockServletConfig();
        MockServletContext mockContext = new MockServletContext();
        mockConfig.setServletContext(mockContext);
        mockConfig.setInitParameter("logDir", LOGDIR);
        mockConfig.setInitParameter("singleProject", SINGLE_PROJECT);
        mockConfig.setInitParameter("currentBuildStatusFile", STATUS_FILE);
        
        this.xmlServlet.init(mockConfig);
        assertEquals(LOGDIR, this.xmlServlet.getLogDirPath());
        assertEquals(SINGLE_PROJECT, this.xmlServlet.getSingleProject());
        assertEquals(STATUS_FILE, this.xmlServlet.getStatusFile());
    }

    public void testServiceFails() throws ServletException, IOException {
        MockServletRequest mockRequest = new MockServletRequest();
        MockServletResponse mockResponse = new MockServletResponse();
        
        try {
            this.xmlServlet.service(mockRequest, mockResponse);
            fail("Should have thown exception - directory not found");
        } catch (ServletException e) {
            // OK
        }
        String actual = mockResponse.getWritten();
        assertEquals("", actual);
    }
    
    public void testActivities() throws ServletException, IOException {
        assertEquals(XmlServlet.ACTIVITY_BUILDING, 
                    this.xmlServlet.getXmlActivity(ProjectState.BUILDING.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_CHECKING_MODIFICATIONS, 
                    this.xmlServlet.getXmlActivity(ProjectState.MODIFICATIONSET.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_CHECKING_MODIFICATIONS, 
                    this.xmlServlet.getXmlActivity(ProjectState.MERGING_LOGS.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_CHECKING_MODIFICATIONS, 
                    this.xmlServlet.getXmlActivity(ProjectState.BOOTSTRAPPING.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_CHECKING_MODIFICATIONS, 
                    this.xmlServlet.getXmlActivity(ProjectState.PUBLISHING.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_SLEEPING, 
                    this.xmlServlet.getXmlActivity(ProjectState.WAITING.getDescription() + " more text"));
        assertEquals(XmlServlet.ACTIVITY_SLEEPING, 
                    this.xmlServlet.getXmlActivity("SomethingElse"));
    }
    
    public void testStatuses() throws ServletException, IOException {
        assertEquals(XmlServlet.STATUS_UNKNOWN, this.xmlServlet.getXmlStatus("somethingElse"));
        assertEquals(XmlServlet.STATUS_SUCCESS, this.xmlServlet.getXmlStatus(StatusHelper.PASSED));
        assertEquals(XmlServlet.STATUS_FAILURE, this.xmlServlet.getXmlStatus(StatusHelper.FAILED));
    }


}
