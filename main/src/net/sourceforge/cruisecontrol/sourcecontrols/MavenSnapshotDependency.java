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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

/**
 * Checks binary dependencies listed in a Maven project rather than in a
 * repository.
 *
 * <pre>
 * Modifications 20060626 (jarkko.viinamaki at removethis.tietoenator.com):
 * - made POM scanning namespace aware. Dependencies were not detected if project.xml
 *   had schema definition in the project element
 * - added support for "ejb-client" dependency type
 * - added echo for detected snapshot dependencies
 * - added support for build.properties or other similiar properties file which contains
 *   key=value tags to replace ${key} type strings in project.xml
 * Modifications 20060627
 * - fixed a bug in replaceVariables method
 * </pre>
 *
 * @author Tim Shadel
 */
public class MavenSnapshotDependency implements SourceControl {

    private SourceControlProperties properties = new SourceControlProperties();
    private List<Modification> modifications;
    private File projectFile;
    private File propertiesFile;
    private File localRepository = new File(System.getProperty("user.home") + "/.maven/repository/");
    private String user;

    /** enable logging for this class */
    private static final Logger LOG = Logger.getLogger(MavenSnapshotDependency.class);

    /**
     * Set the root folder of the directories that we are going to scan
     */
    public void setProjectFile(String s) {
        projectFile = new File(s);
    }

    /**
     * Sets the .properties file which contains overriding tags for POM.
     *
     * Default is build.properties
     */
    public void setPropertiesFile(String s) {
        propertiesFile = new File(s);
    }


    /**
     * Set the path for the local Maven repository
     */
    public void setLocalRepository(String s) {
        localRepository = new File(s);
    }

    /**
     * Set the username listed with changes found in binary dependencies
     */
    public void setUser(String s) {
        user = s;
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(projectFile, "projectFile", this.getClass());
        ValidationHelper.assertTrue(projectFile.exists(),
            "Project file '" + projectFile.getAbsolutePath() + "' does not exist.");
        ValidationHelper.assertFalse(projectFile.isDirectory(),
            "The directory '" + projectFile.getAbsolutePath()
            + "' cannot be used as the projectFile for MavenSnapshotDependency.");

        ValidationHelper.assertTrue(localRepository.exists(),
            "Local Maven repository '" + localRepository.getAbsolutePath() + "' does not exist.");
        ValidationHelper.assertTrue(localRepository.isDirectory(),
            "Local Maven repository '" + localRepository.getAbsolutePath()
            + "' must be a directory.");
    }

    /**
     * The quiet period is ignored. All dependencies changed since the last
     * build trigger a modification.
     *
     *  @param lastBuild
     *            date of last build
     *  @param now
     *            IGNORED
     */
    public List<Modification> getModifications(Date lastBuild, Date now) {
        modifications = new ArrayList<Modification>();

        checkProjectDependencies(projectFile, lastBuild.getTime());

        return modifications;
    }

    /**
     * Add a Modification to the list of modifications. All modifications are
     * listed as "change" and all have the same comment.
     */
    private void addRevision(File dependency) {
        Modification mod = new Modification("maven");
        Modification.ModifiedFile modfile = mod.createModifiedFile(dependency.getName(), dependency.getParent());
        modfile.action = "change";

        mod.userName = user;
        mod.modifiedTime = new Date(dependency.lastModified());
        mod.comment = "Maven project dependency: timestamp change detected.";
        modifications.add(mod);

        properties.modificationFound();
    }

    /**
     * Use Maven library to open project file and check for newer dependencies.
     * Do not download them, only list them as modifications.
     */
    private void checkProjectDependencies(File projectFile, long lastBuild) {
        final List filenames = getSnapshotFilenames(projectFile);
        final Iterator itr = filenames.iterator();
        while (itr.hasNext()) {
            String filename = (String) itr.next();
            File dependency = new File(filename);
            checkFile(dependency, lastBuild);
        }
    }

    /**
     * Replaces variables in a string defined as ${key}.
     *
     * Values for variables are taken from given properties or System properties.
     * Replacement is recursive. If ${key} maps to a string which has other ${keyN} values,
     * those ${keyN} values are replaced also if there is a matching value for them.
     */
    String replaceVariables(Properties p, String value) {
        if (value == null || p == null) {
            return value;
        }

        int i = value.indexOf("${");
        if (i == -1) {
            return value;
        }
        int pos = 0;
        while (i != -1) {
            int j = value.indexOf("}", i);
            if (j == -1) {
                break;
            }
            String key = value.substring(i + 2, j);
            // LOG.info("Tag: " + key);

            if (p.containsKey(key)) {
               value = value.substring(0, i) + p.getProperty(key) + value.substring(j + 1);
               // step one forward from ${ position, otherwise we can get an infinite loop
               pos = i + 1;
            } else if (System.getProperty(key) != null) {
               value = value.substring(0, i) + System.getProperty(key) + value.substring(j + 1);
               pos = i + 1;
            } else {
               // could not replace the value, leave it there
               pos = j + 1;
            }
            // LOG.info("New value: " + value);

            i = value.indexOf("${", pos);
        }
        return value;
    }

    /**
     * Parse the Maven project file, and file names
     */
    List getSnapshotFilenames(File mavenFile) {
        LOG.info("Getting a list of dependencies for " + mavenFile);

        final List<String> filenames = new ArrayList<String>();
        Element mavenElement;
        SAXBuilder builder = new SAXBuilder();
        try {
            mavenElement = builder.build(mavenFile).getRootElement();
        } catch (JDOMException e) {
            LOG.error("failed to load project file ["
                + (mavenFile != null ? mavenFile.getAbsolutePath() : "")
                + "]", e);
            return filenames;
        } catch (IOException e) {
            LOG.error("failed to load project file ["
                + (mavenFile != null ? mavenFile.getAbsolutePath() : "")
                + "]", e);
            return filenames;
        }

        // load the project properties file if it exists
        Properties projectProperties = new Properties();

        if (propertiesFile == null) {
            propertiesFile = new File(mavenFile.getParent() + "/build.properties");
        }

        if (propertiesFile.exists()) {

            BufferedInputStream in = null;
            try {
                FileInputStream fin = new FileInputStream(propertiesFile);
                in = new BufferedInputStream(fin);
                projectProperties.load(in);
            } catch (IOException ex) {
                LOG.error("failed to load project properties file ["
                           + propertiesFile.getAbsolutePath() + "]", ex);
            } finally {
                IO.close(in);
            }
        }

        // set some default properties
        projectProperties.put("basedir", mavenFile.getParent());

        // JAR overrides are currently not implemented. Some guidelines how to do it:
        //   http://jira.public.thoughtworks.org/browse/CC-141
        /*
        boolean mavenJarOverride = false;

        String tmp = projectProperties.getProperty("maven.jar.override");
        if (tmp != null && (tmp.equalsIgnoreCase("on") || tmp.equalsIgnoreCase("true"))) {
            mavenJarOverride = true;
        }
        */

        Namespace ns = mavenElement.getNamespace();

        Element depsRoot = mavenElement.getChild("dependencies", ns);

        // No dependencies listed at all
        if (depsRoot == null) {
            LOG.warn("No dependencies detected.");
            return filenames;
        }

        List dependencies = depsRoot.getChildren();
        Iterator itr = dependencies.iterator();
        while (itr.hasNext()) {
            Element dependency = (Element) itr.next();
            String versionText = dependency.getChildText("version", ns);

            if (versionText != null && versionText.endsWith("SNAPSHOT")) {
                String groupId = dependency.getChildText("groupId", ns);
                String artifactId = dependency.getChildText("artifactId", ns);
                String id = dependency.getChildText("id", ns);
                String type = dependency.getChildText("type", ns);

                // replace variables
                artifactId = replaceVariables(projectProperties, artifactId);
                groupId = replaceVariables(projectProperties, groupId);
                id = replaceVariables(projectProperties, id);
                versionText = replaceVariables(projectProperties, versionText);

                if (type == null) {
                    type = "jar";
                }

                // Format:
                // ${repo}/${groupId}/${type}s/${artifactId}-${version}.${type}
                StringBuffer fileName = new StringBuffer();
                fileName.append(localRepository.getAbsolutePath());
                fileName.append('/');
                if (groupId != null) {
                    fileName.append(groupId);
                } else {
                    fileName.append(id);
                }
                fileName.append('/');

                if ("ejb-client".equals(type)) {
                    fileName.append("ejb");
                } else {
                    fileName.append(type);
                }
                fileName.append('s');
                fileName.append('/');
                if (artifactId != null) {
                    fileName.append(artifactId);
                } else {
                    fileName.append(id);
                }
                fileName.append('-');
                fileName.append(versionText);

                if ("ejb-client".equals(type)) {
                    fileName.append("-client");
                }

                fileName.append('.');
                if ("uberjar".equals(type) || "ejb".equals(type)
                        || "plugin".equals(type) || "ejb-client".equals(type)) {
                    fileName.append("jar");
                } else {
                    fileName.append(type);
                }


                File file = new File(fileName.toString());

                LOG.info("Snapshot detected: " + fileName);

                filenames.add(file.getAbsolutePath());
            }
        }
        return filenames;
    }

    /** Check for newer timestamps */
    private void checkFile(File file, long lastBuild) {
        if ((!file.isDirectory()) && (file.lastModified() > lastBuild)) {
            addRevision(file);
        }
    }
}
