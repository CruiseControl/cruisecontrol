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

package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class LogMergerTest extends TestCase {

    public LogMergerTest(String name) {
        super(name);
    }

    public void setUp() {
        //set up PropertyConfigurator so log4j works
        //create a couple xml files
        BufferedWriter bw1, bw2, bw3;
        try {
            bw1 = new BufferedWriter(new FileWriter(new File("_tempXMLDocument1.xml")));
            bw2 = new BufferedWriter(new FileWriter(new File("_tempXMLDocument2.xml")));
            bw3 = new BufferedWriter(new FileWriter(new File("_tempXMLDocument3.xml")));
            bw1.write("<xml><junk type=\"junk\"/></xml>");
            bw2.write("");
            bw3.write("");
            bw1.flush();
            bw2.flush();
            bw3.flush();
            bw1.close();
            bw2.close();
            bw3.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        } finally {
            bw1 = null;
            bw2 = null;
            bw3 = null;
        }
    }

    public void tearDown() {
        File f1 = new File("_tempXMLDocument1.xml");
        File f2 = new File("_tempXMLDocument2.xml");
        File f3 = new File("_tempXMLDocument3.xml");
        f1.delete();
        f2.delete();
        f3.delete();
    }

    public void testGetLogs() {
        //add newly created files to AdditionalLogs
        LogMerger merger = new LogMerger();
        merger.addLog("_tempXMLDocument1.xml");
        Iterator additionalLogs = merger.getLogs().iterator();
        Element logElement = (Element) additionalLogs.next();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element e = null;
        try {
            e = builder.build("_tempXMLDocument1.xml").getRootElement();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //check that List returned matches contents of files
        assertEquals(logElement.toString(), e.toString());
    }

    public void testGetFileAsElement() {
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element e = null;
        try {
            e = builder.build("_tempXMLDocument1.xml").getRootElement();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        LogMerger merger = new LogMerger();
        Element logElement = merger.getFileAsElement(new File("_tempXMLDocument1.xml"));
        assertEquals(logElement.toString(), e.toString());

        //test nonxml file
        //test nonexistent file
    }
}
