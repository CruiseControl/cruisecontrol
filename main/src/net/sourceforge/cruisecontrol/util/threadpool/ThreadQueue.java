/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Used to encapsulate the concept of a Thread Pool
 * <P>
 * The queue accepts tasks that implement the WorkerThread interface.
 * Each task may be named, but do not have to be.  You may then waitOn
 * for ever task to complete or just the named tasks you care about...
 * or not wait at all.
 *
 * @author Jared Richardson
 * @version $Id$
 */

public class ThreadQueue extends Thread {
    private static final Logger LOG = Logger.getLogger(ThreadQueue.class);

    // A ThreadGroup that logs uncaught exception using Log4J
    private final ThreadGroup loggingGroup = new Log4jThreadGroup("Logging group", LOG);

    /**
     * The list of WorkerThreads that are waiting to run (currently idle)
     */
    private final List<WorkerThread> idleTasks = Collections.synchronizedList(new LinkedList<WorkerThread>());

    /**
     * The list of WorkerThreads that are running now (currently busy)
     */
    private final List<WorkerThread> busyTasks = Collections.synchronizedList(new LinkedList<WorkerThread>());

    /**
     * the resultList from each WorkerThread's run
     */
    private final Map<String, Object> resultList = Collections.synchronizedMap(new HashMap<String, Object>());

    /**
     * Retains a handle to all the running Threads
     * to handle all sorts of interesting situations
     */

    private final Map<WorkerThread, Thread> runningThreads
            = Collections.synchronizedMap(new HashMap<WorkerThread, Thread>());

    /**
     * The number of java.lang.Threads to be launched by the pool at one time
     */
    private final int threadCount = ThreadQueueProperties.getMaxThreadCount();

    /**
     * The amount of time (millis) to sleep between loops
     */
    private static final int SLEEP_TIME = 100;

    /**
     * A handle to the ThreadQueue singleton
     */
    private static ThreadQueue threadQueue;

    /*
     fetch tasks to be executed from the idle list,
     put them on the busy list, and
     execute them
     */
    public void run() {
        while (true) {

            final boolean nothingWaiting = idleTasks.size() == 0;
            final boolean maxedOut = busyTasks.size() >= threadCount;

            if (nothingWaiting || maxedOut) {
                sleep(SLEEP_TIME);
            } else {
                handleWaitingTask();
            }

            cleanCompletedTasks();
        }
    }

    private void handleWaitingTask() {
        LOG.debug("handling waiting task");
        synchronized (busyTasks) {
            synchronized (idleTasks) {
                final WorkerThread firstIdleWorkerThread = idleTasks.get(0);
                //Since idleTasks allows duplicates, lets make sure this project is not already building
                if (getBusyTask(firstIdleWorkerThread.getName()) != null) {
                    LOG.debug("The idle task is already running, it will not be moved to busy tasks yet");
                    return;
                } else {
                    idleTasks.remove(firstIdleWorkerThread);
                }

                final Thread thisThread = new Thread(loggingGroup, firstIdleWorkerThread);
                busyTasks.add(firstIdleWorkerThread);
                runningThreads.put(firstIdleWorkerThread, thisThread);
                thisThread.start();
            }
        }
    }

    private void cleanCompletedTasks() {
        synchronized (busyTasks) {
            final Iterator<WorkerThread> tasks = busyTasks.iterator();
            while (tasks.hasNext()) {
                final WorkerThread task = tasks.next();
                final Object result = task.getResult();
                final boolean taskDone = result != null;
                if (taskDone) {
                    LOG.debug("Found a finished task");
                    LOG.debug("tempTask.getName() = " + task.getName());
                    LOG.debug("tempTask.getResult() = " + task.getResult());

                    resultList.put(task.getName(), result);
                    tasks.remove();
                    runningThreads.remove(task);
                }
            }
        }
    }

    /**
     * An internal wrapper around the creation of the
     * Thread Pool singleton
     * @return ThreadQueuse singleton
     */

    private static ThreadQueue getThreadQueue() {
        if (threadQueue == null) {
            threadQueue = new ThreadQueue();
            threadQueue.start();
        }
        return threadQueue;
    }

    /**
     * Adds a task to the idleList to be executed
     * @param task a task to add to the idleList to be executed
     */
    public static void addTask(final WorkerThread task) {
        LOG.debug("Preparing to add worker task " + task.getName());

        synchronized (getThreadQueue().busyTasks) {
            synchronized (getThreadQueue().idleTasks) {

                // don't trust that 100 ms main loop managed to clean up very
                // recently finished tasks
                getThreadQueue().cleanCompletedTasks();
                getThreadQueue().idleTasks.add(task);
            }
        }
    }

    /**
     * This may not *always* work -- a task may slip by us between queue checks.
     * That's OK.  We'd rather have transient results than block the busy queue
     *    until we're done just to get a position report on a task.
     * @param taskName the taskName to look for
     * @return the tasks position in the queue
     */
    public static String findPosition(final String taskName) {
        WorkerThread task = getIdleTask(taskName);
        if (task != null) {
            return getTaskPosition(task, getThreadQueue().idleTasks, "IDLE");
        }
        task = getBusyTask(taskName);
        if (task != null) {
            return getTaskPosition(task, getThreadQueue().busyTasks, "BUSY");
        }
        final Object result = getResult(taskName);
        if (result != null) {
            return "[ COMPLETE ]";
        }
        return "[ not found in queues ]";
    }

    private static String getTaskPosition(final WorkerThread task, final List queue, final String queueName) {
        final int position;
        final int length;
        synchronized (getThreadQueue().busyTasks) {
            position = queue.indexOf(task);
            length = queue.size();
        }
        return formatPosition(position, length, queueName);
    }

    private static String formatPosition(final int position, final int length, final String queueName) {
        if (position < 0) {
            return "[ NONE ]";
        }
        // position is 0-based, make it 1-based for human reporting
        return queueName + "[ " + (position + 1) + " / " + length + " ]";
    }

    /**
     * Checks to see if a specific task is either running or waiting in our system.
     *
     * @param taskName the task name to check
     * @return TRUE if task is waiting or running, FALSE if it is finished
     */
    public static boolean isActive(final String taskName) {
        synchronized (getThreadQueue().busyTasks) {
            // it's either busy or idle
            return !((getBusyTask(taskName) == null) && (getIdleTask(taskName) == null));
        }
    }

    /**
     * fetch a result from a completed WorkerThread
     * a null result means it's not done yet.
     *
     * @param workerName the worker name to check
     * @return a result from a completed WorkerThread, or null if not yet done.
     */
    private static Object getResult(final String workerName) {
        return getThreadQueue().resultList.get(workerName);
    }

    /**
     * retrieves an active task from the busy list.
     *
     * @param taskName the task name to check
     * @return the active task (if present) or null if it cannot be found
     */
    private static WorkerThread getBusyTask(final String taskName) {
        synchronized (getThreadQueue().busyTasks) {
            return getTask(taskName, getThreadQueue().busyTasks.iterator());
        }
    }

    /**
     * retrieves an idle task from the idle list.
     *
     * @param taskName the task name to check
     * @return the idle task (if present) or null if it cannot be found
     */
    private static WorkerThread getIdleTask(final String taskName) {
        synchronized (getThreadQueue().idleTasks) {
            return getTask(taskName, getThreadQueue().idleTasks.iterator());
        }
    }

    /**
     * retrieves a task from the list
     *
     * @param taskName the task name to get
     * @param taskIter a WorkerThread iterator
     * @return the task (if present) or null if it cannot be found
     */
    private static WorkerThread getTask(final String taskName, final Iterator<WorkerThread> taskIter) {
        while (taskIter.hasNext()) {
            final WorkerThread thisWorker = taskIter.next();
            final String tempString = thisWorker.getName();
            if (tempString.equalsIgnoreCase(taskName)) {
                return thisWorker;
            }
        }
        return null;
    }

    /**
     * @return the names of the tasks in the busy list; may be empty
     */
    public static List<String> getBusyTaskNames() {
        final List<String> names;
        synchronized (getThreadQueue().busyTasks) {
            names = getTaskNames(getThreadQueue().busyTasks.iterator());
        }
        return names;
    }

    /**
     * @return the names of the tasks in the idle list; may be empty
     */
    public static List<String> getIdleTaskNames() {
        final List<String> names;
        synchronized (getThreadQueue().busyTasks) {
            names = getTaskNames(getThreadQueue().idleTasks.iterator());
        }
        return names;
    }

    /**
     * @param taskIter a WorkerThread iterator
     * @return the names of the tasks in the list; may be empty
     */
    private static List<String> getTaskNames(final Iterator<WorkerThread> taskIter) {
        final List<String> names = new LinkedList<String>();
        while (taskIter.hasNext()) {
            final WorkerThread thisWorker = taskIter.next();
            names.add(thisWorker.getName());
        }
        return names;
    }

    /**
     * Utility call for sleeps.
     * @param ms milliseconds to sleep.
     */
    private static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    static void stopQueue() {
        threadQueue.interrupt();
        threadQueue = null;
    }

}
