/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.XMLFilter;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:joriskuipers@xs4all.nl">Joris Kuipers</a>
 *
 */
public class EmptyElementFilterTest extends TestCase {

    private SAXBuilder builder;

    protected void setUp() throws Exception {
        builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        XMLFilter emptyTaskFilter = new EmptyElementFilter("task");
        XMLFilter emptyMessageFilter = new EmptyElementFilter("message");
        emptyTaskFilter.setParent(emptyMessageFilter);
        builder.setXMLFilter(emptyTaskFilter);
    }

    public void testFiltering() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<?xml-stylesheet type=\"text/xsl\" href=\"log.xsl\"?>\n\n"
            + "<build time=\"43 seconds\">\n"
              // empty task:
            + "\t<task location=\"nightlybuild.xml:5: \" name=\"property\" time=\"0 seconds\"></task>\n"
            + "\t<target name=\"build\" time=\"43 seconds\">\n"
            + "\t\t<task location=\"nightlybuild.xml:21: \" name=\"exec\" time=\"1 seconds\">\n"
            + "\t\t\t<message priority=\"info\"><![CDATA[Generating buildscript... done]]></message>\n"
              // empty message:
            + "\t\t\t<message priority=\"warn\"><![CDATA[]]></message>\n"
            + "\t\t</task>\n"
            + "\t</target>\n"
            + "</build>\n";

        Document doc = builder.build(new StringReader(xml));
        Writer stringWriter = new StringWriter();
        new XMLOutputter().output(doc, stringWriter);
        String filteredXml = stringWriter.toString();
        assertTrue("empty <task> should have been filtered",
                   filteredXml.indexOf("<task location=\"nightlybuild.xml:5: \"") == -1);
        assertTrue("empty <message> should have been filtered",
                   filteredXml.indexOf("<message priority=\"warn\"") == -1);
        assertTrue("non-empty task should not have been filtered",
                   filteredXml.indexOf("<task location=\"nightlybuild.xml:21: \"") != -1);
        assertTrue("non-empty message should not have been filtered",
                   filteredXml.indexOf("<message priority=\"info\">") != -1);
    }

}
