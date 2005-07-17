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
package net.sourceforge.cruisecontrol.bootstrappers;

import java.util.Date;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.util.AbstractFTPClass;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import org.apache.commons.net.ftp.FTPClient;


/**
 * Does the same thing as CurrentBuildStatusBootstrapper, but also
 * sends it to an FTP server.
 */
public class CurrentBuildStatusFTPBootstrapper extends AbstractFTPClass
        implements Bootstrapper {

    private String fileName;
    private String thisHostName;
    private String destdir;
    private int thisCCPort = 0;
    private String controlsPropsName = "controls.properties";
    private String projectName = "not-set";

    public void setFile(String fileName) {
        this.fileName = fileName;
    }
    
    
    public void setBuildHost(String host) {
        this.thisHostName = host;
    }
    
    
    public void setBuildPort(int port) {
        this.thisCCPort = port;
    }
    
    
    public void setDestDir(String dir) {
        this.destdir = dir;
    }
    
    
    public void setControlsPropertiesName(String name) {
        this.controlsPropsName = name;
    }

    // FIXME unused
    public void setProjectName(String name) {
        this.projectName = name;
    }
    

    public void bootstrap() throws CruiseControlException {
        String out = makeFile();
        String propText = null;
        String fname = destdir + File.separator + fileName;
        if (thisHostName != null) {
            propText = "MBean\\ Controller=http://" + thisHostName
                + ":" + thisCCPort
                + "/ViewObjectRes//CruiseControl+Project%3Aname%3D>\n";
        }

        ByteArrayInputStream baisF = new ByteArrayInputStream(
            out.getBytes());
        
        //String logName = destdir + File.separator + "logBASE.xml";
        //String baseLog = makeLogFile();
        //ByteArrayInputStream baisL = new ByteArrayInputStream(
        //    baseLog.getBytes());
        
        FTPClient ftp = openFTP();
        
        // we're sending text; don't set binary!
        
        try {
            makeDirsForFile(ftp, fname, null);
            sendStream(ftp, baisF, fname);
            
            //makeDirsForFile(ftp, logName, null);
            //sendStream(ftp, baisL, logName);
            
            if (propText != null && controlsPropsName != null) {
                ByteArrayInputStream baisP = new ByteArrayInputStream(
                    propText.getBytes());
                sendStream(ftp, baisP, destdir + File.separator
                    + controlsPropsName);
            }
        } finally {
            closeFTP(ftp);
        }
    }
    
    
    protected String makeFile()
        throws CruiseControlException {
        CurrentBuildFileWriter.writefile(
            "<span class=\"link\">Current Build Started At:<br>",
            new Date(),
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
            try {
                fr.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        return out.toString();
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
