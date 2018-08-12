/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.config.XMLConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.xml.sax.SAXException;

public class UtilTest extends TestCase {

    private File propsFile;
    private Properties testProps;

    public void setUp() throws Exception {

        //Create a properties file to test properties loading
        propsFile = File.createTempFile("testload", "properties");
        propsFile.deleteOnExit();
        StringBuffer props = new StringBuffer();
        props.append("#Test properties file\n");
        props.append("property1=value1\n");
        props.append("property2 = value2\n");
        props.append("property3=value3\n");
        IO.write(propsFile, props.toString());

        //Create a Properties object to test storing properties
        testProps = new Properties();
        testProps.setProperty("stored1", "value1");
        testProps.setProperty("stored2", "value2");
    }

    public void testLoadPropertiesFromFileMustExist() {
        File file = new File("NoSuchFile");
        try {
            Util.loadPropertiesFromFile(file);
            fail("A non-existant properties file should cause an exception!");
        } catch (Exception e) {
        }
    }

    public void testLoadPropertiesFromFile() throws CruiseControlException,
            IOException {
        Properties properties = Util.loadPropertiesFromFile(propsFile);
        assertEquals(3, properties.size());
        assertEquals("value1", properties.getProperty("property1"));
        assertEquals("value2", properties.getProperty("property2"));
        assertEquals("value3", properties.getProperty("property3"));
    }

    public void testStorePropertiesToFile() throws CruiseControlException,
            IOException {
        File file = File.createTempFile("teststore", "properties");
        file.deleteOnExit();
        Util.storePropertiesToFile(testProps, "Sample Header", file);
        Properties properties = Util.loadPropertiesFromFile(file);
        assertEquals(2, properties.size());
        assertEquals("value1", properties.getProperty("stored1"));
        assertEquals("value2", properties.getProperty("stored2"));
    }

    @SuppressWarnings("unused")
    public void testLoadRootElement_xinclude() throws CruiseControlException,
    IOException, SAXException, JDOMException {
        File file1 = File.createTempFile("file1", ".xml");
        file1.deleteOnExit();
        StringBuffer xml1 = new StringBuffer();
        xml1.append("<?xml version='1.0'?>\n");
        xml1.append("<!DOCTYPE data [");
        xml1.append("<!ELEMENT data (property,plugin)>\n");
        xml1.append("<!ATTLIST property id ID #REQUIRED>\n");
        xml1.append("<!ATTLIST plugin   id ID #REQUIRED>\n");
        xml1.append("]>\n");
        xml1.append("<data id=''>\n");
        xml1.append("<property name='p1' value='v1' id='prop'/>\n");
        xml1.append("<property name='p2' value='v2' id='prop'/>\n");
        xml1.append("<plugin name='schedule' timeout='60' id='sched'/>\n");
        xml1.append("</data>\n");
        IO.write(file1, xml1.toString());

        File file2 = File.createTempFile("file2", ".xml");
        file2.deleteOnExit();
        StringBuffer xml2 = new StringBuffer();
        xml2.append("<?xml version='1.0'?>\n");
        xml2.append("<cruisecontrol>\n");
        xml2.append("<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='" + file1.getPath() + "' xpointer='element(prop)'/>\n");
        xml2.append("<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='" + file1.getPath() + "' xpointer='element(sched)'/>\n");
        xml2.append("<project name='pr'>\n");
        xml2.append("</project>\n");
        xml2.append("</cruisecontrol>\n");
        IO.write(file2, xml2.toString());

        // Read and test
        Element test = Util.loadRootElement(file2);

        assertNotNull(XPath.newInstance("/cruisecontrol/property[@name='p1' and @value='v1']").selectSingleNode(test));
        assertNotNull(XPath.newInstance("/cruisecontrol/property[@name='p2' and @value='v2']").selectSingleNode(test));
        assertNotNull(XPath.newInstance("/cruisecontrol/plugin[@name='schedule']").selectSingleNode(test));

        // For debug printing ...
        //String xmlString = (new XMLOutputter(Format.getPrettyFormat())).outputString(test);
        //System.out.println(xmlString);
     }
}
