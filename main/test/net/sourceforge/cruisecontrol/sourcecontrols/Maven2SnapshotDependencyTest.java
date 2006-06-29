/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
import java.net.URL;
import java.net.URLDecoder;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Unit Tests of maven2 snapshot dependency sourcecontrol.
 *
 * Date: Feb 8, 2006
 * Time: 9:15:47 PM
 *
 * @author Dan Rollo
 */
public class Maven2SnapshotDependencyTest extends TestCase {

    private static final String BAD_REPOSITORY = "folder";

    private static final String PROJECT_XML_RELATIVE_PATH =
        "net/sourceforge/cruisecontrol/sourcecontrols/maven2-pom.xml";

    private static final String TEST_PROJECT_XML;
    private static final String TEST_REPOSITORY;

    static {
        URL projectUrl = ClassLoader.getSystemResource(PROJECT_XML_RELATIVE_PATH);
        TEST_PROJECT_XML = URLDecoder.decode(projectUrl.getPath());
        // Use the parent folder of the project xml as repository folder
        TEST_REPOSITORY = new File(TEST_PROJECT_XML).getParentFile().getAbsolutePath() + "/maven2repo";
    }

    private Maven2SnapshotDependency dep;

    public Maven2SnapshotDependencyTest(String name) {
        super(name);
    }

    protected void setUp() {
        dep = new Maven2SnapshotDependency();
    }


    public void testValidateNoProject() {

        try {
            dep.validate();
            fail("Maven2SnapshotDependency should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals("'pomFile' is required for Maven2SnapshotDependency", e.getMessage());
        }
    }

    public void testValidateProjectDoesNotExist() {

        String fileName = BAD_REPOSITORY;
        dep.setPomFile(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("Maven2SnapshotDependency should throw exceptions when required attributes have bad values.");
        } catch (CruiseControlException e) {
            assertEquals("Pom file '" + f.getAbsolutePath()
                + "' does not exist.", e.getMessage());
        }
    }

    public void testValidateProjectIsDirectory() {

        String fileName = TEST_REPOSITORY;
        dep.setPomFile(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("Maven2SnapshotDependency should throw exceptions when required attributes have bad values.");
        } catch (CruiseControlException e) {
            assertEquals(
                "The directory '"
                    + f.getAbsolutePath()
                    + "' cannot be used as the pomFile for Maven2SnapshotDependency.",
                e.getMessage());
        }
    }

    public void testValidateRepositoryNotSet() throws Exception {
        dep.setPomFile(TEST_PROJECT_XML);
        dep.validate();
    }

    public void testValidateRepositoryDoesNotExist() throws Exception {

        String fileName = BAD_REPOSITORY;
        dep.setPomFile(TEST_PROJECT_XML);
        dep.setLocalRepository(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("Maven2SnapshotDependency should throw exceptions when repository has bad value.");
        } catch (CruiseControlException e) {
            assertEquals("Local Maven repository '" + f.getAbsolutePath()
                + "' does not exist.", e.getMessage());
        }
    }

    public void testValidateRepositoryIsNotDirectory() throws Exception {

        String fileName = TEST_PROJECT_XML;
        dep.setPomFile(fileName);
        dep.setLocalRepository(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("Maven2SnapshotDependency should throw exceptions when repository has bad value.");
        } catch (CruiseControlException e) {
            assertEquals("Local Maven repository '" + f.getAbsolutePath()
                + "' must be a directory.", e.getMessage());
        }
    }

    public void testValidateOkWithRepo() throws Exception {
        dep.setPomFile(TEST_PROJECT_XML);
        dep.setLocalRepository(TEST_REPOSITORY);
        dep.validate();
    }

    public void testValidateOk() throws Exception {
        dep.setPomFile(TEST_PROJECT_XML);
        dep.validate();
    }

/*
    this test is not jre 1.3 compatible because it invokes the MavenEmbedder.
    mocking that connection would allow the test to run under 1.3 and also 
    make the test much faster.  (current ~7 seconds)

    public void testGetPomXml() throws Exception {

        dep.setPomFile(TEST_PROJECT_XML);
         //@todo Fix when maven embedder honors alignWithUserInstallation
        //dep.setLocalRepository(TEST_REPOSITORY);
        

        Maven2SnapshotDependency.ArtifactInfo[] artifactInfos = dep.getSnapshotInfos();
        assertEquals("Filename list is not the correct size", 2, artifactInfos.length);

         //@todo Fix when maven embedder honors alignWithUserInstallation
        // assertEquals("Unexpected filename",
        //        new File(TEST_REPOSITORY + "/ccdeptest/cc-maven-test/1.0-SNAPSHOT/cc-maven-test-1.0-SNAPSHOT.jar"),
        //        artifactInfos[0].getLocalRepoFile());
        
        assertTrue("Unexpected filename [" + artifactInfos[0].getLocalRepoFile().getAbsolutePath() + "]",
                artifactInfos[0].getLocalRepoFile().getAbsolutePath().endsWith(
                        File.separatorChar + "ccdeptest" 
                        + File.separatorChar + "cc-maven-test" 
                        + File.separatorChar + "1.0-SNAPSHOT" 
                        + File.separatorChar + "cc-maven-test-1.0-SNAPSHOT.jar"
                ));

        assertEquals("Unexpected artifact type", Maven2SnapshotDependency.ArtifactInfo.ART_TYPE_DEPENDENCY,
                artifactInfos[0].getArtifactType());


         //@todo Fix when maven embedder honors alignWithUserInstallation
        //assertEquals("Unexpected filename",
        //        new File(TEST_REPOSITORY + "/ccdeptest/maven/1.0-SNAPSHOT/maven-1.0-SNAPSHOT-source.jar"),
        //        artifactInfos[1].getLocalRepoFile());
        
        assertTrue("Unexpected filename [" + artifactInfos[1].getLocalRepoFile().getAbsolutePath() + "]",
                artifactInfos[1].getLocalRepoFile().getAbsolutePath().endsWith(
                        File.separatorChar + "ccdeptest" 
                        + File.separatorChar + "maven" 
                        + File.separatorChar + "1.0-SNAPSHOT" 
                        + File.separatorChar + "maven-1.0-SNAPSHOT-source.jar"
                ));

        assertEquals("Unexpected artifact type", Maven2SnapshotDependency.ArtifactInfo.ART_TYPE_DEPENDENCY,
                artifactInfos[1].getArtifactType());
    }
*/

/*  

    This test is too slow (~55 second). Need tests that don't require going over the network. 
 
    public void testGetModifications() throws Exception {

        dep.setPomFile(TEST_PROJECT_XML);
        
        //@todo Fix when maven embedder honors alignWithUserInstallation
        // dep.setLocalRepository(TEST_REPOSITORY);
        
        Date epoch = new Date(0);
        Date now = new Date();
        List modifications = dep.getModifications(epoch, now);
        assertEquals("Modification list is not the correct size. " + modifications.toString(),
                2, modifications.size());

        Modification mod = (Modification) modifications.get(0);
        assertEquals("Wrong Modification: " + mod, Maven2SnapshotDependency.ArtifactInfo.ART_TYPE_DEPENDENCY
                        + Maven2SnapshotDependency.COMMENT_MISSING_IN_LOCALREPO + "cc-maven-test", mod.comment);

        mod = (Modification) modifications.get(1);
        assertEquals("Wrong Modification: " + mod, Maven2SnapshotDependency.ArtifactInfo.ART_TYPE_DEPENDENCY
                        + Maven2SnapshotDependency.COMMENT_MISSING_IN_LOCALREPO + "maven", mod.comment);
    }
*/
    
    // @todo Add support/test of transitive dependencies with snapshots
}
