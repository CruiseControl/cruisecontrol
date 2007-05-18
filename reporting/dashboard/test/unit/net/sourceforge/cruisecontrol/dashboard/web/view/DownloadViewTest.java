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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class DownloadViewTest extends MockObjectTestCase {
    private Mock mockResponse;
    private Mock mockRequest;
    private DownloadView view;
    private Mock mockOutputStream;


    protected void setUp() throws Exception {
        super.setUp();
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
        mockOutputStream = mock(ServletOutputStream.class);

        view = new DownloadView();
    }

    public void testShouldOutputFileContentAsTextXml() throws Exception {
        File file = DataUtils.createTempFile("file", "ext");
        Map model = new HashMap();
        model.put("targetFile", file);

        mockResponse.expects(once()).method("setContentType").with(eq("text/xml"));
        mockResponse.expects(once()).method("setContentLength").with(eq(file.length(), 0));
        mockResponse.expects(once()).method("addHeader");
        mockResponse.expects(once()).method("getOutputStream").withNoArguments()
                .will(returnValue(mockOutputStream.proxy()));
        mockOutputStream.expects(once()).method("write").withAnyArguments();
        mockOutputStream.expects(once()).method("flush").withNoArguments();


        view.render(model, (HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    public void testContentTypeShouldBeXml() throws Exception {
        assertEquals("text/xml", view.getContentType());
    }
}
