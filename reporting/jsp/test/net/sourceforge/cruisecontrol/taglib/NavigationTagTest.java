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

import junit.framework.TestCase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.IOException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.PageContext;
import javax.servlet.http.HttpSession;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;

public class NavigationTagTest extends TestCase {

    public NavigationTagTest(String name) {
        super(name);
    }

    public void testGetUrl() {
        NavigationTag tag = new NavigationTag();
        final String expectedValue = "cruisecontrol/buildresults?log=log20020222120000";
        assertEquals(expectedValue, tag.getUrl("log20020222120000.xml", "cruisecontrol/buildresults"));
    }

    public void testGetLinkText() {
        NavigationTag tag = new NavigationTag();
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log20020222120000.xml"));
        assertEquals("02/22/2002 12:00:00", tag.getLinkText("log200202221200.xml"));
        assertEquals("02/22/2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11.xml"));
        assertEquals("02/22/2002 12:00:00 (L.0)", tag.getLinkText("log20020222120000LL.0.xml"));        

        String formatString = "dd-MMM-yyyy HH:mm:ss";
        tag.setDateFormat(formatString);

        assertEquals("22-Feb-2002 12:00:00", tag.getLinkText("log20020222120000.xml"));
        assertEquals("22-Feb-2002 12:00:00 (3.11)", tag.getLinkText("log20020222120000L3.11.xml"));
    }

    public void testGetLinksWithSubRange() throws Exception {
        NavigationTag tag = new NavigationTag();
        final MockPageContext pageContext = new MockPageContext();
        tag.setPageContext(pageContext);
        tag.setBodyContent(new MockBodyContent());

        String[] logFiles = { "log20020222120000.xml", "log20020223120000.xml", "log20020224120000.xml",
                              "log20020225120000.xml" };
        tag.setFileNames(logFiles);
        tag.setStartingBuildNumber(1);
        tag.setFinalBuildNumber(2);

        assertEquals(0, tag.getCount());
        tag.doInitBody();
        assertEquals(2, tag.getCount());
        assertEquals("02/23/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doAfterBody());
        assertEquals(3, tag.getCount());
        assertEquals("02/24/2002 12:00:00", pageContext.getAttribute(NavigationTag.LINK_TEXT_ATTR));
        assertEquals(BodyTag.SKIP_BODY, tag.doAfterBody());
        assertEquals(3, tag.getCount());
    }

    public void testGetLinksWithBadRange() throws Exception {
        NavigationTag tag = new NavigationTag();
        final MockPageContext pageContext = new MockPageContext();
        tag.setPageContext(pageContext);
        tag.setBodyContent(new MockBodyContent());

        String[] logFiles = { "log20020222120000.xml", "log20020223120000.xml", "log20020224120000.xml",
                              "log20020225120000.xml" };
        tag.setFileNames(logFiles);
        tag.setStartingBuildNumber(10);

        assertEquals(0, tag.getCount());
        tag.doInitBody();
        assertEquals(BodyTag.SKIP_BODY, tag.doAfterBody());
        assertEquals(10, tag.getCount());
    }
}