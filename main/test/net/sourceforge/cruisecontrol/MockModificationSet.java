package net.sourceforge.cruisecontrol;

import org.jdom.Element;

import java.util.Date;

public class MockModificationSet extends ModificationSet {
    public Element getModifications(Date lastBuild) {
        return new Element("modifications");
    }

    public int size() {
        return 1;
    }
}