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

import java.util.Date;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.jdom.Element;
import org.apache.commons.net.ftp.FTPClient;

public class CurrentBuildStatusFTPPublisher extends AbstractFTPClass
        implements Publisher {

    private String fileName;
    private String destdir;

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
        ByteArrayInputStream bais = new ByteArrayInputStream(
            out.getBytes());
        
        FTPClient ftp = openFTP();
        
        // we're sending text; don't set binary!
        
        try {
            makeDirsForFile(ftp, fname, null);
            sendStream(ftp, bais, fname);
        } finally {
            closeFTP(ftp);
        }
    }
    
    
    protected String makeFile(Element cruisecontrolLog)
            throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);

        long interval = Long.parseLong(
            helper.getCruiseControlInfoProperty("interval"));
        Date datePlusInterval = new Date((new Date()).getTime()
            + (interval * 1000));

        CurrentBuildFileWriter.writefile(
            "<span>Next Build Starts At:<br>",
            datePlusInterval,
            fileName);
        
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
