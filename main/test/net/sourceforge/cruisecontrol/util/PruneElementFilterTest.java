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

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:joriskuipers@xs4all.nl">Joris Kuipers</a>
 *
 */
public class PruneElementFilterTest extends TestCase {

    private SAXBuilder builder;

    protected void setUp() throws Exception {
        builder = new SAXBuilder();
        builder.setXMLFilter(new PruneElementFilter("properties"));
    }

    public void testFiltering() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                   + "<testsuites><testsuite><properties>"
                   + "<property/><property id=\"foo\">With some text</property>"
                   + "</properties><othertag/></testsuite></testsuites>";
        Document doc = builder.build(new StringReader(xml));
        Writer stringWriter = new StringWriter();
        new XMLOutputter().output(doc, stringWriter);
        String filteredXml = stringWriter.toString();
        assertTrue("<properties>-element should have been filtered",
                   filteredXml.indexOf("<properties>") == -1);
        assertTrue("<property>-elements should have been filtered",
                   filteredXml.indexOf("<property>") == -1);
        assertTrue("<testsuite>-element should not have been filtered",
                   filteredXml.indexOf("<testsuite>") != -1);
        assertTrue("<othertag>-element should not have been filtered",
                filteredXml.indexOf("<othertag") != -1);
    }
}
