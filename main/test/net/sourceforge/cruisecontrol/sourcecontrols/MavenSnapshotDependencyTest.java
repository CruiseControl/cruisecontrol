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
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class MavenSnapshotDependencyTest extends TestCase {

    private static final String BAD_REPOSITORY = "folder";

    private static final String TEST_PROJECT_XML =
        "test/net/sourceforge/cruisecontrol/sourcecontrols/maven-project.xml";

    private static final String TEST_REPOSITORY = "test/net/sourceforge/cruisecontrol/sourcecontrols/";

    public MavenSnapshotDependencyTest(String name) {
        super(name);
    }

    public void testValidateNoProject() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        try {
            dep.validate();
            fail("MavenSnapshotDependency should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertEquals(
                "'projectFile' is a required attribute for MavenSnapshotDependency",
                e.getMessage());
        }
    }

    public void testValidateProjectDoesNotExist() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        String fileName = BAD_REPOSITORY;
        dep.setProjectFile(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("MavenSnapshotDependency should throw exceptions when required attributes have bad values.");
        } catch (CruiseControlException e) {
            assertEquals("Project file '" + f.getAbsolutePath()
                + "' does not exist.", e.getMessage());
        }
    }

    public void testValidateProjectIsDirectory() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        String fileName = TEST_REPOSITORY;
        dep.setProjectFile(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("MavenSnapshotDependency should throw exceptions when required attributes have bad values.");
        } catch (CruiseControlException e) {
            assertEquals(
                "The directory '"
                    + f.getAbsolutePath()
                    + "' cannot be used as the projectFile for MavenSnapshotDependency.",
                e.getMessage());
        }
    }

    public void testValidateRepositoryDoesNotExist() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        String fileName = BAD_REPOSITORY;
        dep.setProjectFile(TEST_PROJECT_XML);
        dep.setLocalRepository(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("MavenSnapshotDependency should throw exceptions when repository has bad value.");
        } catch (CruiseControlException e) {
            assertEquals("Local Maven repository '" + f.getAbsolutePath()
                + "' does not exist.", e.getMessage());
        }
    }

    public void testValidateRepositoryIsNotDirectory() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        String fileName = TEST_PROJECT_XML;
        dep.setProjectFile(fileName);
        dep.setLocalRepository(fileName);
        File f = new File(fileName);
        try {
            dep.validate();
            fail("MavenSnapshotDependency should throw exceptions when repository has bad value.");
        } catch (CruiseControlException e) {
            assertEquals("Local Maven repository '" + f.getAbsolutePath()
                + "' must be a directory.", e.getMessage());
        }
    }

    public void testValidateOk() {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        dep.setProjectFile(TEST_PROJECT_XML);
        dep.setLocalRepository(TEST_REPOSITORY);
        try {
            dep.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("MavenSnapshotDependency should not throw exceptions when attributes have valid values: "
                + e.getMessage());
        }
    }

    public void testGetProjectXml() throws Exception {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        dep.setProjectFile(TEST_PROJECT_XML);
        dep.setLocalRepository(TEST_REPOSITORY);
        File testProject = new File(TEST_PROJECT_XML);
        List filenames = dep.getSnapshotFilenames(testProject);
        assertEquals("Filename list is not the correct size", 2, filenames.size());
        String filename = (String) filenames.get(0);
        String expectedFilename = TEST_REPOSITORY + "/maven/jars/cc-maven-test-1.0-SNAPSHOT.jar";
        File expectedFile = new File(expectedFilename);
        assertEquals("Unexpected filename", expectedFile.getAbsolutePath(), filename);
        filename = (String) filenames.get(1);
        expectedFilename = TEST_REPOSITORY + "/maven/jars/maven-1.0-SNAPSHOT.jar";
        expectedFile = new File(expectedFilename);
        assertEquals("Unexpected filename", expectedFile.getAbsolutePath(), filename);
    }

    public void testGettingModifications() throws Exception {
        MavenSnapshotDependency dep = new MavenSnapshotDependency();

        dep.setProjectFile(TEST_PROJECT_XML);
        dep.setLocalRepository(TEST_REPOSITORY);
        Date epoch = new Date(0);
        Date now = new Date();
        List modifications = dep.getModifications(epoch, now);
        assertEquals("Modification list is not the correct size", 2, modifications.size());
    }
}