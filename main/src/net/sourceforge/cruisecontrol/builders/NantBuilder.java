/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit Copyright (c) 2001,
 * ThoughtWorks, Inc. 200 E. Randolph, 25th Floor Chicago, IL 60601 USA All
 * rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: +
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. + Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. + Neither the name of ThoughtWorks, Inc.,
 * CruiseControl, nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

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
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.xml.sax.XMLFilter;

public class NantBuilder extends Builder {

    protected static final String DEFAULT_LOGGER = "NAnt.Core.XmlLogger";
    private static final Logger LOG = Logger.getLogger(NantBuilder.class);
    private String nantWorkingDir = null;
    private String buildFile = "default.build";
    private String target = "";
    private String tempFileName = "log.xml";
    private boolean useLogger;
    private final List properties = new ArrayList();
    private boolean useDebug = false;
    private boolean useQuiet = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private File saveLogDir = null;
    private String targetFramework = null;
    private long timeout = ScriptRunner.NO_TIMEOUT;

    public void validate() throws CruiseControlException {
        super.validate();

        ValidationHelper.assertIsSet(buildFile, "buildFile", this.getClass());
        ValidationHelper.assertIsSet(target, "target", this.getClass());

        ValidationHelper.assertFalse(useDebug && useQuiet, "'useDebug' and 'useQuiet' can't be used together");

        if (saveLogDir != null) {
            ValidationHelper.assertTrue(saveLogDir.isDirectory(), "'saveLogDir' must exist and be a directory");
        }
    }

    /**
     * Build and return the results via xml. Debug status can be determined from
     * log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties, Progress progress) throws CruiseControlException {

        File workingDir = nantWorkingDir != null ? new File(nantWorkingDir) : null;
        NantScript script = getNantScript();
        script.setBuildFile(buildFile);
        script.setBuildProperties(buildProperties);
        script.setNantProperties(properties);
        script.setLoggerClassName(loggerClassName);
        script.setTarget(target);
        script.setTargetFramework(targetFramework);
        script.setTempFileName(tempFileName);
        script.setUseDebug(useDebug);
        // script.setUseDebug(useVerbose);
        script.setUseLogger(useLogger);
        script.setUseQuiet(useQuiet);

        long startTime = System.currentTimeMillis();

        ScriptRunner scriptRunner = new ScriptRunner();
        boolean scriptCompleted = scriptRunner.runScript(workingDir, script, timeout);
        long endTime = System.currentTimeMillis();

        File logFile = new File(nantWorkingDir, tempFileName);
        Element buildLogElement;
        if (!scriptCompleted) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
        } else {
            //read in log file as element, return it
            buildLogElement = getNantLogAsElement(logFile);
            saveNantLog(logFile);
            logFile.delete();
        }
        final Element element = translateNantErrorElements(buildLogElement);
        element.setAttribute("time", DateUtil.getDurationAsString((endTime - startTime)));
        return element;
    }

    public Element buildWithTarget(Map properties, String buildTarget, Progress progress)
            throws CruiseControlException {
        
        String origTarget = target;
        try {
            target = buildTarget;
            return build(properties, progress);
        } finally {
            target = origTarget;
        }
    }

    // factory method for mock...
    protected NantScript getNantScript() {
        return new NantScript();
    }

    /**
     * Set the location to which the NAnt log will be saved before Cruise
     * Control merges the file into its log.
     *
     * @param dir
     *            the absolute path to the directory where the NAnt log will be
     *            saved or relative path to where you started CruiseControl
     */
    public void setSaveLogDir(String dir) {
        saveLogDir = null;

        if (dir != null && !dir.trim().equals("")) {
            saveLogDir = new File(dir.trim());
        }
    }

    /**
     * Set the working directory where NAnt will be invoked. This parameter gets
     * set in the XML file via the nantWorkingDir attribute. The directory can
     * be relative (to the cruisecontrol current working directory) or absolute.
     *
     * @param dir
     *            the directory to make the current working directory.
     */
    public void setNantWorkingDir(String dir) {
        nantWorkingDir = dir;
    }

    /**
     * Set the name of the temporary file used to capture output.
     *
     * @param tempFileName the name of the temporary file used to capture output.
     */
    public void setTempFile(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    /**
     * Set the Ant target(s) to invoke.
     *
     * @param target
     *            the target(s) name.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Sets the name of the build file that NAnt will use. The NAnt default is
     * default.build, use this to override it.
     *
     * @param buildFile
     *            the name of the build file.
     */
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    /**
     * Sets whether NAnt will use the custom loggers.
     *
     * @param useLogger whether NAnt will use the custom loggers.
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    void saveNantLog(File logFile) {
        if (saveLogDir == null) {
            return;
        }

        try {
            final File newNantLogFile = new File(saveLogDir, tempFileName);
            newNantLogFile.createNewFile();

            final FileInputStream in = new FileInputStream(logFile);
            try {
                final FileOutputStream out = new FileOutputStream(newNantLogFile);
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

    public Property createProperty() {
        Property property = new Property();
        properties.add(property);
        return property;
    }

    protected Element getNantLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("NAnt logfile " + file.getAbsolutePath() + " does not exist.");
        }
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

            // get rid of empty <task>- and <message>-elements created by Ant's
            // XmlLogger
            XMLFilter emptyTaskFilter = new EmptyElementFilter("task");
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
            throw new CruiseControlException("Error reading : " + file.getAbsolutePath() + ".  Saved as : "
                    + saveFile.getAbsolutePath(), ee);
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

    public void setLoggerClassName(String logger) {
        loggerClassName = logger;
    }

    public void setTargetFramework(String framework) {
        this.targetFramework = framework;
    }

    protected Element translateNantErrorElements(Element buildLogElement) throws CruiseControlException {
        buildLogElement.setName("build");
        Element failure = buildLogElement.getChild("failure");
        if (failure != null) {
            Element buildError = failure.getChild("builderror");
            if (buildError == null) {
                 throw new CruiseControlException("Expected a builderror element under build/failure");
             }
            Element message = buildError.getChild("message");
            if (message == null) {
                throw new CruiseControlException("Expected a message element under build/failure/builderror");
            }
            List matches = message.getContent(new ContentFilter(ContentFilter.CDATA));
            if (matches.size() == 0) {
                throw new CruiseControlException("Expected CDATA content in build/failure/builderror/message/element");
            }
            String errorMessage = ((CDATA) matches.get(0)).getText();
            buildLogElement.setAttribute(new Attribute("error", errorMessage));
        }
        return buildLogElement;
    }
    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
