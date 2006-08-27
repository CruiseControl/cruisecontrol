/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
package net.sourceforge.cruisecontrol.publishers;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.sourcecontrols.CMSynergy;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * After a successful build, this class copies newly checked in tasks to a given
 * CM Synergy folder.
 * 
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith </a>
 */
public class CMSynergyTaskPublisher extends CMSynergyPublisher {
    
    private static final Logger LOG = Logger.getLogger(CMSynergyTaskPublisher.class);
    private String folderNumber;
    private String folderName;

    /**
     * Sets the name (or a substring of the name) of the folder which will 
     * recieve the new tasks. This folder must exist within the reconfigure
     * properties of the given project.
     * 
     * @param folderName The folder name
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    
    /**
     * The number of the folder which will receive the new tasks.
     * 
     * @param folderNumber The folder number
     */
    public void setFolderNumber(String folderNumber) {
        this.folderNumber = folderNumber;
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(Element log) throws CruiseControlException {

        // Only publish upon a successful build which includes new tasks.
        if (!shouldPublish(log)) {
            return;
        }
        
        // If need be, look up the folder number
        if (folderNumber == null) {
            folderNumber = getFolderNumber(folderName, getProject());
        }

        // Get a string based list of new tasks
        StringBuffer tasks = new StringBuffer();
        List newTasksList = getNewTasks(log);
        Iterator newTasks = newTasksList.iterator();
        for (int index = 0; newTasks.hasNext(); index++) {
            if (index > 0) {
                tasks.append(",");
            }
            tasks.append(newTasks.next());
        }
        
        LOG.info("Copying " + newTasksList.size() + " task(s) into folder "
                + folderNumber + ".");

        // Create our CM Synergy command
        ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        cmd.createArgument("folder");
        cmd.createArgument("-modify");
        cmd.createArguments("-add_tasks", tasks.toString());
        cmd.createArgument(folderNumber);

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            throw new CruiseControlException(
                    "Failed to copy new tasks to folder " + folderNumber, e);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.cruisecontrol.Publisher#validate()
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(folderNumber == null && folderName == null,
            "Must specify either 'folderName' or 'folderNumber'");

        ValidationHelper.assertFalse(folderName != null && getProject() == null,
            "'project' attribute must be set when using the 'folderName' attribute.");
    }
    
    /**
     * Get the CM synergy folder number matching the folder with the given name
     * in the given project.
     * 
     * @param folderName
     *            A case sensitive substring of the folder name.
     * @param project
     *            The CM Synergy project in which the folder exists (in 2 part
     *            name format).
     * 
     * @return The folder number.
     * 
     * @throws CruiseControlException
     *             if the folder number can not be found.
     */
    private String getFolderNumber(String folderName, String project) throws CruiseControlException {
        // Get a list of folders in the project
        ManagedCommandline cmd = CMSynergy.createCcmCommand(
                getCcmExe(), getSessionName(), getSessionFile());
        cmd.createArgument("reconfigure_properties");
        cmd.createArgument("-u");
        cmd.createArgument("-f");
        cmd.createArgument(
                "%description" + CMSynergy.CCM_ATTR_DELIMITER + "%name");
        cmd.createArguments("-show", "folders");
        cmd.createArgument(project);

        try {
            cmd.execute();
            cmd.assertExitCode(0);
        } catch (Exception e) {
            throw new CruiseControlException("Could not get a list of folders in project " + project, e);
        }

        // Try to find a matching folder name
        for (Iterator folders = cmd.getStdoutAsList().iterator(); folders
                .hasNext();) {
            String line = (String) folders.next();
            if (line.indexOf(folderName) > -1) {
                int index = line.indexOf(CMSynergy.CCM_ATTR_DELIMITER);
                if (index == -1) {
                    LOG.warn("Bad format in result: \"" + line + "\"");
                    continue;
                }
                index += CMSynergy.CCM_ATTR_DELIMITER.length();
                return line.substring(index).trim();
            }
        }

        // If we've gotten this far, no such folder exists in the project
        throw new CruiseControlException("Could not find a folder matching \""
                + folderName + "\" in project \"" + project + "\".");
    }

}
