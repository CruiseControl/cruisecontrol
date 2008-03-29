package net.sourceforge.cruisecontrol;

import java.util.Date;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.EmptyLabelIncrementer;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueue;

import org.jdom.Element;

public class ForcingBuildShouldNotLockProjectInQueuedStateTest extends TestCase {

    private BuildQueue buildQueue;
    private int runCount;

    protected void setUp() throws Exception {
        super.setUp();

        runCount = 0;
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

            Project forcedProject = simulateForcingBuild();

            if (ProjectState.QUEUED == forcedProject.getState()) {
                fail(msgRunCount + "Project must NOT be in queued state. "
                        + "This means project is lost and will remain in this state until CC restart.");
            }

            String failure = msgRunCount + "Totally unexpected project state. "
                    + "It has to be either QUEUED (which means it should be detected by previous assertion) "
                    + "or WAITING (which means that we are lucky and race condition is not exposed this time";
            assertEquals(failure, ProjectState.WAITING.getName(), forcedProject.getState().getName());
        } finally {
            buildQueue.stop();
        }
    }

    Project simulateForcingBuild() throws Exception {
        MockProject projectToForce = new MockProject();
        projectToForce.setBuildQueue(buildQueue);
        MockScheduleThatAllowsControllingBuilder schedule = new MockScheduleThatAllowsControllingBuilder();
        ProjectConfig projectConfig = new MockProjectConfig(projectToForce, schedule);
        projectConfig.setName("forcing-build-test-project");
        projectConfig.configureProject();
        schedule.setBuilderToBuildForever();
        projectToForce.start();

        Thread.sleep(150);

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

        public MockProjectConfig(Project project, Schedule schedule) {
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

        Project readProject(String projectName) {
            return project;
        }
    }

    private final class MockScheduleThatAllowsControllingBuilder extends MockSchedule {
        private boolean ihibitBuild = true;
        private boolean scheduledBuildFired = false;

        public long getTimeToNextBuild(Date date, long interval) {
            // build only once
            if (!scheduledBuildFired) {
                scheduledBuildFired = true;
                return 0;
            }
            return 999999999;
        }

        public synchronized Element build(int buildNumber, Date lastBuild, Date now, Map propMap, String buildTarget,
                Progress progress)
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