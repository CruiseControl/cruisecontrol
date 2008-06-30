/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import java.io.File;

public class ProjectConfigTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private ProjectConfig config;

    protected void setUp() {
        config = new ProjectConfig();
        config.setName("test");
    }

    protected void tearDown() {
        config = null;

        filesToDelete.delete();
    }

    public void testBuildAfterFailedShouldDefaultToTrue() {
        assertTrue(config.shouldBuildAfterFailed());
        config.setBuildAfterFailed(false);
        assertFalse(config.shouldBuildAfterFailed());
    }

    public void testValidate_ScheduleRequired() throws CruiseControlException {
        try {
            config.validate();
            fail("a schedule should have been required by ProjectConfig");
        } catch (CruiseControlException expected) {
            assertEquals("project requires a schedule", expected.getMessage());
        }

        config.add(new MockSchedule());
        config.validate();

        filesToDelete.add(new File(TestUtil.getTargetDir(), "logs"));
    }

    public void testValidateCallsSubelementValidates() throws CruiseControlException {
        MockSchedule schedule = new MockSchedule();
        config.add(schedule);
        MockBootstrappers bootstrappers = new MockBootstrappers();
        config.add(bootstrappers);
        MockModificationSet modificationSet = new MockModificationSet();
        config.add(modificationSet);
        MockListeners listeners = new MockListeners();
        config.add(listeners);
        MockPublishers publishers = new MockPublishers();
        config.add(publishers);
        MockLog log = new MockLog();
        config.add(log);

        config.validate();

        assertTrue(schedule.validateWasCalled());
        assertTrue(bootstrappers.validateWasCalled());
        assertTrue(modificationSet.validateWasCalled());
        assertTrue(listeners.validateWasCalled());
        assertTrue(publishers.validateWasCalled());
        assertTrue(log.validateWasCalled());
    }

    public void testReadProject() {
        final Project project = config.readProject(System.getProperty("java.io.tmpdir"));
        assertNotNull(project);
        assertTrue(project.getBuildForced());
    }

    public void testForceBuildNewProject() {
        config.setForceBuildNewProject(false);
        final Project project = config.readProject(System.getProperty("java.io.tmpdir"));
        assertFalse(project.getBuildForced());
    }

    public void testToStringDelegatesToProject() throws Exception {
        config.add(new DefaultLabelIncrementer());
        config.configureProject();
        // see comments at page bottom in:
        // http://confluence.public.thoughtworks.org/display/CC/RunningCruiseControlFromUnixInit
        // for details
        assertEquals("ProjectConfig.toString() should return Project.toString() to avoid breaking external jmx scripts",
                "Project " + config.getName() + ": " + config.getStatus(),
                config.toString());
    }

    public void testShouldBeAbleToGetCommitMessage() throws Exception {
        MockModificationSet modificationSet = new MockModificationSet();
        config.add(new DefaultLabelIncrementer());
        config.add(modificationSet);
        config.configureProject();
        List modifications = config.getModifications();

        assertEquals(2, modifications.size());
        for (int i = 0; i < 2; i++) {
            Modification modification = (Modification) modifications.get(i);
            assertEquals("user" + i, modification.userName);
            assertEquals("comment" + i, modification.comment);
        }
        assertTrue(modificationSet.getCurrentModificationsWasCalled);
    }

    private static class MockBootstrappers extends ProjectConfig.Bootstrappers {

        private boolean validateWasCalled = false;

        public void validate() throws CruiseControlException {
            validateWasCalled = true;
        }

        public boolean validateWasCalled() {
            return validateWasCalled;
        }

    }

    private static class MockModificationSet extends ModificationSet {

        private boolean validateWasCalled = false;
        private boolean getCurrentModificationsWasCalled = false;

        public void validate() throws CruiseControlException {
            validateWasCalled = true;
        }

        public boolean validateWasCalled() {
            return validateWasCalled;
        }

        public List getCurrentModifications() {
            getCurrentModificationsWasCalled  = true;
            List modications = new ArrayList();
            Modification modification = new Modification();
            modification.userName = "user0";
            modification.comment = "comment0";
            modications.add(modification);
            modification = new Modification();
            modification.userName = "user1";
            modification.comment = "comment1";
            modications.add(modification);
            return modications;
        }

    }

    private static class MockListeners extends ProjectConfig.Listeners {

        private boolean validateWasCalled = false;

        public void validate() throws CruiseControlException {
            validateWasCalled = true;
        }

        public boolean validateWasCalled() {
            return validateWasCalled;
        }

    }

    private static class MockPublishers extends ProjectConfig.Publishers {

        private boolean validateWasCalled = false;

        public void validate() throws CruiseControlException {
            validateWasCalled = true;
        }

        public boolean validateWasCalled() {
            return validateWasCalled;
        }

    }

    private static class MockLog extends Log {

        private boolean validateWasCalled = false;

        public void validate() throws CruiseControlException {
            validateWasCalled = true;
        }

        public boolean validateWasCalled() {
            return validateWasCalled;
        }

    }

}
