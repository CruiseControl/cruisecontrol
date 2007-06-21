package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class DurationExtractor extends SAXBasedExtractor {
    private String duration = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("build".equals(qName)) {
            duration = attributes.getValue("time");
            canStop(true);
        }
    }

    public void report(Map resultSet) {
        resultSet.put("duration", duration);
    }
}
