/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class CruiseControlTagSupportTest extends TestCase {
    private CruiseControlTagSupport tag;
    private MockServletRequest request;
    
    private File logDir;
    private File log1;
    private File log2;
    private File log3;

    public void setUp() {
        tag = new CruiseControlTagSupport();
        MockPageContext pageContext = new MockPageContext();
        tag.setPageContext(pageContext);
        request = new MockServletRequest("context", "servlet");
        pageContext.setHttpServletRequest(request);

        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        log1 = new File(logDir, "log1.xml");
        log2 = new File(logDir, "log20040905010203Lsuccessful-build-file.1.xml");
        log3 = new File(logDir, "log3.xml");
    }
    
    public void tearDown() {
        tag = null;
        request = null;
        
        log1.delete();
        log2.delete();
        log3.delete();
        logDir.delete();

        log1 = null;
        log2 = null;
        log3 = null;
        logDir = null;
    }

    public void testCreateUrl() {
        assertEquals("/context/servlet?param=value", tag.createUrl("param", "value"));
    }

    public void testCreateUrlReplacingParam() {
        request.addParameter("param", "differentValue");
        assertEquals("/context/servlet?param=value", tag.createUrl("param", "value"));
    }

    public void testCreateUrlPreservingParam() {
        request.addParameter("otherParam", "otherValue");
        assertEquals("/context/servlet?otherParam=otherValue&param=value", tag.createUrl("param", "value"));
    }

    public void testCreateUrlPreservingAndReplacingParams() {
        request.addParameter("otherParam", "otherValue");
        request.addParameter("param", "differentValue");
        assertEquals("/context/servlet?otherParam=otherValue&param=value", tag.createUrl("param", "value"));
    }

    private void writeFile(File file, String body) throws Exception {
        FileWriter writer = new FileWriter(file);
        writer.write(body);
        writer.close();
    }

}
