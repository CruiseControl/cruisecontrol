package net.sourceforge.cruisecontrol.util.threadpool;

import junit.framework.TestCase;

public class ThreadQueueShouldHandleDuplicateTasksTest extends TestCase {
    private StubWorkerThread scheduledWorkerThread;
    private StubWorkerThread dupeOfScheduledWorkerThread;

    protected void setUp() throws Exception {
        super.setUp();
        scheduledWorkerThread = new StubWorkerThread();
        dupeOfScheduledWorkerThread = new StubWorkerThread();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        scheduledWorkerThread.completeBuild();
        dupeOfScheduledWorkerThread.completeBuild();
        ThreadQueue.stopQueue();
    }

    public void testThreadQueueShouldHandleDuplicateTasks() throws Exception {
        ThreadQueueProperties.setMaxThreadCount(10);

        assertTrue("number of busy/started threads should yet be 0", StubWorkerThread.numberOfRunningThreads == 0);

        ThreadQueue.addTask(scheduledWorkerThread);
        try {
            ThreadQueue.addTask(dupeOfScheduledWorkerThread);
        } catch (RuntimeException e) {
            fail("ThreadQueue should allow the same project to be added twice. "
                    + "Duplicates are handled internally by ThreadQueue "
                    + "which makes sure they are not build simultaneously");
        }

        int count = 0;
        while (StubWorkerThread.numberOfRunningThreads == 0 && count < 5) {
            Thread.sleep(count * 100);
            count++;
        }
        assertTrue("only the first scheduledWorkerThread should be busy/started, so number of threads should be 1",
                StubWorkerThread.numberOfRunningThreads == 1);

        scheduledWorkerThread.completeBuild();

        // This required length of this sleep may vary depending on thread scheduling...maybe we need some kind of
        // queueListener to be sure of such state changes?
        Thread.sleep(250);

        assertTrue("now the second scheduledWorkerThread should be busy/started, so number of threads should be 1",
                StubWorkerThread.numberOfRunningThreads == 1);

        scheduledWorkerThread.completeBuild();
    }

    private static final class StubWorkerThread implements WorkerThread {
        private final String taskName = "Dummy project from ThreadQueueShouldHandleDuplicateTasksTest";
        private boolean completed = false;
        private static int numberOfRunningThreads = 0;

        public synchronized void completeBuild() {
            this.completed = true;
            this.notify();
        }

        public String getName() {
            return taskName;
        }

        public synchronized Object getResult() {
            return this.completed ? "finished" : null;
        }

        public synchronized void run() {
            try {
                if (numberOfRunningThreads > 1) {
                    fail("ThreadQueue should never allow to run the same project simultanously");
                }

                numberOfRunningThreads++;
                this.wait();
                numberOfRunningThreads--;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
