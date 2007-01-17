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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.cruisecontrol.LogFileSetupDecorator;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author jfredrick
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class ArtifactsLinkTagTest extends TestCase {

    private ArtifactsLinkTag tag;
    private MockPageContext pageContext;
    private MockServletRequest request;

    public static Test suite() {
        return new LogFileSetupDecorator(new TestSuite(ArtifactsLinkTagTest.class));
    }

    protected void setUp() throws Exception {
        request = new MockServletRequest("context", "servlet");

        pageContext = new MockPageContext();
        pageContext.setHttpServletRequest(request);

        tag = new ArtifactsLinkTag();
        tag.setPageContext(pageContext);

        final MockServletConfig servletConfig = (MockServletConfig) pageContext.getServletConfig();
        servletConfig.setInitParameter("logDir", LogFileSetupDecorator.LOG_DIR.getAbsolutePath());
    }

    protected void tearDown() {
        tag.release();
        tag = null;
        pageContext.release();
        pageContext = null;
    }

    public void testDoStartTag() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, tag.doStartTag());
    }

    public void testDoAfterBody() throws JspException {
        MockBodyContent content = new MockBodyContent();
        tag.setBodyContent(content);
        assertEquals(Tag.SKIP_BODY, tag.doAfterBody());
    }

    public void testDoInitBody() throws JspException {
        request.addParameter("log", "log20030611123100");
        tag.doInitBody();
        String url = (String) pageContext.getAttribute(ArtifactsLinkTag.URL_ATTRIBUTE);
        assertEquals("artifacts/20030611123100", url);
        request.removeParameter("log");
    }

}
