/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.launch.util.Locator;

/**
 * Provides the means to launch CruiseControl with the appropriate classpath.
 * This code is based heavily on (some parts taken directly from) the Apache 
 * Ant project.
 * 
 * @author <a href="mailto:rjmpsmith@gmail.com>Robert J. Smith</a>
 */
public class Launcher {
    
    /** The property containing the CruiseControl home directory */
    public static final String CCHOME_PROPERTY = "cc.home";

    /** The property containing the CruiseControl dist directory */
   public static final String CCDISTDIR_PROPERTY = "cc.dist.dir";

    /** The property containing the CruiseControl library directory */
    public static final String CCLIBDIR_PROPERTY = "cc.library.dir";

    /** The directory name of the per-user CC directory */
    public static final String CC_PRIVATEDIR = ".cruisecontrol";

    /** The location of a per-user library directory */
    public static final String CC_PRIVATELIB = "lib";

    /** The location of a per-user library directory */
    public static final String USER_LIBDIR = CC_PRIVATEDIR + File.separator
            + CC_PRIVATELIB;

    /** system property with user home directory */
    public static final String USER_HOMEDIR = "user.home";

    /** The startup class that is to be run */
    public static final String MAIN_CLASS = "net.sourceforge.cruisecontrol.Main";

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
     * @exception MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
    private void run(String[] args) throws LaunchException, MalformedURLException {

        // If the CruiseControl home dir was provided as a system property,
        // create a reference to it
        File ccHome = null;
        String ccHomeProperty = System.getProperty(CCHOME_PROPERTY);
        if (ccHomeProperty != null) {
            ccHome = new File(ccHomeProperty);
        }
        
        // If the location was not specifed, or it does not exist, try to guess
        // the location based upon the location of the launcher Jar.
        File sourceJar = Locator.getClassSource(this.getClass());
        File distJarDir = sourceJar.getParentFile();
        if (ccHome == null || !ccHome.exists()) {
            ccHome = distJarDir.getParentFile();
            System.setProperty(CCHOME_PROPERTY, ccHome.getAbsolutePath());
        }
        
        // If none of the above worked, give up now.
        if (!ccHome.exists()) {
            throw new LaunchException(
                    "CruiseControl home is not set or could not be located.");
        }

        // Process the command line arguments. We will handle the classpath 
        // related switches ourself. All other arguments will be repackaged 
        // and passed on the the Main class for processing.
        List libPaths = new ArrayList();
        List argList = new ArrayList();
        boolean  noUserLib = false;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-lib")) {
                if (i == args.length - 1) {
                    throw new LaunchException("The -lib argument must "
                        + "be followed by a library location");
                }
                libPaths.add(args[++i]);
            } else if (args[i].equals("--nouserlib") || args[i].equals("-nouserlib")) {
                noUserLib = true;
            } else {
                argList.add(args[i]);
            }
        }

        // Process the lib dir entries found on the command line
        List libPathURLs = new ArrayList();
        for (Iterator i = libPaths.iterator(); i.hasNext();) {
            String libPath = (String) i.next();
            addPath(libPath, true, libPathURLs);
        }
        URL[] libJars = (URL[]) libPathURLs.toArray(new URL[0]);

        // Determine the CruiseControl directory for the distribution jars.
        // Use the system property if it was provided, otherwise make a guess 
        // based upon the location of the launcher jar.
        File ccDistDir = null;
        String ccDistDirProperty = System.getProperty(CCDISTDIR_PROPERTY);
        if (ccDistDirProperty != null) {
            ccDistDir = new File(ccDistDirProperty);
        }
        if ((ccDistDir == null) || !ccDistDir.exists()) {
            ccDistDir = distJarDir;
            System.setProperty(CCDISTDIR_PROPERTY, ccDistDir.getAbsolutePath());
        }
        URL[] distJars = Locator.getLocationURLs(ccDistDir);
        
        // Determine CruiseControl library directory for third party jars. 
        // Use the system property if it was provided, otherwise make a guess 
        // based upon the CruiseControl home dir we found earlier.
        File ccLibDir = null;
        String ccLibDirProperty = System.getProperty(CCLIBDIR_PROPERTY);
        if (ccLibDirProperty != null) {
            ccLibDir = new File(ccLibDirProperty);
        }
        if ((ccLibDir == null) || !ccLibDir.exists()) {
            ccLibDir = new File(ccHome, "lib");
            System.setProperty(CCLIBDIR_PROPERTY, ccLibDir.getAbsolutePath());
        }
        URL[] supportJars = Locator.getLocationURLs(ccLibDir);

        // Locate any jars in the per-user lib directory
        File userLibDir = new File(System.getProperty(USER_HOMEDIR),
                USER_LIBDIR);
        URL[] userJars = noUserLib ? new URL[0] : Locator
                .getLocationURLs(userLibDir);

        // Locate the Java tools jar
        File toolsJar = Locator.getToolsJar();

        // Concatenate our jar lists - order of precedence will be those jars
        // specified on the command line followed by jars in the per-user
        // lib directory and finally those jars found in the dist and lib
        // folders of CruiseControl home.
        int numJars = libJars.length + userJars.length + distJars.length
                + supportJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        URL[] jars = new URL[numJars];
        System.arraycopy(libJars, 0, jars, 0, libJars.length);
        System.arraycopy(userJars, 0, jars, libJars.length, userJars.length);
        System.arraycopy(distJars, 0, jars, userJars.length + libJars.length,
                distJars.length);
        System.arraycopy(supportJars, 0, jars, userJars.length + libJars.length
                + distJars.length, supportJars.length);
        if (toolsJar != null) {
            jars[jars.length - 1] = toolsJar.toURL();
        }

        // Update the JVM java.class.path property
        StringBuffer baseClassPath
            = new StringBuffer(System.getProperty("java.class.path"));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }
        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jars[i].toString()));
        }
        baseClassPath.append(File.pathSeparatorChar);
        baseClassPath.append(".");
        System.setProperty("java.class.path", baseClassPath.toString());
        System.out.println("Classpath: " + baseClassPath.toString());

        // Create a new class loader which has access to our jars
        URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);
        
        // Launch CruiseControl!
        try {
            Class mainClass = loader.loadClass(MAIN_CLASS);
            CruiseControlMain main = (CruiseControlMain) mainClass.newInstance();
            main.start((String[]) argList.toArray(new String[argList.size()]));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Add a CLASSPATH or -lib to lib path urls.
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     */
   private void addPath(String path, boolean getJars, List libPathURLs)
       throws MalformedURLException {
       StringTokenizer myTokenizer
           = new StringTokenizer(path, System.getProperty("path.separator"));
       while (myTokenizer.hasMoreElements()) {
           String elementName = myTokenizer.nextToken();
           File element = new File(elementName);
           if (elementName.indexOf("%") != -1 && !element.exists()) {
               continue;
           }
           if (getJars && element.isDirectory()) {
               // add any jars in the directory
               URL[] dirURLs = Locator.getLocationURLs(element);
               for (int j = 0; j < dirURLs.length; ++j) {
                   libPathURLs.add(dirURLs[j]);
               }
           }

           libPathURLs.add(element.toURL());
       }
   }
}
