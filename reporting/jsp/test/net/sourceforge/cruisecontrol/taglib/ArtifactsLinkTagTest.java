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
import java.io.FileWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockBodyContent;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

/**
 * @author jfredrick
 */
public class ArtifactsLinkTagTest extends TestCase {

    private ArtifactsLinkTag tag;
    private MockPageContext pageContext;
    private MockServletRequest request;

    private File logDir;
    private File log1;
    private File log2;
    private File log3;
    private File log4;

    public ArtifactsLinkTagTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        tag = new ArtifactsLinkTag();

        pageContext = new MockPageContext();
        request = new MockServletRequest("context", "servlet");
        pageContext.setHttpServletRequest(request);

        tag.setPageContext(pageContext);

        logDir = new File("testresults/ArtifactsLinkTagTest");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        log1 = new File(logDir, "log19920330120000.xml");
        log2 = new File(logDir, "log19930925120000.xml");
        log3 = new File(logDir, "log20020731220000.xml");
        log4 = new File(logDir, "log20030611123100.xml");
    }

    protected void tearDown() {
        tag = null;
        pageContext = null;

        log1.delete();
        log2.delete();
        log3.delete();
        logDir.delete();
        
        log1 = null;
        log2 = null;
        log3 = null;
        log4 = null;        
    }
    
    public void testGetTimeFromLogParam() {
        assertEquals("", tag.getTimeFromLogParam());

        request.addParameter("log", "log20030611123100");
        assertEquals("20030611123100", tag.getTimeFromLogParam());

        request.removeParameter("log");
        request.addParameter("log", "log20030611123100Lbuild.1");
        assertEquals("20030611123100", tag.getTimeFromLogParam());
    }
    
    public void testGetTimeFromLatestLogFile() throws Exception {
        writeFile(log1, "");
        writeFile(log2, "");
        writeFile(log3, "");
        writeFile(log4, "");

        MockServletConfig config = (MockServletConfig) pageContext.getServletConfig();
        config.setInitParameter("logDir", logDir.getAbsolutePath());
        assertEquals("20030611123100", tag.getTimeFromLatestLogFile());
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
    }
    
    private void writeFile(File file, String body) throws Exception {
        FileWriter writer = new FileWriter(file);
        writer.write(body);
        writer.close();
    }

}
