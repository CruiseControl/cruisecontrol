package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class UpgraderTest extends TestCase {
    private static Logger log = Logger.getLogger(UpgraderTest.class);

    public UpgraderTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
        log.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void testCreateBootstappers() throws Exception {
        String expected = "<bootstrappers><currentbuildstatusbootstrapper file=\"currentbuildstatus.txt\"/></bootstrappers>";
        Properties properties = new Properties();
        properties.put("currentBuildStatusFile", "currentbuildstatus.txt");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected)).getRootElement();
        Element actualElement = builder.build(new StringReader(upgrader.createBootstrappers(properties))).getRootElement();

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
        String expectedIncrementerSpecified = "<plugin name=\"labelincrementer\" classname=\"somelabelincrementer\"/>";
        String expectedIncrementerNotSpecified = "<plugin name=\"labelincrementer\" classname=\"net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer\"/>";
        Properties propertiesIncrementerSpecified = new Properties();
        propertiesIncrementerSpecified.put("labelIncrementerClass", "somelabelincrementer");

        Properties propertiesIncrementerNotSpecified = new Properties();

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedIncrementerSpecifiedElement = builder.build(new StringReader(expectedIncrementerSpecified)).getRootElement();
        Element actualIncrementerSpecifiedElement = builder.build(new StringReader(upgrader.createLabelIncrementerPlugin(propertiesIncrementerSpecified))).getRootElement();

        assertEquals(outputter.outputString(expectedIncrementerSpecifiedElement), outputter.outputString(actualIncrementerSpecifiedElement));

        Element expectedIncrementerNotSpecifiedElement = builder.build(new StringReader(expectedIncrementerNotSpecified)).getRootElement();
        Element actualIncrementerNotSpecifiedElement = builder.build(new StringReader(upgrader.createLabelIncrementerPlugin(propertiesIncrementerNotSpecified))).getRootElement();

        assertEquals(outputter.outputString(expectedIncrementerNotSpecifiedElement), outputter.outputString(actualIncrementerNotSpecifiedElement));

    }

    public void testCreatePublishersNoEmailMap() throws Exception {
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
        properties.put("emailmap", "");

        XMLOutputter outputter = new XMLOutputter();
        Upgrader upgrader = new Upgrader();

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Element expectedElement = builder.build(new StringReader(expected.toString())).getRootElement();
        Element actualElement = builder.build(new StringReader(upgrader.createPublishers(properties))).getRootElement();

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
        String xml = "<project><taskdef name=\"modset\" classname=\"net.sourceforge.cruisecontrol.ModificationSet\"/><target><modset quietperiod=\"15\"><element att1=\"value1\" /></modset></target></project>";
        String xml2 = "<project><target><taskdef name=\"modset\" classname=\"net.sourceforge.cruisecontrol.ModificationSet\"/><modset quietperiod=\"15\"><element att1=\"value1\" /></modset></target></project>";
        String xml3 = "<project><target></target></project>";
        String modset = "<modset quietperiod=\"15\"><element att1=\"value1\" /></modset>";

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
            assertEquals("The specified build file: '" + tempBuildFile.getAbsolutePath() + "' does not exist.", e.getMessage());
        }
        writeFile("_tempBuildFile");
        upgrader.setBuildFile(new File("_tempBuildFile"));

        try {
            upgrader.execute();
            fail("Expected CruiseControlException, but didn't get it");
        } catch (Exception e) {
            // expected behavior
            assertEquals("The specified properties file: '" + tempPropertiesFile.getAbsolutePath() + "' does not exist.", e.getMessage());
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