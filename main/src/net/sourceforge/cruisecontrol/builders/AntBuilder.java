/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.XMLFilter;

/**
 * we often see builds that fail because the previous build is still holding on to some resource.
 * we can avoid this by just building in a different process which will completely die after every
 * build.
 */
public class AntBuilder extends Builder {

    private static final String DEFAULT_LOGGER = "org.apache.tools.ant.XmlLogger";
    private static final Logger LOG = Logger.getLogger(AntBuilder.class);

    private String antWorkingDir = null;
    private String buildFile = "build.xml";
    private String target = "";
    private String tempFileName = "log.xml";
    private String antScript;
    private boolean useLogger;
    private List args = new ArrayList();
    private List properties = new ArrayList();
    private boolean useDebug = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private long timeout = -1;

    public void validate() throws CruiseControlException {
        super.validate();

        if (buildFile == null) {
            throw new CruiseControlException("'buildfile' is a required attribute on AntBuilder");
        }

        if (target == null) {
            throw new CruiseControlException("'target' is a required attribute on AntBuilder");
        }
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        Process p = null;
        try {
            String[] commandLine =
                    getCommandLineArgs(buildProperties,
                            useLogger,
                            antScript != null,
                            isWindows());

            File workingDir =
                    (antWorkingDir != null) ? (new File(antWorkingDir)) : null;

            StringBuffer sb = new StringBuffer();
            sb.append("Executing Command: ");
            for (int i = 0; i < commandLine.length; i++) {
                String arg = commandLine[i];
                sb.append(arg);
                sb.append(" ");
            }
            if (antWorkingDir != null) {
                sb.append("in directory " + workingDir.getCanonicalPath());
            }
            LOG.debug(sb.toString());


            p = Runtime.getRuntime().exec(commandLine, null, workingDir);
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute Ant."
                    + " CruiseControl cannot continue.",
                    e);
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
            LOG.info("Was interrupted while waiting for Ant to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } catch (IOException ie) {
            LOG.info("Exception trying to close Process streams.", ie);
        }

        outPumper.flush();
        errorPumper.flush();

        if (killer.processKilled()) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            Element timeoutLogElement = new Element("build");
            timeoutLogElement.setAttribute("error", "build timeout");
            // although log file is most certainly empy, let's try to preserve it
            // somebody should really fix ant's XmlLogger
            File logFile = new File(antWorkingDir, tempFileName);
            if (logFile.exists()) {
                try {
                    timeoutLogElement.setText(getFileContent(logFile));
                } catch (IOException likely) {
                }
            }
            return timeoutLogElement;
        } else {
            //read in log file as element, return it
            File logFile = new File(antWorkingDir, tempFileName);
            if (!logFile.exists()) {
                LOG.error("Ant logfile ["
                        + logFile.getAbsolutePath()
                        + "] cannot be found");
            }

            Element buildLogElement = getAntLogAsElement(logFile);
            if (logFile.exists()) {
                logFile.delete();
            }

            return buildLogElement;
        }
    }

    /**
     * Set the working directory where Ant will be invoked.  This
     * parameter gets set in the XML file via the antWorkingDir attribute.
     * The directory can be relative (to the cruisecontrol current working
     * directory) or absolute.
     *
     * @param dir the directory to make the current working directory.
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
     * @param useLogger
     */
    public void setUseLogger(boolean useLogger) {
        this.useLogger = useLogger;
    }

    public Object createJVMArg() {
        JVMArg arg = new JVMArg();
        args.add(arg);
        return arg;
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
     * @param buildProperties Map holding key/value pairs of arguments to the build process
     * @return String[] holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    protected String[] getCommandLineArgs(Map buildProperties,
                                          boolean useLogger,
                                          boolean useScript,
                                          boolean isWindows) throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        if (useScript) {
            cmdLine.setExecutable(antScript);
        } else {
            if (isWindows()) {
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
            cmdLine.createArgument().setValue(System.getProperty("java.class.path"));
            cmdLine.createArgument().setValue("org.apache.tools.ant.Main");
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

        if (useDebug) {
            cmdLine.createArgument().setValue("-debug");
            cmdLine.createArgument().setValue("-verbose");
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
     * JDOM doesn't like the <?xml:stylesheet ?> tag.  we don't need it, so we'll skip it.
     * TO DO: make sure that we are only skipping this string and not something else
     */
    protected static Element getAntLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath() + " does not exist.");
        }
        try {
            String beginning = getFileContent(file);
            int skip = beginning.lastIndexOf("<build");
            if (skip < 0) {
                throw new CruiseControlException("build tag not found in " + file.getAbsolutePath());
            }

            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            bufferedReader.skip(skip);
            SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            // get rid of empty <task>- and <message>-elements created by Ant's XmlLogger
            XMLFilter emptyTaskFilter = new EmptyElementFilter("task");
            XMLFilter emptyMessageFilter = new EmptyElementFilter("message");
            emptyMessageFilter.setParent(emptyTaskFilter);
            builder.setXMLFilter(emptyMessageFilter);
            return builder.build(bufferedReader).getRootElement();
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

    private static String getFileContent(File f) throws IOException {
        Reader r = new FileReader(f);
        try {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 150; i++) {
                int character = r.read();
                if (character == -1) {
                    break;
                }
                sb.append((char) character);
            }
            String beginning = sb.toString();
            return beginning;
        } finally {
            r.close();
        }
    }

    /**
     * Sets build timeout in seconds.
     *
     * @param timeout long build timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setUseDebug(boolean debug) {
        useDebug = debug;
    }

    public String getLoggerClassName() {
        return loggerClassName;
    }

    public void setLoggerClassName(String string) {
        loggerClassName = string;
    }

    public class JVMArg {
        private String arg;

        public void setArg(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
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
