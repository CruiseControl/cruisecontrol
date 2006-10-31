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
    private static final String TASK_NAME = "TASK:";
    private static final int TASK_COUNT = 5;
    private static final int TENTH_OF_SECOND = 100;
    
    protected void setUp() throws Exception {  
        for (int i = 1; i < TASK_COUNT + 1; i++) {
            final String taskName = TASK_NAME + i;

            IdleThreadQueueClient task = new IdleThreadQueueClient(taskName);
            ThreadQueue.addTask(task);
            assertEquals(i, ThreadQueue.numTotalTasks());
        }

        sleep(3 * TENTH_OF_SECOND);
    }

    protected void tearDown() {
        ThreadQueue.terminate();
    }
    
    public void testIsIdle() throws Exception {
        assertFalse(ThreadQueue.isIdle(TASK_NAME + 1));
        assertTrue(ThreadQueue.isIdle(TASK_NAME + 2));
        assertTrue(ThreadQueue.isIdle(TASK_NAME + 3));

        tasksThatCompleteShouldNotBeIdle();

        caseOfNameShouldNotMatter();

        tasksThatDontExistShouldNotBeIdle();
    }

    private void tasksThatCompleteShouldNotBeIdle() {
        ThreadQueue.waitFor(TASK_NAME + 2);
        assertFalse(ThreadQueue.isIdle(TASK_NAME + 2));
    }

    private void caseOfNameShouldNotMatter() {
        String taskName = TASK_NAME + TASK_COUNT;
        assertTrue(ThreadQueue.isIdle(taskName.toLowerCase()));
    }

    private void tasksThatDontExistShouldNotBeIdle() {
        assertFalse(ThreadQueue.isIdle(TASK_NAME + 42));
    }
    
    public void testInterrupt() throws Exception {
        assertFalse(ThreadQueue.isIdle(TASK_NAME + 1));
        ThreadQueue.interrupt(TASK_NAME + 1);
        assertInterrupted(TASK_NAME + 1);
        
        assertTrue(ThreadQueue.isIdle(TASK_NAME + TASK_COUNT));
        ThreadQueue.interrupt(TASK_NAME + TASK_COUNT);
        assertInterrupted(TASK_NAME + TASK_COUNT);
    }
    
    public void testExecution() {
        verifyCountOfRunningAndIdleTasksCorrect();

        for (int i = 1; i < TASK_COUNT + 1; i++) {
            String taskName = TASK_NAME + i;
            assertTrue(ThreadQueue.taskExists(taskName));
            assertTrue(ThreadQueue.isActive(taskName));
        }

        // now let them all finish
        ThreadQueue.waitForAll();

        assertEquals(0, ThreadQueue.numRunningTasks());
        assertEquals(0, ThreadQueue.numWaitingTasks());

        for (int i = 1; i < TASK_COUNT + 1; i++) {
            String taskName = TASK_NAME + i;

            assertTrue(ThreadQueue.taskExists(taskName));
            assertFalse(ThreadQueue.isActive(taskName));            
            
            // check the return values of all the worker threads
            Object rawResult = ThreadQueue.getResult(taskName);
            assertTrue(rawResult instanceof String);
            assertEquals("DONE WITH " + taskName, (String) rawResult);
        }
    }

    private void verifyCountOfRunningAndIdleTasksCorrect() {
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
    }

    private static void sleep(int ms) {
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

}
