/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ModificationSet;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 *  Upgrades an existing cruisecontrol.properties and build.xml to the new config.xml file.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class Upgrader {

    private static final Logger LOG = Logger.getLogger(Upgrader.class);

    private File buildFile;
    private File configFile;
    private File propertiesFile;
    private String projectName;

    public void setBuildFile(File buildFile) {
        this.buildFile = buildFile;
        LOG.info("Build file: " + buildFile);
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
        LOG.info("Config file: " + configFile);
    }

    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
        LOG.info("Properties file: " + propertiesFile);
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        LOG.info("Project name: " + projectName);
    }

    protected void validate() throws CruiseControlException {
        if (buildFile == null) {
            throw new CruiseControlException("No build file specified.");
        }
        if (propertiesFile == null) {
            throw new CruiseControlException("No properties file specified.");
        }
        if (configFile == null) {
            throw new CruiseControlException("No configuration file specified.");
        }
        if (projectName == null) {
            throw new CruiseControlException("No project name specified.");
        }

        if (!buildFile.exists()) {
            throw new CruiseControlException(
                "The specified build file: '"
                    + buildFile.getAbsolutePath()
                    + "' does not exist.");
        }
        if (!propertiesFile.exists()) {
            throw new CruiseControlException(
                "The specified properties file: '"
                    + propertiesFile.getAbsolutePath()
                    + "' does not exist.");
        }
        if (configFile.exists()) {
            throw new CruiseControlException(
                "The specified configuration file: '"
                    + configFile.getAbsolutePath()
                    + "' exists.  Delete and try again.");
        }

    }

    public void execute() throws CruiseControlException {
        // test for valid members. A controlable NPE.
        validate();

        Properties properties = loadProperties(propertiesFile);
        Element buildFileElement = readFileToElement(buildFile);
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
        bootstrappers.append(
            "<currentbuildstatusbootstrapper file=\""
                + properties.getProperty("currentBuildStatusFile")
                + "\"/>");
        bootstrappers.append("</bootstrappers>");
        return bootstrappers.toString();
    }

    /**
     *  Creates the xml for the schedule.
     */
    public String createSchedule(Properties properties) {
        StringBuffer schedule = new StringBuffer();
        schedule.append(
            "<schedule interval=\""
                + properties.getProperty("buildinterval")
                + "\"");
        schedule.append(
            " intervaltype=\""
                + (properties
                    .getProperty("absoluteInterval")
                    .equalsIgnoreCase("true")
                    ? "absolute"
                    : "relative")
                + "\">");
        schedule.append(
            "<ant buildfile=\""
                + properties.getProperty("antfile")
                + "\" target=\""
                + properties.getProperty("cleantarget")
                + "\" multiple=\""
                + properties.getProperty("cleanBuildEvery")
                + "\"/>");
        schedule.append(
            "<ant buildfile=\""
                + properties.getProperty("antfile")
                + "\" target=\""
                + properties.getProperty("target")
                + "\" multiple=\"1\"/>");
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
        Element outputModSetElement = new Element("modificationset");
        Iterator attributeIterator = modificationSetElement.getAttributes().iterator();
        while (attributeIterator.hasNext()) {
            Attribute attribute = (Attribute) attributeIterator.next();
            if (!attribute.getName().equalsIgnoreCase("lastbuild")) {
                outputModSetElement.setAttribute(attribute.getName(), attribute.getValue());
            }
        }

        Iterator elementIterator = modificationSetElement.getChildren().iterator();
        while (elementIterator.hasNext()) {
            Element childElement = (Element) elementIterator.next();
            if (childElement.getName().equalsIgnoreCase("vsselement")) {
                Iterator vssAttributeIterator = childElement.getAttributes().iterator();
                while (vssAttributeIterator.hasNext()) {
                    Attribute attribute = (Attribute) vssAttributeIterator.next();
                    if (attribute.getName().equalsIgnoreCase("ssdir")) {
                        childElement.removeAttribute("ssdir");
                        childElement.setAttribute("vsspath", attribute.getValue());
                    }
                }
            }
            //correct vss naming scheme

            if (childElement.getName().endsWith("element")) {
                String newName = childElement.getName().substring(0, childElement.getName().indexOf("element"));
                childElement.setName(newName);
            }
            outputModSetElement.addContent(childElement.detach());
        }

        XMLOutputter outputter = new XMLOutputter("", false);
        outputter.setTextNormalize(true);
        return outputter.outputString(outputModSetElement);
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
        publishers.append(
            "<currentbuildstatuspublisher file=\""
                + properties.getProperty("currentBuildStatusFile")
                + "\"/>");
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
        if (properties.getProperty("labelIncrementerClass") != null) {
            String classname = properties.getProperty("labelIncrementerClass");
            return "<plugin name=\"labelincrementer\" classname=\"" + classname + "\"/>";
        } else {
            return "";
        }
    }

    public Element createXML(Properties properties, Element modificationsetElement) throws JDOMException {
        StringBuffer config = new StringBuffer();
        config.append("<cruisecontrol><project name=\"" + projectName + "\">");
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
            LOG.fatal("", e);
        }
        return buildFileElement;
    }

    protected void writeXMLFile(Element element) {
        XMLOutputter outputter = new XMLOutputter("   ", true);
        FileWriter fw = null;
        try {
            fw = new FileWriter(configFile);
            outputter.output(element, fw);
            fw.close();
        } catch (IOException e) {
            LOG.fatal("", e);
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
        LOG.info("started upgrader...");
        final String usageInfo =
            "Usage: java -jar cruisecontrol.jar -upgrade <build file> <properties file> <config file> <projectname>";
        if (args.length != 4) {
            LOG.fatal(usageInfo);
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
                LOG.fatal(usageInfo);
            }
            upgrader.execute();
            LOG.info("upgrader finished");
        } catch (CruiseControlException e) {
            LOG.fatal("", e);
        }
    }
}