/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.jmx;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.InvalidAttributeValueException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginDetail;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.PluginType;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public interface CruiseControlMBean {

    Properties getVersionProperties();

    String getConfigFileName();
    void setConfigFileName(String fileName) throws InvalidAttributeValueException;

    String getConfigFileContents();
    void setConfigFileContents(String contents) throws CruiseControlException;
    //void validateConfigFile(String contents) throws CruiseControlException;

    List<ProjectInterface> getProjects();
    Map<String, String> getAllProjectsStatus();

    PluginDetail[] getAvailableBootstrappers();
    PluginDetail[] getAvailablePublishers();
    PluginDetail[] getAvailableSourceControls();
    PluginDetail[] getAvailablePlugins();
    PluginType[] getAvailablePluginTypes();
    PluginRegistry getPluginRegistry();

    void pause();
    void resume();
    void halt();
    void reloadConfigFile();
    String getBuildQueueStatus();
    
    /**
     * Gets a PluginInfo representing the metadata for all plugins allowed in the tree.
     * @param projectName Project whose plugin registry will be used, or null to use the root plugin registry.
     * @return A PluginInfo for the root cruisecontrol node, which provides access to all the nodes beneath it.
     * @throws java.util.NoSuchElementException If the project name cannot be resolved.
     */
    PluginInfo getPluginInfo(String projectName);
    
    /**
     * Returns a generated HTML page documenting all the plugins currently loaded in the CruiseControl server'
     * @param projectName Project to generate the html file from, if null root registry will be used
     * @return The HTML content.
     * @throws java.util.NoSuchElementException If the project name cannot be resolved
     */
    String getPluginHTML(String projectName);

    /**
     * Gets the CSS content that can be used when formatting HTML descriptions of individual
     * plugins.
     * @return The CSS content.
     */
    String getPluginCSS();
    
}
