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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import net.sourceforge.cruisecontrol.mock.MockBodyContent;

import junit.framework.TestCase;

/**
 * Compatable with JSP 1.1 & Servlet 2.2 (Tomcat 3.3)
 * @author jfredrick
 */
public class CruiseControlBodyTagSupportTest extends TestCase {

    private CruiseControlBodyTagSupport support;
    private BodyContent content;
    
    public CruiseControlBodyTagSupportTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        support = new CruiseControlBodyTagSupport();
        content = new MockBodyContent();
        support.setBodyContent(content);
    }

    protected void tearDown() throws Exception {
        support = null;
        content = null;
    }

    public void testDoStartTag() throws JspException {
        assertEquals(BodyTag.EVAL_BODY_TAG, support.doStartTag());
    }

    public void testRelease() {
        support.release();
        assertNull(support.getBodyContent());
    }

    public void testDoAfterBody() throws JspException {
        assertEquals(Tag.SKIP_BODY, support.doAfterBody());
    }

    public void testGetPreviousOut() {
    }

}
