package net.sourceforge.cruisecontrol;

import org.jdom.Element;

import java.util.Date;
import java.util.Map;

public class MockSchedule extends Schedule {
    public Element build(int buildNumber, Date lastBuild, Date now, Map properties) throws CruiseControlException {
        return new Element("build");
    }

    public boolean isPaused(Date now) {
        return false;
    }
}