package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ModificationSet;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.publishers.LinkEmailPublisher;
import net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusBootstrapper;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

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
    private static Logger log = Logger.getLogger(Upgrader.class);

    private File _buildFile = null;
    private File _configFile = null;
    private File _propertiesFile = null;
    private String _projectName;

    public void setBuildFile(File buildFile) {
        _buildFile = buildFile;
        log.info("Build file: " + buildFile);
    }

    public void setConfigFile(File configFile) {
        _configFile = configFile;
        log.info("Config file: " + configFile);
    }

    public void setPropertiesFile(File propertiesFile) {
        _propertiesFile = propertiesFile;
        log.info("Properties file: " + propertiesFile);
    }

    public void setProjectName(String projectName) {
        _projectName = projectName;
        log.info("Project name: " + projectName);
    }

    protected void validate() throws CruiseControlException {
        if (_buildFile == null) {
            throw new CruiseControlException("No build file specified.");
        }
        if (_propertiesFile == null) {
            throw new CruiseControlException("No properties file specified.");
        }
        if (_configFile == null) {
            throw new CruiseControlException("No configuration file specified.");
        }
        if(_projectName == null) {
            throw new CruiseControlException("No project name specified.");
        }

        if (!_buildFile.exists()) {
            throw new CruiseControlException("The specified build file: '" +
                    _buildFile.getAbsolutePath() + "' does not exist.");
        }
        if (!_propertiesFile.exists()) {
            throw new CruiseControlException("The specified properties file: '" +
                    _propertiesFile.getAbsolutePath() + "' does not exist.");
        }
        if (_configFile.exists()) {
            throw new CruiseControlException("The specified configuration file: '" +
                    _configFile.getAbsolutePath() + "' exists.  Delete and try again.");
        }

    }

    public void execute() throws CruiseControlException {
        // test for valid members. A controlable NPE.
        validate();

        Properties properties = loadProperties(_propertiesFile);
        Element buildFileElement = readFileToElement(_buildFile);
        try {
            writeXMLFile(createXML(properties, findModificationSet(buildFileElement)));
        } catch (JDOMException e) {
            throw new CruiseControlException(e);
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
            if (taskdefElement.getAttributeValue("classname").equals(ModificationSet.class.getName())) {
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
                    if (taskdefElement.getAttributeValue("classname").equals(ModificationSet.class.getName())) {
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
        logString.append("<log dir=\"" + properties.getProperty("logDir") + "\">");
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

    public String createEmailMap(Properties emailmap) {
        StringBuffer map = new StringBuffer();
        if (emailmap.size() == 0) {
            return "";
        }

        map.append("<map>");
        Iterator emailmapIterator = emailmap.keySet().iterator();
        while (emailmapIterator.hasNext()) {
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
        while (buildmasterTokenizer.hasMoreTokens()) {
            publishers.append("<always address=\"" + buildmasterTokenizer.nextToken() + "\"/>");
        }
        StringTokenizer failureTokenizer = new StringTokenizer(properties.getProperty("notifyOnFailure"), ", ");
        while (failureTokenizer.hasMoreTokens()) {
            publishers.append("<failure address=\"" + failureTokenizer.nextToken() + "\"/>");
        }

        File emailMap = new File(properties.getProperty("emailmap"));
        if (emailMap.exists()) {
            Properties emailmap = loadProperties(emailMap);
            Iterator emailmapIterator = emailmap.keySet().iterator();
            while (emailmapIterator.hasNext()) {
                String key = (String) emailmapIterator.next();
                publishers.append("<map alias=\"" + key + "\" address=\"" + emailmap.getProperty(key) + "\"/>");
            }
        }
        publishers.append("</email>");
        publishers.append("</publishers>");
        return publishers.toString();
    }

    public String createLabelIncrementerPlugin(Properties properties) {
        if(properties.getProperty("labelIncrementerClass") != null) {
            String classname = properties.getProperty("labelIncrementerClass");
            return "<plugin name=\"labelincrementer\" classname=\"" + classname + "\"/>";
        } else {
            return "";
        }
    }

    public Element createXML(Properties properties, Element modificationsetElement) throws JDOMException {
        StringBuffer config = new StringBuffer();
        config.append("<cruisecontrol><project name=\"" + _projectName + "\">");
        config.append(createBootstrappers(properties));
        config.append(createModificationSet(modificationsetElement));
        config.append(createSchedule(properties));
        config.append(createLog(properties));
        config.append(createPublishers(properties));
        config.append(createLabelIncrementerPlugin(properties));
        config.append("</project></cruisecontrol>");

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        return builder.build(new StringReader(config.toString())).getRootElement();
    }

    protected Element readFileToElement(File file) {
        Element buildFileElement = null;
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            buildFileElement = builder.build(file).getRootElement();
        } catch (Exception e) {
            log.fatal("", e);
        }
        return buildFileElement;
    }

    protected void writeXMLFile(Element element) {
        XMLOutputter outputter = new XMLOutputter("   ", true);
        FileWriter fw = null;
        try {
            fw = new FileWriter(_configFile);
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
    protected Properties loadProperties(File propertiesFile) {
        Properties properties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propertiesFile);
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
        log.info("started upgrader...");
        final String usageInfo = "Usage: java -jar cruisecontrol.jar -upgrade <build file> <properties file> <config file> <projectname>";
        if(args.length != 4) {
            log.fatal(usageInfo);
            return;
        }
        Upgrader upgrader = new Upgrader();
        try {
            upgrader.setBuildFile(new File(args[0]));
            upgrader.setPropertiesFile(new File(args[1]));
            upgrader.setConfigFile(new File(args[2]));
            upgrader.setProjectName(args[3]);
            try {
                upgrader.validate();
            } catch (CruiseControlException e) {
                log.fatal(usageInfo);
            }
            upgrader.execute();
            log.info("upgrader finished");
        } catch (CruiseControlException e) {
            log.fatal("", e);
        }
    }
}