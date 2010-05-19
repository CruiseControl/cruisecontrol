package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.EmptyElementFilter;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

public class PhingBuilder extends Builder {

    protected static final String DEFAULT_LOGGER = "phing.listener.XmlLogger";
    private static final Logger LOG = Logger.getLogger(PhingBuilder.class);

    private String phingWorkingDir = null;
    private String buildFile = "build.xml";
    private String target = "";
    private String tempFileName = "log.xml";
    private String phingScript = "phing";
    private String phingHome;
    private boolean useLogger;
    private final List<Property> properties = new ArrayList<Property>();
    private boolean useDebug = false;
    private boolean useQuiet = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private File saveLogDir = null;
    private long timeout = ScriptRunner.NO_TIMEOUT;
    private boolean wasValidated = false;

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

        if (phingHome != null) {
            final File phingHomeFile = new File(phingHome);
            ValidationHelper.assertTrue(phingHomeFile.exists() && phingHomeFile.isDirectory(),
                "'phingHome' must exist and be a directory. Expected to find "
                + phingHomeFile.getAbsolutePath());

            final File phingScriptInPhingHome = new File(findPhingScript(Util.isWindows()));
            ValidationHelper.assertTrue(phingScriptInPhingHome.exists() && phingScriptInPhingHome.isFile(),
                "'phingHome' must contain an ant execution script. Expected to find "
                + phingScriptInPhingHome.getAbsolutePath());

            phingScript = phingScriptInPhingHome.getAbsolutePath();
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
        // @todo To support progress, determine text pattern indicating progress messages in output,
        // see AntScript.consumeLine() as an example

        final PhingScript script = new PhingScript();
        script.setBuildProperties(buildProperties);
        script.setProperties(properties);
        script.setUseLogger(useLogger);
        script.setPhingScript(phingScript);
        script.setBuildFile(buildFile);
        script.setTarget(target);
        script.setLoggerClassName(loggerClassName);
        script.setTempFileName(tempFileName);
        script.setUseDebug(useDebug);
        script.setUseQuiet(useQuiet);

        final File workingDir = phingWorkingDir != null ? new File(phingWorkingDir) : null;

        final boolean scriptCompleted = new ScriptRunner().runScript(workingDir, script, timeout,
                getBuildOutputConsumer(buildProperties.get(Builder.BUILD_PROP_PROJECTNAME), workingDir, null));

        final File logFile = new File(phingWorkingDir, tempFileName);
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
                }
            }
        } else {
            //read in log file as element, return it
            buildLogElement = getPhingLogAsElement(logFile);
            savePhingLog(logFile);
            logFile.delete();
        }
        return buildLogElement;
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
        if (!build.isAbsolute() && phingWorkingDir != null) {
            build = new File(phingWorkingDir, buildFile);
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

    void savePhingLog(File logFile) {
        if (saveLogDir == null) {
            return;
        }

        try {
            final File newPhingLogFile = new File(saveLogDir, tempFileName);
            newPhingLogFile.createNewFile();

            final FileInputStream in = new FileInputStream(logFile);
            try {
                final FileOutputStream out = new FileOutputStream(newPhingLogFile);
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
     * Set the working directory where Phing will be invoked. This parameter gets
     * set in the XML file via the phingWorkingDir attribute. The directory can
     * be relative (to the cruisecontrol current working directory) or absolute.
     *
     * @param dir
     *          the directory to make the current working directory.
     */
    public void setPhingWorkingDir(String dir) {
        phingWorkingDir = dir;
    }

    /**
     * Sets the Phing script file to be invoked.
     * 
     * This is a platform dependent script file.
     *
     * @param phingScript the name of the script file
     */
    public void setPhingScript(String phingScript) {
        this.phingScript = phingScript;
    }

    /**
     * If set CC will use the platform specific script provided by Phing
     *
     * @param antHome the path to ANT_HOME
     */
    public void setPhingHome(String antHome) {
        this.phingHome = antHome;
    }

    /**
     * If the phinghome attribute is set, then this method returns the correct shell script
     * to use for a specific environment.
     * @param isWindows true if running under windows
     * @return shell script to use for a specific environment.
     * @throws CruiseControlException if the phinghome attribute is not set
     */
    protected String findPhingScript(boolean isWindows) throws CruiseControlException {
        if (phingHome == null) {
            throw new CruiseControlException("phinghome attribute not set.");
        }

        if (isWindows) {
            return phingHome + "\\bin\\phing.bat";
        } else {
            return phingHome + "/bin/phing";
        }
    }

    /**
     * @param tempFileName the name of the temporary file used to capture output.
     */
    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    /**
     * Set the Phing target(s) to invoke.
     *
     * @param target the target(s) name.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Sets the name of the build file that Phing will use.  The Phing default is
     * build.xml, use this to override it.
     *
     * @param buildFile the name of the build file.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * Sets whether Phing will use the custom loggers.
     * @param useLogger if true, use custom loggers
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    public Property createProperty() {
        Property property = new Property();
        properties.add(property);
        return property;
    }

    protected static Element getPhingLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("phing logfile " + file.getAbsolutePath() + " does not exist.");
        } else if (file.length() == 0) {
            throw new CruiseControlException("phing logfile " + file.getAbsolutePath()
                    + " is empty. Your build probably failed. Check your CruiseControl logs.");
        }

        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

            // old Ant-versions contain a bug in the XmlLogger that outputs
            // an invalid PI containing the target "xml:stylesheet"
            // instead of "xml-stylesheet": fix this
            // FIXME - remove this, as it shouldn't affect Phing (though I don't know what this is
            // for yet.)
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

    public String getLoggerClassName() {
        return loggerClassName;
    }

    public void setLoggerClassName(String string) {
        loggerClassName = string;
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
