package net.sourceforge.cruisecontrol;

import java.util.Date;
import java.util.Map;
import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.EmptyLabelIncrementer;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueue;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.jdom2.Element;

public class ForcingBuildShouldNotLockProjectInQueuedStateTest extends TestCase {

    private BuildQueue buildQueue;
    private int runCount;

    private static final String FORCING_BUILD_TEST_PROJECT_NAME = "forcing-build-test-project";
    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    protected void setUp() throws Exception {
        super.setUp();

        runCount = 0;

        // build create a FORCING_BUILD_TEST_PROJECT_NAME.ser file, so delete it
        filesToDelete.add(new File(FORCING_BUILD_TEST_PROJECT_NAME + ".ser"));
    }

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }

    public void testRepeatForcingBuildShouldNotLockProjectInQueuedState() throws Exception {
        for (int i = 0; i < 10; i++) {
            // may need to be run multiple times since the good thread may just be lucky and win...
            runCount = i;
            testForcingBuildShouldNotLockProjectInQueuedState();
        }
    }

    /**
     * This test tries to expose race condition.
     * It's very time-dependent and requires running multiple times to be 100% sure.
     * @throws Exception if test fails
     */
    public void testForcingBuildShouldNotLockProjectInQueuedState() throws Exception {

        final String msgRunCount = "(" + getName() + " runCount: " + runCount + ") ";

        buildQueue = new BuildQueue();
        buildQueue.start();
        Thread.sleep(500);
        try {

            final Project forcedProject = simulateForcingBuild();

            if (ProjectState.QUEUED == forcedProject.getState()) {
                fail(msgRunCount + "Project must NOT be in queued state. "
                        + "This means project is lost and will remain in this state until CC restart.");
            }

            final String failure = msgRunCount + "Totally unexpected project state. "
                    + "It has to be either QUEUED (which means it should be detected by previous assertion) "
                    + "or WAITING (which means that we are lucky and race condition is not exposed this time";
            assertEquals(failure, ProjectState.WAITING.getName(), forcedProject.getState().getName());
        } finally {
            buildQueue.stop();
        }
    }

    Project simulateForcingBuild() throws Exception {
        final MockProject projectToForce = new MockProject();
        projectToForce.setBuildQueue(buildQueue);
        final MockScheduleThatAllowsControllingBuilder schedule = new MockScheduleThatAllowsControllingBuilder();
        final ProjectConfig projectConfig = new MockProjectConfig(projectToForce, schedule);
        projectConfig.setName(FORCING_BUILD_TEST_PROJECT_NAME);
        projectConfig.configureProject();
        schedule.setBuilderToBuildForever();
        projectToForce.start();

        // @todo Sleep time here may not be enough...maybe we need a state change listener?
        Thread.sleep(250);

        assertEquals(ProjectState.BUILDING.getName(), projectToForce.getState().getName());

        // force during build
        projectToForce.setBuildForced(true);

        // finish current build so that forced build can act
        schedule.setBuilderToFinishBuildingImmediately();

        // give the build some time to finish
        Thread.sleep(300);

        // sanity checks
        assertTrue(buildQueue.isWaiting());

        int count = 0;
        while ((ThreadQueue.getIdleTaskNames().size() != 0) && (count < 20)) {
            count++;
            Thread.sleep(100 * count);
        }
        assertTrue("ThreadQueue.getIdleTaskNames() should be empty", ThreadQueue.getIdleTaskNames().size() == 0);

        count = 0;
        while ((ThreadQueue.getBusyTaskNames().size() != 0) && (count < 20)) {
            count++;
            Thread.sleep(100 * count);
        }
        assertTrue("ThreadQueue.getBusyTaskNames() should be empty", ThreadQueue.getBusyTaskNames().size() == 0);

        return projectToForce;
    }

    private final class MockProjectConfig extends ProjectConfig {

        private final Project project;
        private final Schedule schedule;

        public MockProjectConfig(final Project project, final Schedule schedule) {
            this.project = project;
            this.schedule = schedule;
        }

        public void add(Listeners listeners) {
        }

        public LabelIncrementer getLabelIncrementer() {
            return new EmptyLabelIncrementer();
        }

        public Schedule getSchedule() {
            return schedule;
        }

        public Log getLog() {
            return new Log() {
                public void writeLogFile(Date now) throws CruiseControlException {
                }
            };
        }

        Project readProject(final String projectName) {
            return project;
        }
    }

    private final class MockScheduleThatAllowsControllingBuilder extends MockSchedule {
        private boolean ihibitBuild = true;
        private boolean scheduledBuildFired = false;

        public long getTimeToNextBuild(final Date date, final long interval) {
            // build only once
            if (!scheduledBuildFired) {
                scheduledBuildFired = true;
                return 0;
            }
            return 999999999;
        }

        public synchronized Element build(final int buildNumber, final Date lastBuild, final Date now,
                                          final Map<String, String> propMap, final String buildTarget,
                final Progress progress)
                throws CruiseControlException {

            while (ihibitBuild) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    fail("Exception: " + e.getMessage());
                }
            }

            return super.build(buildNumber, lastBuild, now, propMap, buildTarget, progress);
        }

        public synchronized void setBuilderToBuildForever() {
            this.ihibitBuild = true;
            this.notify();
        }

        public synchronized void setBuilderToFinishBuildingImmediately() {
            this.ihibitBuild = false;
            this.notify();
        }
    }

    private static class MockProject extends Project {
        Element getModifications(boolean buildWasForced) {
            return new Element("modifications");
        }
    }
}
