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

import junit.framework.TestCase;

import java.io.*;

public class XSLTagTest extends TestCase {

    public XSLTagTest(String name) {
        super(name);
    }

    public void testGetLatestLog() {
        writeFile("testresults/log1.xml", "");
        writeFile("testresults/log2.xml", "");
        writeFile("testresults/log3.xml", "");

        XSLTag tag = new XSLTag();
        File result = tag.getLatestLogFile(new File("testresults"));
        assertEquals(result.getName(), "log3.xml");
    }

    public void testTransform() {
        writeFile("testresults/test.xsl", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:lxslt=\"http://xml.apache.org/xslt\"><xsl:output method=\"text\"/><xsl:template match=\"/\"><xsl:value-of select=\"test\"/></xsl:template></xsl:stylesheet>");
        writeFile("testresults/log3.xml", "<test>3</test>");
        InputStream in = null;
        try {
            in = new FileInputStream("testresults/test.xsl");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringWriter out = new StringWriter();

        XSLTag tag = new XSLTag();
        tag.transform(new File("testresults/log3.xml"), in, out);
        assertEquals(out.toString(),"3");
    }

    public void testGetXmlFile() {
        writeFile("testresults/log1.xml", "");
        writeFile("testresults/log2.xml", "");
        writeFile("testresults/log3.xml", "");

        XSLTag tag = new XSLTag();
        assertEquals(tag.getXMLFile("", new File("testresults")).getName(), "log3.xml");
        assertEquals(tag.getXMLFile("log1", new File("testresults")).getName(), "log1.xml");
    }

    public void testGetCachedCopyFileName() {
        XSLTag tag = new XSLTag();
        tag.setXslFile("xsl/cruisecontrol.xsl");
        assertEquals("log20020221120000-cruisecontrol.html", tag.getCachedCopyFileName(new File("log20020221120000.xml")));
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
    public void testServeCachedCopy() {
        writeFile("testresults/log3.xml", "<test></test>");
        StringWriter out = new StringWriter();
        XSLTag tag = new XSLTag();

        tag.serveCachedCopy(new File("testresults/log3.xml"), out);
        assertEquals("<test></test>", out.toString());
    }

    private void writeFile(String fileName, String body) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            writer.write(body);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writer = null;
        }
    }
}