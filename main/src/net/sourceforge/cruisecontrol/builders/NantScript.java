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
package net.sourceforge.cruisecontrol.builders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.util.Commandline;

/**
 * NAnt script class.
 *
 * Contains all the details related to running a NAnt based build.
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>
 */
public class NantScript implements Script {
    private Map buildProperties;
    private List nantProperties;

    private String loggerClassName;
    private String tempFileName = "log.xml";    
    private boolean useLogger;
    private boolean useQuiet;
    private boolean useDebug;
    private String buildFile = "default.build";
    private String target = "";
    private String targetFramework;
    private int exitCode;

    /**
     * construct the command that we're going to execute.
     *
     * @return Commandline holding command to be executed
     */
    public Commandline buildCommandline() {
        Commandline cmdLine = getCommandLine();

        cmdLine.setExecutable("NAnt.exe");
        if (useLogger) {
            cmdLine.createArgument("-logger:" + getLoggerClassName());
            cmdLine.createArgument("-logfile:" + tempFileName);
        } else {
            cmdLine.createArgument("-listener:" + getLoggerClassName());
            cmdLine.createArgument("-D:XmlLogger.file=" + tempFileName);
        }
        /*
        if (useVerbose) {
            cmdLine.createArgument("-verbose");
        }
        */
        if (useDebug) {
            cmdLine.createArgument("-debug+");
        } else if (useQuiet) {
            cmdLine.createArgument("-quiet+");
        }
        if (targetFramework != null) {
            cmdLine.createArgument("-t:" + targetFramework);
        }

        for (Iterator propertiesIter = buildProperties.entrySet().iterator(); propertiesIter.hasNext();) {
            Map.Entry property = (Map.Entry) propertiesIter.next();
            String value = (String) property.getValue();
            if (!"".equals(value)) {
                cmdLine.createArgument("-D:" + property.getKey() + "=" + value);
            }
        }
        for (Iterator nantPropertiesIterator = nantProperties.iterator(); nantPropertiesIterator.hasNext(); ) {
            Property property = (Property) nantPropertiesIterator.next();
            cmdLine.createArgument("-D:" + property.getName() + "=" + property.getValue());
        }

        cmdLine.createArgument("-buildfile:" + buildFile);

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument(targets.nextToken());
        }
        return cmdLine;
    }

    // factory method for mock...
    protected Commandline getCommandLine() {
        return new Commandline();
    }

    /**
     * @param buildProperties The buildProperties to set.
     */
    public void setBuildProperties(Map buildProperties) {
        this.buildProperties = buildProperties;
    }
    
    public void setNantProperties(List properties) {
        this.nantProperties = properties;
    }

    /**
     * @return Returns the loggerClassName.
     */
    public String getLoggerClassName() {
        return loggerClassName;
    }
    /**
     * @param loggerClassName The loggerClassName to set.
     */
    public void setLoggerClassName(String loggerClassName) {
        this.loggerClassName = loggerClassName;
    }
    /**
     * @param buildFile The buildFile to set.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }
    /**
     * @param tempFileName The tempFileName to set.
     */
    public void setTempFileName(String tempFileName) {
        this.tempFileName = tempFileName;
    }
    /**
     * @param useDebug The useDebug to set.
     */
    public void setUseDebug(boolean useDebug) {
        this.useDebug = useDebug;
    }
    /**
     * @param useVerbose The useDebug to set.
     */
    /*
    public void setUseVerbose(boolean useVerbose) {
        this.useVerbose = useVerbose;
    }
    */
    /**
     * @param useLogger The useLogger to set.
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }
    /**
     * @param useQuiet The useQuiet to set.
     */
    public void setUseQuiet(boolean useQuiet) {
        this.useQuiet = useQuiet;
    }
    /**
     * @param target The target to set.
     */
    public void setTarget(String target) {
        this.target = target;
    }
    /**
     * @param targetFramework The targetFramework to set.
     */
    public void setTargetFramework(String targetFramework) {
        this.targetFramework = targetFramework;
    }
    /**
     * @return Returns the exitCode.
     */
    public int getExitCode() {
        return exitCode;
    }
    /**
     * @param exitCode The exitCode to set.
     */
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
