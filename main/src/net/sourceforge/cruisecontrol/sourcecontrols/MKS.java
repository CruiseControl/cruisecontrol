/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControlElement methods for a MKS repository.
 * The call to MKS is assumed to work with any setup: 
 * The call to MKS login should be done prior to calling this class. 
 *  * 
 * attributes: 
 * localWorkingDir - local directory for the sandbox  
 * project - the name and path to the MKS project
 * doNothing - if this attribute is set to true, no mks command is executed. This is for
 * testing purposes, if a potentially slow mks server connection should avoid
 * 
 * @author Suresh K Bathala Skila, Inc.
 * @author Dominik Hirt, Wincor-Nixdorf International GmbH, Leipzig
 */
public class MKS implements SourceControl {
    private static final Logger LOG = Logger.getLogger(MKS.class);

    private SourceControlProperties properties = new SourceControlProperties();
    private String project;
    private File localWorkingDir;

    /**
     * if this attribute is set to true, no mks command is executed. This is for
     * testing purposes, if a potentially slow mks server connection should
     * avoid
     */
    private boolean doNothing;

    /**
     * This is the workaround for the missing feature of MKS to return differences 
     * for a given time period. If a modification is detected during the quietperiod, 
     * CruiseControl calls <code>getModifications</code> of this sourcecontrol object 
     * again, with the new values for <code>Date now</code>. In that case, and if all
     * modification are already found in the first cycle, the list of modifications 
     * becomes empty. Therefor, we have to return the summarized list of modifications: 
     * the values from the last run,  and -maybe- results return by MKS for this run.
     */
    private List listOfModifications = new ArrayList();
    
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Sets the local working copy to use when making calls to MKS.
     * 
     * @param local
     *            String indicating the relative or absolute path to the local
     *            working copy of the module of which to find the log history.
     */
    public void setLocalWorkingDir(String local) {
        localWorkingDir = new File(local);
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setDoNothing(String doNothing) {
        this.doNothing = new Boolean(doNothing).booleanValue();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(localWorkingDir, "localWorkingDir", this.getClass());
        ValidationHelper.assertIsSet(project, "project", this.getClass());
    }

    /**
     * Returns an ArrayList of Modifications. 
     * MKS ignores dates for such a range so therefor ALL 
     * modifications since the last resynch step are returned.
     * 
     * @param lastBuild
     *            Last build time.
     * @param now
     *            Time now, or time to check.
     * @return maybe empty, never null.
     */
    public List getModifications(Date lastBuild, Date now) {
        
        int numberOfFilesForDot = 0;
        boolean printCR = false;
        
        if (doNothing) {
            properties.modificationFound();
            return listOfModifications;
        }
        String cmd;

        String projectFilePath = localWorkingDir.getAbsolutePath() + File.separator + project;
        if (!new File(projectFilePath).exists()) {
            throw new RuntimeException("project file not found at " + projectFilePath);
        }
        cmd = new String("si resync -f -R -S " + projectFilePath);

        /* Sample output:
         * output: Connecting to baswmks1:7001 ... Connecting to baswmks1:7001
         * as dominik.hirt ... Resynchronizing files...
         * c:\temp\test\Admin\ComponentBuild\antfile.xml
         * c:\temp\test\Admin\PCEAdminCommand\projectbuild.properties: checked
         * out revision 1.1
         */

        try {
            LOG.debug(cmd);
            Process proc = Runtime.getRuntime()
                    .exec(cmd, null, localWorkingDir);
            logStream(proc.getInputStream(), System.out);
            InputStream in = proc.getErrorStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            String line = reader.readLine();

            while (line != null) {
                int idxCheckedOutRevision = line
                        .indexOf(": checked out revision");

                if (idxCheckedOutRevision == -1) {
                    numberOfFilesForDot++;
                    if (numberOfFilesForDot == 20) {
                        System.out.print("."); // don't use LOG, avoid linefeed
                        numberOfFilesForDot = 0;
                        printCR = true;
                    }
                    line = reader.readLine();
                    continue;
                }
                if (printCR) {
                    System.out.println(""); // avoid LOG prefix 'MKS - ' 
                    printCR = false;
                }
                LOG.info(line);

                int idxSeparator = line.lastIndexOf(File.separator);
                String folderName = line.substring(0, idxSeparator);
                String fileName = line.substring(idxSeparator + 1,
                        idxCheckedOutRevision);
                Modification modification = new Modification();
                Modification.ModifiedFile modFile = modification
                        .createModifiedFile(fileName, folderName);
                modification.modifiedTime = new Date(new File(folderName,
                        fileName).lastModified());
                modFile.revision = line.substring(idxCheckedOutRevision + 23);
                modification.revision = modFile.revision;
                setUserNameAndComment(modification, folderName, fileName);

                listOfModifications.add(modification);

                line = reader.readLine();

                properties.modificationFound();
            }
            proc.waitFor();
            proc.getInputStream().close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();

        } catch (Exception ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        System.out.println(); // finishing dotted line, avoid LOG prefix 'MKS - '
        LOG.info("resync finished");

        return listOfModifications;
    }

    /**
     * Sample output:
     * dominik.hirt;add forceDeploy peter.neumcke;path to properties file fixed,
     * copy generated properties Member added to project
     * d:/MKS/PCE_Usedom/Products/Info/Info.pj
     * 
     * @param fileName
     */
    private void setUserNameAndComment(Modification modification,
            String folderName, String fileName) {
                
        try {
            Commandline commandLine = new Commandline();
            commandLine.setExecutable("si");
    
            if (localWorkingDir != null) {
                commandLine.setWorkingDirectory(localWorkingDir.getAbsolutePath());
            }
    
            commandLine.createArgument().setValue("rlog");
            commandLine.createArgument().setValue("--format={author};{description}");
            commandLine.createArgument().setValue("--noHeaderFormat");
            commandLine.createArgument().setValue("--noTrailerFormat");
            commandLine.createArgument().setValue("-r");
            commandLine.createArgument().setValue(modification.revision);
            commandLine.createArgument().setValue(folderName + File.separator + fileName);
              
            LOG.debug(commandLine.toString());
            
            Process proc = commandLine.execute();
            
            logStream(proc.getErrorStream(), System.err);
            InputStream in = proc.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            String line = reader.readLine();
            LOG.debug(line);

            int idx = line.indexOf(";");
            while (idx == -1) {
                line = reader.readLine(); // unknown output, read again
                LOG.debug(line);
                idx = line.indexOf(";");
            }

            modification.userName = line.substring(0, idx);
            modification.comment = line.substring(idx + 1);
            
            proc.waitFor();
            proc.getInputStream().close();
            proc.getOutputStream().close();
            proc.getErrorStream().close();
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            modification.userName = "";
            modification.comment = "";
        }
    }

    private static void logStream(InputStream inStream, OutputStream outStream) {
        StreamPumper errorPumper = new StreamPumper(inStream, new PrintWriter(
                outStream, true));
        new Thread(errorPumper).start();
    }
}
