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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

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
        log1 = new File(logDir, "log1.xml");
        log2 = new File(logDir, "log2.xml");
        log3 = new File(logDir, "log3.xml");

    }

    protected void tearDown() throws Exception {
        log1.delete();
        log2.delete();
        log3.delete();
        logDir.delete();
    }

    public void testGetLatestLog() throws Exception {
        writeFile(log1, "");
        writeFile(log2, "");
        writeFile(log3, "");

        XSLTag tag = new XSLTag();
        File result = tag.getLatestLogFile(logDir);
        assertEquals(result.getName(), "log3.xml");
    }

    public void testTransform() throws Exception {
        final String styleSheetText =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" "
                        + "xmlns:lxslt=\"http://xml.apache.org/xslt\">"
                    + "<xsl:output method=\"text\"/>"
                    + "<xsl:template match=\"/\">"
                        + "<xsl:value-of select=\"test\"/>"
                    + "</xsl:template>"
                + "</xsl:stylesheet>";
        writeFile(log1, styleSheetText);
        writeFile(log3, "<test>3</test>");
        InputStream in = new FileInputStream(log1);
        StringWriter out = new StringWriter();

        XSLTag tag = new XSLTag();
        tag.transform(log3, in, out);
        assertEquals("3", out.toString());
    }

    public void testGetXmlFile() throws Exception {
        writeFile(log1, "");
        writeFile(log3, "");

        XSLTag tag = new XSLTag();
        assertEquals(tag.getXMLFile("", logDir).getName(), "log3.xml");
        assertEquals(tag.getXMLFile("log1", logDir).getName(), "log1.xml");
    }

    public void testGetCachedCopyFileName() {
        XSLTag tag = new XSLTag();
        tag.setXslFile("xsl/cruisecontrol.xsl");
        final String expectedValue = "log20020221120000-cruisecontrol.html";
        assertEquals(expectedValue, tag.getCachedCopyFileName(new File("log20020221120000.xml")));
    }

    /*
public void testIsCachedCopyCurrent() {
    writeFile("testresults/log1.xml", "");
    writeFile("testresults/log2.xml", "");
    writeFile("testresults/log3.xml", "");
    File log1 = new File("testresults/log1.xml");
    File log2 = new File("testresults/log2.xml");
    File log3 = new File("testresults/log3.xml");

    XSLTag tag = new XSLTag();
    assertEquals(true, tag.isCacheFileCurrent(log1, log2, log3));
    assertEquals(false, tag.isCacheFileCurrent(log2, log3, log1));
}
      */
    public void testServeCachedCopy() throws Exception {
        writeFile(log3, "<test></test>");
        StringWriter out = new StringWriter();
        XSLTag tag = new XSLTag();

        tag.serveCachedCopy(log3, out);
        assertEquals("<test></test>", out.toString());
    }

    private void writeFile(File file, String body) throws Exception {
        FileWriter writer = null;
        writer = new FileWriter(file);
        writer.write(body);
        writer.close();
    }
}