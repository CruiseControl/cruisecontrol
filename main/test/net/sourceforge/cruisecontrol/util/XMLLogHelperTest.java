package net.sourceforge.cruisecontrol.util;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.Element;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class XMLLogHelperTest extends TestCase {

    private Element _successfulLogElement;
    private Element _failedLogElement;

    public XMLLogHelperTest(String name) {
        super(name);
    }

    private Element createModificationsElement(String username1, String username2) {
        Element modificationsElement = new Element("modifications");
        Element modificationElement1 = new Element("modification");
        Element userElement1 = new Element("user");
        userElement1.addContent(username1);
        modificationElement1.addContent(userElement1);
        Element modificationElement2 = new Element("modification");
        Element userElement2 = new Element("user");
        userElement2.addContent(username2);
        modificationElement2.addContent(userElement2);
        modificationsElement.addContent(modificationElement1);
        modificationsElement.addContent(modificationElement2);

        return modificationsElement;
    }

    private Element createBuildElement(boolean successful, String propertyValue1, String propertyValue2) {
        Element buildElement = new Element("build");
        if (!successful) {
            buildElement.setAttribute("error", "No Build Necessary");
        }
        Element propertiesElement = new Element("properties");
        Element propertyElement1 = new Element("property");
        propertyElement1.setAttribute("name", "antprop1");
        propertyElement1.setAttribute("value", propertyValue1);
        Element propertyElement2 = new Element("property");
        propertyElement2.setAttribute("name", "ant.project.name");
        propertyElement2.setAttribute("value", "someproject");
        propertiesElement.addContent(propertyElement1);
        propertiesElement.addContent(propertyElement2);
        buildElement.addContent(propertiesElement);

        return buildElement;
    }

    private Element createInfoElement(String label, boolean lastSuccessful) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("label", label);
        properties.put("lastbuildtime", "");
        properties.put("lastgoodbuildtime", "");
        properties.put("lastbuildsuccessful", lastSuccessful + "");
        properties.put("buildfile", "");
        properties.put("target", "");
        properties.put("logfile", "log20020313120000.xml");

        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyName = (String) propertyIterator.next();
            Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setAttribute("value", (String) properties.get(propertyName));
            infoElement.addContent(propertyElement);
        }

        return infoElement;
    }

    public void setUp() {
        _successfulLogElement = new Element("cruisecontrol");
        _successfulLogElement.addContent(createInfoElement("1.0", false));
        _successfulLogElement.addContent(createBuildElement(true, "antvalue1", "antvalue2"));
        _successfulLogElement.addContent(createModificationsElement("username1", "username2"));

        _failedLogElement = new Element("cruisecontrol");
        _failedLogElement.addContent(createInfoElement("1.1", true));
        _failedLogElement.addContent(createBuildElement(false, "antvalue3", "antvalue4"));
        _failedLogElement.addContent(createModificationsElement("username3", "username4"));
    }

    public void testGetLabel() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        try {
            assertEquals("1.0", successHelper.getLabel());
            assertEquals("1.1", failureHelper.getLabel());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetLogFileName() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        try {
            assertEquals("log20020313120000.xml", successHelper.getLogFileName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }


    public void testWasPreviousBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        try {
            assertEquals(false, successHelper.wasPreviousBuildSuccessful());
            assertEquals(true, failureHelper.wasPreviousBuildSuccessful());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testGetAntProperty() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        try {
            assertEquals("antvalue1", successHelper.getAntProperty("antprop1"));
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
        try {
            successHelper.getAntProperty("notaproperty");
        } catch (CruiseControlException e) {
            assertEquals("Property: notaproperty not found.", e.getMessage());
        }
    }

    public void testGetCruiseControlInfoProperty() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        try {
            assertEquals("1.0", successHelper.getCruiseControlInfoProperty("label"));
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
        try {
            successHelper.getCruiseControlInfoProperty("notaproperty");
        } catch (CruiseControlException e) {
            assertEquals("Property: notaproperty not found.", e.getMessage());
        }
    }

    public void testIsBuildNecessary() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        assertEquals(true, successHelper.isBuildNecessary());
        assertEquals(false, failureHelper.isBuildNecessary());
    }

    public void testGetProjectName() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);

        try {
            assertEquals("someproject", successHelper.getProjectName());
        } catch (CruiseControlException e) {
            assertTrue(false);
        }
    }

    public void testIsBuildSuccessful() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        XMLLogHelper failureHelper = new XMLLogHelper(_failedLogElement);
        assertEquals(true, successHelper.isBuildSuccessful());
        assertEquals(false, failureHelper.isBuildSuccessful());
    }

    public void testGetBuildParticipants() {
        XMLLogHelper successHelper = new XMLLogHelper(_successfulLogElement);
        Set successHelperParticipants = successHelper.getBuildParticipants();
        assertEquals(true, successHelperParticipants.contains("username1"));
        assertEquals(true, successHelperParticipants.contains("username2"));
        assertEquals(false, successHelperParticipants.contains("notaperson"));
    }
}
