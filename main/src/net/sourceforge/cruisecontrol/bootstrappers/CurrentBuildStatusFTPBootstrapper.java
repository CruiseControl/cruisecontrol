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
package net.sourceforge.cruisecontrol.bootstrappers;

import java.util.Date;
import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.Util;
import org.apache.log4j.Logger;


/**
 * Does the same thing as CurrentBuildStatusBootstrapper, but also
 * sends it to an FTP server.
 * @deprecated Was obsoleted by {@link net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusFTPListener}
 */
public class CurrentBuildStatusFTPBootstrapper extends AbstractFTPClass
        implements Bootstrapper {
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusFTPBootstrapper.class);

    private String fileName;
    private String destdir;

    public CurrentBuildStatusFTPBootstrapper() {
        LOG.warn("CurrentBuildStatusFTPBootstrapper was obsoleted by CurrentBuildStatusFTPListener");
    }

    public void setFile(String fileName) {
        this.fileName = fileName;
    }


    public void setDestDir(String dir) {
        this.destdir = dir;
    }


    public void bootstrap() throws CruiseControlException {
        String out = makeFile();
        String fname = destdir + File.separator + fileName;

        sendFileToFTPPath(out, fname);
    }


    protected String makeFile()
        throws CruiseControlException {
        CurrentBuildFileWriter.writefile(
            "Current Build Started At:\n",
            new Date(),
            fileName);

        try {
            return Util.readFileToString(fileName);
        } catch (IOException ioe) {
            throw new CruiseControlException(ioe.getMessage());
        }
    }

    /*
    protected String makeLogFile() throws CruiseControlException {
        StringBuffer out = new StringBuffer(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<cruisecontrol>"
               + "<modifications />"
               + "<info>"
                    + "<property name=\"lastbuild\" value=\"200301010000000\" />"
                    + "<property name=\"lastsuccessfulbuildbuild\" value=\"200301010000000\" />"
                    + "<property name=\"builddate\" value=\"01/01/2003 00:00:00\" />"
                    + "<property name=\"cctimestamp\" value=\"200301010000000\" />"
                    + "<property name=\"label\" value=\"\" />"
                    + "<property name=\"interval\" value=\"0\" />"
                    + "<property name=\"lastbuildsuccessful\" value=\"true\" />"
                    + "<property name=\"logfile\" value=\"\\logBASE.xml\" />"
                    + "<property name=\"projectname\" value=\"");
        out.append(projectName);
        out.append("\" />"
               + "</info>"
               + "<build error=\"Never built\" />"
            + "</cruisecontrol>");
        return out.toString();
    }
    */


    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(fileName, "file", this.getClass());
        ValidationHelper.assertIsSet(destdir, "destdir", this.getClass());
        super.validate();
    }

}
