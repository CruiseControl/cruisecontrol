package net.sourceforge.cruisecontrol.builders;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

public class PropertyLogger implements BuildListener {

    public void buildFinished(BuildEvent event) {
        Hashtable propsHashtable = event.getProject().getProperties();
        Iterator propertyIterator = propsHashtable.keySet().iterator();
        Element propertiesElement = new Element("properties");
        while (propertyIterator.hasNext()) {
            Element propertyElement = new Element("property");
            String name = (String) propertyIterator.next();
            propertyElement.setAttribute("name", name);
            propertyElement.setAttribute("value", (String) propsHashtable.get(name));
            propertiesElement.addContent(propertyElement);
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("propertylogger.xml"));
            XMLOutputter outputter = new XMLOutputter("   ", true);
            outputter.output(propertiesElement, bw);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bw = null;
        }
    }

    //don't care about these methods

    public void buildStarted(BuildEvent event) {
    }

    public void targetStarted(BuildEvent event) {
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    }
}