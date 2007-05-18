package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;
import net.sourceforge.cruisecontrol.dashboard.BuildMessage;
import net.sourceforge.cruisecontrol.dashboard.MessageLevel;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class MessageExtractor extends SAXBasedExtractor {

    private boolean readingMessage;

    private String priority;

    private BuildMessage message;

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = true;
        }
        if (readingMessage) {
            priority = attributes.getValue("priority");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingMessage) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            message = new BuildMessage(text, MessageLevel.getLevelForPriority(priority));
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = false;
        }
    }

    public void report(Map resultSet) {
        if (message != null) {
            resultSet.put("message", message);
        }
    }
}
