package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DashboardXmlConfigService implements DashboardConfigService, InitializingBean {
    public static final String PROPS_CC_DASHBOARD_CONFIG = "dashboard.config";

    private static final Logger LOGGER = Logger.getLogger(DashboardXmlConfigService.class);

    private boolean isValid;

    private SystemService systemService;

    private NamedNodeMap featureAttributes;

    private NamedNodeMap buildLoopAttributes;

    private Document document;

    public DashboardXmlConfigService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void afterPropertiesSet() throws Exception {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder newDocumentBuilder = builderFactory.newDocumentBuilder();
            File configXml = getDashboardConfigFileLocation();
            document = newDocumentBuilder.parse(configXml);
            featureAttributes = getAttributesMap("features");
            buildLoopAttributes = getAttributesMap("buildloop");
            isValid = true;
        } catch (Exception e) {
            LOGGER.warn(e);
            isValid = false;
        }
    }

    private NamedNodeMap getAttributesMap(String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        Node node = nodes.item(0);
        return node.getAttributes();
    }

    private File getDashboardConfigFileLocation() {
        String dashboardConfigLocation = systemService.getProperty(PROPS_CC_DASHBOARD_CONFIG);
        if (systemService.isAbsolutePath(dashboardConfigLocation)) {
            return new File(dashboardConfigLocation);
        } else {
            String cchome = systemService.getProperty(SystemPropertyConfigService.PROPS_CC_HOME);
            dashboardConfigLocation =
                    StringUtils.isEmpty(dashboardConfigLocation) ? "dashboard-config.xml"
                            : dashboardConfigLocation;
            return new File(new File(StringUtils.defaultString(cchome)), dashboardConfigLocation);
        }
    }

    public String getArtifactsDir() throws ConfigurationException {
        return isValid ? buildLoopAttributes.getNamedItem("artifactsdir").getNodeValue() : "";
    }

    public String getConfigXml() {
        return isValid ? buildLoopAttributes.getNamedItem("configfile").getNodeValue() : "";
    }

    public String getJMXPort() {
        return isValid ? buildLoopAttributes.getNamedItem("jmxport").getNodeValue() : "";
    }

    public String getLogsDir() throws ConfigurationException {
        return isValid ? buildLoopAttributes.getNamedItem("logsdir").getNodeValue() : "";
    }

    public String getProjectsDir() throws ConfigurationException {
        return isValid ? buildLoopAttributes.getNamedItem("projectsdir").getNodeValue() : "";
    }

    public String getRMIPort() {
        return isValid ? buildLoopAttributes.getNamedItem("rmiport").getNodeValue() : "";
    }

    public String isConfigFileEditable() {
        return isValid ? featureAttributes.getNamedItem("alloweditconfig").getNodeValue() : "";
    }

    public String isForceBuildEnabled() {
        return isValid ? featureAttributes.getNamedItem("allowforcebuild").getNodeValue() : "";
    }

    public String getCCHome() {
        return isValid ? buildLoopAttributes.getNamedItem("home").getNodeValue() : "";
    }

    public Map getStoryTrackers() {
        if (!isValid) {
            return new HashMap();
        }
        NodeList nodes = document.getElementsByTagName("storytracker");
        Map storyTrackers = new HashMap();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            NamedNodeMap attributes = item.getAttributes();
            StoryTracker storyTracker =
                    new StoryTracker(attributes.getNamedItem("projectname").getNodeValue(), attributes
                            .getNamedItem("baseurl").getNodeValue(), attributes.getNamedItem("keywords")
                            .getNodeValue());
            storyTrackers.put(storyTracker.getProjectName(), storyTracker);
        }
        return storyTrackers;
    }
}
