/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletContext;
import net.sourceforge.cruisecontrol.mock.MockServletRequest;
import net.sourceforge.cruisecontrol.mock.MockServletResponse;

/**
 * @author jfredrick
 */
public class FileServletTest extends TestCase {

    private FileServlet servlet;

    public FileServletTest(String testName) {
        super(testName);
    }

    protected void setUp() {
        servlet = new FileServlet();
    }

    protected void tearDown() {
        servlet = null;
    }

    public void testGetRootDir() {
        MockServletConfig config = new MockServletConfig();
        MockServletContext context = new MockServletContext();
        config.setServletContext(context);
        
        try {
            servlet.getRootDir(config);
            fail("should have exception when required attributes not set");
        } catch (ServletException e) {
        }

        config.setInitParameter("rootDir", ".");
        try {
            servlet.getRootDir(config);
        } catch (ServletException e) {
            fail("shouldn't throw exception when valid rootDir parameter set");
        }
        
        context.setInitParameter("logDir", "this directory does not exist");
        try {
            servlet.getRootDir(config);
        } catch (ServletException e) {
            fail("good rootDir but bad logDir should work");
        }
        
        config = new MockServletConfig();
        context = new MockServletContext();
        config.setServletContext(context);

        context.setInitParameter("logDir", ".");
        try {
            servlet.getRootDir(config);
        } catch (ServletException e) {
            fail("shouldn't throw exception when valid logDir parameter set");
        }
        
        config.setInitParameter("rootDir", "this directory does not exist");
        try {
            servlet.getRootDir(config);
        } catch (ServletException e) {
            fail("bad rootDir but good logDir should work");
        }
    }

    /*
     * Test for void service(HttpServletRequest, HttpServletResponse)
     */
    public void testService() throws ServletException, IOException {
        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();
        File file = File.createTempFile("tmp", ".html");
        file.deleteOnExit();
        final File dir = file.getParentFile();
        request.setPathInfo(file.getName());
        servlet = new FileServlet() {
            File getRootDir(ServletConfig servletconfig) {
                return dir;
            }
        };
        servlet.service(request, response);
        String actual = response.getWritten();
        String expected = "<html><body><h1>" + file.getName() + "</h1><h1>Invalid File or Directory</h1></body></html>";
        assertEquals(expected, actual);
        String actualMimeType = response.getContentType();
        assertEquals("text/html", actualMimeType);
    }

    public void testGetMimeType() {
        MockServletContext context = new MockServletContext() {
            public String getMimeType(String s) {
                return "text/html";
            }
        };
        servlet = new TestFileServlet(context);
        assertEquals("text/html", servlet.getMimeType(""));

        context = new MockServletContext() {
            public String getMimeType(String s) {
                return null;
            }
        };
        servlet = new TestFileServlet(context);
        assertEquals("text/plain", servlet.getMimeType(""));
    }

    private final class TestFileServlet extends FileServlet {
        private MockServletContext context;

        private TestFileServlet(MockServletContext msc) {
            super();
            context = msc;
        }

        public ServletContext getServletContext() {
            return context;
        }
    }

}
