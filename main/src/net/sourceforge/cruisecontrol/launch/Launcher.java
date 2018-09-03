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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.launch.util.Locator;

/**
 * Provides the means to launch CruiseControl with the appropriate classpath.
 * This code is based heavily on (some parts taken directly from) the Apache
 * Ant project.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com">Robert J. Smith</a>
 */
public class Launcher {

    /** The property containing the CruiseControl home directory.
     *  This is for backward compatibility of the individual modules. They should use "proj" instead
     *  of "home" (if they mean main CC projects directory. User's home can be obtained by
     *  {@link Configuration#USER_HOMEDIR})  */
    public static final String CCHOME_PROPERTY = "cc.home";

    /** The startup class that is to be run */
    public static final String MAIN_CLASS = "net.sourceforge.cruisecontrol.Main";

    /** Property name used by log4j to find its configuration source. */
    public static final String PROP_LOG4J_CONFIGURATION = "log4j.configuration";

    /** The location of a per-user library directory */
    public static final File USER_LIBDIR = new File(new File(Configuration.USER_HOMEDIR, ".cruisecontrol"), "lib");

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
        final Configuration config = Configuration.getInstance(args);
        final File sourceJar = getClassSource();

        // Make notice to log4j where is configuration file is
        if (config.wasOptionSet(Configuration.KEY_LOG4J_CONFIG)) {
            final URL log4jcofig = config.getOptionUrl(Configuration.KEY_LOG4J_CONFIG);
            System.setProperty(PROP_LOG4J_CONFIGURATION, log4jcofig.toString());
        }

        // Determine the CruiseControl directory for the distribution jars if it was provided,
        // Otherwise make a guess based upon the location of the launcher jar.
        final File ccDistDir = getCCDistDir(config);
        final File ccProjDir = getCCProjDir(config);
        // The property is required by other modules. It would be better to use Configuration
        // directly ... Moreover, the "cc.proj" should be used instead!
        System.setProperty(CCHOME_PROPERTY, ccProjDir.getAbsolutePath());

        // Determine CruiseControl library directory for third party jars, if it was provided.
        // Otherwise make a guess based upon the CruiseControl home dir we found earlier.
        final File[] ccLibDirs = config.getOptionDirArray(Configuration.KEY_LIBRARY_DIRS, ccDistDir);
        final URL[] distJars = collectJars(ccDistDir, config.getLogger());
        final URL[] libJars = collectJars(ccLibDirs, config.getLogger());
        final URL[] antJars = collectAntLibs(ccLibDirs, config.getLogger());

        // Locate any jars in the per-user lib directory
        final boolean noUserLib = config.getOptionBool(Configuration.KEY_NO_USER_LIB);
        final URL[] userJars2 = noUserLib ? new URL[0] : collectJars(USER_LIBDIR, config.getLogger());
        // Process the user lib dir directories found on the command line
        final File[] userDir1 = noUserLib ? new File[0]
                : config.getOptionDirArray(Configuration.KEY_USER_LIB_DIRS, ccProjDir);
        final URL[] userJars1 = collectJars(userDir1, config.getLogger());
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
        config.getLogger().info("Classpath: " + baseClassPath.toString());

        // Create a new class loader which has access to our jars
        final URLClassLoader loader = new URLClassLoader(jars.toArray(new URL[jars.size()]));
        Thread.currentThread().setContextClassLoader(loader);

        // Launch CruiseControl!
        try {
            final Class mainClass = loader.loadClass(MAIN_CLASS);
            final CruiseControlMain main = (CruiseControlMain) mainClass.newInstance();
            final boolean normalExit = main.start(config);
            if (!normalExit) {
                exitWithErrorCode();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** @return the path to Jar (or directory) where Launcher.class file is located */
    File getClassSource() {
        return Locator.getClassSource(Launcher.class);
    }


    /**
     * When called, invokes System.exit(1). The method is protected to be overridden in tests.
     */
    protected void exitWithErrorCode() {
       System.exit(1);
    }

    /** Exception message if CC Home directory couldn't be determined. */
    static final String MSG_BAD_CCPROJ = "CruiseControl project is not set or could not be located.";
    /** Exception message if CC Home directory couldn't be determined. */
    static final String MSG_BAD_CCDIST = "CruiseControl dist is not set or could not be located.";

    /**
     * Determine and return the CC dist directory. It is either the value set explicitly byHOME
     * {@link Configuration.KEY_DIST_DIR} option or guessed as the ../../{@link #getClassSource()} path.
     * @return CruiseControl dist directory
     * @throws LaunchException if CruiseControl dist is not set or could not be located.
     * @see Configuration#getDistDir(File)
     */
    File getCCDistDir(Configuration conf) throws LaunchException {
        File dist;

        // Try if set
        dist = conf.getDistDir(null);
        if (dist != null) {
            return dist;
        }

        // If the location was not specified, or it does not exist, try to guess
        // the location based upon the location of the launcher Jar.
        File classSource = getClassSource();
        conf.getLogger().warn("Trying to guess '" + Configuration.KEY_DIST_DIR + "' from '"
                + classSource.getAbsolutePath());

        dist = conf.getDistDir(classSource);
        if (dist != null) {
            return dist;
        }
        // If none of the above worked, give up.
        throw new LaunchException(MSG_BAD_CCDIST);
    }
    /**
     * Determine and return the CC Home directory, and reset the {@link #CCHOME_PROPERTY} to match if needed.
     * @return CruiseControl home directory
     * @throws LaunchException if CruiseControl home is not set or could not be located.
     * @see Configuration#getProjDir(File)
     */
    File getCCProjDir(Configuration conf) throws LaunchException {
        File proj;

        // Try if set
        proj = conf.getProjDir(null);
        if (proj != null) {
            return proj;
        }

        // If the location was not specified, or it does not exist, try to guess
        // the location based upon the location of the working directory.
        File workdir = new File(".");
        conf.getLogger().warn("Trying to guess '" + Configuration.KEY_PROJ_DIR + "' from working dir '"
                + workdir.getAbsolutePath() + "'");

        proj = conf.getProjDir(workdir);
        if (proj != null) {
            return proj;
        }
        // If none of the above worked, give up.
        throw new LaunchException(MSG_BAD_CCPROJ);
    }

    /**
     * Add a CLASSPATH or -lib to lib path urls.
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     * @throws MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
   private void addPath(final String path, final boolean getJars, final List<URL> libPathURLs)
       throws MalformedURLException {

       final StringTokenizer myTokenizer
           = new StringTokenizer(path, System.getProperty("path.separator"));
       while (myTokenizer.hasMoreElements()) {
           final String elementName = myTokenizer.nextToken();
           final File element = new File(elementName);
           if (elementName.indexOf("%") != -1 && !element.exists()) {
               continue;
           }
           if (getJars && element.isDirectory()) {
               // add any jars in the directory
               final URL[] dirURLs = Locator.getLocationURLs(element);
               libPathURLs.addAll(Arrays.asList(dirURLs));
           }

           libPathURLs.add(element.toURI().toURL());
       }
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
}
