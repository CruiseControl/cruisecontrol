/******************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ******************************************************************************/
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.jmx.ProjectControllerAgent;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * Command line entry point.
 *
 * @author alden almagro, ThoughtWorks, Inc. 2002
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class Main {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(Main.class);

    /**
     * Print the version, configure the project with serialized build info
     * and/or arguments and start the project build process.
     */
    public static void main(String args[]) {
        Main main = new Main();
        main.printVersion();

        Project project = null;
        boolean startQueue = true;
        BuildQueue buildQueue = new BuildQueue(startQueue);
        try {
            project = main.configureProject(args);
            // Init the project once, to check the current config file is ok
            project.init();
            project.setBuildQueue(buildQueue);

            if (shouldStartProjectController(args)) {
                ProjectControllerAgent agent =
                    new ProjectControllerAgent(project, parsePort(args));
                agent.start();
            }
        }
        catch (CruiseControlException e) {
            log.fatal(e.getMessage());
            usage();
        }

        Thread projectSchedulingThread = new Thread(project, "Project "+project.getName()+" thread");
        projectSchedulingThread.start();
    }

    /**
     *  Displays the standard usage message and exit.
     */
    public static void usage() {
        log.info("Usage:");
        log.info("");
        log.info("Starts a continuous integration loop");
        log.info("");
        log.info("java CruiseControl [options]");
        log.info("where options are:");
        log.info("");
        log.info(
            "   -port number           where number is the port of the Controller web site");
        log.info(
            "   -projectname name      where name is the name of the project");
        log.info(
            "   -lastbuild timestamp   where timestamp is in yyyyMMddHHmmss format.  note HH is the 24 hour clock.");
        log.info(
            "   -label label           where label is in x.y format, y being an integer.  x can be any string.");
        log.info(
            "   -configfile file       where file is the configuration file");
        System.exit(1);
    }

    /**
     * Set Project attributes from previously serialized project if it exists
     * and then overrides attributes using command line arguments if they exist.
     *
     * @return configured Project; should never return null
     * @throws CruiseControlException
     */
    public Project configureProject(String args[])
        throws CruiseControlException {
        Project project = readProject(parseProjectName(args));

        project.setLastBuild(parseLastBuild(args, project.getLastBuild()));

        if (project.getLastSuccessfulBuild() == null) {
            project.setLastSuccessfulBuild(project.getLastBuild());
        }
        project.setLabel(parseLabel(args, project.getLabel()));
        project.setName(parseProjectName(args));
        project.setConfigFileName(
            parseConfigFileName(args, project.getConfigFileName()));

        return project;
    }

    /**
     * Parse lastbuild from arguments and override any existing lastbuild value
     * from reading serialized Project info.
     *
     * @param lastBuild existing lastbuild value read from serialized Project
     * info
     * @return final value of lastbuild; never null
     * @throws CruiseControlException if final lastbuild value is null
     */
    protected String parseLastBuild(String args[], String lastBuild)
        throws CruiseControlException {
        lastBuild = parseArgument(args, "lastbuild", lastBuild);
        if (lastBuild == null) {
            throw new CruiseControlException("'lastbuild' is a required argument to CruiseControl.");
        }
        return lastBuild;
    }

    /**
     * Parse label from arguments and override any existing lastbuild value
     * from reading serialized Project info.
     *
     * @param label existing label value read from serialized Project
     * info
     * @return final value of label; never null
     * @throws CruiseControlException if final label value is null
     */
    protected String parseLabel(String args[], String label)
        throws CruiseControlException {
        label = parseArgument(args, "label", label);
        if (label == null) {
            throw new CruiseControlException("'label' is a required argument to CruiseControl.");
        }
        return label;
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
    protected String parseConfigFileName(String args[], String configFileName)
        throws CruiseControlException {
        configFileName = parseArgument(args, "configfile", configFileName);
        if (configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    /**
     * Parse projectname from arguments.  projectname should always be specified
     * in arguments.
     *
     * @return project name; never null
     * @throws CruiseControlException if projectname is not specified
     */
    protected String parseProjectName(String args[])
        throws CruiseControlException {
        String projectName = parseArgument(args, "projectname", null);
        if (projectName == null) {
            throw new CruiseControlException("'projectname' is a required argument to CruiseControl.");
        }
        return projectName;
    }

    private static boolean shouldStartProjectController(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-port")) {
                log.debug(
                    "Main: -port parameter found. will start ProjectControllerAgent.");
                return true;
            }
        }
        log.debug(
            "Main: -port parameter not found. will not start ProjectControllerAgent.");
        return false;
    }

    /**
     * Parse port number from arguments.
     *
     * @return port number
     * @throws IllegalArgumentException if port argument is not specified
     *          or invalid
     */
    static int parsePort(String args[])
        throws IllegalArgumentException, CruiseControlException {

        String portString = parseArgument(args, "port", null);
        if (portString == null) {
            throw new IllegalStateException(
                "Should not reach this point "
                    + " without returning or throwing CruiseControlException");
        }
        int port;
        try {
            port = Integer.parseInt(portString);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "-port parameter, specified as '"
                    + portString
                    + "', requires integer argument");
        }
        return port;
    }

    /**
     * Reads project configuration from a previously serialized Project.  The
     * name of the serialized project file is equivalent to the name of the
     * project.
     *
     * @param fileName name of the serialized project file
     * @return Deserialized Project or a new Project if there are any problems
     * reading the serialized Project; should never return null
     */
    private Project readProject(String fileName) {
        File serializedProjectFile = new File(fileName);
        log.debug(
            "Reading serialized project from: "
                + serializedProjectFile.getAbsolutePath());
        if (!serializedProjectFile.exists()
            || !serializedProjectFile.canRead()) {
            log.warn(
                "No previously serialized project found: "
                    + serializedProjectFile.getAbsolutePath());
        }
        else {
            try {
                ObjectInputStream s =
                    new ObjectInputStream(
                        new FileInputStream(serializedProjectFile));
                Project project = (Project) s.readObject();
                return project;
            }
            catch (Exception e) {
                log.warn(
                    "Error deserializing project file from "
                        + serializedProjectFile.getAbsolutePath(),
                    e);
            }
        }

        return new Project();
    }

    /**
     * Writes the current version information, as indicated in the
     * version.properties file, to the logging information stream.
     */
    private void printVersion() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/version.properties"));
        }
        catch (IOException e) {
            log.error("Error reading version properties", e);
        }
        log.info("CruiseControl Version " + props.getProperty("version"));
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
     * @param defaultArgValue A default argument value, in case one was not
     *      specified.
     * @return The argument value found, or the default if none was found.
     * @throws CruiseControlException If the user specified the argument name
     *      but didn't include the argument, as in "-port" when it should most
     *      likely be "-port 8080".
     */
    public static String parseArgument(
        String[] args,
        String argName,
        String defaultArgValue)
        throws CruiseControlException {

        //Init to default value.
        String returnArgValue = defaultArgValue;

        for (int i = 0; i <= args.length - 1; i++) {
            if (args[i].equals("-" + argName)) {
                try {
                    returnArgValue = args[i + 1];
                    log.debug(
                        "Main: value of parameter "
                            + argName
                            + " is ["
                            + returnArgValue
                            + "]");
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException(
                        "'" + argName + "' argument was not specified.");
                }
            }
        }
        return returnArgValue;
    }
}
