package net.sourceforge.cruisecontrol.dashboard.exception;

import org.xml.sax.SAXException;

public class ShouldStopParsingException extends SAXException {
    public ShouldStopParsingException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -2858066307713966338L;
}
