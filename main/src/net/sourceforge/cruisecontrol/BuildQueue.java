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
package net.sourceforge.cruisecontrol;

import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueue;
import org.apache.log4j.Logger;

import java.util.LinkedList;

/**
 * Provides an independent thread of execution that knows how to
 * build Projects.  Passes {@link Project} objects to a thread
 * queue.  The number of worker threads is defined in config.xml
 *
 * @author Peter Mei <pmei@users.sourceforge.net>
 * @author jfredrick
 * @author Jared Richardson <jared.richardson@sas.com>
 */
public class BuildQueue implements Runnable {
    private static final Logger LOG = Logger.getLogger(BuildQueue.class);

    private LinkedList queue = new LinkedList();
    private boolean waiting = false;
    private Thread buildQueueThread;

    /**
     * @param project
     */
    public void requestBuild(Project project) {
        synchronized (queue) {
            queue.add(project);
            queue.notify();
        }
    }

    void serviceQueue() {
        while (!queue.isEmpty()) {
            Project nextProject;
            synchronized (queue) {
                nextProject = (Project) queue.remove(0);
            }
            if (nextProject != null) {
                LOG.info("now adding to the thread queue: " + nextProject.getName());
                ProjectWrapper pw = new ProjectWrapper(nextProject);
                // let's not add the task more than once
                String name = nextProject.getName();
                if (ThreadQueue.isActive(name)) {
                  // it's already there... don't re-add it.
                  // later, we'll need to add it to a queued up list
                  // so we don't 'forget' about the new build request
                } else {
                  ThreadQueue.addTask(pw);
                }
            }
        }
    }

    public void run() {
        try {
            LOG.info("BuildQueue started");
            while (true) {
                synchronized (queue) {
                    if (queue.isEmpty()) {
                        waiting = true;
                        queue.wait();
                    }
                    waiting = false;
                }
                serviceQueue();
            }
        } catch (InterruptedException e) {
            String message = "BuildQueue.run() interrupted. Stopping?";
            LOG.debug(message, e);
        } catch (Throwable e) {
            LOG.error("BuildQueue.run()", e);
        } finally {
            waiting = false;
            LOG.info("BuildQueue thread is no longer alive");
        }
    }

    void start() {
        buildQueueThread = new Thread(this, "BuildQueueThread");
        buildQueueThread.setDaemon(false);
        buildQueueThread.start();
        while (!buildQueueThread.isAlive()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                String message = "BuildQueue.start() interrupted";
                LOG.error(message, e);
                throw new RuntimeException(message);
            }
        }
    }

    void stop() {
        LOG.info("Stopping BuildQueue");
        buildQueueThread.interrupt();
        synchronized (queue) {
            queue.notify();
        }
    }

    public boolean isAlive() {
        return true;
    }

    public boolean isWaiting() {
        return waiting;
    }
}
