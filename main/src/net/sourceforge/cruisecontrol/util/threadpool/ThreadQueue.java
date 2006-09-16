/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.util.TdTimer;

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
    private final List idleTasks = Collections.synchronizedList(new LinkedList());
    /**
     * The list of WorkerThreads that are running now (currently busy)
     */
    private final List busyTasks = Collections.synchronizedList(new LinkedList());

    /**
     * the resultList from each WorkerThread's run
     */
    private final Map resultList = Collections.synchronizedMap(new HashMap());

    /**
     * Retains a handle to all the running Threads
     * to handle all sorts of interesting situations
     */

    private final Map runningThreads = Collections.synchronizedMap(new HashMap());

    /**
     * The number of java.lang.Threads to be launched by the pool at one time
     */
    private final int threadCount = ThreadQueueProperties.getMaxThreadCount();

    /**
     * The amount to time to sleep between loops
     */
    private static final int SLEEP_TIME = 100;

    /**
     * A handle to the Thread Pool
     */
    private static ThreadQueue threadPool;

    /**
     * tells the main process when to exit
     */
    private static boolean terminate = false;

    /*
       fetch tasks to be executed from the idle list,
       put them on the busy list, and
       execute them
    */
    public void run() {
        while (true) {
            if (ThreadQueue.terminate) {
                LOG.info("terminating ThreadQueue.run()");
                return;
            }

            final boolean nothingWaiting = idleTasks.size() == 0;
            final boolean maxedOut = busyTasks.size() >= threadCount;

            if (nothingWaiting || maxedOut) {
                sleep(SLEEP_TIME);
            } else {
                LOG.debug("handling waiting task");
                handleWaitingTask();
            }

            cleanCompletedTasks();
        }
    }

    private void handleWaitingTask() {
        synchronized (busyTasks) {
            WorkerThread worker = (WorkerThread) idleTasks.remove(0);
            Thread thisThread = new Thread(loggingGroup, worker);
            busyTasks.add(worker);
            runningThreads.put(worker, thisThread);
            if (!ThreadQueue.terminate) {
                thisThread.start();
            }
        }
    }

    private void cleanCompletedTasks() {
        synchronized (busyTasks) {
            Iterator tasks = busyTasks.iterator();
            while (tasks.hasNext()) {
                WorkerThread task = (WorkerThread) tasks.next();
                Object result = task.getResult();
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
     */

    private static ThreadQueue getThreadQueue() {
        if (threadPool == null) {
            threadPool = new ThreadQueue();
            threadPool.start();
        }
        return threadPool;
    }

    /**
     * Adds a task to the idleList to be executed
     */
    public static void addTask(WorkerThread task) {
        LOG.debug("Preparing to add worker task " + task.getName());

        if (isActive(task.getName())) {
            throw new RuntimeException("Duplicate task name!");
        }
        synchronized (getThreadQueue().busyTasks) {
            getThreadQueue().idleTasks.add(task);
        }
    }

    /**
     * This may not *always* work -- a task may slip by us between queue checks.
     * That's OK.  We'd rather have transient results than block the busy queue
     *    until we're done just to get a position report on a task.
     */
    public static String findPosition(String taskName) {
        WorkerThread task = getIdleTask(taskName);
        if (task != null) {
            return getTaskPosition(task, getThreadQueue().idleTasks, "IDLE");
        }
        task = getBusyTask(taskName);
        if (task != null) {
            return getTaskPosition(task, getThreadQueue().busyTasks, "BUSY");
        }
        Object result = getResult(taskName);
        if (result != null) {
            return "[ COMPLETE ]";
        }
        return "[ not found in queues ]";
    }

    private static String getTaskPosition(WorkerThread task, List queue, String queueName) {
        int position;
        int length;
        synchronized (getThreadQueue().busyTasks) {
            position = queue.indexOf(task);
            length = queue.size();
        }
        return formatPosition(position, length, queueName);
    }

    private static String formatPosition(int position, int length, String queueName) {
        if (position < 0) {
            return "[ NONE ]";
        }
        // position is 0-based, make it 1-based for human reporting
        return queueName + "[ " + (position + 1) + " / " + length + " ]";
    }

    /**
     * Checks to see if all tasks are done
     */
    public static boolean isQueueIdle() {
        synchronized (getThreadQueue().busyTasks) {
            return ((getThreadQueue().busyTasks.size() == 0) && (getThreadQueue().idleTasks.size() == 0));
        }
    }

    /**
     * Checks to see if a specific task is done
     */
    public static boolean isDone(String taskName) {
        return getThreadQueue().resultList.containsKey(taskName);
    }

    /**
     * Waits until all tasks are done
     * same as Thread t.wait()
     */
    public static void waitForAll() {
        while (!ThreadQueue.isQueueIdle()) {
            sleep(SLEEP_TIME);
        }
    }

    /**
     * Waits until all tasks are done
     * same as Thread t.wait()
     *
     * @return TRUE is all tasks finished, FALSE if timeout occurred
     */
    public static boolean waitForAll(int timeout) {
        TdTimer myTimer = new TdTimer();
        while (!ThreadQueue.isQueueIdle()) {
            sleep(SLEEP_TIME);
            if (myTimer.time() > timeout) {
                return false;
            }
        }
        return true;
    }

    /**
     * Waits for a specific task to finish
     * same as Thread t.wait()
     */
    public static void waitFor(String taskName) {
        if (!taskExists(taskName)) {
            LOG.debug("taskName " + taskName + " doesn't exist");
            return;
        }
        while (!getThreadQueue().resultList.containsKey(taskName)) {
            sleep(SLEEP_TIME);
        }
    }

    /**
     * Waits for a specific task to finish, but with a timeout
     * same as Thread t.wait(), but with a timeout
     *
     * @return TRUE if task finished, FALSE if timeout occurred
     */
    public static boolean waitFor(String taskName, int timeout) {
        if (!taskExists(taskName)) {
            return false;
        }
        TdTimer myTimer = new TdTimer();
        while (!getThreadQueue().resultList.containsKey(taskName)) {
            sleep(SLEEP_TIME);
            if (myTimer.split() > timeout) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks to see if a specific task is in our system
     *
     * @return TRUE if task is found, FALSE if not
     */
    public static boolean taskExists(String taskName) {
        synchronized (getThreadQueue().busyTasks) {
            // it's either done, busy or idle
            return !((getResult(taskName) == null)
                    && (getBusyTask(taskName) == null)
                    && (getIdleTask(taskName) == null));
        }
    }

    /**
     * Checks to see if a specific task is either running or waiting in our system
     *
     * @return TRUE if task is waiting or running, FALSE if it is finished
     */
    public static boolean isActive(String taskName) {
        synchronized (getThreadQueue().busyTasks) {
            // it's either busy or idle
            return !((getBusyTask(taskName) == null)
                    && (getIdleTask(taskName) == null));
        }
    }

    /**
     * fetch a result from a completed WorkerThread
     * a null result means it's not done yet
     */

    public static Object getResult(String workerName) {
        return getThreadQueue().resultList.get(workerName);
    }

    /**
     * tells you how many tasks are running now
     */
    public static int numRunningTasks() {
        return getThreadQueue().busyTasks.size();
    }

    /**
     * tells you how many tasks are waiting now
     */
    public static int numWaitingTasks() {
        return getThreadQueue().idleTasks.size();
    }

    /**
     * tells you how many tasks have completed
     */
    public static int numCompletedTasks() {
        return getThreadQueue().resultList.size();
    }

    /**
     * tells you if a task is waiting now
     */
    public static boolean isIdle(String taskName) {
        return getIdleTask(taskName) != null;
    }

    /**
     * retrieves an active task from the busy list
     *
     * @return the active task (if present) or null if it cannot be found
     */
    private static WorkerThread getBusyTask(String taskName) {
        synchronized (getThreadQueue().busyTasks) {
            return getTask(taskName, getThreadQueue().busyTasks.iterator());
        }
    }

    /**
     * retrieves an idle task from the idle list
     *
     * @return the idle task (if present) or null if it cannot be found
     */
    private static WorkerThread getIdleTask(String taskName) {
        synchronized (getThreadQueue().idleTasks) {
            return getTask(taskName, getThreadQueue().idleTasks.iterator());
        }
    }

    /**
     * retrieves a task from the list
     *
     * @return the task (if present) or null if it cannot be found
     */
    private static WorkerThread getTask(String taskName, Iterator myIt) {
        while (myIt.hasNext()) {
            WorkerThread thisWorker = (WorkerThread) myIt.next();
            String tempString = thisWorker.getName();
            if (tempString.equalsIgnoreCase(taskName)) {
                return thisWorker;
            }
        }
        return null;
    }

    /**
     * @return the names of the tasks in the busy list; may be empty
     */
    public static List getBusyTaskNames() {
        List names;
        synchronized (getThreadQueue().busyTasks) {
            names = getTaskNames(getThreadQueue().busyTasks.iterator());
        }
        return names;
    }

    /**
     * @return the names of the tasks in the idle list; may be empty
     */
    public static List getIdleTaskNames() {
        List names;
        synchronized (getThreadQueue().busyTasks) {
            names = getTaskNames(getThreadQueue().idleTasks.iterator());
        }
        return names;
    }

    /**
     * @return the names of the tasks in the list; may be empty
     */
    private static List getTaskNames(Iterator taskIter) {
        List names = new LinkedList();
        while (taskIter.hasNext()) {
            WorkerThread thisWorker = (WorkerThread) taskIter.next();
            names.add(thisWorker.getName());
        }
        return names;
    }

    /**
     * returns a string telling you number of idle
     * and busy worker threads
     */
    public static String stats() {
        String stats = numRunningTasks() + " tasks running \n";
        stats += numWaitingTasks() + " tasks waiting \n";

        return stats;
    }

    /**
     * returns the number of idle, busy and finished
     * worker threads
     */
    public static int numTotalTasks() {
        return numRunningTasks() + numWaitingTasks() + numCompletedTasks();
    }

    /**
     * Terminate the queue's operation
     */
    public static void terminate() {
        ThreadQueue.terminate = true;
        // give everyone up to 10 seconds to acknowledge the terminate
        ThreadQueue.waitForAll(10000);
        // empty the various
        getThreadQueue().idleTasks.clear();
        getThreadQueue().busyTasks.clear();
        getThreadQueue().resultList.clear();
        threadPool = null;
        getThreadQueue();
        ThreadQueue.terminate = false;
    }

    public static void interruptAllRunningTasks() {
        synchronized (getThreadQueue().busyTasks) {
            Map currentRunningThreads = getThreadQueue().runningThreads;
            
            terminateRunningTasks(currentRunningThreads);
            interruptRunningThreads(currentRunningThreads);
        }
    }

    private static void interruptRunningThreads(Map currentRunningThreads) {
        for (Iterator iter = currentRunningThreads.values().iterator(); iter.hasNext();) {
            Thread currentThread = (Thread) iter.next();
            currentThread.interrupt();
        }
    }

    private static void terminateRunningTasks(Map currentRunningThreads) {
        for (Iterator iter = currentRunningThreads.keySet().iterator(); iter.hasNext();) {
            WorkerThread currentTask = (WorkerThread) iter.next();
            currentTask.terminate();
            
            LOG.info("Preparing to stop " + currentTask.getName());
        }
    }

    /**
     * Waits for a specific task to finish
     * same as Thread t.wait()
     */
    public static void interrupt(String taskName) {
        synchronized (getThreadQueue().busyTasks) {

            // check for it in the idleList
            // *remove it (before it starts running)
            // *return

            if (ThreadQueue.isIdle(taskName)) {
                if (getThreadQueue().idleTasks.remove(getIdleTask(taskName))) {
                    LOG.debug("removed idle project " + taskName);
                } else {
                    LOG.warn("could not remove idle project " + taskName);
                }
                return;
            } // end of if ( getThreadQueue().isIdle(taksName()) {

            // At this point, it must be busy if it is in our system
            // *interrupt it
            // *cleanup
            // *return
            WorkerThread thisWorker = getBusyTask(taskName);
            if (thisWorker != null) {
                LOG.debug("Attempting to stop a project building at the moment: " + taskName);
                Thread thisThread =
                        (Thread) getThreadQueue().runningThreads.get(thisWorker);
                thisThread.interrupt();
                getThreadQueue().busyTasks.remove(thisWorker);
                getThreadQueue().runningThreads.remove(thisThread);
                LOG.debug("Stopped " + taskName + " succesfully");
                return;
            }
            
            LOG.warn("Project is neither idle nor busy: " + taskName + "; taking no action");
        }
    }

    /**
     * Tells the caller how many worker threads are in
     * use to service the worker tasks
     */

    static int getMaxNumWorkerThreads() {
        return getThreadQueue().threadCount;
    }

    /**
     * Utility call for sleeps
     */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }
}
