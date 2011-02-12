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
import net.sourceforge.cruisecontrol.gendoc.annotations.Cardinality;
import net.sourceforge.cruisecontrol.gendoc.annotations.Default;
import net.sourceforge.cruisecontrol.gendoc.annotations.Description;
import net.sourceforge.cruisecontrol.gendoc.annotations.DescriptionFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.ExamplesFile;
import net.sourceforge.cruisecontrol.gendoc.annotations.Optional;
import net.sourceforge.cruisecontrol.gendoc.annotations.Required;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
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
@DescriptionFile
@ExamplesFile
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
        if (shouldAddDashboardLoggerJarToCommandLine(isLiveOutput(), useLogger)) {
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
        script.setShowAntOutput(isLiveOutput());
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
        if (isLiveOutput()) {
            // TODO: I think there's a bug here when workingDir == null
            buildOutputConsumer = getBuildOutputConsumer(buildProperties.get(Builder.BUILD_PROP_PROJECTNAME), 
                    workingDir, AntOutputLogger.DEFAULT_OUTFILE_NAME);

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

    @Description(
            "If supplied, a copy of the ant log will be saved in the specified "
            + "local directory. Example: saveLogDir=\"/usr/local/dev/projects/cc/logs\".")
    @Optional
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

    @Description(
            "Will invoke ANT in the specified directory. This directory can be "
            + "absolute or relative to the cruisecontrol working directory.")
    @Optional
    public void setAntWorkingDir(String dir) {
        antWorkingDir = dir;
    }

    @Description(
            "Absolute filename of script (shell script or bat file) used to start Ant. "
            + "You can use this to make CruiseControl use your own Ant installation. "
            + "If this is not specified, the AntBuilder uses the Ant distribution that "
            + "ships with CruiseControl. See below for <a href=\"#ant-examples\">examples"
            + "</a>.")
    @Optional(
            "Recommended, however. Cannot be specified if anthome attribute "
            + "is also specified")
    public void setAntScript(String antScript) {
        this.antScript = antScript;
    }

    @Description(
            "Directory in which Ant is installed. CruiseControl will attempt to use the "
            + "standard Ant execution scripts (i.e. ant.bat or ant). See below for "
            + "<a href=\"#ant-examples\">examples</a>.")
    @Optional("Cannot be specified if antscript attribute is also specified.")
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

    @Description("Name of temp file used to capture output.")
    @Optional
    @Default("log.xml")
    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    @Description(
            "Ant target(s) to run. Default is \"\", or the default target for "
            + "the build file.")
    @Optional
    public void setTarget(String target) {
        this.target = target;
    }

    @Description("Path to Ant build file.")
    @Optional
    @Default("build.xml")
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    @Description(
            "'true' if CruiseControl should call Ant using -logger; 'false' to call Ant "
            + "using '-listener', thus using the loggerclass as a Listener. uselogger="
            + "\"true\" will make Ant log its messages using the class specified by "
            + "loggerclassname as an Ant Logger, which can make for smaller log files since "
            + "it doesn't log DEBUG messages (see useDebug and useQuiet attributes below, "
            + "and the <a href=\"http://ant.apache.org/manual/listeners.html\">Ant manual</a>). "
            + "Set to false to have Ant echo ant messages to console "
            + "using its DefaultLogger, which is useful when debugging your ant build. "
            + "Defaults to 'false' to make initial setup easier but setting it to 'true' is "
            + "recommended for production situations."
            + "<br/><br/>"
            + "RE: liveOutput: If liveOutput=true AND uselogger=true, this builder will write "
            + "the ant output to a file (antBuilderOutput.log) that can be read by the "
            + "Dashboard reporting application. The liveOutput setting has no effect if "
            + "uselogger=false. <a href=\"#antbootstrapper\">AntBootstrapper</a> and "
            + "<a href=\"#antpublisher\">AntPublisher</a> do not provide access to "
            + "liveOutput, and operate as if liveOutput=false. NOTE: In order to show ant "
            + "output while uselogger=true, the AntBuilder uses a custom Build Listener. If "
            + "this interferes with your Ant build, set liveOutput=false (and please report "
            + "the problem)")
    @Optional
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    /**
     * Sets whether Ant will use the custom AntOutputLogger as a listener in order to show live output.
     * @param showAntOutput if true, add AntOutputLogger as a listener.
     * @deprecated Use {@link #setLiveOutput(boolean)} instead.
     */
    @SkipDoc
    public void setShowAntOutput(final boolean showAntOutput) {
        setLiveOutput(showAntOutput);
    }

    /**
     * @return true if Ant will use the custom AntOutputLogger as a listener in order to show live output.
     * @deprecated Use {@link #isLiveOutput()} instead.
     */
    boolean getShowAntOutput() {
        return isLiveOutput();
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

    @Description("Pass specified argument to the jvm used to invoke ant."
            + "Ignored if using anthome or antscript. The element has a single required"
            + "attribute: \"arg\".<br />"
            + "<strong>Example:</strong> <code>&lt;jvmarg arg=\"-Xmx120m\"/&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public JVMArg createJVMArg() {
        final JVMArg arg = new JVMArg();
        args.add(arg);
        return arg;
    }

    @Description("Used to define additional <a "
            + "href=\"http://ant.apache.org/manual/running.html#libs\">library directories</a> "
            + "for the ant build. The element has one required attribute: \"searchPath\".<br /> "
            + "<strong>Example:</strong> <code>&lt;lib searchPath=\"/home/me/myantextensions\"/"
            + "&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public Lib createLib() {
        final Lib lib = new Lib();
        libs.add(lib);
        return lib;
    }

    @Description("Used to define additional <a "
            + "href=\"http://ant.apache.org/manual/listeners.html\">listeners</a> for the "
            + "ant build. The element has one required attribute: \"classname\".<br />"
            + "<strong>Example:</strong> <code>&lt;listener classname=\"org.apache.tools."
            + "ant.listener.Log4jListener\"/&gt;</code>")
    @Cardinality(min = 0, max = -1)
    public Listener createListener() {
        final Listener listener = new Listener();
        listeners.add(listener);
        return listener;
    }

    @Description("Used to define properties for the ant build. The element has two "
            + "required attributes: \"name\" and \"value\". These will be passed on the "
            + "ant command-line as \"-Dname=value\"<br />"
            + "<strong>Example:</strong> <code>&lt;property name=\"foo\" value=\"bar\"/"
            + "&gt;</code>")
    @Cardinality(min = 0, max = -1)
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

    @Description(
            "If true will invoke ant with -debug, which can be useful for debugging your "
            + "ant build. Defaults to 'false', cannot be set to 'true' if usequiet is "
            + "also set to 'true'. When used in combination with uselogger=\"true\", "
            + "this will result in bigger XML log files; otherwise, it will cause more "
            + "output to be written to the console by Ant's DefaultLogger.")
    @Optional
    public void setUseDebug(boolean debug) {
        useDebug = debug;
    }

    @Description(
            "If true will invoke ant with -quiet, which can be useful for creating smaller "
            + "log files since messages with a priority of INFO will not be logged. Defaults "
            + "to 'false', cannot be set to 'true' if usedebug is also set to 'true'. "
            + "Smaller logfiles are only achieved when used in combination with uselogger="
            + "\"true\", otherwise there will just be less output echoed to the console by "
            + "Ant's DefaultLogger."
            + "<br/><br/>"
            + "RE: showProgress: useQuiet=\"true\" will prevent any progress messages from "
            + "being displayed. NOTE: In order to show progress, the AntBuilder uses custom "
            + "Build Loggers and Listeners. If these interfere with your Ant build, set "
            + "showProgress=false (and please report the problem).")
    @Optional
    public void setUseQuiet(boolean quiet) {
        useQuiet = quiet;
    }

    @Description(
            "If true will invoke ant with -keep-going, which can be useful for performing "
            + "build steps after an optional step fails. Defaults to 'false'.")
    @Optional
    public void setKeepGoing(boolean keepGoing) {
        this.keepGoing = keepGoing;
    }

    public String getLoggerClassName() {
        return loggerClassName;
    }

    @Description(
            "If you want to use another logger (or listener, when uselogger=\"false\") than "
            + "Ant's XmlLogger, you can specify the classname of the logger here. The logger "
            + "needs to output compatible XML, and the class needs to be available on the "
            + "classpath at buildtime.")
    @Optional
    @Default("org.apache.tools.ant.XmlLogger")
    public void setLoggerClassName(String string) {
        loggerClassName = string;
        isLoggerClassNameSet = true;
    }

    @Description("Passes an argument to the JVM used to invoke ANT.")
    public class JVMArg implements Serializable {
        private static final long serialVersionUID = 402625457108399047L;

        private String arg;

        @Description("Command-line argument to pass to the ANT JVM.")
        @Required
        public void setArg(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
    }
    
    @Description("Provides additional library directories for an ANT build.")
    public class Lib implements Serializable {
        private static final long serialVersionUID = 1804469347425625224L;

        private String searchPath;

        @Description("Path to use for loading libraries into the ANT JVM.")
        @Required
        public void setSearchPath(String searchPath) {
            this.searchPath = searchPath;
        }

        public String getSearchPath() {
            return searchPath;
        }
    }
    
    @Description("Provides additional listeners for an ANT build.")
    public class Listener implements Serializable {
        private static final long serialVersionUID = 4813682685614734386L;

        private String className;

        @Description("Name of the Java listener class to register with this ANT build.")
        @Required
        public void setClassName(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    @Description(
            "Ant build will be halted if it continues longer than the specified timeout. "
            + "Value in seconds.")
    @Optional
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    @Description(
            "Load all properties from file with -D properties (like child <code><a href=\""
            + "#antbuilderchildprop\">&lt;property&gt;</a></code> elements) taking "
            + "precedence. Useful when the propertyfile content can change for every build.")
    @Optional
    public void setPropertyfile(String propertyfile) {
        this.propertyfile = propertyfile;
    }

    @Description(
            "Overrides the default -lib search path used to add support for showProgress "
            + "features in the ant builder. This search path ensures customized ant "
            + "Loggers/Listeners are available on the classpath of the ant builder VM. You "
            + "should not normally set this value. If you do set this value, you should "
            + "use the full path (including filename) to cruisecontrol-antprogresslogger.jar. "
            + "This setting has no effect if showProgress=false.")
    @Optional
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
