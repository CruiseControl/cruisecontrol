package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;

public class UpgraderTest extends TestCase {

    public UpgraderTest(String name) {
        super(name);
    }

    public void testCreateBootstappers() {
        String expected = "<bootstrappers><currentbuildstatusbootstrapper file=\"currentbuildstatus.txt\"/></bootstrappers>";
        Properties properties = new Properties();
        properties.put("currentBuildStatusFile", "currentbuildstatus.txt");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element expectedElement = builder.build(new StringReader(expected)).getRootElement();
            Element actualElement = builder.build(new StringReader(upgrader.createBootstrappers(properties))).getRootElement();
            assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
        } catch (JDOMException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCreateLog() {
        String expected = "<log logdir=\"somelogdirectory\"></log>";
        Properties properties = new Properties();
        properties.put("logDir", "somelogdirectory");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element expectedElement = builder.build(new StringReader(expected)).getRootElement();
            Element actualElement = builder.build(new StringReader(upgrader.createLog(properties))).getRootElement();
            assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
        } catch (JDOMException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCreateLabelIncrementer() {
        String expectedIncrementerSpecified = "<plugin name=\"labelincrementer\" classname=\"somelabelincrementer\"/>";
        String expectedIncrementerNotSpecified = "<plugin name=\"labelincrementer\" classname=\"net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer\"/>";
        Properties propertiesIncrementerSpecified = new Properties();
        propertiesIncrementerSpecified.put("labelIncrementerClass", "somelabelincrementer");

        Properties propertiesIncrementerNotSpecified = new Properties();

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element expectedIncrementerSpecifiedElement = builder.build(new StringReader(expectedIncrementerSpecified)).getRootElement();
            Element actualIncrementerSpecifiedElement = builder.build(new StringReader(upgrader.createLabelIncrementerPlugin(propertiesIncrementerSpecified))).getRootElement();
            assertEquals(outputter.outputString(expectedIncrementerSpecifiedElement), outputter.outputString(actualIncrementerSpecifiedElement));

            Element expectedIncrementerNotSpecifiedElement = builder.build(new StringReader(expectedIncrementerNotSpecified)).getRootElement();
            Element actualIncrementerNotSpecifiedElement = builder.build(new StringReader(upgrader.createLabelIncrementerPlugin(propertiesIncrementerNotSpecified))).getRootElement();
            assertEquals(outputter.outputString(expectedIncrementerNotSpecifiedElement), outputter.outputString(actualIncrementerNotSpecifiedElement));
        } catch (JDOMException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCreatePublishers() {
        StringBuffer expected = new StringBuffer();
        expected.append("<publishers>");
        expected.append("<currentbuildstatuspublisher file=\"currentbuildstatus.txt\"/>");
        expected.append("<email mailhost=\"mail@mail.com\" returnaddress=\"user@host.com\" defaultsuffix=\"@host.com\" buildresultsurl=\"http://host.com\">");
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

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element expectedElement = builder.build(new StringReader(expected.toString())).getRootElement();
            Element actualElement = builder.build(new StringReader(upgrader.createPublishers(properties))).getRootElement();
            assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
        } catch (JDOMException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCreateSchedule() {
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
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element expectedElement = builder.build(new StringReader(expected.toString())).getRootElement();
            Element actualElement = builder.build(new StringReader(upgrader.createSchedule(properties))).getRootElement();
            assertEquals(outputter.outputString(expectedElement), outputter.outputString(actualElement));
        } catch (JDOMException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testFindModificationSet() {
        String xml  = "<project><taskdef name=\"modset\" classname=\"net.sourceforge.cruisecontrol.ModificationSet\"/><target><modset quietperiod=\"15\"><element att1=\"value1\" /></modset></target></project>";
        String xml2 = "<project><target><taskdef name=\"modset\" classname=\"net.sourceforge.cruisecontrol.ModificationSet\"/><modset quietperiod=\"15\"><element att1=\"value1\" /></modset></target></project>";
        String xml3 = "<project><target></target></project>";
        String modset = "<modset quietperiod=\"15\"><element att1=\"value1\" /></modset>";

        //get xml string to element
        Element buildFileElement = null;
        Element buildFileElement2 = null;
        Element buildFileElement3 = null;
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            buildFileElement = builder.build(new StringReader(xml)).getRootElement();
            buildFileElement2 = builder.build(new StringReader(xml2)).getRootElement();
            buildFileElement3 = builder.build(new StringReader(xml3)).getRootElement();
        } catch (JDOMException e) {
            e.printStackTrace();
        }

        Upgrader upgrader = new Upgrader();
        XMLOutputter outputter = new XMLOutputter();
        try {
            assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement)));
            assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement2)));
        } catch (CruiseControlException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        try {
            assertEquals(modset, outputter.outputString(upgrader.findModificationSet(buildFileElement3)));
            assertTrue(false);
        } catch (CruiseControlException e) {
            assertEquals("Could not find a modification set.", e.getMessage());
        }
    }

    public void testExecute() {
        Upgrader upgrader = new Upgrader();

        File tempBuildFile = new File("nosuchbuildfile");
        upgrader.setBuildFile("nosuchbuildfile");
        try {
            upgrader.execute();
            assertTrue(false);
        } catch (Exception e) {
            assertEquals("The specified build file: '" + tempBuildFile.getAbsolutePath() + "' does not exist.", e.getMessage());
        }
        writeFile("_tempBuildFile");
        upgrader.setBuildFile("_tempBuildFile");

        File tempPropertiesFile = new File("nosuchpropertiesfile");
        upgrader.setPropertiesFile("nosuchpropertiesfile");
        try {
            upgrader.execute();
            assertTrue(false);
        } catch (Exception e) {
            assertEquals("The specified properties file: '" + tempPropertiesFile.getAbsolutePath() + "' does not exist.", e.getMessage());
        }
        writeFile("_tempPropertiesFile");
        upgrader.setPropertiesFile("_tempPropertiesFile");

        File tempConfigFile = new File("_tempConfigFile");
        writeFile(tempConfigFile.getName());
        upgrader.setConfigFile("_tempConfigFile");
        try {
            upgrader.execute();
            assertTrue(false);
        } catch (Exception e) {
            assertEquals("The specified configuration file: '" + tempConfigFile.getAbsolutePath() + "' exists.  Delete and try again.", e.getMessage());
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