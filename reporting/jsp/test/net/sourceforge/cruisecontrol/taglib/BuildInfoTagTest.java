/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.cruisecontrol.BuildInfoSummary;
import net.sourceforge.cruisecontrol.LogFileSetupDecorator;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;

/**
 * Test case for the BuildInfoTagTest.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class BuildInfoTagTest extends TestCase {
    private MockPageContext pageContext;
    private BuildInfoTag tag;

    public static Test suite() {
        return new LogFileSetupDecorator(new TestSuite(BuildInfoTagTest.class));
    }

    protected void setUp() {
        pageContext = new MockPageContext();
        tag = new BuildInfoTag();
        tag.setPageContext(pageContext);

        final MockServletConfig servletConfig = (MockServletConfig) pageContext.getServletConfig();
        servletConfig.setInitParameter("logDir", LogFileSetupDecorator.LOG_DIR.getAbsolutePath());
    }

    protected void tearDown() {
        tag.release();
    }

    /** Verify that after the tag executes, an instance of BuildInfo is available and configured. */
    public void testTagCreatesBuildInfo() throws JspException {
        assertEquals(Tag.SKIP_BODY, tag.doStartTag());
        assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        BuildInfoSummary buildInfoSummary = (BuildInfoSummary) pageContext.getAttribute(BuildInfoTag.INFO_ATTRIBUTE);
        assertNotNull(buildInfoSummary);
        assertEquals(6, buildInfoSummary.size());
    }
}
