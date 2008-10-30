/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.CruiseControlMain;
import net.sourceforge.cruisecontrol.launch.Launcher;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.report.BuildLoopPostingConfiguration;
import net.sourceforge.cruisecontrol.util.MainArgs;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueueProperties;
import net.sourceforge.cruisecontrol.web.EmbeddedJettyServer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Command line entry point.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2002
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public final class Main implements CruiseControlMain {

    private static final Logger LOG = Logger.getLogger(Main.class);

     
    /** the default name for the instance. */
    private static final String DEFAULT_NAME = "";
    
    /**
     * the default webapp directory.
     */
    private static final String DEFAULT_WEBAPP_PATH = "/webapps/cruisecontrol";

    /**
     * the default dashboard (new webapp) directory.
     */
    private static final String DEFAULT_DASHBOARD_PATH = "/webapps/dashboard";

    /**
     * the default port for the embedded Jetty.
     */
    private static final int DEFAULT_WEB_PORT = 8080;

    /**
     * the default time interval used for http posting.
     */
    private static final int DEFAULT_INTERVAL = 5;
    
    private static final boolean DEFAULT_POSTING_ENABLED = true;
    
    /**
     * Commandline entry point into the application.
     *
     * @param args command line arguments
     * @deprecated Use the Launcher class instead
     */
    public static void main(String[] args) {
        boolean normalExit = new Main().start(args);
        if (!normalExit) {
            System.exit(1);
        }
    }

    private CruiseControlController controller;

    private CruiseControlControllerAgent agent;

    /**
     * Print the version, configure the project with serialized build info and/or arguments and start the project build
     * process.
     *
     * @return true indicates normal return/exit.
     */
    public boolean start(String[] args) {
        Properties versionProperties = getBuildVersionProperties();
        printVersion(versionProperties);
        if (shouldPrintUsage(args)) {
            printUsage();
            return false;
        }
        try {
            checkDeprecatedArguments(args, LOG);
            if (MainArgs.findIndex(args, "debug") != MainArgs.NOT_FOUND) {
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }
            controller = createController(args, versionProperties);
            if (shouldStartJmxAgent(args)) {
                startJmxAgent(args);
            }
            if (shouldStartEmbeddedServer(args)) {
                startEmbeddedServer(args);
            }
            if (shouldPostDataToDashboard(args)) {
                startPostingToDashboard(args);
            }
            parseCCName(args);
            controller.resume();
        } catch (CruiseControlException e) {
            LOG.fatal(e.getMessage());
            printUsage();
            return false;
        }
        return true;
    }

    private void startJmxAgent(String[] args) {
        agent = new CruiseControlControllerAgent(controller, parseJMXHttpPort(args),
                parseRmiPort(args), parseUser(args), parsePassword(args), parseXslPath(args), 
                parseEnableJMXAgentUtility(args));
        agent.start();
    }

    private CruiseControlController createController(String[] args, Properties versionProperties)
      throws CruiseControlException {
        CruiseControlController ccController = new CruiseControlController();
        ccController.setVersionProperties(versionProperties);
        File configFile = new File(parseConfigFileName(args, CruiseControlController.DEFAULT_CONFIG_FILE_NAME));
        try {
          ccController.setConfigFile(configFile);
        } catch (CruiseControlException e) {
            LOG.error("error setting config file on controller", e);
            throw e;
        }
        int maxNbThreads = ccController.getConfigManager().getCruiseControlConfig().getMaxNbThreads(); 
        ThreadQueueProperties.setMaxThreadCount(maxNbThreads);
        return ccController;
    }

    /**
     * Starts the embedded Jetty server on the port given by the command line argument -webport and loads the
     * application from the path specified by the command line argument -webapppath. Uses default values if either
     * argument are not specified.
     *
     * @param args command line arguments
     * @throws CruiseControlException if final configfile value is null
     */
    void startEmbeddedServer(final String[] args) throws CruiseControlException {
        String configFileName = parseConfigFileName(args, CruiseControlController.DEFAULT_CONFIG_FILE_NAME);
        int jmxPort = parseJMXHttpPort(args);
        int rmiPort = parseRmiPort(args);
        setUpSystemPropertiesForDashboard(configFileName, jmxPort, rmiPort);
        
        String ccHome = System.getProperty(Launcher.CCHOME_PROPERTY, ".");
        System.setProperty("jetty.home", ccHome);
        
        File jettyXml = new File(parseJettyXml(args, ccHome));
        EmbeddedJettyServer embeddedJettyServer = new EmbeddedJettyServer(jettyXml, parseWebPort(args));
        embeddedJettyServer.start();
    }
    
    private void setUpSystemPropertiesForDashboard(String configFileName, int jmxPort, int rmiPort) {
        if (configFileName != null) {
            File configFile = new File(configFileName);
            if (!configFile.exists()) {
                throw new RuntimeException("Cannot find config file at " + configFile.getAbsolutePath());
            }
            System.setProperty("cc.config.file", configFile.getAbsolutePath());
        }
        System.setProperty("cc.rmiport", String.valueOf(rmiPort));
        System.setProperty("cc.jmxport", String.valueOf(jmxPort));
    }

    static void checkDeprecatedArguments(String[] args, Logger logger) {
        if (MainArgs.findIndex(args, "port") != MainArgs.NOT_FOUND) {
            logger.warn("WARNING: The port argument is deprecated. Use jmxport instead.");
        }
    }

    /**
     * System property name, when if true, bypasses the system.exit call when printing
     * the usage message. Intended for unit tests only.
     */
    static final String SYSPROP_CCMAIN_SKIP_USAGE = "cc.main.skip.usage";

    public static void printUsage() {
        if (Boolean.getBoolean(SYSPROP_CCMAIN_SKIP_USAGE)) {
            return;
        }

        System.out.println("");
        System.out.println("Usage:");
        System.out.println("");
        System.out.println("Starts a continuous integration loop");
        System.out.println("");
        System.out.println("cruisecontrol [options]");
        System.out.println("");
        System.out.println("Build loop options are:");
        System.out.println("");
        System.out.println("  -configfile file     configuration file; default config.xml");
        System.out.println("  -debug               set logging level to DEBUG");
        System.out.println("  -"
                + Launcher.ARG_LOG4J_CONFIG + " url     URL to a log4j config (example: \"file:/c:/mylog4j.xml\")");
        System.out.println("  -? or -help          print this usage message");
        System.out.println("");
        System.out.println("Options when using JMX");
        System.out.println("  Note: JMX server only started if -jmxport and/or -rmiport specified");
        System.out.println("  -jmxport [number]       port of the JMX HttpAdapter; default 8000");
        System.out.println("  -rmiport [number]       RMI port of the Controller; default 1099");
        System.out.println("  -user username          username for HttpAdapter; default no login required");
        System.out.println("  -password pwd           password for HttpAdapter; default no login required");
        System.out.println("  -xslpath directory      location of jmx xsl files; default files in package");
        System.out.println("  -" + CruiseControlControllerAgent.ARG_JMX_AGENTUTIL
                + " [true/false] load JMX Build Agent utility; default is true");
        System.out.println("");
        System.out.println("Options when using embedded Jetty");
        System.out.println("  -webport [number]       port for the Reporting website; default 8080, removing");
        System.out.println("                          this propery will make cruisecontrol start without Jetty");
        System.out.println("  -jettyxml file          Jetty configuration xml. Defaults to jetty.xml");
        System.out.println("  -postenabled enabled    switch of posting current build information to dashboard");
        System.out.println("                          default is true");
        System.out.println("  -dashboardurl url       the url for dashboard (used for posting build information)");
        System.out.println("                          default is http://localhost:8080/dashboard");
        System.out.println("  -postinterval interval  how frequently build information will be posted to dashboard");
        System.out.println("                          default is 5 (in second).");
        System.out.println("  -ccname name            A logical name which will be displayed in the");
        System.out.println("                          Reporting Application's status page.");
        System.out.println("");
    }

    /**
     * Parse cc Name from arguments.
     *
     * @param args command line arguments.
     * @return the name of this instance if specified on the command line, otherwise DEFAULT_NAME.
     */
    static String parseCCName(String[] args) {
        String theCCName = MainArgs.parseArgument(args, "ccname", DEFAULT_NAME, DEFAULT_NAME);
        System.setProperty("ccname", theCCName);
        return theCCName;
    }

    static boolean shouldPostDataToDashboard(String[] args) {
        return parseHttpPostingEnabled(args) && BuildLoopMonitorRepository.getBuildLoopMonitor() == null;
    }

    public void startPostingToDashboard(String[] args) {
        String url = parseDashboardUrl(args);
        long interval = parseHttpPostingInterval(args);
        BuildLoopMonitorRepository.cancelExistingAndStartNewPosting(controller,
                new BuildLoopPostingConfiguration(url, interval));
    }

    /**
     * Parse webport from arguments.
     *
     * @param args command line arguments.
     * @return the webport if specified on the command line, otherwise DEFAULT_WEB_PORT.
     */
    static int parseWebPort(String[] args) {
        return MainArgs.parseInt(args, "webport", DEFAULT_WEB_PORT, DEFAULT_WEB_PORT);
    }

    /**
     * Parse webapppath from arguments.
     *
     * @param args command line arguments.
     * @return the webappdir if specified in the command line arguments, otherwise returns DEFAULT_WEBAPP_DIR.
     */
    String parseWebappPath(String[] args) {
        String webappPath = MainArgs.parseArgument(args, "webapppath", getDefaultWebAppPath(), getDefaultWebAppPath());
        if (webappPath != null) {
            validateWebAppPath(webappPath, "webapppath");
        }
        return webappPath;
    }

    /**
     * Creates the default webapppath by combining cchome and DEFAULT_WEBAPP_PATH
     *
     * @return the full default path
     */
    private String getDefaultWebAppPath() {
        return System.getProperty(Launcher.CCHOME_PROPERTY, ".") + DEFAULT_WEBAPP_PATH;
    }

    /**
     * Creates the default webapppath by combining cchome and DEFAULT_WEBAPP_PATH
     *
     * @return the full default path
     */
    private static String getDefaultDashboardPath() {
        return System.getProperty(Launcher.CCHOME_PROPERTY, ".") + DEFAULT_DASHBOARD_PATH;
    }

    /**
     * Parse dashboardpath (new webapp) from arguments.
     *
     * @param args command line arguments.
     * @return the directory if specified in the command line arguments, otherwise returns DEFAULT_DASHBOARD_PATH.
     */
    static String parseDashboardPath(String[] args) {
        String dashboardPath = MainArgs
                .parseArgument(args, "dashboard", getDefaultDashboardPath(), getDefaultDashboardPath());
        if (dashboardPath != null) {
            validateWebAppPath(dashboardPath, "dashboard");
        }
        return dashboardPath;
    }

    private static void validateWebAppPath(String webappPath, String path) {
        File directory = new File(webappPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    "'" + path + "' argument must specify an existing directory but was " + webappPath);
        }
        directory = new File(webappPath, "WEB-INF");
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("'" + path + "' argument must point to an exploded web app.  "
                    + "No WEB-INF directory exists for: " + webappPath);
        }
    }

    /**
     * Parse configfile from arguments and override any existing configfile value from reading serialized Project info.
     *
     * @param args command line arguments
     * @param configFileName existing configfile value read from serialized Project info
     * @return final value of configFileName; never null
     * @throws CruiseControlException if final configfile value is null
     */
    static String parseConfigFileName(String[] args, String configFileName) throws CruiseControlException {
        configFileName = MainArgs.parseArgument(args, "configfile", configFileName, null);
        if (configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    static String parseJettyXml(String[] args, String ccHome)  {
        boolean nullOrEmpty = ccHome == null || ccHome.length() == 0; 
        String defaultJettyXml = nullOrEmpty ? "etc/jetty.xml" : ccHome + "/etc/jetty.xml";
        return MainArgs.parseArgument(args, "jettyxml", defaultJettyXml, defaultJettyXml);
    }

    static boolean shouldStartJmxAgent(String[] args) {
        return MainArgs.argumentPresent(args, "jmxport") || MainArgs.argumentPresent(args, "rmiport") || MainArgs
                .argumentPresent(args, "port");
    }

    /**
     * If either -webport or -webapppath are specified on the command line, then the embedded Jetty server should be
     * started, otherwise it should not.
     *
     * @param args command line arguments.
     * @return true if the embedded Jetty server should be started, false if not.
     */
    static boolean shouldStartEmbeddedServer(String[] args) {
        return MainArgs.argumentPresent(args, "webport") || MainArgs.argumentPresent(args, "webapppath");
    }

    /**
     * Parse port number from arguments.
     *
     * @param args command line arguments
     * @return port number
     * @throws IllegalArgumentException if port argument is invalid
     */
    static int parseJMXHttpPort(String[] args) {
        if (MainArgs.argumentPresent(args, "jmxport") && MainArgs.argumentPresent(args, "port")) {
            throw new IllegalArgumentException(
                    "'jmxport' and 'port' arguments are not valid together. Use" + " 'jmxport' instead.");
        } else if (MainArgs.argumentPresent(args, "jmxport")) {
            return MainArgs.parseInt(args, "jmxport", MainArgs.NOT_FOUND, 8000);
        } else {
            return MainArgs.parseInt(args, "port", MainArgs.NOT_FOUND, 8000);
        }
    }

    static int parseRmiPort(String[] args) {
        return MainArgs.parseInt(args, "rmiport", MainArgs.NOT_FOUND, 1099);
    }

    static String parseXslPath(String[] args) {
        String xslpath = MainArgs.parseArgument(args, "xslpath", null, null);
        if (xslpath != null) {
            File directory = new File(xslpath);
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException(
                        "'xslpath' argument must specify an existing directory but was " + xslpath);
            }
        }
        return xslpath;
    }

    static CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL parseEnableJMXAgentUtility(String[] args) {
        if (!MainArgs.argumentPresent(args, CruiseControlControllerAgent.ARG_JMX_AGENTUTIL)) {
            /** default, if no command line arg present. Not an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.LOAD_IF_AVAILABLE;
        }

        if (MainArgs.parseBoolean(args, CruiseControlControllerAgent.ARG_JMX_AGENTUTIL, false, true)) {
            /** -agentutil true. Considered an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD;
        }

        /** -agentutil false. Do not attempt to load. */
        return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_BYPASS;
    }

    /**
     * Parse password from arguments and override any existing password value from reading serialized Project info.
     *
     * @param args command line arguments
     * @return final value of password.
     */
    static String parsePassword(String[] args) {
        return MainArgs.parseArgument(args, "password", null, null);
    }

    /**
     * Parse user from arguments and override any existing user value from reading serialized Project info.
     *
     * @param args command line arguments
     * @return final value of user.
     */
    static String parseUser(String[] args) {
        return MainArgs.parseArgument(args, "user", null, null);
    }

    /**
     * @return the current version information, as indicated in the version.properties file.
     */
    private static Properties getBuildVersionProperties() {
        Properties props = new Properties();
        try {
            props.load(Main.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            LOG.error("Error reading version properties", e);
        }
        return props;
    }

    /**
     * Writes the current version information to the logging information stream.
     * @param props the current version information, as indicated in the version.properties file.
     */
    private static void printVersion(Properties props) {
        LOG.info("CruiseControl Version " + props.getProperty("version") + " " + props.getProperty("version.info"));
    }

    static boolean shouldPrintUsage(String[] args) {
        return MainArgs.findIndex(args, "?") != MainArgs.NOT_FOUND
                || MainArgs.findIndex(args, "help") != MainArgs.NOT_FOUND;
    }

    public void stop() {
        controller.pause();
        agent.stop();
    }

    public static String parseDashboardUrl(String[] args) {
        int webport = parseWebPort(args);
        return MainArgs.parseArgument(args, "dashboardurl", defaultDashboardUrl(webport), defaultDashboardUrl(8080));
    }

    private static String defaultDashboardUrl(int port) {
        return "http://localhost:" + port + "/dashboard";
    }

    public static long parseHttpPostingInterval(String[] args) {
        return MainArgs.parseInt(args, "postinterval", DEFAULT_INTERVAL, DEFAULT_INTERVAL);
    }

    public static boolean parseHttpPostingEnabled(String[] args) {
        return MainArgs.parseBoolean(args, "postenabled", DEFAULT_POSTING_ENABLED, DEFAULT_POSTING_ENABLED);
    }
}
