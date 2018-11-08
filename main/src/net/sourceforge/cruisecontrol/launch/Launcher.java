/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
/*
 * Copyright  2003-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.cruisecontrol.launch.util.Locator;

/**
 * Provides the means to launch CruiseControl with the appropriate classpath.
 * This code is based heavily on (some parts taken directly from) the Apache
 * Ant project.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class Launcher {

    /** The startup class that is to be run */
    public static final String MAIN_CLASS = "net.sourceforge.cruisecontrol.Main";

    /** Property name used by log4j to find its configuration source. */
    public static final String PROP_LOG4J_CONFIGURATION = "log4j.configuration";

    /** The location of a per-user library directory */
    public static final File USER_LIBDIR = new File(new File(LaunchOptions.USER_HOMEDIR, ".cruisecontrol"),
            "lib");

    /**
     *  Entry point for starting CruiseControl from the command line
     *
     * @param  args commandline arguments
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (LaunchException e) {
            System.err.println(e.getMessage());
            printHelp();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Run the launcher
     *
     * @param args the command line arguments
     *
     * @throws LaunchException if CruiseControl home is not set or could not be located, or if other
     *            invalid argument values are given.
     * @throws MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
    void run(final String[] args) throws LaunchException, MalformedURLException {

        // First of all read the configuration
        final LogInterface firstLogger = new LogBuffer();
        final LaunchOptions config = new LaunchOptions(args, firstLogger, this);
        // Determine the CruiseControl directory for the distribution jars if it was provided,
        // Otherwise make a guess based upon the location of the launcher jar.
        // These are the most important directories the other config options may point to. So resolve
        // them as the very first
        final File ccDistDir = getCCDistDir(config, firstLogger);
        final File ccProjDir = getCCProjDir(config, firstLogger);

        // Make notice to log4j where is configuration file is
        if (config.wasOptionSet(LaunchOptions.KEY_LOG4J_CONFIG)) {
            final URL log4jcofig = config.getOptionUrl(LaunchOptions.KEY_LOG4J_CONFIG);
            System.setProperty(PROP_LOG4J_CONFIGURATION, log4jcofig.toString());
        }

        // Determine CruiseControl library directory for third party jars, if it was provided.
        // Otherwise make a guess based upon the CruiseControl home dir we found earlier.
        final File[] ccLibDirs = config.getOptionDirArray(LaunchOptions.KEY_LIBRARY_DIRS, ccDistDir);
        final URL[] distJars = collectJars(ccDistDir, firstLogger);
        final URL[] libJars = collectJars(ccLibDirs, firstLogger);
        final URL[] antJars = collectAntLibs(ccLibDirs, firstLogger);

        // Locate any jars in the per-user lib directory
        final boolean noUserLib = config.getOptionBool(LaunchOptions.KEY_NO_USER_LIB);
        final URL[] userJars2 = noUserLib ? new URL[0] : collectJars(USER_LIBDIR, firstLogger);
        // Process the user lib dir directories found on the command line
        final File[] userDir1 = noUserLib ? new File[0]
                : config.getOptionDirArray(LaunchOptions.KEY_USER_LIB_DIRS, ccProjDir);
        final URL[] userJars1 = collectJars(userDir1, firstLogger);
        // Locate the Java tools jar
        final File toolsJar = Locator.getToolsJar();

        // Concatenate our jar lists - order of precedence will be those jars
        // specified on the command line followed by jars in the per-user
        // lib directory and finally those jars found in the dist and lib
        // folders of CruiseControl home.

        final List<URL> jars = new ArrayList<URL>();
        Collections.addAll(jars, userJars1);
        Collections.addAll(jars, userJars2);
        Collections.addAll(jars, distJars);
        Collections.addAll(jars, libJars);
        Collections.addAll(jars, antJars);

        if (toolsJar != null) {
            jars.add(toolsJar.toURI().toURL());
        }

        // Update the JVM java.class.path property
        final StringBuffer baseClassPath
            = new StringBuffer(System.getProperty("java.class.path"));
        if (baseClassPath.charAt(baseClassPath.length() - 1) == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }
        for (final URL jar : jars) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jar.toString()));
        }
        baseClassPath.append(File.pathSeparatorChar);
        baseClassPath.append(".");

        // adding the homedirectory to the classpath
        baseClassPath.append(File.pathSeparatorChar);
        baseClassPath.append(ccProjDir.getAbsolutePath()).append(File.separatorChar);

        System.setProperty("java.class.path", baseClassPath.toString());
        firstLogger.info("Classpath: " + baseClassPath.toString());

        // Create a new class loader which has access to our jars
        final URLClassLoader loader = new URLClassLoader(jars.toArray(new URL[jars.size()]));
        Thread.currentThread().setContextClassLoader(loader);

        // If there is --help option set, print the help (prior to the help of the main CC process
        if (config.wasOptionSet("help") || config.wasOptionSet("h") || config.wasOptionSet("?")) {
            printHelp();
        }

        // Launch CruiseControl!
        try {
            final Class< ? > mainClass = loader.loadClass(MAIN_CLASS);
            final CruiseControlMain main = (CruiseControlMain) mainClass.newInstance();

            // Pass the config to main CC process
            final Object confOwner = newConfOwner();
            final Options confMain = main.confFactory(confOwner);

            for (final String key : config.allOptionKeys()) {
                if (confMain.knowsOption(key)) {
                    confMain.setOption(key, config.getOptionRaw(key), confOwner);
                }
            }

            // Start the CC process
            final boolean normalExit = main.start();
            if (!normalExit) {
                exitWithErrorCode();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            exitWithErrorCode();
        }
    }

    /** @return the path to Jar (or directory) where Launcher.class file is located */
    static File getClassSource() {
        return Locator.getClassSource(Launcher.class);
    }


    /**
     * When called, invokes System.exit(1). The method is protected to be overridden in tests.
     */
    protected void exitWithErrorCode() {
       System.exit(1);
    }
    /**
     * Each call gets the new instance of {@link Object} to be passed to {@link CruiseControlMain#confFactory(Object)}.
     * The method is mainly for test purposes;
     * @return new instance of {@link Object}
     */
    protected Object newConfOwner() {
        return new Object();
    }

    /** Exception message if CC Home directory couldn't be determined. */
    static final String MSG_BAD_CCPROJ = "CruiseControl project is not set or could not be located.";
    /** Exception message if CC Home directory couldn't be determined. */
    static final String MSG_BAD_CCDIST = "CruiseControl dist is not set or could not be located.";

    /**
     * Determine and return the CC dist directory. It is either the value set explicitly by
     * {@link LaunchOptions#KEY_DIST_DIR} option or guessed as the ../../{@link #getClassSource()} path.
     * @param config the configuration holder
     * @param log the logger
     * @return CruiseControl dist directory
     * @throws LaunchException if CruiseControl dist is not set or could not be located.
     */
    File getCCDistDir(LaunchOptions config, LogInterface log) throws LaunchException {
        // Check, if the directory was defined in a configuration
        if (config.wasOptionSet(LaunchOptions.KEY_DIST_DIR)) {
            try {
                return config.getOptionDir(LaunchOptions.KEY_DIST_DIR);
            } catch (IllegalArgumentException e) {
                throw new LaunchException(MSG_BAD_CCDIST);
            }
        }

        // If the location was not specified, or it does not exist, try to guess
        // the location based upon the location of the launcher Jar.
        File classSource = getClassSource();
        log.warn("Trying to guess '" + LaunchOptions.KEY_DIST_DIR + "' from '" + classSource.getAbsolutePath());

        // If the location was not specified, or it does not exist, try to guess
        // the location based upon the location of the launcher Jar.
        if (classSource == null || !classSource.exists()) {
            return null;
        }
        // Parent dir of classSource
        if (classSource.getParentFile() != null) {
            classSource = classSource.getParentFile();
        }
        // Parent dir of lib/
        if (classSource.getParentFile() != null) {
            classSource = classSource.getParentFile();
            // Store it and return
            config.setOption(LaunchOptions.KEY_DIST_DIR, classSource.getAbsolutePath(), this);
            return classSource;
        }
        // If none of the above worked, give up.
        throw new LaunchException(MSG_BAD_CCDIST);
    }
    /**
     * Determine and return the CC Home directory.
     * @param config the configuration holder
     * @param log the logger
     * @return CruiseControl home directory
     * @throws LaunchException if CruiseControl home is not set or could not be located.
     */
    File getCCProjDir(LaunchOptions config, LogInterface log) throws LaunchException {
        // Check, if the directory was defined in a configuration
        if (config.wasOptionSet(LaunchOptions.KEY_PROJ_DIR)) {
            try {
                return config.getOptionDir(LaunchOptions.KEY_PROJ_DIR);
            } catch (IllegalArgumentException e) {
                throw new LaunchException(MSG_BAD_CCPROJ);
            }
        }
        // If the location was not specified, or it does not exist, try to guess
        // the location based upon the location of the working directory.
        File workdir = new File(".");
        log.warn("Trying to guess '" + LaunchOptions.KEY_PROJ_DIR + "' from working dir '"
                + workdir.getAbsolutePath() + "'");

        if (workdir == null || !workdir.exists()) {
            throw new LaunchException(MSG_BAD_CCPROJ);
        }
        try {
            workdir =  config.getOptionDir(LaunchOptions.KEY_PROJ_DIR, workdir);
        } catch (IllegalArgumentException e) {
            workdir = workdir.getAbsoluteFile();
        }
        // Store it and return
        config.setOption(LaunchOptions.KEY_PROJ_DIR, workdir.getAbsolutePath(), this);
        return workdir;

    }

   /**
    * Collects JAR files within the given list of directories
    * @param dir the directory to search
    * @param log the logger to log errors through
    * @return list of {@link URL} classes with the <code>.jar</code> files found
    */
   private URL[] collectJars(final File dir, final LogInterface log) {
       try {
           return Locator.getLocationURLs(dir);
       } catch (MalformedURLException e) {
           log.error("Failed .jar collection in " + dir.getAbsolutePath() + ": " + e.getMessage());
           return new URL[0];
       }
   }
   /**
    * Collects JAR files within the given list of directories
    * @param dirs the list of directories to search
    * @param log the logger to log errors through
    * @return list of {@link URL} classes with the <code>.jar</code> files found
    */
   private URL[] collectJars(final File[] dirs, final LogInterface log) {
       Set<URL> urls = new HashSet<URL>(dirs.length * 5);

       for (File d : dirs) {
            Collections.addAll(urls, collectJars(d, log));
       }
       return urls.toArray(new URL[urls.size()]);
   }
   /**
    * Collects JAR files of the ant project. It searches all the directories for the <code>ant/</code>
    * subdirectory from where the <code>.jar</code> files are read
    * @param dirs the list of directories to search
    * @param log the logger to log errors through
    * @return list of {@link URL} classes with the <code>ant/*.jar</code> files found
    * @throws LaunchException
    */
   private URL[] collectAntLibs(final File[] dirs, final LogInterface log) throws LaunchException {
       Set<URL> urls = new HashSet<URL>(dirs.length * 5);

       for (File d : dirs) {
            if ("ant".equals(d.getName())) {
                d = new File(d, "ant");
            }
            if (!d.exists() || !d.isDirectory()) {
                continue;
            }

            Collections.addAll(urls, collectJars(d, log));
       }
       return urls.toArray(new URL[urls.size()]);
   }

   private static void printHelp() {
       System.err.println("");
       System.err.println("CruiseControl launch options. The preference orderign is (lowest to highest)");
       System.err.println(" - config XML file, as set by ${" + LaunchOptions.KEY_CONFIG_FILE + "} option");
       System.err.println(" - system properties, prefixed by cc.");
       System.err.println(" - command line options");
       System.err.println("");
       System.out.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_CONFIG_FILE));
       System.out.println("       ... the path to the launch configuration XML. It is an XML file with");
       System.out.println("           either <launch> root element or <cruisecontrol><launch> sub-node, under ");
       System.out.println("           which all the following options can be placed as embedded XML nodes.");
       System.out.println("           When launcher configuration is set in its own file with <launch> root,");
       System.out.println("           it must contain <" + LaunchOptions.KEY_CONFIG_FILE + "> element");
       System.out.println("           with the path to an external main cruisecontrol configuration file.");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_DIST_DIR));
       System.err.println("       ... the path to the directory where the CruiseControl was installed. If not");
       System.err.println("           set, parent to the " + getClassSource().getAbsolutePath());
       System.err.println("           directory is used (i.e. /foo/bar/ for /foo/bar/lib/cruisecontrol-launch.jar).");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_PROJ_DIR));
       System.err.println("       ... the path to the directory where the CruiseControl projects are located.");
       System.err.println("           If not set, working directory is used.");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_LIBRARY_DIRS));
       System.err.println("       ... the path to the directories where the CC libraries are located;");
       System.err.println("           the option may repeat, on each occurence a single directory is added to");
       System.err.println("           the list. Relative paths use ${" + LaunchOptions.KEY_DIST_DIR + "} parent"
                                   + "directory.");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_USER_LIB_DIRS));
       System.err.println("       ... the path to the directories where additional libraries are located;");
       System.err.println("           the option may repeat, on each occurrence a single directory is added to");
       System.err.println("           the list. Relative paths use ${" + LaunchOptions.KEY_PROJ_DIR + "} parent "
                                   + "directory.");
       System.err.println("           In addition, " + USER_LIBDIR.getAbsolutePath() + " is always added to the");
       System.err.println("           list, unless ${" + LaunchOptions.KEY_NO_USER_LIB + "} is set.");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_NO_USER_LIB));
       System.err.println("       ... when set, no user library is used.");
       System.err.println(LaunchOptions.getBaseHelp(LaunchOptions.KEY_LOG4J_CONFIG));
       System.err.println("       ... the URL pointing to the file with log4j cpnfiguration.");

       System.err.println("");
   }
}
