package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import org.jdom.output.XMLOutputter;

public class CruiseControlPropertiesTest extends TestCase {

    private CruiseControlProperties _properties;

    public CruiseControlPropertiesTest(String name) {
        super(name);
    }

    public void setUp() {
        _properties = new CruiseControlProperties();
        _properties.setBuildInterval(600000);
    }

    public void testToElement() {
        String expected = "<properties><interval seconds=\"600\" /></properties>";
        XMLOutputter outputter = new XMLOutputter();
        assertEquals(outputter.outputString(_properties.toElement()), expected);
    }

}