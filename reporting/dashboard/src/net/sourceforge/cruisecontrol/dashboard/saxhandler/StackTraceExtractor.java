package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class StackTraceExtractor extends SAXBasedExtractor {

    private boolean readingBuild;

    private boolean readingStatckTrace;

    private String stackTrace = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = true;
        }
        if ("stacktrace".equals(qName)) {
            readingStatckTrace = true;
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingBuild && readingStatckTrace) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            stackTrace += text;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = false;
        }
        if ("stacktrace".equals(qName)) {
            readingStatckTrace = false;
        }
    }

    public void report(Map resultSet) {
        resultSet.put("stacktrace", stackTrace);
    }
}
