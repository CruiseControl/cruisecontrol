/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
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
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletContext;

public class NavigationTagTest extends TestCase {
    private NavigationTag tag;
    private MockPageContext pageContext;
    private File[] logFiles;
    private File logDir;

    public NavigationTagTest(String name) {
        super(name);
    }

    public void setUp() throws IOException {
        tag = new NavigationTag();
        pageContext = new MockPageContext();
        final MockServletConfig config = new MockServletConfig();
        pageContext.setServletConfig(config);
        pageContext.setServletContext(new MockServletContext());

        tag.setPageContext(pageContext);
        tag.setBodyContent(new MockBodyContent());

        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        config.setInitParameter("logDir", logDir.getAbsolutePath());

        logFiles = new File[] { new File(logDir, "log20020222120000.xml"), new File(logDir, "log20020223120000.xml"),
                                new File(logDir, "log20020224120000.xml"), new File(logDir, "log20020225120000.xml") };
        for (int i = 0; i < logFiles.length; i++) {
            File logFile = logFiles[i];
            logFile.createNewFile();
        }
    }

    protected void tearDown() throws Exception {
        for (int i = 0; i < logFiles.length; i++) {
            logFiles[i].delete();
        }
        logDir.delete();
    }

    public void testGetUrl() {
        final String expectedValue = "cruisecontrol/buildresults?log=log20020222120000";
        assertEquals(expectedValue, tag.getUrl("log20020222120000", "cruisecontrol/buildresults"));
    }

    public void testGetLinkText() {
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log20020222120000"));
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log200202221200"));
        assertEquals("02/22/2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11"));
        assertEquals("02/22/2002 12:00:00 (L.0)", tag.getLinkText("log20020222120000LL.0"));
    }

    public void testGetFormattedLinkText() {
        String formatString = "dd-MMM-yyyy HH:mm:ss";
        tag.setDateFormat(formatString);
        assertEquals("22-Feb-2002 12:00:00", tag.getLinkText("log20020222120000"));
        assertEquals("22-Feb-2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11"));
    }

    public void testGetLinks() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();
        assertEquals("02/25/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals("log20020225120000", pageContext.getAttribute(NavigationTag.LOG_FILE_ATTR));
        assertEquals("02/25/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/23/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/22/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.SKIP_BODY, tag.doAfterBody());
    }

    public void testGetLinksWithSubRange() throws Exception {
        tag.setStartingBuildNumber(1);
        tag.setFinalBuildNumber(2);

        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/23/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.SKIP_BODY, tag.doAfterBody());
    }

    public void testGetLinksWithLargeStartRange() throws Exception {
        tag.setStartingBuildNumber(10);
        assertEquals(BodyTag.SKIP_BODY, tag.doStartTag());
    }

    public void testGetLinksWithLowEndRange() throws Exception {
        tag.setFinalBuildNumber(-1);
        assertEquals(BodyTag.SKIP_BODY, tag.doStartTag());
    }

    public void testGetLinksWithInvertedRange() throws Exception {
        tag.setFinalBuildNumber(0);
        tag.setStartingBuildNumber(10);
        assertEquals(BodyTag.SKIP_BODY, tag.doStartTag());
    }
}