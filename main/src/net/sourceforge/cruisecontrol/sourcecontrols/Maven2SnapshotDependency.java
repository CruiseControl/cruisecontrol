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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Checks snapshot dependencies listed in a Maven2 pom against the local repositorty.
 *
 * Date: Feb 8, 2006
 * Time: 9:15:47 PM
 *
 * @author Dan Rollo
 */
public class Maven2SnapshotDependency  implements SourceControl {

    /** enable logging for this class */
    private static final Logger LOG = Logger.getLogger(Maven2SnapshotDependency.class);

    private final SourceControlProperties properties = new SourceControlProperties();
    private List modifications;
    private File pomFile;
    private String user;
    private File localRepoDir; //@todo Must be null until maven embedder honors alignWithUserInstallation.

    static final String COMMENT_TIMESTAMP_CHANGE = " timestamp change detected: ";
    static final String COMMENT_MISSING_IN_LOCALREPO = " missing in local repo: ";


    /**
     * @param s the pom.xml file who's snapshot dependencies we are going to scan
     */
    public void setPomFile(String s) {
        pomFile = new File(s);
    }

    /**
     * @param s the path for the local Maven repository.
     * Normally, this is not set in order to use the default location: user.home/.m2/repository.
     */
    //@todo Make "public" when maven embedder honors alignWithUserInstallation
    void setLocalRepository(String s) {
        if (s != null) {
            localRepoDir = new File(s);
        } else {
            localRepoDir = null;
        }
    }

    /**
     * @param s the username listed with changes found in binary dependencies
     */
    public void setUser(String s) {
        user = s;
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }


    public void validate() throws CruiseControlException {

        ValidationHelper.assertIsSet(pomFile, "pomFile", this.getClass());

        ValidationHelper.assertTrue(pomFile.exists(),
            "Pom file '" + pomFile.getAbsolutePath() + "' does not exist.");

        ValidationHelper.assertFalse(pomFile.isDirectory(),
            "The directory '" + pomFile.getAbsolutePath()
            + "' cannot be used as the pomFile for Maven2SnapshotDependency.");


        if (localRepoDir != null) {
            ValidationHelper.assertTrue(localRepoDir.exists(),
                "Local Maven repository '" + localRepoDir.getAbsolutePath() + "' does not exist.");

            ValidationHelper.assertTrue(localRepoDir.isDirectory(),
                "Local Maven repository '" + localRepoDir.getAbsolutePath()
                + "' must be a directory.");
        }
    }


    /**
     * The quiet period is ignored. All dependencies changed since the last
     * build trigger a modification.
     *
     * @param lastBuild
     *            date of last build
     *  @param now
     *            IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {

        modifications = new ArrayList();

        LOG.debug("Reading pom: " + pomFile.getAbsolutePath() + " with lastBuild: " + lastBuild);

        ArtifactInfo[] artifactsToCheck = getSnapshotInfos();

        for (int i = 0; i < artifactsToCheck.length; i++) {
            checkFile(artifactsToCheck[i], lastBuild.getTime());
        }

        return modifications;
    }


    /**
     * Add a Modification to the list of modifications. All modifications are
     * listed as "change" or "missing" if not in local repo.
     * @param dependency snapshot detected as modified
     * @param changeType modification type ("change" or "missing")
     * @param comment constant note according to changeType
     */
    private void addRevision(File dependency, String changeType, String comment) {
        Modification newMod = new Modification("maven2");
        Modification.ModifiedFile modfile = newMod.createModifiedFile(dependency.getName(), dependency.getParent());
        modfile.action = changeType;

        newMod.userName = user;
        newMod.modifiedTime = new Date(dependency.lastModified());
        newMod.comment = comment;
        modifications.add(newMod);

        properties.modificationFound();
    }


    /** Immutable data holder class. */
    static final class ArtifactInfo {
        static final String ART_TYPE_PARENT = "parent";
        static final String ART_TYPE_DEPENDENCY = "dependency";

        private final Artifact artifact;
        private final String artifactType;
        private final File localRepoFile;

        private ArtifactInfo(final Artifact artifact, final String artifactType, File localRepoFile) {
            this.artifact = artifact;
            this.artifactType = artifactType;
            this.localRepoFile = localRepoFile;
        }

        Artifact getArtifact() {
            return artifact;
        }

        String getArtifactType() {
            return artifactType;
        }

        File getLocalRepoFile() {
            return localRepoFile;
        }

        public String toString() {
            return artifact + "," + artifactType + ","
                    + (localRepoFile != null ? localRepoFile.getAbsolutePath() : null);
        }
    }


    /**
     * Return a file referring to the given artifact in the local repository.
     * @param localRepoBaseDir the actual base dir of the active local repository
     * @param artifact a artifact to be checked in the local repository
     * @return a file referring to the given artifact in the local repository
     */
    //@todo Maybe we can delete this whole method after a while.
    private static File getArtifactFilename(final File localRepoBaseDir, final Artifact artifact) {

        LOG.warn("We should not need this approach to finding artifact files. Artifact: " + artifact);

        // Format:
        // ${repo}/${groupId,dots as dirs}/${artifactId}/${version}/${artifactId}-${version}[-${classifier}].${type}
        StringBuffer fileName = new StringBuffer();
        fileName.append(localRepoBaseDir.getAbsolutePath());

        fileName.append('/');

        fileName.append(artifact.getGroupId().replace('.', '/'));

        fileName.append('/');

        final String artifactId = artifact.getArtifactId();
        fileName.append(artifactId);

        fileName.append('/');

        final String versionText = artifact.getVersion();
        fileName.append(versionText);

        fileName.append('/');

        fileName.append(artifactId);
        fileName.append('-');
        fileName.append(versionText);

        if (artifact.getClassifier() != null) {
            fileName.append('-');
            fileName.append(artifact.getClassifier());
        }

        fileName.append('.');

        final String type = artifact.getType();
        fileName.append(type != null ? type : "jar");

        //@todo Handle type="system" and "systemPath", or not if we can delete this whole method.

        return new File(fileName.toString());
    }

    /**
     * Parse the Maven pom file, and return snapshot artifact info populated with dependencies to be checked.
     * @return return snapshot artifact info populated with dependencies to be checked
     */
    ArtifactInfo[] getSnapshotInfos() {

        final MavenEmbedder embedder = getMvnEmbedder();
        try {

            // With readProjectWithDependencies(), local repo dependencies (+transitive) will be updated if possible
            final MavenProject projectWithDependencies = getProjectWithDependencies(embedder, pomFile);

            // use local repo dir from embedder because this is the dir it is actually using
            final File localRepoBaseDir = new File(embedder.getLocalRepository().getBasedir());


            final List artifactInfos = new ArrayList();

            // handle parents and grandparents...
            findParentSnapshotArtifacts(projectWithDependencies, artifactInfos, localRepoBaseDir, embedder, pomFile);


            // handle dependencies
            final Set snapshotArtifacts;
            if (projectWithDependencies != null) {

                // projectWithDependencies.getDependencyArtifacts() would exclude transitive artifacts
                snapshotArtifacts = getSnaphotArtifacts(projectWithDependencies.getArtifacts());

            } else {

                // couldn't read project, so try to do some stuff manually
                snapshotArtifacts = getSnapshotArtifactsManually(embedder);
            }
            Artifact artifact;
            for (Iterator i = snapshotArtifacts.iterator(); i.hasNext(); ) {
                artifact = (Artifact) i.next();

                addArtifactInfo(artifactInfos, artifact, ArtifactInfo.ART_TYPE_DEPENDENCY, localRepoBaseDir);
            }


            return (ArtifactInfo[]) artifactInfos.toArray(new ArtifactInfo[artifactInfos.size()]);

        } finally {
            try {
                embedder.stop();
            } catch (MavenEmbedderException e) {
                LOG.error("Failed to stop embedded maven2", e);
            }
        }
    }

    private static void findParentSnapshotArtifacts(MavenProject projectWithDependencies, List artifactInfos,
                                                    File localRepoBaseDir, MavenEmbedder embedder, File pomFile) {
        // handle parents and grandparents...
        if (projectWithDependencies != null) {

            MavenProject currMvnProject = projectWithDependencies;

            Artifact parentArtifact = currMvnProject.getParentArtifact();
            while ((parentArtifact != null)
                    && parentArtifact.isSnapshot()) {

                addArtifactInfo(artifactInfos, parentArtifact, ArtifactInfo.ART_TYPE_PARENT, localRepoBaseDir);
                currMvnProject = currMvnProject.getParent();

                parentArtifact = currMvnProject.getParentArtifact();
            }

        } else {

            // couldn't read project, so try to do some stuff manually
            MavenProject mavenProject = null;
            try {
                mavenProject = embedder.readProject(pomFile);
            } catch (ProjectBuildingException e) {
                LOG.error("Failed to read maven2 mavenProject", e);
            }

            if (mavenProject != null) {

                MavenProject currMvnProject = mavenProject;

                Artifact artifact = currMvnProject.getParentArtifact();
                while ((artifact != null)
                        && (artifact.getVersion().endsWith(Artifact.SNAPSHOT_VERSION) || artifact.isSnapshot())
                        ) {

                    addArtifactInfo(artifactInfos, artifact, ArtifactInfo.ART_TYPE_PARENT, localRepoBaseDir);

                    resolveArtifact(embedder, artifact, mavenProject, embedder.getLocalRepository());

                    currMvnProject = currMvnProject.getParent();

                    artifact = currMvnProject.getParentArtifact();
                }
            }
        }
    }

    private static MavenProject getProjectWithDependencies(MavenEmbedder embedder, File pomFile) {
        // With readProjectWithDependencies(), local repo dependencies (+transitive) will be updated if possible
        MavenProject projectWithDependencies = null;
        try {

            final TransferListener transferListener = new ConsoleDownloadMonitor() {
                public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
                    // do nothing to avoid lot's of progress messages in logs
                }
            };

            projectWithDependencies = embedder.readProjectWithDependencies(pomFile, transferListener);

        } catch (ProjectBuildingException e) {
            LOG.error("Failed to read maven2 projectWithDependencies", e);
        } catch (ArtifactResolutionException e) {
            LOG.warn("Failed to resolve artifact", e);
        } catch (ArtifactNotFoundException e) {
            LOG.warn("Couldn't find artifact", e);
        }
        return projectWithDependencies;
    }

    private static void resolveArtifact(MavenEmbedder embedder, Artifact artifact,
                                        MavenProject mavenProject, ArtifactRepository localRepo) {
        try {
            embedder.resolve(artifact, mavenProject.getPluginArtifactRepositories(), localRepo);
        } catch (ArtifactResolutionException e) {
            LOG.debug("Unresolved artifact", e);
        } catch (ArtifactNotFoundException e) {
            LOG.debug("Missing artifact", e);
        }
    }


    private static void addArtifactInfo(List artifactInfos, Artifact artifact, String artifactType,
                                        File localRepoBaseDir) {

        final File file;
        if (artifact.getFile() == null) {
            file = getArtifactFilename(localRepoBaseDir, artifact);
        } else {
            file = artifact.getFile();
        }

        artifactInfos.add(new ArtifactInfo(artifact, artifactType, file));
    }


    /**
     * Filter out non-SNAPSHOT artifacts.
     * @param artifacts all project artifacts, including non-SNAPSHOTS
     * @return a set of artifacts containing only SNAPSHOTs
     */
    private static Set getSnaphotArtifacts(final Set artifacts) {

        final Set retVal = new HashSet();

        Artifact artifact;
        for (Iterator i = artifacts.iterator(); i.hasNext(); ) {
            artifact = (Artifact) i.next();
            LOG.debug("Examining artifact: " + artifact);
            if (artifact.isSnapshot()) {
                retVal.add(artifact);
            }
        }

        return retVal;
    }


    /**
     * Doesn't handle transitive deps, nor actually download anything so far.
     * @param embedder the maven embedder used to read the pomFile
     * @return a set of artifacts containing only SNAPSHOTs
     */
    private Set getSnapshotArtifactsManually(final MavenEmbedder embedder) {

        final MavenProject mavenProject;
        try {
            mavenProject = embedder.readProject(pomFile);
        } catch (ProjectBuildingException e) {
            LOG.error("Failed to read maven2 mavenProject", e);
            return new HashSet();
        }

        // override default repo if needed
        final ArtifactRepository localRepo;
        if (localRepoDir != null) {
            try {
                localRepo = embedder.createLocalRepository(localRepoDir);
            } catch (ComponentLookupException e) {
                LOG.error("Error setting maven2 local repo to: " + localRepoDir.getAbsolutePath(), e);
                throw new RuntimeException("Error setting maven2 local repo to: " + localRepoDir.getAbsolutePath()
                        + "; " + e.getMessage());
            }
        } else {
            localRepo = embedder.getLocalRepository();
        }

        // get snapshot dependencies
        final Set snapshotArtifacts = getSnapshotDepsManually(embedder, mavenProject);

        Artifact artifact;
        for (Iterator i = snapshotArtifacts.iterator(); i.hasNext();) {
            artifact = (Artifact) i.next();
            LOG.debug("Manually examining artifact: " + artifact);
            resolveArtifact(embedder, artifact, mavenProject, localRepo);
        }

        return snapshotArtifacts;
    }

    private static Set getSnapshotDepsManually(final MavenEmbedder mavenEmbedder, final MavenProject mavenProject) {
        final Set retVal = new HashSet();

        // Not really sure if mavenEmbedder.readProject() is any better than mavenEmbedder.readModel()
        // At this point, which ever is used, it should not update files in the local repo.

        /*
        //final Set deps = mavenProject.getDependencyArtifacts(); // This returns null, how to init embedder correctly?
        final List depsList = mavenProject.getDependencyManagement().getSnapshotDepsManually();
        //*/

        //*  // should not update files in the local repo.
        final Model model = mavenProject.getModel();
        final List depsList = model.getDependencies();
        //*/


        LOG.debug("found dependencies manually: " + depsList.toString());
        Dependency dep;
        Artifact artifact;
        for (int i = 0; i < depsList.size(); i++) {
            dep = (Dependency) depsList.get(i);
            if (dep.getVersion().endsWith(Artifact.SNAPSHOT_VERSION)) {
                if (dep.getClassifier() != null) {
                    artifact = mavenEmbedder.createArtifactWithClassifier(dep.getGroupId(), dep.getArtifactId(),
                                    dep.getVersion(), dep.getType(), dep.getClassifier());
                } else {
                    artifact = mavenEmbedder.createArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                                    dep.getScope(), dep.getType());
                }

                if (Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
                    // fill in systemPath for file
                    artifact.setFile(new File(dep.getSystemPath(),
                            artifact.getArtifactId() + "-" + artifact.getVersion()
                                    + (artifact.getClassifier() != null ? "-" + artifact.getClassifier() : "")
                                    + "." + artifact.getType()));
                }
                retVal.add(artifact);
            }
        }

        return retVal;
    }


    /**
     * Check for newer timestamps, add modification if change detected.
     * @param artifactInfo artifact data to be compared against the last build date to determine if modified
     * @param lastBuild the last build date
     */
    private void checkFile(final ArtifactInfo artifactInfo, long lastBuild) {
        final File file = artifactInfo.localRepoFile;
        LOG.debug("Checking artifact: " + artifactInfo.getArtifact());
        if ((!file.isDirectory()) && (file.lastModified() > lastBuild)) {

            addRevision(file, "change", artifactInfo.artifactType
                    + COMMENT_TIMESTAMP_CHANGE + artifactInfo.getArtifact().getArtifactId());

        } else if (!file.isDirectory() && !file.exists()) {

            addRevision(file, "missing", artifactInfo.artifactType
                    + COMMENT_MISSING_IN_LOCALREPO + artifactInfo.getArtifact().getArtifactId());
        }
    }

    private MavenEmbedder getMvnEmbedder() {

        final MavenEmbedder mvnEmbedder = new MavenEmbedder();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        mvnEmbedder.setClassLoader(classLoader);
        mvnEmbedder.setLogger(new MavenEmbedderConsoleLogger());

        // what do these really do?
        //mvnEmbedder.setOffline(true);
        //mvnEmbedder.setCheckLatestPluginVersion(false);
        //mvnEmbedder.setUpdateSnapshots(false);
        //mvnEmbedder.setInteractiveMode(false);
        //mvnEmbedder.setUsePluginRegistry(false);
        //mvnEmbedder.setPluginUpdateOverride(true);

        if (localRepoDir != null) {
            mvnEmbedder.setLocalRepositoryDirectory(localRepoDir);
            mvnEmbedder.setAlignWithUserInstallation(false);
        } else {
            mvnEmbedder.setAlignWithUserInstallation(true);
        }

        try {
            // embedder start can take a long time when debugging
            mvnEmbedder.start();
        } catch (MavenEmbedderException e) {
            LOG.error("Failed to start embedded maven2", e);
        }

        return mvnEmbedder;
    }
}
