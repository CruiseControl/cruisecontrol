/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.Modification.ModifiedFile;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 *  This class implements the SourceControl class for a Plastic SCM repository.
 *
 *  @author <a href="mailto:rdealba@codicesoftware.com">Rubén de Alba</a>
 */
public class PlasticSCM implements SourceControl {

    private static final Logger LOG = Logger.getLogger(PlasticSCM.class);
 
    private String wkspath;
    private String branch;
    private String repository;
    
    private SourceControlProperties properties = new SourceControlProperties();

    //Format passed to Plastic SCM
    public static final String DATEFORMAT = "dd.MM.yyyy.HH.mm.ss";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat (DATEFORMAT);
    
    public static final String DELIMITER = "#@&@#";
    public static final String QUERYFORMAT = DELIMITER + "{item}" + DELIMITER 
                                             + "{owner}" + DELIMITER + "{date}";
 
    /**
     * Selects a workspace
     *
     * @param wkspath
     *          the path of the workspace to work in, in the local filesystem
     */   
    public void setWkspath(String wkspath) {
        this.wkspath = wkspath;
    }

    /**
     * Selects a branch
     *
     * @param branch
     *          the branch in which changes will be looked for.
     */
    public void setBranch (String branch) {
        this.branch = branch;
    }

    /**
     * Selects a repository
     *
     * @param repository
     *          the repository in which changes will be looked for.
     */    
    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }    
    /**
     * Validate the attributes.
     */    
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet (wkspath, "wkspath", this.getClass());
        ValidationHelper.assertIsSet (branch, "branch", this.getClass());

        File workingDir = new File(wkspath);
        ValidationHelper.assertTrue(workingDir.exists(),
                    "'wkspath' must be an existing directory. Was <" + wkspath + ">");
        ValidationHelper.assertTrue(workingDir.isDirectory(),
                    "'wkspath' must be an existing directory, not a file. Was <"
                    + wkspath + ">");
        
    }         

    /**
     *  Returns an {@link java.util.List List} of {@link Modification}s detailing all the changes between now
     *  and the last build.
     *
     *@param  lastBuild the last build time
     *@param  now time now, or time to check
     *@return  the list of modifications, an empty (not null) list if no
     *      modifications or if developer had checked in files since quietPeriod seconds ago.
     *
     */
    public List getModifications(Date lastBuild, Date now) {    
        List modifications = new ArrayList();
        try {
            Commandline commandLine = buildFindCommand(lastBuild, now);
            Process p = commandLine.execute(); 
            InputStream input = p.getInputStream();
            modifications = parseStream(input);
            p.waitFor();
            IO.close(p);            
            
        } catch (Exception e) {
            LOG.error("Error in executing the PlasticSCM command : ", e);
            return new ArrayList();
        }
        
        if (!modifications.isEmpty()) {
            properties.modificationFound();
        }
        
        return modifications;
    }
 
    /**
     * Build the Plastic SCM find command.
     */   
    protected Commandline buildFindCommand(Date lastBuild, Date now) throws CruiseControlException 
    {
        Commandline commandLine = new Commandline();

        commandLine.setWorkingDirectory(wkspath);
        
        commandLine.setExecutable("cm");
        commandLine.createArgument("find");
        commandLine.createArgument("revision");
        commandLine.createArgument("where");
        commandLine.createArguments("branch", "=");
        commandLine.createArgument("'" + branch + "'");
        commandLine.createArguments("and", "revno");
        commandLine.createArguments("!=", "'CO'");
        commandLine.createArguments("and", "date");
        commandLine.createArguments("between", "'" + dateFormat.format(lastBuild) + "'");
        commandLine.createArguments("and", "'" + dateFormat.format(now) + "'");
        
        if (repository != null) {
            commandLine.createArguments("on" , "repository");
            commandLine.createArgument("'" + repository + "'");
        }
        
        commandLine.createArgument("--dateformat=\"" + DATEFORMAT + "\"");
        commandLine.createArgument("--format=\"" + QUERYFORMAT + "\"");
        
        return commandLine;
    }

    /**
     * Parse the find command output.
     */   
    protected List parseStream(InputStream input) throws IOException, ParseException 
    {
        ArrayList modifications = new ArrayList();
        ArrayList filemodifications = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        
        while ((line = reader.readLine()) != null) {
            
            if (!line.startsWith(DELIMITER)) {
                continue;
            }
            
            if (!line.equals("") && !line.startsWith("Total:")) {
                String[] fields = line.split(DELIMITER);
                File file = new File (fields[1]);
                Modification mod = new Modification ("plasticscm");
                ModifiedFile modfile = mod.createModifiedFile(file.getName(), file.getParent());
                mod.userName = fields[2];
                mod.modifiedTime = dateFormat.parse(fields[3]);
                if (file.exists() && file.isFile() && !filemodifications.contains(file)) {
                    filemodifications.add(file);
                    modifications.add(mod);
                }
            }
        }
        return modifications;
    }
}