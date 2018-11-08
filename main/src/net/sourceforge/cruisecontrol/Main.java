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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.Config;
import net.sourceforge.cruisecontrol.launch.CruiseControlMain;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.report.BuildLoopPostingConfiguration;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueueProperties;
import net.sourceforge.cruisecontrol.web.EmbeddedJettyServer;

/**
 * Command line entry point.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2002
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public final class Main implements CruiseControlMain {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private CruiseControlController controller;

    private CruiseControlControllerAgent agent;



    public Config confFactory(Object owner) {
        try {
            return CruiseControlSettings.getInstance(owner);
        } catch (CruiseControlException e) {
            throw new IllegalStateException("Configuration instantiation failed", e);
        }
    }

    /**
     * Print the version, configure the project with serialized build info and/or arguments and start the project build
     * process.
     *
     * @return true indicates normal return/exit.
     */
    public boolean start() {
        CruiseControlSettings config;

        // Config must be filled
        try {
            config = CruiseControlSettings.getInstance();
        } catch (CruiseControlException e) {
            throw new IllegalStateException("Configuration instance has not been instantiated");
        }


        Properties versionProperties = getBuildVersionProperties();
        printVersion(versionProperties);
        try {
            if (shouldPrintUsage()) {
                printUsage();
                return false;
            }

            if (config.getOptionBool(CruiseControlSettings.KEY_DEBUG)) {
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }
//            // Set the logger. Now it is fully configured
//            try {
//                Configuration.setRealLog(new Log4jLog());
//            } catch (LaunchException e) {
//                LOG.error("Unable to set real logger to config class; all previous messages are probably lost", e);
//            }

            checkDeprecatedArguments();
            controller = createController(versionProperties);
            if (shouldStartJmxAgent()) {
                startJmxAgent();
            }
            if (shouldStartEmbeddedServer()) {
                startEmbeddedServer();
            } else {
                LOG.info("Skipping start of embedded server");
            }
            if (shouldPostDataToDashboard()) {
                startPostingToDashboard();
            }
            parseCCName();
            controller.resume();
        } catch (Exception e) {
            LOG.fatal(e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.fatal("Failure details:", e);
            }
            System.err.println("Failed to start CruiseControl: " + e.getMessage());
            System.err.println("See log file for more details");
            printUsage();
            return false;
        }
        return true;
    }

    private void startJmxAgent() throws CruiseControlException {
        agent = new CruiseControlControllerAgent(controller, parseJMXHttpPort(),
                parseRmiPort(), parseUser(), parsePassword(), parseXslPath(),
                parseEnableJMXAgentUtility());
        agent.start();
    }

    private CruiseControlController createController(Properties versionProperties)
            throws CruiseControlException {
        CruiseControlController ccController = new CruiseControlController();
        ccController.setVersionProperties(versionProperties);
        File configFile = CruiseControlSettings.getInstance().getOptionFile(CruiseControlSettings.KEY_CONFIG_FILE);
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
     * @throws CruiseControlException if final config file value is null
     */
    void startEmbeddedServer() throws CruiseControlException {
        CruiseControlSettings config = CruiseControlSettings.getInstance();
        String configFileName = parseConfigFileName(config.getOptionFile(
                    CruiseControlSettings.KEY_CONFIG_FILE).getAbsolutePath());
        int jmxPort = parseJMXHttpPort();
        int rmiPort = parseRmiPort();
        int webPort = parseWebPort();
        setUpSystemPropertiesForDashboard(configFileName, jmxPort, rmiPort, webPort);

        File ccDist = config.getOptionDir(CruiseControlSettings.KEY_DIST_DIR);

        System.setProperty("jetty.home", ccDist.getAbsolutePath());

        File jettyXml = new File(parseJettyXml(ccDist));
        EmbeddedJettyServer embeddedJettyServer = new EmbeddedJettyServer(jettyXml, webPort);
        embeddedJettyServer.start();
    }

    public static void setUpSystemPropertiesForDashboard(String configFileName, int jmxPort, int rmiPort, int webPort) {
        if (configFileName != null) {
            File configFile = new File(configFileName);
            if (!configFile.exists()) {
                throw new RuntimeException("Cannot find config file at " + configFile.getAbsolutePath());
            }
            System.setProperty("cc.config.file", configFile.getAbsolutePath());
        }
        System.setProperty("cc.rmiport", String.valueOf(rmiPort));
        System.setProperty("cc.jmxport", String.valueOf(jmxPort));
        System.setProperty("cc.webport", String.valueOf(webPort));
    }

    static void checkDeprecatedArguments() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        if (config.wasOptionSet(CruiseControlSettings.KEY_PORT)) {
            LOG.warn("WARNING: The port argument is deprecated. Use jmxport instead.");
        }
    }

    /**
     * System property name, when if true, bypasses the system.exit call when printing
     * the usage message. Intended for unit tests only.
     */
    static final String SYSPROP_CCMAIN_SKIP_USAGE = "cc.main.skip.usage";

    public void printUsage() {
        if (Boolean.getBoolean(SYSPROP_CCMAIN_SKIP_USAGE)) {
            return;
        }

        System.out.println("");
        System.out.println("Starts a continuous integration loop");
        System.out.println("Build loop options are:");
        System.out.println("");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_CONFIG_FILE));
        System.out.println("       ... the main cruisecontrol configuration file. It is the XML file with ");
        System.out.println("           <cruisecontrol> root element.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_DEBUG));
        System.out.println("       ... set logging level to DEBUG.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_PRINT_HELP1));
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_PRINT_HELP2));
        System.out.println("       ... print this usage message.");
        System.out.println("");
        System.out.println("Options when using JMX");
        System.out.println("  Note: JMX server is only started if -jmxport and/or -rmiport specified.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_JMX_PORT));
        System.out.println("       ... port of the JMX HttpAdapter.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_RMI_PORT));
        System.out.println("       ... RMI port of the Controller.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_USER));
        System.out.println("       ... username for HttpAdapter; when not set, no login is required.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_PASSWORD));
        System.out.println("       ... password for HttpAdapter; when not set, no login is required.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_XLS_PATH));
        System.out.println("       ... location of jmx xsl files.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_JMX_AGENT_UTIL));
        System.out.println("       ... load JMX Build Agent utility.");
        System.out.println("");
        System.out.println("Options when using embedded Jetty");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_WEB_PORT));
        System.out.println("       ... port for the Reporting website; removing this propery will make ");
        System.out.println("           cruisecontrol start without Jetty.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_JETTY_XML));
        System.out.println("       ... Jetty configuration xml.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_POST_ENABLED));
        System.out.println("       ... switch of posting current build information to dashboard.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_POST_INTERVAL));
        System.out.println("       ... interval how frequently build information will be posted to dashboard,");
        System.out.println("           value in seconds.");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_DASHBOARD_URL));
        System.out.println("       ... the url for dashboard (used for posting build information).");
        System.out.println(CruiseControlSettings.getBaseHelp(CruiseControlSettings.KEY_CC_NAME));
        System.out.println("       ... A logical name which will be displayed in the Reporting Application's");
        System.out.println("           status page.");

        System.out.println("");
    }

    /**
     * Parse cc Name from configuration.
     *
     * @return the name of this instance if specified on the command line, otherwise DEFAULT_NAME.
     * @throws CruiseControlException
     */
    static String parseCCName() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final String theCCName = config.getOptionStr(CruiseControlSettings.KEY_CC_NAME);
        System.setProperty("ccname", theCCName);
        return theCCName;
    }

    static boolean shouldPostDataToDashboard() throws CruiseControlException {
        return parseHttpPostingEnabled() && BuildLoopMonitorRepository.getBuildLoopMonitor() == null;
    }

    public void startPostingToDashboard() throws CruiseControlException {
        final String url = parseDashboardUrl();
        final long interval = parseHttpPostingInterval();
        BuildLoopMonitorRepository.cancelExistingAndStartNewPosting(controller,
                new BuildLoopPostingConfiguration(url, interval));
    }

    /**
     * Parse webport from configuration.
     *
     * @return the webport if specified, otherwise its default value.
     * @throws CruiseControlException
     */
    static int parseWebPort() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionInt(CruiseControlSettings.KEY_WEB_PORT);
    }

    /**
     * Parse webapppath from configuration.
     *
     * @return the webappdir if specified in the command line arguments, otherwise returns DEFAULT_WEBAPP_DIR.
     * @throws CruiseControlException
     */
    String parseWebappPath() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final File webappPath = config.getOptionDir(CruiseControlSettings.KEY_WEBAPP_PATH);
        // TODO: validate is useless, since conf.getOptionDir() already validates the dir
        validateWebAppPath(webappPath, CruiseControlSettings.KEY_WEBAPP_PATH);
        return webappPath.getAbsolutePath();
    }

    /**
     * Parse dashboardpath (new webapp) from configuration.
     *
     * @param args command line arguments.
     * @return the directory if specified in the command line arguments, otherwise returns DEFAULT_DASHBOARD_PATH.
     * @throws CruiseControlException
     */
    static String parseDashboardPath() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final File dashboardPath = config.getOptionFile(CruiseControlSettings.KEY_DASHBOARD);
        validateWebAppPath(dashboardPath, CruiseControlSettings.KEY_DASHBOARD);

        return dashboardPath.getAbsolutePath();
    }

    private static void validateWebAppPath(File webappPath, String path) {
        if (!webappPath.isDirectory()) {
            throw new IllegalArgumentException(
                    "'" + path + "' argument must specify an existing directory but was " + webappPath);
        }
        webappPath = new File(webappPath, "WEB-INF");
        if (!webappPath.isDirectory()) {
            throw new IllegalArgumentException("'" + path + "' argument must point to an exploded web app.  "
                    + "directory " + webappPath + " does not exist");
        }
    }

    /**
     * Parse configfile from configuration and override any existing configfile value from reading serialized
     * Project info.
     *
     * @param configFileName existing configfile value read from serialized Project info (DEPRECATED!)
     * @return final value of configFileName; never null
     * @throws CruiseControlException if final configfile value is invalid
     */
    static String parseConfigFileName(String configFileName) throws CruiseControlException {
        try {
            CruiseControlSettings config = CruiseControlSettings.getInstance();
            configFileName = config.getOptionFile(CruiseControlSettings.KEY_CONFIG_FILE).getAbsolutePath();
        } catch (IllegalArgumentException e) {
            // This is deprecated behaviour where the given config file is used instad of relaying on
            // Configuration's ability to check that the (default) config file does not exist
            LOG.warn("DEPRECATED behaviour. The default config file was not found (as reported by the"
                    + "embedded exception), so relying on " + configFileName + " path instead", e);
        } catch (Exception e) {
            throw new CruiseControlException(e);
        }
        if (configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    static String parseJettyXml(File ccDist) throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        if (config.wasOptionSet(CruiseControlSettings.KEY_JETTY_XML)) {
            return config.getOptionFile(CruiseControlSettings.KEY_JETTY_XML).getAbsolutePath();
        }
        // ccDist not set, try current working directory
        if (ccDist == null) {
            ccDist = new File("./");
        }
        // Not set. Use default value and search it the ccDist directory
        final File path = new File(ccDist, config.getOptionRaw(CruiseControlSettings.KEY_JETTY_XML));
        if (path.exists()) {
            return path.getAbsolutePath();
        }
        throw new CruiseControlException("Unable to find " + config.getOptionRaw(CruiseControlSettings.KEY_JETTY_XML)
                + "; does not exist in " + path.getAbsolutePath());
    }

    static boolean shouldStartJmxAgent() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final boolean res;
        res = config.wasOptionSet(CruiseControlSettings.KEY_JMX_PORT)
                || config.wasOptionSet(CruiseControlSettings.KEY_RMI_PORT)
                || config.wasOptionSet(CruiseControlSettings.KEY_PORT);

        if (!res) {
            LOG.info("Skipping start of Jmx agent. None of " + CruiseControlSettings.KEY_JMX_PORT + "/"
                    + CruiseControlSettings.KEY_RMI_PORT + "/" + CruiseControlSettings.KEY_PORT + " was set");
        }
        return res;
    }

    /**
     * If either -webport or -webapppath are specified in the configuration, then the embedded Jetty server should be
     * started, otherwise it should not.
     *
     * @return true if the embedded Jetty server should be started, false if not.
     * @throws CruiseControlException
     */
    static boolean shouldStartEmbeddedServer() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final boolean res = config.wasOptionSet(CruiseControlSettings.KEY_WEB_PORT)
                || config.wasOptionSet(CruiseControlSettings.KEY_WEBAPP_PATH);

        if (!res) {
            LOG.info("Skipping start of embedded server. None of " + CruiseControlSettings.KEY_WEB_PORT + "/"
                    + CruiseControlSettings.KEY_WEBAPP_PATH + " was set");
        }
        return res;
    }

    /**
     * Parse port number from configuration.
     *
     * @return port number
     * @throws CruiseControlException
     * @throws IllegalArgumentException if port argument is invalid
     */
    static int parseJMXHttpPort() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        if (config.wasOptionSet(CruiseControlSettings.KEY_JMX_PORT)
                && config.wasOptionSet(CruiseControlSettings.KEY_PORT)) {
            throw new IllegalArgumentException(
                    "'jmxport' and 'port' arguments are not valid together. Use" + " 'jmxport' instead.");
        } else if (config.wasOptionSet(CruiseControlSettings.KEY_JMX_PORT)) {
            return config.getOptionInt(CruiseControlSettings.KEY_JMX_PORT);
        } else {
            return config.getOptionInt(CruiseControlSettings.KEY_PORT);
        }
    }

    static int parseRmiPort() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionInt(CruiseControlSettings.KEY_RMI_PORT);
    }

    /**
     * @return absolute path for {@link CruiseControlSettings#KEY_XLS_PATH} config item
     * @throws CruiseControlException
     */
    static String parseXslPath() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionDir(CruiseControlSettings.KEY_XLS_PATH).getAbsolutePath();
    }

    static CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL parseEnableJMXAgentUtility() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        final Boolean argval = (Boolean) config.getOptionType(CruiseControlSettings.KEY_JMX_AGENT_UTIL, Boolean.class);
        if (argval == null) {
            /** default, if no command line arg present. Not an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.LOAD_IF_AVAILABLE;
        }
        if (argval.booleanValue()) {
            /** -agentutil true. Considered an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD;
        }
        /** -agentutil false or not set. Do not attempt to load. */
        return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_BYPASS;
    }

    /**
     * Parse password from configuration and override any existing password value from reading serialized Project info.
     *
     * @return final value of password.
     * @throws CruiseControlException
     */
    static String parsePassword() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionStr(CruiseControlSettings.KEY_PASSWORD);
    }

    /**
     * Parse user from configuration and override any existing user value from reading serialized Project info.
     *
     * @return final value of user.
     * @throws CruiseControlException
     */
    static String parseUser() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionStr(CruiseControlSettings.KEY_USER);
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

    static boolean shouldPrintUsage() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return (config.getOptionBool(CruiseControlSettings.KEY_PRINT_HELP1)
             || config.getOptionBool(CruiseControlSettings.KEY_PRINT_HELP2));
    }

    public void stop() {
        controller.pause();
        agent.stop();
    }

    public static String parseDashboardUrl() throws CruiseControlException {
        CruiseControlSettings config = CruiseControlSettings.getInstance();
        URL url = config.getOptionUrl(CruiseControlSettings.KEY_DASHBOARD_URL);

        // Special composition of some arguments if port has been set but dashboard url
        // has not. Use default dashboard url, but change the default port in it
        if (config.wasOptionSet(CruiseControlSettings.KEY_WEB_PORT)
                && !config.wasOptionSet(CruiseControlSettings.KEY_DASHBOARD_URL)) {
            try {
              url = new URL(url.getProtocol(), url.getHost(),
                      config.getOptionInt(CruiseControlSettings.KEY_WEB_PORT), url.getPath());
            } catch (MalformedURLException e) {
              // should not happen ...
            }
        }
        return url.toString();
    }

    public static int parseHttpPostingInterval() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionInt(CruiseControlSettings.KEY_POST_INTERVAL);
    }

    public static boolean parseHttpPostingEnabled() throws CruiseControlException {
        final CruiseControlSettings config = CruiseControlSettings.getInstance();
        return config.getOptionBool(CruiseControlSettings.KEY_POST_ENABLED);
    }
}
