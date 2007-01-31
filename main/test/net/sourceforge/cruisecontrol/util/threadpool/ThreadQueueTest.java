/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util.threadpool;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Jared Richardson <p/> JUnit test class to work on
 *         net.sourceforge.cruisecontrol.util.ThreadQueue
 */
public class ThreadQueueTest extends TestCase {
    private static final String TIMING_SENSITIVE_MESSAGE = "This is timing sensitive, please run it several times"
            + " before reporting it";
    private static final String TASK_NAME = "TASK:";
    private static final int TASK_COUNT = 5;
    private static final int TENTH_OF_SECOND = 100;
    private List tasks;

    protected void setUp() throws Exception {
        tasks = new ArrayList();
        for (int i = 1; i < TASK_COUNT + 1; i++) {
            final String taskName = TASK_NAME + i;

            IdleThreadQueueClient task = new IdleThreadQueueClient(taskName);
            ThreadQueue.addTask(task);
            tasks.add(task);
        }

        // @todo Without this (at least on jdk 1.5, Linux), this test takes 100% cpu and never exits.
        // I believe this problem results from longer thread startup times in 1.5 AND that it may
        // point to a bug in ThreadQueue - thought I'm not sure of either...
        // In any case, adding this kludge so other unit tests are allowed to continue.
        sleep(3 * TENTH_OF_SECOND);
    }

    protected void tearDown() {
        ThreadQueue.stopQueue();
        tasks = null;
    }

    public void testExecution() {
        for (int i = 1; i < TASK_COUNT + 1; i++) {
            String taskName = TASK_NAME + i;
            String message = TIMING_SENSITIVE_MESSAGE + " Failure: " + taskName + " not active.";
            assertTrue(message, ThreadQueue.isActive(taskName));
        }

        // now let them all finish
        boolean allFinished = false;
        int loops = 0;
        while (!allFinished && loops < 10) {
            sleep(10 * TENTH_OF_SECOND);
            boolean anyActive = false;
            for (int i = 1; i < TASK_COUNT + 1 || anyActive; i++) {
                String taskName = TASK_NAME + i;
                if (ThreadQueue.isActive(taskName)) {
                    anyActive = true;
                }
            }
            allFinished = !anyActive;
        }
        
        for (int i = 1; i < TASK_COUNT + 1; i++) {
            String taskName = TASK_NAME + i;
            String message = TIMING_SENSITIVE_MESSAGE + " task " + taskName + " still active.";
            assertFalse(message, ThreadQueue.isActive(taskName));

            IdleThreadQueueClient task = (IdleThreadQueueClient) tasks.get(i - 1);
            assertEquals("DONE WITH " + taskName, task.getResult());
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }
}
