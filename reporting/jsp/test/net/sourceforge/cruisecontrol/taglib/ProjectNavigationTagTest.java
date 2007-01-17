/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.taglib;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

public class ProjectNavigationTagTest extends TestCase {
    private ProjectNavigationTag tag;
    private MockPageContext pageContext;
    private File logDir;
    private File[] logProjects;

    public void setUp() throws IOException {
        tag = new ProjectNavigationTag();
        pageContext = new MockPageContext();
        MockServletRequest request = new MockServletRequest("context", "servlet");
        request.setLocale(Locale.US);
        request.setPathInfo("/Project A");
        pageContext.setHttpServletRequest(request);

        tag.setPageContext(pageContext);
        tag.setBodyContent(new MockBodyContent());

        logDir = new File("testresults/ProjectNavigationTagTest");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        final MockServletConfig servletConfig = (MockServletConfig) pageContext.getServletConfig();
        servletConfig.setInitParameter("logDir", logDir.getAbsolutePath());


        logProjects = new File[] { new File(logDir, "ProjectB"), new File(logDir, "Project A"),
                                   new File(logDir, "First_project") };
        for (int i = 0; i < logProjects.length; i++) {
            if (!logProjects[i].exists()) {
                assertTrue("Failed to create test project directories", logProjects[i].mkdir());
            }
        }
    }

    protected void tearDown() throws Exception {
        for (int i = 0; i < logProjects.length; i++) {
            logProjects[i].delete();
        }
        logDir.delete();
    }

    public void testFindProjects() throws JspException {
        String[] projects = tag.findProjects();
        assertEquals(logProjects.length, projects.length);
        assertEquals("First_project", projects[0]);
    }


    public void testGetLinks() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();

        assertEquals("", pageContext.getAttribute(ProjectNavigationTag.SELECTED_ATTR));
        assertEquals("/context/", pageContext.getAttribute(ProjectNavigationTag.URL_ATTR));
        assertEquals(ProjectNavigationTag.STATUS_PAGE_TEXT,
                     pageContext.getAttribute(ProjectNavigationTag.LINK_TEXT_ATTR));

        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("", pageContext.getAttribute(ProjectNavigationTag.SELECTED_ATTR));
        assertEquals("/context/servlet/First_project", pageContext.getAttribute(ProjectNavigationTag.URL_ATTR));
        assertEquals("First_project", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));

        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals(ProjectNavigationTag.SELECTED_ATTR_VALUE,
                     // selected because the getPathInfo is set to this project
                     pageContext.getAttribute(ProjectNavigationTag.SELECTED_ATTR));
        assertEquals("/context/servlet/Project A", pageContext.getAttribute(ProjectNavigationTag.URL_ATTR));
        assertEquals("Project A", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));

        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("", pageContext.getAttribute(ProjectNavigationTag.SELECTED_ATTR));
        assertEquals("ProjectB", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));

        assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
    }


}
