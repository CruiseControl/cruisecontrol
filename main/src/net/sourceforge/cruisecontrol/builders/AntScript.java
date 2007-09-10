/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.builders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.File;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.UtilLocator;
import org.apache.log4j.Logger;

/**
 * Ant script class.
 *
 * Contains all the details related to running a Ant based build via
 * either a batch script or inprocess.
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>
 */
public class AntScript implements Script, StreamConsumer {

    private static final Logger LOG = Logger.getLogger(AntScript.class);
    static final String CLASSNAME_DASHBOARD_LISTENER
            = "net.sourceforge.cruisecontrol.builders.AntOutputLogger";

    static final String CLASSNAME_ANTPROGRESS_LOGGER
            = "net.sourceforge.cruisecontrol.builders.AntProgressLogger";

    static final String CLASSNAME_ANTPROGRESS_XML_LOGGER
            = "net.sourceforge.cruisecontrol.builders.AntProgressXmlLogger";
    static final String CLASSNAME_ANTPROGRESS_XML_LISTENER
            = "net.sourceforge.cruisecontrol.builders.AntProgressXmlListener";
    public static final String LIBNAME_PROGRESS_LOGGER = "cruisecontrol-antprogresslogger.jar";
    /**
     * Prefix prepended to system out messages to be detected by AntScript as progress messages.
     * NOTE: Must be the exact same string as that defined in AntProgressLog constant, kept separate
     * to avoid dependence on Ant Builder classes in AntScript.
     */
    static final String MSG_PREFIX_ANT_PROGRESS = "ccAntProgress -- ";

    private Map buildProperties;

    private boolean isWindows;
    private String antScript;
    private List args;
    private List libs;
    private List listeners;
    private String loggerClassName;
    private boolean isLoggerClassNameSet;
    private String tempFileName = "log.xml";
    private boolean useScript;
    private boolean useLogger;
    private boolean useQuiet;
    private boolean useDebug;
    private boolean keepGoing;
    private String buildFile = "build.xml";
    private List properties;
    private String target = "";
    private String systemClassPath;
    private int exitCode;
    private String propertyfile;
    private String progressLoggerLib;
    private Progress progress;

    /**
     * construct the command that we're going to execute.
     *
     * @return Commandline holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    public Commandline buildCommandline() throws CruiseControlException {
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
                String arg = ((AntBuilder.JVMArg) argsIterator.next()).getArg();
                // empty args may break the command line
                if (arg != null && arg.length() > 0) {
                    cmdLine.createArgument(arg);
                }
            }

            cmdLine.createArguments("-classpath", getAntLauncherJarLocation(systemClassPath, isWindows));
            cmdLine.createArgument("org.apache.tools.ant.launch.Launcher");
            cmdLine.createArguments("-lib", systemClassPath);
        }

        if (progress == null) {
            if (useLogger) {
                useLogger(cmdLine);
                cmdLine.createArguments("-logfile", tempFileName);
            } else {
                cmdLine.createArguments("-listener", getLoggerClassName());
                cmdLine.createArgument("-DXmlLogger.file=" + tempFileName);
            }

        } else {
            // need to showProgress

            // use proper default logger if loggerClassName was not specified by config
            setupResolvedLoggerClassname();
                                            // was set to proper default according to showProgress state.
            useLogger(cmdLine);

            if (useLogger) {
                // need to use AntProgressXmlLogger as a listener
                cmdLine.createArguments("-listener", CLASSNAME_ANTPROGRESS_XML_LISTENER);
                cmdLine.createArgument("-DXmlLogger.file=" + tempFileName);

            } else {
                cmdLine.createArguments("-listener", AntBuilder.DEFAULT_LOGGER);
                cmdLine.createArgument("-DXmlLogger.file=" + tempFileName);
            }

            setupDefaultProgressLoggerLib();
            // add -lib to progressLogger classes
            cmdLine.createArguments("-lib", progressLoggerLib);
        }


        // -debug and -quiet only affect loggers, not listeners: when we use the loggerClassName as
        // a listener, they will affect the default logger that writes to the console
        if (useDebug) {
            cmdLine.createArgument("-debug");
        } else if (useQuiet) {
            cmdLine.createArgument("-quiet");
        }

        if (keepGoing) {
            cmdLine.createArgument("-keep-going");
        }

        for (Iterator antLibsIterator = libs.iterator(); antLibsIterator.hasNext(); ) {
            cmdLine.createArguments("-lib", ((AntBuilder.Lib) antLibsIterator.next()).getSearchPath());
        }

        for (Iterator antListenersIterator = listeners.iterator(); antListenersIterator.hasNext(); ) {
            cmdLine.createArguments("-listener", ((AntBuilder.Listener) antListenersIterator.next()).getClassName());
        }

        for (Iterator propertiesIter = buildProperties.entrySet().iterator(); propertiesIter.hasNext(); ) {
            Map.Entry property = (Map.Entry) propertiesIter.next();
            String value = (String) property.getValue();
            if (!"".equals(value)) {
                cmdLine.createArgument("-D" + property.getKey() + "=" + value);
            }
        }

        for (Iterator antPropertiesIterator = properties.iterator(); antPropertiesIterator.hasNext(); ) {
            Property property = (Property) antPropertiesIterator.next();
            cmdLine.createArgument("-D" + property.getName() + "=" + property.getValue());
        }

        if (propertyfile != null) {
            cmdLine.createArguments("-propertyfile", propertyfile);
        }

        cmdLine.createArguments("-buildfile", buildFile);

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument(targets.nextToken());
        }
        return cmdLine;
    }

    private void useLogger(final Commandline cmdLine) {
        cmdLine.createArguments("-logger", getLoggerClassName());
        cmdLine.createArguments("-listener", CLASSNAME_DASHBOARD_LISTENER);
    }

    /**
     * @param path the classpath in which to search for the ant-launcher.jar
     * @param isWindows true if running on Windows
     * @return the path to ant-launcher*.jar taken from the given path
     * @throws CruiseControlException if path to ant-launcher.jar could not be found.
     */
    String getAntLauncherJarLocation(String path, boolean isWindows) throws CruiseControlException {
        final String separator = isWindows ? ";" : ":";
        final StringTokenizer pathTokenizer = new StringTokenizer(path, separator);
        while (pathTokenizer.hasMoreTokens()) {
            final String pathElement = pathTokenizer.nextToken();
            if (pathElement.indexOf("ant-launcher") != -1 && pathElement.endsWith(".jar")) {
                return pathElement;
            }
        }
        throw new CruiseControlException("Couldn't find path to ant-launcher jar in this classpath: '" + path + "'");
    }

    void setupResolvedLoggerClassname() {
        // use proper default logger if loggerClassName was not specified by config
        if ((progress != null) && (!isLoggerClassNameSet)) {
            if (useLogger) {
                // use string to avoid dependence on ant classes
                loggerClassName = CLASSNAME_ANTPROGRESS_XML_LOGGER;
            } else {
                // use string to avoid dependence on ant classes
                loggerClassName = CLASSNAME_ANTPROGRESS_LOGGER;
            }
        } else {

            if (progress != null) {
                LOG.warn("Ant Progress support is enabled AND loggerClassname is set. "
                        + "Be sure the loggerClassName: " + loggerClassName + " is compatible with"
                        + " Ant Progress.");
            }
        }
        LOG.debug("Using loggerClassName: " + loggerClassName);        
    }

    /**
     * Finds the default location of the {@link AntScript#LIBNAME_PROGRESS_LOGGER cruisecontrol-antprogresslogger.jar}
     * by first finding the location of the jar containing the {@link AntScript} class.
     *
     * @return the full path (including jar name) to the jar file
     * ({@link AntScript#LIBNAME_PROGRESS_LOGGER cruisecontrol-antprogresslogger.jar})
     * containing the AntProgressLogger/Listener classes.
     * @throws ProgressLibLocatorException if the search class ({@link AntScript}) file can't be found,
     * likely related to running under Java Webstart >= 6, or simply if the jar can't be found
     */
    public static String findDefaultProgressLoggerLib() throws ProgressLibLocatorException {
        // find path (including filename) to progressLoggerLib jar
        final String progressLoggerLib;

        final File ccMain = UtilLocator.getClassSource(AntScript.class);
        if (ccMain == null) {
            throw new ProgressLibLocatorException(
                "Could not determine -lib path for progressLoggerLib. (Java 6/Webstart issue?) "
                + "\n\tTo enable showProgress, manully copy " + LIBNAME_PROGRESS_LOGGER + " to some directory "
                + "\n\tand set the full path (including filename) to " + LIBNAME_PROGRESS_LOGGER + " in config.xml "
                + "\n\tas the value of 'progressLoggerLib' for this <ant> builder, "
                + "\n\tOR set showProgress=false for this <ant> builder.");
        } else {
            final String pathToDirContainingCCMainJar;
            if (ccMain.isDirectory()) {
                pathToDirContainingCCMainJar = ccMain.getAbsolutePath();
            } else {
                pathToDirContainingCCMainJar = ccMain.getParentFile().getAbsolutePath();
            }

            final File expectedProgressLoggerJar = new File(pathToDirContainingCCMainJar, LIBNAME_PROGRESS_LOGGER);
            if (expectedProgressLoggerJar.exists()) {
                // Use the specific jar if that jar exists.
                // This is a bit of a hack to load the progress logger jar into
                // ant without loading other jars (such as, ant.jar for instance)
                progressLoggerLib = expectedProgressLoggerJar.getAbsolutePath();
            } else {
                // Missing Progress Logger Lib is nasty to debug, so error out here if we can't find it for sure.
                throw new ProgressLibLocatorException("The progressLoggerLib jar file does not exist where expected: "
                        + expectedProgressLoggerJar.getAbsolutePath());
            }
        }
        return progressLoggerLib;
    }

    void setupDefaultProgressLoggerLib() throws ProgressLibLocatorException {
        if (progressLoggerLib == null) {
            // Use a valid default for progressLoggerLib
            progressLoggerLib = findDefaultProgressLoggerLib();
            LOG.debug("Using default progressLoggerLib: " + progressLoggerLib);
        }
    }

    public static final class ProgressLibLocatorException extends CruiseControlException {
        private ProgressLibLocatorException(final String msg) {
            super(msg);
        }
    }
    
    /**
     * Analyze the output of ant command, used to detect errors progress messages.
     */
    public void consumeLine(final String line) {
        if (progress != null && line != null
                && line.startsWith(MSG_PREFIX_ANT_PROGRESS)) {

            progress.setValue(line.substring(MSG_PREFIX_ANT_PROGRESS.length()));
        }
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
     * @param isLoggerClassNameSet The loggerClassName to set.
     */
    public void setIsLoggerClassNameSet(boolean isLoggerClassNameSet) {
        this.isLoggerClassNameSet = isLoggerClassNameSet;
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
    public void setKeepGoing(boolean keepGoing) {
        this.keepGoing = keepGoing;
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
     * @param libs The set of library paths to use.
     */
    public void setLibs(List libs) {
        this.libs = libs;
    }
    /**
     * @param listeners The set of listener classes to use.
     */
    public void setListeners(List listeners) {
        this.listeners = listeners;
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
    /**
     * @param propertyFile The properties file to set.
     */
    public void setPropertyFile(String propertyFile) {
        this.propertyfile = propertyFile;
    }

    /**
     * @param progressLoggerLib The directory containing the AntProgressLogger/Listener classes.
     */
    public void setProgressLoggerLib(final String progressLoggerLib) {
        this.progressLoggerLib = progressLoggerLib;
    }

    /**
     * @param progress The progress callback object to set.
     */
    public void setProgress(final Progress progress) {
        this.progress = progress;
    }
}
