package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class BuildMessageExtractor extends SAXBasedExtractor {
    public static final Object KEY_BUILD = "build.error";
    public static final String KEY_MESSAGES = "build.messages";

    private MessageExtractor messageExtractor = new MessageExtractor();

    private List buildMessages = new ArrayList();

    private boolean readingBuild;

    private boolean readingMessage;

    private Map messagesResult = new HashMap();

    private String buildError;

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (isBuildTag(qName)) {
            readingBuild = true;
            buildError = attributes.getValue("error");
            return;
        }

        if (isMessageTag(qName)) {
            readingMessage = true;
            if (readingBuild) {
                messageExtractor = new MessageExtractor();
                messageExtractor.startElement(uri, localName, qName, attributes);
            }
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingBuild && readingMessage) {
            messageExtractor.characters(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (readingBuild && readingMessage) {
            messageExtractor.endElement(uri, localName, qName);
            messageExtractor.report(messagesResult);
            buildMessages.add(messagesResult.get("message"));
        }

        if (isMessageTag(qName)) {
            readingMessage = false;
            return;
        }

        if (isBuildTag(qName)) {
            readingBuild = false;
        }
    }

    public void report(Map resultSet) {
        resultSet.put(KEY_BUILD, buildError);
        resultSet.put(KEY_MESSAGES, buildMessages);
    }

    private boolean isMessageTag(String qName) {
        return "message".equals(qName);
    }

    private boolean isBuildTag(String qName) {
        return "build".equals(qName);
    }
}
