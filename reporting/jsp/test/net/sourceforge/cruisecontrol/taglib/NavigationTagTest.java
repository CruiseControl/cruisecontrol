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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildInfo;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

public class NavigationTagTest extends TestCase {
    private NavigationTag tag;
    private MockPageContext pageContext;
    private File[] logFiles;
    private File logDir;

    public void setUp() throws IOException {
        tag = new NavigationTag();
        pageContext = new MockPageContext();
        MockServletRequest request = new MockServletRequest("context", "servlet");
        request.setLocale(Locale.US);
        pageContext.setHttpServletRequest(request);

        tag.setPageContext(pageContext);
        tag.setBodyContent(new MockBodyContent());

        logDir = new File("testresults/NavigationTagTest");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdirs());
        }
        final MockServletConfig servletConfig = (MockServletConfig) pageContext.getServletConfig();
        servletConfig.setInitParameter("logDir", logDir.getAbsolutePath());

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

    private String getLinkText(String infoText) throws ParseException {
        return tag.getLinkText(new BuildInfo(new File(infoText + ".xml")));
    }
    public void testGetLinkText() throws JspTagException, ParseException {
        assertEquals("02/22/2002 12:00:00", getLinkText("log20020222120000"));

        // XXX Do we want to support log files without seconds?
        //assertEquals("02/22/2002 12:00:00", getLinkText("log200202221200"));
        assertEquals("02/22/2002 12:00:00 (3.11)", getLinkText("log20020222120000L3.11"));
        assertEquals("02/22/2002 12:00:00 (L.0)", getLinkText("log20020222120000LL.0"));
    }

    public void testGetFormattedLinkText() throws ParseException, JspTagException {
        String formatString = "dd-MMM-yyyy HH:mm:ss";
        tag.setDateFormat(formatString);

        DateFormat inputFormat = new  SimpleDateFormat("yyyyMMddHHmmss");
        Date date = inputFormat.parse("20020222120000");
        DateFormat expectedFormat = new SimpleDateFormat(formatString);
        String expectedDate = expectedFormat.format(date);

        assertEquals(expectedDate, getLinkText("log20020222120000"));
        assertEquals(expectedDate + " (3.11)", getLinkText("log20020222120000L3.11"));
    }

    public void testGetLinks() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();
        assertEquals("02/25/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals("log20020225120000", pageContext.getAttribute(NavigationTag.LOG_FILE_ATTR));
        assertEquals("/context/servlet?log=log20020225120000", pageContext.getAttribute(NavigationTag.URL_ATTR));
        assertEquals("02/25/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/23/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/22/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
    }

    public void testGetLinksWithSubRange() throws Exception {
        tag.setStartingBuildNumber(1);
        tag.setFinalBuildNumber(2);

        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals("02/23/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
    }

    public void testGetLinksWithRangeSizeOne() throws Exception {
        tag.setStartingBuildNumber(1);
        tag.setFinalBuildNumber(1);
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
        tag.doInitBody();
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.SKIP_BODY, tag.doAfterBody());
    }

    public void testGetLinksWithLargeStartRange() throws Exception {
        tag.setStartingBuildNumber(10);
        assertEquals(Tag.SKIP_BODY, tag.doStartTag());
    }

    public void testGetLinksWithLowEndRange() throws Exception {
        tag.setFinalBuildNumber(-1);
        assertEquals(Tag.SKIP_BODY, tag.doStartTag());
    }

    public void testGetLinksWithInvertedRange() throws Exception {
        tag.setFinalBuildNumber(0);
        tag.setStartingBuildNumber(10);
        assertEquals(Tag.SKIP_BODY, tag.doStartTag());
    }
}
