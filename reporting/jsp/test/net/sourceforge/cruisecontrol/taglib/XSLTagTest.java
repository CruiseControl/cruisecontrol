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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.LogFile;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletConfig;
import net.sourceforge.cruisecontrol.mock.MockServletContext;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

public class XSLTagTest extends TestCase {

    private File logDir;
    private File log1;
    private File log2;
    private File log3;

    public XSLTagTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        logDir = new File("testresults/");
        if (!logDir.exists()) {
            assertTrue("Failed to create test result dir", logDir.mkdir());
        }
        log1 = new File(logDir, "log20040903010203.xml");
        log2 = new File(logDir, "log20040905010203.xml");
        log3 = new File(logDir, "log20051021103500.xml");
    }

    protected void tearDown() throws Exception {
        log1.delete();
        log2.delete();
        log3.delete();
        logDir.delete();
    }

    public void testTransform() throws Exception {
        final String styleSheetText =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" "
                        + "xmlns:lxslt=\"http://xml.apache.org/xslt\">"
                    + "<xsl:output method=\"text\"/>"
                    + "<xsl:template match=\"/\">"
                        +  "<xsl:apply-templates />"
                    + "</xsl:template>"
                    + "<xsl:template match=\"test\" >"
                        + "test=<xsl:value-of select=\"/\" />.<xsl:value-of select=\"@sub\" />"
                    + "</xsl:template>"
                + "</xsl:stylesheet>";
        IO.write(log1, styleSheetText);
        IO.write(log3, "<test sub=\"1\">3</test>");
        OutputStream out = new ByteArrayOutputStream();

        XSLTag tag = createXSLTag();
        tag.transform(new LogFile(log3), log1.toURI().toURL(), out);
        assertEquals("test=3.1", out.toString());
    }

    public void testTransformNested() throws Exception {
        final String innerStyleSheetText =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" "
                        + "xmlns:lxslt=\"http://xml.apache.org/xslt\">"
                    + "<xsl:template match=\"test\" >"
                        + "test=<xsl:value-of select=\"/\" />.<xsl:value-of select=\"@sub\" />"
                    + "</xsl:template>"
                + "</xsl:stylesheet>";
        IO.write(log1, innerStyleSheetText);
        final String outerStyleSheetText =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" "
                        + "xmlns:lxslt=\"http://xml.apache.org/xslt\">"
                    + "<xsl:output method=\"text\"/>"
                    + "<xsl:include href=\"" + log1.getName() + "\" />"
                    + "<xsl:template match=\"/\">"
                        +  "<xsl:apply-templates />"
                    + "</xsl:template>"
                + "</xsl:stylesheet>";
        IO.write(log2, outerStyleSheetText);
        IO.write(log3, "<test sub=\"1\">3</test>");
        OutputStream out = new ByteArrayOutputStream();

        XSLTag tag = createXSLTag();

        tag.transform(new LogFile(log3), log2.toURI().toURL(), out);
        assertEquals("test=3.1", out.toString());
    }
    
    public void testTransformUTF8() throws Exception {
         final String styleSheetText =
                 "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" "
                         + "xmlns:lxslt=\"http://xml.apache.org/xslt\">"
                     + "<xsl:output method=\"text\"/>"
                     + "<xsl:template match=\"/\">"
                         + "<xsl:value-of disable-output-escaping=\"yes\" select=\"'&#198;&#216;&#197;'\"/>"
                     + "</xsl:template>"
                 + "</xsl:stylesheet>";
         IO.write(log1, styleSheetText);
         IO.write(log2, "<test sub=\"1\">3</test>");

         XSLTag tag = createXSLTag();
         tag.setXslFile(log1.getName());
         tag.updateCacheFile(new LogFile(log2), log3);
         Writer writer = new CharArrayWriter();
         tag.serveCachedCopy(log3, writer);
         assertEquals("\u00c6\u00d8\u00c5", writer.toString());
     }

    public void testGetCachedCopyFileName() {
        XSLTag tag = createXSLTag();
        tag.setXslFile("xsl/cruisecontrol.xsl");
        final String expectedValue = "log20020221120000-cruisecontrol.html";
        assertEquals(expectedValue, tag.getCachedCopyFileName(new File("log20020221120000.xml")));
    }

    public void testIsNoCacheCurrent() throws Exception {
        IO.write(log1, "");

        XSLTag tag = createXSLTag();
        assertFalse(tag.isCacheFileCurrent(log1, log2));
    }

    public void testIsEmptyCacheCurrent() throws Exception {
        IO.write(log1, "");
        IO.write(log2, "");

        XSLTag tag = createXSLTag();
        assertFalse(tag.isCacheFileCurrent(log1, log2));
    }

    // NOTE: Testing if a the XSL file is newer than the cache or XML log file
    // cannot use newly created files because of the timestamp granularity of
    // the filesystem which can be as high as 2 seconds

    public void testServeCachedCopy() throws Exception {
        IO.write(log3, "<test></test>");
        StringWriter out = new StringWriter();
        XSLTag tag = createXSLTag();

        tag.serveCachedCopy(log3, out);
        assertEquals("<test></test>", out.toString());
    }

    public void testGetXSLTParameters() throws Exception {
        final String styleSheetText =
                 "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
                     + "<xsl:output method=\"text\"/>"
                     + "<xsl:param name=\"context.parameter\"/>"
                     + "<xsl:param name=\"config.parameter\"/>"
                     + "<xsl:template match=\"/\">"
                         + "<xsl:value-of select=\"$config.parameter\"/>"
                         + "<xsl:value-of select=\"$context.parameter\"/>"
                     + "</xsl:template>"
                 + "</xsl:stylesheet>";
        IO.write(log1, styleSheetText);
        IO.write(log2, "<test/>");

        XSLTag tag = createXSLTag();
        MockServletConfig config = (MockServletConfig) tag.getPageContext().getServletConfig();
        config.setInitParameter("xslt.config.parameter", "config.value");
        MockServletContext context = (MockServletContext) tag.getPageContext().getServletContext();
        context.setInitParameter("xslt.context.parameter", "context.value");
        tag.setXslFile(log1.getName());
        tag.updateCacheFile(new LogFile(log2), log3);
        Writer writer = new CharArrayWriter();
        tag.serveCachedCopy(log3, writer);
        assertEquals("config.valuecontext.value", writer.toString());
    }

    private XSLTag createXSLTag() {
        XSLTag tag = new XSLTag();
        final MockPageContext pageContext = new MockPageContext();
        final MockServletContext servletContext = (MockServletContext) pageContext.getServletContext();
        servletContext.setBaseResourceDir(logDir);
        tag.setPageContext(pageContext);
        return tag;
    }
}
