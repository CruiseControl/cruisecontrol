/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import net.sourceforge.cruisecontrol.launch.CruiseControlMain;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueueProperties;
import net.sourceforge.cruisecontrol.util.MainArgs;

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

    /**
     * Commandline entry point into the application.
     * 
     * @deprecated Use the Launcher class instead
     */
    public static void main(String[] args) {
        new Main().start(args);
    }

    /**
     * Print the version, configure the project with serialized build info
     * and/or arguments and start the project build process.
     */
    public void start(String[] args) {
        Properties versionProperties = getBuildVersionProperties();
        printVersion(versionProperties);
        if (shouldPrintUsage(args)) {
            printUsageAndExit();
        }
        try {
            checkDeprecatedArguments(args, LOG);

            if (MainArgs.findIndex(args, "debug") != MainArgs.NOT_FOUND) {
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }
            CruiseControlController controller = new CruiseControlController();
            controller.setVersionProperties(versionProperties);
            File configFile = new File(parseConfigFileName(args, CruiseControlController.DEFAULT_CONFIG_FILE_NAME));
            controller.setConfigFile(configFile);
            ServerXMLHelper helper = new ServerXMLHelper(configFile);
            ThreadQueueProperties.setMaxThreadCount(helper.getNumThreads());
            if (shouldStartController(args)) {
                CruiseControlControllerAgent agent = new CruiseControlControllerAgent(controller,
                        parseJMXHttpPort(args), parseRmiPort(args), parseUser(args), parsePassword(args),
                        parseXslPath(args));
                agent.start();
            }
            controller.resume();
        } catch (CruiseControlException e) {
            LOG.fatal(e.getMessage());
            printUsageAndExit();
        }
    }

    protected static void checkDeprecatedArguments(String[] args, Logger logger) {
        if (MainArgs.findIndex(args, "port") != MainArgs.NOT_FOUND) {
            logger.warn("WARNING: The port argument is deprecated. Use jmxport instead.");
        }
    }

    /**
     * System property name, when if true, bypasses the system.exit call when printing
     * the usage message. Intended for unit tests only.
     */
    public static final String SYSPROP_CCMAIN_SKIP_USAGE_EXIT = "cc.main.skip.usage.exit";

    public static void printUsageAndExit() {
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
        System.out.println("  -? or -help          print this usage message");
        System.out.println("");
        System.out.println("Options when using JMX");
        System.out.println("  Note: JMX server only started if -jmxport and/or -rmiport specified");
        System.out.println("  -jmxport [number]  port of the JMX HttpAdapter; default 8000");
        System.out.println("  -rmiport [number]      RMI port of the Controller; default 1099");
        System.out.println("  -user username         username for HttpAdapter; default no login required");
        System.out.println("  -password pwd          password for HttpAdapter; default no login required");
        System.out.println("  -xslpath directory     location of jmx xsl files; default files in package");
        System.out.println("");
        System.out.println("Options when using embedded Jetty");
        System.out.println("  -webport [number]       port for the Reporting website; default 8080");
        System.out.println("  -cchome directory       location from which to start Cruise; default to .");
        System.out.println("  -ccname name            name for this Cruise instance; default to none");
        System.out.println("");

        if (!Boolean.getBoolean(SYSPROP_CCMAIN_SKIP_USAGE_EXIT)) {
            System.exit(1);
        }
    }

    /**
     * Parse configfile from arguments and override any existing configfile
     * value from reading serialized Project info.
     * 
     * @param configFileName
     *            existing configfile value read from serialized Project info
     * @return final value of configFileName; never null
     * @throws CruiseControlException
     *             if final configfile value is null
     */
    static String parseConfigFileName(String[] args, String configFileName) throws CruiseControlException {
        configFileName = MainArgs.parseArgument(args, "configfile", configFileName, null);
        if (configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    static boolean shouldStartController(String[] args) {
        return MainArgs.argumentPresent(args, "jmxport") || MainArgs.argumentPresent(args, "rmiport")
                || MainArgs.argumentPresent(args, "port");
    }

    /**
     * Parse port number from arguments.
     * 
     * @return port number
     * @throws IllegalArgumentException
     *             if port argument is invalid
     */
    static int parseJMXHttpPort(String[] args) {
        if (MainArgs.argumentPresent(args, "jmxport") && MainArgs.argumentPresent(args, "port")) {
            throw new IllegalArgumentException("'jmxport' and 'port' arguments are not valid together. Use"
                    + " 'jmxport' instead.");
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
                throw new IllegalArgumentException("'xslpath' argument must specify an existing directory but was "
                        + xslpath);
            }
        }
        return xslpath;
    }

    /**
     * Parse password from arguments and override any existing password value
     * from reading serialized Project info.
     * 
     * @return final value of password.
     */
    static String parsePassword(String[] args) {
        return MainArgs.parseArgument(args, "password", null, null);
    }

    /**
     * Parse user from arguments and override any existing user value from
     * reading serialized Project info.
     * 
     * @return final value of user.
     */
    static String parseUser(String[] args) {
        return MainArgs.parseArgument(args, "user", null, null);
    }

    /**
     * Retrieves the current version information, as indicated in the
     * version.properties file.
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
     */
    private static void printVersion(Properties props) {
        LOG.info("CruiseControl Version " + props.getProperty("version") + " " + props.getProperty("version.info"));
    }

    static boolean shouldPrintUsage(String[] args) {
        return MainArgs.findIndex(args, "?") != MainArgs.NOT_FOUND
                || MainArgs.findIndex(args, "help") != MainArgs.NOT_FOUND;
    }
}
