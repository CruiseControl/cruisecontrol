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
package net.sourceforge.cruisecontrol;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import junit.framework.TestCase;

public class LogFileReaderTest extends TestCase {

    private LogFileReader reader;

    protected void setUp() throws Exception {
        super.setUp();
        Document document = createValidDocument();
        reader = new LogFileReader(document);
        
    }


    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLogFileReader() {
        assertNotNull("reader should have been constructed", reader);
    }

    
    /**
     * test that we successufully get the build interval
     */
    public void testGetBuildInterval() {
        assertEquals("Build interval", 100, reader.getBuildInterval());
    }
    
    /**
     * test that we get an interval of 0 if the property is not found
     * @throws IOException 
     */
    public void testGetBuildIntervalInvalid() throws IOException {
        LogFileReader logFileReader = new LogFileReader(createInvalidDocument());
        assertEquals("Build interval", 0, logFileReader.getBuildInterval());
    }
    
    

    private Document createValidDocument() throws IOException {
        Element root = new Element("cruisecontrol");
        Element info = new Element("info");
        root.addContent(info);
        Element property = new Element("property");
        property.setAttribute("name", "anotherproperty");
        property.setAttribute("value", "astring");
        info.addContent(property);
        
        Element property2 = new Element("property");
        property2.setAttribute("name", "interval");
        property2.setAttribute("value", "100");
        info.addContent(property2);
        
        Document document = new Document(root);
        XMLOutputter outputter = new XMLOutputter();
        outputter.output(document, System.out);
        return document;
    }
    
    private Document createInvalidDocument() throws IOException {
        Element root = new Element("cruisecontrol");
        Element info = new Element("info");
        root.addContent(info);
        Element property = new Element("property");
        property.setAttribute("name", "notinterval");
        property.setAttribute("value", "100");
        info.addContent(property);
        
        Document document = new Document(root);
        XMLOutputter outputter = new XMLOutputter();
        outputter.output(document, System.out);
        return document;
    }

}
