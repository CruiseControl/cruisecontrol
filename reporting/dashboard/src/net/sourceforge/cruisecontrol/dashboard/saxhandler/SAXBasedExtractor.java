package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

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
}
