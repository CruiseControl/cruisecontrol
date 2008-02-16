/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.dashboard.service;

import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardXmlConfigService implements DashboardConfigService, InitializingBean {
    private static final Logger LOGGER = Logger.getLogger(DashboardXmlConfigService.class);

    private boolean isValid;

    private NamedNodeMap featureAttributes;

    private NamedNodeMap buildLoopAttributes;

    private Document document;

    private DashboardConfigFileFactory factory;

    public DashboardXmlConfigService(DashboardConfigFileFactory factory) {
        this.factory = factory;
    }

    public void afterPropertiesSet() throws Exception {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder newDocumentBuilder = builderFactory.newDocumentBuilder();
            document = newDocumentBuilder.parse(factory.asStream());
            featureAttributes = getAttributesMapForSingleTag("features");
            buildLoopAttributes = getAttributesMapForSingleTag("buildloop");
            isValid = true;
        } catch (Exception e) {
            LOGGER.warn(e);
            isValid = false;
        }
    }

    private NamedNodeMap getAttributesMapForSingleTag(String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        Node node = nodes.item(0);
        return node.getAttributes();
    }

    public String getArtifactsDir() throws ConfigurationException {
        return isValid ? buildLoopAttributes.getNamedItem("artifactsdir").getNodeValue() : "";
    }

    public String getLogsDir() throws ConfigurationException {
        return isValid ? buildLoopAttributes.getNamedItem("logsdir").getNodeValue() : "";
    }

    public String isForceBuildEnabled() {
        return isValid ? featureAttributes.getNamedItem("allowforcebuild").getNodeValue() : "";
    }

    public Map getStoryTrackers() {
        if (!isValid) {
            return new HashMap();
        }
        NodeList nodes = document.getElementsByTagName("trackingtool");
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

    public File getConfigurationFile() {
        return factory.getDashboardConfigFileLocation();
    }
    
    public boolean isDashboardConfigFileValid(File configFile) {
        return configFile != null && configFile.exists()
                && configFile.getName().endsWith(".xml");
    }

    public List getSubTabClassNames() {
        if (!isValid) {
            return new ArrayList();
        }
        NodeList nodes = document.getElementsByTagName("subtab");
        List subtabClassNames = new ArrayList();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node item = nodes.item(i);
            NamedNodeMap attributes = item.getAttributes();
            subtabClassNames.add(attributes.getNamedItem("class").getNodeValue());
        }
        return subtabClassNames;
    }

}
