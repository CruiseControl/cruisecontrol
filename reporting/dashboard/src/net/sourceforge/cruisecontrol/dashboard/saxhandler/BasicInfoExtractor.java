package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class BasicInfoExtractor extends SAXBasedExtractor {

    private boolean readingInfo;

    private String projectName;

    private String label;

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("info".equals(qName)) {
            readingInfo = true;
            return;
        }
        if (readingInfo && "property".equals(qName)) {
            String propName = attributes.getValue("name");
            if ("projectname".equals(propName)) {
                projectName = attributes.getValue("value");
            }
            if ("label".equals(propName)) {
                label = attributes.getValue("value");
            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("info".equals(qName)) {
            readingInfo = false;
            canStop(true);
        }
    }

    public void report(Map resultSet) {
        resultSet.put("projectname", projectName);
        resultSet.put("label", label);
    }
}
