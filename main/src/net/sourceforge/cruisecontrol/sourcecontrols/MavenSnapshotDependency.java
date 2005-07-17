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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Checks binary dependencies listed in a Maven project rather than in a
 * repository.
 * 
 *  <at> author Tim Shadel
 */
public class MavenSnapshotDependency implements SourceControl {

    private Hashtable properties = new Hashtable();
    private String property;
    private List modifications;
    private File projectFile;
    private File localRepository = new File(System.getProperty("user.home") + "/.maven/repository/");
    private String user;

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(MavenSnapshotDependency.class);

    /**
     * Set the root folder of the directories that we are going to scan
     */
    public void setProjectFile(String s) {
        projectFile = new File(s);
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
        this.property = property;
    }

    /**
     * Unsupported by MavenDependency.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return properties;
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
     *  <at> param lastBuild
     *            date of last build
     *  <at> param now
     *            IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {
        modifications = new ArrayList();

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

        if (property != null) {
            properties.put(property, "true");
        }
    }

    /**
     * Use Maven library to open project file and check for newer dependencies.
     * Do not download them, only list them as modifications.
     */
    private void checkProjectDependencies(File projectFile, long lastBuild) {
        List filenames = getSnapshotFilenames(projectFile);
        Iterator itr = filenames.iterator();
        while (itr.hasNext()) {
            String filename = (String) itr.next();
            File dependency = new File(filename);
            checkFile(dependency, lastBuild);
        }
    }

    /**
     * Parse the Maven project file, and file names
     */
    List getSnapshotFilenames(File mavenFile) {
        List filenames = new ArrayList();
        Element mavenElement;
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        try {
            mavenElement = builder.build(mavenFile).getRootElement();
        } catch (JDOMException e) {
            log.error("failed to load project file ["
                + (mavenFile != null ? mavenFile.getAbsolutePath() : "")
                + "]", e);
            return filenames;
        } catch (IOException e) {
            log.error("failed to load project file ["
                + (mavenFile != null ? mavenFile.getAbsolutePath() : "")
                + "]", e);
            return filenames;
        }
        Element depsRoot = mavenElement.getChild("dependencies");

        // No dependencies listed at all
        if (depsRoot == null) {
            return filenames;
        }
        List dependencies = depsRoot.getChildren();
        Iterator itr = dependencies.iterator();
        while (itr.hasNext()) {
            Element dependency = (Element) itr.next();
            String versionText = dependency.getChildText("version");
            if (versionText != null && versionText.endsWith("SNAPSHOT")) {
                String groupId = dependency.getChildText("groupId");
                String artifactId = dependency.getChildText("artifactId");
                String id = dependency.getChildText("id");
                String type = dependency.getChildText("type");

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
                fileName.append(type);
                fileName.append('s');
                fileName.append('/');
                if (artifactId != null) {
                    fileName.append(artifactId);
                } else {
                    fileName.append(id);
                }
                fileName.append('-');
                fileName.append(versionText);
                fileName.append('.');
                if ("uberjar".equals(type) || "ejb".equals(type)
                        || "plugin".equals(type)) {
                    fileName.append("jar");
                } else {
                    fileName.append(type);
                }
                File file = new File(fileName.toString());
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
