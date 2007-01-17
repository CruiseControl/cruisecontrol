/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.servlet;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    public void testServiceInvalidFile() throws ServletException, IOException {
        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        final String fileName = "tmp12345.html";
        request.setPathInfo(fileName);

        servlet.service(request, response);
        String actual = response.getWritten();
        String expected = "<html><body><h1>" + fileName + "</h1><h1>Invalid File or Directory</h1></body></html>";
        assertEquals(expected, actual);
        String actualMimeType = response.getContentType();
        assertEquals("text/html", actualMimeType);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    /*
     * Test for void service(HttpServletRequest, HttpServletResponse)
     */
    public void testServiceFile() throws ServletException, IOException {
        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();
        File file = File.createTempFile("tmp", ".html");
        file.deleteOnExit();
        final File dir = file.getParentFile();

        request.setPathInfo(file.getName());
        servlet = new FileServlet() {
            protected File getRootDir(ServletConfig servletconfig) {
                return dir;
            }

            protected String getMimeType(String filename) {
                if (filename.endsWith(".html")) {
                    return "text/html";
                }
                return null;
            }
        };
        final MockServletConfig servletconfig = new MockServletConfig();
        servletconfig.setServletContext(new MockServletContext());
        servlet.init(servletconfig);
        servlet.service(request, response);
        String actual = response.getWritten();
        String expected = "";
        assertEquals(expected, actual);
        String actualMimeType = response.getContentType();
        assertEquals("text/html", actualMimeType);
    }

    /*
     * Test for void service(HttpServletRequest, HttpServletResponse)
     */
    public void testServiceParametrizedMimeType() throws ServletException, IOException {
        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();
        File file = File.createTempFile("tmp", ".html");
        file.deleteOnExit();
        final File dir = file.getParentFile();

        request.setPathInfo(file.getName());
        request.addParameter("mimetype", "text/plain");
        servlet = new FileServlet() {
            protected File getRootDir(ServletConfig servletconfig) {
                return dir;
            }

            protected String getMimeType(String filename) {
                if (filename.endsWith(".html")) {
                    return "text/html";
                }
                return null;
            }
        };
        final MockServletConfig servletconfig = new MockServletConfig();
        servletconfig.setServletContext(new MockServletContext());
        servlet.init(servletconfig);
        servlet.service(request, response);
        String actual = response.getWritten();
        String expected = "";
        assertEquals(expected, actual);
        String actualMimeType = response.getContentType();
        assertEquals("text/plain", actualMimeType);

        request.setPathInfo(file.getName());

    }

    public void testGetIndexes() throws ServletException, IOException {
        MockServletConfig config = new MockServletConfig();
        MockServletContext context = new MockServletContext();
        config.setServletContext(context);

        List indexes;

        // 1- no index defined
        indexes = servlet.getIndexFiles(config);
        assertNotNull(indexes);
        assertEquals(0, indexes.size());

        // 2-
        context.setInitParameter("fileServlet.welcomeFiles", null);
        indexes = servlet.getIndexFiles(config);
        assertNotNull(indexes);
        assertEquals(0, indexes.size());

        // 3-
        context.setInitParameter("fileServlet.welcomeFiles", "");
        indexes = servlet.getIndexFiles(config);
        assertNotNull(indexes);
        assertEquals(0, indexes.size());

        // 4- some indexes defined
        context.setInitParameter("fileServlet.welcomeFiles", "index.htm index.html");
        indexes = servlet.getIndexFiles(config);
        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("index.htm", indexes.get(0));
        assertEquals("index.html", indexes.get(1));

        // 5- resistant to strange spacing
        context.setInitParameter("fileServlet.welcomeFiles", " index.html  index.htm ");
        indexes = servlet.getIndexFiles(config);
        assertNotNull(indexes);
        assertEquals(2, indexes.size());
        assertEquals("index.html", indexes.get(0));
        assertEquals("index.htm", indexes.get(1));
    }

    /** a mock that allows to specify the names of the files returned by {@link #list()}**/
    static class MockWebFile extends WebFile {
        private String[] subFiles;

        public MockWebFile(File root, String path, String[] subFiles) {
            super(root, path);
            this.subFiles = subFiles;
        }

        public String[] list() {
            return subFiles;
        }
    }

    /**
     * Simulates both a request with or without a sessionid attached.
     * @throws IOException
     */
    public void testPrintDirs() throws IOException {
        FileServlet fileServlet = new FileServlet() {
            protected WebFile getSubWebFile(final String subFilePath) {
                return new WebFile(getRootDir(), subFilePath) {
                    public boolean isDir() {
                        String lastPathElt = subFilePath.substring(subFilePath.lastIndexOf('/') + 1);
                        final boolean b = lastPathElt.indexOf(".") == -1;
                        return b;
                    }
                };
            }
        };

        String[] files =
            {
                new String("log1.txt"),
                new String("log2")
            };

        final StringWriter writer1 = new StringWriter();

        final MockServletRequest request1 = new MockServletRequest() {
            public String getRequestURI() {
                return "/artifacts/abc/";
            }
        };
        fileServlet.printDirs(request1, new MockWebFile(new File("notimportant"), "notimportant", files), writer1);

        final String expectedOutput1 =
            "<ul>"
            + "<li><a href=\"/artifacts/abc/log1.txt\">log1.txt</a></li>"
            + "<li><a href=\"/artifacts/abc/log2\">log2/</a></li>"
            + "</ul>";

        assertEquals(expectedOutput1, writer1.getBuffer().toString());


        final StringWriter writer2 = new StringWriter();
        final MockServletRequest request2 = new MockServletRequest() {
            public String getRequestURI() {
                return "/artifacts/abc;jsessionid=012456789ABCDEF";
            }
        };
        fileServlet.printDirs(request2, new MockWebFile(new File("/tmp"), "test", files), writer2);

        final String expectedOutput2 =
            "<ul>"
            + "<li><a href=\"/artifacts/abc/log1.txt;jsessionid=012456789ABCDEF\">log1.txt</a></li>"
            + "<li><a href=\"/artifacts/abc/log2;jsessionid=012456789ABCDEF\">log2/</a></li>"
            + "</ul>";

        assertEquals(expectedOutput2, writer2.getBuffer().toString());
    }

    public void testServiceIndexFile() throws ServletException, IOException {
        MockServletRequest request = new MockServletRequest() {
          public String getRequestURI() {
            return "";
          }
        };
        MyMockServletResponse response1 = new MyMockServletResponse();
        final File dir = new File(System.getProperty("java.io.tmpdir"));

        MyMockFileServlet myServlet = new MyMockFileServlet();
        myServlet.setRootDir(dir);

        MockServletConfig config = new MockServletConfig();
        MockServletContext context = new MockServletContext() {
            public String getMimeType(String s) {
                return "text/html";
            }
        };
        config.setServletContext(context);
        myServlet.init(config);

        File indexFile = new File(dir, "index.html");
        indexFile.deleteOnExit();
        assertFalse("cannot test service index if index.html already exists", indexFile.exists());
        boolean created = indexFile.createNewFile();
        assertTrue(created);

        // redirect when no trailing path, even if no index defined.
        request.setPathInfo("");
        myServlet.service(request, response1);
        response1.ensureRedirect("/");
        assertEquals(0, myServlet.printDirCalls);

        // do not display index if none configured
        myServlet.init();
        MyMockServletResponse response2 = new MyMockServletResponse();
        request.setPathInfo("/");
        myServlet.service(request, response2);
        String actual = response2.getWritten();
        assertTrue(actual.startsWith("<html><body><h1>"));
        assertTrue(myServlet.printDirCalls > 0);
        String actualMimeType = response2.getContentType();
        assertEquals("text/html", actualMimeType);

        // use index if one exists when asking for trailing path and fileServlet.indexFiles configured
        myServlet.init();
        MyMockServletResponse response3 = new MyMockServletResponse();
        context.setInitParameter("fileServlet.welcomeFiles", "index.html");
        config.setServletContext(context);
        myServlet.init(config);

        request.setPathInfo("/");
        myServlet.service(request, response3);
        actual = response3.getWritten();
        String expected = "";
        assertEquals(expected, actual);
        actualMimeType = response3.getContentType();
        assertEquals("text/html", actualMimeType);
        assertEquals(0, myServlet.printDirCalls);
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

    static class MyMockServletResponse extends MockServletResponse {
        private String redirected;
        public void sendRedirect(String arg0) throws IOException {
            redirected = arg0;
        }

        public void ensureRedirect(String expectedRedirect) {
            assertEquals(expectedRedirect, redirected);
        }

        public String encodeRedirectURL(String arg0) {
          return arg0;
        }
    }

    static class MyMockFileServlet extends FileServlet {
        private File rootDir;
        private int printDirCalls;

        public void init() {
            printDirCalls = 0;
        }

        public void setRootDir(File rootDir) {
            this.rootDir = rootDir;
        }

        protected File getRootDir(ServletConfig servletconfig) {
            return rootDir;
        }

        void printDirs(HttpServletRequest request, WebFile file, Writer writer) throws IOException {
            super.printDirs(request, file, writer);
            printDirCalls++;
        }
    }
}
