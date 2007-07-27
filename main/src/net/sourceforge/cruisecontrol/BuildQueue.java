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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.cruisecontrol.util.threadpool.TaskAlreadyAddedException;
import net.sourceforge.cruisecontrol.util.threadpool.ThreadQueue;

import org.apache.log4j.Logger;

/**
 * Provides an independent thread of execution that knows how to
 * build Projects.  Passes {@link ProjectInterface} objects to a thread
 * queue.  The number of worker threads is defined in config.xml
 *
 * @author Peter Mei <pmei@users.sourceforge.net>
 * @author jfredrick
 * @author Jared Richardson <jared.richardson@sas.com>
 */
public class BuildQueue implements Runnable {
    private static final Logger LOG = Logger.getLogger(BuildQueue.class);

    private final LinkedList queue = new LinkedList();

    private boolean waiting = false;

    private Thread buildQueueThread;

    private List listeners = new ArrayList();

    /**
     * @param project
     */
    public void requestBuild(ProjectInterface project) {
        LOG.debug("BuildQueue.requestBuild Thread = " + Thread.currentThread().getName());

        notifyListeners();
        synchronized (queue) {
            queue.add(project);
            queue.notifyAll();
        }
    }

    /**
     * @param project The project to find in the queues
     * @return String representing this project's position in the various queues, e.g. IDLE[ 5 / 24 ]
     */
    public String findPosition(ProjectInterface project) {
        int position;
        int length;
        synchronized (queue) {
            position = queue.indexOf(project);
            length = queue.size();
        }
        if (position < 0) {
            return ThreadQueue.findPosition(project.getName());
        }
        // position is 0-based, make it 1-based for human reporting
        return "BUILD_REQUESTED[ " + (position + 1) + " / " + length + " ]";
    }

    private void serviceQueue() {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                ProjectInterface nextProject = (ProjectInterface) queue.remove(0);
                if (nextProject != null) {
                    LOG.info("now adding to the thread queue: " + nextProject.getName());
                    ProjectWrapper pw = new ProjectWrapper(nextProject);
                    try {
                        ThreadQueue.addTask(pw);
                    } catch (TaskAlreadyAddedException e) {
                        // it's already there... don't re-add it.
                        // later, we'll need to add it to a queued up list
                        // so we don't 'forget' about the new build request
                    }
                }
            }
        }
    }

    public void run() {
        try {
            LOG.info("BuildQueue started");
            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        waiting = true;
                        queue.wait();
                    }
                    waiting = false;
                }
                serviceQueue();
            }
        } catch (InterruptedException e) {
            LOG.debug("BuildQueue.run() interrupted. Stopping?", e);
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
        if (buildQueueThread != null) {
            buildQueueThread.interrupt();
        }
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public boolean isAlive() {
        return true;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        synchronized (listeners) {
            Iterator toNotify = listeners.iterator();
            while (toNotify.hasNext()) {
                try {
                    ((Listener) toNotify.next()).buildRequested();
                } catch (Exception e) {
                    LOG.error("exception notifying listener before project queued", e);
                }
            }
        }
    }

    public static interface Listener extends EventListener {
        void buildRequested();
    }
}
