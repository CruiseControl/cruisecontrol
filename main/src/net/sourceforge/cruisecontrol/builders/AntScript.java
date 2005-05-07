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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.AntBuilder.JVMArg;
import net.sourceforge.cruisecontrol.util.Commandline;

import org.apache.log4j.Logger;


/**
 * Ant script class.
 *
 * Contains all the details related to running a Ant based build via
 * either a batch script or inprocess.
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>
 */
public class AntScript implements Script {
    private static final Logger LOG = Logger.getLogger(AntScript.class);
    
    private Map buildProperties;   

    private boolean isWindows;
    private String antScript;
    private List args;
    private String loggerClassName;
    private String tempFileName = "log.xml";
    private boolean useScript;    
    private boolean useLogger;
    private boolean useQuiet;
    private boolean useDebug;
    private String buildFile = "build.xml";
    private List properties;
    private String target = "";
    private String systemClassPath;
    private int exitCode;
   
    
    /**
     * construct the command that we're going to execute.
     *
     * @param buildProperties Map holding key/value pairs of arguments to the build process
     * @return String[] holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    public String[] getCommandLineArgs() throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        if (useScript) {
            cmdLine.setExecutable(antScript);
        } else {
            if (isWindows) {
                cmdLine.setExecutable("java.exe");
            } else {
                cmdLine.setExecutable("java");
            }
            for (Iterator argsIterator = args.iterator(); argsIterator.hasNext(); ) {
                String arg = ((JVMArg) argsIterator.next()).getArg();
                // empty args may break the command line
                if (arg != null && arg.length() > 0) {
                    cmdLine.createArgument().setValue(arg);
                }
            }
            
            cmdLine.createArgument().setValue("-classpath");
            cmdLine.createArgument().setValue(getAntLauncherJarLocation(systemClassPath, isWindows));
            cmdLine.createArgument().setValue("org.apache.tools.ant.launch.Launcher");
            cmdLine.createArgument().setValue("-lib");
            cmdLine.createArgument().setValue(systemClassPath);
        }

        if (useLogger) {
            cmdLine.createArgument().setValue("-logger");
            cmdLine.createArgument().setValue(getLoggerClassName());
            cmdLine.createArgument().setValue("-logfile");
            cmdLine.createArgument().setValue(tempFileName);
        } else {
            cmdLine.createArgument().setValue("-listener");
            cmdLine.createArgument().setValue(getLoggerClassName());
            cmdLine.createArgument().setValue("-DXmlLogger.file=" + tempFileName);
        }

        // -debug and -quiet only affect loggers, not listeners: when we use the loggerClassName as
        // a listener, they will affect the default logger that writes to the console
        if (useDebug) {
            cmdLine.createArgument().setValue("-debug");
        } else if (useQuiet) {
            cmdLine.createArgument().setValue("-quiet");
        }
        
        for (Iterator propertiesIter = buildProperties.entrySet().iterator(); propertiesIter.hasNext(); ) {
            Map.Entry property = (Map.Entry) propertiesIter.next();
            String value = (String) property.getValue();
            if (!"".equals(value)) {
                cmdLine.createArgument().setValue("-D" + property.getKey() + "=" + value);
            }
        }

        for (Iterator antPropertiesIterator = properties.iterator(); antPropertiesIterator.hasNext(); ) {
            Property property = (Property) antPropertiesIterator.next();
            cmdLine.createArgument().setValue("-D" + property.getName() + "=" + property.getValue());
        }

        cmdLine.createArgument().setValue("-buildfile");
        cmdLine.createArgument().setValue(buildFile);

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument().setValue(targets.nextToken());
        }

        return cmdLine.getCommandline();
    }

    
    /**
     * @return the path to ant-launcher*.jar taken from the given path
     */
    String getAntLauncherJarLocation(String path, boolean isWindows) throws CruiseControlException {
        String separator = isWindows ? ";" : ":";
        StringTokenizer pathTokenizer = new StringTokenizer(path, separator);
        while (pathTokenizer.hasMoreTokens()) {
            String pathElement = pathTokenizer.nextToken();
            if (pathElement.indexOf("ant-launcher") != -1 && pathElement.endsWith(".jar")) {
                return pathElement;
            }
        }
        throw new CruiseControlException("Couldn't find path to ant-launcher jar in this classpath: '" + path + "'");
    }    
    
    /**
     * @param buildProperties The buildProperties to set.
     */
    public void setBuildProperties(Map buildProperties) {
        this.buildProperties = buildProperties;
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
     * @param antScript The antScript to set.
     */
    public void setAntScript(String antScript) {
        this.antScript = antScript;
    }
    /**
     * @param args The args to set.
     */
    public void setArgs(List args) {
        this.args = args;
    }
    /**
     * @param isWindows The isWindows to set.
     */
    public void setWindows(boolean isWindows) {
        this.isWindows = isWindows;
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
     * @param useScript The useScript to set.
     */
    public void setUseScript(boolean useScript) {
        this.useScript = useScript;
    }
    /**
     * @param systemClassPath The systemClassPath to set.
     */
    public void setSystemClassPath(String systemClassPath) {
        this.systemClassPath = systemClassPath;
    }
    /**
     * @param properties The properties to set.
     */
    public void setProperties(List properties) {
        this.properties = properties;
    }
    /**
     * @param target The target to set.
     */
    public void setTarget(String target) {
        this.target = target;
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
