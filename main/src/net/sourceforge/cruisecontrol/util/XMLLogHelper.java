package net.sourceforge.cruisecontrol.util;

import org.jdom.Element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class XMLLogHelper {

    private Element _log;

    public XMLLogHelper(Element log) {
        _log = log;
    }

    public String getProjectName() {
        return _log.getAttributeValue("name");
    }

    /**
     *
     */
    public boolean isBuildSuccessful() {
        if (_log.getAttribute("error") != null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     *
     */
    public boolean wasPreviousBuildSuccessful() {
        return _log.getChild("lastBuild").getAttributeValue("status").equals("success");
    }

    /**
     *
     */
    public Set getBuildParticipants() {
        Set results = new HashSet();
        Iterator modificationIterator = _log.getChild("modifications").getChildren("modification").iterator();
        while (modificationIterator.hasNext()) {
            Element modification = (Element) modificationIterator.next();
            results.add(modification.getChild("user").getText());
        }
        return results;
    }
}