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
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

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
    private boolean useQuiet = false;
    private String loggerClassName = DEFAULT_LOGGER;
    private long timeout = -1;
    private File saveLogDir = null;
    private String antHome;

    public void validate() throws CruiseControlException {
        super.validate();

        if (buildFile == null) {
            throw new CruiseControlException("'buildfile' is a required attribute on AntBuilder");
        }

        if (target == null) {
            throw new CruiseControlException("'target' is a required attribute on AntBuilder");
        }
        
        if (useDebug && useQuiet) {
            throw new CruiseControlException("'useDebug' and 'useQuiet' can't be used together");
        }
        
        if (!useLogger && (useDebug || useQuiet)) {
            LOG.warn("usedebug and usequiet are ignored if uselogger is not set to 'true'!");
        }
        
        if (antScript != null && !args.isEmpty()) {
            LOG.warn("jvmargs will be ignored if you specify your own antscript!");
        }
        
        if (saveLogDir != null) {
            if (!saveLogDir.isDirectory()) {
                throw new CruiseControlException("'saveLogDir' must exist and be a directory");
            }
        }

        if (antScript != null && antHome != null) {
            throw new CruiseControlException("'antHome' and 'antscript' cannot both be set");
        }

        if (antHome != null) {
            final File antHomeFile = new File(antHome);
            if (!antHomeFile.exists() || !antHomeFile.isDirectory()) {
                throw new CruiseControlException("'antHome' must exist and be a directory. Expected to find "
                        + antHomeFile.getAbsolutePath());
            }

            final File antScriptInAntHome = new File(findAntScript(Util.isWindows()));
            if (!antScriptInAntHome.exists() || !antScriptInAntHome.isFile()) {
                throw new CruiseControlException("'antHome' must contain an ant execution script. Expected to find "
                        + antScriptInAntHome.getAbsolutePath());
            }
        }


    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        Process p;
        try {
            String[] commandLine = getCommandLineArgs(buildProperties, useLogger, Util.isWindows());

            File workingDir = antWorkingDir != null ? new File(antWorkingDir) : null;

            if (LOG.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Executing Command: ");
                for (int i = 0; i < commandLine.length; i++) {
                    sb.append(commandLine[i]).append(" ");
                }
                if (workingDir != null) {
                    sb.append("in directory " + workingDir.getCanonicalPath());
                }
                LOG.debug(sb);
            }

            p = Runtime.getRuntime().exec(commandLine, null, workingDir);
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute Ant."
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
            LOG.info("Was interrupted while waiting for Ant to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } catch (IOException ie) {
            LOG.info("Exception trying to close Process streams.", ie);
        }

        outPumper.flush();
        errorPumper.flush();

        File logFile = new File(antWorkingDir, tempFileName);
        Element buildLogElement;
        if (killer.processKilled()) {
            LOG.warn("Build timeout timer of " + timeout + " seconds has expired");
            buildLogElement = new Element("build");
            buildLogElement.setAttribute("error", "build timeout");
            // although log file is most certainly empy, let's try to preserve it
            // somebody should really fix ant's XmlLogger
            if (logFile.exists()) {
                try {
                    buildLogElement.setText(Util.readFileToString(logFile));
                } catch (IOException likely) {
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
            File newAntLogFile = new File(saveLogDir, tempFileName);
            newAntLogFile.createNewFile();

            FileInputStream in = new FileInputStream(logFile);
            FileOutputStream out = new FileOutputStream(newAntLogFile);

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
     * Used to invoke the builder via JMX with a different target.
     */
    protected void overrideTarget(String target) {
        setTarget(target);    
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

    /**
     * construct the command that we're going to execute.
     *
     * @param buildProperties Map holding key/value pairs of arguments to the build process
     * @return String[] holding command to be executed
     * @throws CruiseControlException on unquotable attributes
     */
    protected String[] getCommandLineArgs(Map buildProperties,
                                          boolean useLogger,
                                          boolean isWindows) throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        if (antScript != null) {
            cmdLine.setExecutable(antScript);
        } else if (antHome != null) {
            cmdLine.setExecutable(findAntScript(isWindows));
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
            String systemClassPath = getSystemClassPath();
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

    protected String getSystemClassPath() {
      return System.getProperty("java.class.path");
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

    protected static Element getAntLogAsElement(File file) throws CruiseControlException {
        if (!file.exists()) {
            throw new CruiseControlException("ant logfile " + file.getAbsolutePath() + " does not exist.");
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

    public void setUseQuiet(boolean quiet) {
        useQuiet = quiet;
    }

    public String getLoggerClassName() {
        return loggerClassName;
    }

    public void setLoggerClassName(String string) {
        loggerClassName = string;
    }

    public void setAntHome(String string) {
        antHome = string;
    }

    /**
     * If the anthome attribute is set, then this method returns the correct shell script
     * to use for a specific environment.
     */
    protected String findAntScript(boolean isWindows) throws CruiseControlException {
        if (antHome == null) {
            throw new CruiseControlException("anthome attribute not set.");
        }

        if (isWindows) {
            return antHome + "\\bin\\ant.bat";
        } else {
            return antHome + "/bin/ant.sh";
        }
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
