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

import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * Provides an independent thread of execution that knows how to
 * build Projects.  Consumes {@link Project} objects from a blocking queue.
 * 
 * @author Peter Mei <pmei@users.sourceforge.net>
 * @author jfredrick
 */
public class BuildQueue implements Runnable {
    private static final Logger LOG = Logger.getLogger(BuildQueue.class);

    private LinkedList _queue = new LinkedList();
    private boolean _alive = false;
    private boolean _waiting = false;

    /**
     * @param project
     */
    public void requestBuild(Project project) {
        synchronized (_queue) {
            _queue.add(project);
            _queue.notify();
        }
    }

    void serviceQueue() {
        while (!_queue.isEmpty()) {
            Project nextProject = null;
            synchronized (_queue) {
                nextProject = (Project) _queue.remove(0);
            }
            if (nextProject != null) {
                LOG.info("now building: " + nextProject.getName());
                nextProject.execute();
            }
        }
    }

    public void run() {
        try {
            while (_alive) {
                synchronized (_queue) {
                    if (_queue.isEmpty()) {
                        try {
                            _waiting = true;
                            _queue.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    _waiting = false;
                }
                serviceQueue();
            }
        } finally {
            LOG.info("BuildQueue thread is no longer alive");
        }
    }

    void start() {
        Thread buildQueueThread = new Thread(this, "BuildQueueThread");
        buildQueueThread.setDaemon(false);
        buildQueueThread.start();
        while (!buildQueueThread.isAlive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        _alive = true;
    }
    
    void stop() {
        _alive = false;
        synchronized (_queue) {
            _queue.notify();
        }        
    }
    
    public boolean isAlive() {
        return _alive;
    }
    
    public boolean isWaiting() {
        return _waiting;
    }

    BuildQueue(boolean startQueue) {
        if (startQueue) {
            start();
        }
    }

}
