package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;
import org.apache.log4j.Category;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *  Upgrades an existing cruisecontrol.properties and build.xml to the new config.xml file.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class Upgrader {

    /** enable logging for this class */
    private static Category log = Category.getInstance(Upgrader.class.getName());

    private String _buildFileName;
    private String _configFileName;
    private String _propertiesFileName;

    public void execute() throws CruiseControlException {
        File buildFile = new File(_buildFileName);
        if (!buildFile.exists()) {
            throw new CruiseControlException("The specified build file: '" + buildFile.getAbsolutePath() + "' does not exist.");
        }
        File propertiesFile = new File(_propertiesFileName);
        if (!propertiesFile.exists()) {
            throw new CruiseControlException("The specified properties file: '" + propertiesFile.getAbsolutePath() + "' does not exist.");
        }
        File configFile = new File(_configFileName);
        if (configFile.exists()) {
            throw new CruiseControlException("The specified configuration file: '" + configFile.getAbsolutePath() + "' exists.  Delete and try again.");
        }

        Properties properties = loadProperties(_propertiesFileName);
        Element buildFileElement = readFileToElement(buildFile.getAbsolutePath());
        try {
            writeXMLFile(createXML(properties, findModificationSet(buildFileElement)));
        } catch (JDOMException e) {
            log.fatal("", e);
        } catch (CruiseControlException e) {
            log.fatal("", e);
        }
    }

    /**
     *  Extracts the modificationset from the specified build file xml.
     *
     */
    public Element findModificationSet(Element buildFileElement) throws CruiseControlException {
        String elementName = null;
        Iterator taskdefIterator = buildFileElement.getChildren("taskdef").iterator();
        while (taskdefIterator.hasNext()) {
            Element taskdefElement = (Element) taskdefIterator.next();
            if (taskdefElement.getAttributeValue("classname").equals("net.sourceforge.cruisecontrol.ModificationSet")) {
                elementName = taskdefElement.getAttributeValue("name");
            }
        }

        Iterator targetIterator = buildFileElement.getChildren("target").iterator();
        while (targetIterator.hasNext()) {
            Element targetElement = (Element) targetIterator.next();

            if (elementName == null) {
                Iterator nestedTaskdefIterator = targetElement.getChildren("taskdef").iterator();
                while (nestedTaskdefIterator.hasNext()) {
                    Element taskdefElement = (Element) nestedTaskdefIterator.next();
                    if (taskdefElement.getAttributeValue("classname").equals("net.sourceforge.cruisecontrol.ModificationSet")) {
                        elementName = taskdefElement.getAttributeValue("name");
                    }
                }
            }

            if (elementName != null) {
                if (targetElement.getChild(elementName) != null) {
                    return targetElement.getChild(elementName).detach();
                }
            }
        }
        throw new CruiseControlException("Could not find a modification set.");
    }

    /**
     *  Creates the xml for the bootstrappers.
     */
    public String createBootstrappers(Properties properties) {
        StringBuffer bootstrappers = new StringBuffer();
        bootstrappers.append("<bootstrappers>");
        bootstrappers.append("<currentbuildstatusbootstrapper file=\"" + properties.getProperty("currentBuildStatusFile") + "\"/>");
        bootstrappers.append("</bootstrappers>");
        return bootstrappers.toString();
    }

    /**
     *  Creates the xml for the schedule.
     */
    public String createSchedule(Properties properties) {
        StringBuffer schedule = new StringBuffer();
        schedule.append("<schedule interval=\"" + properties.getProperty("buildinterval") + "\"");
        schedule.append(" intervaltype=\"" + (properties.getProperty("absoluteInterval").equalsIgnoreCase("true") ? "absolute" : "relative") + "\">");
        schedule.append("<ant buildfile=\"" + properties.getProperty("antfile") + "\" target=\"" + properties.getProperty("cleantarget") + "\" multiple=\"" + properties.getProperty("cleanBuildEvery") + "\"/>");
        schedule.append("<ant buildfile=\"" + properties.getProperty("antfile") + "\" target=\"" + properties.getProperty("target") + "\" multiple=\"1\"/>");
        schedule.append("</schedule>");
        return schedule.toString();
    }

    /**
     *  Creates the xml for the log.
     */
    public String createLog(Properties properties) {
        StringBuffer logString = new StringBuffer();
        logString.append("<log logdir=\"" + properties.getProperty("logDir") + "\">");
        logString.append("</log>");
        return logString.toString();
    }

    /**
     *  Creates the xml for the modificationset.
     */
    public String createModificationSet(Element modificationSetElement) {
        modificationSetElement.removeAttribute("lastbuild");
        XMLOutputter outputter = new XMLOutputter("", false);
        outputter.setTextNormalize(true);
        return outputter.outputString(modificationSetElement);
    }

    public boolean isMapEnabled(Properties properties) {
        if(properties.getProperty("mapSourceControlUsersToEmail") != null && properties.getProperty("mapSourceControlUsersToEmail").equalsIgnoreCase("true")) {
            return new File(properties.getProperty("emailmap")).exists();
        }
        return false;
    }

    public String createEmailMap(Properties emailmap) {
        StringBuffer map = new StringBuffer();
        if(emailmap.size() == 0) {
            return "";
        }

        map.append("<map>");
        Iterator emailmapIterator = emailmap.keySet().iterator();
        while(emailmapIterator.hasNext()) {
            String key = (String) emailmapIterator.next();
            map.append("<map alias=\"" + key + "\" address=\"" + emailmap.getProperty(key) + "\"/>");
        }

        map.append("</map>");

        return map.toString();
    }

    /**
     *  Creates the xml for the publishers.
     */
    public String createPublishers(Properties properties) {
        StringBuffer publishers = new StringBuffer();
        publishers.append("<publishers>");
        publishers.append("<currentbuildstatuspublisher file=\"" + properties.getProperty("currentBuildStatusFile") + "\"/>");
        publishers.append("<email ");
        publishers.append(" mailhost=\"" + properties.getProperty("mailhost") + "\"");
        publishers.append(" returnaddress=\"" + properties.getProperty("returnAddress") + "\"");
        publishers.append(" defaultsuffix=\"" + properties.getProperty("defaultEmailSuffix") + "\"");
        publishers.append(" buildresultsurl=\"" + properties.getProperty("servletURL") + "\"");
        publishers.append(">");
        StringTokenizer buildmasterTokenizer = new StringTokenizer(properties.getProperty("buildmaster"), ", ");
        while(buildmasterTokenizer.hasMoreTokens()) {
            publishers.append("<always address=\"" + buildmasterTokenizer.nextToken() + "\"/>");
        }
        StringTokenizer failureTokenizer = new StringTokenizer(properties.getProperty("notifyOnFailure"), ", ");
        while(failureTokenizer.hasMoreTokens()) {
            publishers.append("<failure address=\"" + failureTokenizer.nextToken() + "\"/>");
        }
        if(isMapEnabled(properties)) {
            Properties emailmap = loadProperties(properties.getProperty("emailmap"));
            Iterator emailmapIterator = emailmap.keySet().iterator();
            while(emailmapIterator.hasNext()) {
                String key = (String) emailmapIterator.next();
                publishers.append("<map alias=\"" + key + "\" address=\"" + emailmap.getProperty(key) + "\"/>");
            }
        }
        publishers.append("</email>");
        publishers.append("</publishers>");
        return publishers.toString();
    }

    public String createLabelIncrementerPlugin(Properties properties) {
        String classname = properties.getProperty("labelIncrementerClass") != null ? properties.getProperty("labelIncrementerClass") : "net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer";
        return "<plugin name=\"labelincrementer\" classname=\"" + classname + "\"/>";
    }

    public Element createXML(Properties properties, Element modificationsetElement) throws JDOMException {
        StringBuffer config = new StringBuffer();
        config.append("<cruisecontrol><project>");
        config.append(createBootstrappers(properties));
        config.append(createModificationSet(modificationsetElement));
        config.append(createSchedule(properties));
        config.append(createLog(properties));
        config.append(createPublishers(properties));
        config.append("<plugin name=\"currentbuildstatusbootstrapper\" classname=\"net.sourceforge.cruisecontrol.CurrentBuildStatusBootstrapper\"/>");
        config.append("<plugin name=\"ant\" classname=\"net.sourceforge.cruisecontrol.builders.AntBuilder\"/>");
        config.append("<plugin name=\"email\" classname=\"net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher\"/>");
        config.append("<plugin name=\"currentbuildstatuspublisher\" classname=\"net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher\"/>");
        config.append(createLabelIncrementerPlugin(properties));
        config.append("</project></cruisecontrol>");

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        return builder.build(new StringReader(config.toString())).getRootElement();
    }

    public void setBuildFile(String buildFile) {
        _buildFileName = buildFile;
    }

    public void setConfigFile(String configFile) {
        _configFileName = configFile;
    }

    public void setPropertiesFile(String propertiesFile) {
        _propertiesFileName = propertiesFile;
    }

    protected Element readFileToElement(String filename) {
        Element buildFileElement = null;
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            buildFileElement = builder.build(filename).getRootElement();
        } catch (Exception e) {
            log.fatal("", e);
        }
        return buildFileElement;
    }

    protected void writeXMLFile(Element element) {
        XMLOutputter outputter = new XMLOutputter("   ", true);
        FileWriter fw = null;
        try {
            fw = new FileWriter(_configFileName);
            outputter.output(element, fw);
            fw.close();
        } catch (IOException e) {
            log.fatal("", e);
        } finally {
            fw = null;
        }
    }

    /**
     *  Load a properties file.
     */
    protected Properties loadProperties(String propertiesFileName) {
        Properties properties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propertiesFileName);
            properties.load(fis);
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            fis = null;
        }
        return properties;
    }

    public static void main(String args[]) {
        Upgrader upgrader = new Upgrader();
        try {
            upgrader.setBuildFile(args[0]);
            upgrader.setPropertiesFile(args[1]);
            upgrader.setConfigFile(args[2]);
            upgrader.execute();
        } catch (CruiseControlException e) {
            log.fatal("", e);
        }
    }
}