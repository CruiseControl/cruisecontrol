package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import org.jdom.output.XMLOutputter;

public class ProjectPropertiesHelperTest extends TestCase {

    private ProjectPropertiesHelper _properties;

    public ProjectPropertiesHelperTest(String name) {
        super(name);
    }

    public void setUp() {
        _properties = new ProjectPropertiesHelper();
        _properties.setBuildInterval(600000);
    }

    public void testToElement() {
        String expected = "<properties><interval seconds=\"600\" /></properties>";
        XMLOutputter outputter = new XMLOutputter();
        assertEquals(outputter.outputString(_properties.toElement()), expected);
    }

}