/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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

import junit.framework.TestCase;

public class ForcingBuildShouldNotLockProjectInQueuedStateTest extends TestCase {
    private static final String DUMMY_NAME = "Dummy project for junit";

    public void testThreadQueueShouldCleanCompletedTasksWhenAddingNewTask() throws Exception {
        assertFalse("worker is not yet added", ThreadQueue.isActive(DUMMY_NAME));

        WorkerThreadThatSleepsForever scheduledWorkerThread = new WorkerThreadThatSleepsForever(DUMMY_NAME);
        ThreadQueue.addTask(scheduledWorkerThread);

        assertTrue("scheduled worker is active", ThreadQueue.isActive(DUMMY_NAME));
        assertTrue("scheduled worker is yet idle", ThreadQueue.getIdleTaskNames().contains(DUMMY_NAME));

        Thread.sleep(150);

        assertFalse("scheduled worker is now busy", ThreadQueue.getIdleTaskNames().contains(DUMMY_NAME));

        scheduledWorkerThread.completeBuild();

        WorkerThread forcedWorkerThread = new WorkerThreadThatSleepsForever(DUMMY_NAME);
        try {
            ThreadQueue.addTask(forcedWorkerThread);
        } catch (TaskAlreadyAddedException e) {
            fail("worker should be easily added because previous one with the same name is already completed");
        }
    }

    private static final class WorkerThreadThatSleepsForever implements WorkerThread {
        private String taskName;

        private boolean completed = false;

        public WorkerThreadThatSleepsForever(String taskName) {
            this.taskName = taskName;
        }

        public void completeBuild() {
            this.completed = true;
        }

        public String getName() {
            return taskName;
        }

        public Object getResult() {
            return this.completed ? "finished" : null;
        }

        public void terminate() {
        }

        public void run() {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}