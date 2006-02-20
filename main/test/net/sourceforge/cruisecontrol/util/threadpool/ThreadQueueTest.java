/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util.threadpool;

import junit.framework.TestCase;

/**
 * @author Jared Richardson
 *         <p/>
 *         JUnit test class to work on net.sourceforge.cruisecontrol.util.ThreadQueue
 */
public class ThreadQueueTest extends TestCase {
    private static final String TASK_NAME_PREFIX = "SHOULD NOT MATTER - ";
    private static final int TASK_COUNT = 5;
    private static final int TASK_SLEEP_TIME = 1000;
    
    protected void setUp() throws Exception {  
        long start = System.currentTimeMillis();
        for (int i = 0; i < TASK_COUNT; i++) {
            final String taskName = taskNameFor(i);

            IdleThreadQueueClient pt = new IdleThreadQueueClient();
            String[] myArgs = {taskName, TASK_SLEEP_TIME + ""};
            pt.setParams(myArgs);
            ThreadQueue.addTask(pt);
            assertEquals(ThreadQueue.numTotalTasks(), i + 1);
        }

        sleep(TASK_SLEEP_TIME / 10);
    }

    protected void tearDown() {
        long start = System.currentTimeMillis();
        ThreadQueue.terminate();
    }
    
    public void testIsIdle() throws Exception {
        assertTrue(ThreadQueue.isIdle(taskNameFor(1)));
        assertTrue(ThreadQueue.isIdle(taskNameFor(2)));
    }
    
    public void testIsNotIdle() throws Exception {
        assertFalse(ThreadQueue.isIdle(taskNameFor(0)));

        ThreadQueue.waitFor(taskNameFor(1));
        assertFalse(ThreadQueue.isIdle(taskNameFor(1)));
    }
    
    public void testIsIdleNotCaseSensitive() throws Exception {
        assertTrue(ThreadQueue.isIdle(taskNameFor(TASK_COUNT - 1).toLowerCase()));
    }

    public void testNonexistentTasksAreNotIdle() throws Exception {
        assertFalse(ThreadQueue.isIdle(taskNameFor(42)));
    }

    public void testInterrupt() throws Exception {
        ThreadQueue.interrupt(taskNameFor(0));
        assertInterrupted(taskNameFor(0));
        
        ThreadQueue.interrupt(taskNameFor(TASK_COUNT - 1));
        assertInterrupted(taskNameFor(TASK_COUNT - 1));
    }
    
    /**
     * testPoolInterruptAll adds tasks to the queue and then, while interrupting
     * each task, checks the state of the pool
     */
    public void testPoolInterruptAll() {
//        System.out.println("\nStarting testPoolInterruptAll");

        for (int i = 0; i < TASK_COUNT; i++) {
            ThreadQueue.interruptAllRunningTasks();

            // We want to sleep long enough for the running task to wake up
            // and notice it's terminated but not so long that the next task
            // is scheduled enough time to actually finish
            sleep(250);

//            System.out.println("ThreadQueue.numTotalTasks()->" + ThreadQueue.numTotalTasks());
//            System.out.println("ThreadQueue.numWaitingTasks()->" + ThreadQueue.numWaitingTasks());
//            System.out.println("ThreadQueue.numCompletedTasks()->" + ThreadQueue.numCompletedTasks());

            assertEquals("total tasks should be constant", TASK_COUNT, ThreadQueue.numTotalTasks());
            int stillWaiting = TASK_COUNT - i - 2;
            if (stillWaiting < 0) {
                stillWaiting = 0;
            }
            assertEquals("waiting tasks should drop", stillWaiting, ThreadQueue.numWaitingTasks());
            assertEquals("completed tasks should increase", i + 1, ThreadQueue.numCompletedTasks());
        }
        assertEquals(TASK_COUNT, ThreadQueue.numTotalTasks());
        ThreadQueue.terminate();
        assertEquals(0, ThreadQueue.numTotalTasks());

//        System.out.println("\nExiting testPoolInterruptAll");
    }

    /**
     * testPoolFunctions adds tasks to the queue and then checks various thread queue functions
     */
    public void testPoolFunctions() {
//        System.out.println("\nStarting testPoolFunctions");

        // ensure the tasks were all created and added
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertTrue(ThreadQueue.taskExists(taskName));
        }

        // ensure all the tasks are active
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertTrue(ThreadQueue.isActive(taskName));
        }

        ThreadQueue.waitForAll();

        // ensure the tasks are still in the system
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertTrue(ThreadQueue.taskExists(taskName));
        }

        // ensure all the tasks aren't active
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertFalse(ThreadQueue.isActive(taskName));
        }
//        System.out.println("\nExiting testPoolFunctions");
    }

    /**
     * testExecution adds tasks to the queue and then waits for them to finish
     */
    public void testExecution() {
//        System.out.println("\nStarting testExecution");

        // check that the number of running tasks is the same number as we have
        // worker threads available.  Be sure that every available worker thread
        // is in use
        int numRunningTasks = ThreadQueue.numRunningTasks();
        int numWorkerThreads = ThreadQueue.getMaxNumWorkerThreads();
        assertEquals(numWorkerThreads, numRunningTasks);

        // make sure the correct number of idle tasks are idle
        // the waiting number should be the total number of worker tasks less the
        // number of threads
        int numThatShouldBeWaiting = TASK_COUNT - ThreadQueue.getMaxNumWorkerThreads();
        // unless the overall number of worker tasks is less than the number of threads
        if (ThreadQueue.getMaxNumWorkerThreads() > TASK_COUNT) {
            numThatShouldBeWaiting = 0;
        }
        assertEquals(numThatShouldBeWaiting, ThreadQueue.numWaitingTasks());

        // wait for a subset of the time it takes one task to run and
        // make sure the numbers are all still correct
        ThreadQueue.waitForAll(TASK_SLEEP_TIME / 10);

        //check that the number of running tasks is the same number as we have
        // worker threads available.  Be sure that every available worker thread
        // is in use
        numRunningTasks = ThreadQueue.numRunningTasks();
        numWorkerThreads = ThreadQueue.getMaxNumWorkerThreads();

        assertEquals(numWorkerThreads, numRunningTasks);

        // make sure the correct number of idle tasks is idle
        assertEquals(numThatShouldBeWaiting, ThreadQueue.numWaitingTasks());

        // ensure the tasks are still in the system
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertTrue(ThreadQueue.taskExists(taskName));
        }

        // now let them all finish
        ThreadQueue.waitForAll();

        int currentTasks = ThreadQueue.numRunningTasks() + ThreadQueue.numWaitingTasks();
        assertEquals(0, currentTasks);

        // ensure the tasks are still in the system
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertTrue(ThreadQueue.taskExists(taskName));
        }

        // ensure all the tasks aren't active anymore
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            assertFalse(ThreadQueue.isActive(taskName));
        }

        // check the return values of all the worker threads
        for (int i = 0; i < TASK_COUNT; i++) {
            String taskName = taskNameFor(i);
            Object rawResult = ThreadQueue.getResult(taskName);
            assertTrue(rawResult instanceof String);
            assertEquals("DONE WITH " + taskName, (String) rawResult);
        }

//        System.out.println("\nExiting testExecution");
    }

    /**
     * testCounters adds tasks to the queue and then checks various counters as the tasks finish
     */
    public void testCounters() {
//        System.out.println("\nStarting testCounters");

        // now that all the tasks are loaded and running
        // see if the counts are correct.
        assertEquals(ThreadQueue.getMaxNumWorkerThreads(), ThreadQueue.numRunningTasks());
        int i = 0;
        while (i < TASK_COUNT) {
            i = i + ThreadQueue.getMaxNumWorkerThreads();

//            System.out.println("count is " + TASK_COUNT);
//            System.out.println("i is " + i);
//            System.out.println("ThreadQueue.numWaitingTasks() is " + ThreadQueue.numWaitingTasks());

            // how many are waiting
            assertEquals(TASK_COUNT - i, ThreadQueue.numWaitingTasks());

            // how many are finished
            // should be i less the currently running taks
            // unless we are at the end of the loop
            if (i + ThreadQueue.getMaxNumWorkerThreads() <= TASK_COUNT) {
                assertEquals(i - ThreadQueue.getMaxNumWorkerThreads(), ThreadQueue.numCompletedTasks());
            }

            // how many are running
            // unless we are at the end of the loop
            if (i + ThreadQueue.getMaxNumWorkerThreads() <= TASK_COUNT) {
                assertEquals(ThreadQueue.numRunningTasks(), ThreadQueue.getMaxNumWorkerThreads());
            }

//            System.out.println("waiting for " + (i - 1));
            ThreadQueue.waitFor(taskNameFor(i - 1));
        }

//        System.out.println("Exiting testCounters ");
    }

    /**
     * Utility call for sleeps
     */
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }
    
    private static void assertInterrupted(String taskName) {
        assertFalse(ThreadQueue.isActive(taskName));
        assertFalse(ThreadQueue.isDone(taskName));
        assertFalse(ThreadQueue.isIdle(taskName));
    }

    private static String taskNameFor(int i) {
        return TASK_NAME_PREFIX + i;
    }
}
