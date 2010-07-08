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
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.BuildQueue.Listener;
import junit.framework.TestCase;

public class BuildQueueTest extends TestCase {

    private BuildQueue queue;

    protected void setUp() throws Exception {
        queue = new BuildQueue();
    }

    public void testListener() {
        TestListener listener = new TestListener();
        queue.addListener(listener);
        assertFalse(listener.wasBuildRequested());
        queue.requestBuild(new ProjectConfig());
        assertTrue(listener.wasBuildRequested());
    }

    public void testListenerExceptionShouldNotLeakOut() {
        Listener listener = new Listener() {
            public void buildRequested() {
                throw new RuntimeException("project before queued exception");
            }
        };

        queue.addListener(listener);
        queue.requestBuild(new ProjectConfig());
    }

    class TestListener implements Listener {
        private boolean buildRequested = false;

        boolean wasBuildRequested() {
            return buildRequested;
        }

        public void buildRequested() {
            buildRequested = true;
        }
    }


    /**
     * Unit test helper method to allow tests access to package visible method, w/out exposing setter in production API.
     * @param testBuildQueue the unit test buildQueue to be started
     */
    public static void startBuildQueue(final BuildQueue testBuildQueue) {
        testBuildQueue.start();
    }
}
