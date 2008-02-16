package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import net.sourceforge.cruisecontrol.dashboard.BuildMessage;
import net.sourceforge.cruisecontrol.dashboard.MessageLevel;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

public class MessageExtractor extends SAXBasedExtractor {

    private boolean readingMessage;

    private String priority;

    private List messages = new ArrayList();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = true;
            priority = attributes.getValue("priority");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingMessage) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            messages.add(text);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("message".equals(qName)) {
            readingMessage = false;
        }
    }

    public void report(Map resultSet) {
        if (!messages.isEmpty()) {
            String text = "";
            for (Iterator i = messages.iterator(); i.hasNext();) {
                text += (String) i.next();
            }
            resultSet.put("message", new BuildMessage(text, MessageLevel.getLevelForPriority(priority)));
        }
    }
}
