/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol;

import java.util.Date;

import junit.framework.TestCase;

/**
 * @author Peter Mei <pmei@users.sourceforge.net>
 * @author jfredrick
 */
public class BuildQueueTest extends TestCase {

    private BuildQueue queue;

    protected void setUp() throws Exception {
        boolean startQueue = false;
        queue = new BuildQueue(startQueue);
    }

    public void testServiceQueue() {
        MockProject queuedProjects[] = new MockProject[3];
        for (int i = 0; i < queuedProjects.length; i++) {
            queuedProjects[i] = new MockProject();
            queuedProjects[i].setName("Build " + i);
            queue.requestBuild(queuedProjects[i]);
        }

        queue.serviceQueue();

        for (int i = 0; i < queuedProjects.length - 1; i++) {
            MockProject thisBuild = queuedProjects[i];
            MockProject nextBuild = queuedProjects[i + 1];
            Date thisBuildDate = thisBuild.getLastBuildDate();
            Date nextBuildDate = nextBuild.getLastBuildDate();

            assertEquals(1, thisBuild.getBuildCount());
            assertTrue(
                thisBuild.getName()
                    + " should be before "
                    + nextBuild.getName(),
                thisBuildDate.before(nextBuildDate));
        }
    }

    public void testStartAndStop() throws InterruptedException {
        queue.start();
        MockProject project = new MockProject();
        project.setName("BuildQueueTest.testStartAndStop()");
        queue.requestBuild(project);        
        for (int sleepCount = 0; project.getBuildCount() == 0; sleepCount++) {
            if (sleepCount > 5) {
                break;
            }
            Thread.sleep(500);
        }
        assertEquals(1, project.getBuildCount());

        queue.stop();
        Thread.sleep(1000);
        assertTrue(!queue.isWaiting());
        assertTrue(!queue.isAlive());
        
        queue.requestBuild(project);
        for (int sleepCount = 0; project.getBuildCount() == 1; sleepCount++) {
            if (sleepCount > 2) {
                break;
            }
            Thread.sleep(500);
        }
        assertEquals(1, project.getBuildCount());
    }

}
