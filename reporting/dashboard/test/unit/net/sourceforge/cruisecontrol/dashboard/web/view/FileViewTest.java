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
package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class FileViewTest extends MockObjectTestCase {
    private MockHttpServletResponse mockResponse;

    private MockHttpServletRequest mockRequest;

    private FileView view;

    private Mock mockServletContext;

    protected void setUp() throws Exception {
        super.setUp();
        mockRequest = new MockHttpServletRequest();
        mockResponse = new MockHttpServletResponse();
        mockServletContext = mock(ServletContext.class);
        mockServletContext.expects(once()).method("getInitParameter").will(returnValue(null));
        view = new FileView();
        view.setServletContext((ServletContext) mockServletContext.proxy());
    }

    public void testShouldOutputTxtFileContentAsTextPlain() throws Exception {
        File file = DataUtils.createTempFile("file", "txt");
        Map model = new HashMap();
        model.put("targetFile", file);
        mockServletContext.expects(once()).method("getMimeType").will(
                returnValue("text/plain"));
        view.render(model, mockRequest, mockResponse);
        assertEquals("text/plain", mockResponse.getContentType());
        assertEquals(0, mockResponse.getContentLength());
    }

    public void testShouldSupportSVG() throws Exception {
        File file = DataUtils.createTempFile("coverage", ".svg");
        Map model = new HashMap();
        model.put("targetFile", file);
        mockServletContext.expects(never()).method("getMimeType").will(
                returnValue("text/plain"));
        view.render(model, mockRequest, mockResponse);
        assertEquals("image/svg+xml", mockResponse.getContentType());
        assertEquals(0, mockResponse.getContentLength());
    }

    public void testDefaultContentTypeShouldBeTextPlain() throws Exception {
        mockServletContext.reset();
        assertEquals("application/octet-stream", view.getContentType());
    }

    public void testShouldSetContentDispositionForBigFile() throws Exception {
        File file = new MockFile(DataUtils.createTempFile("file", ".svg"));
        Map model = new HashMap();
        model.put("targetFile", file);
        view.render(model, mockRequest, mockResponse);
        assertEquals("attachment; filename=" + file.getName(), mockResponse
                .getHeader("Content-Disposition"));
    }

    public void testShouldBeAbleToOverrideDownloadThreshhold() throws Exception {
        File file = new MockFile(DataUtils.createTempFile("file", ".svg"));
        Map model = new HashMap();
        model.put("targetFile", file);
        mockServletContext.reset();
        mockServletContext.expects(once()).method("getInitParameter").will(returnValue("5000000"));
        view.render(model, mockRequest, mockResponse);
        assertNull(mockResponse.getHeader("Content-Disposition"));
    }
    
    private static class MockFile extends File {
        public MockFile(File realFile) {
            super(realFile.getPath());
        }

        public long length() {
            return FileView.DEFAULT_DOWNLOAD_THRESHHOLD + 1;
        }
    }
}
