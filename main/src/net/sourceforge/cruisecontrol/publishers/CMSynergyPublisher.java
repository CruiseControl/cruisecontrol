/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

/**
 * Provides an abstract base class to handle the functionality common to all CM
 * Synergy publishers.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith </a>
 */
public abstract class CMSynergyPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(CMSynergyPublisher.class);

    /**
     * The file which contains the mapping between CM Synergy session names
     * and IDs.
     */
    private File sessionFile;

    /**
     * The given name of the CM Synergy session to use.
     */
    private String sessionName;

    /**
     * The CM Synergy project spec (2 part name) of the project we will
     * be referencing.
     */
    private String projectSpec;

    /**
     * The CM Synergy executable used for executing commands. If not set,
     * we will use the default value "ccm".
     */
    private String ccmExe;

    /**
     * Sets the file which contains the mapping between CM Synergy session names
     * and IDs. This file should be in the standard properties file format. Each
     * line should map one name to a CM Synergy session ID (as returned by the
     * "ccm status" command).
     * <p>
     * example:
     * <br><br>
     * session1=localhost:65024:192.168.1.17
     *
     * @param sessionFile
     *            The session file
     */
    public void setSessionFile(String sessionFile) {
        this.sessionFile = new File(sessionFile);
    }

    /**
     * Returns the session file which maps CM Synergy session names to CM
     * Synergy session IDs.
     *
     * @return The session file.
     */
    File getSessionFile() {
        return this.sessionFile;
    }

    /**
     * Sets the name of the CM Synergy session to use with this plugin. This
     * name should appear in the specified session file.
     *
     * @param sessionName
     *            The session name
     *
     * @see #setSessionFile(String)
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    /**
     * Returns the name of the CM Synergy session used with this publisher.
     *
     * @return The CM Synergy session name.
     */
    String getSessionName() {
        return this.sessionName;
    }

    /**
     * Sets the CM Synergy project to be used with this publisher.
     *
     * @param projectSpec
     *            The project spec (in 2 part name format).
     */
    public void setProject(String projectSpec) {
        this.projectSpec = projectSpec;
    }

    /**
     * Gets the CM Synergy project to be used with this publisher.
     *
     * @return The CM Synergy project (in 2 part name format), or
     *         <code>null</code> if it was not set.
     */
    protected String getProject() {
        return this.projectSpec;
    }

    /**
     * Sets the name of the CM Synergy executable to use when issuing
     * commands.
     *
     * @param ccmExe the name of the CM Synergy executable
     */
    public void setCcmExe(String ccmExe) {
        this.ccmExe = ccmExe;
    }

    /**
     * Gets the full path of the ccm command line executable.
     *
     * @return The full path of the ccm command line executable, or
     *         <code>null</code> if it was not set.
     */
    String getCcmExe() {
        return this.ccmExe;
    }

    /**
     * Uses the log to determine if the build was successful.
     *
     * @param log
     *            The Cruise Control log (as a JDOM Element).
     *
     * @return <code>true</code> if the build was successful,
     *         <code>false</code> otherwise.
     */
    private boolean isBuildSuccessful(final Element log) {
        final XMLLogHelper helper = new XMLLogHelper(log);
        return helper.isBuildSuccessful();
    }

    /**
     * Extracts the build properties from the Cruise Control log
     *
     * @param log The log
     *
     * @return The properties set within the current build.
     */
    Properties getBuildProperties(Element log) {
        final Properties buildProperties = new Properties();

        for (Object o : log.getChild("info").getChildren("property")) {
            final Element property = (Element) o;
            buildProperties.put(property.getAttributeValue("name"), property
                    .getAttributeValue("value"));

        }

        return buildProperties;
    }

    /**
     * Extract a list of CM Synergy modifications from the Cruise Control log
     *
     * @param log
     *            The Cruise Control log (as a JDOM element).
     *
     * @return A <code>List</code> of new CM Synergy tasks
     */
    List<String> getNewTasks(final Element log) {
        final List<String> taskList = new ArrayList<String>();

        // Get the modification list from the log
        final Element modifications = log.getChild("modifications");
        if (modifications != null) {
            // From this list, extract all CM Synergy modifications (tasks)
            for (Object o : modifications.getChildren("modification")) {
                final Element modification = (Element) o;
                final String type = modification.getAttributeValue("type");
                if (type != null && type.equals("ccmtask")) {
                    final String task = modification.getChild("task").getText();
                    if (task != null) {
                        taskList.add(task.trim());
                    }
                }
            }
        }

        return taskList;
    }

    /**
     * Determines if the publish should take place.
     *
     * @param log
     *            The Cruise Control log (as a JDOM element).
     * @return true if the build was successful and new CM Synergy tasks were
     *         found, false otherwise.
     */
    public boolean shouldPublish(Element log) {
        // Only publish upon a successful build.
        if (!isBuildSuccessful(log)) {
            LOG.info("Build failed. Skipping publisher.");
            return false;
        }

        // Do not publish if no new tasks were found
        List newTasks = getNewTasks(log);
        if (newTasks.size() < 1) {
            LOG.info("No new CM Synergy tasks in build. Skipping publisher.");
            return false;
        }

        return true;
    }
    
    /**
     * Finds out which version of Synergy is in use. Useful to set flags for features
     * available only to newer Synergy versions. 
     *  
     * @return A double representing the version of Synergy
     */
    protected double getVersion() {

        final ManagedCommandline versionCmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        versionCmd.clearArgs();
        versionCmd.createArgument("version");
        versionCmd.createArgument("-c");

        // Execute the command
        try {
            versionCmd.execute();
        } catch (Exception e) {
            LOG.warn("Could not get Synergy version", e);
        }

        final String versionString = versionCmd.getStdoutAsString();
        final String[] versionList = versionString.split("\r\n|\r|\n");
        return Double.parseDouble(versionList[versionList.length - 1]);
    }
}
