/********************************************************************************
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import org.apache.log4j.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 *  Command line entry point.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class Main {

    /** enable logging for this class */
    private static Category log = Category.getInstance(Main.class.getName());

    public static void main(String args[]) {
        Project project = new Project();
        Main main = new Main();
        try {
            project = main.configureProject(args);
        } catch (CruiseControlException e) {
            log.fatal(e.getMessage());
            usage();
        }
        project.execute();
    }

    public Project configureProject(String args[]) throws CruiseControlException {
        Project project = null;
        project = readProject(parseProjectName(args));
        project.setLastBuild(parseLastBuild(args, project.getLastBuild()));
        project.setLabel(parseLabel(args, project.getLabel()));
        project.setName(parseProjectName(args));
        project.setConfigFileName(parseConfigFileName(args, project.getConfigFileName()));
        return project;
    }

    /**
     *  see if there's a serialized project already here
     */
    public Project readProject(String fileName) {
        File serializedProjectFile = new File(fileName);
        log.debug("Reading serialized project from: " + serializedProjectFile.getAbsolutePath());
        if (!serializedProjectFile.exists() || !serializedProjectFile.canRead()) {
            log.error("Cannot read serialized project: " + serializedProjectFile.getAbsolutePath());
        } else {
            try {
                ObjectInputStream s = new ObjectInputStream(new FileInputStream(serializedProjectFile));
                Project project = (Project) s.readObject();
                return project;
            } catch (Exception e) {
                log.error("Error deserializing project.", e);
            }
        }
        return new Project();
    }

    /**
     *  required if not in a serialized project
     */
    public String parseLastBuild(String args[], String lastBuild) throws CruiseControlException {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-lastbuild")) {
                try {
                    lastBuild = args[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException("'lastbuild' argument was not specified.");
                }
            }
        }

        if(lastBuild == null) {
            throw new CruiseControlException("'lastbuild' is a required argument to CruiseControl.");
        }
        return lastBuild;
    }

    /**
     *  required if not in a serialized project
     */
    public String parseLabel(String args[], String label) throws CruiseControlException {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-label")) {
                try {
                    label = args[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException("'label' argument was not specified.");
                }
            }
        }
        if(label == null) {
            throw new CruiseControlException("'label' is a required argument to CruiseControl.");
        }
        return label;
    }

    /**
     *  required if not in a serialized project
     */
    public String parseConfigFileName(String args[], String configFileName) throws CruiseControlException {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-configfile")) {
                try {
                    configFileName = args[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException("'configfile' argument was not specified.");
                }
            }
        }

        if(configFileName == null) {
            throw new CruiseControlException("'configfile' is a required argument to CruiseControl.");
        }
        return configFileName;
    }

    /**
     *  always required
     */
    public String parseProjectName(String args[]) throws CruiseControlException {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-projectname")) {
                try {
                    return args[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CruiseControlException("'projectname' argument was not specified.");
                }
            }
        }
        throw new CruiseControlException("'projectname' is a required argument to CruiseControl.");
    }

    /**
     *  Display the standard usage message and exit.
     */
    public static void usage() {
        log.info("Usage:");
        log.info("");
        log.info("Starts a continuous integration loop");
        log.info("");
        log.info("java CruiseControl [options]");
        log.info("where options are:");
        log.info("");
        log.info("   -lastbuild timestamp   where timestamp is in yyyyMMddHHmmss format.  note HH is the 24 hour clock.");
        log.info("   -label label           where label is in x.y format, y being an integer.  x can be any string.");
        log.info("   -configfile file       where file is the configuration file");
        log.info("   -projectname name      where name is the name of the project");
        System.exit(1);
    }
}