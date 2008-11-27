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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.BuildOutputLoggerManager;
import net.sourceforge.cruisecontrol.util.EmptyElementFilter;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.apache.log4j.Logger;

/**
 * we often see builds that fail because the previous build is still holding on to some resource.
 * we can avoid this by just building in a different process which will completely die after every
 * build.
 */
public class AntBuilder extends Builder {

    protected static final String DEFAULT_LOGGER = "org.apache.tools.ant.XmlLogger";

    private static final Logger LOG = Logger.getLogger(AntBuilder.class);

    private String antWorkingDir = null;
    private String buildFile = "build.xml";
    private String target = "";
    private String tempFileName = "log.xml";
    private String antScript;
    private String antHome;
    private boolean useLogger;
    private final List<JVMArg> args = new ArrayList<JVMArg>();
    private final List<Lib> libs = new ArrayList<Lib>();
    private final List<Listener> listeners = new ArrayList<Listener>();
    private final List<Property> properties = new ArrayList<Property>();
    private boolean useDebug = false;
    private boolean useQuiet = false;
    private boolean keepGoing = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private boolean isLoggerClassNameSet;
    // @todo Verify default showAntOutput=true doesn't break AntBootstrapper and AntPublisher (see constructors)
    private boolean showAntOutput = true;
    private File saveLogDir = null;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private boolean wasValidated = false;
    private String propertyfile;
    private String progressLoggerLib;

    public void validate() throws CruiseControlException {
        super.validate();

        ValidationHelper.assertIsSet(buildFile, "buildfile", this.getClass());
        ValidationHelper.assertIsSet(target, "target", this.getClass());
        ValidationHelper.assertFalse(useDebug && useQuiet,
            "'useDebug' and 'useQuiet' can't be used together");

        if (!useLogger && (useDebug || useQuiet)) {
            LOG.warn("usedebug and usequiet are ignored if uselogger is not set to 'true'!");
        }

        if (saveLogDir != null) {
            ValidationHelper.assertTrue(saveLogDir.isDirectory(), "'saveLogDir' must exist and be a directory");
        }

        ValidationHelper.assertFalse(antScript != null && antHome != null,
            "'antHome' and 'antscript' cannot both be set");

        // NOTE: We can't validate showProgress here because we don't know if we will really show progress until
        // the AntBuilder.build() method is called (as parent Builders/Schedule may override the showProgress value).

        // Validate showAntOutput
        if (shouldAddDashboardLoggerJarToCommandLine(showAntOutput, useLogger)) {
            if (progressLoggerLib == null) {
                // since progressLoggerLib is not specified in the config.xml,
                // we must be able to find the path to {@link AntScript#LIBNAME_PROGRESS_LOGGER}
                // to ensure the separate ant VM will have access to the required listeners
                AntScript.findDefaultProgressLoggerLib();
            } else {
                // config.xml specified progressLoggerLib, so just make sure it exists
                ValidationHelper.assertExists(new File(progressLoggerLib), "progressLoggerLib", this.getClass());
            }
        }

        if (antHome != null) {
            final File antHomeFile = new File(antHome);
            ValidationHelper.assertTrue(antHomeFile.exists() && antHomeFile.isDirectory(),
                "'antHome' must exist and be a directory. Expected to find "
                + antHomeFile.getAbsolutePath());

            final File antScriptInAntHome = new File(findAntScript(Util.isWindows()));
            ValidationHelper.assertTrue(antScriptInAntHome.exists() && antScriptInAntHome.isFile(),
                "'antHome' must contain an ant execution script. Expected to find "
                + antScriptInAntHome.getAbsolutePath());

            antScript = antScriptInAntHome.getAbsolutePath();
        }

        if (antScript != null && !args.isEmpty()) {
            LOG.warn("jvmargs will be ignored if you specify anthome or your own antscript!");
        }

        wasValidated = true;
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(final Map<String, String> buildProperties, final Progress progressIn)
            throws CruiseControlException {
        
        if (!wasValidated) {
            throw new IllegalStateException("This builder was never validated."
                 + " The build method should not be getting called.");
        }

        validateBuildFileExists();

        final Progress progress = getShowProgress() ? progressIn : null;

        final AntScript script = new AntScript();
        script.setBuildProperties(buildProperties);
        script.setProperties(properties);
        script.setLibs(libs);
        script.setListeners(listeners);
        script.setUseLogger(useLogger);
        script.setUseScript(antScript != null);
        script.setWindows(Util.isWindows());
        script.setAntScript(antScript);
        script.setArgs(args);
        script.setBuildFile(buildFile);
        script.setTarget(target);
        script.setLoggerClassName(loggerClassName);
        script.setIsLoggerClassNameSet(isLoggerClassNameSet);
        script.setShowAntOutput(showAntOutput);
        script.setTempFileName(tempFileName);
        script.setUseDebug(useDebug);
        script.setUseQuiet(useQuiet);
        script.setKeepGoing(keepGoing);
        script.setSystemClassPath(getSystemClassPath());
        script.setPropertyFile(propertyfile);
        script.setProgressLoggerLib(progressLoggerLib);
        script.setProgress(progress);

        final File workingDir = antWorkingDir != null ? new File(antWorkingDir) : null;

        final BuildOutputLogger buildOutputConsumer;
        if (showAntOutput) {
            // TODO: I think there's a bug here when workingDir == null
            final File antBuilderOutput = new File(workingDir, AntOutputLogger.DEFAULT_OUTFILE_NAME);
            buildOutputConsumer = BuildOutputLoggerManager.INSTANCE.lookupOrCreate(antBuilderOutput);
            buildOutputConsumer.clear();
        } else {
            buildOutputConsumer = null;
        }

        final boolean scriptCompleted = runScript(script, workingDir, buildOutputConsumer);

        final File logFile = new File(antWorkingDir, tempFileName);
        final Element buildLogElement;
        if (!scriptCompleted) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
            // although log file is most certainly empty, let's try to preserve it
            // somebody should really fix ant's XmlLogger
            if (logFile.exists()) {
                try {
                    buildLogElement.setText(Util.readFileToString(logFile));
                } catch (IOException likely) {
                    // ignored
                }
            }
        } else {
            //read in log file as element, return it
            buildLogElement = getAntLogAsElement(logFile);
            saveAntLog(logFile);
            logFile.delete();
        }
        return buildLogElement;
    }

    boolean runScript(final AntScript script, final File workingDir, final BuildOutputLogger outputLogger)
            throws CruiseControlException {
        return new ScriptRunner().runScript(workingDir, script, timeout, outputLogger);
    }

    public Element buildWithTarget(final Map<String, String> properties, final String buildTarget,
                                   final Progress progress)
            throws CruiseControlException {
        
        final String origTarget = target;
        try {
            target = buildTarget;
            return build(properties, progress);
        } finally {
            target = origTarget;
        }
    }

    void validateBuildFileExists() throws CruiseControlException {
        File build = new File(buildFile);
        if (!build.exists() && !build.isAbsolute() && antWorkingDir != null) {
            build = new File(antWorkingDir, buildFile);
        }
        ValidationHelper.assertExists(build, "buildfile", this.getClass());
    }


    /**
     * Set the location to which the ant log will be saved before Cruise
     * Control merges the file into its log.
     *
     * @param dir
     *          the absolute path to the directory where the ant log will be
     *          saved or relative path to where you started CruiseControl
     */
    public void setSaveLogDir(String dir) {
        saveLogDir = null;

        if (dir != null && !dir.trim().equals("")) {
            saveLogDir = new File(dir.trim());
        }
    }

    void saveAntLog(File logFile) {
        if (saveLogDir == null) {
            return;
        }

        try {
            final File newAntLogFile = new File(saveLogDir, tempFileName);
            newAntLogFile.createNewFile();

            final FileInputStream in = new FileInputStream(logFile);
            try {
                final FileOutputStream out = new FileOutputStream(newAntLogFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            LOG.error(ioe);
            LOG.error("Unable to create file: " + new File(saveLogDir, tempFileName));
        }
  }

    /**
     * Set the working directory where Ant will be invoked. This parameter gets
     * set in the XML file via the antWorkingDir attribute. The directory can
     * be relative (to the cruisecontrol current working directory) or absolute.
     *
     * @param dir
     *          the directory to make the current working directory.
     */
    public void setAntWorkingDir(String dir) {
        antWorkingDir = dir;
    }

    /**
     * Sets the Script file to be invoked (in place of calling the Ant class
     * directly).  This is a platform dependent script file.
     *
     * @param antScript the name of the script file
     */
    public void setAntScript(String antScript) {
        this.antScript = antScript;
    }

    /**
     * If set CC will use the platform specific script provided by Ant
     *
     * @param antHome the path to ANT_HOME
     */
    public void setAntHome(String antHome) {
        this.antHome = antHome;
    }

    /**
     * @param isWindows if true, running under windows
     * @return If the anthome attribute is set, then this method returns the correct shell script
     * to use for a specific environment.
     * @throws CruiseControlException if <code>antHome</code> is not set
     */
    protected String findAntScript(boolean isWindows) throws CruiseControlException {
        if (antHome == null) {
            throw new CruiseControlException("anthome attribute not set.");
        }

        if (isWindows) {
            return antHome + "\\bin\\ant.bat";
        } else {
            return antHome + "/bin/ant";
        }
    }

    /**
     * Set the name of the temporary file used to capture output.
     *
     * @param tempFileName temp file name
     */
    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    /**
     * Set the Ant target(s) to invoke.
     *
     * @param target the target(s) name.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Sets the name of the build file that Ant will use.  The Ant default is
     * build.xml, use this to override it.
     *
     * @param buildFile the name of the build file.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * Sets whether Ant will use the custom loggers.
     *
     * @param useLogger if true, use custom logger
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    /**
     * Sets whether Ant will use the custom AntOutputLogger as a listener.
     * @param showAntOutput if true, add AntOutputLogger as a listener.
     */
    public void setShowAntOutput(final boolean showAntOutput) {
        this.showAntOutput = showAntOutput;
    }
    boolean getShowAntOutput() {
        return showAntOutput;
    }

    /**
     * @param showAntOutput if false, disables Dashboard AntOutputLogger
     * @param useLogger if false, disables Dashboard AntOutputLogger
     * @return true if the jar containing the custom Dashboard logger class must be added to the command line used
     * to execute Ant.
     */
    static boolean shouldAddDashboardLoggerJarToCommandLine(final boolean showAntOutput, final boolean useLogger) {
        return showAntOutput && useLogger;
    }

    public Object createJVMArg() {
        final JVMArg arg = new JVMArg();
        args.add(arg);
        return arg;
    }

    public Object createLib() {
        final Lib lib = new Lib();
        libs.add(lib);
        return lib;
    }

    public Object createListener() {
        final Listener listener = new Listener();
        listeners.add(listener);
        return listener;
    }

    public Property createProperty() {
        final Property property = new Property();
        properties.add(property);
        return property;
    }

    protected String getSystemClassPath() {
      return System.getProperty("java.class.path");
    }

    protected Element getAntLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath() + " does not exist.");
        } else if (file.length() == 0) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath()
                    + " is empty. Your build probably failed. Check your CruiseControl logs.");
        }

        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

            // old Ant-versions contain a bug in the XmlLogger that outputs
            // an invalid PI containing the target "xml:stylesheet"
            // instead of "xml-stylesheet": fix this
            XMLFilter piFilter = new XMLFilterImpl() {
                public void processingInstruction(String target, String data) throws SAXException {
                    if (target.equals("xml:stylesheet")) { target = "xml-stylesheet"; }
                    super.processingInstruction(target, data);
                }
            };

            // get rid of empty <task>- and <message>-elements created by Ant's XmlLogger
            XMLFilter emptyTaskFilter = new EmptyElementFilter("task");
            emptyTaskFilter.setParent(piFilter);
            XMLFilter emptyMessageFilter = new EmptyElementFilter("message");
            emptyMessageFilter.setParent(emptyTaskFilter);
            builder.setXMLFilter(emptyMessageFilter);
            return builder.build(file).getRootElement();
        } catch (Exception ee) {
            if (ee instanceof CruiseControlException) {
                throw (CruiseControlException) ee;
            }
            File saveFile = new File(file.getParentFile(), System.currentTimeMillis() + file.getName());
            file.renameTo(saveFile);
            throw new CruiseControlException("Error reading : " + file.getAbsolutePath()
                    + ".  Saved as : " + saveFile.getAbsolutePath(), ee);
        }
    }

    public void setUseDebug(boolean debug) {
        useDebug = debug;
    }

    public void setUseQuiet(boolean quiet) {
        useQuiet = quiet;
    }

    public void setKeepGoing(boolean keepGoing) {
        this.keepGoing = keepGoing;
    }

    public String getLoggerClassName() {
        return loggerClassName;
    }

    public void setLoggerClassName(String string) {
        loggerClassName = string;
        isLoggerClassNameSet = true;
    }

    public class JVMArg implements Serializable {
        private static final long serialVersionUID = 402625457108399047L;

        private String arg;

        public void setArg(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
    }
    
    public class Lib implements Serializable {
        private static final long serialVersionUID = 1804469347425625224L;

        private String searchPath;

        public void setSearchPath(String searchPath) {
            this.searchPath = searchPath;
        }

        public String getSearchPath() {
            return searchPath;
        }
    }
    
    public class Listener implements Serializable {
        private static final long serialVersionUID = 4813682685614734386L;

        private String className;

        public void setClassName(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    /**
     * @param propertyfile The propertyfile to set.
     */
    public void setPropertyfile(String propertyfile) {
        this.propertyfile = propertyfile;
    }

    /**
     * @param progressLoggerLib The path (including filename) to the jar file
     * ({@link AntScript#LIBNAME_PROGRESS_LOGGER cruisecontrol-antprogresslogger.jar})
     * containing the AntProgressLogger/Listener classes.
     */
    public void setProgressLoggerLib(String progressLoggerLib) {
        this.progressLoggerLib = progressLoggerLib;
    }
    /**
     * @return The path (including filename) to the jar file
     * ({@link AntScript#LIBNAME_PROGRESS_LOGGER cruisecontrol-antprogresslogger.jar})
     * containing the AntProgressLogger/Listener classes.
     */
    public String getProgressLoggerLib() {
        return progressLoggerLib;
    }
}
