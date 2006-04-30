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
package net.sourceforge.cruisecontrol.util.threadpool;

/**
 * A client for an ThreadQueue task.  This client was designed to
 * sleep (or idle) for a set amount of time
 *
 * @author Jared Richardson
 */

public class IdleThreadQueueClient implements WorkerThread {
    private static final int ONE_SECOND = 1000;
    private static final int TENTH_OF_SECOND = 100;
    
    private final String name;
    private Object result = null;
    private boolean terminate = false;
    private Object mutex = new Object();
    
    public IdleThreadQueueClient(String name) {
        this.name = name;
    }

    public void run() {
        int time = 0;

        while (time < ONE_SECOND) {
            synchronized (mutex) {
                try {
                    mutex.wait(TENTH_OF_SECOND);
                } catch (InterruptedException e) {
                }
            }
            time += TENTH_OF_SECOND;
            if (terminate) {
                break;
            }
        }
        result = "DONE WITH " + name;
    }

    public Object getResult() {
        // result will be null until the process is finished
        return result;
    }

    public void terminate() {
        terminate = true;
        synchronized (mutex) {
            mutex.notifyAll();
        }
        return;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        throw new IllegalStateException("should not be called during tests");
    }

}
