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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.ConfigurationHandler;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.VCS;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

public class ConfigXmlFileService {

    public static final String CC_CONFIG = "cc.config";

    public static final String CC_CONFIG_EDITABLE = "cc.config.editable";

    public static final String CRUISE_CONFIG_FILE = "CRUISE_CONFIG_FILE";

    public static final String CRUISE_CONFIG_EDITABLE = "CRUISE_CONFIG_EDITABLE";

    private final OSEnvironment oSEnvironment;

    private final TemplateRenderService renderService;

    // this is only here for unit tests. should refactor those.
    public ConfigXmlFileService(OSEnvironment env) {
        this(env, getDefaultRenderService());
    }

    private static TemplateRenderService getDefaultRenderService() {
        TemplateRenderService renderService = new TemplateRenderService();
        try {
            renderService.loadTemplates();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return renderService;
    }

    public ConfigXmlFileService(OSEnvironment env, TemplateRenderService renderService) {
        this.oSEnvironment = env;
        this.renderService = renderService;
    }

    public String readContentFromConfigFile(String pathToConfigFile) {
        File config = new File(StringUtils.defaultString(pathToConfigFile));
        if (!config.exists()) {
            return "";
        }
        try {
            return FileUtils.readFileToString(config, null);
        } catch (IOException e) {
            return "";
        }
    }

    public void writeContentToConfigXml(String pathToConfigFile, String configFileContent)
            throws IOException {
        FileUtils.writeStringToFile(new File(pathToConfigFile), configFileContent, null);
    }

    public File getConfigXmlFile(File cruiseConfigFile) {
        if (isConfigFileValid(cruiseConfigFile)) {
            return cruiseConfigFile;
        }
        File configFile = new File(getConfigProperty(CRUISE_CONFIG_FILE, CC_CONFIG));
        if (isConfigFileValid(configFile)) {
            return configFile;
        }
        return null;
    }

    public boolean isConfigFileValid(File cruiseConfigFile) {
        return cruiseConfigFile != null && cruiseConfigFile.exists()
                && cruiseConfigFile.getName().endsWith(".xml");
    }

    public net.sourceforge.cruisecontrol.dashboard.Projects getProjects(File cruiseConfigFile) {
        if (cruiseConfigFile == null) {
            return null;
        } else {
            Projects projects = null;
            try {
                projects = new Projects(cruiseConfigFile);
                ConfigurationHandler handler = new ConfigurationHandler(projects);
                SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                saxParser.parse(cruiseConfigFile, handler);
                return projects;
            } catch (Exception e) {
                return projects;
            }
        }
    }

    public void addProject(File cruiseConfigFile, String projectName, VCS vcs) {
        Map map = new HashMap();
        map.put("$projectname", projectName);
        map.put("$bootstrapper", vcs.getBootStrapper());
        map.put("$repository", vcs.getRepository());
        String xml = renderService.renderTemplate("project_xml.template", map);
        String content;
        try {
            content = FileUtils.readFileToString(cruiseConfigFile, null);
        } catch (IOException e) {
            content = "";
        }
        content = StringUtils.replace(content, "</cruisecontrol>", "");
        try {
            this.writeContentToConfigXml(cruiseConfigFile.getAbsolutePath(), content + xml
                    + "\n</cruisecontrol>");
        } catch (IOException e) {
            throw new RuntimeException("");
        }
    }

    private String getConfigProperty(String configFile, String fileProperties) {
        String systemProperty = System.getProperty(fileProperties);
        String env = oSEnvironment.getVariable(configFile);
        return StringUtils.isNotBlank(systemProperty) ? systemProperty : StringUtils
                .defaultString(env);
    }

    public boolean isConfigFileEditable() {
        String configProperty = getConfigProperty(CRUISE_CONFIG_EDITABLE, CC_CONFIG_EDITABLE);
        if (StringUtils.isEmpty(configProperty)) {
            return true;
        } else {
            return BooleanUtils.toBoolean(configProperty);
        }
    }
}
