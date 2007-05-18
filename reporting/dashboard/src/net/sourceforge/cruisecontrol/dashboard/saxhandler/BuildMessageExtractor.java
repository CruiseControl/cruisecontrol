package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class BuildMessageExtractor extends SAXBasedExtractor {

    private MessageExtractor messageExtractor = new MessageExtractor();

    private List buildMessages = new ArrayList();

    private boolean readingBuild;

    private boolean readingMessage;

    private boolean readingTarget;

    private Map messagesResult = new HashMap();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = true;
            return;
        }
        if ("target".equals(qName)) {
            readingTarget = true;
            return;
        }
        readingMessage = "message".equals(qName);
        if (readingBuild && readingMessage && !readingTarget) {
            messageExtractor.startElement(uri, localName, qName, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingBuild && readingMessage && !readingTarget) {
            messageExtractor.characters(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingBuild && readingMessage && !readingTarget) {
            messageExtractor.endElement(uri, localName, qName);
            messageExtractor.report(messagesResult);
            buildMessages.add(messagesResult.get("message"));
        }

        if ("build".equals(qName)) {
            readingBuild = false;
        }
    }

    public void report(Map resultSet) {
        resultSet.put("buildmessages", buildMessages);
    }
}
