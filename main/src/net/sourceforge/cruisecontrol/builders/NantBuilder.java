/*******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit Copyright (c) 2001,
 * ThoughtWorks, Inc. 651 W Washington Ave. Suite 600 Chicago, IL 60661 USA All
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.EmptyElementFilter;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

public class NantBuilder extends Builder {

    private static final String DEFAULT_LOGGER = "NAnt.Core.XmlLogger";
    private static final Logger LOG = Logger.getLogger(NantBuilder.class);
    private String nantWorkingDir = null;
    private String buildFile = "default.build";
    private String target = "";
    private String tempFileName = "log.xml";
    private boolean useLogger;
    private List properties = new ArrayList();
    private boolean useDebug = false;
    private boolean useQuiet = false;
    private boolean useVerbose = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private long timeout = -1;
    private File saveLogDir = null;
    private String targetFramework = null;

    public void validate() throws CruiseControlException {
        super.validate();

        if (buildFile == null) {
            throw new CruiseControlException("'buildfile' is a required attribute on NantBuilder");
        }

        if (target == null) {
            throw new CruiseControlException("'target' is a required attribute on NantBuilder");
        }

        if (useDebug && useQuiet) {
            throw new CruiseControlException("'useDebug' and 'useQuiet' can't be used together");
        }

        if (!useLogger && (useDebug || useQuiet)) {
            LOG.warn("usedebug and usequiet are ignored if uselogger is not set to 'true'!");
        }

        if (saveLogDir != null) {
            if (!saveLogDir.isDirectory()) {
                throw new CruiseControlException("'saveLogDir' must exist and be a directory");
            }
        }
    }

    /**
     * build and return the results via xml. debug status can be determined from
     * log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        Process p;
        String workingDirPath = null;
        try {
            File workingDir = nantWorkingDir != null ? new File(nantWorkingDir) : null;

            String[] commandLine = getCommandLineArgs(buildProperties);

            if (LOG.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Executing Command: '");
                for (int i = 0; i < commandLine.length; i++) {
                    sb.append(commandLine[i]).append(" ");
                }
                if (workingDir != null) {
                    sb.append("' in directory " + workingDir);
                }
                LOG.debug(sb);
            }

            p = Runtime.getRuntime().exec(commandLine, null, workingDir);
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute NAnt."
                    + " CruiseControl cannot continue.", e);
        }

        StreamPumper errorPumper = new StreamPumper(p.getErrorStream());
        StreamPumper outPumper = new StreamPumper(p.getInputStream());
        new Thread(errorPumper).start();
        new Thread(outPumper).start();
        AsyncKiller killer = new AsyncKiller(p, timeout);
        if (timeout > 0) {
            killer.start();
        }

        try {
            p.waitFor();
            killer.interrupt();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (InterruptedException e) {
            LOG.info("Was interrupted while waiting for NAnt to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } catch (IOException ie) {
            LOG.info("Exception trying to close Process streams.", ie);
        }

        outPumper.flush();
        errorPumper.flush();

        File logFile = new File(nantWorkingDir, tempFileName);
        Element buildLogElement;
        if (killer.processKilled()) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
        } else {
            //read in log file as element, return it
            buildLogElement = getNantLogAsElement(logFile);
            saveNantLog(logFile);
            logFile.delete();
        }
        return translateNantErrorElements(buildLogElement);
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
     * @param tempFileName
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
     * Used to invoke the builder via JMX with a different target.
     */
    protected void overrideTarget(String target) {
        setTarget(target);
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
     * @param useLogger
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    void saveNantLog(File logFile) {
        if (saveLogDir == null) {
            return;
        }

        try {
            File newNantLogFile = new File(saveLogDir, tempFileName);
            newNantLogFile.createNewFile();

            FileInputStream in = new FileInputStream(logFile);
            FileOutputStream out = new FileOutputStream(newNantLogFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
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

    protected boolean isWindows() {
        String osName = getOsName();
        boolean isWindows = osName.indexOf("Windows") > -1;
        LOG.debug("os.name = " + osName);
        LOG.debug("isWindows = " + isWindows);
        return isWindows;
    }

    protected String getOsName() {
        String osName = System.getProperty("os.name");
        return osName;
    }

    /**
     * construct the command that we're going to execute.
     * 
     * @param buildProperties
     *            Map holding key/value pairs of arguments to the build process
     * @return String[] holding command to be executed
     * @throws CruiseControlException
     *             on unquotable attributes
     */
    protected String[] getCommandLineArgs(Map buildProperties) throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        cmdLine.setExecutable("NAnt.exe");
        if (useLogger) {
            cmdLine.createArgument().setValue("-logger:" + getLoggerClassName());
            cmdLine.createArgument().setValue("-logfile:" + tempFileName);
            if (useDebug) {
                cmdLine.createArgument().setValue("-debug" + (useDebug ? '+' : '-'));
            } else if (useQuiet) {
                cmdLine.createArgument().setValue("-quiet" + (useQuiet ? '+' : '-'));
            }
        } else {
            cmdLine.createArgument().setValue("-listener:" + getLoggerClassName());
            cmdLine.createArgument().setValue("-D:XmlLogger.file=" + tempFileName);
        }
        if (targetFramework != null) {
            cmdLine.createArgument().setValue("-t:" + targetFramework);
        }

        for (Iterator propertiesIter = buildProperties.entrySet().iterator(); propertiesIter.hasNext();) {
            Map.Entry property = (Map.Entry) propertiesIter.next();
            String value = (String) property.getValue();
            if (!"".equals(value)) {
                cmdLine.createArgument().setValue("-D:" + property.getKey() + "=" + value);
            }
        }

        cmdLine.createArgument().setValue("-buildfile:" + buildFile);

        StringTokenizer targets = new StringTokenizer(target);
        while (targets.hasMoreTokens()) {
            cmdLine.createArgument().setValue(targets.nextToken());
        }

        return cmdLine.getCommandline();
    }

    protected static Element getNantLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("NAnt logfile " + file.getAbsolutePath() + " does not exist.");
        }
        try {
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

            // TODO: What to do here? NAnt might have different issues...

            // old Ant-versions contain a bug in the XmlLogger that outputs
            // an invalid PI containing the target "xml:stylesheet"
            // instead of "xml-stylesheet": fix this
            XMLFilter piFilter = new XMLFilterImpl() {
                public void processingInstruction(String target, String data) throws SAXException {
                    if (target.equals("xml:stylesheet")) {
                        target = "xml-stylesheet";
                    }
                    super.processingInstruction(target, data);
                }
            };

            // get rid of empty <task>- and <message>-elements created by Ant's
            // XmlLogger
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
            throw new CruiseControlException("Error reading : " + file.getAbsolutePath() + ".  Saved as : "
                    + saveFile.getAbsolutePath(), ee);
        }
    }

    /**
     * Sets build timeout in seconds.
     * 
     * @param timeout
     *            long build timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setUseDebug(boolean debug) {
        useDebug = debug;
    }

    public void setUseVerbose(boolean verbose) {
        useVerbose = verbose;
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
            Element message = buildError.getChild("message");
            List matches = message.getContent(new ContentFilter(ContentFilter.CDATA));
            String errorMessage = ((CDATA) matches.get(0)).getText();
            buildLogElement.setAttribute(new Attribute("error", errorMessage));
        }
        return buildLogElement;
    }
    
    public class Property {
        private String name;

        private String value;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class AsyncKiller extends Thread {
        private final Process p;

        private final long timeout;

        private boolean killed;

        AsyncKiller(final Process p, final long timeout) {
            this.p = p;
            this.timeout = timeout;
        }

        public void run() {
            try {
                sleep(timeout * 1000L);
                synchronized (this) {
                    p.destroy();
                    killed = true;
                }
            } catch (InterruptedException expected) {
            }
        }

        public synchronized boolean processKilled() {
            return killed;
        }
    }
}