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

import java.util.Date;
import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.jdom.Element;
import org.apache.log4j.Logger;

/**
 * Does the same thing as CurrentBuildStatusPublisher, but also
 * sends it to an FTP server.
 * @deprecated Was obsoleted by {@link net.sourceforge.cruisecontrol.listeners.CurrentBuildStatusFTPListener}
 */
public class CurrentBuildStatusFTPPublisher extends AbstractFTPClass
        implements Publisher {
    private static final Logger LOG = Logger.getLogger(CurrentBuildStatusFTPPublisher.class);

    private String fileName;
    private String destdir;

    public CurrentBuildStatusFTPPublisher() {
        LOG.warn("CurrentBuildStatusFTPPublisher was obsoleted by CurrentBuildStatusFTPListener");
    }

    public void setFile(String fileName) {
        this.fileName = fileName;
    }


    public void setDestDir(String dir) {
        this.destdir = dir;
    }


    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(fileName, "file", this.getClass());
        ValidationHelper.assertIsSet(destdir, "destdir", this.getClass());
        super.validate();
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        String out = makeFile(cruisecontrolLog);
        String fname = destdir + File.separator + fileName;

        sendFileToFTPPath(out, fname);
    }

    protected String makeFile(Element cruisecontrolLog)
            throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);

        long interval = Long.parseLong(helper.getCruiseControlInfoProperty("interval"));
        writeFile(new Date(), interval);

        try {
            return Util.readFileToString(fileName);
        } catch (IOException ioe) {
            throw new CruiseControlException(ioe.getMessage());
        }
    }

    protected void writeFile(Date date, long interval) throws CruiseControlException {
        Date datePlusInterval = new Date(date.getTime() + (interval * 1000));
        CurrentBuildFileWriter.writefile("Next Build Starts At:\n", datePlusInterval, fileName);
    }
}
