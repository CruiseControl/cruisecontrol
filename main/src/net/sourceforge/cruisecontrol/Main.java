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

import net.sourceforge.cruisecontrol.jmx.CruiseControlControllerAgent;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Command line entry point.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2002
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class);
    public static final int NOT_FOUND = -1;
    private static final String NOT_FOUND_STRING = String.valueOf(NOT_FOUND);

    /**
     * Print the version, configure the project with serialized build info
     * and/or arguments and start the project build process.
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.printVersion();

        if (printUsage(args)) {
            usage();
        }
        try {
            CruiseControlController controller = new CruiseControlController();
            String configFileName = parseConfigFileName(args, CruiseControlController.DEFAULT_CONFIG_FILE_NAME);
            controller.setConfigFile(new File(configFileName));

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
        LOG.info("where options are:");
        LOG.info("");
        LOG.info("   -port number           where number is the port of the Controller web site");
        LOG.info("   -rmiport number        where number is the RMI port of the Controller");
        LOG.info("   -xslpath directory     where directory is location of jmx xsl files");
        LOG.info("   -configfile file       where file is the configuration file");
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
        configFileName = parseArgument(args, "configfile", configFileName);
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
     * @throws IllegalArgumentException if port argument is not specified
     *          or invalid
     */
    static int parseHttpPort(String[] args) throws CruiseControlException {
        return parseInt(args, "port", NOT_FOUND_STRING);
    }

    static int parseRmiPort(String[] args) throws CruiseControlException {
        return parseInt(args, "rmiport", NOT_FOUND_STRING);
    }

    private static int parseInt(String[] args, String argName, String defaultValue) throws CruiseControlException {
        String intString = parseArgument(args, argName, defaultValue);
        if (intString == null) {
            throw new IllegalStateException("Should not reach this point");
        }
        int intValue;
        try {
            intValue = Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "-" + argName + " parameter, specified as '" + intString + "', requires integer argument");
        }
        return intValue;
    }

    static String parseXslPath(String[] args) throws CruiseControlException {
        String xslpath = parseArgument(args, "xslpath", null);
        if (xslpath != null) {
            File directory = new File(xslpath);
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException("'xslpath' argument must specify an existing directory.");
            }
        }
        return xslpath;
    }

    /**
     * Writes the current version information, as indicated in the
     * version.properties file, to the logging information stream.
     */
    private void printVersion() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/version.properties"));
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
     * @param defaultValue A default argument value, in case one was not
     *      specified.
     * @return The argument value found, or the default if none was found.
     * @throws CruiseControlException If the user specified the argument name
     *      but didn't include the argument, as in "-port" when it should most
     *      likely be "-port 8080".
     */
    public static String parseArgument(String[] args, String argName, String defaultValue)
            throws CruiseControlException {
        String returnArgValue = defaultValue;
        int argIndex = findIndex(args, argName);
        if (argIndex != NOT_FOUND) {
            try {
                returnArgValue = args[argIndex + 1];
                LOG.debug("Main: value of parameter " + argName + " is [" + returnArgValue + "]");
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CruiseControlException("'" + argName + "' argument was not specified.");
            }
        }
        return returnArgValue;
    }

    static int findIndex(String[] args, String argName) {
        for (int i = 0; i <= args.length - 1; i++) {
            if (args[i].equals("-" + argName)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    static boolean printUsage(String[] args) {
        return findIndex(args, "?") != NOT_FOUND;
    }
}
