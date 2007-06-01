package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.exception.ShouldStopParsingException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * TODO Dynamic Proxy/Functor
 */
public class CompositeExtractor extends SAXBasedExtractor {

    private static final SAXException SHOULD_STOP_EXCEPTION = new ShouldStopParsingException("");

    private final SAXBasedExtractor[] handlers;

    public CompositeExtractor(SAXBasedExtractor[] handlers) {
        this.handlers = handlers;
    }

    public void report(Map resultSet) {
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].report(resultSet);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].characters(ch, start, length);
        }
        throwExceptionIfCanStop();
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].endElement(uri, localName, qName);
        }
        throwExceptionIfCanStop();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].startElement(uri, localName, qName, attributes);
        }
        throwExceptionIfCanStop();
    }

    private void throwExceptionIfCanStop() throws SAXException {
        for (int i = 0; i < handlers.length; i++) {
            if (!handlers[i].canStop()) {
                return;
            }
        }
        throw SHOULD_STOP_EXCEPTION;
    }
}
