/**
 * Created by IntelliJ IDEA.
 * User: mstave
 * Date: Dec 6, 2002
 * Time: 3:07:03 PM
 * To change this template use Options | File Templates.
 */
package net.sourceforge.cruisecontrol.testutil;

import org.jdom.Element;

import java.util.Hashtable;
import java.util.Iterator;

public class Util {
    public static Element createElement(boolean success, boolean lastBuildSuccess) {
        return createElement(success, lastBuildSuccess, "2 minutes 20 seconds", 4, null);
    }

    public static Element createModsElement(int numMods) {
        Element modificationsElement = new Element("modifications");
        for (int i = 1; i <= numMods; i++) {
            Element modificationElement = new Element("modification");
            Element userElement = new Element("user");
	    int userNumber = (i>2)?i-1:i;
            userElement.addContent("user" + userNumber);
            modificationElement.addContent(userElement);
            modificationsElement.addContent(modificationElement);
        }
        return modificationsElement;
    }

    public static Element createElement(boolean success, boolean lastBuildSuccess, String time, int modCount, String failureReason ) {
        Element cruisecontrolElement = new Element("cruisecontrol");
        Element buildElement = new Element("build");
        buildElement.setAttribute("time", time);

        if (!success) {
            buildElement.setAttribute("error", (failureReason == null) ? "Compile failed" : failureReason );
        }

        cruisecontrolElement.addContent(createModsElement(modCount));
        cruisecontrolElement.addContent(buildElement);
        cruisecontrolElement.addContent(createInfoElement("somelabel", lastBuildSuccess));
        return cruisecontrolElement;
    }

    public static Element createInfoElement(String label, boolean lastBuildSuccess) {
        Element infoElement = new Element("info");

        Hashtable properties = new Hashtable();
        properties.put("label", label);
        properties.put("lastbuildsuccessful", lastBuildSuccess + "");
        properties.put("logfile", "log20020206120000.xml");
        properties.put("projectname", "TestProject");
        properties.put("builddate", "12/09/2002 13:43:15");

        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyName = (String) propertyIterator.next();
            Element propertyElement = new Element("property");
            propertyElement.setAttribute("name", propertyName);
            propertyElement.setAttribute("value", (String) properties.get(propertyName));
            infoElement.addContent(propertyElement);
        }

        return infoElement;
    }
}
