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
package net.sourceforge.cruisecontrol.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class UpgraderTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(UpgraderTest.class);

    public UpgraderTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        LOG.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void testCreateBootstappers() throws Exception {
        String expected =
            "<bootstrappers><currentbuildstatusbootstrapper file=\"currentbuildstatus.txt\"/></bootstrappers>";
        Properties properties = new Properties();
        properties.put("currentBuildStatusFile", "currentbuildstatus.txt");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected)).getRootElement();
        Element actualElement =
            builder
                .build(
                    new StringReader(upgrader.createBootstrappers(properties)))
                .getRootElement();

        assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
    }

    public void testCreateLog() throws Exception {
        String expected = "<log dir=\"somelogdirectory\"></log>";
        Properties properties = new Properties();
        properties.put("logDir", "somelogdirectory");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected)).getRootElement();
        Element actualElement = builder.build(new StringReader(upgrader.createLog(properties))).getRootElement();

        assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
    }

    public void testCreateLabelIncrementer() throws Exception {
        String expectedIncrementerSpecified =
            "<plugin name=\"labelincrementer\" classname=\"somelabelincrementer\"/>";
        Properties propertiesIncrementerSpecified = new Properties();
        propertiesIncrementerSpecified.put("labelIncrementerClass", "somelabelincrementer");

        Properties propertiesIncrementerNotSpecified = new Properties();

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedIncrementerSpecifiedElement =
            builder
                .build(new StringReader(expectedIncrementerSpecified))
                .getRootElement();
        Element actualIncrementerSpecifiedElement =
            builder
                .build(
                    new StringReader(
                        upgrader.createLabelIncrementerPlugin(
                            propertiesIncrementerSpecified)))
                .getRootElement();

        assertEquals(
            outputter.outputString(expectedIncrementerSpecifiedElement),
            outputter.outputString(actualIncrementerSpecifiedElement));
        assertEquals("", upgrader.createLabelIncrementerPlugin(propertiesIncrementerNotSpecified));
    }

    public void testCreatePublishersNoEmailMap() throws Exception {
        StringBuffer expected = new StringBuffer();
        expected.append("<publishers>");
        expected.append("<currentbuildstatuspublisher file=\"currentbuildstatus.txt\"/>");
        expected.append(
            "<email mailhost=\"mail@mail.com\" returnaddress=\"user@host.com\""
                + " defaultsuffix=\"@host.com\" buildresultsurl=\"http://host.com\">");
        expected.append("<always address=\"user1@host.com\"/>");
        expected.append("<always address=\"user2@host.com\"/>");
        expected.append("<failure address=\"user3@host.com\"/>");
        expected.append("</email>");
        expected.append("</publishers>");

        Properties properties = new Properties();
        properties.put("currentBuildStatusFile", "currentbuildstatus.txt");
        properties.put("mailhost", "mail@mail.com");
        properties.put("returnAddress", "user@host.com");
        properties.put("defaultEmailSuffix", "@host.com");
        properties.put("servletURL", "http://host.com");
        properties.put("buildmaster", "user1@host.com, user2@host.com");
        properties.put("notifyOnFailure", "user3@host.com");
        properties.put("emailmap", "");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected.toString())).getRootElement();
        Element actualElement =
            builder
                .build(new StringReader(upgrader.createPublishers(properties)))
                .getRootElement();

        assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
    }

    public void testCreatePublishersEmailMap() throws JDOMException {
        StringBuffer expected = new StringBuffer();
        expected.append("<publishers>");
        expected.append("<currentbuildstatuspublisher "
                + "file=\"currentbuildstatus.txt\"/>");
        expected.append("<email mailhost=\"mail@mail.com\" "
                + "returnaddress=\"user@host.com\" defaultsuffix=\"@host.com\" "
                + "buildresultsurl=\"http://host.com\">");
        expected.append("<always address=\"user1@host.com\"/>");
        expected.append("<always address=\"user2@host.com\"/>");
        expected.append("<failure address=\"user3@host.com\"/>");
        expected.append("<map alias=\"user1\" address=\"user1@host.com\"/>");
        expected.append("</email>");
        expected.append("</publishers>");

        Properties properties = new Properties();
        properties.put("currentBuildStatusFile", "currentbuildstatus.txt");
        properties.put("mailhost", "mail@mail.com");
        properties.put("returnAddress", "user@host.com");
        properties.put("defaultEmailSuffix", "@host.com");
        properties.put("servletURL", "http://host.com");
        properties.put("buildmaster", "user1@host.com, user2@host.com");
        properties.put("notifyOnFailure", "user3@host.com");
        properties.put("emailmap",
                "test/net/sourceforge/cruisecontrol/util/emailmap.properties");

        SAXBuilder builder = new SAXBuilder(
                "org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(
                new StringReader(expected.toString())).getRootElement();

        Upgrader upgrader = new Upgrader();
        Element actualElement = builder.build(new StringReader(
                upgrader.createPublishers(properties))).getRootElement();

        XMLOutputter outputter = new XMLOutputter();
        assertEquals(outputter.outputString(expectedElement),
                outputter.outputString(actualElement));
    }

    public void testCreateSchedule() throws Exception {
        StringBuffer expected = new StringBuffer();
        expected.append("<schedule interval=\"15\" intervaltype=\"absolute\">");
        expected.append("<ant buildfile=\"build.xml\" target=\"cleantarget\" multiple=\"5\"/>");
        expected.append("<ant buildfile=\"build.xml\" target=\"defaulttarget\" multiple=\"1\"/>");
        expected.append("</schedule>");

        Properties properties = new Properties();
        properties.put("buildinterval", "15");
        properties.put("absoluteInterval", "true");
        properties.put("antfile", "build.xml");
        properties.put("target", "defaulttarget");
        properties.put("cleantarget", "cleantarget");
        properties.put("cleanBuildEvery", "5");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected.toString())).getRootElement();
        Element actualElement = builder.build(new StringReader(upgrader.createSchedule(properties))).getRootElement();

        assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));

    }

    public void testFindModificationSet() throws Exception {
        String xml =
            "<project><taskdef name=\"modset\" classname=\"net.sourceforge."
                + "cruisecontrol.ModificationSet\"/><target><modset quietperiod"
                + "=\"15\"><vsselement att1=\"value1\" /></modset></target></project>";
        String xml2 =
            "<project><target><taskdef name=\"modset\" classname=\"net."
                + "sourceforge.cruisecontrol.ModificationSet\"/><modset "
                + "quietperiod=\"15\"><vsselement att1=\"value1\" /></modset></target></project>";
        String xml3 = "<project><target></target></project>";
        String modset = "<modset quietperiod=\"15\"><vsselement att1=\"value1\" /></modset>";

        //get xml string to element
        Element buildFileElement = null;
        Element buildFileElement2 = null;
        Element buildFileElement3 = null;

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        buildFileElement = builder.build(new StringReader(xml)).getRootElement();
        buildFileElement2 = builder.build(new StringReader(xml2)).getRootElement();
        buildFileElement3 = builder.build(new StringReader(xml3)).getRootElement();


        Upgrader upgrader = new Upgrader();
        XMLOutputter outputter = new XMLOutputter();

        assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement)));
        assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement2)));

        try {
            assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement3)));
            fail("Expected CruiseControlException, but didn't get it.");
        } catch (CruiseControlException e) {
            // expected behavior
            assertEquals("Could not find a modification set.", e.getMessage());
        }
    }

    public void testCreateModificationSet() throws Exception {
        String oldModSet = "<modset lastbuild=\"\" quietperiod=\"15\"><starteamelement att1=\"value1\" /></modset>";
        String newModSet = "<modificationset quietperiod=\"15\"><starteam att1=\"value1\" /></modificationset>";

        String oldModSetVss = "<modset lastbuild=\"\" quietperiod=\"15\"><vsselement ssdir=\"value1\" /></modset>";
        String newModSetVss = "<modificationset quietperiod=\"15\"><vss vsspath=\"value1\" /></modificationset>";
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element modSetElement = builder.build(new StringReader(oldModSet)).getRootElement();
        Element modSetElementVss = builder.build(new StringReader(oldModSetVss)).getRootElement();

        Upgrader upgrader = new Upgrader();

        assertEquals(newModSet, upgrader.createModificationSet(modSetElement));
        assertEquals(newModSetVss, upgrader.createModificationSet(modSetElementVss));
    }

    public void testExecute() throws Exception {
        Upgrader upgrader = new Upgrader();

        File tempBuildFile = new File("nosuchbuildfile");
        upgrader.setBuildFile(tempBuildFile);
        File tempPropertiesFile = new File("nosuchpropertiesfile");
        upgrader.setPropertiesFile(tempPropertiesFile);
        File tempConfigFile = new File("_tempConfigFile");
        upgrader.setConfigFile(tempConfigFile);
        upgrader.setProjectName("someproject");

        try {
            upgrader.execute();
            fail("Expected CruiseControlException, but didn't get it");
        } catch (CruiseControlException e) {
            // expected behavior
            assertEquals(
                "The specified build file: '"
                    + tempBuildFile.getAbsolutePath()
                    + "' does not exist.",
                e.getMessage());
        }
        writeFile("_tempBuildFile");
        upgrader.setBuildFile(new File("_tempBuildFile"));

        try {
            upgrader.execute();
            fail("Expected CruiseControlException, but didn't get it");
        } catch (Exception e) {
            // expected behavior
            assertEquals(
                "The specified properties file: '"
                    + tempPropertiesFile.getAbsolutePath()
                    + "' does not exist.",
                e.getMessage());
        }
        writeFile("_tempPropertiesFile");
        upgrader.setPropertiesFile(new File("_tempPropertiesFile"));
        
        // we create this file because the assertion is that it doesn't exist.
        writeFile(tempConfigFile.getName());
        try {
            upgrader.execute();
            fail("Expected CruiseControlException, but didn't get it");
        } catch (Exception e) {
            // expected behavior
            assertEquals(
                "The specified configuration file: '"
                    + tempConfigFile.getAbsolutePath()
                    + "' exists.  Delete and try again.",
                e.getMessage());
        }
    }

    private void writeFile(String filename) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(filename));
            bw.write(" ");
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            bw = null;
        }
    }
    
}