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
package net.sourceforge.cruisecontrol.listeners;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Listener;
import net.sourceforge.cruisecontrol.ProjectEvent;
import net.sourceforge.cruisecontrol.ProjectState;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import org.apache.log4j.Logger;

import java.util.Date;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

/**
 * Does the same thing as CurrentBuildStatusListener, but also sends it to an FTP server.
 *
 * Obsoletes both {@link net.sourceforge.cruisecontrol.bootstrappers.CurrentBuildStatusFTPBootstrapper}
 * and {@link net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusFTPPublisher}
 *
 * @see {@link net.sourceforge.cruisecontrol.DateFormatFactory} for the dateformat
 * @author jerome@coffeebreaks.org
 */
public class CurrentBuildStatusFTPListener extends AbstractFTPClass implements Listener {
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusListener.class);
    private String fileName;
    private String destdir;

    public void handleEvent(ProjectEvent event) throws CruiseControlException {
        if (event instanceof ProjectStateChangedEvent) {
            final ProjectStateChangedEvent stateChanged = (ProjectStateChangedEvent) event;
            final ProjectState newState = stateChanged.getNewState();
            LOG.debug("updating status to " + newState.getName()  + " for project " + stateChanged.getProjectName());
            final String text = "<span class=\"link\">" + newState.getDescription() + " since<br>";
            CurrentBuildFileWriter.writefile(text, new Date(), fileName);

            String out = readFileToString(fileName);
            String fname = destdir + File.separator + fileName;

            sendFileToFTPPath(out, fname);
        } else {
            // ignore other ProjectEvents
            LOG.debug("ignoring event " + event.getClass().getName() + " for project " + event.getProjectName());
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(fileName, "file", this.getClass());
        CurrentBuildFileWriter.validate(fileName);
        ValidationHelper.assertIsSet(destdir, "destdir", this.getClass());
    }

    public void setFile(String fileName) {
        this.fileName = fileName.trim();
        LOG.debug("set fileName = " + fileName);
    }

    public void setDestDir(String dir) {
        this.destdir = dir;
        LOG.debug("set destdir = " + dir);
    }

    public String getFileName() {
        return fileName;
    }

    public static String readFileToString(String fileName) throws CruiseControlException {
        FileReader fr = null;
        StringBuffer out = new StringBuffer();
        try {
            fr = new FileReader(fileName);
            char[] buff = new char[4096];
            int size = fr.read(buff, 0, 4096);
            while (size > 0) {
                out.append(buff, 0, size);
                size = fr.read(buff, 0, 4096);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException(ioe.getMessage());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        return out.toString();
    }
}
