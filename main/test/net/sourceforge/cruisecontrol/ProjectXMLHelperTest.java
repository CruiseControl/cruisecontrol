package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;

import java.text.DateFormat;

import org.jdom.Element;

public class ProjectXMLHelperTest extends TestCase {

    public ProjectXMLHelperTest(String name) {
        super(name);
    }

    public void testDateFormat() {
        assertEquals("MM/dd/yyyy HH:mm:ss", DateFormatFactory.getFormat());

        Element projectElement = new Element("project");
        Element dateFormatElement = new Element("dateformat");
        dateFormatElement.setAttribute("format", "yyyy/MM/dd hh:mm:ss a");
        projectElement.addContent(dateFormatElement);

        ProjectXMLHelper helper = new ProjectXMLHelper();
        helper.setDateFormat(projectElement);

        assertEquals("yyyy/MM/dd hh:mm:ss a", DateFormatFactory.getFormat());
    }
}
