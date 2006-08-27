/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.buildloggers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import net.sourceforge.cruisecontrol.BuildLogger;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This ClearCaseAuditLogger will parse a specified configuration record (created as the
 * result of an audited build) and place it into CruiseControl's log.
 * 
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin A. Lee</a>
 *
 */
public class ClearCaseAuditLogger implements BuildLogger {

    private static final Logger LOG = Logger.getLogger(ClearCaseAuditLogger.class);

    private String doFiles;

    /**
     * set the list of comma separated files to retrieve the config recs of
     * @param files comma separated list of derived objects
     */
    public void setDoFiles(String files) {
        this.doFiles = files;
    }
    
    /**
     * check that enough attributes have been set
     */
    public void validate() throws CruiseControlException {
        // check we have at least a configrecfile
        ValidationHelper.assertIsSet(doFiles , "dofiles", this.getClass());
    }

    /**
     * Merge the configuration records of a set of derived objects into the build log 
     * @param buildLog
     * @throws CruiseControlException
     */
    public void log(Element buildLog) throws CruiseControlException {
        String[] doList = splitOnComma(doFiles);
        for (int i = 0; i < doList.length; i++) {
            
            // add an element for audit
            Element auditElement = new Element("audit");
            auditElement.setAttribute("name", doList[i]);
            buildLog.addContent(auditElement);
        
            Commandline commandLine = buildConfigRecCommand(doList[i]);
            LOG.debug("Executing: " + commandLine);
            try {
                Process p = Runtime.getRuntime().exec(commandLine.getCommandline());
                StreamPumper errorPumper =
                    new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
                 new Thread(errorPumper).start();
                 try {
                     InputStreamReader isr = new InputStreamReader(p.getInputStream());
                     BufferedReader br = new BufferedReader(isr);
                     String line;
                     while ((line = br.readLine()) != null) {
                         if (line.startsWith("---")) { 
                             // ignore
                         } else if (line.startsWith("MVFS")) { 
                             // ignore  
                         } else {
                             Element doElement = new Element("do");
                             // removing leading characters
                             line = line.substring(line.indexOf(File.separator), line.length());
                             if (line.indexOf("@") > 0) {
                                 doElement.setAttribute("name", line.substring(0, line.indexOf("@")));
                             } else {
                                 doElement.setAttribute("name", line.substring(0, line.indexOf("<") - 1));
                             }
                             // do we have a element version or another do version
                             if (line.endsWith(">")) {
                                 // element version
                                 doElement.setAttribute("type", "version");
                                 if (line.indexOf("@") > 0) {
                                     doElement.setAttribute("version", line.substring(line.indexOf("@") 
                                         + 2, line.lastIndexOf("<") - 1));
                                 } else {
                                     doElement.setAttribute("version", line.substring(line.indexOf("<")
                                         + 1, line.lastIndexOf(">") - 1)); 
                                 }
                             } else {
                                 // do version
                                 doElement.setAttribute("type", "do");
                                 doElement.setAttribute("version", line.substring(line.indexOf("@") 
                                     + 2, line.length()));
                             }
                             auditElement.addContent(doElement);
                        }
                     }
                 } catch (IOException ioe) {
                     LOG.error("Error executing ClearCase catcr command", ioe);  
                 }
                 p.waitFor();
                 IO.close(p);
             } catch (Exception e) {
                 LOG.error("Error executing ClearCase catcr command", e);
             }
        }
    }
    
    private String[] splitOnComma(String doFiles) {
        return doFiles.split(",");
    }

    /*
     * build a command line for retrieving a configuration record
     */
    protected Commandline buildConfigRecCommand(String file) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArgument().setValue("catcr");
        commandLine.createArgument().setValue("-union");
        commandLine.createArgument().setValue(file);
        return commandLine;
    }      
    
}
