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
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueueProperties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Command line entry point.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2002
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class);
    public static final int NOT_FOUND = -1;

    private Main() { }
    
    /**
     * Print the version, configure the project with serialized build info
     * and/or arguments and start the project build process.
     */
    public static void main(String[] args) {
        printVersion();
        if (printUsage(args)) {
            usage();
        }
        try {
            if (findIndex(args, "debug") != NOT_FOUND) {
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }
            CruiseControlController controller = new CruiseControlController();
            File configFile = new File(parseConfigFileName(args, CruiseControlController.DEFAULT_CONFIG_FILE_NAME));
            controller.setConfigFile(configFile);
            ServerXMLHelper helper = new ServerXMLHelper(configFile);
            ThreadQueueProperties.setMaxThreadCount(helper.getNumThreads());

            if (shouldStartController(args)) {
                CruiseControlControllerAgent agent
                        = new CruiseControlControllerAgent(controller,
                                                           parseHttpPort(args),
                                                           parseRmiPort(args),
                                                           parseXslPath(args));
                agent.start();
            }
            controller.resume();
        } catch (CruiseControlException e) {
            LOG.fatal(e.getMessage());
            usage();
        }
    }

    /**
     *  Displays the standard usage message and exit.
     */
    public static void usage() {
        LOG.info("Usage:");
        LOG.info("");
        LOG.info("Starts a continuous integration loop");
        LOG.info("");
        LOG.info("java CruiseControl [options]");
        LOG.info("where options (all optional) are:");
        LOG.info("");
        LOG.info("  -port [number]       where number is the port of the Controller web site; defaults to 8000");
        LOG.info("  -rmiport [number]    where number is the RMI port of the Controller; defaults to 1099");
        LOG.info("  -xslpath directory   where directory is location of jmx xsl files;"
                 + " defaults to files in package");
        LOG.info("  -configfile file     where file is the configuration file;"
                 + " defaults to config.xml in the current directory");
        LOG.info("  -debug               to set the internal logging level to DEBUG");
        LOG.info("");
        LOG.info("Please keep in mind that the JMX server will only be started "
                 + "if you specify -port and/or -rmiport");
        System.exit(1);
    }

    /**
     * Parse configfile from arguments and override any existing configfile value
     * from reading serialized Project info.
     *
     * @param configFileName existing configfile value read from serialized Project
     * info
     * @return final value of configFileName; never null
     * @throws CruiseControlException if final configfile value is null
     */
    static String parseConfigFileName(String[] args, String configFileName)
        throws CruiseControlException {
        configFileName = parseArgument(args, "configfile", configFileName, null);
        if (configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    static boolean shouldStartController(String[] args) {
        return findIndex(args, "port") != NOT_FOUND || findIndex(args, "rmiport") != NOT_FOUND;
    }

    /**
     * Parse port number from arguments.
     *
     * @return port number
     * @throws IllegalArgumentException if port argument is invalid
     */
    static int parseHttpPort(String[] args) {
        return parseInt(args, "port", NOT_FOUND, 8000);
    }

    static int parseRmiPort(String[] args) {
        return parseInt(args, "rmiport", NOT_FOUND, 1099);
    }

    private static int parseInt(String[] args, String argName, int defaultIfNoParam, int defaultIfNoValue) {
        String intString = parseArgument(args, 
                                         argName, 
                                         Integer.toString(defaultIfNoParam), 
                                         Integer.toString(defaultIfNoValue));
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "-" + argName + " parameter, specified as '" + intString + "', requires integer argument");
        }
    }

    static String parseXslPath(String[] args) {
        String xslpath = parseArgument(args, "xslpath", null, null);
        if (xslpath != null) {
            File directory = new File(xslpath);
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException(
                        "'xslpath' argument must specify an existing directory but was " + xslpath);
            }
        }
        return xslpath;
    }

    /**
     * Writes the current version information, as indicated in the
     * version.properties file, to the logging information stream.
     */
    private static void printVersion() {
        Properties props = new Properties();
        try {
            props.load(Main.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            LOG.error("Error reading version properties", e);
        }
        LOG.info("CruiseControl Version " + props.getProperty("version"));
    }

    /**
     * Searches the array of args for the value corresponding to a particular
     * argument name. This method assumes that the argName doesn't include
     * a "-", but adds one while looking through the array. For example, if a
     * user is supposed to type "-port", the appropriate argName to supply to
     * this method is just "port".
     *
     * This method also allows the specification
     * of a default argument value, in case one was not specified.
     *
     * @param args Application arguments like those specified to the standard
     *      Java main function.
     * @param argName Name of the argument, without any preceeding "-",
     *      i.e. "port" not "-port".
     * @param defaultIfNoParam A default argument value, 
     *      in case the parameter argName was not specified
     * @param defaultIfNoValue A default argument value, 
     *      in case the parameter argName was specified without a value
     * @return The argument value found, or the default if none was found.
     */
    static String parseArgument(String[] args, String argName, String defaultIfNoParam, String defaultIfNoValue) {
        int argIndex = findIndex(args, argName);
        if (argIndex == NOT_FOUND) {
            return defaultIfNoParam;
        }
        // check to see if the user supplied a value for the parameter;
        // if not, return the supplied default
        if (argIndex == args.length - 1            // last arg
            || args[argIndex + 1].charAt(0) == '-' // start of new param
        ) {
            return defaultIfNoValue;
        }
        return args[argIndex + 1];
    }

    static int findIndex(String[] args, String argName) {
        
        String searchString = "-" + argName;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(searchString)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    static boolean printUsage(String[] args) {
        return findIndex(args, "?") != NOT_FOUND;
    }
}
