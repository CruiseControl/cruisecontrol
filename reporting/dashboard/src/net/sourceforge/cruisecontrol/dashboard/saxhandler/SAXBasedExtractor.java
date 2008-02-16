package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public abstract class SAXBasedExtractor extends DefaultHandler {
    public abstract void report(Map resultSet);

    private boolean canStop;

    public boolean canStop() {
        return canStop;
    }

    protected void canStop(boolean canStop) {
        this.canStop = canStop;
    }

    protected String getAttribute(Attributes attributes, String attributeName) {
        String attributeValue = attributes.getValue(attributeName);
        return attributeValue == null ? "" : attributeValue;
    }
}
