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

    /**
     * The list of WorkerThreads that are waiting to run (currently idle)
     */
    private List idleTasks = Collections.synchronizedList(new LinkedList());
    /**
     * The list of WorkerThreads that are running now (currently busy)
     */
    private List busyTasks = Collections.synchronizedList(new LinkedList());

    /**
     * the resultList from each WorkerThread's run
     */
    private Map resultList = Collections.synchronizedMap(new HashMap());

    /**
     * Retains a handle to all the running Threads
     * to handle all sorts of interesting situations
     */

    private Map runningThreads = Collections.synchronizedMap(new HashMap());

    /**
     * The number of java.lang.Threads to be launched by the pool at one time
     */
    private int threadCount = ThreadQueueProperties.getMaxThreadCount();

    /**
     * The amount to time to sleep between loops
     */
    private static int sleepTime = 100;

    /**
     * A handle to the Thread Pool
     */
    private static ThreadQueue threadPool;

    /**
     * this variable is used to generate a unique name
     * for tasks that are not named when they arrive in
     * the queue
     */

    private static long nameCounter = Long.MIN_VALUE;

    /**
     * this variable is simple used to synchronize
     * access to the nameCounter above
     */

    private static Long nameCounterSynchObject = new Long("0");

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
                sleep(sleepTime);
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
            Thread thisThread = new Thread(worker);
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
        if (task.getName() == WorkerThread.BLANK_NAME) {
            task.setName(nextName());
        }
        //System.out.println("adding worker task "+task.getName());
        if (isActive(task.getName())) {
            throw new RuntimeException("Duplicate task name!");
        }
        synchronized (getThreadQueue().busyTasks) {
            getThreadQueue().idleTasks.add(task);
        }
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
            sleep(sleepTime);
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
            sleep(sleepTime);
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
            sleep(sleepTime);
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
            sleep(sleepTime);
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
            if ((getResult(taskName) == null)
                    && (getBusyTask(taskName) == null)
                    && (getIdleTask(taskName) == null)) {
                return false;
            }
            return true;
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
            if ((getBusyTask(taskName) == null)
                    && (getIdleTask(taskName) == null)) {
                return false;
            }
            return true;
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
        return getThreadQueue().idleTasks.contains(taskName);
    }

    /**
     * retrieves an active task from the busy list
     *
     * @return the active task (if present) or null if it cannot be found
     */
    private static WorkerThread getBusyTask(String taskName) {
        return getTask(taskName, getThreadQueue().busyTasks.iterator());
    }

    /**
     * retrieves an idle task from the idle list
     *
     * @return the idle task (if present) or null if it cannot be found
     */
    private static WorkerThread getIdleTask(String taskName) {
        return getTask(taskName, getThreadQueue().idleTasks.iterator());
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
        int num = numRunningTasks() + numWaitingTasks() + numCompletedTasks();
        return num;
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

            // check for the taskName in the resultsList
            // if it's there, it's already finished
            // *return
            if (getResult(taskName) != null) {
                return;
            }

            // check for it in the idleList
            // *remove it (before it starts running)
            // *return

            if (ThreadQueue.isIdle(taskName)) {
                getThreadQueue().idleTasks.remove(taskName);
                return;
            } // end of if ( getThreadQueue().isIdle(taksName()) {

            // At this point, it must be busy if it is in our system
            // *interrupt it
            // *cleanup
            // *return
            WorkerThread thisWorker = getBusyTask(taskName);
            if (thisWorker != null) {
                Thread thisThread =
                        (Thread) getThreadQueue().runningThreads.get(thisWorker);
                thisThread.interrupt();
                getThreadQueue().busyTasks.remove(thisWorker);
                getThreadQueue().runningThreads.remove(thisThread);
            }
        }
    }

    /**
     * This call wraps the next unique name for a task
     * that is inerted with no name
     */

    private static String nextName() {
        synchronized (nameCounterSynchObject) {
            if (nameCounter == Long.MAX_VALUE) {
                nameCounter = Long.MIN_VALUE;
            }
        }
        nameCounter++;
        return nameCounter + "";
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
        } catch (Exception e) {
        }
    }
}
