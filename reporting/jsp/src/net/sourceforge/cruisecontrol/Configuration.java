package net.sourceforge.cruisecontrol;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private JMXServiceURL address;

    private Map environment = new HashMap();

    private JMXConnector cntor;

    private MBeanServerConnection mbsc;

    private ObjectName ccMgrObjName;

    public Configuration() throws IOException, MalformedObjectNameException {
        address = new JMXServiceURL(
                "service:jmx:rmi://localhost:7856/jndi/jrmp");
        environment.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put(Context.PROVIDER_URL, "rmi://localhost:7856");
        cntor = JMXConnectorFactory.connect(address, environment);
        mbsc = cntor.getMBeanServerConnection();
        ccMgrObjName = ObjectName
                .getInstance("CruiseControl Manager:id=unique");
    }

    public String getConfiguration() throws AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, JDOMException {

        String xml = (String) mbsc.getAttribute(ccMgrObjName, "ConfigFileContents");
        Document doc = new SAXBuilder().build(new StringReader(xml));
        xml = new XMLOutputter(Format.getPrettyFormat()).outputString(doc);
        return xml.trim();
    }

    public void setConfiguration(String configuration)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, IOException {
        mbsc.setAttribute(ccMgrObjName, new Attribute("ConfigFileContents",
                URLDecoder.decode(configuration)));
    }
}
