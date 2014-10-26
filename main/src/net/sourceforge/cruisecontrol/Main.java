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

import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.Configuration;
import net.sourceforge.cruisecontrol.launch.CruiseControlMain;
import net.sourceforge.cruisecontrol.launch.LaunchException;
import net.sourceforge.cruisecontrol.launch.LogInterface;
import net.sourceforge.cruisecontrol.report.BuildLoopMonitorRepository;
import net.sourceforge.cruisecontrol.report.BuildLoopPostingConfiguration;
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

    private CruiseControlController controller;

    private CruiseControlControllerAgent agent;

    /**
     * Print the version, configure the project with serialized build info and/or arguments and start the project build
     * process.
     *
     * @return true indicates normal return/exit.
     */
    public boolean start(Configuration config) {
        Properties versionProperties = getBuildVersionProperties();
        printVersion(versionProperties);
        try {
            if (shouldPrintUsage(config)) {
                printUsage();
                return false;
            }

            if (config.getOptionBool(Configuration.KEY_DEBUG)) {
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }
            // Set the logger. Now it is fully configured
            try {
                Configuration.setRealLog(new Log4jLog());
            } catch (LaunchException e) {
                LOG.error("Unable to set real logger to config class; all previous messages are probably lost", e);
            }

            checkDeprecatedArguments(config, LOG);
            controller = createController(config, versionProperties);
            if (shouldStartJmxAgent(config)) {
                startJmxAgent(config);
            }
            if (shouldStartEmbeddedServer(config)) {
                startEmbeddedServer(config);
            }
            if (shouldPostDataToDashboard(config)) {
                startPostingToDashboard(config);
            }
            parseCCName(config);
            controller.resume();
        } catch (Exception e) {
            LOG.fatal(e.getMessage());
            printUsage();
            return false;
        }
        return true;
    }

    private void startJmxAgent(Configuration conf) {
        agent = new CruiseControlControllerAgent(controller, parseJMXHttpPort(conf),
                parseRmiPort(conf), parseUser(conf), parsePassword(conf), parseXslPath(conf), 
                parseEnableJMXAgentUtility(conf));
        agent.start();
    }

    private CruiseControlController createController(Configuration config, Properties versionProperties)
            throws CruiseControlException {
        CruiseControlController ccController = new CruiseControlController();
        ccController.setVersionProperties(versionProperties);
        File configFile = new File(parseConfigFileName(config, CruiseControlController.DEFAULT_CONFIG_FILE_NAME));
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
     * @param conf configuration holder
     * @throws CruiseControlException if final configfile value is null
     */
    void startEmbeddedServer(Configuration conf) throws CruiseControlException {
        String configFileName = parseConfigFileName(conf, CruiseControlController.DEFAULT_CONFIG_FILE_NAME);
        int jmxPort = parseJMXHttpPort(conf);
        int rmiPort = parseRmiPort(conf);
        int webPort = parseWebPort(conf);
        setUpSystemPropertiesForDashboard(configFileName, jmxPort, rmiPort, webPort);
        
        File ccHome;
        try {
            ccHome = conf.getOptionFile(Configuration.KEY_HOME_DIR);
        } catch (IllegalArgumentException e) {
            throw new CruiseControlException(e);
        }

        System.setProperty("jetty.home", ccHome.getAbsolutePath());
        
        File jettyXml = new File(parseJettyXml(conf, ccHome.getAbsolutePath()));
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

    static void checkDeprecatedArguments(Configuration conf, Logger logger) {
        if (conf.wasOptionSet(Configuration.KEY_PORT)) {
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
        System.out.println("  -" + Configuration.KEY_DEBUG + "  set logging level to DEBUG");
        System.out.println("  -"
                + Configuration.KEY_LOG4J_CONFIG + " url     URL to a log4j config (example: "
                + "\"file:/c:/mylog4j.xml\")");
        System.out.println("  -" + Configuration.KEY_PRINT_HELP1 + " or -" + Configuration.KEY_PRINT_HELP2
                + "          print this usage message");
        System.out.println("");
        System.out.println("Options when using JMX");
        System.out.println("  Note: JMX server only started if -jmxport and/or -rmiport specified");
        System.out.println("  -jmxport [number]       port of the JMX HttpAdapter; default 8000");
        System.out.println("  -rmiport [number]       RMI port of the Controller; default 1099");
        System.out.println("  -user username          username for HttpAdapter; default no login required");
        System.out.println("  -password pwd           password for HttpAdapter; default no login required");
        System.out.println("  -xslpath directory      location of jmx xsl files; default files in package");
        System.out.println("  -" + Configuration.KEY_JMX_AGENT_UTIL
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
        System.out.println("                          default is 5 (in seconds).");
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
    static String parseCCName(Configuration conf) {
        final String theCCName = conf.getOptionStr(Configuration.KEY_CC_NAME);
        System.setProperty("ccname", theCCName);
        return theCCName;
    }

    static boolean shouldPostDataToDashboard(Configuration conf) {
        return parseHttpPostingEnabled(conf) && BuildLoopMonitorRepository.getBuildLoopMonitor() == null;
    }

    public void startPostingToDashboard(Configuration conf) {
        String url = parseDashboardUrl(conf);
        long interval = parseHttpPostingInterval(conf);
        BuildLoopMonitorRepository.cancelExistingAndStartNewPosting(controller,
                new BuildLoopPostingConfiguration(url, interval));
    }

    /**
     * Parse webport from arguments.
     *
     * @param args command line arguments.
     * @return the webport if specified, otherwise its default value.
     */
    static int parseWebPort(Configuration conf) {
        return conf.getOptionInt(Configuration.KEY_WEB_PORT);
        //return MainArgs.parseInt(args, "webport", DEFAULT_WEB_PORT, DEFAULT_WEB_PORT);
    }

    /**
     * Parse webapppath from arguments.
     *
     * @param args command line arguments.
     * @return the webappdir if specified in the command line arguments, otherwise returns DEFAULT_WEBAPP_DIR.
     */
    String parseWebappPath(Configuration conf) {
        final File webappPath = conf.getOptionDir(Configuration.KEY_WEBAPP_PATH);
        // TODO: validate is useless, since conf.getOptionDir() already validates the dir
        validateWebAppPath(webappPath, Configuration.KEY_WEBAPP_PATH);
        return webappPath.getAbsolutePath();
    }

    /**
     * Parse dashboardpath (new webapp) from arguments.
     *
     * @param args command line arguments.
     * @return the directory if specified in the command line arguments, otherwise returns DEFAULT_DASHBOARD_PATH.
     */
    static String parseDashboardPath(Configuration conf) {
        File dashboardPath = conf.getOptionFile(Configuration.KEY_DASHBOARD);
        validateWebAppPath(dashboardPath, Configuration.KEY_DASHBOARD);

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
     * Parse configfile from arguments and override any existing configfile value from reading serialized Project info.
     *
     * @param conf configuration holder
     * @param configFileName existing configfile value read from serialized Project info (DEPRECATED!)
     * @return final value of configFileName; never null
     * @throws CruiseControlException if final configfile value is invalid
     */
    static String parseConfigFileName(Configuration conf, String configFileName) throws CruiseControlException {
        try {
            configFileName = conf.getOptionFile(Configuration.KEY_CONFIG_FILE).getAbsolutePath();
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

    static String parseJettyXml(Configuration conf, String ccHome) {
        if (conf.wasOptionSet(Configuration.KEY_JETTY_XML)) {
            return conf.getOptionFile(Configuration.KEY_JETTY_XML).getAbsolutePath();
        }
        final boolean nullOrEmpty = ccHome == null || ccHome.length() == 0;
        final String defaultJettyXml = conf.getOptionFile(Configuration.KEY_JETTY_XML).getPath();
        return nullOrEmpty ? defaultJettyXml : ccHome + File.separatorChar + defaultJettyXml;
    }

    static boolean shouldStartJmxAgent(Configuration conf) {
        return conf.wasOptionSet(Configuration.KEY_JMX_PORT) || conf.wasOptionSet(Configuration.KEY_RMI_PORT)
                || conf.wasOptionSet(Configuration.KEY_PORT);
    }

    /**
     * If either -webport or -webapppath are specified on the command line, then the embedded Jetty server should be
     * started, otherwise it should not.
     *
     * @param conf configuration holder
     * @return true if the embedded Jetty server should be started, false if not.
     */
    static boolean shouldStartEmbeddedServer(Configuration conf) {
        return conf.wasOptionSet(Configuration.KEY_WEB_PORT) || conf.wasOptionSet(Configuration.KEY_WEBAPP_PATH);
    }

    /**
     * Parse port number from arguments.
     *
     * @param conf configuration holder
     * @return port number
     * @throws IllegalArgumentException if port argument is invalid
     */
    static int parseJMXHttpPort(Configuration conf) {
        if (conf.wasOptionSet(Configuration.KEY_JMX_PORT) && conf.wasOptionSet(Configuration.KEY_PORT)) {
            throw new IllegalArgumentException(
                    "'jmxport' and 'port' arguments are not valid together. Use" + " 'jmxport' instead.");
        } else if (conf.wasOptionSet(Configuration.KEY_JMX_PORT)) {
            return conf.getOptionInt(Configuration.KEY_JMX_PORT);
        } else {
            return conf.getOptionInt(Configuration.KEY_PORT);
        }
    }

    static int parseRmiPort(Configuration conf) {
        return conf.getOptionInt(Configuration.KEY_RMI_PORT);
    }

    static String parseXslPath(Configuration conf) {
        return conf.getOptionDir(Configuration.KEY_XLS_PATH).getAbsolutePath();
    }

    static CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL parseEnableJMXAgentUtility(Configuration conf) {
        final String argval = conf.getOptionStr(Configuration.KEY_JMX_AGENT_UTIL); 
        if (argval.isEmpty()) {
            /** default, if no command line arg present. Not an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.LOAD_IF_AVAILABLE;
        }
        if (Boolean.parseBoolean(argval)) {
            /** -agentutil true. Considered an error if load fails. */
            return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_LOAD;
        }
        /** -agentutil false or not set. Do not attempt to load. */
        return  CruiseControlControllerAgent.LOAD_JMX_AGENTUTIL.FORCE_BYPASS;
    }

    /**
     * Parse password from arguments and override any existing password value from reading serialized Project info.
     *
     * @param args command line arguments
     * @return final value of password.
     */
    static String parsePassword(Configuration conf) {
        if (conf.wasOptionSet(Configuration.KEY_PASSWORD)) {
            return conf.getOptionStr(Configuration.KEY_PASSWORD);
        }
        return null;
    }

    /**
     * Parse user from arguments and override any existing user value from reading serialized Project info.
     *
     * @param args command line arguments
     * @return final value of user.
     */
    static String parseUser(Configuration conf) {
        if (conf.wasOptionSet(Configuration.KEY_USER)) {
            return conf.getOptionStr(Configuration.KEY_USER);
        }
        return null;
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

    static boolean shouldPrintUsage(Configuration conf) {
        return (conf.getOptionBool(Configuration.KEY_PRINT_HELP1) || conf.getOptionBool(Configuration.KEY_PRINT_HELP2));
    }

    public void stop() {
        controller.pause();
        agent.stop();
    }

    public static String parseDashboardUrl(Configuration conf) {
        URL url = conf.getOptionUrl(Configuration.KEY_DASHBOARD_URL);

        // Special composition of some arguments if port has been set but dashboard url
        // has not. Use default dashboard url, but change the default port in it
        if (conf.wasOptionSet(Configuration.KEY_WEB_PORT) && !conf.wasOptionSet(Configuration.KEY_DASHBOARD_URL)) {
            try {
              url = new URL(url.getProtocol(), url.getHost(), conf.getOptionInt(Configuration.KEY_WEB_PORT),
                      url.getPath());
            } catch (MalformedURLException e) {
              // should not happen ...
            }
        }
        return url.toString();
    }

    public static int parseHttpPostingInterval(Configuration conf) {
        return conf.getOptionInt(Configuration.KEY_POST_INTERVAL);
    }

    public static boolean parseHttpPostingEnabled(Configuration conf) {
        return conf.getOptionBool(Configuration.KEY_POST_ENABLED);
    }

    /** Implementation of the {@link LogInterface} passing data to Log4j logger instance */
    private static class Log4jLog implements LogInterface {

        @Override
        public void error(Object message) {
            LOG.error(message); // use the context of Main
        }
        @Override
        public void warn(Object message) {
            LOG.warn(message);
        }
        @Override
        public void info(Object message) {
            LOG.info(message);
        }
        @Override
        /** Does nothing, throws LaunchException when called */
        public void flush(LogInterface log) throws LaunchException {
            throw new LaunchException("Cannot flush log4j to nother log, probably trying to set "
                    + "log4j when one already set");
        }
    }
}
