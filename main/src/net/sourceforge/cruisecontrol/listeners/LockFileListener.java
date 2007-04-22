/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol.listeners;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

public class LockFileListener implements Listener {

    private static final Logger LOG = Logger.getLogger(LockFileListener.class);

    private String path;
    private String projectName;

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (!(event instanceof ProjectStateChangedEvent)) {
            return;
        }

        if (!ProjectState.IDLE.equals(((ProjectStateChangedEvent) event).getNewState())) {
            return;
        }
        
        File lock = new File(path);
        String message = "Project " + event.getProjectName() + " failed to delete lock file " + path;
        
        if (!lock.exists()) {
            LOG.debug(message);
            LOG.debug("Lockfile didn't exist");
            return;
        }
        
        String nameInLockFile = getFileContents(lock);
        if (!projectName.equalsIgnoreCase(nameInLockFile)) {
            LOG.debug(message);
            LOG.debug("Content (" + nameInLockFile + ") didn't match projectName attribute (" + projectName + ")");
            return;
        }

        boolean deletedFile = attemptToDeleteLock();
        if (!deletedFile) {
            throw new CruiseControlException(message);
        }
    }

    String getFileContents(File lock) throws CruiseControlException {
        String nameInLockFile;
        try {
            nameInLockFile = Util.readFileToString(lock);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
        return nameInLockFile;
    }

    boolean attemptToDeleteLock() {
        return new File(path).delete();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(path, "lockfile", LockFileListener.class);
        ValidationHelper.assertIsSet(projectName, "projectName", LockFileListener.class);
    }

    public void setLockFile(String path) {
        this.path = path;
    }

    public void setProjectName(String string) {
        projectName = string;
    }

}
