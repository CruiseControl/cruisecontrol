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

package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.io.IOException;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import org.apache.log4j.Logger;

/**
 * Takes a script and runs it.  Monitors how long the script takes to run, and
 * returns whether the script completed or not.
 */
public class ScriptRunner  {
    private static final Logger LOG = Logger.getLogger(ScriptRunner.class);
    public static final long NO_TIMEOUT = -1;
    
    public static class AsyncKiller extends Thread {
        private final Process p;
        private final long timeout;
        private boolean killed;

        AsyncKiller(final Process p, final long timeout) {
            this.p = p;
            this.timeout = timeout;
        }

        public void run() {
            try {
                sleep(timeout * 1000L);
                synchronized (this) {
                    p.destroy();
                    killed = true;
                }
            } catch (InterruptedException expected) {
            }
        }

        public synchronized boolean processKilled() {
            return killed;
        }
    }
    /**
     * build and return the results via xml. debug status can be determined from
     * log4j category once we get all the logging in place.
     * 
     * @param workingDir  The directory to run the script from.
     * @param script  The details on the script to be run.
     */
    public boolean runScript(File workingDir, Script script, long timeout) throws CruiseControlException {

        Process p;
        int exitCode = -1;
            
        String[] commandLine = script.getCommandLineArgs();

        String commandLineAsString = asString(commandLine, " ");

        try {
            if (LOG.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Executing Command: '");
                sb.append(commandLineAsString);
                if (workingDir != null) {
                    sb.append("' in directory " + workingDir);
                }
                LOG.debug(sb);
            }

            p = Runtime.getRuntime().exec(commandLine, null, workingDir);
        } catch (IOException e) {
            throw new CruiseControlException("Encountered an IO exception while attempting to execute '" 
                    + commandLineAsString + "'. CruiseControl cannot continue.", e);
        }

        StreamPumper errorPumper = null;
        StreamPumper outPumper = null;
        if (script instanceof StreamConsumer) {
            errorPumper = new StreamPumper(p.getErrorStream(), (StreamConsumer) script);
            outPumper = new StreamPumper(p.getInputStream(), (StreamConsumer) script);            
        } else {
            errorPumper = new StreamPumper(p.getErrorStream());
            outPumper = new StreamPumper(p.getInputStream());
        }
        
        
        new Thread(errorPumper).start();
        new Thread(outPumper).start();
        AsyncKiller killer = new AsyncKiller(p, timeout);
        if (timeout > 0) {
            killer.start();
        }

        try {
            exitCode = p.waitFor();
            killer.interrupt();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (InterruptedException e) {
            LOG.info("Was interrupted while waiting for script to finish."
                    + " CruiseControl will continue, assuming that it completed");
        } catch (IOException ie) {
            LOG.info("Exception trying to close Process streams.", ie);
        }

        outPumper.flush();
        errorPumper.flush();
        
        script.setExitCode(exitCode);
        
        return !killer.processKilled();

    }

    private String asString(Object[] array, String delim) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }
}




